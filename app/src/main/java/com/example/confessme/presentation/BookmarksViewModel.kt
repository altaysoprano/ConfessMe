package com.example.confessme.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.confessme.data.model.Confession
import com.example.confessme.data.repository.ConfessionRepo
import com.example.confessme.util.ConfessionCategory
import com.example.confessme.util.UiState
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val repository: ConfessionRepo
) : ViewModel() {

    private val _fetchBookmarksState = MutableLiveData<UiState<List<Confession?>>>()
    val fetchBookmarksState: LiveData<UiState<List<Confession?>>>
        get() = _fetchBookmarksState

    fun fetchBookmarks(limit: Long) {
        _fetchBookmarksState.value = UiState.Loading
        repository.fetchBookmarks(limit) { result ->
            _fetchBookmarksState.postValue(result)
        }
    }
}