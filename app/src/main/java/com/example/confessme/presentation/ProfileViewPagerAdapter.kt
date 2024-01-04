package com.example.confessme.presentation

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.confessme.presentation.ui.BookmarksFragment
import com.example.confessme.presentation.ui.ConfessionsFragment
import com.example.confessme.presentation.ui.ConfessionsToMeFragment
import com.example.confessme.util.ConfessionCategory

class ProfileViewPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    val fragments = mutableListOf<Fragment>()

    override fun getItemCount(): Int {
        return 3
    }

    override fun createFragment(position: Int): Fragment {
        val fragment = when (position) {
            0 -> ConfessionsFragment()
            1 -> ConfessionsToMeFragment()
            2 -> BookmarksFragment()
            else -> ConfessionsFragment()
        }
        fragments.add(fragment)
        return fragment
    }
}
