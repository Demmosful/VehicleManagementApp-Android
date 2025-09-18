package com.demmos.parqueaderoapp.ui.login

import com.demmos.parqueaderoapp.data.model.Usuario


sealed class LoginResult {
    object Loading : LoginResult()
    data class Success(val usuario: Usuario) : LoginResult()
    data class Error(val message: String) : LoginResult()
}