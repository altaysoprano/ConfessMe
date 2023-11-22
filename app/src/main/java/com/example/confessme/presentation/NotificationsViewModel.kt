package com.example.confessme.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.confessme.R
import com.example.confessme.data.model.Confession
import com.example.confessme.data.model.Notification
import com.example.confessme.data.repository.AuthRepo
import com.example.confessme.data.repository.ConfessionRepo
import com.example.confessme.data.repository.NotificationRepo
import com.example.confessme.presentation.ui.FragmentNavigation
import com.example.confessme.presentation.ui.HomeFragment
import com.example.confessme.presentation.ui.LoginFragment
import com.example.confessme.util.UiState
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val authRepo: AuthRepo,
    private val notificationRepo: NotificationRepo
) : ViewModel() {

    private val _signOutState = MutableLiveData<UiState<String>>()
    val signOutState: LiveData<UiState<String>>
        get() = _signOutState

    private val _fetchNotificationsState = MutableLiveData<UiState<List<Notification>>>()
    val fetchNotificationsState: LiveData<UiState<List<Notification>>>
        get() = _fetchNotificationsState

    private val _onPagingState = MutableLiveData<UiState<List<Notification>>>()
    val onPagingState: LiveData<UiState<List<Notification>>>
        get() = _onPagingState

    private val _onSwipeState = MutableLiveData<UiState<List<Notification>>>()
    val onSwipeState: LiveData<UiState<List<Notification>>>
        get() = _onSwipeState

    fun signOut() {
        _signOutState.value = UiState.Loading
        authRepo.signOut {
            _signOutState.value = it
        }
    }

    fun fetchNotifications(limit: Long) {
        _fetchNotificationsState.value = UiState.Loading
        notificationRepo.fetchNotificationsForUser(limit) {
            _fetchNotificationsState.value = it
        }
    }

    fun onPaging(limit: Long) {
        _onPagingState.value = UiState.Loading
        notificationRepo.fetchNotificationsForUser(limit) { result ->
            _onPagingState.postValue(result)
        }
    }

    fun onSwiping(limit: Long) {
        _onSwipeState.value = UiState.Loading
        notificationRepo.fetchNotificationsForUser(limit) { result ->
            _onSwipeState.postValue(result)
        }
    }
}