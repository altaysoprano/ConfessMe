package com.example.confessme.presentation

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.confessme.presentation.ui.ConfessedFragment
import com.example.confessme.presentation.ui.ConfessionsFragment
import com.example.confessme.presentation.ui.ConfessionsToMeFragment
import com.example.confessme.presentation.ui.OthersConfessionsFragment
import com.example.confessme.util.ConfessionCategory

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