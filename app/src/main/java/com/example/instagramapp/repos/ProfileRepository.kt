package com.example.instagramapp.repos

import com.example.instagramapp.models.Profile
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ProfileRepository @Inject constructor(
    private val firestore: FirebaseFirestore
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

    suspend fun editProfile(profile: Profile) {

    }

    suspend fun deleteProfile(profileUuid: String) {

    }
}