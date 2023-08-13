package com.example.confessme.di

import com.example.confessme.data.repository.Repository
import com.example.confessme.data.repository.RepositoryImp
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
    fun provideRepository(
        firebaseAuth: FirebaseAuth,
        database: FirebaseFirestore,
        storage: FirebaseStorage
    ) : Repository {
        return RepositoryImp(firebaseAuth, database, storage)
    }

}