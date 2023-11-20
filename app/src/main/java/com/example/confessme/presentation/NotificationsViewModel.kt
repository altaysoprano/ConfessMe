package com.example.confessme.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.confessme.R
import com.example.confessme.data.repository.AuthRepo
import com.example.confessme.data.repository.ConfessionRepo
import com.example.confessme.presentation.ui.FragmentNavigation
import com.example.confessme.presentation.ui.HomeFragment
import com.example.confessme.presentation.ui.LoginFragment
import com.example.confessme.util.UiState
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val authRepo: AuthRepo
) : ViewModel() {

    private var firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _signOutState = MutableLiveData<UiState<String>>()
    val signOutState: LiveData<UiState<String>>
        get() = _signOutState

    fun signOut() {
        _signOutState.value = UiState.Loading
        authRepo.signOut {
            _signOutState.value = it
        }
    }
}