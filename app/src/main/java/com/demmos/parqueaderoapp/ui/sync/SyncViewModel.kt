package com.demmos.parqueaderoapp.ui.sync

import android.util.Log
import androidx.lifecycle.*
import com.demmos.parqueaderoapp.data.model.Vehiculo
import com.demmos.parqueaderoapp.data.repository.UsuarioRepository
import com.demmos.parqueaderoapp.data.repository.VehiculoRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

sealed class SyncResult {
    object Loading : SyncResult()
    data class Success(val message: String, val operationTag: String = "generic") : SyncResult()
    data class Error(val message: String) : SyncResult()
}

class SyncViewModel(
    private val vehiculoRepository: VehiculoRepository,
    private val usuarioRepository: UsuarioRepository
) : ViewModel() {

    private val _syncResult = MutableLiveData<SyncResult>()
    val syncResult: LiveData<SyncResult> = _syncResult

    private fun parsearFecha(fechaString: String?): Long? {
        if (fechaString.isNullOrBlank()) return null
        val formatos = listOf(
            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()),
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        )
        for (formato in formatos) {
            try {
                return formato.parse(fechaString)?.time
            } catch (e: Exception) { /* Ignorar y probar el siguiente */ }
        }
        return null
    }

    fun importarVehiculosDesdeCSV(inputStream: InputStream) {
        _syncResult.value = SyncResult.Loading
        viewModelScope.launch {
            val currentUser = Firebase.auth.currentUser?.uid?.let { usuarioRepository.getUserDetails(it) }
            if (currentUser == null) {
                _syncResult.postValue(SyncResult.Error("No se pudo identificar al usuario actual."))
                return@launch
            }

            var registrosNuevos = 0
            var registrosOmitidos = 0
            var registrosActualizados = 0
            val lineasConError = mutableListOf<String>()

            try {

                val todosLosVehiculosDB = withContext(Dispatchers.IO) { vehiculoRepository.getAllVehiclesSuspend() }



                val matriculasActivasEnEsteCSV = mutableSetOf<String>()

                withContext(Dispatchers.IO) {
                    val lineas = inputStream.bufferedReader().readLines()
                    val cabecera = lineas.firstOrNull()?.uppercase()?.trim() ?: throw Exception("El archivo CSV está vacío.")
                    val separador = if (cabecera.contains(';')) ';' else ','

                    lineas.drop(1).forEach { line ->
                        try {
                            if (line.isBlank()) return@forEach
                            val tokens = line.split(separador).map { it.trim().removeSurrounding("\"") }

                            val matricula = tokens.getOrNull(1)?.uppercase() ?: throw Exception("Línea sin matrícula")
                            val fechaSalidaStr = tokens.getOrNull(8)
                            val esRegistroActivoEnCSV = fechaSalidaStr.isNullOrBlank() || fechaSalidaStr == "-"


                            val registrosPreviosEnDB = todosLosVehiculosDB.filter { it.matricula == matricula }




                            if (esRegistroActivoEnCSV && matriculasActivasEnEsteCSV.contains(matricula)) {
                                registrosOmitidos++
                                return@forEach
                            }


                            if (esRegistroActivoEnCSV && registrosPreviosEnDB.any { it.estado == "activo" }) {
                                registrosOmitidos++
                                return@forEach
                            }


                            val fechaIngresoCSV = parsearFecha(tokens.getOrNull(6))
                            if (registrosPreviosEnDB.any { it.fechaIngreso == fechaIngresoCSV }) {
                                registrosOmitidos++
                                return@forEach
                            }


                            val id = tokens.getOrNull(0)?.takeIf { it.isNotBlank() } ?: Firebase.firestore.collection("vehiculos").document().id
                            val fechaSalida = parsearFecha(fechaSalidaStr)

                            val vehiculoACrear = Vehiculo(
                                id = id,
                                matricula = matricula,
                                marcaNombre = tokens.getOrNull(2) ?: "-",
                                modeloNombre = tokens.getOrNull(3) ?: "-",
                                ubicacion = tokens.getOrNull(4) ?: "-",
                                detalle = tokens.getOrNull(5)?.takeIf { it.isNotBlank() },
                                fechaIngreso = fechaIngresoCSV ?: System.currentTimeMillis(),
                                estado = if (esRegistroActivoEnCSV) "activo" else "salido",
                                usuarioRegistrador = "importado",
                                nombreCompletoRegistrador = tokens.getOrNull(7) ?: currentUser.nombreCompleto,
                                fechaSalida = fechaSalida,
                                usuarioSalidaId = if (!esRegistroActivoEnCSV) "importado" else null,
                                nombreCompletoUsuarioSalida = tokens.getOrNull(9)?.takeIf { it != "-" }
                            )

                            vehiculoRepository.insertVehiculo(vehiculoACrear)
                            registrosNuevos++


                            if (vehiculoACrear.estado == "activo") {
                                matriculasActivasEnEsteCSV.add(matricula)
                            }

                        } catch (e: Exception) {
                            lineasConError.add(line)
                            Log.e("SyncVM", "Error procesando línea: '$line'", e)
                        }
                    }
                }

                val resumen = StringBuilder("Importación finalizada.\n\n")
                resumen.append("Registros nuevos creados: $registrosNuevos\n")
                resumen.append("Registros existentes actualizados: $registrosActualizados\n")
                resumen.append("Registros omitidos (duplicados): $registrosOmitidos\n")
                resumen.append("Filas con error: ${lineasConError.size}")
                _syncResult.postValue(SyncResult.Success(resumen.toString(), "import"))

            } catch (e: Exception) {
                _syncResult.postValue(SyncResult.Error("Error al leer el archivo: ${e.message}"))
            }
        }
    }

    fun exportarVehiculosACSV(fechaInicio: Long, fechaFin: Long, outputStream: OutputStream) {
        _syncResult.value = SyncResult.Loading
        viewModelScope.launch {
            try {
                val vehiculosDelRango = vehiculoRepository.getVehiculosEntreFechasSuspend(fechaInicio, fechaFin)

                if (vehiculosDelRango.isEmpty()) {
                    _syncResult.postValue(SyncResult.Success("No se encontraron vehículos en el rango de fechas.", "export"))
                    return@launch
                }

                withContext(Dispatchers.IO) {
                    outputStream.bufferedWriter().use { writer ->
                        writer.write(""""ID";"Matricula";"Marca";"Modelo";"Ubicacion";"Detalle";"FechaIngreso";"UsuarioIngreso";"FechaSalida";"UsuarioSalida"""")
                        writer.newLine()

                        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

                        vehiculosDelRango.forEach { vehiculo ->
                            val fechaIngresoStr = sdf.format(Date(vehiculo.fechaIngreso))
                            val fechaSalidaStr = vehiculo.fechaSalida?.let { sdf.format(Date(it)) } ?: "-"
                            val usuarioIngreso = vehiculo.nombreCompletoRegistrador ?: vehiculo.usuarioRegistrador
                            val usuarioSalida = vehiculo.nombreCompletoUsuarioSalida ?: "-"

                            val fila = listOf(
                                vehiculo.id,
                                vehiculo.matricula,
                                vehiculo.marcaNombre,
                                vehiculo.modeloNombre,
                                vehiculo.ubicacion,
                                vehiculo.detalle ?: "",
                                fechaIngresoStr,
                                usuarioIngreso,
                                fechaSalidaStr,
                                usuarioSalida
                            ).joinToString(";") { "\"${it}\"" }

                            writer.write(fila)
                            writer.newLine()
                        }
                    }
                }
                _syncResult.postValue(SyncResult.Success("Reporte de ${vehiculosDelRango.size} vehículos exportado.", "export"))
            } catch (e: Exception) {
                _syncResult.postValue(SyncResult.Error("Error al exportar el reporte: ${e.message}"))
            }
        }
    }
}

class SyncViewModelFactory(
    private val vehiculoRepository: VehiculoRepository,
    private val usuarioRepository: UsuarioRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SyncViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SyncViewModel(vehiculoRepository, usuarioRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}