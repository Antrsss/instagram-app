package com.example.instagramapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.instagramapp.models.Post
import com.example.instagramapp.models.Profile
import com.example.instagramapp.repos.PostRepository
import com.example.instagramapp.repos.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val postRepository: PostRepository
) : ViewModel() {

    private val _profileUiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Initial)
    val profileUiState: StateFlow<ProfileUiState> = _profileUiState.asStateFlow()

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts = _posts.asStateFlow()

    fun loadProfile(userUid: String) {
        _profileUiState.value = ProfileUiState.Loading
        viewModelScope.launch {
            try {
                val profileDeferred = async { profileRepository.getProfile(userUid) }
                val postsDeferred = async { postRepository.getUserPosts(userUid) }

                val profileResult = profileDeferred.await()
                val postsResult = postsDeferred.await()

                profileResult.onSuccess { profile ->
                    _profileUiState.value = ProfileUiState.Loaded(profile)
                }.onFailure { error ->
                    _profileUiState.value = ProfileUiState.Error(
                        error.message ?: "Failed to load profile"
                    )
                }

                postsResult.onSuccess { posts ->
                    _posts.value = posts
                }
            } catch (e: Exception) {
                _profileUiState.value = ProfileUiState.Error(
                    e.message ?: "Failed to load data"
                )
            }
        }
    }

    fun createProfile(profile: Profile) {
        _profileUiState.value = ProfileUiState.Loading
        viewModelScope.launch {
            profileRepository.createProfile(profile)
                .onSuccess {
                    _profileUiState.value = ProfileUiState.Loaded(profile)
                }
                .onFailure { error ->
                    _profileUiState.value = ProfileUiState.Error(
                        error.message ?: "Failed to save profile"
                    )
                }
        }
    }

    fun updateProfilePhoto(userUid: String, imageUri: String) {
        _profileUiState.value = ProfileUiState.Loading
        viewModelScope.launch {
            profileRepository.updateProfilePhoto(userUid, imageUri)
                .onSuccess { photoUrl ->
                    val currentProfile = (_profileUiState.value as? ProfileUiState.Loaded)?.profile
                    currentProfile?.let {
                        _profileUiState.value = ProfileUiState.Loaded(
                            it.copy(photoUrl = photoUrl),
                            "Photo updated"
                        )
                    }
                }
                .onFailure { error ->
                    _profileUiState.value = ProfileUiState.Error(
                        error.message ?: "Failed to update photo"
                    )
                }
        }
    }

    fun refreshData(userUid: String) {
        loadProfile(userUid)
    }

    fun editProfile(profile: Profile) {
        _profileUiState.value = ProfileUiState.Loading
        viewModelScope.launch {
            profileRepository.editProfile(profile)
                .onSuccess {
                    _profileUiState.value = ProfileUiState.Loaded(profile, "Profile updated")
                }
                .onFailure { error ->
                    _profileUiState.value = ProfileUiState.Error(
                        error.message ?: "Failed to update profile"
                    )
                }
        }
    }

    fun deleteProfilePhoto(userUid: String) {
        _profileUiState.value = ProfileUiState.Loading
        viewModelScope.launch {
            profileRepository.deleteProfile(userUid)
                .onSuccess {
                    val currentProfile =
                        (_profileUiState.value as? ProfileUiState.Loaded)?.profile
                    currentProfile?.let {
                        _profileUiState.value = ProfileUiState.Loaded(
                            it.copy(photoUrl = null),
                            "Photo removed"
                        )
                    }
                }
                .onFailure { error ->
                    _profileUiState.value = ProfileUiState.Error(
                        error.message ?: "Failed to remove photo"
                    )
                }
        }
    }

    fun checkUsernameAvailability(username: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            profileRepository.checkUsernameAvailability(username)
                .onSuccess { isAvailable ->
                    onResult(isAvailable)
                }
                .onFailure {
                    onResult(false)
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