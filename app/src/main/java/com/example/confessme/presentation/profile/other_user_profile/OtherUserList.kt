package com.example.confessme.presentation.profile.other_user_profile

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.example.confessme.presentation.utils.FragmentNavigation

open class OtherUserListFragment: Fragment() {

    protected fun navigateToUserProfile(userEmail: String, userUid: String, userName: String,
                                      userToken: String, navRegister: FragmentNavigation, currentUserId: String) {
        val bundle = Bundle()
        bundle.putString("userEmail", userEmail)
        bundle.putString("userUid", userUid)
        bundle.putString("userName", userName)
        bundle.putString("userToken", userToken)

        val profileFragment = OtherUserProfileFragment()
        profileFragment.arguments = bundle

        if (userUid != currentUserId) {
            navRegister.navigateFrag(profileFragment, true)
        }
    }
}