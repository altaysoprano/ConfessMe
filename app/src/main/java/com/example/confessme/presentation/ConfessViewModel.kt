package com.example.confessme.presentation

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.confessme.data.model.Bookmark
import com.example.confessme.data.model.Confession
import com.example.confessme.data.repository.ConfessionRepo
import com.example.confessme.util.ConfessionCategory
import com.example.confessme.util.UiState
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConfessViewModel @Inject constructor(
    private val repository: ConfessionRepo
) : ViewModel() {

    private val _addConfessionState = MutableLiveData<UiState<String>>()
    val addConfessionState: LiveData<UiState<String>>
        get() = _addConfessionState

    private val _addAnswerState = MutableLiveData<UiState<Confession?>>()
    val addAnswerState: LiveData<UiState<Confession?>>
        get() = _addAnswerState

    private val _addFavoriteState = MutableLiveData<UiState<Confession?>>()
    val addFavoriteState: LiveData<UiState<Confession?>>
        get() = _addFavoriteState

    private val _addFavoriteAnswer = MutableLiveData<UiState<Confession?>>()
    val addFavoriteAnswer: LiveData<UiState<Confession?>>
        get() = _addFavoriteAnswer

    private val _fetchConfessionsState = MutableLiveData<UiState<List<Confession>>>()
    val fetchConfessionsState: LiveData<UiState<List<Confession>>>
        get() = _fetchConfessionsState

    private val _getConfessionState = MutableLiveData<UiState<Confession?>>()
    val getConfessionState: LiveData<UiState<Confession?>>
        get() = _getConfessionState

    private val _deleteAnswerState = MutableLiveData<UiState<Confession?>>()
    val deleteAnswerState: LiveData<UiState<Confession?>>
        get() = _deleteAnswerState

    private val _deleteConfessionState = MutableLiveData<UiState<Confession?>>()
    val deleteConfessionState: LiveData<UiState<Confession?>>
        get() = _deleteConfessionState

    private val _addBookmarkState = MutableLiveData<UiState<Confession?>>()
    val addBookmarkState: LiveData<UiState<Confession?>>
        get() = _addBookmarkState

    private val _deleteBookmarkState = MutableLiveData<UiState<Confession?>>()
    val removeBookmarkState: LiveData<UiState<Confession?>>
        get() = _deleteBookmarkState

    private var addFavoriteAnswerJob: Job? = null
    private var addConfessionJob: Job? = null

    fun addConfession(userUid: String, confessionText: String, isAnonymous: Boolean) {
        addConfessionJob?.cancel()
        _addConfessionState.value = UiState.Loading
        addConfessionJob= viewModelScope.launch {
            repository.addConfession(userUid, confessionText, isAnonymous) {
                _addConfessionState.value = it
            }
        }
    }

    fun fetchConfessions(userUid: String, limit: Long, confessionCategory: ConfessionCategory) {
        _fetchConfessionsState.value = UiState.Loading
        viewModelScope.launch {
            repository.fetchConfessions(userUid, limit, confessionCategory) { result ->
                _fetchConfessionsState.postValue(result)
            }
        }
    }

    fun getConfession(confessionId: String) {
        _getConfessionState.value = UiState.Loading
        viewModelScope.launch {
            repository.getConfession(confessionId) {result ->
                _getConfessionState.postValue(result)
            }
        }
    }

    fun addAnswer(confessionId: String, answerText: String) {
        _addAnswerState.value = UiState.Loading
        repository.addAnswer(confessionId, answerText) {
            _addAnswerState.value = it
        }
    }

    fun addFavorite(favorited: Boolean, confessionId: String) {
        _addFavoriteState.value = UiState.Loading
        repository.addFavorite(favorited, confessionId) {
            _addFavoriteState.value = it
        }
    }

    fun addAnswerFavorite(isFavorited: Boolean, confessionId: String) {
        addFavoriteAnswerJob = viewModelScope.launch {
            _addFavoriteAnswer.value = UiState.Loading
            repository.favoriteAnswer(isFavorited, confessionId) {
                _addFavoriteAnswer.value = it
            }
        }
    }

    fun deleteAnswer(confessionId: String) {
        _deleteAnswerState.value = UiState.Loading
        repository.deleteAnswer(confessionId) {
            _deleteAnswerState.value = it
        }
    }

    fun deleteConfession(confessionId: String) {
        _deleteConfessionState.value = UiState.Loading
        repository.deleteConfession(confessionId) {
            _deleteConfessionState.postValue(it)
        }
    }

    fun addBookmark(confessionId: String, timestamp: Timestamp, userUid: String) {
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
}
