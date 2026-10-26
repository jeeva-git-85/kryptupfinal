package com.kryptup.kryptup.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kryptup.kryptup.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _authState.value = AuthState.Error("Please fill all fields")
            return
        }
        
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            repository.login(email, pass)
                .onSuccess {
                    _authState.value = AuthState.Success("Login successful")
                }
                .onFailure {
                    _authState.value = AuthState.Error(it.message ?: "Login failed")
                }
        }
    }

    fun register(email: String, pass: String, name: String) {
        val cleanEmail = email.trim()
        val cleanName = name.trim()
        if (cleanEmail.isBlank() || pass.isBlank() || cleanName.isBlank()) {
            _authState.value = AuthState.Error("Please fill all fields")
            return
        }
        if (pass.length < 6) {
            _authState.value = AuthState.Error("Password must be at least 6 characters long")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            repository.register(cleanEmail, pass, cleanName)
                .onSuccess {
                    _authState.value = AuthState.Success("Registration successful")
                }
                .onFailure {
                    _authState.value = AuthState.Error(it.message ?: "Registration failed")
                }
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    fun sendPasswordResetEmail(email: String) {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank()) {
            _authState.value = AuthState.Error("Please enter your email address")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            repository.sendPasswordResetEmail(cleanEmail)
                .onSuccess {
                    _authState.value = AuthState.ResetEmailSent(cleanEmail)
                }
                .onFailure {
                    _authState.value = AuthState.Error(it.message ?: "Failed to send reset email")
                }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            repository.firebaseSignInWithGoogle(idToken)
                .onSuccess {
                    _authState.value = AuthState.Success("Google Sign-In successful")
                }
                .onFailure {
                    _authState.value = AuthState.Error(it.message ?: "Google Sign-In failed")
                }
        }
    }
}
