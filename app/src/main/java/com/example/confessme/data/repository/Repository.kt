package com.example.confessme.data.repository

import android.net.Uri
import com.example.confessme.util.UiState

interface Repository {

    fun signIn(email: String, pass: String, result: (UiState<String>) -> Unit)
    fun signUp(email: String, pass: String, confirmPass: String, result: (UiState<String>) -> Unit)
    fun updateProfile(userName: String, bio: String, imageUri: Uri, result: (UiState<String>) -> Unit)
}