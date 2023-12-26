package com.example.confessme.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.confessme.data.repository.AuthRepo
import com.example.confessme.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: AuthRepo
) : ViewModel() {

    private val _updatePasswordState = MutableLiveData<UiState<String>>()
    val updatePasswordState: LiveData<UiState<String>>
        get() = _updatePasswordState

    fun updatePassword(previousPassword: String, newPassword: String, newPasswordAgain: String) {
        _updatePasswordState.value = UiState.Loading
        repository.updatePassword(previousPassword, newPassword) { result ->
            _updatePasswordState.postValue(result)
        }
    }

    fun updateLanguage(language: String) {
        repository.updateLanguage(language)
    }
}
