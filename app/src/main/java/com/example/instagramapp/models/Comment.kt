package com.example.instagramapp.models

import java.util.Date
import java.util.UUID

data class Comment(
    val authorUid: String,
    val authorUsername: String,
    val postUuid: UUID,
    val text: String,
    val creationTime: Date = Date(),
    val likes: List<String> = emptyList(),
    val id: String = ""
)
