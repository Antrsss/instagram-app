package com.example.instagramapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.instagramapp.components.PostItem
import com.example.instagramapp.components.StoryCircle
import com.example.instagramapp.models.Profile
import com.example.instagramapp.viewmodels.HomeViewModel
import com.example.instagramapp.models.Story
import com.example.instagramapp.navigation.Screen


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    homeViewModel: HomeViewModel = hiltViewModel(),
) {
    val state by homeViewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        homeViewModel.loadData()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Instagram") }) }
    ) { padding ->
        when {
            state.isLoading -> LoadingScreen(modifier = Modifier.padding(padding))
            state.error != null -> ErrorScreen(
                error = state.error,
                onRetry = { homeViewModel.refresh() },
                modifier = Modifier.padding(padding)
            )
            else -> ContentScreen(
                state = state,
                onStoryClick = { stories, startIndex ->
                    navController.currentBackStackEntry?.let { entry ->
                        entry.savedStateHandle.set("stories", stories)
                        navController.navigate(Screen.StoryViewer.createRoute(startIndex)) {
                            launchSingleTop = true
                        }
                    }
                },
                onProfileClick = { userId ->
                    navController.navigate(Screen.Profile.createRoute(userId))
                },
                onLikeClick = { postId ->
                    homeViewModel.likePost(postId)
                },
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun ContentScreen(
    state: HomeViewModel.HomeScreenState,
    onStoryClick: (List<Story>, Int) -> Unit,
    onProfileClick: (String) -> Unit,
    onLikeClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        item {
            StoriesRow(
                stories = state.stories,
                profiles = state.followingProfiles,
                onStoryClick = onStoryClick,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        items(state.posts) { post ->
            val profile = state.followingProfiles.find { it.userUid == post.authorUid } ?: return@items
            PostItem(
                post = post,
                profile = profile,
                onProfileClick = onProfileClick,
                onLikeClick = onLikeClick,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}

@Composable
fun StoriesRow(
    stories: List<Story>,
    profiles: List<Profile>,
    onStoryClick: (List<Story>, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val storiesByAuthor = stories.groupBy { it.authorUid }

    if (profiles.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No stories available")
        }
    } else {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            profiles.forEach { profile ->
                val userStories = storiesByAuthor[profile.userUid] ?: emptyList()
                StoryCircle(
                    profile = profile,
                    hasUnseenStories = userStories.isNotEmpty(),
                    onClick = {
                        if (userStories.isNotEmpty()) {
                            val allStories = stories.sortedBy { it.creationTime }
                            val startIndex = allStories.indexOfFirst { it.authorUid == profile.userUid }
                            onStoryClick(allStories, startIndex)
                        }
                    },
                    modifier = Modifier.size(80.dp)
                )
            }
        }
    }
}

@Composable
fun LoadingScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorScreen(
    error: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(error ?: "Unknown error")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Try again")
        }
    }
}