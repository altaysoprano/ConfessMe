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
import com.example.confessme.R
import com.example.confessme.databinding.FragmentOtherUserProfileBinding
import com.example.confessme.databinding.FragmentProfileBinding
import com.example.confessme.presentation.OtherUserViewPagerAdapter
import com.example.confessme.presentation.ProfileViewModel
import com.example.confessme.presentation.ProfileViewPagerAdapter
import com.example.confessme.presentation.SharedViewModel
import com.example.confessme.util.UiState
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OtherUserProfileFragment : Fragment() {

    private lateinit var binding: FragmentOtherUserProfileBinding
    private lateinit var navRegister: FragmentNavigation
    private lateinit var viewPagerAdapter: OtherUserViewPagerAdapter
    private lateinit var userUid: String
    private lateinit var userEmail: String
    private val viewModel: ProfileViewModel by viewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOtherUserProfileBinding.inflate(inflater, container, false)
        (activity as AppCompatActivity?)!!.setSupportActionBar(binding.otherUserProfileToolbar)
        (activity as AppCompatActivity?)?.title = ""
        navRegister = activity as FragmentNavigation
        setHasOptionsMenu(true)
        userEmail = arguments?.getString("userEmail") ?: ""
        userUid = arguments?.getString("userUid") ?: ""

        binding.otherUserProfileTabLayout.addOnTabSelectedListener(object :
            TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {

                tab?.let {
                    binding.otherUserProfileViewPager.currentItem = it.position
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                // Bir sekme seçilmemiş durumdayken yapılacak işlemler
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                // Zaten seçili olan bir sekmeye tekrar tıklanıldığında yapılacak işlemler
            }
        })

        binding.otherUserProfileViewPager.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.otherUserProfileTabLayout.getTabAt(position)?.select()
            }
        })

        if (!userEmail.isNullOrEmpty()) {
            viewModel.fetchUserProfileByEmail(userEmail)
            checkIfUserFollowed(userEmail)
            viewPagerAdapter = OtherUserViewPagerAdapter(userUid, this)
            binding.otherUserProfileViewPager.adapter = viewPagerAdapter
        } else {
            Toast.makeText(requireContext(), "An error occured. Please try again.", Toast.LENGTH_SHORT)
                .show()
        }

        viewModel.fetchProfileState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.otherUserProgressBarProfile.visibility = View.VISIBLE
                    setAllProfileDataDefault()
                }

                is UiState.Failure -> {
                    binding.otherUserProgressBarProfile.visibility = View.GONE
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.otherUserProgressBarProfile.visibility = View.GONE
                    val userProfile = state.data
                    if (userProfile != null) {
                        binding.otherUserFirstNameTv.text = userProfile.userName
                        binding.otherUserBioTv.text = userProfile.bio

                        if (userProfile.imageUrl.isNotEmpty()) {
                            Glide.with(requireContext())
                                .load(userProfile.imageUrl)
                                .into(binding.otherUserProfileScreenProfileImage)
                        } else {
                            binding.otherUserProfileScreenProfileImage.setImageResource(R.drawable.empty_profile_photo)
                        }
                    }
                }
            }
        }

        binding.otherUserProgressButtonLayout.followButtonCardview.setOnClickListener {
            followOrUnfollowUser()
        }

        binding.otherUserConfessFabButton.setOnClickListener {

            if (!userUid.isEmpty()) {
                val bundle = Bundle()
                bundle.putString("userUid", userUid)

                val confessFragment = ConfessFragment()
                confessFragment.arguments = bundle

                navRegister.navigateFrag(confessFragment, true)
            } else {
                Toast.makeText(requireContext(), "User not found", Toast.LENGTH_SHORT).show()
            }
        }

        return binding.root
    }

    private fun followOrUnfollowUser() {

        if (!userEmail.isNullOrEmpty()) {
            viewModel.followOrUnfollowUser(userEmail)
            viewModel.followUserState.observe(viewLifecycleOwner) { state ->
                when (state) {
                    is UiState.Loading -> {
                        binding.otherUserProgressButtonLayout.progressBarFollowButton.visibility =
                            View.VISIBLE
                    }

                    is UiState.Failure -> {
                        binding.otherUserProgressButtonLayout.progressBarFollowButton.visibility =
                            View.GONE
                        Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                            .show()
                    }

                    is UiState.Success -> {
                        binding.otherUserProgressButtonLayout.progressBarFollowButton.visibility =
                            View.GONE
                        checkIfUserFollowed(userEmail)
                        Toast.makeText(requireContext(), state.data, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    @SuppressLint("ResourceAsColor")
    private fun checkIfUserFollowed(useremailToCheck: String) {
        viewModel.checkIfUserFollowed(useremailToCheck)
        viewModel.checkFollowingState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is UiState.Success -> {
                    val isFollowing = result.data
                    if (isFollowing) {
                        binding.otherUserProgressButtonLayout.followButtonTv.text = "FOLLOWING"
                        binding.otherUserProgressButtonLayout.followButtonLayout.setBackgroundColor(
                            Color.WHITE
                        )
                        binding.otherUserProgressButtonLayout.followButtonTv.setTextColor(Color.BLACK)
                        binding.otherUserProgressButtonLayout.progressBarFollowButton.indeterminateTintList =
                            ColorStateList.valueOf(
                                Color.BLACK
                            )
                    } else {
                        binding.otherUserProgressButtonLayout.followButtonTv.text = "FOLLOW"
                        binding.otherUserProgressButtonLayout.followButtonLayout.setBackgroundColor(
                            Color.parseColor("#cf363c")
                        )
                        binding.otherUserProgressButtonLayout.followButtonTv.setTextColor(
                            Color.parseColor(
                                "#ffffff"
                            )
                        )
                        binding.otherUserProgressButtonLayout.progressBarFollowButton.indeterminateTintList =
                            ColorStateList.valueOf(
                                Color.WHITE
                            )
                    }
                    binding.otherUserProgressButtonLayout.progressBarFollowButton.visibility =
                        View.GONE
                }

                is UiState.Failure -> {
                    binding.otherUserProgressButtonLayout.progressBarFollowButton.visibility =
                        View.GONE
                    Toast.makeText(requireContext(), result.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Loading -> {
                    binding.otherUserProgressButtonLayout.progressBarFollowButton.visibility =
                        View.VISIBLE
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        val actionBar = (activity as AppCompatActivity?)?.supportActionBar

        actionBar?.setDisplayHomeAsUpEnabled(true)
        actionBar?.setDisplayShowHomeEnabled(true)
        requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView).visibility =
            View.GONE
        actionBar?.setHomeAsUpIndicator(R.drawable.ic_back)
        return super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                requireActivity().onBackPressed()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setAllProfileDataDefault() {
        binding.otherUserProfileScreenProfileImage.setImageResource(R.drawable.empty_profile_photo)
        binding.otherUserFirstNameTv.text = ""
        binding.otherUserBioTv.text = ""
    }

}