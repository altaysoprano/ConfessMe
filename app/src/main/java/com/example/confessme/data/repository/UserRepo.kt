package com.example.confessme.data.repository

import android.net.Uri
import com.example.confessme.data.model.User
import com.example.confessme.util.UiState

interface UserRepo {

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

}