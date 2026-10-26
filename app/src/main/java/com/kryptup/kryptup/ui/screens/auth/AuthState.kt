package com.kryptup.kryptup.ui.screens.auth

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val message: String) : AuthState()
    data class Error(val message: String) : AuthState()
    data class ResetEmailSent(val email: String) : AuthState()
}
