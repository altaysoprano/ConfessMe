package com.example.confessme.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.confessme.data.model.Confession
import com.example.confessme.data.repository.ConfessionRepo
import com.example.confessme.util.UiState
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

    fun getConfession(confessionId: String) {
        _getConfessionState.value = UiState.Loading
        viewModelScope.launch {
            repository.getConfession(confessionId) {result ->
                _getConfessionState.postValue(result)
            }
        }
    }
}
