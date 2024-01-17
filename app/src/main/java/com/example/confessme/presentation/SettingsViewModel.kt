package com.example.confessme.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.confessme.data.repository.AuthRepo
import com.example.confessme.util.UiState
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: AuthRepo
) : ViewModel() {

    private val _updatePasswordState = MutableLiveData<UiState<String>>()
    val updatePasswordState: LiveData<UiState<String>>
        get() = _updatePasswordState

    private val _checkIfGoogleSignInState = MutableLiveData<UiState<Boolean>>()
    val checkIfGoogleSignInState: LiveData<UiState<Boolean>>
        get() = _checkIfGoogleSignInState

    private val _deleteAccountState = MutableLiveData<UiState<String>>()
    val deleteAccountState: LiveData<UiState<String>>
        get() = _deleteAccountState

    fun updatePassword(previousPassword: String, newPassword: String) {
        _updatePasswordState.value = UiState.Loading
        repository.updatePassword(previousPassword, newPassword) { result ->
            _updatePasswordState.postValue(result)
        }
    }

    fun checkIfGoogleSignIn() {
        _checkIfGoogleSignInState.value = UiState.Loading
        repository.isGoogleSignIn { result ->
            _checkIfGoogleSignInState.postValue(result)
        }
    }

    fun updateLanguage(language: String) {
        repository.updateLanguage(language)
    }

    fun deleteAccountWithConfessionsAndSignOut(currentPassword: String, googleSignInAccount: GoogleSignInAccount?) {
        _deleteAccountState.value = UiState.Loading
        repository.deleteAccountWithConfessionsAndSignOut(currentPassword, googleSignInAccount) { result ->
            _deleteAccountState.postValue(result)
        }
    }
}
