package com.example.confessme.data.repository

import android.net.Uri
import com.example.confessme.data.model.Confession
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
    fun fetchUserProfileByEmail(username: String, result: (UiState<User?>) -> Unit)
    fun searchUsers(query: String, result: (UiState<List<User>>) -> Unit)
    fun followUser(useremailToFollow: String, callback: (UiState<String>) -> Unit)
    fun checkIfUserFollowed(useremailToCheck: String, callback: (UiState<Boolean>) -> Unit)
    fun unfollowUser(useremailToUnfollow: String, callback: (UiState<String>) -> Unit)
    fun addConfession(userEmail: String, confessionText: String, result: (UiState<String>) -> Unit)
    fun fetchConfessions(
        limit: Long,
        isMyConfessions: Boolean,
        result: (UiState<List<Confession>>) -> Unit
    )

    fun addAnswer(
        confessionId: String,
        answerText: String,
        result: (UiState<String>) -> Unit
    )

    fun addFavorite(
        confessionId: String,
        callback: (UiState<Confession?>) -> Unit
    )

    fun favoriteAnswer(confessionId: String, result: (UiState<Confession?>) -> Unit)
    fun deleteAnswer(confessionId: String, result: (UiState<Confession?>) -> Unit)
    fun deleteConfession(confessionId: String, result: (UiState<Confession?>) -> Unit)
}