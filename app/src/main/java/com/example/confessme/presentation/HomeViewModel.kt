package com.example.confessme.presentation

import androidx.lifecycle.ViewModel
import com.example.confessme.data.repository.Repository
import com.example.confessme.presentation.ui.FragmentNavigation
import com.example.confessme.presentation.ui.LoginFragment
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: Repository
) : ViewModel() {

    private var firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var navRegister: FragmentNavigation

    fun signOut(activity: FragmentNavigation) {
        navRegister = activity as FragmentNavigation
        firebaseAuth.signOut()
        navRegister.navigateFrag(LoginFragment(), false)
    }

}