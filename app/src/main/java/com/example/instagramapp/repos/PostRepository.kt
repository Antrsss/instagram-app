package com.example.instagramapp.repos

import android.util.Log
import com.example.instagramapp.models.Comment
import com.example.instagramapp.models.Post
import com.example.instagramapp.services.FirebaseStorageService
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject

class PostRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storageService: FirebaseStorageService,
    private val userRepository: UserRepository
) {
    private val postsCollection = firestore.collection("posts")
    private val commentsCollection = firestore.collection("comments")
    private val bookmarksCollection = firestore.collection("bookmarks")
    private val likesCollection = firestore.collection("likes")

    suspend fun createPost(post: Post): Result<Post> = withContext(Dispatchers.IO) {
        try {
            if (post.imageUrls.isEmpty()) {
                throw IllegalArgumentException("Post must have at least one image URL")
            }

            val postData = hashMapOf(
                "authorUid" to post.authorUid,
                "description" to post.description,
                "imageUrls" to post.imageUrls, // Важно: сохраняем URLs!
                "likes" to post.likes,
                "likedBy" to emptyList<String>(),
                "postUuid" to post.postUuid,
                "creationTime" to post.creationTime
            )

            postsCollection.document(post.postUuid)
                .set(postData)
                .await()

            Log.d("PostRepository", "Post saved with URLs: ${post.imageUrls}")
            Result.success(post.copy(likes = 0, likedByUser = false))
        } catch (e: Exception) {
            Log.e("PostRepository", "Error saving post", e)
            Result.failure(e)
        }
    }

    suspend fun getPost(postUuid: String): Result<Post> = withContext(Dispatchers.IO) {
        try {
            val snapshot = postsCollection.document(postUuid).get().await()
            val likedBy = snapshot.get("likedBy") as? List<String> ?: emptyList()
            val isLiked = userRepository.currentUser?.uid?.let { likedBy.contains(it) } ?: false

            val post = snapshot.toObject<Post>()
                ?.copy(
                    postUuid = snapshot.id,
                    likedByUser = isLiked
                )
                ?: throw Exception("Post not found")

            Result.success(post)
        } catch (e: Exception) {
            Log.e("PostRepository", "Error getting post $postUuid", e)
            Result.failure(e)
        }
    }

    fun getUserPostsLive(userId: String): Flow<List<Post>> = postsCollection
        .whereEqualTo("authorUid", userId)
        .orderBy("creationTime", Query.Direction.DESCENDING)
        .snapshots()
        .map { snapshot ->
            snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(Post::class.java)?.copy(postUuid = doc.id)
                } catch (e: Exception) {
                    Log.e("PostRepository", "Error converting document to Post", e)
                    null
                }
            }
        }

    suspend fun getUserPosts(userId: String): Result<List<Post>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = postsCollection
                .whereEqualTo("authorUid", userId)
                .orderBy("creationTime", Query.Direction.DESCENDING)
                .get()
                .await()

            Log.d("PostRepository", "Found ${snapshot.documents.size} posts for user $userId")

            val posts = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(Post::class.java)?.copy(postUuid = doc.id).also {
                        Log.d("PostRepository", "Parsed post: ${it?.postUuid}")
                    }
                } catch (e: Exception) {
                    Log.e("PostRepository", "Error parsing post ${doc.id}", e)
                    null
                }
            }

            Result.success(posts)
        } catch (e: Exception) {
            Log.e("PostRepository", "Error getting posts for user $userId", e)
            Result.failure(e)
        }
    }

    suspend fun deletePost(postUuid: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val post = getPost(postUuid).getOrThrow()
            storageService.deletePostImages(post.imageUrls)
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

    suspend fun updatePost(updatedPost: Post): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val updateData = mutableMapOf<String, Any>().apply {
                updatedPost.description?.let { put("description", it) }
                put("imageUrls", updatedPost.imageUrls)
            }

            postsCollection.document(updatedPost.postUuid)
                .update(updateData)
                .await()

            Log.d("PostRepository", "Post ${updatedPost.postUuid} updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("PostRepository", "Error updating post ${updatedPost.postUuid}", e)
            Result.failure(e)
        }
    }

    suspend fun toggleLike(postId: String, userId: String) {
        val postRef = firestore.collection("posts").document(postId)
        val likeRef = likesCollection.document("${postId}_$userId")

        firestore.runTransaction { transaction ->
            val postSnapshot = transaction.get(postRef)
            val likeSnapshot = transaction.get(likeRef)

            val currentLikes = postSnapshot.getLong("likes") ?: 0
            val isLiked = likeSnapshot.exists()

            if (isLiked) {
                // Удаляем лайк, если он уже существует
                transaction.delete(likeRef)
                transaction.update(postRef, "likes", currentLikes - 1)
                transaction.update(postRef, "likedBy", FieldValue.arrayRemove(userId))
            } else {
                // Добавляем лайк, если его нет
                transaction.set(likeRef, mapOf(
                    "postId" to postId,
                    "userId" to userId,
                    "timestamp" to FieldValue.serverTimestamp()
                ))
                transaction.update(postRef, "likes", currentLikes + 1)
                transaction.update(postRef, "likedBy", FieldValue.arrayUnion(userId))
            }
        }.await()
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