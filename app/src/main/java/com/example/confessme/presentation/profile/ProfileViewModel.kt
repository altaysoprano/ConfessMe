package com.example.confessme.presentation.profile

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.confessme.data.model.FollowUser
import com.example.confessme.data.model.User
import com.example.confessme.data.repository.UserRepo
import com.example.confessme.presentation.utils.FragmentNavigation
import com.example.confessme.presentation.profile.edit_set_profile.ProfilePhotoAction
import com.example.confessme.presentation.utils.UiState
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: UserRepo
) : ViewModel() {

    private var firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var navRegister: FragmentNavigation

    private val _updateProfileState = MutableLiveData<UiState<String>>()
    val updateProfileState: LiveData<UiState<String>>
        get() = _updateProfileState

    private val _deleteProfilePhotoState = MutableLiveData<UiState<String>>()
    val deleteProfilePhotoState: LiveData<UiState<String>>
        get() = _deleteProfilePhotoState

    private val _fetchProfileState = MutableLiveData<UiState<User?>>()
    val fetchProfileState: LiveData<UiState<User?>>
        get() = _fetchProfileState

    private val _getProfileState = MutableLiveData<UiState<User?>>()
    val getProfileState: LiveData<UiState<User?>>
        get() = _getProfileState

    private val _followUserState = MutableLiveData<UiState<FollowUser>>()
    val followUserState: LiveData<UiState<FollowUser>>
        get() = _followUserState

    private val _checkFollowingState = MutableLiveData<UiState<FollowUser>>()
    val checkFollowingState: LiveData<UiState<FollowUser>>
        get() = _checkFollowingState

    fun updateProfile(previousUserName: String, previousImageUrl: String, username: String, bio: String,
                      imageUri: Uri, profilePhotoAction: ProfilePhotoAction
    ) {
        _updateProfileState.value = UiState.Loading
        repository.updateProfile(previousUserName, previousImageUrl, username, bio, imageUri, profilePhotoAction) {
            _updateProfileState.value = it
        }
    }
    fun fetchUserProfileByUserId(userUid: String) {
        _fetchProfileState.value = UiState.Loading

        repository.fetchUserProfileByUid(userUid) { result ->
            _fetchProfileState.postValue(result)
        }
    }

    fun followOrUnfollowUser(userUid: String, userName: String, userToken: String) {
        _followUserState.value = UiState.Loading

        repository.checkIfUserFollowed(userUid) { result ->
            if (result is UiState.Success && result.data.isFollowed) {
                repository.unfollowUser(userUid) { unfollowResult ->
                    if (unfollowResult is UiState.Success) {
                        _followUserState.postValue(unfollowResult)
                    } else {
                        _followUserState.postValue(UiState.Failure(unfollowResult.toString()))
                    }
                }
            } else {
                repository.followUser(userUid, userName, userToken) { followResult ->
                    if (followResult is UiState.Success) {
                        _followUserState.postValue(UiState.Success(followResult.data))
                    } else {
                        _followUserState.postValue(UiState.Failure(followResult.toString()))
                    }
                }
            }
        }
    }
    fun checkIfUserFollowed(userUid: String) {
        _checkFollowingState.value = UiState.Loading

        repository.checkIfUserFollowed(userUid) { result ->
            _checkFollowingState.postValue(result)
        }
    }

    fun getProfileData() {
        _getProfileState.value = UiState.Loading

        repository.fetchUserProfile() { result ->
            _getProfileState.postValue(result)
        }
    }
}