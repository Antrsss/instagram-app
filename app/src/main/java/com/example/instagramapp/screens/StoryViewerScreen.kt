package com.example.instagramapp.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.instagramapp.models.Story

@Composable
fun StoryViewerScreen(
    stories: List<Story>,
    startIndex: Int,
    onBack: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Text("Story Viewer: ${stories[startIndex].authorUid}")

        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Icon(Icons.Default.ArrowBack, "Back")
        }
    }
}