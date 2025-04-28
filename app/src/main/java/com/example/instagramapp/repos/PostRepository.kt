package com.example.instagramapp.repos

import com.google.firebase.firestore.FirebaseFirestore
import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
import com.example.instagramapp.models.Post
import java.util.UUID
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class PostRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val cloudinary: Cloudinary
) {
    private val postsCollection = firestore.collection("posts")

    suspend fun createPost(post: Post) {
        val imageUrls = post.imageUrls.map { imageUrl ->
            uploadImageToCloudinary(imageUrl)
        }

        val postData = post.copy(imageUrls = imageUrls)
        postsCollection.document(post.postUuid.toString()).set(postData).await()
    }

    suspend fun getUserPosts(userUid: String): List<Post> {
        val snapshot = postsCollection
            .whereEqualTo("authorUid", userUid)
            .get()
            .await()

        return snapshot.documents.map { doc ->
            doc.toObject(Post::class.java) ?: throw Exception("Post not found")
        }
    }

    suspend fun deletePost(postUuid: UUID) {
        postsCollection.document(postUuid.toString()).delete().await()
    }

    suspend fun likePost(postUuid: UUID) {
        val postRef = postsCollection.document(postUuid.toString())
        val postSnapshot = postRef.get().await()
        val post = postSnapshot.toObject(Post::class.java)

        post?.let {
            val updatedPost = it.copy(likes = it.likes + 1)
            postRef.set(updatedPost).await()
        }
    }

    suspend fun saveToBookmarks(postUuid: UUID, userUid: String) {

    }

    suspend fun commentPost(postUuid: UUID, comment: String) {
        val commentData = mapOf(
            "comment" to comment,
            "postUuid" to postUuid.toString(),
            "timestamp" to System.currentTimeMillis()
        )
        firestore.collection("comments").add(commentData).await()
    }

    private fun uploadImageToCloudinary(imageUrl: String): String {
        val uploadResult = cloudinary.uploader().upload(imageUrl, ObjectUtils.emptyMap())
        return uploadResult["url"] as String
    }
}
