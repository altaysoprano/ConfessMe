package com.example.confessme.presentation.profile.my_profile

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.confessme.presentation.profile.my_profile.bookmarks.BookmarksFragment
import com.example.confessme.presentation.profile.my_profile.my_confessions.ConfessionsFragment
import com.example.confessme.presentation.profile.my_profile.confessions_to_me.ConfessionsToMeFragment

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
