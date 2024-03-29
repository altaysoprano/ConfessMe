package com.example.confessme.presentation.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.confessme.data.model.FollowUser
import com.example.confessme.data.model.User
import com.example.confessme.data.repository.UserRepo
import com.example.confessme.presentation.utils.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    private val _deleteAllHistory = MutableLiveData<UiState<Boolean>>()
    val deleteAllHistory: LiveData<UiState<Boolean>>
        get() = _deleteAllHistory

    private val _deleteHistoryItem = MutableLiveData<UiState<String>>()
    val deleteHistoryItem: LiveData<UiState<String>>
        get() = _deleteHistoryItem

    private val _followUserState = MutableLiveData<UiState<FollowUser>>()
    val followUserState: LiveData<UiState<FollowUser>>
        get() = _followUserState

    private val _searchViewFocused = MutableLiveData<Boolean>()
    val searchViewFocused: LiveData<Boolean>
        get() = _searchViewFocused

    private val _searchViewText = MutableLiveData<String>()
    val searchViewText: LiveData<String>
        get() = _searchViewText

    private var searchJob: Job? = null
    private var getHistoryJob: Job? = null
    private var deleteHistoryJob: Job? = null

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

    fun followOrUnfollowUser(userUid: String, userName: String, userToken: String, isFollowing: Boolean) {
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
                repository.followUser(userUid, userName, userToken) { followResult ->
                    if (followResult is UiState.Success) {
                        _followUserState.postValue(UiState.Success(followResult.data))
                    } else {
                        _followUserState.postValue(UiState.Failure(followResult.toString()))
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
        searchJob?.cancel()

        _historyResults.value = UiState.Loading
        getHistoryJob = viewModelScope.launch {
            repository.getSearchHistoryUsers(limit) {result ->
                _historyResults.postValue(result)
            }
        }
    }

    fun deleteAllHistory() {
        _deleteAllHistory.value = UiState.Loading
        deleteHistoryJob = viewModelScope.launch {
            repository.deleteSearchHistoryCollection {result ->
                _deleteAllHistory.postValue(result)
            }
        }
    }

    fun deleteHistoryItem(searchId: String) {
            _deleteHistoryItem.value = UiState.Loading
            deleteHistoryJob = viewModelScope.launch {
                repository.deleteSearchHistoryDocument(searchId) {result ->
                    _deleteHistoryItem.postValue(result)
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

    fun setSearchViewFocused(isFocused: Boolean) {
        _searchViewFocused.value = isFocused
    }

    fun setSearchViewText(text: String) {
        _searchViewText.value = text
    }
}
