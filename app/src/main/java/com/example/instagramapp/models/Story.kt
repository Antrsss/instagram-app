package com.example.instagramapp.models

import java.util.Date
import java.util.UUID

data class Story(
    val authorUid: String,
    val creationTime: Date,
    val expirationTime: Date,
    val isVisible: Boolean,
    val photoUrl: String,
    val likes: Int = 0,
    val storyUuid: UUID = UUID.randomUUID(),
)