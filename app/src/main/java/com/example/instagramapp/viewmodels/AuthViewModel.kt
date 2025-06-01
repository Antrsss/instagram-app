package com.example.instagramapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.instagramapp.repos.UserRepository
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Initial)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val currentUser: FirebaseUser?
        get() = userRepository.currentUser

    init {
        observeAuthState()
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            userRepository.authState.collect { user ->
                _uiState.value = if (user != null) {
                    if (user.uid.isNullOrBlank()) {
                        AuthUiState.Error("User ID is missing")
                    } else {
                        AuthUiState.Authenticated(user)
                    }
                } else {
                    AuthUiState.Unauthenticated
                }
            }
        }
    }

    fun signIn(email: String, password: String) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            userRepository.signIn(email, password)
                .onSuccess { user ->
                    _uiState.value = AuthUiState.Authenticated(user)
                }
                .onFailure { error ->
                    _uiState.value = AuthUiState.Error(error.message ?: "Sign in failed")
                }
        }
    }

    fun signUp(email: String, password: String) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            userRepository.signUp(email, password)
                .onSuccess { user ->
                    _uiState.value = AuthUiState.Authenticated(user)
                }
                .onFailure { error ->
                    _uiState.value = AuthUiState.Error(error.message ?: "Sign up failed")
                }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            userRepository.signOut()
            _uiState.value = AuthUiState.Unauthenticated
        }
    }

    fun sendEmailVerification() {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            userRepository.sendEmailVerification()
                .onSuccess {
                    _uiState.value = uiState.value.copyWithMessage("Verification email sent")
                }
                .onFailure { error ->
                    _uiState.value = AuthUiState.Error(error.message ?: "Failed to send verification email")
                }
        }
    }

    fun reloadUser() {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            userRepository.reloadUser()
                .onSuccess {
                    _uiState.value = uiState.value.copyWithMessage("User reloaded successfully")
                }
                .onFailure { error ->
                    _uiState.value = AuthUiState.Error(error.message ?: "Failed to reload user")
                }
        }
    }
}

sealed class AuthUiState {
    object Initial : AuthUiState()
    object Loading : AuthUiState()
    object Unauthenticated : AuthUiState()
    data class Authenticated(val user: FirebaseUser, val message: String? = null) : AuthUiState()
    data class Error(val message: String) : AuthUiState()

    fun copyWithMessage(message: String?): AuthUiState {
        return when (this) {
            is Authenticated -> this.copy(message = message)
            else -> this
        }
    }
}