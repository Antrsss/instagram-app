package com.example.instagramapp.di

import android.content.Context
import com.example.instagramapp.repos.CommentRepository
import com.example.instagramapp.repos.PostRepository
import com.example.instagramapp.repos.ProfileRepository
import com.example.instagramapp.repos.StoryRepository
import com.example.instagramapp.repos.UserRepository
import com.example.instagramapp.services.FirebaseStorageService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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
    fun provideFirestore() : FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideUserRepository(firebaseAuth: FirebaseAuth): UserRepository {
        return UserRepository(firebaseAuth)
    }

    @Provides
    @Singleton
    fun provideCommentRepository(firestore: FirebaseFirestore): CommentRepository {
        return CommentRepository(firestore)
    }

    @Provides
    @Singleton
    fun provideFirebaseStorageService(): FirebaseStorageService {
        return FirebaseStorageService()
    }

    @Provides
    @Singleton
    fun providePostRepository(
        firestore: FirebaseFirestore,
        storageService: FirebaseStorageService,
        userRepository: UserRepository
    ): PostRepository {
        return PostRepository(firestore, storageService, userRepository)
    }

    @Provides
    @Singleton
    fun provideProfileRepository(
        firestore: FirebaseFirestore,
        storageService: FirebaseStorageService
    ): ProfileRepository {
        return ProfileRepository(firestore, storageService)
    }

    @Provides
    @Singleton
    fun provideStoryRepository(
        firestore: FirebaseFirestore,
        storageService: FirebaseStorageService
    ): StoryRepository {
        return StoryRepository(firestore, storageService)
    }
}