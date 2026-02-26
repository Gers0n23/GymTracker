package com.gcordero.gymtracker.ui.screens.auth

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcordero.gymtracker.domain.model.User
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthUiState {
    object Idle    : AuthUiState()
    object Loading : AuthUiState()
    object Success : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel : ViewModel() {

    private val auth      = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // ── Email / Password ─────────────────────────────────────────────────────

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Completa todos los campos")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                auth.signInWithEmailAndPassword(email.trim(), password).await()
                _uiState.value = AuthUiState.Success
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(parseError(e.message))
            }
        }
    }

    fun register(name: String, email: String, password: String) {
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Completa todos los campos")
            return
        }
        if (password.length < 6) {
            _uiState.value = AuthUiState.Error("La contraseña debe tener al menos 6 caracteres")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
                val uid    = result.user?.uid ?: throw Exception("No se pudo obtener el UID")
                val user   = User(id = uid, name = name.trim(), email = email.trim())
                firestore.collection("users").document(uid).set(user).await()
                _uiState.value = AuthUiState.Success
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(parseError(e.message))
            }
        }
    }

    // ── Google Sign-In ───────────────────────────────────────────────────────

    /** Devuelve el Intent que lanza el selector de cuentas de Google. */
    fun createGoogleSignInIntent(context: Context): Intent {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(WEB_CLIENT_ID)
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso).signInIntent
    }

    /** Recibe el ID token de Google y lo intercambia por credencial Firebase. */
    fun handleGoogleSignInResult(idToken: String?) {
        if (idToken == null) {
            _uiState.value = AuthUiState.Error("No se pudo iniciar sesión con Google")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(credential).await()

                // Crear documento en Firestore si es la primera vez con Google
                auth.currentUser?.let { user ->
                    val userRef = firestore.collection("users").document(user.uid)
                    if (!userRef.get().await().exists()) {
                        userRef.set(
                            User(
                                id    = user.uid,
                                name  = user.displayName ?: "",
                                email = user.email ?: ""
                            )
                        ).await()
                    }
                }
                _uiState.value = AuthUiState.Success
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(parseError(e.message))
            }
        }
    }

    // ── Sign out / utils ─────────────────────────────────────────────────────

    fun signOut() {
        auth.signOut()
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }

    private fun parseError(msg: String?): String = when {
        msg == null -> "Error desconocido"
        msg.contains("no user record")                  ||
        msg.contains("user-not-found")                   -> "No existe una cuenta con ese correo"
        msg.contains("password is invalid")             ||
        msg.contains("wrong-password")                   -> "Contraseña incorrecta"
        msg.contains("email address is already in use") ||
        msg.contains("email-already-in-use")             -> "Ya existe una cuenta con ese correo"
        msg.contains("badly formatted")                 ||
        msg.contains("invalid-email")                    -> "Correo electrónico inválido"
        msg.contains("network")                          -> "Error de red. Revisa tu conexión"
        else                                             -> "Error: $msg"
    }

    companion object {
        private const val WEB_CLIENT_ID =
            "510442852605-404tsgmao7nt57gsd81ic2ie9p7v0dmv.apps.googleusercontent.com"
    }
}
