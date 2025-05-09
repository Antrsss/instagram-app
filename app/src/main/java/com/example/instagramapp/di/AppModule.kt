package com.example.instagramapp.di

import com.example.instagramapp.repos.CommentRepository
import com.example.instagramapp.repos.PostRepository
import com.example.instagramapp.repos.ProfileRepository
import com.example.instagramapp.repos.StoryRepository
import com.example.instagramapp.repos.UserRepository
import com.example.instagramapp.services.CloudinaryService
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
    fun provideFirestore() : FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideCommentRepository(firestore: FirebaseFirestore): CommentRepository {
        return CommentRepository(firestore)
    }

    @Provides
    @Singleton
    fun providePostRepository(firestore: FirebaseFirestore, cloudinaryService: CloudinaryService): PostRepository {
        return PostRepository(firestore, cloudinaryService)
    }

    @Provides
    @Singleton
    fun provideProfileRepository(firestore: FirebaseFirestore, cloudinaryService: CloudinaryService): ProfileRepository {
        return ProfileRepository(firestore, cloudinaryService)
    }

    @Provides
    @Singleton
    fun provideStoryRepository(firestore: FirebaseFirestore, cloudinaryService: CloudinaryService): StoryRepository {
        return StoryRepository(firestore, cloudinaryService)
    }

    @Provides
    @Singleton
    fun provideUserRepository(firebaseAuth: FirebaseAuth): UserRepository {
        return UserRepository(firebaseAuth)
    }
}