package com.example.confessme.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    fun getFollowUsers(userUid: String, followType: FollowType) {
        _followingUsers.value = UiState.Loading

        viewModelScope.launch {
            repository.getFollowersOrFollowing(userUid, followType) { result ->
                _followingUsers.postValue(result)
            }
        }
    }

}