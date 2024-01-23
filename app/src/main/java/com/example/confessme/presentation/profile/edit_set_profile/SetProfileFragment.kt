package com.example.confessme.presentation.profile.edit_set_profile
import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.example.confessme.R
import com.example.confessme.databinding.FragmentSetProfileBinding
import com.example.confessme.presentation.utils.ConfessMeDialog
import com.example.confessme.presentation.profile.ProfileViewModel
import com.example.confessme.presentation.utils.FragmentNavigation
import com.example.confessme.presentation.search.SearchFragment
import com.example.confessme.utils.MyUtils.disable
import com.example.confessme.utils.MyUtils.enable
import com.example.confessme.presentation.utils.ShareHelper
import com.example.confessme.presentation.utils.UiState
import com.example.confessme.utils.MyUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SetProfileFragment : Fragment() {

    private lateinit var binding: FragmentSetProfileBinding
    private lateinit var selectedImg: Uri
    private lateinit var navRegister: FragmentNavigation
    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var currentUsername: String
    private lateinit var currentBio: String
    private lateinit var sharedUsername: String
    private lateinit var shareHelper: ShareHelper
    private lateinit var dialogHelper: ConfessMeDialog
    private lateinit var currentImageUrl: String
    private val userNameMaxLength = 30
    private val userNameMinLength = 3
    private val bioMaxLength = 200
    private var userNameCurrentLength = 0
    private var userName = ""
    private var bio = ""
    private var bioCurrentLength = 0
    private var isUserNameEmpty: Boolean? = false
    private val maxLines = 3
    private val defaultLines = 3
    private var isProfilePhotoRemoved: Boolean = false
    private var isProfilePhotoChanged: Boolean = false
    private val READ_STORAGE_PERMISSION_CODE = 101

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSetProfileBinding.inflate(inflater, container, false)
        (activity as AppCompatActivity?)!!.title = getString(R.string.set_your_profile)
        (activity as AppCompatActivity?)!!.setSupportActionBar(binding.setProfileToolbar)
        navRegister = activity as FragmentNavigation
        dialogHelper = ConfessMeDialog(requireContext())
        shareHelper = ShareHelper(requireContext())
        setHasOptionsMenu(true)

        setWelcomeAnimation()
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
                    binding.progressBarSetProfile.visibility = View.VISIBLE
                    setInputsEnabled(false)
                }

                is UiState.Failure -> {
                    binding.progressBarSetProfile.visibility = View.GONE
                    setInputsEnabled(true)
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarSetProfile.visibility = View.GONE
                    setInputsEnabled(true)
                    val userProfile = state.data
                    if (userProfile != null) {
                        currentUsername = userProfile.userName
                        currentBio = userProfile.bio
                        currentImageUrl = userProfile.imageUrl
                        binding.setFirstNameEt.setText(userProfile.userName)
                        binding.setBioEt.setText(userProfile.bio)
                        if (userProfile.imageUrl.isNotEmpty()) {
                            Glide.with(requireContext())
                                .load(userProfile.imageUrl)
                                .into(binding.setProfileImage)
                        }
                        sharedUsername = currentUsername
                    }
                }
            }
        }
    }

    private fun observeUpdateProfile() {
        viewModel.updateProfileState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    setInputsEnabled(false)
                    binding.progressBarSetProfile.visibility = View.VISIBLE
                }

                is UiState.Failure -> {
                    binding.progressBarSetProfile.visibility = View.GONE
                    setInputsEnabled(true)
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
                        .replace(R.id.coordinator, SearchFragment())
                        .commit()
                    dialogHelper.showDialog(
                        getString(R.string.share_profile),
                        getString(R.string.share_your_profile_with_your_friends_for),
                        getString(R.string.share),
                        getString(R.string.maybe_later),
                        { shareProfile() })
                }
            }
        }
    }

    private fun setOnClickListeners() {
        binding.setSaveButton.setOnClickListener {
            val username = binding.setFirstNameEt.text?.trim().toString()
            val bio = binding.setBioEt.text?.trim().toString()
            val cleanedBio = bio.replace("\\s+".toRegex(), " ")
            sharedUsername = username

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

        binding.setButton.setOnClickListener {
            onEditProfilePhotoClick()
        }

        binding.setSkipButton.setOnClickListener {
            val fragmentManager = parentFragmentManager
            for (i in 0 until fragmentManager.backStackEntryCount) {
                fragmentManager.popBackStack()
            }
            fragmentManager.beginTransaction()
                .replace(R.id.coordinator, SearchFragment())
                .commit()
            dialogHelper.showDialog(
                getString(R.string.share_profile),
                getString(R.string.share_your_profile_with_your_friends_for),
                getString(R.string.share),
                getString(R.string.maybe_later),
                { shareProfile() })
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
        binding.setFirstNameEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                isUserNameEmpty = s?.isEmpty()
                userNameCurrentLength = s?.trim()?.length ?: 0
                userName = s.toString()

                binding.setProfileFirstNameCounterTextView.text = "$userNameCurrentLength/$userNameMaxLength"
                binding.setSaveButton.enable()

                if (userNameCurrentLength > userNameMaxLength) {
                    binding.setProfileFirstNameCounterTextView.setTextColor(Color.RED)
                } else {
                    binding.setProfileFirstNameCounterTextView.setTextColor(Color.parseColor("#b6b6b6"))
                }

                checkIfUserNameAndBioValid(bioCurrentLength, userNameCurrentLength, userNameMaxLength,
                    userNameMinLength, isUserNameEmpty, bioMaxLength, userName, bio)

            }

            override fun afterTextChanged(s: Editable?) {
            }
        })

        binding.setBioEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                bio = s?.trim().toString().replace("\\s+".toRegex(), " ")
                bioCurrentLength = bio.length

                binding.setProfileCounterTextView.text = "$bioCurrentLength/$bioMaxLength"
                binding.setSaveButton.enable()

                if (bioCurrentLength > bioMaxLength) {
                    binding.setProfileCounterTextView.setTextColor(Color.RED)
                } else {
                    binding.setProfileCounterTextView.setTextColor(Color.parseColor("#b6b6b6"))
                }

                checkIfUserNameAndBioValid(bioCurrentLength, userNameCurrentLength, userNameMaxLength,
                    userNameMinLength, isUserNameEmpty, bioMaxLength, userName, bio)
            }

            override fun afterTextChanged(s: Editable?) {
                val editText = binding.setBioEt
                val lineCount = editText.lineCount

                if (lineCount > maxLines) {
                    editText.setLines(lineCount)
                } else if (lineCount <= defaultLines) {
                    editText.setLines(defaultLines)
                }
            }
        })
    }

    private fun checkIfUserNameAndBioValid(bioCurrentLength: Int, userNameCurrentLength: Int, userNameMaxLength: Int,
                                           userNameMinLength: Int, isUserNameEmpty: Boolean?, bioMaxLength: Int,
                                           userName: String?, bio: String?) {
        if (userNameCurrentLength > userNameMaxLength) {
            binding.setFirstNameEt.error = getString(R.string.username_can_be_up_to_30_characters_long)
            binding.setSaveButton.disable()
        }
        if(userNameCurrentLength < userNameMinLength) {
            binding.setFirstNameEt.error = getString(R.string.username_must_be_more_than_3_characters)
            binding.setSaveButton.disable()
        }
        if(isUserNameEmpty == true) {
            binding.setFirstNameEt.error = getString(R.string.username_cannot_be_empty)
            binding.setSaveButton.disable()
        }
        if (!MyUtils.isUserNameValid(userName)) {
            binding.setFirstNameEt.error = getString(R.string.username_invalid_characters)
            binding.setSaveButton.disable()
        }
        if (bioCurrentLength > bioMaxLength) {
            binding.setBioEt.error = getString(R.string.bio_can_be_up_to_200_characters_long)
            binding.setSaveButton.disable()
        }
        if(userName == "Anonymous") {
            binding.setFirstNameEt.error = getString(R.string.username_cannot_be_anonymous)
            binding.setSaveButton.disable()
        }
        if (userName?.equals(currentUsername) == true && (bio.equals(currentBio))
            && !isProfilePhotoChanged && !isProfilePhotoRemoved
        ) {
            binding.setSaveButton.disable()
        }
    }

    private fun setInputsEnabled(enabled: Boolean) {
        if (enabled) {
            binding.setSaveButton.enable()
            binding.setSkipButton.enable()
            binding.setButton.enable()
            binding.setFirstNameEt.enable()
            binding.setBioEt.enable()
        } else {
            binding.setSaveButton.disable()
            binding.setSkipButton.disable()
            binding.setButton.disable()
            binding.setFirstNameEt.disable()
            binding.setBioEt.disable()
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
        binding.setProfileImage.setImageResource(defaultImageResource)
        selectedImg = Uri.EMPTY
        isProfilePhotoRemoved = true
        binding.setSaveButton.enable()
        checkIfUserNameAndBioValid(bioCurrentLength, userNameCurrentLength, userNameMaxLength,
            userNameMinLength, isUserNameEmpty, bioMaxLength, userName, bio)
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

    private fun shareProfile() {
        if(!sharedUsername.isNullOrEmpty()) {
            shareHelper.shareImage(sharedUsername)
        } else {
            Toast.makeText(context, getString(R.string.share_error), Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setOutsideTouchListener() {
        val rootLayout = binding.root
        rootLayout.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val inputMethodManager = requireContext().getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(rootLayout.windowToken, 0)

                binding.setFirstNameEt.clearFocus()
                binding.setBioEt.clearFocus()
            }
            false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (data != null) {
            if (data.data != null) {
                selectedImg = data.data!!
                binding.setProfileImage.setImageURI(selectedImg)
                isProfilePhotoChanged = true
                binding.setSaveButton.enable()
                checkIfUserNameAndBioValid(bioCurrentLength, userNameCurrentLength, userNameMaxLength,
                    userNameMinLength, isUserNameEmpty, bioMaxLength, userName, bio)
            }
        }
    }
}