package com.arhomedesign.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arhomedesign.auth.data.AuthRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthInFlight {
    None,
    SignIn,
    SignUp
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val inFlight: AuthInFlight = AuthInFlight.None,
    val isLoggedIn: Boolean = false,
    val error: String? = null
)

class AuthViewModel(
    private val repository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState(isLoggedIn = repository.isLoggedIn()))
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, inFlight = AuthInFlight.SignIn, error = null)
            }
            runCatching { repository.login(email, password) }
                .onSuccess {
                    _uiState.value = AuthUiState(isLoggedIn = true)
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            inFlight = AuthInFlight.None,
                            isLoggedIn = false,
                            error = mapAuthError(throwable)
                        )
                    }
                }
        }
    }

    fun register(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, inFlight = AuthInFlight.SignUp, error = null)
            }
            runCatching { repository.register(email, password) }
                .onSuccess {
                    _uiState.value = AuthUiState(isLoggedIn = true)
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            inFlight = AuthInFlight.None,
                            isLoggedIn = false,
                            error = mapAuthError(throwable)
                        )
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

private fun mapAuthError(throwable: Throwable): String {
    if (throwable is FirebaseNetworkException) {
        return "Network error. Check your connection and try again."
    }
    val e = throwable as? FirebaseAuthException
        ?: return throwable.message?.takeIf { it.isNotBlank() }
            ?: "Something went wrong. Check your connection and try again."

    return when (e.errorCode) {
        "ERROR_EMAIL_ALREADY_IN_USE" ->
            "This email is already registered. Switch to Sign in or use another email."
        "ERROR_INVALID_EMAIL" -> "Enter a valid email address."
        "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL" ->
            "Incorrect email or password."
        "ERROR_USER_NOT_FOUND" -> "No account found for this email. Try creating an account."
        "ERROR_USER_DISABLED" -> "This account has been disabled."
        "ERROR_WEAK_PASSWORD" -> "Password must be at least 6 characters."
        "ERROR_NETWORK_REQUEST_FAILED" -> "Network error. Check your connection and try again."
        "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Please wait a moment and try again."
        "ERROR_OPERATION_NOT_ALLOWED" ->
            "Email/password sign-in is not enabled for this project in Firebase Console."
        else -> e.message?.takeIf { it.isNotBlank() } ?: "Authentication failed (${e.errorCode})."
    }
}
