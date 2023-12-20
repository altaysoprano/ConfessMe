package com.example.confessme.di

import android.content.Context
import com.example.confessme.data.repository.AuthRepo
import com.example.confessme.data.repository.AuthRepoImp
import com.example.confessme.data.repository.ConfessionRepo
import com.example.confessme.data.repository.ConfessionRepoImp
import com.example.confessme.data.repository.NotificationRepo
import com.example.confessme.data.repository.NotificationRepoImp
import com.example.confessme.data.repository.UserRepo
import com.example.confessme.data.repository.UserRepoImp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideConfessionRepo(
        firebaseAuth: FirebaseAuth,
        database: FirebaseFirestore,
        @ApplicationContext context: Context
    ): ConfessionRepo {
        return ConfessionRepoImp(firebaseAuth, database, context)
    }

    @Provides
    @Singleton
    fun provideNotificationRepo(
        firebaseAuth: FirebaseAuth,
        database: FirebaseFirestore
    ) : NotificationRepo {
        return NotificationRepoImp(firebaseAuth, database)
    }

    @Provides
    @Singleton
    fun provideAuthRepo(
        firebaseAuth: FirebaseAuth,
        database: FirebaseFirestore,
        @ApplicationContext context: Context
    ) : AuthRepo {
        return AuthRepoImp(firebaseAuth, database, context)
    }

    @Provides
    @Singleton
    fun provideUserRepo(
        firebaseAuth: FirebaseAuth,
        database: FirebaseFirestore,
        storage: FirebaseStorage
    ) : UserRepo {
        return UserRepoImp(firebaseAuth, database, storage)
    }

}