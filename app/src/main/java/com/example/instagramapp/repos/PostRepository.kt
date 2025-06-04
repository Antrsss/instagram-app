package com.example.instagramapp.repos

import android.content.Context
import android.net.Uri
import com.example.instagramapp.models.Comment
import com.example.instagramapp.models.Post
import com.example.instagramapp.services.CloudinaryService
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject

class PostRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val cloudinaryService: CloudinaryService,
    @ApplicationContext private val context: Context
) {
    private val postsCollection = firestore.collection("posts")
    private val commentsCollection = firestore.collection("comments")
    private val bookmarksCollection = firestore.collection("bookmarks")
    private val likesCollection = firestore.collection("likes")

    suspend fun createPost(post: Post): Result<Post> = withContext(Dispatchers.IO) {
        try {
            // Загружаем Uri изображений в Cloudinary
            val uploadedUrls = post.imageUris.map { uri ->
                cloudinaryService.uploadImage(uri)
            }

            val postToCreate = post.copy(
                imageUris = emptyList(), // Очищаем Uri, так как сохраняем только URLs
                imageUrls = uploadedUrls, // Сохраняем загруженные URL
                postUuid = UUID.randomUUID().toString(),
                creationTime = Date()
            )

            postsCollection.document(postToCreate.postUuid)
                .set(postToCreate)
                .await()

            Result.success(postToCreate)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadImageToCloudinary(uri: Uri): String = withContext(Dispatchers.IO) {
        try {
            cloudinaryService.uploadImage(uri)
        } catch (e: Exception) {
            throw Exception("Failed to upload image: ${e.message}")
        }
    }

    suspend fun getPost(postUuid: String): Result<Post> = withContext(Dispatchers.IO) {
        try {
            val snapshot = postsCollection.document(postUuid).get().await()
            val post = snapshot.toObject<Post>()
                ?: throw Exception("Post not found")

            Result.success(post)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserPosts(userUid: String): Result<List<Post>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = postsCollection
                .whereEqualTo("authorUid", userUid)
                .orderBy("creationTime", Query.Direction.DESCENDING)
                .get()
                .await()

            val posts = snapshot.documents.mapNotNull { it.toObject<Post>() }
            Result.success(posts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePost(postUuid: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val post = getPost(postUuid).getOrThrow()

            // Удаляем изображения из Cloudinary (используем imageUrls)
            post.imageUrls.forEach { imageUrl ->
                try {
                    cloudinaryService.deleteImage(imageUrl)
                } catch (e: Exception) {
                    println("Failed to delete image from Cloudinary: ${e.message}")
                }
            }

            postsCollection.document(postUuid).delete().await()
            deletePostRelatedData(postUuid)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun deletePostRelatedData(postUuid: String) {
        likesCollection.whereEqualTo("postUuid", postUuid)
            .get()
            .await()
            .documents
            .forEach { it.reference.delete().await() }

        commentsCollection.whereEqualTo("postUuid", postUuid)
            .get()
            .await()
            .documents
            .forEach { it.reference.delete().await() }

        bookmarksCollection.whereEqualTo("postUuid", postUuid)
            .get()
            .await()
            .documents
            .forEach { it.reference.delete().await() }
    }

    suspend fun likePost(postUuid: String, userId: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val postRef = postsCollection.document(postUuid)
            val likeDocRef = likesCollection.document("${postUuid}_$userId")

            firestore.runTransaction { transaction ->
                val postSnapshot = transaction.get(postRef)
                val post = postSnapshot.toObject<Post>() ?: throw Exception("Post not found")

                val likeSnapshot = transaction.get(likeDocRef)
                val newLikesCount = if (likeSnapshot.exists()) {
                    transaction.delete(likeDocRef)
                    post.likes - 1
                } else {
                    transaction.set(likeDocRef, mapOf(
                        "postUuid" to postUuid,
                        "userId" to userId,
                        "timestamp" to FieldValue.serverTimestamp()
                    ))
                    post.likes + 1
                }

                transaction.update(postRef, "likes", newLikesCount)
                newLikesCount
            }.await().let { Result.success(it) }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveToBookmarks(postUuid: String, userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            bookmarksCollection.document("${postUuid}_$userId")
                .set(mapOf(
                    "postUuid" to postUuid,
                    "userId" to userId,
                    "timestamp" to FieldValue.serverTimestamp()
                ))
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeFromBookmarks(postUuid: String, userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            bookmarksCollection.document("${postUuid}_$userId")
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addComment(postUuid: String, comment: Comment): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            commentsCollection.add(comment).await()

            postsCollection.document(postUuid)
                .update("commentsCount", FieldValue.increment(1))
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPostComments(postUuid: String): Result<List<Comment>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = commentsCollection
                .whereEqualTo("postUuid", postUuid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            val comments = snapshot.documents.mapNotNull { it.toObject<Comment>() }
            Result.success(comments)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}