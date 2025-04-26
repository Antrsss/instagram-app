package com.example.instagramapp.repos

import com.example.instagramapp.models.Comment
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

class CommentRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val commentsCollection = firestore.collection("comments")

    suspend fun createComment(comment: Comment): Result<String> {
        return try {
            val documentRef = commentsCollection.add(comment).await()
            Result.success(documentRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPostComments(postUuid: UUID): Result<List<Comment>> {
        return try {
            val querySnapshot = commentsCollection
                .whereEqualTo("postUuid", postUuid.toString())
                .get()
                .await()

            val comments = querySnapshot.documents.mapNotNull {
                it.toObject(Comment::class.java)
            }
            Result.success(comments)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun editComment(commentId: String, newText: String): Result<Unit> {
        return try {
            commentsCollection.document(commentId)
                .update("text", newText)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteComment(commentId: String): Result<Unit> {
        return try {
            commentsCollection.document(commentId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun likeComment(commentId: String, userUid: String): Result<Unit> {
        return try {
            commentsCollection.document(commentId)
                .update("likes", FieldValue.arrayUnion(userUid))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

