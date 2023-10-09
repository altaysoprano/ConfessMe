package com.example.confessme.presentation.ui

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModelProvider
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
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.example.confessme.R
import com.example.confessme.databinding.FragmentEditProfileBinding
import com.example.confessme.databinding.FragmentProfileBinding
import com.example.confessme.presentation.LoginViewModel
import com.example.confessme.presentation.ProfileViewModel
import com.example.confessme.util.UiState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EditProfileFragment : Fragment() {

    private lateinit var binding: FragmentEditProfileBinding
    private lateinit var selectedImg: Uri
    private lateinit var navRegister: FragmentNavigation
    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var currentUsername: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        (activity as AppCompatActivity?)!!.title = "Edit Profile"
        navRegister = activity as FragmentNavigation
        setHasOptionsMenu(true)

        (activity as AppCompatActivity?)!!.supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_close) // "X" simgesi olarak kullanılan drawable
        }

        // Profil fotoğrafı kısmına tıklayınca galeriyi aç
        binding.profileImage.setOnClickListener {
            val intent = Intent()
            intent.action = Intent.ACTION_GET_CONTENT
            intent.type = "image/*"
            startActivityForResult(intent, 1)
        }

        viewModel.getProfileData()

        viewModel.getProfileState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBarEditProfile.visibility = View.VISIBLE
                }

                is UiState.Failure -> {
                    binding.progressBarEditProfile.visibility = View.GONE
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarEditProfile.visibility = View.GONE
                    val userProfile = state.data
                    if (userProfile != null) {
                        currentUsername = userProfile.userName
                        binding.firstNameEt.setText(userProfile.userName)
                        binding.bioEt.setText(userProfile.bio)
                        if (userProfile.imageUrl.isNotEmpty()) {
                            Glide.with(requireContext())
                                .load(userProfile.imageUrl)
                                .into(binding.profileImage)
                        }
                    }
                }
            }
        }

        binding.saveButton.setOnClickListener {
            val username = binding.firstNameEt.text?.trim().toString()
            val bio = binding.bioEt.text?.trim().toString()

            if (::selectedImg.isInitialized) {
                viewModel.updateProfile(currentUsername, username, bio, selectedImg)
            } else {
                viewModel.updateProfile(
                    currentUsername,
                    username,
                    bio,
                    Uri.EMPTY
                )
            }
        }

        viewModel.updateProfileState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBarEditProfile.visibility = View.VISIBLE
                }

                is UiState.Failure -> {
                    binding.progressBarEditProfile.visibility = View.GONE
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarEditProfile.visibility = View.GONE
                    val fragmentManager = parentFragmentManager
                    for (i in 0 until fragmentManager.backStackEntryCount) {
                        fragmentManager.popBackStack()
                    }
                    fragmentManager.beginTransaction()
                        .replace(R.id.coordinator, ProfileFragment())
                        .commit()
                }
            }
        }

        return binding.root
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (data != null) {

            if (data.data != null) {
                selectedImg = data.data!!

                binding.profileImage.setImageURI(selectedImg)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                requireActivity().onBackPressed()
                return true
            }
        }
        return false
    }

}