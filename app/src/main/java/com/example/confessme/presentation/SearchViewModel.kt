package com.example.confessme.presentation

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.confessme.data.model.FollowUser
import com.example.confessme.data.model.User
import com.example.confessme.data.repository.UserRepo
import com.example.confessme.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: UserRepo
) : ViewModel() {

    private val _searchResults = MutableLiveData<UiState<List<User>>>()
    val searchResults: LiveData<UiState<List<User>>>
        get() = _searchResults

    private val _historyResults = MutableLiveData<UiState<List<User>>>()
    val historyResults: LiveData<UiState<List<User>>>
        get() = _historyResults

    private val _followUserState = MutableLiveData<UiState<FollowUser>>()
    val followUserState: LiveData<UiState<FollowUser>>
        get() = _followUserState

    private var searchJob: Job? = null
    private var getHistoryJob: Job? = null

    fun searchUsers(query: String) {
        searchJob?.cancel()
        getHistoryJob?.cancel()

        _searchResults.value = UiState.Loading

        searchJob = viewModelScope.safeLaunch {
            delay(500)
            if (!query.isBlank()) {
                repository.searchUsers(query) { result ->
                    _searchResults.postValue(result)
                }
            } else {
                _searchResults.postValue(UiState.Success(emptyList()))
            }
        }
    }

    fun followOrUnfollowUser(userUid: String) {
        _followUserState.value = UiState.Loading

        repository.checkIfUserFollowed(userUid) { result ->
            if (result is UiState.Success && result.data.isFollowed) {
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

    fun addToSearchHistory(userUid: String) {
        viewModelScope.launch {
            repository.addSearchToHistory(userUid)
        }
    }

    fun getSearchHistoryUsers(limit: Long) {
        _historyResults.value = UiState.Loading
        getHistoryJob = viewModelScope.launch {
            repository.getSearchHistoryUsers(limit) {result ->
                _historyResults.postValue(result)
            }
        }
    }

    fun CoroutineScope.safeLaunch(block: suspend CoroutineScope.() -> Unit): Job {
        return this.launch {
            try {
                block()
            } catch (ce: CancellationException) {

            }
        }
    }
}
