package com.demmos.parqueaderoapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.demmos.parqueaderoapp.data.repository.UsuarioRepository
import com.demmos.parqueaderoapp.databinding.ActivityLoginBinding
import com.demmos.parqueaderoapp.ui.login.LoginResult
import com.demmos.parqueaderoapp.ui.login.LoginViewModel
import com.demmos.parqueaderoapp.ui.login.LoginViewModelFactory
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var loginViewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- SOLUCIÓN DIRECTA Y DEFINITIVA ---
        // Forzamos la inicialización de Firebase aquí, antes de usar cualquier servicio.
        FirebaseApp.initializeApp(this)
        // ------------------------------------

        // Ahora, esta comprobación se ejecutará DESPUÉS de la inicialización.
        if (Firebase.auth.currentUser != null) {
            navigateToMain()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val repository = UsuarioRepository()
        val factory = LoginViewModelFactory(repository)
        loginViewModel = ViewModelProvider(this, factory)[LoginViewModel::class.java]

        setupListeners()
        setupObservers()
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString()
            loginViewModel.doLogin(email, password)
        }
    }

    private fun setupObservers() {
        loginViewModel.loginResult.observe(this) { result ->
            when (result) {
                is LoginResult.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnLogin.isEnabled = false
                }
                is LoginResult.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnLogin.isEnabled = true
                    Toast.makeText(this, "Bienvenido ${result.usuario.nombreCompleto}", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                }
                is LoginResult.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnLogin.isEnabled = true
                    Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}