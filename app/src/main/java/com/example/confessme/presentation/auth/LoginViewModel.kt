package com.example.confessme.presentation.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.confessme.data.repository.AuthRepo
import com.example.confessme.presentation.utils.UiState
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val authRepo: AuthRepo
) : ViewModel() {

    private val _signInState = MutableLiveData<UiState<String>>()
    val signInState: LiveData<UiState<String>>
        get() = _signInState

    var isUserLoggedIn = false

    init {
        checkIfUserLoggedIn()
    }

    fun signIn(email: String, password: String) {
        _signInState.value = UiState.Loading
        authRepo.signIn(email, password) {
            _signInState.value = it
        }
    }

    fun googleSignIn(idToken: String, googleSignInAccount: GoogleSignInAccount?) {
        _signInState.value = UiState.Loading
        authRepo.googleSignIn(idToken, googleSignInAccount) {
            _signInState.value = it
        }
    }

    private fun checkIfUserLoggedIn() {
        isUserLoggedIn = firebaseAuth.currentUser != null
    }


}