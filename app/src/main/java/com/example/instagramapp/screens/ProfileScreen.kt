package com.example.instagramapp.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.instagramapp.models.Post
import com.example.instagramapp.models.Profile
import com.example.instagramapp.utils.RegisterImagePicker
import com.example.instagramapp.utils.rememberImagePicker
import com.example.instagramapp.viewmodels.ProfileUiState
import com.example.instagramapp.viewmodels.ProfileViewModel
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    userId: String,
    navController: NavController,
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by profileViewModel.profileUiState.collectAsState()
    val posts by profileViewModel.posts.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Posts", "Tagged")

    var showEditDialog by remember { mutableStateOf(false) }
    var showImagePicker by remember { mutableStateOf(false) }
    val imagePicker = rememberImagePicker()

    RegisterImagePicker(
        imagePicker = imagePicker,
        onImagePicked = { uri ->
            uri?.let {
                profileViewModel.updateProfilePhoto(userId, it)
            }
        }
    )

    LaunchedEffect(userId) {
        profileViewModel.loadProfile(userId)
    }

    if (showImagePicker) {
        LaunchedEffect(showImagePicker) {
            imagePicker.pickImage { /* Этот callback теперь обрабатывается в RegisterImagePicker */ }
            showImagePicker = false
        }
    }

    Scaffold(
        topBar = {
            ProfileTopBar(
                username = (uiState as? ProfileUiState.Loaded)?.profile?.username ?: "",
                onBackClick = { navController.popBackStack() },
                onSettingsClick = { /* Открыть меню настроек */ }
            )
        }
    ) { paddingValues ->
        when (uiState) {
            ProfileUiState.Initial -> Unit
            ProfileUiState.Loading -> FullScreenLoader()
            is ProfileUiState.Loaded -> {
                val profile = (uiState as ProfileUiState.Loaded).profile
                val message = (uiState as ProfileUiState.Loaded).message

                message?.let {
                    scope.launch {
                        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                ) {
                    ProfileHeader(
                        profile = profile,
                        postCount = posts.size,
                        followerCount = profile.followersCount ?: 0,
                        followingCount = profile.followingCount ?: 0,
                        onEditProfileClick = { showEditDialog = true },
                        onPhotoClick = { showImagePicker = true },
                        isCurrentUser = true // или проверка что это текущий пользователь
                    )

                    ProfileBio(
                        name = profile.name,
                        bio = profile.bio,
                        website = profile.website
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    ProfileTabs(
                        selectedTabIndex = selectedTabIndex,
                        tabs = tabs,
                        onTabSelected = { selectedTabIndex = it }
                    )

                    when (selectedTabIndex) {
                        0 -> PostsGrid(posts = posts, navController = navController)
                        // 1 -> ReelsGrid()
                        // 2 -> TaggedGrid()
                        else -> PostsGrid(posts = posts, navController = navController)
                    }
                }
            }
            is ProfileUiState.Error -> ErrorState(
                message = (uiState as ProfileUiState.Error).message,
                onRetry = { profileViewModel.loadProfile(userId) }
            )
        }
    }

    if (showEditDialog) {
        (uiState as? ProfileUiState.Loaded)?.profile?.let { profile ->
            EditProfileDialog(
                profile = profile,
                onDismiss = { showEditDialog = false },
                onSave = { updatedProfile ->
                    profileViewModel.editProfile(updatedProfile)
                    showEditDialog = false
                },
                checkUsernameAvailability = { username ->
                    var isAvailable = false
                    profileViewModel.checkUsernameAvailability(username) { available ->
                        isAvailable = available
                    }
                    isAvailable
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileTopBar(
    username: String,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = username,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        actions = {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Settings"
                )
            }
        }
    )
}

@Composable
private fun ProfileHeader(
    profile: Profile,
    postCount: Int,
    followerCount: Int,
    followingCount: Int,
    onEditProfileClick: () -> Unit,
    onPhotoClick: () -> Unit,
    isCurrentUser: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .clickable(onClick = onPhotoClick)
                .background(Color.LightGray, CircleShape)
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
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile placeholder",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ProfileStatItem(count = postCount, label = "Posts")
            ProfileStatItem(count = followerCount, label = "Followers")
            ProfileStatItem(count = followingCount, label = "Following")
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Button(
            onClick = onEditProfileClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp),
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorScheme.surfaceVariant,
                contentColor = colorScheme.onSurface
            )
        ) {
            Text(
                text = if (isCurrentUser) "Edit Profile" else if (profile.isPrivate) "Request" else "Follow",
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun ProfileStatItem(count: Int, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = count.toString(),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Text(
            text = label,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun ProfileBio(
    name: String?,
    bio: String?,
    website: String?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        name?.let {
            Text(
                text = it,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        bio?.let {
            Text(
                text = it,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        website?.let {
            Text(
                text = it,
                color = Color.Blue,
                fontSize = 14.sp,
                modifier = Modifier.clickable { /* Открыть ссылку */ }
            )
        }
    }
}

@Composable
private fun ProfileTabs(
    selectedTabIndex: Int,
    tabs: List<String>,
    onTabSelected: (Int) -> Unit
) {
    TabRow(
        selectedTabIndex = selectedTabIndex,
        modifier = Modifier.fillMaxWidth(),
        indicator = { tabPositions ->
            TabRowDefaults.Indicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                height = 2.dp,
                color = colorScheme.primary
            )
        },
        divider = {
            Divider(
                color = colorScheme.surfaceVariant,
                thickness = 1.dp
            )
        }
    ) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = selectedTabIndex == index,
                onClick = { onTabSelected(index) },
                text = { Text(title) },
                selectedContentColor = colorScheme.primary,
                unselectedContentColor = colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}


@Composable
private fun FullScreenLoader() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            color = colorScheme.error,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Button(onClick = onRetry) {
            Text("Try Again")
        }
    }
}

@Composable
fun EditProfileDialog(
    profile: Profile,
    onDismiss: () -> Unit,
    onSave: (Profile) -> Unit,
    checkUsernameAvailability: (String) -> Boolean
) {
    var name by remember { mutableStateOf(profile.name ?: "") }
    var username by remember { mutableStateOf(profile.username) }
    var bio by remember { mutableStateOf(profile.bio ?: "") }
    var website by remember { mutableStateOf(profile.website ?: "") }
    var isPrivate by remember { mutableStateOf(profile.isPrivate) }
    var usernameError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        usernameError = null
                    },
                    label = { Text("Username") },
                    isError = usernameError != null,
                    modifier = Modifier.fillMaxWidth()
                )

                if (usernameError != null) {
                    Text(usernameError!!, color = Color.Red, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = website,
                    onValueChange = { website = it },
                    label = { Text("Website") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isPrivate,
                        onCheckedChange = { isPrivate = it }
                    )
                    Text("Private account", modifier = Modifier.clickable { isPrivate = !isPrivate })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (username.isBlank()) {
                        usernameError = "Username cannot be empty"
                        return@Button
                    }

                    if (username != profile.username && !checkUsernameAvailability(username)) {
                        usernameError = "Username is already taken"
                        return@Button
                    }

                    onSave(
                        profile.copy(
                            name = name.takeIf { it.isNotBlank() },
                            username = username,
                            bio = bio.takeIf { it.isNotBlank() },
                            website = website.takeIf { it.isNotBlank() },
                            isPrivate = isPrivate
                        )
                    )
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun PostsGrid(
    posts: List<Post>,
    navController: NavController
) {
    val context = LocalContext.current

    if (posts.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No posts yet",
                color = Color.Gray
            )
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(1.dp)
        ) {
            items(posts.size) { index -> // Используем posts.size и индекс
                val post = posts[index]
                val firstImageUrl = post.imageUris.firstOrNull()

                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .padding(1.dp)
                        .clickable {
                            navController.navigate("post/${post.postUuid}")
                        }
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(firstImageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Post image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}