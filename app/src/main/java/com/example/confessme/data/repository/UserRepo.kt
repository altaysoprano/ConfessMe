package com.example.confessme.data.repository

import android.net.Uri
import com.example.confessme.data.model.FollowUser
import com.example.confessme.data.model.User
import com.example.confessme.presentation.follow.FollowType
import com.example.confessme.presentation.profile.edit_set_profile.ProfilePhotoAction
import com.example.confessme.presentation.utils.UiState

interface UserRepo {

    fun updateProfile(
        previousUserName: String, previousImageUrl: String, userName: String, bio: String, imageUri: Uri,
        profilePhotoAction: ProfilePhotoAction, result: (UiState<String>) -> Unit
    )    fun fetchUserProfile(result: (UiState<User?>) -> Unit)
    fun fetchUserProfileByUid(userUid: String, result: (UiState<User?>) -> Unit)
    fun searchUsers(query: String, result: (UiState<List<User>>) -> Unit)
    fun getFollowersOrFollowing(userUid: String, limit: Long, followType: FollowType,
                                result: (UiState<List<User>>) -> Unit)
    fun followUser(
        userUidToFollow: String,
        userName: String,
        userToken: String,
        callback: (UiState<FollowUser>) -> Unit
    )
    fun checkIfUserFollowed(userUidToCheck: String, callback: (UiState<FollowUser>) -> Unit)
    fun unfollowUser(
        userUidToUnfollow: String,
        callback: (UiState<FollowUser>) -> Unit
    )
    fun addSearchToHistory(userUid: String)
    suspend fun getSearchHistoryUsers(limit: Long, result: (UiState<List<User>>) -> Unit)
    suspend fun deleteSearchHistoryCollection(result: (UiState<Boolean>) -> Unit)
    suspend fun deleteSearchHistoryDocument(documentIdToDelete: String, result: (UiState<String>) -> Unit)
}
