package com.demmos.parqueaderoapp.data.repository

import android.util.Log
import com.demmos.parqueaderoapp.data.model.Marca
import com.demmos.parqueaderoapp.data.model.Modelo
import com.demmos.parqueaderoapp.data.model.Vehiculo
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.*

class VehiculoRepository {

    private val db = Firebase.firestore
    private val vehiculosCollection = db.collection("vehiculos")
    private val marcasCollection = db.collection("marcas")
    private val modelosCollection = db.collection("modelos")


    suspend fun isVehiculoActive(matricula: String): Boolean {
        return try {
            val querySnapshot = vehiculosCollection
                .whereEqualTo("matricula", matricula)
                .whereEqualTo("estado", "activo")
                .limit(1)
                .get()
                .await()
            !querySnapshot.isEmpty
        } catch (e: Exception) {
            Log.e("FirestoreError", "Error al verificar si el vehículo está activo", e)
            false
        }
    }

    suspend fun borrarVehiculosPorPeriodo(fechaInicio: Long, fechaFin: Long): Int {
        return try {
            val querySnapshot = vehiculosCollection
                .whereGreaterThanOrEqualTo("fechaIngreso", fechaInicio)
                .whereLessThanOrEqualTo("fechaIngreso", fechaFin)
                .get()
                .await()

            if (querySnapshot.isEmpty) {
                return 0
            }


            val batch = db.batch()
            for (document in querySnapshot.documents) {
                batch.delete(document.reference)
            }
            batch.commit().await()

            querySnapshot.size()
        } catch (e: Exception) {
            Log.e("FirestoreError", "Error al borrar vehículos por período", e)
            throw e // Relanza la excepción para que el ViewModel la maneje
        }
    }

    suspend fun insertVehiculo(vehiculo: Vehiculo) {
        try {
            if (vehiculo.id.isBlank()) {
                vehiculosCollection.add(vehiculo).await()
            } else {
                vehiculosCollection.document(vehiculo.id).set(vehiculo).await()
            }
        } catch (e: Exception) {
            Log.e("FirestoreError", "Error al añadir vehículo: ", e)
            throw e
        }
    }

    suspend fun updateVehiculo(vehiculo: Vehiculo) {
        if (vehiculo.id.isBlank()) throw IllegalArgumentException("ID de vehículo no puede estar vacío para actualizar.")
        try {
            vehiculosCollection.document(vehiculo.id).set(vehiculo).await()
        } catch (e: Exception) {
            Log.e("FirestoreError", "Error al actualizar vehículo: ", e)
            throw e
        }
    }

    suspend fun deleteVehiculo(vehiculo: Vehiculo) {
        if (vehiculo.id.isBlank()) throw IllegalArgumentException("ID de vehículo no puede estar vacío para eliminar.")
        try {
            vehiculosCollection.document(vehiculo.id).delete().await()
        } catch (e: Exception) {
            Log.e("FirestoreError", "Error al eliminar vehículo: ", e)
            throw e
        }
    }

    fun getTodosLosVehiculosStream(): Flow<List<Vehiculo>> = callbackFlow {
        val listener = vehiculosCollection
            .orderBy("fechaIngreso", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { cancel(); return@addSnapshotListener }
                snapshot?.let {
                    val vehiculos = it.documents.mapNotNull { doc -> doc.toObject<Vehiculo>()?.copy(id = doc.id) }
                    trySend(vehiculos)
                }
            }
        awaitClose { listener.remove() }
    }

    fun getVehiculosActivosStream(): Flow<List<Vehiculo>> = callbackFlow {
        val listener = vehiculosCollection
            .whereEqualTo("estado", "activo")
            .orderBy("fechaIngreso", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { cancel(); return@addSnapshotListener }
                snapshot?.let {
                    val vehiculos = it.documents.mapNotNull { doc -> doc.toObject<Vehiculo>()?.copy(id = doc.id) }
                    trySend(vehiculos)
                }
            }
        awaitClose { listener.remove() }
    }

    fun getConteoVehiculosActivosStream(): Flow<Int> = callbackFlow {
        val listener = vehiculosCollection
            .whereEqualTo("estado", "activo")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { cancel(); return@addSnapshotListener }
                snapshot?.let { trySend(it.size()) }
            }
        awaitClose { listener.remove() }
    }

    fun getTodasLasMarcasStream(): Flow<List<Marca>> = callbackFlow {
        val listener = marcasCollection.orderBy("nombre").addSnapshotListener { snapshot, error ->
            if (error != null) { cancel(); return@addSnapshotListener }
            snapshot?.let {
                val marcas = it.documents.mapNotNull { doc -> doc.toObject<Marca>()?.copy(id = doc.id) }
                trySend(marcas)
            }
        }
        awaitClose { listener.remove() }
    }

    fun getModelosPorMarcaStream(marcaId: String): Flow<List<Modelo>> = callbackFlow {
        val listener = modelosCollection
            .whereEqualTo("marcaId", marcaId)
            .orderBy("nombre")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { cancel(); return@addSnapshotListener }
                snapshot?.let {
                    val modelos = it.documents.mapNotNull { doc -> doc.toObject<Modelo>()?.copy(id = doc.id) }
                    trySend(modelos)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun findOrCreateMarca(nombre: String): Marca {
        val nombreFormateado = nombre.uppercase().trim()
        val querySnapshot = marcasCollection.whereEqualTo("nombre", nombreFormateado).limit(1).get().await()
        return if (!querySnapshot.isEmpty) {
            val document = querySnapshot.documents.first()
            document.toObject<Marca>()!!.copy(id = document.id)
        } else {
            val nuevaMarca = Marca(nombre = nombreFormateado)
            val documentReference = marcasCollection.add(nuevaMarca).await()
            nuevaMarca.copy(id = documentReference.id)
        }
    }

    suspend fun findOrCreateModelo(nombre: String, marcaId: String): Modelo {
        val nombreFormateado = nombre.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        val querySnapshot = modelosCollection
            .whereEqualTo("nombre", nombreFormateado)
            .whereEqualTo("marcaId", marcaId)
            .limit(1).get().await()
        return if (!querySnapshot.isEmpty) {
            val document = querySnapshot.documents.first()
            document.toObject<Modelo>()!!.copy(id = document.id)
        } else {
            val nuevoModelo = Modelo(nombre = nombreFormateado, marcaId = marcaId)
            val documentReference = modelosCollection.add(nuevoModelo).await()
            nuevoModelo.copy(id = documentReference.id)
        }
    }

    suspend fun getAllVehiclesSuspend(): List<Vehiculo> {
        return try {
            vehiculosCollection.get().await().toObjects(Vehiculo::class.java)
        } catch (e: Exception) {
            Log.e("FirestoreError", "Error al obtener todos los vehículos (suspend): ", e)
            emptyList()
        }
    }

    suspend fun getTodasLasMarcasSuspend(): List<Marca> {
        return try {
            marcasCollection.get().await().toObjects(Marca::class.java)
        } catch (e: Exception) {
            Log.e("FirestoreError", "Error obteniendo todas las marcas (suspend)", e)
            emptyList()
        }
    }

    suspend fun getVehiculosEntreFechasSuspend(fechaInicio: Long, fechaFin: Long): List<Vehiculo> {
        return try {
            vehiculosCollection
                .whereGreaterThanOrEqualTo("fechaIngreso", fechaInicio)
                .whereLessThanOrEqualTo("fechaIngreso", fechaFin)
                .get()
                .await()
                .toObjects(Vehiculo::class.java)
        } catch (e: Exception) {
            Log.e("FirestoreError", "Error obteniendo vehículos entre fechas (suspend)", e)
            emptyList()
        }
    }
}