package com.example.instagramapp.models

import android.net.Uri
import com.google.firebase.firestore.Exclude
import java.util.Date

data class Post(
    val authorUid: String = "",
    val description: String? = null,
    @Exclude val imageUris: List<Uri> = emptyList(),
    val imageUrls: List<String> = emptyList(),
    val likes: Int = 0,
    val postUuid: String = "",
    val creationTime: Date? = null
) {
    constructor() : this("", null, emptyList(), emptyList(), 0, "", null)

    @Exclude
    fun getImagesToDisplay(): List<Any> = when {
        imageUrls.isNotEmpty() -> imageUrls
        imageUris.isNotEmpty() -> imageUris
        else -> emptyList()
    }
}
