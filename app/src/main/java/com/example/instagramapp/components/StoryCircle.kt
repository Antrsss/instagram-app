package com.example.instagramapp.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.instagramapp.R
import com.example.instagramapp.models.Profile

@Composable
fun StoryCircle(
    profile: Profile,
    hasUnseenStories: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
    ) {
        AsyncImage(
            model = profile.photoUrl ?: R.drawable.ic_profile_placeholder,
            contentDescription = "Profile photo",
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .clickable(onClick = onClick),
            contentScale = ContentScale.Crop
        )

        if (hasUnseenStories) {
            // Индикатор новых историй
        }
    }
}