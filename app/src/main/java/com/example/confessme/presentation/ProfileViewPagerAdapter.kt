package com.example.confessme.presentation

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.confessme.presentation.ui.ConfessionsFragment
import com.example.confessme.presentation.ui.ConfessionsToMeFragment

class ProfileViewPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int {
        return 2
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ConfessionsFragment(isMyConfessions = true)
            1 -> ConfessionsToMeFragment(isMyConfessions = false)
            else -> ConfessionsFragment(isMyConfessions = true)
        }
    }
}