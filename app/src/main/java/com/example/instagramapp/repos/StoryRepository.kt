package com.example.instagramapp.repos

import com.example.instagramapp.models.Story
import com.example.instagramapp.services.CloudinaryService
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.*
import javax.inject.Inject

class StoryRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val cloudinaryService: CloudinaryService
) {
    private val storiesCollection = firestore.collection("stories")

    suspend fun createStory(story: Story): Story {
        try {
            storiesCollection
                .document(story.storyUuid.toString())
                .set(story)
                .await()
            return story
        } catch (e: Exception) {
            throw Exception("Failed to create story", e)
        }
    }

    suspend fun getActiveStories(userUid: String): List<Story> {
        return try {
            val now = Date()
            storiesCollection
                .whereEqualTo("authorUid", userUid)
                .whereEqualTo("isVisible", true)
                .whereGreaterThan("expirationTime", now)
                .get()
                .await()
                .toObjects(Story::class.java)
        } catch (e: Exception) {
            throw Exception("Failed to get active stories", e)
        }
    }

    suspend fun getArchivedStories(userUid: String): List<Story> {
        return try {
            val now = Date()
            storiesCollection
                .whereEqualTo("authorUid", userUid)
                .whereLessThan("expirationTime", now)
                .get()
                .await()
                .toObjects(Story::class.java)
        } catch (e: Exception) {
            throw Exception("Failed to get archived stories", e)
        }
    }

    suspend fun deleteStory(storyUuid: UUID) {
        try {
            val document = storiesCollection
                .document(storyUuid.toString())
                .get()
                .await()

            val story = document.toObject(Story::class.java)
                ?: throw Exception("Story not found")

            cloudinaryService.deleteImage(story.photoUrl)

            storiesCollection
                .document(storyUuid.toString())
                .delete()
                .await()
        } catch (e: Exception) {
            throw Exception("Failed to delete story", e)
        }
    }

    suspend fun likeStory(storyUuid: UUID) {
        try {
            val storyRef = storiesCollection.document(storyUuid.toString())
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(storyRef)
                val currentLikes = snapshot.getLong("likes") ?: 0
                transaction.update(storyRef, "likes", currentLikes + 1)
            }.await()
        } catch (e: Exception) {
            throw Exception("Failed to like story", e)
        }
    }
}