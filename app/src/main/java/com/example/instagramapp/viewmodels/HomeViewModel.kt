package com.example.instagramapp.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.instagramapp.models.Post
import com.example.instagramapp.models.Profile
import com.example.instagramapp.models.Story
import com.example.instagramapp.repos.PostRepository
import com.example.instagramapp.repos.ProfileRepository
import com.example.instagramapp.repos.StoryRepository
import com.example.instagramapp.repos.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val storyRepository: StoryRepository,
    private val profileRepository: ProfileRepository,
    private val userRepository: UserRepository,
    private val postRepository: PostRepository
) : ViewModel() {

    // Перенесем HomeScreenState внутрь класса ViewModel
    data class HomeScreenState(
        val stories: List<Story> = emptyList(),
        val followingProfiles: List<Profile> = emptyList(),
        val posts: List<Post> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null,
        val currentUserId: String? = null
    )

    private val _state = MutableStateFlow(HomeScreenState())
    val state: StateFlow<HomeScreenState> = _state.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        _state.update { currentState ->
            currentState.copy(isLoading = true, error = null)
        }

        viewModelScope.launch {
            try {
                val currentUserId = userRepository.currentUser?.uid ?: run {
                    _state.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            error = "User not authenticated"
                        )
                    }
                    return@launch
                }

                val currentProfile = profileRepository.getProfile(currentUserId).getOrThrow()
                val following = (currentProfile.following as? List<String>) ?: emptyList()

                val followingProfiles = following.mapNotNull { userId ->
                    runCatching {
                        profileRepository.getProfile(userId).getOrNull()
                    }.getOrNull()
                }

                val stories = loadStories(followingProfiles)
                val posts = loadPosts(following)

                _state.update { currentState ->
                    currentState.copy(
                        stories = stories,
                        followingProfiles = followingProfiles,
                        posts = posts.sortedByDescending { it.creationTime ?: Date(0) },
                        isLoading = false,
                        currentUserId = currentUserId
                    )
                }
            } catch (e: Exception) {
                Log.e("HomeVM", "Error loading data", e)
                _state.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load data"
                    )
                }
            }
        }
    }

    private suspend fun loadStories(followingProfiles: List<Profile>): List<Story> {
        val stories = mutableListOf<Story>()
        followingProfiles.forEach { profile ->
            runCatching {
                storyRepository.getActiveStories(profile.userUid)
            }.onSuccess { profileStories ->
                stories.addAll(profileStories)
            }.onFailure { e ->
                Log.e("HomeVM", "Error loading stories for ${profile.userUid}", e)
            }
        }
        return stories.sortedByDescending { it.creationTime }
    }

    private suspend fun loadPosts(followingUserIds: List<String>): List<Post> {
        return if (followingUserIds.isEmpty()) {
            emptyList()
        } else {
            try {
                postRepository.getPostsByUsers(followingUserIds).getOrThrow()
            } catch (e: Exception) {
                Log.e("HomeVM", "Error loading posts", e)
                emptyList()
            }
        }
    }

    fun likeStory(storyUuid: UUID) {
        viewModelScope.launch {
            try {
                storyRepository.likeStory(storyUuid)
                loadData()
            } catch (e: Exception) {
                _state.update { currentState ->
                    currentState.copy(error = e.message ?: "Failed to like story")
                }
            }
        }
    }

    fun likePost(postId: String) {
        viewModelScope.launch {
            try {
                val currentUserId = userRepository.currentUser?.uid ?: run {
                    _state.update { currentState ->
                        currentState.copy(error = "User not authenticated")
                    }
                    return@launch
                }

                val currentPosts = _state.value.posts
                val postIndex = currentPosts.indexOfFirst { it.postUuid == postId }
                if (postIndex == -1) return@launch

                val post = currentPosts[postIndex]
                val isLiked = post.likedByUser

                _state.update { currentState ->
                    val updatedPosts = currentState.posts.toMutableList().apply {
                        set(postIndex, post.copy(
                            likes = if (isLiked) post.likes - 1 else post.likes + 1,
                            likedByUser = !isLiked
                        ))
                    }
                    currentState.copy(posts = updatedPosts)
                }

                postRepository.likePost(postId, currentUserId)

            } catch (e: Exception) {
                Log.e("HomeVM", "Error liking post", e)
                _state.update { currentState ->
                    currentState.copy(error = e.message ?: "Failed to like post")
                }
                loadData()
            }
        }
    }

    fun refresh() {
        loadData()
    }
}