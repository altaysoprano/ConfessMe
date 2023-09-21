package com.example.confessme.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ProfileSearchSharedViewModel @Inject constructor() : ViewModel() {

    private var _selectedUserName = MutableLiveData("")
    val selectedUserName: LiveData<String> = _selectedUserName

    fun setSelectedUserName(username: String) {
        _selectedUserName.value = username
    }

}