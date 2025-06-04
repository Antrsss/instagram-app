package com.example.instagramapp.di

import android.content.Context
import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
import com.example.instagramapp.services.CloudinaryService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CloudinaryModule {

    @Provides
    @Singleton
    fun provideCloudinary(): Cloudinary {
        /*val config = mapOf(
            "cloud_name" to "dgkym15ev",
            "api_key" to "333425918551866",
            "api_secret" to "cEGHL0RGJwkARN5GcJQf5BzWBWk",
            "secure" to true
        )
        return Cloudinary(config)*/
        return Cloudinary(
            ObjectUtils.asMap(
                "cloud_name", "dgkym15ev",
                "api_key", "333425918551866",
                "api_secret", "cEGHL0RGJwkARN5GcJQf5BzWBWk",
                "secure", true
        ))
    }

    @Provides
    @Singleton
    fun provideCloudinaryService(
        cloudinary: Cloudinary,
        @ApplicationContext context: Context
    ): CloudinaryService {
        return CloudinaryService(cloudinary, context)
    }
}