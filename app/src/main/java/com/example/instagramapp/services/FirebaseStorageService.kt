package com.example.instagramapp.services

import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseStorageService @Inject constructor() {
    private val storage = FirebaseStorage.getInstance()
    private val storageRef = storage.reference

    // Для профильных фото
    private val profilePhotosRef = storageRef.child("profile_photos")

    // Для постов
    private val postsRef = storageRef.child("posts")

    // Для сторис
    private val storiesRef = storageRef.child("stories")

    suspend fun uploadProfilePhoto(userId: String, uri: Uri): String {
        val fileRef = profilePhotosRef.child("$userId/${UUID.randomUUID()}")
        fileRef.putFile(uri).await()
        return fileRef.downloadUrl.await().toString()
    }

    suspend fun deleteProfilePhoto(photoUrl: String) {
        val ref = storage.getReferenceFromUrl(photoUrl)
        ref.delete().await()
    }

    suspend fun uploadPostImages(userId: String, uris: List<Uri>): List<String> {
        return try {
            uris.map { uri ->
                val fileRef = storage.getReference("posts/$userId/${UUID.randomUUID()}")
                fileRef.putFile(uri).await()
                fileRef.downloadUrl.await().toString() // Возвращаем публичный URL
            }
        } catch (e: Exception) {
            Log.e("PostsVm", "Upload failed", e)
            emptyList()
        }
    }

    suspend fun deletePostImages(imageUrls: List<String>) {
        imageUrls.forEach { url ->
            try {
                val ref = storage.getReferenceFromUrl(url)
                ref.delete().await()
            } catch (e: Exception) {
                // Логируем ошибку, но не прерываем выполнение
                println("Failed to delete image: ${e.message}")
            }
        }
    }

    suspend fun uploadStory(userId: String, uri: Uri): String {
        val fileRef = storiesRef.child("$userId/${UUID.randomUUID()}")
        fileRef.putFile(uri).await()
        return fileRef.downloadUrl.await().toString()
    }

    suspend fun deleteStory(photoUrl: String) {
        val ref = storage.getReferenceFromUrl(photoUrl)
        ref.delete().await()
    }
}