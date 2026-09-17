package com.example.data.auth

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SupabaseAuthRepository(
    private val client: SupabaseClient,
    private val ownershipManager: com.example.data.sync.DatasetOwnershipManager? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) : AuthRepository {

    private val _authState = MutableStateFlow<AuthState>(AuthState.LocalOnly)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        scope.launch {
            client.auth.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        evaluateAuthState()
                    }
                    is SessionStatus.NotAuthenticated -> {
                        _authState.value = AuthState.LocalOnly
                    }
                    is SessionStatus.Initializing -> {
                        _authState.value = AuthState.Loading
                    }
                    is SessionStatus.RefreshFailure -> {
                        // Session expired or refresh token invalid; return to local-only with prompt for re-auth
                        _authState.value = AuthState.LocalOnly
                    }
                }
            }
        }
    }

    private suspend fun evaluateAuthState() {
        val user = client.auth.currentUserOrNull()
        if (user != null) {
            val ownerId = ownershipManager?.getOwnerUserId()
            if (ownerId != null && ownerId != user.id) {
                _authState.value = AuthState.AccountMismatch(
                    currentUserId = user.id,
                    currentEmail = user.email ?: "",
                    ownerUserId = ownerId
                )
            } else {
                _authState.value = AuthState.Authenticated(
                    userId = user.id,
                    email = user.email ?: ""
                )
            }
        } else {
            _authState.value = AuthState.LocalOnly
        }
    }

    override suspend fun refreshAuthState() {
        evaluateAuthState()
    }

    override suspend fun signIn(email: String, password: String): Result<Unit> {
        _authState.value = AuthState.Loading
        return try {
            client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            Result.success(Unit)
        } catch (e: Exception) {
            val message = e.message ?: "Failed to sign in"
            _authState.value = AuthState.Error(message)
            Result.failure(e)
        }
    }

    override suspend fun signUp(email: String, password: String): Result<Unit> {
        _authState.value = AuthState.Loading
        return try {
            client.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            Result.success(Unit)
        } catch (e: Exception) {
            val message = e.message ?: "Failed to sign up"
            _authState.value = AuthState.Error(message)
            Result.failure(e)
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            client.auth.signOut()
            _authState.value = AuthState.LocalOnly
            Result.success(Unit)
        } catch (e: Exception) {
            _authState.value = AuthState.LocalOnly
            Result.failure(e)
        }
    }

    override fun getCurrentUserId(): String? = client.auth.currentUserOrNull()?.id

    override fun getCurrentEmail(): String? = client.auth.currentUserOrNull()?.email
}
