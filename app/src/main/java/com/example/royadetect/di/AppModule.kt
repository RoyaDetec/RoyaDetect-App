package com.example.royadetect.di

import android.content.Context
import com.example.royadetect.repository.AuthRepository
import com.example.royadetect.utils.SessionManager
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
    fun provideSessionManager(@ApplicationContext context: Context): SessionManager {
        return SessionManager(context)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(sessionManager: SessionManager): AuthRepository {
        return AuthRepository(sessionManager)
    }
}