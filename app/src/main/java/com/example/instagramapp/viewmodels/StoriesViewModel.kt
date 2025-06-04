package com.example.instagramapp.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.instagramapp.models.Story
import com.example.instagramapp.repos.StoryRepository
import com.example.instagramapp.repos.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class StoriesViewModel @Inject constructor(
    private val storyRepository: StoryRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<StoryUiState>(StoryUiState.Idle)
    val uiState: StateFlow<StoryUiState> = _uiState.asStateFlow()

    private val _selectedMedia = MutableStateFlow<Uri?>(null)
    val selectedMedia: StateFlow<Uri?> = _selectedMedia.asStateFlow()

    fun selectMedia(uri: Uri?) {
        _selectedMedia.value = uri
    }

    fun createStory() {
        viewModelScope.launch {
            _uiState.value = StoryUiState.Loading
            val currentUser = userRepository.currentUser?.uid ?: run {
                _uiState.value = StoryUiState.Error("User not authenticated")
                return@launch
            }

            val mediaUri = _selectedMedia.value ?: run {
                _uiState.value = StoryUiState.Error("No media selected")
                return@launch
            }

            val now = Date()
            val story = Story(
                authorUid = currentUser,
                creationTime = now,
                expirationTime = Date(now.time + 24 * 60 * 60 * 1000), // 24 hours
                isVisible = true,
                photoUrl = mediaUri.toString(),
                likes = 0
            )

            val result = runCatching {
                storyRepository.createStory(story)
            }

            result
                .onSuccess {
                    _uiState.value = StoryUiState.Success(it)
                    _selectedMedia.value = null
                }
                .onFailure {
                    _uiState.value = StoryUiState.Error(it.message ?: "Failed to create story")
                }

        }
    }

    sealed class StoryUiState {
        object Idle : StoryUiState()
        object Loading : StoryUiState()
        data class Success(val story: Story) : StoryUiState()
        data class Error(val message: String) : StoryUiState()
    }
}