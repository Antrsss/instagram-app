package com.example.instagramapp.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.instagramapp.models.Post
import com.example.instagramapp.repos.PostRepository
import com.example.instagramapp.repos.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class PostsViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    private val _currentUser = userRepository.currentUser

    private val _uiState = MutableStateFlow<PostUiState>(PostUiState.Idle)
    val uiState: StateFlow<PostUiState> = _uiState.asStateFlow()

    private val _selectedImages = MutableStateFlow<List<Uri>>(emptyList())
    val selectedImages: StateFlow<List<Uri>> = _selectedImages.asStateFlow()

    fun selectImages(uris: List<Uri>) {
        _selectedImages.value = uris
    }

    fun createPost(description: String?) {
        viewModelScope.launch {
            _uiState.value = PostUiState.Loading
            try {
                val currentUser = userRepository.currentUser?.uid ?: run {
                    _uiState.value = PostUiState.Error("User not authenticated")
                    return@launch
                }

                val post = Post(
                    authorUid = currentUser,
                    description = description,
                    imageUris = _selectedImages.value,
                    likes = 0,
                    postUuid = UUID.randomUUID().toString()
                )

                postRepository.createPost(post)
                    .onSuccess {
                        _uiState.value = PostUiState.Success(it)
                        _selectedImages.value = emptyList()
                    }
                    .onFailure {
                        _uiState.value = PostUiState.Error(
                            it.message ?: "Failed to create post"
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = PostUiState.Error(
                    "Error: ${e.message ?: "Unknown error"}"
                )
            }
        }
    }

    sealed class PostUiState {
        object Idle : PostUiState()
        object Loading : PostUiState()
        data class Success(val post: Post) : PostUiState()
        data class Error(val message: String) : PostUiState()
    }
}