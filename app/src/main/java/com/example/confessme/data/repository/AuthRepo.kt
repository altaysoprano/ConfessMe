package com.example.confessme.data.repository

import com.example.confessme.util.UiState

interface AuthRepo {
    fun signIn(email: String, pass: String, result: (UiState<String>) -> Unit)
    fun signUp(email: String, pass: String, confirmPass: String, result: (UiState<String>) -> Unit)
    fun updatePassword(previousPassword: String, newPassword: String, result: (UiState<String>) -> Unit)
}