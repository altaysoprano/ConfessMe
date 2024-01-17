package com.example.confessme.data.repository

import com.example.confessme.util.UiState
import com.google.android.gms.auth.api.signin.GoogleSignInAccount

interface AuthRepo {
    fun signIn(email: String, pass: String, result: (UiState<String>) -> Unit)
    fun signUp(email: String, pass: String, confirmPass: String, result: (UiState<String>) -> Unit)
    fun googleSignIn(idToken: String, googleSignInAccount: GoogleSignInAccount?, result: (UiState<String>) -> Unit)
    fun signOut(result: (UiState<String>) -> Unit)
    fun updatePassword(previousPassword: String, newPassword: String, result: (UiState<String>) -> Unit)
    fun updateLanguage(language: String)
    fun isGoogleSignIn(result: (UiState<Boolean>) -> Unit)
    fun deleteAccountWithConfessionsAndSignOut(currentPassword: String, googleSignInAccount: GoogleSignInAccount?,
                                               result: (UiState<String>) -> Unit)
}

