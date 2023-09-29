package com.example.confessme.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.confessme.data.model.Confession
import com.example.confessme.data.repository.Repository
import com.example.confessme.util.UiState
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ConfessViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val repository: Repository
) : ViewModel() {

    private val _addConfessionState = MutableLiveData<UiState<String>>()
    val addConfessionState: LiveData<UiState<String>>
        get() = _addConfessionState

    private val _addAnswerState = MutableLiveData<UiState<String>>()
    val addAnswerState: LiveData<UiState<String>>
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

    fun addConfession(userName: String, confessionText: String) {
        _addConfessionState.value = UiState.Loading
        repository.addConfession(userName, confessionText) {
            _addConfessionState.value = it
        }
    }

    fun fetchConfessions(limit: Long, isMyConfessions: Boolean = true) {
        _fetchConfessionsState.value = UiState.Loading
        repository.fetchConfessions(limit, isMyConfessions) { result ->
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

    fun addAnswerFavorite(confessionId: String) {
        _addFavoriteAnswer.value = UiState.Loading
        repository.favoriteAnswer(confessionId) {
            _addFavoriteAnswer.value = it
        }
    }
}
