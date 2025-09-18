package com.demmos.parqueaderoapp.ui.login

import androidx.lifecycle.*
import com.demmos.parqueaderoapp.data.model.Usuario
import com.demmos.parqueaderoapp.data.repository.AuthResult
import com.demmos.parqueaderoapp.data.repository.UsuarioRepository
import kotlinx.coroutines.launch

class LoginViewModel(private val repository: UsuarioRepository) : ViewModel() {

    private val _loginResult = MutableLiveData<LoginResult>()
    val loginResult: LiveData<LoginResult> = _loginResult

    /**
     * Realiza el proceso de login completo: autentica y luego obtiene el perfil.
     */
    fun doLogin(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _loginResult.value = LoginResult.Error("Por favor, introduce el correo y la contraseña.")
            return
        }
        _loginResult.value = LoginResult.Loading

        viewModelScope.launch {
            when (val authResult = repository.signInWithEmail(email, password)) {
                is AuthResult.Success -> {
                    // Si la autenticación es exitosa, obtenemos los detalles del perfil desde Firestore
                    val userDetails = repository.getUserDetails(authResult.user.uid)
                    if (userDetails != null) {
                        // Si tenemos los detalles, el login es un éxito completo
                        _loginResult.value = LoginResult.Success(userDetails)
                    } else {
                        // Caso raro: se autenticó pero no hay perfil en la base de datos
                        _loginResult.value = LoginResult.Error("No se pudieron encontrar los datos del perfil del usuario.")
                        repository.logout() // Cerramos la sesión por seguridad
                    }
                }
                is AuthResult.Error -> {
                    // Si la autenticación falla, mostramos el error
                    _loginResult.value = LoginResult.Error(authResult.message)
                }
            }
        }
    }
}

class LoginViewModelFactory(private val repository: UsuarioRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}