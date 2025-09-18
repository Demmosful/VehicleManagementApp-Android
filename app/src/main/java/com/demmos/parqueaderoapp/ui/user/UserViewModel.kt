package com.demmos.parqueaderoapp.ui.user

import androidx.lifecycle.*
import com.demmos.parqueaderoapp.data.model.Usuario
import com.demmos.parqueaderoapp.data.repository.AuthResult
import com.demmos.parqueaderoapp.data.repository.UsuarioRepository
import kotlinx.coroutines.launch

class UserViewModel(private val repository: UsuarioRepository) : ViewModel() {

    val todosLosUsuarios: LiveData<List<Usuario>> = repository.getAllUsers().asLiveData()

    private val _operationResult = MutableLiveData<AuthResult>()
    val operationResult: LiveData<AuthResult> = _operationResult

    fun addUsuario(usuario: Usuario, clave: String) = viewModelScope.launch {
        _operationResult.value = repository.createUser(usuario, clave)
    }

    fun deleteUsuario(usuario: Usuario) = viewModelScope.launch {
        repository.deleteUser(usuario)
    }

    fun updateUser(usuario: Usuario) = viewModelScope.launch {
        repository.updateUserProfile(usuario)
    }
}

class UserViewModelFactory(private val repository: UsuarioRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UserViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}