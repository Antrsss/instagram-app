package com.example.instagramapp.services

import android.content.Context
import android.net.Uri
import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

class CloudinaryService @Inject constructor(
    private val cloudinary: Cloudinary,
    @ApplicationContext private val context: Context
) {
    suspend fun uploadImage(uri: Uri): String = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw IOException("Cannot open input stream")

            val params = mapOf(
                "folder" to "instagram_posts",
                "resource_type" to "image",
                "public_id" to "post_${System.currentTimeMillis()}"
            )

            val uploadResult = cloudinary.uploader().upload(inputStream, params)
            uploadResult["secure_url"] as? String
                ?: throw Exception("Cloudinary returned null URL")
        } catch (e: Exception) {
            throw Exception("Upload failed: ${e.message}")
        }
    }

    suspend fun deleteImage(imageUrl: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val publicId = extractPublicIdFromUrl(imageUrl)
                ?: throw IllegalArgumentException("Invalid Cloudinary URL")

            val deleteResult = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap())
            "ok" == deleteResult["result"] as? String
        } catch (e: Exception) {
            throw Exception("Failed to delete image: ${e.message}")
        }
    }

    private fun extractPublicIdFromUrl(url: String): String? {
        val pattern = Regex("upload/(?:v\\d+/)?(.+?)(?:\\.[^.]+)?$")
        return pattern.find(url)?.groupValues?.get(1)
    }
}

class CloudinaryUploadException(message: String, cause: Throwable?) : Exception(message, cause)
class CloudinaryDeleteException(message: String, cause: Throwable?) : Exception(message, cause)