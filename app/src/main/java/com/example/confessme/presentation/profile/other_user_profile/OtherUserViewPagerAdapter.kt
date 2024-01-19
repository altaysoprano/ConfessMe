package com.example.confessme.presentation.profile.other_user_profile

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.confessme.presentation.profile.ConfessionCategory

class OtherUserViewPagerAdapter(private val userUid: String, fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int {
        return 2
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> OthersConfessionsFragment(userUid, confessionCategory = ConfessionCategory.OTHER_USER_CONFESSIONS)
            1 -> ConfessedFragment(userUid, confessionCategory = ConfessionCategory.CONFESSIONS_TO_OTHERS)
            else -> OthersConfessionsFragment(userUid, confessionCategory = ConfessionCategory.CONFESSIONS_TO_OTHERS)
        }
    }
}