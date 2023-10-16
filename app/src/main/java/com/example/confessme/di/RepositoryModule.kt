package com.example.confessme.di

import com.example.confessme.data.repository.AuthRepo
import com.example.confessme.data.repository.AuthRepoImp
import com.example.confessme.data.repository.ConfessionRepo
import com.example.confessme.data.repository.ConfessionRepoImp
import com.example.confessme.data.repository.UserRepo
import com.example.confessme.data.repository.UserRepoImp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideConfessionRepo(
        firebaseAuth: FirebaseAuth,
        database: FirebaseFirestore
    ) : ConfessionRepo {
        return ConfessionRepoImp(firebaseAuth, database)
    }

    @Provides
    @Singleton
    fun provideAuthRepo(
        firebaseAuth: FirebaseAuth,
        database: FirebaseFirestore
    ) : AuthRepo {
        return AuthRepoImp(firebaseAuth, database)
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