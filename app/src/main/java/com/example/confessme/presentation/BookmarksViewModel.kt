package com.example.confessme.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.confessme.data.model.Confession
import com.example.confessme.data.repository.ConfessionRepo
import com.example.confessme.util.UiState
import com.google.firebase.firestore.DocumentReference
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val repository: ConfessionRepo
) : ViewModel() {

    private val _fetchBookmarksState = MutableLiveData<UiState<List<Confession?>>>()
    val fetchBookmarksState: LiveData<UiState<List<Confession?>>>
        get() = _fetchBookmarksState

    private val _deleteBookmarkState = MutableLiveData<UiState<DocumentReference>>()
    val removeBookmarkState: LiveData<UiState<DocumentReference>>
        get() = _deleteBookmarkState

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