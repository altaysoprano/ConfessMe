package com.example.confessme.data.repository

import android.net.Uri
import com.example.confessme.data.model.User
import com.example.confessme.util.UiState

interface Repository {

    fun signIn(email: String, pass: String, result: (UiState<String>) -> Unit)
    fun signUp(email: String, pass: String, confirmPass: String, result: (UiState<String>) -> Unit)
    fun updateProfile(
        previousUserName: String,
        userName: String,
        bio: String,
        imageUri: Uri,
        result: (UiState<String>) -> Unit
    )

    fun fetchUserProfile(result: (UiState<User?>) -> Unit)

    fun searchUsers(query: String, result: (UiState<List<User>>) -> Unit)
}