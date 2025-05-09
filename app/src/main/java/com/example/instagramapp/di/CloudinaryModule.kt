package com.example.instagramapp.di

import com.cloudinary.Cloudinary
import com.example.instagramapp.services.CloudinaryService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CloudinaryModule {

    @Provides
    @Singleton
    fun provideCloudinary(): Cloudinary {
        val config = mapOf(
            "cloud_name" to "dgkym15ev",
            "api_key" to "333425918551866",
            "api_secret" to "cEGHL0RGJwkARN5GcJQf5BzWBWk",
            "secure" to true
        )
        return Cloudinary(config)
    }

    @Provides
    @Singleton
    fun provideCloudinaryService(cloudinary: Cloudinary): CloudinaryService {
        return CloudinaryService(cloudinary)
    }
}