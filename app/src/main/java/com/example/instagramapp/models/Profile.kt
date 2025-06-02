package com.example.instagramapp.models

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Profile(
    val userUid: String = "",
    val username: String = "",
    val name: String? = null,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val photoUrl: String? = null,
    val bio: String? = null,
    val website: String? = null,
    val isPrivate: Boolean = false
) {
    constructor() : this("", "")
}