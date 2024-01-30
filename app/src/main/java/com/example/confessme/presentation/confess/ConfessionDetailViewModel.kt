package com.example.confessme.presentation.confess

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.confessme.data.model.Bookmark
import com.example.confessme.data.model.Confession
import com.example.confessme.data.repository.ConfessionRepo
import com.example.confessme.presentation.utils.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConfessionDetailViewModel @Inject constructor(
    private val repository: ConfessionRepo
) : ViewModel() {

    private val _getConfessionState = MutableLiveData<UiState<Confession?>>()
    val getConfessionState: LiveData<UiState<Confession?>>
        get() = _getConfessionState

    private val _addFavoriteState = MutableLiveData<UiState<Confession?>>()
    val addFavoriteState: LiveData<UiState<Confession?>>
        get() = _addFavoriteState

    private val _deleteConfessionState = MutableLiveData<UiState<Confession?>>()
    val deleteConfessionState: LiveData<UiState<Confession?>>
        get() = _deleteConfessionState

    private val _onSwipeState = MutableLiveData<UiState<Confession?>>()
    val onSwipeState: LiveData<UiState<Confession?>>
        get() = _onSwipeState

    private val _addBookmarkState = MutableLiveData<UiState<Confession?>>()
    val addBookmarkState: LiveData<UiState<Confession?>>
        get() = _addBookmarkState

    private val _deleteBookmarkState = MutableLiveData<UiState<Bookmark?>>()
    val removeBookmarkState: LiveData<UiState<Bookmark?>>
        get() = _deleteBookmarkState

    fun getConfession(confessionId: String) {
        _getConfessionState.value = UiState.Loading
        viewModelScope.launch {
            repository.getConfession(confessionId) {result ->
                _getConfessionState.postValue(result)
            }
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

    fun addBookmark(confessionId: String, timestamp: com.google.firebase.Timestamp?, userUid: String) {
        _addBookmarkState.value = UiState.Loading
        repository.addBookmark(confessionId, timestamp, userUid) {
            _addBookmarkState.postValue(it)
        }
    }

    fun deleteBookmark(confessionId: String) {
        _deleteBookmarkState.value = UiState.Loading
        repository.removeBookmark(confessionId) { result ->
            _deleteBookmarkState.postValue(result)
        }
    }

    fun onSwiping(confessionId: String) {
        _onSwipeState.value = UiState.Loading
        repository.getConfession(confessionId) {result ->
            _onSwipeState.postValue(result)
        }
    }
}
