package com.example.confessme.presentation

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.confessme.presentation.ui.ConfessionsFragment
import com.example.confessme.presentation.ui.ConfessionsToMeFragment
import com.example.confessme.util.ConfessionCategory

class ProfileViewPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int {
        return 2
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ConfessionsFragment(confessionCategory = ConfessionCategory.MY_CONFESSIONS)
            1 -> ConfessionsToMeFragment(confessionCategory = ConfessionCategory.CONFESSIONS_TO_ME)
            else -> ConfessionsFragment(confessionCategory = ConfessionCategory.MY_CONFESSIONS)
        }
    }
}