package com.example.confessme.presentation.ui

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.TextUtils
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.example.confessme.R
import com.example.confessme.data.model.Confession
import com.example.confessme.databinding.FragmentConfessAnswerBinding
import com.example.confessme.presentation.ConfessViewModel
import com.example.confessme.presentation.DeleteDialog
import com.example.confessme.util.UiState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ConfessAnswerFragment(
    private val onUpdateItem: (position: Int, updatedConfession: Confession) -> Unit,
    private val findItemById: (confessionId: String) -> Int
) : DialogFragment() {

    private lateinit var binding: FragmentConfessAnswerBinding
    private lateinit var navRegister: FragmentNavigation
    private val viewModel: ConfessViewModel by viewModels()
    private var isAnswerButtonEnabled = false
    private var isEditAnswer: Boolean = false
    private lateinit var currentUserUid: String
    private lateinit var answerUserUid: String
    private lateinit var answerFromUserUid: String
    private var isAnswerFavorited: Boolean = false
    private var isConfessionAnswered: Boolean = false
    private var answerDate: String = ""
    private lateinit var fromUserImageUrl: String
    private lateinit var answeredUserName: String
    private lateinit var confessedUserName: String
    private lateinit var answerText: String
    private lateinit var dialogHelper: DeleteDialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentConfessAnswerBinding.inflate(inflater, container, false)
        navRegister = activity as FragmentNavigation
        setHasOptionsMenu(true)
        isConfessionAnswered = arguments?.getBoolean("isAnswered", false) ?: false
        answerText = arguments?.getString("answerText", "") ?: ""
        currentUserUid = arguments?.getString("currentUserUid", "") ?: ""
        answerUserUid = arguments?.getString("answerUserUid", "") ?: ""
        answerFromUserUid = arguments?.getString("answerFromUserUid", "") ?: ""
        fromUserImageUrl = arguments?.getString("fromUserImageUrl", "") ?: ""
        answeredUserName = arguments?.getString("answeredUserName", "") ?: ""
        confessedUserName = arguments?.getString("confessedUserName", "") ?: ""
        isAnswerFavorited = arguments?.getBoolean("favorited", false) ?: false
        answerDate = arguments?.getString("answerDate", "") ?: ""
        dialogHelper = DeleteDialog(requireContext())

        setUserImage()
        setSaveButton()
        setImageAndTextStates()
        setFavoriteDeleteEditReplyStates()
        observeAddAnswer()
        observeFavorite()
        observeDeleteAnswer()

        return binding.root
    }

    private fun setUserImage() {
        if (fromUserImageUrl.isNotEmpty()) {
            Glide.with(requireContext())
                .load(fromUserImageUrl)
                .into(binding.answerScreenProfileImage)
        } else {
            binding.answerScreenProfileImage.setImageResource(R.drawable.empty_profile_photo)
        }
    }

    private fun observeFavorite() {
        viewModel.addFavoriteAnswer.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.answerIcFavorite.visibility = View.INVISIBLE
                    binding.progressBarAnswerFavorite.visibility = View.VISIBLE
                }

                is UiState.Failure -> {
                    binding.answerIcFavorite.visibility = View.VISIBLE
                    binding.progressBarAnswerFavorite.visibility = View.GONE
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.answerIcFavorite.visibility = View.VISIBLE
                    binding.progressBarAnswerFavorite.visibility = View.GONE
                    val updatedConfession = state.data
                    val position = updatedConfession?.let { findItemById(it.id) }

                    if (position != -1) {
                        if (updatedConfession != null) {
                            if (position != null) {
                                onUpdateItem(position, updatedConfession)
                            }
                        }
                    }

                    if(state.data?.answer?.favorited == true) {
                        binding.answerIcFavorite.setColorFilter(resources.getColor(R.color.confessmered))
                    } else {
                        binding.answerIcFavorite.setColorFilter(Color.parseColor("#B8B8B8"))
                    }
                }
            }
        }
    }

    private fun observeAddAnswer() {
        viewModel.addAnswerState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.replyButton.isEnabled = false
                    binding.replyButton.alpha = 0.5f
                    binding.progressBarConfessAnswer.visibility = View.VISIBLE
                }

                is UiState.Failure -> {
                    binding.replyButton.isEnabled = true
                    binding.replyButton.alpha = 1f
                    binding.progressBarConfessAnswer.visibility = View.GONE
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarConfessAnswer.visibility = View.GONE
                    dismiss()
                    val updatedConfession = state.data
                    val position = updatedConfession?.let { findItemById(it.id) }

                    if (position != -1) {
                        if (updatedConfession != null) {
                            if (position != null) {
                                onUpdateItem(position, updatedConfession)
                            }
                        }
                    }

                    Toast.makeText(requireContext(), "Answered successfully", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    private fun observeDeleteAnswer() {
        viewModel.deleteAnswerState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBarConfessAnswer.visibility = View.VISIBLE
                }

                is UiState.Failure -> {
                    binding.progressBarConfessAnswer.visibility = View.GONE
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarConfessAnswer.visibility = View.GONE
                    dismiss()
                    val updatedConfession = state.data

                    val position = updatedConfession?.let { findItemById(it.id) }

                    if (position != -1) {
                        if (updatedConfession != null) {
                            if (position != null) {
                                onUpdateItem(position, updatedConfession)
                            }
                        }
                    }

                    Toast.makeText(
                        requireContext(),
                        "Answer deleted successfully",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
            }
        }
    }

    private fun setFavoriteDeleteEditReplyStates() {
        val confessionId = arguments?.getString("confessionId", "")

        binding.replyButton.setOnClickListener {
            val answerEditText = binding.confessAnswerEditText.text.toString()

             viewModel.addAnswer(confessionId ?: "", answerEditText)
        }

        binding.answerIcEdit.setOnClickListener {
            binding.confessAnswerTextView.visibility = View.GONE
            binding.answerScreenProfileImage.visibility = View.GONE
            binding.confessAnswerUserNameAndDate.visibility = View.GONE
            binding.replyButton.visibility = View.VISIBLE
            binding.confessAnswerEditText.let {
                it.visibility = View.VISIBLE
                it.setText(answerText)
            }
            isEditAnswer = true
        }

        binding.answerIcFavorite.setOnClickListener {
            isAnswerFavorited = !isAnswerFavorited
            viewModel.addAnswerFavorite(isAnswerFavorited, confessionId ?: "")
        }

        binding.answerIcDelete.setOnClickListener {
            dialogHelper.showDialog(
                "delete answer",
                "Are you sure you really want to delete this answer?",
                { viewModel.deleteAnswer(confessionId ?: "") }
            )
        }

        if(currentUserUid == answerFromUserUid) {
            binding.replyButton.visibility = View.GONE
            binding.answerIcEdit.visibility = View.GONE
            binding.answerIcFavorite.visibility = View.VISIBLE
            binding.answerIcDelete.visibility = View.GONE

            if (isAnswerFavorited) {
                binding.answerIcFavorite.setColorFilter(resources.getColor(R.color.confessmered))
            } else {
                binding.answerIcFavorite.setColorFilter(Color.parseColor("#B8B8B8"))
            }
        } else if(currentUserUid == answerUserUid) {
            binding.answerIcFavorite.isEnabled = false
            binding.answerIcFavorite.alpha = 0.5f

            if (isConfessionAnswered == true && !isEditAnswer) {
                binding.replyButton.visibility = View.GONE
                binding.answerIcEdit.visibility = View.VISIBLE
                binding.answerIcFavorite.visibility = View.VISIBLE
                binding.answerIcDelete.visibility = View.VISIBLE
                if (isAnswerFavorited) {
                    binding.answerIcFavorite.setColorFilter(resources.getColor(R.color.confessmered))
                } else {
                    binding.answerIcFavorite.setColorFilter(Color.parseColor("#B8B8B8"))
                }
            } else {
                binding.replyButton.visibility = View.VISIBLE
                binding.answerIcEdit.visibility = View.GONE
                binding.answerIcFavorite.visibility = View.GONE
                binding.answerIcDelete.visibility = View.GONE
                setSaveButton()
            }
        } else {
            binding.replyButton.visibility = View.GONE
            binding.answerIcEdit.visibility = View.GONE
            binding.answerIcFavorite.visibility = View.VISIBLE
            binding.answerIcFavorite.alpha = 0.5f
            binding.answerIcFavorite.isEnabled = false
            binding.answerIcDelete.visibility = View.GONE

            if (isAnswerFavorited) {
                binding.answerIcFavorite.setColorFilter(resources.getColor(R.color.confessmered))
            } else {
                binding.answerIcFavorite.setColorFilter(Color.parseColor("#B8B8B8"))
            }
        }
    }

    private fun setImageAndTextStates() {

        if (isConfessionAnswered == true && !isEditAnswer) {
            binding.confessAnswerEditText.visibility = View.GONE
            binding.confessAnswerTextView.visibility = View.VISIBLE
            binding.confessAnswerUserNameAndDate.visibility = View.VISIBLE
            setUserNameProfileImageAndAnswerText()
            setUsernameAndDateText()
        } else {
            binding.confessAnswerEditText.visibility = View.VISIBLE
            binding.confessAnswerTextView.visibility = View.GONE
            binding.answerScreenProfileImage.visibility = View.GONE
        }

        val maxLength = 560
        binding.confessAnswerEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val currentLength = s?.length ?: 0
                val isTextEmpty = s?.trim()?.isEmpty()

                if(isTextEmpty == true) {
                    isAnswerButtonEnabled = false
                    setSaveButton()
                }
                else if (currentLength > maxLength) {
                    isAnswerButtonEnabled = false
                    setSaveButton()
                    binding.confessAnswerEditText.error = "Confession is too long (max $maxLength characters)"
                } else {
                    binding.confessAnswerEditText.error = null
                    isAnswerButtonEnabled = true
                    setSaveButton()
                }
                /*
                                if (currentLength > maxLength) {
                                    binding.confessAnswerEditText.error = "Character limit exceeded"
                                    binding.replyButton.alpha = 0.5f
                                    binding.replyButton.isClickable = false
                                } else {
                                    binding.confessAnswerEditText.error = null
                                    binding.replyButton.isClickable = true
                                    binding.replyButton.alpha = 1f
                                }
                */

                if(s.toString() == answerText) {
                    isAnswerButtonEnabled = false
                    setSaveButton()
                }
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })
    }

    private fun setSaveButton() {
        if(isAnswerButtonEnabled) {
            binding.replyButton.alpha = 1f
            binding.replyButton.isClickable = true
        } else {
            binding.replyButton.alpha = 0.5f
            binding.replyButton.isClickable = false
        }
    }

    private fun setUsernameAndDateText() {
        val answeredUserNameBold = SpannableString(answeredUserName)
        answeredUserNameBold.setSpan(StyleSpan(Typeface.BOLD), 0, answeredUserName.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        val answerDateBold = SpannableString(answerDate)
        val usernameAndDateText = TextUtils.concat(answeredUserNameBold, " · ", answerDateBold)
        binding.confessAnswerUserNameAndDate.text = usernameAndDateText
    }

    private fun setUserNameProfileImageAndAnswerText() {
        val confessedUserNameBold = SpannableString("@$confessedUserName")
        confessedUserNameBold.setSpan(StyleSpan(Typeface.BOLD), 0, confessedUserName.length + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        confessedUserNameBold.setSpan(ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.confessmered)), 0, confessedUserName.length + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(view: View) {
                if (currentUserUid != answerFromUserUid) {
                    val bundle = Bundle()
                    bundle.putString("userUid", answerFromUserUid)

                    val profileFragment = OtherUserProfileFragment()
                    profileFragment.arguments = bundle

                    dismiss()
                    navRegister.navigateFrag(profileFragment, true)
                }
            }

            override fun updateDrawState(ds: TextPaint) {
                ds.color = ContextCompat.getColor(requireContext(), R.color.confessmered)
                ds.isUnderlineText = false
            }
        }
        confessedUserNameBold.setSpan(clickableSpan, 0, confessedUserName.length + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        val usernameAndAnswerText = TextUtils.concat(confessedUserNameBold, " ", answerText)

        binding.confessAnswerTextView.text = usernameAndAnswerText
        binding.confessAnswerTextView.movementMethod = LinkMovementMethod.getInstance()
        binding.confessAnswerTextView.highlightColor = Color.TRANSPARENT

        binding.answerScreenProfileImage.setOnClickListener {
            if (currentUserUid != answerUserUid) {
                val bundle = Bundle()
                bundle.putString("userUid", answerUserUid)

                val profileFragment = OtherUserProfileFragment()
                profileFragment.arguments = bundle

                dismiss()
                navRegister.navigateFrag(profileFragment, true)
            }
        }
    }
}