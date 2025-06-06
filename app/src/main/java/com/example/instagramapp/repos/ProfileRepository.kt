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
            if (checkIfFollowing(currentUserUid, targetUserUid)) {
                return Result.success(Unit)
            }

            firestore.runTransaction { transaction ->
                val currentUserRef = profilesCollection.document(currentUserUid)
                val targetUserRef = profilesCollection.document(targetUserUid)

                transaction.update(currentUserRef, "followingCount", FieldValue.increment(1))
                transaction.update(targetUserRef, "followersCount", FieldValue.increment(1))

                transaction.update(currentUserRef, "following", FieldValue.arrayUnion(targetUserUid))
                transaction.update(targetUserRef, "followers", FieldValue.arrayUnion(currentUserUid))
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendFollowRequest(currentUserUid: String, targetUserUid: String): Result<Unit> {
        return try {
            val requestData = hashMapOf(
                "fromUser" to currentUserUid,
                "toUser" to targetUserUid,
                "timestamp" to FieldValue.serverTimestamp(),
                "status" to "pending"
            )

            firestore.collection("follow_requests")
                .document("${currentUserUid}_$targetUserUid")
                .set(requestData)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFollowStatus(currentUserUid: String, targetUserUid: String): Boolean? {
        return try {
            if (checkIfFollowing(currentUserUid, targetUserUid)) {
                return true
            }

            val request = firestore.collection("follow_requests")
                .document("${currentUserUid}_$targetUserUid")
                .get()
                .await()

            if (request.exists() && request.getString("status") == "pending") {
                null
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun checkIfFollowing(currentUserUid: String, targetUserUid: String): Boolean {
        return try {
            val currentUser = profilesCollection.document(currentUserUid).get().await()
            val following = currentUser.get("following") as? List<*> ?: emptyList<Any>()
            following.contains(targetUserUid)
        } catch (e: Exception) {
            false
        }
    }

    suspend fun unfollowUser(currentUserUid: String, targetUserUid: String): Result<Unit> {
        return try {
            firestore.runTransaction { transaction ->
                val currentUserRef = profilesCollection.document(currentUserUid)
                val targetUserRef = profilesCollection.document(targetUserUid)

                transaction.update(currentUserRef, "followingCount", FieldValue.increment(-1))
                transaction.update(targetUserRef, "followersCount", FieldValue.increment(-1))

                transaction.update(currentUserRef, "following", FieldValue.arrayRemove(targetUserUid))
                transaction.update(targetUserRef, "followers", FieldValue.arrayRemove(currentUserUid))
            }.await()

            try {
                firestore.collection("follow_requests")
                    .document("${currentUserUid}_$targetUserUid")
                    .delete()
                    .await()
            } catch (e: Exception) {
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFollowerIds(userId: String): List<String> {
        return try {
            val snapshot = profilesCollection.document(userId).get().await()
            snapshot.get("followers") as? List<String> ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getFollowingIds(userId: String): List<String> {
        return try {
            val snapshot = profilesCollection.document(userId).get().await()
            snapshot.get("following") as? List<String> ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}