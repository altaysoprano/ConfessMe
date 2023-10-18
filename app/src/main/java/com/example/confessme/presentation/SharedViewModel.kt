package com.example.confessme.presentation

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.confessme.data.model.Confession
import com.example.confessme.presentation.ui.ConfessionListAdapter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SharedViewModel @Inject constructor() : ViewModel() {

    private var _selectedUserUid = MutableLiveData("")
    val selectedUserUid: LiveData<String> = _selectedUserUid

    private var _selectedUserName = MutableLiveData("")
    val selectedUserName: LiveData<String> = _selectedUserName

    fun setSelectedUserName(username: String) {
        _selectedUserName.value = username
    }

    private var _selectedUserEmail = MutableLiveData("")
    val selectedUserEmail: LiveData<String> = _selectedUserEmail

    fun setSelectedUserEmail(useremail: String) {
        _selectedUserEmail.value = useremail
    }

    fun setSelectedUserUid(userUid: String) {
        _selectedUserUid.value = userUid
    }

}
