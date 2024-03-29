package com.example.confessme.presentation.profile.my_profile.bookmarks

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.confessme.data.model.Bookmark
import com.example.confessme.data.model.Confession
import com.example.confessme.data.repository.ConfessionRepo
import com.example.confessme.presentation.utils.UiState
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val repository: ConfessionRepo
) : ViewModel() {

    private val _fetchBookmarksState = MutableLiveData<UiState<List<Confession?>>>()
    val fetchBookmarksState: LiveData<UiState<List<Confession?>>>
        get() = _fetchBookmarksState

    private val _deleteBookmarkState = MutableLiveData<UiState<Bookmark?>>()
    val removeBookmarkState: LiveData<UiState<Bookmark?>>
        get() = _deleteBookmarkState

    private val _addBookmarkState = MutableLiveData<UiState<Confession?>>()
    val addBookmarkState: LiveData<UiState<Confession?>>
        get() = _addBookmarkState

    private val _addFavoriteState = MutableLiveData<UiState<Confession?>>()
    val addFavoriteState: LiveData<UiState<Confession?>>
        get() = _addFavoriteState

    private val _deleteConfessionState = MutableLiveData<UiState<Confession?>>()
    val deleteConfessionState: LiveData<UiState<Confession?>>
        get() = _deleteConfessionState

    fun fetchBookmarks(limit: Long) {
        _fetchBookmarksState.value = UiState.Loading
        repository.fetchBookmarks(limit) { result ->
            _fetchBookmarksState.postValue(result)
        }
    }

    fun addBookmark(confessionId: String, timestamp: Timestamp?, userUid: String) {
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

    fun deleteConfession(confessionId: String) {
        _deleteConfessionState.value = UiState.Loading
        repository.deleteConfession(confessionId) {
            _deleteConfessionState.postValue(it)
        }
    }

    fun addFavorite(favorited: Boolean, confessionId: String) {
        _addFavoriteState.value = UiState.Loading
        repository.addFavorite(favorited, confessionId) {
            _addFavoriteState.value = it
        }
    }
}