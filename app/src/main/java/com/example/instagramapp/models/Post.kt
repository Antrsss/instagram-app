package com.example.instagramapp.models

import java.util.Date
import java.util.UUID

data class Post(
    val authorUid: String,
    val creationTime: Date,
    val description: String,
    val imageUrls: List<String> = emptyList(),
    val likes: Int = 0,
    val postUuid: UUID = UUID.randomUUID(),
)