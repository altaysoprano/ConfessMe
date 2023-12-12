package com.example.confessme.presentation.ui

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.confessme.R
import com.example.confessme.databinding.FragmentProfileBinding
import com.example.confessme.presentation.ProfileViewModel
import com.example.confessme.presentation.ProfileViewPagerAdapter
import com.example.confessme.presentation.ScrollableToTop
import com.example.confessme.util.FollowType
import com.example.confessme.util.UiState
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileFragment() : Fragment() {

    private lateinit var binding: FragmentProfileBinding
    private lateinit var navRegister: FragmentNavigation
    private lateinit var viewPagerAdapter: ProfileViewPagerAdapter
    private val viewModel: ProfileViewModel by viewModels()
    private var reselectedTabItemIndex: Int? = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentProfileBinding.inflate(inflater, container, false)
        (activity as AppCompatActivity?)!!.setSupportActionBar(binding.profileToolbar)
        (activity as AppCompatActivity?)?.title = ""
        navRegister = activity as FragmentNavigation
        setHasOptionsMenu(true)

        setTablayoutAndViewPager()
        setAllClickListener()
        fetchUserProfile()
        observeFetchState()

        return binding.root
    }

    private fun observeFetchState() {
        viewModel.getProfileState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBarProfile.visibility = View.VISIBLE
                    setAllProfileDataDefault()
                }

                is UiState.Failure -> {
                    binding.progressBarProfile.visibility = View.GONE
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarProfile.visibility = View.GONE
                    val userProfile = state.data
                    if (userProfile != null) {
                        binding.firstNameTv.text = userProfile.userName
                        binding.bioTv.text = userProfile.bio
                        binding.profileFollowingCountTv.text = userProfile.followCount.toString()
                        binding.profileFollowerCountTv.text = userProfile.followersCount.toString()

                        if (userProfile.imageUrl.isNotEmpty()) {
                            Glide.with(requireContext())
                                .load(userProfile.imageUrl)
                                .into(binding.profileScreenProfileImage)
                        } else {
                            binding.profileScreenProfileImage.setImageResource(R.drawable.empty_profile_photo)
                        }
                    }
                }
            }
        }
    }

    private fun fetchUserProfile() {
        viewModel.getProfileData()
    }

    private fun setAllClickListener() {
        binding.profileFollowingTv.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("userUid", "")
            bundle.putInt("followType", FollowType.MyFollowings.ordinal)

            val followsFragment = FollowsFragment()
            followsFragment.arguments = bundle

            navRegister.navigateFrag(followsFragment, true)
        }

        binding.profileFollowersTv.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("userUid", "")
            bundle.putInt("followType", FollowType.MyFollowers.ordinal)

            val followsFragment = FollowsFragment()
            followsFragment.arguments = bundle

            navRegister.navigateFrag(followsFragment, true)
        }
    }

    private fun setTablayoutAndViewPager() {
        viewPagerAdapter = ProfileViewPagerAdapter(this)
        binding.profileViewPager.adapter = viewPagerAdapter

        binding.profileTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    binding.profileViewPager.currentItem = it.position
                    reselectedTabItemIndex = tab.position
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                // Bir sekme seçilmemiş durumdayken yapılacak işlemler
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                onTabReselected()
            }
        })

        binding.profileViewPager.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.profileTabLayout.getTabAt(position)?.select()
            }
        })
    }

    fun onTabReselected() {
        reselectedTabItemIndex?.let {
            val fragmentPosition = reselectedTabItemIndex
            val fragmentTag = "f$fragmentPosition"

            val fragment = childFragmentManager.findFragmentByTag(fragmentTag)
            if (fragment != null && fragment is ScrollableToTop) {
                fragment.scrollToTop()
            }
        }
    }

    fun onBottomNavItemReselected() {
        binding.profileTabLayout.getTabAt(0)?.select()

        val fragmentPosition = 0
        val fragmentTag = "f$fragmentPosition"

        val fragment = childFragmentManager.findFragmentByTag(fragmentTag)
        if (fragment != null && fragment is ScrollableToTop) {
            fragment.scrollToTop()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        val actionBar = (activity as AppCompatActivity?)?.supportActionBar
        inflater.inflate(R.menu.profile_menu, menu)
        actionBar?.setDisplayHomeAsUpEnabled(false)
        actionBar?.setDisplayShowHomeEnabled(false)
        requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView).visibility =
            View.VISIBLE
        return super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.edit_profile -> {
                navRegister.navigateFrag(EditProfileFragment(), true)
            }

            R.id.settings -> {
                navRegister.navigateFrag(SettingsFragment(), true)
            }

            android.R.id.home -> {
                requireActivity().onBackPressed()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setAllProfileDataDefault() {
        binding.profileScreenProfileImage.setImageResource(R.drawable.empty_profile_photo)
        binding.firstNameTv.text = ""
        binding.bioTv.text = ""
    }
}