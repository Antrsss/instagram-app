package com.example.instagramapp.di

import android.content.Context
import com.cloudinary.Cloudinary
import com.cloudinary.android.MediaManager
import com.example.instagramapp.repos.ProfileRepository
import com.example.instagramapp.repos.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = Firebase.auth

    @Provides
    @Singleton
    fun provideUserRepository(firebaseAuth: FirebaseAuth): UserRepository {
        return UserRepository(firebaseAuth)
    }

    @Provides
    @Singleton
    fun provideFirestore() : FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideProfileRepository(firestore: FirebaseFirestore): ProfileRepository {
        return ProfileRepository(firestore)
    }

    @Provides
    @Singleton
    fun provideCloudinary(context: Context): Cloudinary {
        val config = mapOf(
            "cloud_name" to "dgkym15ev",
            "api_key" to "333425918551866",
            "api_secret" to "cEGHL0RGJwkARN5GcJQf5BzWBWk"
        )

        val cloudinary = Cloudinary(config)
        MediaManager.init(context, config) // для доступа через MediaManager, если используешь его тоже
        return cloudinary
    }
}