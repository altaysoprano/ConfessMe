package com.example.confessme.presentation

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.confessme.data.model.FollowUser
import com.example.confessme.data.model.User
import com.example.confessme.data.repository.UserRepo
import com.example.confessme.util.FollowType
import com.example.confessme.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FollowsViewModel @Inject constructor(
    private val repository: UserRepo
) : ViewModel() {

    private val _followingUsers = MutableLiveData<UiState<List<User>>>()
    val followingUsers: LiveData<UiState<List<User>>>
        get() = _followingUsers

    private val _followUserState = MutableLiveData<UiState<FollowUser>>()
    val followUserState: LiveData<UiState<FollowUser>>
        get() = _followUserState

    private val _checkFollowingState = MutableLiveData<UiState<FollowUser>>()
    val checkFollowingState: LiveData<UiState<FollowUser>>
        get() = _checkFollowingState

    fun getFollowUsers(userUid: String, limit: Long, followType: FollowType) {
        _followingUsers.value = UiState.Loading

        viewModelScope.launch {
            repository.getFollowersOrFollowing(userUid, limit, followType) { result ->
                _followingUsers.postValue(result)
            }
        }
    }

    fun followOrUnfollowUser(userUid: String, isFollowing: Boolean) {
        _followUserState.value = UiState.Loading

            if (isFollowing) {
                repository.unfollowUser(userUid) { unfollowResult ->
                    if (unfollowResult is UiState.Success) {
                        _followUserState.postValue(unfollowResult)
                    } else {
                        _followUserState.postValue(UiState.Failure(unfollowResult.toString()))
                    }
                }
            } else {
                repository.followUser(userUid) { followResult ->
                    if (followResult is UiState.Success) {
                        _followUserState.postValue(UiState.Success(followResult.data))
                    } else {
                        _followUserState.postValue(UiState.Failure(followResult.toString()))
                    }
                }
            }
        }
}