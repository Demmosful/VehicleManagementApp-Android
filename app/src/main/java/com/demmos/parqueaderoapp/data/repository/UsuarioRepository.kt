// RUTA: java/com/demmos/parqueaderoapp/data/repository/UsuarioRepository.kt

package com.demmos.parqueaderoapp.data.repository

import android.util.Log
import com.demmos.parqueaderoapp.data.model.Usuario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

sealed class AuthResult {
    data class Success(val user: FirebaseUser) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class UsuarioRepository {

    private val auth: FirebaseAuth = Firebase.auth
    private val usersCollection = Firebase.firestore.collection("usuarios")

    suspend fun signInWithEmail(email: String, password: String): AuthResult {
        return try {
            val credential = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = credential.user
            if (firebaseUser != null) {
                AuthResult.Success(firebaseUser)
            } else {
                AuthResult.Error("El usuario de Firebase es nulo después del login.")
            }
        } catch (e: Exception) {
            Log.e("AuthError", "Error en el inicio de sesión: ${e.message}")
            AuthResult.Error(e.localizedMessage ?: "Ocurrió un error desconocido.")
        }
    }

    suspend fun createUser(usuario: Usuario, clave: String): AuthResult {
        try {
            val credential = auth.createUserWithEmailAndPassword(usuario.nombre, clave).await()
            val firebaseUser = credential.user
            if (firebaseUser != null) {
                // Quitamos el campo 'clave' si existiera en el modelo antes de guardar
                val usuarioParaGuardar = usuario.copy(id = firebaseUser.uid)
                usersCollection.document(firebaseUser.uid).set(usuarioParaGuardar).await()
                return AuthResult.Success(firebaseUser)
            } else {
                return AuthResult.Error("El usuario de Firebase es nulo después del registro.")
            }
        } catch (e: Exception) {
            Log.e("AuthError", "Error en el registro: ${e.message}")
            return AuthResult.Error(e.localizedMessage ?: "Ocurrió un error desconocido.")
        }
    }

    fun logout() {
        auth.signOut()
    }

    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    suspend fun getUserDetails(uid: String): Usuario? {
        return try {
            usersCollection.document(uid).get().await().toObject(Usuario::class.java)
        } catch (e: Exception) {
            Log.e("FirestoreError", "Error al obtener detalles del usuario: ", e)
            null
        }
    }

    // --- FUNCIÓN QUE FALTABA ---
    fun getAllUsers(): Flow<List<Usuario>> = callbackFlow {
        val listener = usersCollection.orderBy("nombreCompleto").addSnapshotListener { snapshot, error ->
            if (error != null) {
                cancel()
                return@addSnapshotListener
            }
            snapshot?.let { trySend(it.toObjects()) }
        }
        awaitClose { listener.remove() }
    }

    // --- FUNCIÓN QUE FALTABA ---
    suspend fun deleteUser(usuario: Usuario) {
        if (usuario.id.isBlank()) return
        try {
            // Nota: Esto solo borra el perfil de Firestore. Borrar de Auth es una operación de admin avanzada.
            usersCollection.document(usuario.id).delete().await()
        } catch (e: Exception) {
            Log.e("FirestoreError", "Error al borrar usuario de Firestore", e)
            throw e
        }
    }

    suspend fun updateUserProfile(usuario: Usuario) {
        if (usuario.id.isBlank()) return
        try {
            usersCollection.document(usuario.id).set(usuario).await()
        } catch (e: Exception) {
            Log.e("FirestoreError", "Error al actualizar el perfil de usuario", e)
            throw e
        }
    }
}