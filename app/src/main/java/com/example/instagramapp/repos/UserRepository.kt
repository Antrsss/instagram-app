package com.example.instagramapp.repos

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) {

    val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser

    private val _authState = MutableStateFlow(currentUser)
    val authState: Flow<FirebaseUser?> = _authState.asStateFlow()

    init {
        firebaseAuth.addAuthStateListener { firebaseAuth ->
            _authState.value = firebaseAuth.currentUser
        }
    }

    suspend fun signUp(email: String, password: String): Result<FirebaseUser> {
        return try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            Result.success(authResult.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            Result.success(authResult.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        firebaseAuth.signOut()
    }

    suspend fun sendEmailVerification(): Result<Unit> {
        return try {
            val user = currentUser ?: throw IllegalStateException("User not logged in")
            user.sendEmailVerification().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reloadUser(): Result<Unit> {
        val user = currentUser ?: return Result.failure(IllegalStateException("User not logged in"))

        return try {
            val task = user.reload()
            task.await()
            if (task.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(task.exception ?: RuntimeException("Reload failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}