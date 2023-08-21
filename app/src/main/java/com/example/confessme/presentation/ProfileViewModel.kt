package com.example.confessme.presentation

import android.app.AlertDialog
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.confessme.data.model.User
import com.example.confessme.data.repository.Repository
import com.example.confessme.presentation.ui.FragmentNavigation
import com.example.confessme.presentation.ui.LoginFragment
import com.example.confessme.util.UiState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: Repository
) : ViewModel() {

    private var firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var navRegister: FragmentNavigation

    private val _updateProfileState = MutableLiveData<UiState<String>>()
    val updateProfileState: LiveData<UiState<String>>
        get() = _updateProfileState

    private val _fetchProfileState = MutableLiveData<UiState<User?>>()
    val fetchProfileState: LiveData<UiState<User?>>
        get() = _fetchProfileState

    fun updateProfile(previousUserName: String, username: String, bio: String, imageUri: Uri) {
        _updateProfileState.value = UiState.Loading
        repository.updateProfile(previousUserName, username, bio, imageUri) {
            _updateProfileState.value = it
        }
    }
    fun fetchUserProfileByUsername(username: String) {
        _fetchProfileState.value = UiState.Loading

        repository.fetchUserProfileByUsername(username) { result ->
            _fetchProfileState.postValue(result)
        }
    }

    fun getProfileData() {
        _fetchProfileState.value = UiState.Loading

        repository.fetchUserProfile() { result ->
            _fetchProfileState.postValue(result)
        }
    }

    fun signOut(activity: FragmentNavigation) {
        navRegister = activity as FragmentNavigation
        firebaseAuth.signOut()
        navRegister.navigateFrag(LoginFragment(), false)
    }

}