package com.example.confessme.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.confessme.data.repository.AuthRepo
import com.example.confessme.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepo: AuthRepo
): ViewModel() {

    private val _signUpState = MutableLiveData<UiState<String>>()
    val signUpState: LiveData<UiState<String>>
        get() = _signUpState

    fun signUp(email: String, pass: String, confirmPass: String) {
        _signUpState.value = UiState.Loading
        authRepo.signUp(email, pass, confirmPass) {
            _signUpState.value = it
        }
    }

}