package com.example.instagramapp.models

import android.net.Uri
import java.util.Date

data class Post(
    val authorUid: String,
    val description: String?,
    val imageUris: List<Uri> = emptyList(), // Для работы с локальными Uri
    val imageUrls: List<String> = emptyList(), // Для хранения URL в Cloudinary
    val likes: Int,
    val postUuid: String = "",
    val creationTime: Date? = null
) {
    fun getImagesToDisplay(): List<Any> = when {
        imageUrls.isNotEmpty() -> imageUrls
        imageUris.isNotEmpty() -> imageUris
        else -> emptyList()
    }
}
