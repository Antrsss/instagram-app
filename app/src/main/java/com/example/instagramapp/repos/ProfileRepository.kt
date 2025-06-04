package com.example.instagramapp.repos

import android.net.Uri
import com.example.instagramapp.models.Profile
import com.example.instagramapp.services.CloudinaryService
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ProfileRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val cloudinaryService: CloudinaryService
) {
    private val profilesCollection = firestore.collection("profiles")

    suspend fun createProfile(profile: Profile): Result<Unit> {
        return try {
            profilesCollection.document(profile.userUid).set(profile).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProfile(userUid: String): Result<Profile> {
        return try {
            val snapshot = profilesCollection.document(userUid).get().await()
            val profile = snapshot.toObject(Profile::class.java)
            if (profile != null) Result.success(profile)
            else Result.failure(Exception("Profile not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun editProfile(profile: Profile): Result<Unit> {
        return try {
            profilesCollection.document(profile.userUid).update(
                "name", profile.name,
                "username", profile.username,
                "bio", profile.bio,
                "website", profile.website,
                "isPrivate", profile.isPrivate
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfilePhoto(userUid: String, photoUri: Uri): Result<String> {
        return try {
            val photoUrl = cloudinaryService.uploadImage(photoUri)
            profilesCollection.document(userUid).update("photoUrl", photoUrl).await()

            Result.success(photoUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProfilePhoto(userUid: String): Result<Unit> {
        return try {
            val profile = getProfile(userUid).getOrThrow()

            profile.photoUrl?.let { photoUrl ->
                cloudinaryService.deleteImage(photoUrl)
            }
            profilesCollection.document(userUid).update("photoUrl", null).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProfile(userUid: String): Result<Unit> {
        return try {
            deleteProfilePhoto(userUid)
            profilesCollection.document(userUid).delete().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkUsernameAvailability(username: String): Result<Boolean> {
        return try {
            val query = profilesCollection.whereEqualTo("username", username).get().await()
            Result.success(query.isEmpty)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}