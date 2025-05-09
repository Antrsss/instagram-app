package com.example.instagramapp.services

import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CloudinaryService @Inject constructor(private val cloudinary: Cloudinary) {

    suspend fun uploadImage(imageUri: String): String = withContext(Dispatchers.IO) {
        try {
            val uploadResult = cloudinary.uploader().upload(
                imageUri,
                ObjectUtils.emptyMap()
            )
            uploadResult["secure_url"] as String
        } catch (e: Exception) {
            throw CloudinaryUploadException("Failed to upload image", e)
        }
    }

    suspend fun deleteImage(imageUrl: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val publicId = extractPublicIdFromUrl(imageUrl)

            if (publicId.isNullOrEmpty()) {
                throw IllegalArgumentException("Invalid Cloudinary URL")
            }

            val deleteResult = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap())
            "ok" == deleteResult["result"] as String
        } catch (e: Exception) {
            throw CloudinaryDeleteException("Failed to delete image", e)
        }
    }

    private fun extractPublicIdFromUrl(url: String): String? {
        // Пример URL: https://res.cloudinary.com/demo/image/upload/v1234567/sample.jpg
        val pattern = Regex("upload/(?:v\\d+/)?(.+?)(?:\\.[^.]+)?$")
        return pattern.find(url)?.groupValues?.get(1)
    }
}

class CloudinaryUploadException(message: String, cause: Throwable?) : Exception(message, cause)
class CloudinaryDeleteException(message: String, cause: Throwable?) : Exception(message, cause)