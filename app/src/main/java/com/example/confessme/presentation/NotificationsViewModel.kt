package com.example.confessme.presentation

import androidx.lifecycle.ViewModel
import com.example.confessme.R
import com.example.confessme.data.repository.ConfessionRepo
import com.example.confessme.presentation.ui.FragmentNavigation
import com.example.confessme.presentation.ui.HomeFragment
import com.example.confessme.presentation.ui.LoginFragment
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repository: ConfessionRepo
) : ViewModel() {

    private var firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    fun signOut() {
        firebaseAuth.signOut()
    }
}