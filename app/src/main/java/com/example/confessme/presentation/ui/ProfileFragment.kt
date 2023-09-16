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
import android.widget.TableLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.confessme.R
import com.example.confessme.databinding.FragmentProfileBinding
import com.example.confessme.presentation.ProfileSearchSharedViewModel
import com.example.confessme.presentation.ProfileViewModel
import com.example.confessme.presentation.ProfileViewPagerAdapter
import com.example.confessme.util.UiState
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private lateinit var binding: FragmentProfileBinding
    private lateinit var navRegister: FragmentNavigation
    private lateinit var viewPagerAdapter: ProfileViewPagerAdapter
    private val viewModel: ProfileViewModel by viewModels()
    private val sharedViewModel: ProfileSearchSharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentProfileBinding.inflate(inflater, container, false)
        (activity as AppCompatActivity?)!!.supportActionBar!!.show()
        (activity as AppCompatActivity?)!!.title = "My Profile"
        navRegister = activity as FragmentNavigation
        setHasOptionsMenu(true)
        viewPagerAdapter = ProfileViewPagerAdapter(this)
        binding.profileViewPager.adapter = viewPagerAdapter

        binding.profileTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {

                tab?.let {
                    binding.profileViewPager.currentItem = it.position
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                // Bir sekme seçilmemiş durumdayken yapılacak işlemler buraya gelecek
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                // Zaten seçili olan bir sekmeye tekrar tıklanıldığında yapılacak işlemler buraya gelecek
            }
        })

        binding.profileViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.profileTabLayout.getTabAt(position)?.select()
            }
        })

        sharedViewModel.selectedUserName.observe(viewLifecycleOwner) { username ->
            if (!username.isNullOrEmpty()) {
                viewModel.fetchUserProfileByUsername(username)
                checkIfUserFollowed(username)
                binding.progressButtonLayout.followButtonCardview.visibility = View.VISIBLE
                binding.profileViewPager.adapter = null
                binding.profileTabLayout.visibility = View.GONE
            } else {
                viewModel.getProfileData()
                binding.progressButtonLayout.followButtonCardview.visibility = View.GONE
            }
        }

        viewModel.fetchProfileState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBarProfile.visibility = View.VISIBLE
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
                        if (userProfile.imageUrl.isNotEmpty()) {
                            Glide.with(requireContext())
                                .load(userProfile.imageUrl)
                                .into(binding.profileScreenProfileImage)
                        }
                    }
                }
            }
        }

        binding.progressButtonLayout.followButtonCardview.setOnClickListener {
            followOrUnfollowUser()
        }

        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        sharedViewModel.setSelectedUserName("")
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        val actionBar = (activity as AppCompatActivity?)?.supportActionBar

        if (!sharedViewModel.selectedUserName.value.isNullOrEmpty()) {
            actionBar?.setDisplayHomeAsUpEnabled(true)
            actionBar?.setDisplayShowHomeEnabled(true)
            requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView).visibility =
                View.GONE
            actionBar?.setHomeAsUpIndicator(R.drawable.ic_back)
            (activity as AppCompatActivity?)!!.title = "Profile"
        } else {
            inflater.inflate(R.menu.profile_menu, menu)
        }
        return super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.sign_out -> {
                viewModel.signOut(activity as FragmentNavigation)
            }

            R.id.edit_profile -> {
                navRegister.navigateFrag(EditProfileFragment(), true)
            }

            android.R.id.home -> {
                requireActivity().onBackPressed()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        if (sharedViewModel.selectedUserName.value.isNullOrEmpty()) {
            requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView).visibility =
                View.VISIBLE
            (activity as AppCompatActivity?)!!.supportActionBar?.apply {
                setDisplayHomeAsUpEnabled(false)
            }
        }
    }

    private fun followOrUnfollowUser() {
        val selectedUserName = sharedViewModel.selectedUserName.value
        if (!selectedUserName.isNullOrEmpty()) {
            viewModel.followOrUnfollowUser(selectedUserName)
            viewModel.followUserState.observe(viewLifecycleOwner) { state ->
                when (state) {
                    is UiState.Loading -> {
                        binding.progressButtonLayout.progressBarFollowButton.visibility = View.VISIBLE
                    }

                    is UiState.Failure -> {
                        binding.progressButtonLayout.progressBarFollowButton.visibility = View.GONE
                        Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                            .show()
                    }

                    is UiState.Success -> {
                        binding.progressButtonLayout.progressBarFollowButton.visibility = View.GONE
                        checkIfUserFollowed(selectedUserName)
                        Toast.makeText(requireContext(), state.data, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    @SuppressLint("ResourceAsColor")
    private fun checkIfUserFollowed(usernameToCheck: String) {
        viewModel.checkIfUserFollowed(usernameToCheck)
        viewModel.checkFollowingState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is UiState.Success -> {
                    val isFollowing = result.data
                    if (isFollowing) {
                        binding.progressButtonLayout.followButtonTv.text = "FOLLOWING"
                        binding.progressButtonLayout.followButtonLayout.setBackgroundColor(Color.WHITE)
                        binding.progressButtonLayout.followButtonTv.setTextColor(Color.BLACK)
                        binding.progressButtonLayout.progressBarFollowButton.indeterminateTintList = ColorStateList.valueOf(Color.BLACK)
                    } else {
                        binding.progressButtonLayout.followButtonTv.text = "FOLLOW"
                        binding.progressButtonLayout.followButtonLayout.setBackgroundColor(Color.parseColor("#cf363c"))
                        binding.progressButtonLayout.followButtonTv.setTextColor(Color.parseColor("#ffffff"))
                        binding.progressButtonLayout.progressBarFollowButton.indeterminateTintList = ColorStateList.valueOf(Color.WHITE)
                    }
                    binding.progressButtonLayout.progressBarFollowButton.visibility = View.GONE
                }
                is UiState.Failure -> {
                    binding.progressButtonLayout.progressBarFollowButton.visibility = View.GONE
                    Toast.makeText(requireContext(), result.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }
                is UiState.Loading -> {
                    binding.progressButtonLayout.progressBarFollowButton.visibility = View.VISIBLE
                }
            }
        }
    }

}