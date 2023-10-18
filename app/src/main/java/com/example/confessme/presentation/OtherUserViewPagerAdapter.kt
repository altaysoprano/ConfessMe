package com.example.confessme.presentation

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.confessme.presentation.ui.ConfessionsFragment
import com.example.confessme.presentation.ui.ConfessionsToMeFragment
import com.example.confessme.util.ConfessionCategory

class OtherUserViewPagerAdapter(private val userUid: String, fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int {
        return 2
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ConfessionsFragment(userUid, confessionCategory = ConfessionCategory.CONFESSIONS_TO_OTHERS)
            1 -> ConfessionsToMeFragment(userUid, confessionCategory = ConfessionCategory.OTHER_USER_CONFESSIONS)
            else -> ConfessionsFragment(userUid, confessionCategory = ConfessionCategory.CONFESSIONS_TO_OTHERS)
        }
    }
}