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
    fun fetchUserProfileByUid(userUid: String, result: (UiState<User?>) -> Unit)
    fun searchUsers(query: String, result: (UiState<List<User>>) -> Unit)
    fun getFollowingUsers(userUid: String, result: (UiState<List<User>>) -> Unit)
    fun getMyFollowingUsers(result: (UiState<List<User>>) -> Unit)
    fun followUser(userUidToFollow: String, callback: (UiState<String>) -> Unit)
    fun checkIfUserFollowed(userUidToCheck: String, callback: (UiState<Boolean>) -> Unit)
    fun unfollowUser(userUidToUnfollow: String, callback: (UiState<String>) -> Unit)

}