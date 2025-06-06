package com.example.instagramapp.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.instagramapp.R
import com.example.instagramapp.models.Post
import com.example.instagramapp.navigation.Screen
import com.example.instagramapp.utils.formatTimeAgo
import com.example.instagramapp.viewmodels.PostViewModel
import com.example.instagramapp.viewmodels.ProfileUiState
import com.example.instagramapp.viewmodels.ProfileViewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostScreen(
    postId: String,
    navController: NavController,
    postViewModel: PostViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    val postState by postViewModel.postState.collectAsState()
    val profileState by profileViewModel.profileUiState.collectAsState()
    val currentUser = postViewModel.currentUser

    var showOptionsMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    LaunchedEffect(postId) {
        postViewModel.loadPost(postId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Post") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (postState.post?.authorUid == currentUser?.uid) {
                        IconButton(onClick = { showOptionsMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Additional options"
                            )
                        }

                        DropdownMenu(
                            expanded = showOptionsMenu,
                            onDismissRequest = { showOptionsMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                onClick = {
                                    showOptionsMenu = false
                                    showEditDialog = true
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit"
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = {
                                    showOptionsMenu = false
                                    showDeleteDialog = true
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete"
                                    )
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            postState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Loading post...")
                }
            }
            postState.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Loading error: ${postState.error}")
                }
            }
            postState.post != null -> {
                val post = postState.post!!

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    PostHeader(
                        authorUid = post.authorUid,
                        profileState = profileState,
                        navController = navController,
                        postUuid = post.postUuid
                    )

                    PostImages(
                        images = post.getImagesToDisplay(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                    )

                    PostActions(
                        likesCount = post.likes,
                        isLiked = postState.post?.likedByUser ?: false,
                        onLikeClick = { postViewModel.toggleLike(postId) },
                        onCommentClick = { navController.navigate("comments/${postId}") },
                        onShareClick = { /* Поделиться */ }
                    )

                    PostDescription(
                        username = (profileState as? ProfileUiState.Loaded)?.profile?.username ?: "",
                        description = post.description ?: "",
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    PostTimeStamp(
                        timestamp = post.creationTime?.time ?: System.currentTimeMillis(),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    Divider()
                }
            }
        }
    }

    if (showDeleteDialog) {
        DeletePostDialog(
            onConfirm = {
                postViewModel.deletePost(postId)
                showDeleteDialog = false
                navController.popBackStack()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    if (showEditDialog && postState.post != null) {
        EditPostDialog(
            post = postState.post!!,
            onDismiss = { showEditDialog = false },
            onSave = { updatedPost ->
                postViewModel.updatePost(updatedPost)
                showEditDialog = false
            }
        )
    }
}

@Composable
fun PostHeader(
    authorUid: String,
    profileState: ProfileUiState,
    navController: NavController,
    postUuid: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (profileState) {
            is ProfileUiState.Loaded -> {
                val profile = profileState.profile
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable {
                            navController.navigate(Screen.Post.createRoute(postUuid))
                        }
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) {
                    if (profile.photoUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(profile.photoUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Profile photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_profile_placeholder),
                            contentDescription = "Profile photo",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = profile.username,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        navController.navigate("profile/${authorUid}")
                    }
                )
            }
            else -> {
                // Загрузка или заглушка
            }
        }
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun PostImages(
    images: List<Any>,
    modifier: Modifier = Modifier
) {
    if (images.isEmpty()) {
        Box(
            modifier = modifier
                .background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            Text("No images yet")
        }
    } else if (images.size == 1) {
        val image = images.first()
        if (image is String) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(image)
                    .crossfade(true)
                    .build(),
                contentDescription = "Post image",
                modifier = modifier,
                contentScale = ContentScale.Crop
            )
        } else if (image is Uri) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(image)
                    .crossfade(true)
                    .build(),
                contentDescription = "Post image",
                modifier = modifier,
                contentScale = ContentScale.Crop
            )
        }
    } else {
        val pagerState = rememberPagerState()

        Box(modifier = modifier) {
            HorizontalPager(
                count = images.size,
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val currentImage = images[page]
                when (currentImage) {
                    is String -> {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(currentImage)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Post image $page",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    is Uri -> {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(currentImage)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Post image $page",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            if (images.size > 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(images.size) { iteration ->
                        val color = if (pagerState.currentPage == iteration) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        }
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PostActions(
    likesCount: Int,
    isLiked: Boolean,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onLikeClick) {
                Icon(
                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (isLiked) Color.Red else MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "$likesCount",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun PostDescription(
    username: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(vertical = 4.dp)) {
        Text(
            text = username,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Text(
            text = description,
            fontSize = 14.sp
        )
    }
}

@Composable
fun PostTimeStamp(
    timestamp: Long,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        Text(
            text = formatTimeAgo(timestamp),
            color = Color.Gray,
            fontSize = 12.sp
        )
    }
}

@Composable
fun DeletePostDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete post") },
        text = { Text("Are you sure you want to delete this post? This action cannot be cancelled.") },
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text("Delete", color = Color.Red)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditPostDialog(
    post: Post,
    onDismiss: () -> Unit,
    onSave: (Post) -> Unit
) {
    var description by remember { mutableStateOf(post.description ?: "") }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit post") },
        text = {
            Column {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(post.copy(description = description))
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}