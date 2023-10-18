package com.example.confessme.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.confessme.data.model.User
import com.example.confessme.data.repository.UserRepo
import com.example.confessme.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: UserRepo
) : ViewModel() {

    private val _searchResults = MutableLiveData<UiState<List<User>>>()
    val searchResults: LiveData<UiState<List<User>>>
        get() = _searchResults

    private val _userProfile = MutableLiveData<UiState<User?>>()
    val userProfile: LiveData<UiState<User?>>
        get() = _userProfile


    private var searchJob: Job? = null

    fun searchUsers(query: String) {
        searchJob?.cancel()

        _searchResults.value = UiState.Loading

        searchJob = viewModelScope.launch {
            delay(500)
            if (query.isNotBlank()) {
                repository.searchUsers(query) { result ->
                    _searchResults.postValue(result)
                }
            } else {
                _searchResults.postValue(UiState.Success(emptyList()))
            }
        }
    }
}
