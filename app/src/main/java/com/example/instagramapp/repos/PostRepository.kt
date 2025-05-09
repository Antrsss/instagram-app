package com.example.instagramapp.repos

import com.example.instagramapp.models.Comment
import com.google.firebase.firestore.FirebaseFirestore
import com.example.instagramapp.models.Post
import com.example.instagramapp.services.CloudinaryService
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.Dispatchers
import java.util.UUID
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject

class PostRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val cloudinaryService: CloudinaryService
) {
    private val postsCollection = firestore.collection("posts")
    private val commentsCollection = firestore.collection("comments")
    private val bookmarksCollection = firestore.collection("bookmarks")
    private val likesCollection = firestore.collection("likes")

    suspend fun createPost(post: Post): Result<Post> = withContext(Dispatchers.IO) {
        try {
            val uploadedImageUrls = post.imageUrls.map { url ->
                cloudinaryService.uploadImage(url)
            }

            val postToCreate = post.copy(
                imageUrls = uploadedImageUrls,
                postUuid = UUID.randomUUID(),
                creationTime = Date()
            )

            postsCollection.document(postToCreate.postUuid.toString())
                .set(postToCreate)
                .await()

            Result.success(postToCreate)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPost(postUuid: UUID): Result<Post> = withContext(Dispatchers.IO) {
        try {
            val snapshot = postsCollection.document(postUuid.toString()).get().await()
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

    suspend fun deletePost(postUuid: UUID): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val post = getPost(postUuid).getOrThrow()

            post.imageUrls.forEach { imageUrl ->
                cloudinaryService.deleteImage(imageUrl)
            }

            postsCollection.document(postUuid.toString()).delete().await()

            deletePostRelatedData(postUuid)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    public suspend fun deletePostRelatedData(postUuid: UUID) {
        likesCollection.whereEqualTo("postUuid", postUuid.toString())
            .get()
            .await()
            .documents
            .forEach { it.reference.delete().await() }

        commentsCollection.whereEqualTo("postUuid", postUuid.toString())
            .get()
            .await()
            .documents
            .forEach { it.reference.delete().await() }

        bookmarksCollection.whereEqualTo("postUuid", postUuid.toString())
            .get()
            .await()
            .documents
            .forEach { it.reference.delete().await() }
    }

    suspend fun likePost(postUuid: UUID, userId: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val postRef = postsCollection.document(postUuid.toString())
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
                        "postUuid" to postUuid.toString(),
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

    suspend fun saveToBookmarks(postUuid: UUID, userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            bookmarksCollection.document("${postUuid}_$userId")
                .set(mapOf(
                    "postUuid" to postUuid.toString(),
                    "userId" to userId,
                    "timestamp" to FieldValue.serverTimestamp()
                ))
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeFromBookmarks(postUuid: UUID, userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            bookmarksCollection.document("${postUuid}_$userId")
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addComment(postUuid: UUID, comment: Comment): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            commentsCollection.add(comment).await()

            postsCollection.document(postUuid.toString())
                .update("commentsCount", FieldValue.increment(1))
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPostComments(postUuid: UUID): Result<List<Comment>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = commentsCollection
                .whereEqualTo("postUuid", postUuid.toString())
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