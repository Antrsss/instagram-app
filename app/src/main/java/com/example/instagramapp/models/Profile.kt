package com.example.instagramapp.models

data class Profile(
    val userUid: String,
    val name: String?,
    val username: String,
    val photoUrl: String? = null,
    val bio: String? = null,
    val website: String? = null,
    val isPrivate: Boolean = false,
)