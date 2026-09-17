package com.example.data.auth

import kotlinx.coroutines.flow.StateFlow

/**
 * Authentication state representing either offline/local-only mode
 * or an active authenticated session with cloud backup enabled.
 */
sealed class AuthState {
    data object LocalOnly : AuthState()
    data object Loading : AuthState()
    data class Authenticated(val userId: String, val email: String) : AuthState()
    data class AccountMismatch(
        val currentUserId: String,
        val currentEmail: String,
        val ownerUserId: String
    ) : AuthState()
    data class Error(val message: String) : AuthState()
}

/**
 * Provider-agnostic authentication repository.
 * Keeps auth strictly optional for core app functionality.
 */
interface AuthRepository {
    val authState: StateFlow<AuthState>

    suspend fun signIn(email: String, password: String): Result<Unit>

    suspend fun signUp(email: String, password: String): Result<Unit>

    suspend fun signOut(): Result<Unit>

    suspend fun refreshAuthState()

    fun getCurrentUserId(): String?

    fun getCurrentEmail(): String?
}
