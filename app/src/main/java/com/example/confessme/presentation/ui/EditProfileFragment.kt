package com.example.confessme.presentation.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
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
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
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
        (activity as AppCompatActivity?)!!.title = getString(R.string.edit_profile)
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
        setOutsideTouchListener()

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
        val options = arrayOf(getString(R.string.remove_profile_photo), getString(R.string.change_profile_photo))

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

                binding.editProfileFirstNameCounterTextView.text = "$userNameCurrentLength/$userNameMaxLength"
                binding.saveButton.isEnabled = true
                binding.saveButton.alpha = 1f

                if (userNameCurrentLength > userNameMaxLength) {
                    binding.editProfileFirstNameCounterTextView.setTextColor(Color.RED)
                } else {
                    binding.editProfileFirstNameCounterTextView.setTextColor(Color.parseColor("#b6b6b6"))
                }

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
                val bio = s?.trim().toString().replace("\\s+".toRegex(), " ")
                bioCurrentLength = bio.length

                binding.editProfileCounterTextView.text = "$bioCurrentLength/$bioMaxLength"
                binding.saveButton.isEnabled = true
                binding.saveButton.alpha = 1f

                if (bioCurrentLength > bioMaxLength) {
                    binding.editProfileCounterTextView.setTextColor(Color.RED)
                } else {
                    binding.editProfileCounterTextView.setTextColor(Color.parseColor("#b6b6b6"))
                }

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
            binding.firstNameEt.error = getString(R.string.username_can_be_up_to_30_characters_long)
            binding.saveButton.isEnabled = false
            binding.saveButton.alpha = 0.5f
        }
        if (userNameCurrentLength < userNameMinLength) {
            binding.firstNameEt.error = getString(R.string.username_must_be_more_than_3_characters)
            binding.saveButton.isEnabled = false
            binding.saveButton.alpha = 0.5f
        }
        if (isUserNameEmpty == true) {
            binding.firstNameEt.error = getString(R.string.username_cannot_be_empty)
            binding.saveButton.isEnabled = false
            binding.saveButton.alpha = 0.5f
        }
        if (userName?.contains(" ") == true) {
            binding.firstNameEt.error = getString(R.string.username_cannot_contain_spaces)
            binding.saveButton.isEnabled = false
            binding.saveButton.alpha = 0.5f
        }
        if (bioCurrentLength > bioMaxLength) {
            binding.bioEt.error = getString(R.string.bio_can_be_up_to_200_characters_long)
            binding.saveButton.isEnabled = false
            binding.saveButton.alpha = 0.5f
        }
        if (userName == "Anonymous") {
            binding.firstNameEt.error = getString(R.string.username_cannot_be_anonymous)
            binding.saveButton.isEnabled = false
            binding.saveButton.alpha = 0.5f
        }
        if (userName?.contains("\n") == true) {
            binding.firstNameEt.error = getString(R.string.username_cannot_contain_line_breaks)
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
                    Toast.makeText(requireContext(), getString(R.string.access_to_files_denied), Toast.LENGTH_SHORT).show()
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

    @SuppressLint("ClickableViewAccessibility")
    private fun setOutsideTouchListener() {
        val rootLayout = binding.root
        rootLayout.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val inputMethodManager = requireContext().getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(rootLayout.windowToken, 0)

                binding.firstNameEt.clearFocus()
                binding.bioEt.clearFocus()
            }
            false
        }
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