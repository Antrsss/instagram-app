package com.example.instagramapp.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.instagramapp.R
import com.example.instagramapp.models.Post
import com.example.instagramapp.models.Profile
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PostItem(
    post: Post,
    profile: Profile,
    onProfileClick: (String) -> Unit,
    onLikeClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Divider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.LightGray, CircleShape)
                        .clip(CircleShape)
                        .clickable { onProfileClick(profile.userUid) },
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
                                .padding(4.dp),
                            tint = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = profile.username,
                    modifier = Modifier.clickable { onProfileClick(profile.userUid) }
                )
            }
        }

        if (post.imageUrls.isNotEmpty()) {
            val pagerState = rememberPagerState(pageCount = { post.imageUrls.size })

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    AsyncImage(
                        model = post.imageUrls[page],
                        contentDescription = "Post image $page",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(R.drawable.ic_image_placeholder),
                        error = painterResource(R.drawable.ic_image_placeholder)
                    )
                }

                if (post.imageUrls.size > 1) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        repeat(post.imageUrls.size) { iteration ->
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row {
                Icon(
                    imageVector = if (post.likedByUser) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (post.likedByUser) Color.Red else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onLikeClick(post.postUuid) }
                )
            }
        }

        if (post.likes > 0) {
            Text(
                text = "Liked by ${post.likes} people",
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        Text(
            text = "${profile.username} ${post.description ?: ""}",
            modifier = Modifier.padding(8.dp)
        )
    }
}