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

        sharedViewModel.selectedUserEmail.observe(viewLifecycleOwner) { useremail ->
            if (!useremail.isNullOrEmpty()) {
                Log.d("Mesaj: ", "Email boş değil: $useremail")
                viewModel.fetchUserProfileByEmail(useremail)
                checkIfUserFollowed(useremail)
                binding.progressButtonLayout.followButtonCardview.visibility = View.VISIBLE
                binding.profileViewPager.adapter = null
                binding.profileTabLayout.visibility = View.GONE
                binding.confessFabButton.visibility = View.VISIBLE
            } else {
                Log.d("Mesaj: ", "Email boş")
                viewModel.getProfileData()
                viewPagerAdapter = ProfileViewPagerAdapter(this)
                binding.profileViewPager.adapter = viewPagerAdapter
                binding.profileTabLayout.visibility = View.VISIBLE
                binding.confessFabButton.visibility = View.GONE
                binding.progressButtonLayout.followButtonCardview.visibility = View.GONE
            }
        }

        viewModel.fetchProfileState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBarProfile.visibility = View.VISIBLE
                    Log.d("Mesaj: ", "fetchProfileState loadingte")
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
                        Log.d("Mesaj: ", "userProfile boş değil")
                        binding.firstNameTv.text = userProfile.userName
                        binding.bioTv.text = userProfile.bio
                        if (userProfile.imageUrl.isNotEmpty()) {
                            Log.d("Mesaj: ", "pp boş değil. url: ${userProfile.imageUrl}")
                            Glide.with(requireContext())
                                .load(userProfile.imageUrl)
                                .into(binding.profileScreenProfileImage)
                        } else {
                            Log.d("Mesaj: ", "pp boş")
                        }
                    } else {
                        Log.d("Mesaj: ", "userProfile boş")
                    }
                    Log.d("Mesaj: ", "fetchProfileState successte")
                }
            }
        }

        viewModel.getProfileState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBarProfile.visibility = View.VISIBLE
                    Log.d("Mesaj: ", "getProfileData loadingte")
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
                        Log.d("Mesaj: ", "my userProfile boş değil")
                        binding.firstNameTv.text = userProfile.userName
                        binding.bioTv.text = userProfile.bio
                        if (userProfile.imageUrl.isNotEmpty()) {
                            Log.d("Mesaj: ", "my pp boş değil. url: ${userProfile.imageUrl}")
                            Glide.with(requireContext())
                                .load(userProfile.imageUrl)
                                .into(binding.profileScreenProfileImage)
                        } else {
                            Log.d("Mesaj: ", "my pp boş")
                        }
                    } else {
                        Log.d("Mesaj: ", "my userProfile boş")
                    }
                    Log.d("Mesaj: ", "getProfileData successte")
                }
            }
        }

        binding.progressButtonLayout.followButtonCardview.setOnClickListener {
            followOrUnfollowUser()
        }

        binding.confessFabButton.setOnClickListener {
            val selectedUserEmail = sharedViewModel.selectedUserEmail.value
            if (!selectedUserEmail.isNullOrEmpty()) {
                val confessFragment = ConfessFragment()
                navRegister.navigateFrag(confessFragment, true)
            } else {
                Toast.makeText(requireContext(), "User not found", Toast.LENGTH_SHORT).show()
            }
        }
        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        sharedViewModel.setSelectedUserEmail("")
        sharedViewModel.setSelectedUserName("")
    }

    fun onBackPressedInProfileFragment() {
        sharedViewModel.setSelectedUserName("")
        sharedViewModel.setSelectedUserEmail("")
        Log.d("Mesaj: ", "onbackpressed çalıştı, email: ${sharedViewModel.selectedUserEmail.value}")
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        val actionBar = (activity as AppCompatActivity?)?.supportActionBar

        sharedViewModel.selectedUserEmail.observe(viewLifecycleOwner) { userEmail ->
            if (!userEmail.isNullOrEmpty()) {
                Log.d("Mesaj: ", "Şu an selecteduseremail boş değil, bu yüzden other user menüsü görünüyor")
                actionBar?.setDisplayHomeAsUpEnabled(true)
                actionBar?.setDisplayShowHomeEnabled(true)
                requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView).visibility =
                    View.GONE
                actionBar?.setHomeAsUpIndicator(R.drawable.ic_back)
                (activity as AppCompatActivity?)!!.title = "Profile"
            } else {
                Log.d("Mesaj: ", "Şu an selecteduseremail dolu, bu yüzden myprofile menüsü görünüyor")
                inflater.inflate(R.menu.profile_menu, menu)
                actionBar?.setDisplayHomeAsUpEnabled(false)
                actionBar?.setDisplayShowHomeEnabled(false)
                (activity as AppCompatActivity?)!!.title = "My Profile"
            }
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
        if (sharedViewModel.selectedUserEmail.value.isNullOrEmpty()) {
            requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView).visibility =
                View.VISIBLE
            (activity as AppCompatActivity?)!!.supportActionBar?.apply {
                setDisplayHomeAsUpEnabled(false)
            }
        }
    }

    private fun followOrUnfollowUser() {
        val selectedUserEmail = sharedViewModel.selectedUserEmail.value

        if (!selectedUserEmail.isNullOrEmpty()) {
            viewModel.followOrUnfollowUser(selectedUserEmail)
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
                        checkIfUserFollowed(selectedUserEmail)
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