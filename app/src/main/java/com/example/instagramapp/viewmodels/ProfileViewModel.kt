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

    suspend fun updateProfilePhoto(userUid: String, photoUri: Uri): Boolean {
        return try {
            _profileUiState.value = ProfileUiState.Loading
            val photoUrl = storageService.uploadProfilePhoto(userUid, photoUri)
            profileRepository.updateProfilePhoto(userUid, photoUri).getOrThrow()
            loadProfile(userUid) // Перезагружаем профиль
            true // Успех
        } catch (e: Exception) {
            _profileUiState.value = ProfileUiState.Error(e.message ?: "Failed to update photo")
            false // Ошибка
        } finally {
            _profileUiState.value = _profileUiState.value.copyWithMessage(null)
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
            // 1. Получаем профиль цели
            val targetProfile = profileRepository.getProfile(targetUserUid).getOrNull()

            // 2. Проверяем, является ли профиль приватным
            if (targetProfile?.isPrivate == true) {
                // Для приватного профиля - отправляем запрос
                profileRepository.sendFollowRequest(currentUserUid, targetUserUid)
                    .onSuccess {
                        _isFollowing.value = null // Состояние "запрос отправлен"
                        _profileUiState.value = _profileUiState.value.copyWithMessage("Follow request sent")
                    }
                    .onFailure { e ->
                        _profileUiState.value = ProfileUiState.Error(e.message ?: "Failed to send request")
                    }
            } else {
                // Для публичного профиля - подписываемся сразу
                profileRepository.followUser(currentUserUid, targetUserUid)
                    .onSuccess {
                        _isFollowing.value = true
                        loadProfile(targetUserUid) // Обновляем данные профиля
                        _profileUiState.value = _profileUiState.value.copyWithMessage("Followed successfully")
                    }
                    .onFailure { e ->
                        _profileUiState.value = ProfileUiState.Error(e.message ?: "Failed to follow")
                    }
            }
        }
    }

    fun checkFollowStatus(currentUserUid: String, targetUserUid: String) {
        viewModelScope.launch {
            _isFollowing.value = profileRepository.getFollowStatus(currentUserUid, targetUserUid)
        }
    }

    fun unfollow(currentUserUid: String, targetUserUid: String) {
        viewModelScope.launch {
            profileRepository.unfollowUser(currentUserUid, targetUserUid)
                .onSuccess {
                    _isFollowing.value = false
                    loadProfile(targetUserUid)
                    _profileUiState.value = _profileUiState.value.copyWithMessage("Unfollowed successfully")
                }
                .onFailure { e ->
                    _profileUiState.value = ProfileUiState.Error(e.message ?: "Failed to unfollow")
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