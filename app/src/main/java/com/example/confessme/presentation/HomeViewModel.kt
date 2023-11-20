package com.example.confessme.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.confessme.data.model.Confession
import com.example.confessme.data.repository.ConfessionRepo
import com.example.confessme.presentation.ui.FragmentNavigation
import com.example.confessme.presentation.ui.LoginFragment
import com.example.confessme.util.ConfessionCategory
import com.example.confessme.util.UiState
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ConfessionRepo
) : ViewModel() {

    private var firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _fetchConfessionsState = MutableLiveData<UiState<List<Confession>>>()
    val fetchConfessionsState: LiveData<UiState<List<Confession>>>
        get() = _fetchConfessionsState

    private val _deleteConfessionState = MutableLiveData<UiState<Confession?>>()
    val deleteConfessionState: LiveData<UiState<Confession?>>
        get() = _deleteConfessionState

    private val _addFavoriteState = MutableLiveData<UiState<Confession?>>()
    val addFavoriteState: LiveData<UiState<Confession?>>
        get() = _addFavoriteState

    private val _addBookmarkState = MutableLiveData<UiState<String>>()
    val addBookmarkState: LiveData<UiState<String>>
        get() = _addBookmarkState

    private val _onPagingState = MutableLiveData<UiState<List<Confession>>>()
    val onPagingState: LiveData<UiState<List<Confession>>>
        get() = _onPagingState

    private val _onSwipeState = MutableLiveData<UiState<List<Confession>>>()
    val onSwipeState: LiveData<UiState<List<Confession>>>
        get() = _onSwipeState

    fun fetchConfessions(limit: Long) {
        _fetchConfessionsState.value = UiState.Loading
        repository.fetchFollowedUsersConfessions(limit) { result ->
            _fetchConfessionsState.postValue(result)
        }
    }

    fun addFavorite(favorited: Boolean, confessionId: String) {
        _addFavoriteState.value = UiState.Loading
        repository.addFavorite(favorited, confessionId) {
            _addFavoriteState.value = it
        }
    }

    fun deleteConfession(confessionId: String) {
        _deleteConfessionState.value = UiState.Loading
        repository.deleteConfession(confessionId) {
            _deleteConfessionState.postValue(it)
        }
    }

    fun addBookmark(confessionId: String, timestamp: String, userUid: String) {
        _addBookmarkState.value = UiState.Loading
        repository.addBookmark(confessionId, timestamp, userUid) {
            _addBookmarkState.postValue(it)
        }
    }

    fun onPaging(limit: Long) {
        _onPagingState.value = UiState.Loading
        repository.fetchFollowedUsersConfessions(limit) { result ->
            _onPagingState.postValue(result)
        }
    }

    fun onSwiping(limit: Long) {
        _onSwipeState.value = UiState.Loading
        repository.fetchFollowedUsersConfessions(limit) { result ->
            _onSwipeState.postValue(result)
        }
    }

    fun signOut() {
        firebaseAuth.signOut()
    }

}