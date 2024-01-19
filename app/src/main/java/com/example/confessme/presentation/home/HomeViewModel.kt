package com.example.confessme.presentation.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.confessme.data.model.Bookmark
import com.example.confessme.data.model.Confession
import com.example.confessme.data.model.Notification
import com.example.confessme.data.repository.AuthRepo
import com.example.confessme.data.repository.ConfessionRepo
import com.example.confessme.data.repository.NotificationRepo
import com.example.confessme.presentation.utils.UiState
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val confessRepo: ConfessionRepo,
    private val authRepo: AuthRepo,
    private val notificationRepo: NotificationRepo,
    ) : ViewModel() {

    private val _fetchConfessionsState = MutableLiveData<UiState<List<Confession>>>()
    val fetchConfessionsState: LiveData<UiState<List<Confession>>>
        get() = _fetchConfessionsState

    private val _deleteConfessionState = MutableLiveData<UiState<Confession?>>()
    val deleteConfessionState: LiveData<UiState<Confession?>>
        get() = _deleteConfessionState

    private val _addFavoriteState = MutableLiveData<UiState<Confession?>>()
    val addFavoriteState: LiveData<UiState<Confession?>>
        get() = _addFavoriteState

    private val _addBookmarkState = MutableLiveData<UiState<Confession?>>()
    val addBookmarkState: LiveData<UiState<Confession?>>
        get() = _addBookmarkState

    private val _deleteBookmarkState = MutableLiveData<UiState<Bookmark?>>()
    val removeBookmarkState: LiveData<UiState<Bookmark?>>
        get() = _deleteBookmarkState

    private val _onPagingState = MutableLiveData<UiState<List<Confession>>>()
    val onPagingState: LiveData<UiState<List<Confession>>>
        get() = _onPagingState

    private val _onSwipeState = MutableLiveData<UiState<List<Confession>>>()
    val onSwipeState: LiveData<UiState<List<Confession>>>
        get() = _onSwipeState

    private val _fetchNotificationsState = MutableLiveData<UiState<List<Notification>>>()
    val fetchNotificationsState: LiveData<UiState<List<Notification>>>
        get() = _fetchNotificationsState

    private val _signOutState = MutableLiveData<UiState<String>>()
    val signOutState: LiveData<UiState<String>>
        get() = _signOutState

    fun fetchConfessions(limit: Long) {
        _fetchConfessionsState.value = UiState.Loading
        confessRepo.fetchFollowedUsersConfessions(limit) { result ->
            _fetchConfessionsState.postValue(result)
        }
    }

    fun addFavorite(favorited: Boolean, confessionId: String) {
        _addFavoriteState.value = UiState.Loading
        confessRepo.addFavorite(favorited, confessionId) {
            _addFavoriteState.value = it
        }
    }

    fun deleteConfession(confessionId: String) {
        _deleteConfessionState.value = UiState.Loading
        confessRepo.deleteConfession(confessionId) {
            _deleteConfessionState.postValue(it)
        }
    }

    fun addBookmark(confessionId: String, timestamp: Timestamp?, userUid: String) {
        _addBookmarkState.value = UiState.Loading
        confessRepo.addBookmark(confessionId, timestamp, userUid) {
            _addBookmarkState.postValue(it)
        }
    }

    fun deleteBookmark(confessionId: String) {
        _deleteBookmarkState.value = UiState.Loading
        confessRepo.removeBookmark(confessionId) { result ->
            _deleteBookmarkState.postValue(result)
        }
    }

    fun onPaging(limit: Long) {
        _onPagingState.value = UiState.Loading
        confessRepo.fetchFollowedUsersConfessions(limit) { result ->
            _onPagingState.postValue(result)
        }
    }

    fun onSwiping(limit: Long) {
        _onSwipeState.value = UiState.Loading
        confessRepo.fetchFollowedUsersConfessions(limit) { result ->
            _onSwipeState.postValue(result)
        }
    }

    fun fetchNotifications(limit: Long) {
        _fetchNotificationsState.value = UiState.Loading
        notificationRepo.fetchNotificationsForUser(limit, false) {
            _fetchNotificationsState.value = it
        }
    }

    fun updateLanguage(language: String) {
        authRepo.updateLanguage(language)
    }

    fun signOut() {
        _signOutState.value = UiState.Loading
        authRepo.signOut {
            _signOutState.value = it
        }
    }

}