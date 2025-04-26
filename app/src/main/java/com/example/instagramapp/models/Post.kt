package com.example.instagramapp.models

import android.graphics.Bitmap
import java.util.Date
import java.util.UUID

data class Post(
    val authorUid: String,
    val creationTime: Date,
    val images: List<Bitmap>,
    val description: String,
    val likes: Int = 0,
    val postUuid: UUID = UUID.randomUUID(),
)