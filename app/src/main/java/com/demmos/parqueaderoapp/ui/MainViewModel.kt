package com.demmos.parqueaderoapp.ui

import androidx.lifecycle.*
import com.demmos.parqueaderoapp.data.model.Marca
import com.demmos.parqueaderoapp.data.model.Modelo
import com.demmos.parqueaderoapp.data.model.Usuario
import com.demmos.parqueaderoapp.data.model.Vehiculo
import com.demmos.parqueaderoapp.data.repository.UsuarioRepository
import com.demmos.parqueaderoapp.data.repository.VehiculoRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class MainEvent {
    data class ShowToast(val message: String) : MainEvent()
    data class ShowDialog(val title: String, val message: String) : MainEvent()
}

class MainViewModel(
    private val vehiculoRepository: VehiculoRepository,
    private val usuarioRepository: UsuarioRepository
) : ViewModel() {

    private val _event = MutableStateFlow<MainEvent?>(null)
    val event: LiveData<MainEvent?> = _event.asLiveData()

    private val _currentUserProfile = MutableLiveData<Usuario?>()
    val currentUserProfile: LiveData<Usuario?> = _currentUserProfile

    private val _searchQuery = MutableStateFlow("")

    private val todosLosVehiculosSource: StateFlow<List<Vehiculo>> = vehiculoRepository.getTodosLosVehiculosStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todosLosVehiculos: LiveData<List<Vehiculo>> = combine(
        todosLosVehiculosSource,
        _searchQuery
    ) { vehiculos, query ->
        if (query.isBlank()) {
            vehiculos
        } else {
            vehiculos.filter { it.matricula.contains(query, ignoreCase = true) }
        }
    }.asLiveData()

    val vehiculosActivosPorAntiguedad: LiveData<List<Vehiculo>> = vehiculoRepository.getVehiculosActivosStream().asLiveData()
    val conteoVehiculosActivos: LiveData<Int> = vehiculoRepository.getConteoVehiculosActivosStream().asLiveData()
    val todasLasMarcas: LiveData<List<Marca>> = vehiculoRepository.getTodasLasMarcasStream().asLiveData()

    private val _marcaSeleccionadaId = MutableStateFlow<String?>(null)
    val modelosDeMarcaSeleccionada: LiveData<List<Modelo>> = _marcaSeleccionadaId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else vehiculoRepository.getModelosPorMarcaStream(id)
    }.asLiveData()

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        Firebase.auth.currentUser?.uid?.let { uid ->
            viewModelScope.launch {
                _currentUserProfile.value = usuarioRepository.getUserDetails(uid)
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun onMarcaSeleccionada(marcaId: String) {
        _marcaSeleccionadaId.value = marcaId
    }

    fun limpiarSeleccionModelos() {
        _marcaSeleccionadaId.value = null
    }

    fun onEventHandled() {
        _event.value = null
    }


    fun insertVehiculo(vehiculo: Vehiculo) = viewModelScope.launch {
        if (vehiculoRepository.isVehiculoActive(vehiculo.matricula)) {
            _event.value = MainEvent.ShowDialog(
                "Vehículo ya se encuentra en campa",
                "Si es una nueva entrada, debe marcar la salida del registro anterior para poder crear uno nuevo."
            )
        } else {
            try {
                vehiculoRepository.insertVehiculo(vehiculo)
                _event.value = MainEvent.ShowToast("Vehículo ${vehiculo.matricula} guardado.")
            } catch (e: Exception) {
                _event.value = MainEvent.ShowDialog("Error", "No se pudo guardar el vehículo.")
            }
        }
    }

    fun borrarRegistrosPorPeriodo(fechaInicio: Long, fechaFin: Long) {
        viewModelScope.launch {
            try {
                _event.value = MainEvent.ShowToast("Borrando registros...")
                val count = vehiculoRepository.borrarVehiculosPorPeriodo(fechaInicio, fechaFin)
                _event.value = MainEvent.ShowDialog("Operación Completada", "$count registros han sido eliminados permanentemente.")
            } catch (e: Exception) {
                _event.value = MainEvent.ShowDialog("Error", "No se pudieron eliminar los registros: ${e.message}")
            }
        }
    }

    fun updateVehiculo(vehiculo: Vehiculo) = viewModelScope.launch {
        vehiculoRepository.updateVehiculo(vehiculo)
    }

    fun marcarVehiculoComoSalido(vehiculo: Vehiculo) = viewModelScope.launch {
        try {
            val user = _currentUserProfile.value ?: throw IllegalStateException("Usuario actual no encontrado")

            val vehiculoActualizado = vehiculo.copy(
                estado = "salido",
                fechaSalida = System.currentTimeMillis(),
                nombreCompletoUsuarioSalida = user.nombreCompleto
            )
            vehiculoRepository.updateVehiculo(vehiculoActualizado)
        } catch (e: Exception) {
            _event.value = MainEvent.ShowDialog("Error", "No se pudo marcar la salida: ${e.message}")
        }
    }

    fun deleteVehiculo(vehiculo: Vehiculo) = viewModelScope.launch {
        vehiculoRepository.deleteVehiculo(vehiculo)
    }

    fun deleteMultipleVehiculos(vehiculos: List<Vehiculo>) = viewModelScope.launch {
        vehiculos.forEach { vehiculoRepository.deleteVehiculo(it) }
    }

    suspend fun findOrCreateMarca(nombre: String): Marca {
        return vehiculoRepository.findOrCreateMarca(nombre)
    }

    suspend fun findOrCreateModelo(nombre: String, marcaId: String): Modelo {
        return vehiculoRepository.findOrCreateModelo(nombre, marcaId)
    }
}

class MainViewModelFactory(
    private val vehiculoRepository: VehiculoRepository,
    private val usuarioRepository: UsuarioRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(vehiculoRepository, usuarioRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}