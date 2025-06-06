package com.example.instagramapp.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import com.example.instagramapp.models.Profile

@Composable
fun StoryCircle(
    profile: Profile,
    hasUnseenStories: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // Градиентная обводка для непросмотренных историй
        if (hasUnseenStories) {
            // Здесь можно использовать Box с градиентной обводкой
        }

        Image(
            painter = rememberImagePainter(profile.photoUrl),
            contentDescription = "Profile photo",
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .clickable(onClick = onClick),
            contentScale = ContentScale.Crop
        )
    }
}