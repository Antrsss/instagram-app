package com.example.instagramapp.repos

import android.graphics.Bitmap
import android.util.Base64
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.instagramapp.models.Post
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.util.*
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class PostRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val postsCollection = firestore.collection("posts")

    suspend fun createPost(post: Post): Result<Post> {
        return try {
            val imageUrls = post.images.map { bitmap ->
                val result = uploadToCloudinary(bitmap)
                result ?: throw Exception("Image upload failed")
            }

            val postMap = mapOf(
                "authorUid" to post.authorUid,
                "creationTime" to post.creationTime,
                "images" to imageUrls,
                "description" to post.description,
                "likes" to post.likes,
                "postUuid" to post.postUuid.toString()
            )

            postsCollection.document(post.postUuid.toString()).set(postMap).await()
            Result.success(post.copy(images = emptyList()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserPosts(userUid: String): Result<List<Post>> {
        return try {
            val snapshot = postsCollection
                .whereEqualTo("authorUid", userUid)
                .get().await()

            val posts = snapshot.documents.mapNotNull { doc ->
                val images = doc.get("images") as? List<String> ?: return@mapNotNull null
                Post(
                    authorUid = doc.getString("authorUid") ?: return@mapNotNull null,
                    creationTime = doc.getDate("creationTime") ?: Date(),
                    images = emptyList(), // ссылки можно загрузить при необходимости
                    description = doc.getString("description") ?: "",
                    likes = (doc.getLong("likes") ?: 0).toInt(),
                    postUuid = UUID.fromString(doc.getString("postUuid"))
                )
            }

            Result.success(posts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePost(postUuid: UUID): Result<Unit> {
        return try {
            postsCollection.document(postUuid.toString()).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun likePost(postUuid: UUID): Result<Unit> {
        return try {
            val ref = postsCollection.document(postUuid.toString())
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(ref)
                val currentLikes = snapshot.getLong("likes") ?: 0
                transaction.update(ref, "likes", currentLikes + 1)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveToBookmarks(postUuid: UUID) {
        // Зависит от реализации закладок
    }

    suspend fun commentPost(postUuid: UUID) {
        // Зависит от модели комментариев
    }

    private suspend fun uploadToCloudinary(bitmap: Bitmap): String? {
        val byteArray = bitmapToByteArray(bitmap)
        val encoded = Base64.encodeToString(byteArray, Base64.DEFAULT)
        val dataUri = "data:image/png;base64,$encoded"

        return suspendCancellableCoroutine { cont ->
            MediaManager.get().upload(dataUri)
                .unsigned("your_unsigned_preset") // Замени на свой preset
                .option("resource_type", "image")
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String?) {}
                    override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}
                    override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                        val secureUrl = resultData?.get("secure_url") as? String
                        cont.resume(secureUrl)
                    }

                    override fun onError(requestId: String?, error: ErrorInfo?) {
                        cont.resumeWithException(Exception(error.toString()))
                    }

                    override fun onReschedule(requestId: String?, error: ErrorInfo?) {
                        cont.resumeWithException(Exception("Upload rescheduled: $error"))
                    }
                })
                .dispatch()
        }
    }

    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }
}
