package com.example.confessme.presentation.profile.other_user_profile

import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.confessme.R
import com.example.confessme.presentation.confess.ConfessAnswerFragment
import com.example.confessme.presentation.profile.ConfessionListAdapter
import com.example.confessme.presentation.utils.FragmentNavigation

open class OtherUserViewPagerFragment: Fragment() {

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

    protected fun onAnswerClick(
        confessionId: String,
        adapter: ConfessionListAdapter,
        currentUserUid: String
    ) {
        if (!confessionId.isNullOrEmpty()) {
            val bundle = Bundle()
            bundle.putString("confessionId", confessionId)
            bundle.putString("currentUserUid", currentUserUid)

            val confessAnswerFragment = ConfessAnswerFragment(
                { position, updatedConfession ->
                    adapter.updateItem(position, updatedConfession)
                },
                { confessionId ->
                    findPositionById(confessionId, adapter)
                }
            )
            confessAnswerFragment.arguments = bundle
            confessAnswerFragment.show(
                requireActivity().supportFragmentManager,
                "ConfessAnswerFragment"
            )
        } else {
            Toast.makeText(
                requireContext(),
                getString(R.string.confession_not_found),
                Toast.LENGTH_SHORT
            )
                .show()
        }
    }

    protected fun findPositionById(confessionId: String, adapter: ConfessionListAdapter): Int {
        for (index in 0 until adapter.confessList.size) {
            if (adapter.confessList[index].id == confessionId) {
                return index
            }
        }
        return -1
    }
}