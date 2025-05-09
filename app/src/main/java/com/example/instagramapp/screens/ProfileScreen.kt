package com.example.instagramapp.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
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
import com.example.instagramapp.models.Profile
import com.example.instagramapp.viewmodels.ProfileUiState
import com.example.instagramapp.viewmodels.ProfileViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ProfileScreen(
    userId: String,
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.profileUiState.collectAsState()
    //posts
    val selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Posts", "Reels", "Tagged")

    LaunchedEffect(userId) {
        viewModel.loadProfile(userId)
        //posts
    }

    Scaffold(
        topBar = {
            ProfileTopBar(
                username = (uiState as? ProfileUiState.Loaded)?.profile?.username ?: "",
                onSettingsClick = {  }
            )
        }
    ) { paddingValues ->
        when (uiState) {
            ProfileUiState.Initial -> Unit
            ProfileUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is ProfileUiState.Loaded -> {
                val profile = (uiState as ProfileUiState.Loaded).profile

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    ProfileHeader(
                        profile = profile,
                        postCount = 0,
                        onEditProfileClick = {  },
                        isCurrentUser = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ProfileBio(
                        name = profile.name,
                        bio = profile.bio,
                        website = profile.website
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    /*TabRow(
                        selectedTabIndex = selectedTabIndex,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = { Text(text = title) }
                            )
                        }
                    }*/

                   /* when (selectedTabIndex) {
                        0 -> PostsGrid(posts = posts, navController = navController)
                        // 1 -> ReelsGrid()
                        // 2 -> TaggedGrid()
                        else -> PostsGrid(posts = posts, navController = navController)
                    }*/
                }
            }
            is ProfileUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = (uiState as ProfileUiState.Error).message, color = Color.Red)
                }
            }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileTopBar(
    username: String,
    onSettingsClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(text = username, fontWeight = FontWeight.Bold)
        },
        actions = {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Settings")
            }
        }
    )
}

@Composable
private fun ProfileHeader(
    profile: Profile,
    postCount: Int,
    onEditProfileClick: () -> Unit,
    isCurrentUser: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .aspectRatio(1f)
                .border(
                    width = 2.dp,
                    color = if (profile.isPrivate) Color.Red else Color.LightGray,
                    shape = CircleShape
                )
                .padding(4.dp)
                .clip(CircleShape)
                .background(Color.LightGray)
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
                Image(
                    painter = painterResource(id = R.drawable.ic_profile_placeholder),
                    contentDescription = "Profile photo placeholder",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.width(20.dp))

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            //Profile stats (posts, followers, following
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Button(
                onClick = onEditProfileClick,
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp),
                shape = RoundedCornerShape(6.dp)) {
                Text(
                    text = if (isCurrentUser) "Edit Profile" else if (profile.isPrivate) "Request" else "Follow",
                    fontSize = 12.sp
                )
            }
        }
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
        Text(
            text = if (name != null) name else "",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        if (!bio.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = bio,
                fontSize = 14.sp
            )
        }

        if (!website.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = website,
                color = Color.Blue,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun PostsGrid(posts: List<Post>, navController: NavController) {

}

