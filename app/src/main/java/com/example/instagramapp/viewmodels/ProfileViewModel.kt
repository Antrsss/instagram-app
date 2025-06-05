package com.example.instagramapp.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.instagramapp.models.Post
import com.example.instagramapp.models.Profile
import com.example.instagramapp.repos.PostRepository
import com.example.instagramapp.repos.ProfileRepository
import com.example.instagramapp.services.FirebaseStorageService
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
    private val postRepository: PostRepository,
    private val storageService: FirebaseStorageService
) : ViewModel() {

    private val _profileUiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Initial)
    val profileUiState: StateFlow<ProfileUiState> = _profileUiState.asStateFlow()

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    private val _isFollowing = MutableStateFlow<Boolean?>(null)
    val isFollowing: StateFlow<Boolean?> get() = _isFollowing

    fun loadProfile(userUid: String) {
        _profileUiState.value = ProfileUiState.Loading
        viewModelScope.launch {
            try {
                profileRepository.getProfile(userUid)
                    .onSuccess { profile ->
                        _profileUiState.value = ProfileUiState.Loaded(profile)
                    }
                    .onFailure { error ->
                        _profileUiState.value = ProfileUiState.Error(
                            error.message ?: "Failed to load profile"
                        )
                    }
            } catch (e: Exception) {
                _profileUiState.value = ProfileUiState.Error(
                    e.message ?: "Failed to load data"
                )
            }
        }
    }

    fun subscribeToUserPosts(userId: String) {
        viewModelScope.launch {
            postRepository.getUserPostsLive(userId)
                .collect { posts ->
                    _posts.value = posts
                }
        }
    }

    fun refreshPosts(userUid: String) {
        viewModelScope.launch {
            postRepository.getUserPosts(userUid)
                .onSuccess { posts ->
                    _posts.value = posts
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

    fun updateProfilePhoto(userUid: String, photoUri: Uri) {
        _profileUiState.value = ProfileUiState.Loading
        viewModelScope.launch {
            try {
                val photoUrl = storageService.uploadProfilePhoto(userUid, photoUri)

                profileRepository.updateProfilePhoto(userUid, photoUri)
                    .onSuccess {
                        val currentProfile = (_profileUiState.value as? ProfileUiState.Loaded)?.profile
                        currentProfile?.let {
                            _profileUiState.value = ProfileUiState.Loaded(
                                it.copy(photoUrl = photoUrl),
                                "Photo updated successfully"
                            )
                        }
                    }
                    .onFailure { error ->
                        _profileUiState.value = ProfileUiState.Error(
                            error.message ?: "Failed to update profile photo"
                        )
                    }
            } catch (e: Exception) {
                _profileUiState.value = ProfileUiState.Error(
                    e.message ?: "Failed to upload photo"
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

    fun followUser(currentUserUid: String, targetUserUid: String) {
        viewModelScope.launch {
            profileRepository.followUser(currentUserUid, targetUserUid)
                .onSuccess {
                    val current = (_profileUiState.value as? ProfileUiState.Loaded)?.profile
                    current?.let {
                        _profileUiState.value = ProfileUiState.Loaded(
                            it.copy(followersCount = (it.followersCount ?: 0) + 1),
                            "Followed successfully"
                        )
                    }
                }
                .onFailure { error ->
                    _profileUiState.value = ProfileUiState.Error(
                        error.message ?: "Failed to follow user"
                    )
                }
        }
    }

    fun checkIfUserIsFollowed(currentUserUid: String, targetUserUid: String) {
        viewModelScope.launch {
            val isFollowed = profileRepository.checkIfFollow(currentUserUid, targetUserUid)
            _isFollowing.value = isFollowed
        }
    }

    fun unfollow(currentUserUid: String, targetUserUid: String) {
        viewModelScope.launch {
            try {
                profileRepository.unfollowUser(currentUserUid, targetUserUid)
                _isFollowing.value = false
            } catch (e: Exception) {
                // Обработка ошибки при необходимости
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