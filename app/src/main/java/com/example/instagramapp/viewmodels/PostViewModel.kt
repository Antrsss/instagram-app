package com.example.instagramapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.instagramapp.models.Post
import com.example.instagramapp.repos.PostRepository
import com.example.instagramapp.repos.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PostViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    private val _postState = MutableStateFlow(PostState())
    val postState: StateFlow<PostState> = _postState

    val currentUser = userRepository.currentUser

    fun loadPost(postId: String) {
        _postState.value = PostState(isLoading = true)
        viewModelScope.launch {
            val result = postRepository.getPost(postId)
            if (result.isSuccess) {
                _postState.value = PostState(post = result.getOrNull())
            } else {
                _postState.value = PostState(error = result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }

    fun toggleLike(postId: String) {
        val userId = userRepository.currentUser?.uid ?: return

        viewModelScope.launch {
            _postState.value.post?.let { currentPost ->
                // Оптимистичное обновление
                val newLikes = if (currentPost.likedByUser) currentPost.likes - 1 else currentPost.likes + 1
                _postState.value = _postState.value.copy(
                    post = currentPost.copy(
                        likes = newLikes,
                        likedByUser = !currentPost.likedByUser
                    )
                )

                try {
                    postRepository.toggleLike(postId, userId)
                } catch (e: Exception) {
                    // Откат при ошибке
                    _postState.value = _postState.value.copy(
                        post = currentPost.copy(
                            likes = currentPost.likes,
                            likedByUser = currentPost.likedByUser
                        )
                    )
                }
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            postRepository.deletePost(postId)
        }
    }

    fun updatePost(post: Post) {
        viewModelScope.launch {
            postRepository.updatePost(post)
        }
    }
}

data class PostState(
    val isLoading: Boolean = false,
    val post: Post? = null,
    val error: String? = null
)