package com.example.instagramapp.viewmodels

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.instagramapp.models.Post
import com.example.instagramapp.repos.PostRepository
import com.example.instagramapp.repos.UserRepository
import com.example.instagramapp.services.FirebaseStorageService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class PostsViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    private val storageService: FirebaseStorageService
) : ViewModel() {
    private val _currentUser = userRepository.currentUser

    private val _uiState = MutableStateFlow<PostUiState>(PostUiState.Idle)
    val uiState: StateFlow<PostUiState> = _uiState.asStateFlow()

    private val _selectedImages = MutableStateFlow<List<Uri>>(emptyList())
    val selectedImages: StateFlow<List<Uri>> = _selectedImages.asStateFlow()

    private val _userPosts = MutableStateFlow<List<Post>>(emptyList())
    val userPosts: StateFlow<List<Post>> = _userPosts.asStateFlow()

    fun loadCurrentUserPosts() {
        viewModelScope.launch {
            _currentUser?.uid?.let { userId ->
                postRepository.getUserPosts(userId).onSuccess { posts ->
                    _userPosts.value = posts
                }
            }
        }
    }

    suspend fun loadUserPosts(userId: String) {
        withContext(Dispatchers.IO) {
            postRepository.getUserPosts(userId)
                .onSuccess { posts ->
                    Log.d("PostsVM", "Loaded ${posts.size} posts for user $userId")
                    posts.forEach { post ->
                        Log.d("PostsVM", "Post: $post")
                    }
                    _userPosts.value = posts
                }
                .onFailure { e ->
                    Log.e("PostsVM", "Error loading posts for $userId", e)
                    _userPosts.value = emptyList()
                }
        }
    }

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

                // Проверка: есть ли выбранные изображения?
                if (selectedImages.value.isEmpty()) {
                    _uiState.value = PostUiState.Error("No images selected")
                    return@launch
                }

                // Логируем URI перед загрузкой
                Log.d("PostsVM", "Uploading URIs: ${selectedImages.value}")

                // 1. Загружаем изображения в Storage
                val imageUrls = storageService.uploadPostImages(
                    userId = currentUser,
                    uris = selectedImages.value
                )

                // Проверка: получили ли URL?
                if (imageUrls.isEmpty()) {
                    _uiState.value = PostUiState.Error("Failed to upload images (empty URLs)")
                    return@launch
                }
                Log.d("PostsVM", "Uploaded URLs: $imageUrls")

                // 2. Создаем пост
                val post = Post(
                    authorUid = currentUser,
                    description = description,
                    imageUrls = imageUrls,
                    imageUris = emptyList(),
                    likes = 0,
                    postUuid = UUID.randomUUID().toString(),
                    creationTime = Date()
                )

                // 3. Сохраняем в Firestore
                postRepository.createPost(post)
                    .onSuccess {
                        _selectedImages.value = emptyList()
                        loadCurrentUserPosts()
                        _uiState.value = PostUiState.Success(it)
                    }
                    .onFailure { e ->
                        _uiState.value = PostUiState.Error("Firestore error: ${e.message}")
                    }
            } catch (e: Exception) {
                Log.e("CreatePost", "Error", e)
                _uiState.value = PostUiState.Error("Failed: ${e.localizedMessage}")
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