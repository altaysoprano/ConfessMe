package com.example.confessme.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.confessme.data.repository.Repository
import com.example.confessme.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: Repository
) : ViewModel() {

    private val _updatePasswordState = MutableLiveData<UiState<String>>()
    val updatePasswordState: LiveData<UiState<String>>
        get() = _updatePasswordState

    fun updatePassword(previousPassword: String, newPassword: String, newPasswordAgain: String) {

        if (newPassword != newPasswordAgain) {
            _updatePasswordState.value = UiState.Failure("Passwords do not match.")
            return
        }

        _updatePasswordState.value = UiState.Loading
        repository.updatePassword(previousPassword, newPassword) { result ->
            _updatePasswordState.postValue(result)
        }
    }
}
