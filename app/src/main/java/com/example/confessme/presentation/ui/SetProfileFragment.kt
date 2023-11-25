package com.example.confessme.presentation.ui
import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
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
import com.example.confessme.databinding.FragmentSetProfileBinding
import com.example.confessme.presentation.LoginViewModel
import com.example.confessme.presentation.ProfileViewModel
import com.example.confessme.util.ProfilePhotoAction
import com.example.confessme.util.UiState
import com.google.common.collect.ComparisonChain.start
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SetProfileFragment : Fragment() {

    private lateinit var binding: FragmentSetProfileBinding
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
        binding = FragmentSetProfileBinding.inflate(inflater, container, false)
        (activity as AppCompatActivity?)!!.title = "Set Your Profile"
        (activity as AppCompatActivity?)!!.setSupportActionBar(binding.setProfileToolbar)
        navRegister = activity as FragmentNavigation
        setHasOptionsMenu(true)

        setWelcomeAnimation()
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
                    binding.progressBarSetProfile.visibility = View.VISIBLE
                }

                is UiState.Failure -> {
                    binding.progressBarSetProfile.visibility = View.GONE
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarSetProfile.visibility = View.GONE
                    val userProfile = state.data
                    if (userProfile != null) {
                        currentUsername = userProfile.userName
                        currentImageUrl = userProfile.imageUrl
                        binding.setFirstNameEt.setText(userProfile.userName)
                        binding.setBioEt.setText(userProfile.bio)
                        if (userProfile.imageUrl.isNotEmpty()) {
                            Glide.with(requireContext())
                                .load(userProfile.imageUrl)
                                .into(binding.setProfileImage)
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
                    binding.setSaveButton.isEnabled = false
                    binding.setSaveButton.alpha = 0.5f
                    binding.setSkipButton.isEnabled = false
                    binding.setSkipButton.alpha = 0.5f
                    binding.setButton.isEnabled = false
                    binding.setButton.alpha = 0.5f
                    binding.setFirstNameEt.isEnabled = false
                    binding.setFirstNameEt.alpha = 0.5f
                    binding.setBioEt.isEnabled = false
                    binding.setBioEt.alpha = 0.5f
                    binding.progressBarSetProfile.visibility = View.VISIBLE
                }

                is UiState.Failure -> {
                    binding.progressBarSetProfile.visibility = View.GONE
                    binding.setSaveButton.isEnabled = true
                    binding.setSaveButton.alpha = 1f
                    binding.setSkipButton.isEnabled = true
                    binding.setSkipButton.alpha = 1f
                    binding.setButton.isEnabled = true
                    binding.setButton.alpha = 1f
                    binding.setFirstNameEt.isEnabled = true
                    binding.setFirstNameEt.alpha = 1f
                    binding.setBioEt.isEnabled = true
                    binding.setBioEt.alpha = 1f
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarSetProfile.visibility = View.GONE
                    val fragmentManager = parentFragmentManager
                    for (i in 0 until fragmentManager.backStackEntryCount) {
                        fragmentManager.popBackStack()
                    }
                    fragmentManager.beginTransaction()
                        .replace(R.id.coordinator, HomeFragment())
                        .commit()
                }
            }
        }
    }

    private fun setOnClickListeners() {
        binding.setSaveButton.setOnClickListener {
            val username = binding.setFirstNameEt.text?.trim().toString()
            val bio = binding.setBioEt.text.toString()

            if (::selectedImg.isInitialized && selectedImg != Uri.EMPTY) {
                viewModel.updateProfile(currentUsername, currentImageUrl, username, bio, selectedImg, ProfilePhotoAction.CHANGE)
            } else if (isProfilePhotoRemoved) {
                viewModel.updateProfile(
                    currentUsername,
                    currentImageUrl,
                    username,
                    bio,
                    Uri.EMPTY,
                    ProfilePhotoAction.REMOVE
                )
            } else {
                viewModel.updateProfile(
                    currentUsername,
                    currentImageUrl,
                    username,
                    bio,
                    Uri.EMPTY,
                    ProfilePhotoAction.DO_NOT_CHANGE
                )
            }
        }

        binding.setButton.setOnClickListener {
            onEditProfilePhotoClick()
        }

        binding.setSkipButton.setOnClickListener {
            val fragmentManager = parentFragmentManager
            for (i in 0 until fragmentManager.backStackEntryCount) {
                fragmentManager.popBackStack()
            }
            fragmentManager.beginTransaction()
                .replace(R.id.coordinator, HomeFragment())
                .commit()
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

        binding.setFirstNameEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                isUserNameEmpty = s?.isEmpty()
                userNameCurrentLength = s?.length ?: 0
                userName = s.toString()

                binding.setSaveButton.isEnabled = true
                binding.setSaveButton.alpha = 1f

                checkIfUserNameAndBioValid(bioCurrentLength, userNameCurrentLength, userNameMaxLength,
                    userNameMinLength, isUserNameEmpty, bioMaxLength, userName)

            }

            override fun afterTextChanged(s: Editable?) {
            }
        })

        binding.setBioEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                bioCurrentLength = s?.length ?: 0

                binding.setSaveButton.isEnabled = true
                binding.setSaveButton.alpha = 1f

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
            binding.setFirstNameEt.error = "Username can be up to 30 characters long."
            binding.setSaveButton.isEnabled = false
            binding.setSaveButton.alpha = 0.5f
        }
        if(userNameCurrentLength < userNameMinLength) {
            binding.setFirstNameEt.error = "Username must be more than 3 characters."
            binding.setSaveButton.isEnabled = false
            binding.setSaveButton.alpha = 0.5f
        }
        if(isUserNameEmpty == true) {
            binding.setFirstNameEt.error = "Username cannot be empty."
            binding.setSaveButton.isEnabled = false
            binding.setSaveButton.alpha = 0.5f
        }
        if(userName?.contains(" ") == true) {
            binding.setFirstNameEt.error = "Username cannot contain spaces."
            binding.setSaveButton.isEnabled = false
            binding.setSaveButton.alpha = 0.5f
        }
        if (bioCurrentLength > bioMaxLength) {
            binding.setBioEt.error = "Bio can be up to 200 characters long."
            binding.setSaveButton.isEnabled = false
            binding.setSaveButton.alpha = 0.5f
        }
        if(userName == "Anonymous") {
            binding.setFirstNameEt.error = "Username cannot be \"Anonymous\"."
            binding.setSaveButton.isEnabled = false
            binding.setSaveButton.alpha = 0.5f
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
        binding.setProfileImage.setImageResource(defaultImageResource)
        selectedImg = Uri.EMPTY
        isProfilePhotoRemoved = true
    }

    private fun setWelcomeAnimation() {
        val welcomeTextView = binding.welcomeTextview
        val confessMeTextView = binding.confessmeTextview

        val animDuration = 1000L

        val animSet = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(welcomeTextView, "translationX", -100f, 0f),
                ObjectAnimator.ofFloat(confessMeTextView, "translationX", -100f, 0f),
                ObjectAnimator.ofFloat(welcomeTextView, "alpha", 0f, 1f),
                ObjectAnimator.ofFloat(confessMeTextView, "alpha", 0f, 1f)
            )
            duration = animDuration
            start()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (data != null) {
            if (data.data != null) {
                selectedImg = data.data!!
                binding.setProfileImage.setImageURI(selectedImg)
            }
        }
    }
}