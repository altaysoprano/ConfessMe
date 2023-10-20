package com.example.confessme.presentation

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.confessme.data.model.Confession
import com.example.confessme.data.repository.ConfessionRepo
import com.example.confessme.util.ConfessionCategory
import com.example.confessme.util.UiState
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConfessViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
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

    private val _deleteAnswerState = MutableLiveData<UiState<Confession?>>()
    val deleteAnswerState: LiveData<UiState<Confession?>>
        get() = _deleteAnswerState

    private val _deleteConfessionState = MutableLiveData<UiState<Confession?>>()
    val deleteConfessionState: LiveData<UiState<Confession?>>
        get() = _deleteConfessionState

    private var addFavoriteAnswerJob: Job? = null

    fun addConfession(userUid: String, confessionText: String) {
        _addConfessionState.value = UiState.Loading
        repository.addConfession(userUid, confessionText) {
            _addConfessionState.value = it
        }
    }

    fun fetchConfessions(userUid: String, limit: Long, confessionCategory: ConfessionCategory) {
        _fetchConfessionsState.value = UiState.Loading
        repository.fetchConfessions(userUid, limit, confessionCategory) { result ->
            _fetchConfessionsState.postValue(result)
        }
    }

    fun addAnswer(confessionId: String, answerText: String) {
        _addAnswerState.value = UiState.Loading
        repository.addAnswer(confessionId, answerText) {
            _addAnswerState.value = it
        }
    }

    fun addFavorite(confessionId: String) {
        _addFavoriteState.value = UiState.Loading
        repository.addFavorite(confessionId) {
            _addFavoriteState.value = it
        }
    }

    fun addAnswerFavorite(isFavorited: Boolean, confessionId: String) {
        _addFavoriteAnswer.value = UiState.Loading

        addFavoriteAnswerJob?.cancel()

        addFavoriteAnswerJob = viewModelScope.launch {
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
}
