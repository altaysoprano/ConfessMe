package com.example.confessme.presentation.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.example.confessme.R
import com.example.confessme.databinding.FragmentEditProfileBinding
import com.example.confessme.databinding.FragmentProfileBinding
import com.example.confessme.presentation.LoginViewModel
import com.example.confessme.presentation.ProfileViewModel
import com.example.confessme.util.ProfilePhotoAction
import com.example.confessme.util.UiState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EditProfileFragment : Fragment() {

    private lateinit var binding: FragmentEditProfileBinding
    private lateinit var selectedImg: Uri
    private lateinit var navRegister: FragmentNavigation
    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var currentUsername: String
    private lateinit var currentImageUrl: String
    private var isProfilePhotoRemoved: Boolean = false
    private val READ_STORAGE_PERMISSION_CODE = 101
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        (activity as AppCompatActivity?)!!.title = "Edit Profile"
        (activity as AppCompatActivity?)!!.setSupportActionBar(binding.editProfileToolbar)
        navRegister = activity as FragmentNavigation
        setHasOptionsMenu(true)

        (activity as AppCompatActivity?)!!.supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_back)
        }

        setOnClickListeners()
        viewModel.getProfileData()
        observeUpdateProfile()
        setUserNameAndBioTv()

        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        observeGetProfile()
    }

    private fun observeGetProfile() {
        viewModel.getProfileState.observe(this) { state ->
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
                        currentImageUrl = userProfile.imageUrl
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
    }

    private fun observeUpdateProfile() {
        viewModel.updateProfileState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.saveButton.isEnabled = false
                    binding.saveButton.alpha = 0.5f
                    binding.editButton.isEnabled = false
                    binding.editButton.alpha = 0.5f
                    binding.firstNameEt.isEnabled = false
                    binding.firstNameEt.alpha = 0.5f
                    binding.bioEt.isEnabled = false
                    binding.bioEt.alpha = 0.5f
                    binding.progressBarEditProfile.visibility = View.VISIBLE
                }

                is UiState.Failure -> {
                    binding.progressBarEditProfile.visibility = View.GONE
                    binding.saveButton.isEnabled = true
                    binding.saveButton.alpha = 1f
                    binding.editButton.isEnabled = true
                    binding.editButton.alpha = 1f
                    binding.firstNameEt.isEnabled = true
                    binding.firstNameEt.alpha = 1f
                    binding.bioEt.isEnabled = true
                    binding.bioEt.alpha = 1f
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
    }

    private fun setOnClickListeners() {
        binding.saveButton.setOnClickListener {
            val username = binding.firstNameEt.text?.trim().toString()
            val bio = binding.bioEt.text?.trim().toString()
            val cleanedBio = bio.replace("\\s+".toRegex(), " ")

            if (::selectedImg.isInitialized && selectedImg != Uri.EMPTY) {
                viewModel.updateProfile(currentUsername, currentImageUrl, username, cleanedBio, selectedImg, ProfilePhotoAction.CHANGE)
            } else if (isProfilePhotoRemoved) {
                viewModel.updateProfile(
                    currentUsername,
                    currentImageUrl,
                    username,
                    cleanedBio,
                    Uri.EMPTY,
                    ProfilePhotoAction.REMOVE
                )
            } else {
                viewModel.updateProfile(
                    currentUsername,
                    currentImageUrl,
                    username,
                    cleanedBio,
                    Uri.EMPTY,
                    ProfilePhotoAction.DO_NOT_CHANGE
                )
            }
        }

        binding.editButton.setOnClickListener {
            onEditProfilePhotoClick()
        }
    }

    private fun onEditProfilePhotoClick() {
        val options = arrayOf("Remove Profile Photo", "Change Profile Photo")

        val builder = AlertDialog.Builder(requireContext())
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> {
                    removeProfilePhoto()
                }
                1 -> {
                    checkPermission()
                }
            }
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun setUserNameAndBioTv() {
        val userNameMaxLength = 30
        val userNameMinLength = 3
        val bioMaxLength = 200
        var userNameCurrentLength = 0
        var userName = ""
        var bioCurrentLength = 0
        var isUserNameEmpty: Boolean? = false

        binding.firstNameEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                isUserNameEmpty = s?.isEmpty()
                userNameCurrentLength = s?.trim()?.length ?: 0
                userName = s.toString()

                binding.saveButton.isEnabled = true
                binding.saveButton.alpha = 1f

                checkIfUserNameAndBioValid(bioCurrentLength, userNameCurrentLength, userNameMaxLength,
                    userNameMinLength, isUserNameEmpty, bioMaxLength, userName)

            }

            override fun afterTextChanged(s: Editable?) {
            }
        })

        binding.bioEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                bioCurrentLength = s?.length ?: 0

                binding.saveButton.isEnabled = true
                binding.saveButton.alpha = 1f

                checkIfUserNameAndBioValid(bioCurrentLength, userNameCurrentLength, userNameMaxLength,
                    userNameMinLength, isUserNameEmpty, bioMaxLength, userName)
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })
    }

    private fun checkIfUserNameAndBioValid(bioCurrentLength: Int, userNameCurrentLength: Int, userNameMaxLength: Int,
                                           userNameMinLength: Int, isUserNameEmpty: Boolean?, bioMaxLength: Int,
                                            userName: String?) {
        if (userNameCurrentLength > userNameMaxLength) {
            binding.firstNameEt.error = "Username can be up to 30 characters long."
            binding.saveButton.isEnabled = false
            binding.saveButton.alpha = 0.5f
        }
        if (userNameCurrentLength < userNameMinLength) {
            binding.firstNameEt.error = "Username must be more than 3 characters."
            binding.saveButton.isEnabled = false
            binding.saveButton.alpha = 0.5f
        }
        if (isUserNameEmpty == true) {
            binding.firstNameEt.error = "Username cannot be empty."
            binding.saveButton.isEnabled = false
            binding.saveButton.alpha = 0.5f
        }
        if (userName?.contains(" ") == true) {
            binding.firstNameEt.error = "Username cannot contain spaces."
            binding.saveButton.isEnabled = false
            binding.saveButton.alpha = 0.5f
        }
        if (bioCurrentLength > bioMaxLength) {
            binding.bioEt.error = "Bio can be up to 200 characters long."
            binding.saveButton.isEnabled = false
            binding.saveButton.alpha = 0.5f
        }
        if (userName == "Anonymous") {
            binding.firstNameEt.error = "Username cannot be \"Anonymous\"."
            binding.saveButton.isEnabled = false
            binding.saveButton.alpha = 0.5f
        }
        if (userName?.contains("\n") == true) {
            binding.firstNameEt.error = "Username cannot contain line breaks."
            binding.saveButton.isEnabled = false
            binding.saveButton.alpha = 0.5f
        }
    }

    private fun openImageFiles() {
        val intent = Intent()
        intent.action = Intent.ACTION_GET_CONTENT
        intent.type = "image/*"
        startActivityForResult(intent, 1)
    }

    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                READ_STORAGE_PERMISSION_CODE
            )
        } else {
            openImageFiles()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            READ_STORAGE_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openImageFiles()
                } else {
                    Toast.makeText(requireContext(), "Access to files denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun removeProfilePhoto() {
        val defaultImageResource = R.drawable.empty_profile_photo
        binding.profileImage.setImageResource(defaultImageResource)
        selectedImg = Uri.EMPTY
        isProfilePhotoRemoved = true
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