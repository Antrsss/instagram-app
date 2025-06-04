package com.example.instagramapp.repos

import android.net.Uri
import com.example.instagramapp.models.Profile
import com.example.instagramapp.services.FirebaseStorageService
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ProfileRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storageService: FirebaseStorageService
) {
    public val profilesCollection = firestore.collection("profiles")

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

    suspend fun deleteProfile(userUid: String): Result<Unit> {
        return try {
            deleteProfilePhoto(userUid)
            profilesCollection.document(userUid).delete().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfilePhoto(userUid: String, photoUri: Uri): Result<String> {
        return try {
            val photoUrl = storageService.uploadProfilePhoto(userUid, photoUri)
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
                storageService.deleteProfilePhoto(photoUrl)
            }
            profilesCollection.document(userUid).update("photoUrl", null).await()
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

    suspend fun searchProfiles(query: String): List<Profile> {
        return try {
            val snapshot = profilesCollection
                .orderBy("username")
                .startAt(query)
                .endAt("$query\uf8ff")
                .get()
                .await()

            snapshot.documents.mapNotNull { it.toObject(Profile::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun followUser(currentUserUid: String, targetUserUid: String): Result<Unit> {
        return try {
            val currentUserRef = firestore.collection("profiles").document(currentUserUid)
            val targetUserRef = firestore.collection("profiles").document(targetUserUid)

            firestore.runBatch { batch ->
                batch.update(currentUserRef, "followingCount", FieldValue.increment(1))
                batch.update(targetUserRef, "followersCount", FieldValue.increment(1))

                batch.update(currentUserRef, "following", FieldValue.arrayUnion(targetUserUid))
                batch.update(targetUserRef, "followers", FieldValue.arrayUnion(currentUserUid))
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkIfFollow(currentUserUid: String, targetUserUid: String): Boolean {
        return try {
            val doc = firestore.collection("users")
                .document(currentUserUid)
                .collection("following")
                .document(targetUserUid)
                .get()
                .await()

            doc.exists()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun unfollowUser(currentUserUid: String, targetUserUid: String) {
        try {
            val currentUserFollowingRef = firestore.collection("users")
                .document(currentUserUid)
                .collection("following")
                .document(targetUserUid)

            val targetUserFollowersRef = firestore.collection("users")
                .document(targetUserUid)
                .collection("followers")
                .document(currentUserUid)

            currentUserFollowingRef.delete().await()
            targetUserFollowersRef.delete().await()
        } catch (e: Exception) {
            throw e
        }
    }
}