package com.example.instagramapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.instagramapp.models.Profile
import com.example.instagramapp.repos.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _profileState = MutableStateFlow<ProfileUiState>(ProfileUiState.Initial)
    val profileState: StateFlow<ProfileUiState> = _profileState.asStateFlow()

    fun loadProfile(userUid: String) {
        _profileState.value = ProfileUiState.Loading
        viewModelScope.launch {
            profileRepository.getProfile(userUid)
                .onSuccess { profile ->
                    _profileState.value = ProfileUiState.Loaded(profile)
                }
                .onFailure { error ->
                    _profileState.value = ProfileUiState.Error(
                        error.message ?: "Failed to load profile"
                    )
                }
        }
    }

    fun createProfile(profile: Profile) {
        _profileState.value = ProfileUiState.Loading
        viewModelScope.launch {
            profileRepository.createProfile(profile)
                .onSuccess {
                    _profileState.value = ProfileUiState.Loaded(profile)
                }
                .onFailure { error ->
                    _profileState.value = ProfileUiState.Error(
                        error.message ?: "Failed to save profile"
                    )
                }
        }
    }
}

sealed class ProfileUiState {
    object Initial : ProfileUiState()
    object Loading : ProfileUiState()
    data class Loaded(val profile: Profile, val message: String? = null) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()

    fun copyWithMessage(message: String?) : ProfileUiState {
        return  when (this) {
            is Loaded -> this.copy(message = message)
            else -> this
        }
    }
}