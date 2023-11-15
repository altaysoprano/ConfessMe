package com.example.confessme.presentation.ui

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
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
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
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
import com.example.confessme.presentation.ConfessMeDialog
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
    private var isAnswerFavorited: Boolean = false
    private lateinit var currentUserUid: String
    private lateinit var confessionId: String
    private lateinit var answerDate: String
    private lateinit var dialogHelper: ConfessMeDialog

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentConfessAnswerBinding.inflate(inflater, container, false)
        navRegister = activity as FragmentNavigation
        setHasOptionsMenu(true)
        confessionId = arguments?.getString("confessionId", "") ?: ""
        currentUserUid = arguments?.getString("currentUserUid", "") ?: ""
        answerDate = arguments?.getString("answerDate", "") ?: ""
        dialogHelper = ConfessMeDialog(requireContext())

        getConfession(confessionId)

        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        observeGetConfession()
        observeAddAnswer()
        observeFavorite()
        observeDeleteAnswer()
    }

    private fun observeGetConfession() {
        viewModel.getConfessionState.observe(this) { state->
            when(state) {
                is UiState.Loading -> {
                    binding.progressBarConfessAnswer.visibility = View.VISIBLE
                    binding.answerIcFavorite.visibility = View.INVISIBLE
                    binding.answerIcDelete.visibility = View.INVISIBLE
                    binding.answerIcEdit.visibility = View.INVISIBLE
                    binding.replyButton.visibility = View.INVISIBLE
                    binding.answerScreenProfileImage.visibility = View.INVISIBLE
                }

                is UiState.Failure -> {
                    binding.progressBarConfessAnswer.visibility = View.GONE
                }

                is UiState.Success -> {
                    binding.progressBarConfessAnswer.visibility = View.GONE

                    val answerText = state.data?.answer?.text ?: ""
                    isAnswerFavorited = state.data?.answer?.favorited ?: false
                    val answerFromUserUid = state.data?.fromUserId ?: ""
                    val answerUserUid = state.data?.userId ?: ""
                    val isConfessionAnswered = state.data?.answered ?: false
                    val answeredUserName = state.data?.answer?.fromUserUsername ?: ""
                    val confessedUserName = state.data?.answer?.username ?: ""

                    setUserImage(state.data?.answer?.fromUserImageUrl)
                    setSaveButton()
                    setImageAndTextStates(isConfessionAnswered, answerText, answeredUserName,
                        answerDate, answerUserUid, answerFromUserUid,
                        confessedUserName)
                    setFavoriteDeleteEditReplyStates(
                        answerText,
                        answerFromUserUid,
                        answerUserUid,
                        isConfessionAnswered)
                    setFavorite(isAnswerFavorited)
                }
            }
        }
    }

    private fun observeFavorite() {
        viewModel.addFavoriteAnswer.observe(this) { state ->
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

                    setFavorite(updatedConfession?.answer?.favorited)
                }
            }
        }
    }

    private fun getConfession(confessionId: String) {
        viewModel.getConfession(confessionId)
    }

    private fun observeAddAnswer() {
        viewModel.addAnswerState.observe(this) { state ->
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
        viewModel.deleteAnswerState.observe(this) { state ->
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

    private fun setUserImage(fromUserImageUrl: String?) {
        if (fromUserImageUrl?.isNotEmpty() == true) {
            Glide.with(requireContext())
                .load(fromUserImageUrl)
                .into(binding.answerScreenProfileImage)
        } else {
            binding.answerScreenProfileImage.setImageResource(R.drawable.empty_profile_photo)
        }
    }

    private fun setFavorite(favorited: Boolean?) {
        if(favorited == true) {
            binding.answerIcFavorite.setColorFilter(resources.getColor(R.color.confessmered))
        } else {
            binding.answerIcFavorite.setColorFilter(Color.parseColor("#B8B8B8"))
        }
    }

    private fun setFavoriteDeleteEditReplyStates(answerText: String, answerFromUserUid: String,
                                                    answerUserUid: String, isConfessionAnswered: Boolean) {
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

    private fun setImageAndTextStates(isConfessionAnswered: Boolean, answerText: String, answeredUserName: String,
                                        answerDate: String, answerUserUid: String, answerFromUserUid: String,
                                      confessedUserName: String) {

        if (isConfessionAnswered == true && !isEditAnswer) {
            binding.answerScreenProfileImage.visibility = View.VISIBLE
            binding.confessAnswerEditText.visibility = View.GONE
            binding.confessAnswerTextView.visibility = View.VISIBLE
            binding.confessAnswerUserNameAndDate.visibility = View.VISIBLE
            setUserNameProfileImageAndAnswerText(answerUserUid, answerFromUserUid, confessedUserName, answerText)
            setUsernameAndDateText(answeredUserName, answerDate)
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

    private fun setUsernameAndDateText(answeredUserName: String, answerDate: String) {
        val answeredUserNameBold = SpannableString(answeredUserName)
        answeredUserNameBold.setSpan(StyleSpan(Typeface.BOLD), 0, answeredUserName.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        val answerDateBold = SpannableString(answerDate)
        val usernameAndDateText = TextUtils.concat(answeredUserNameBold, " · ", answerDateBold)
        binding.confessAnswerUserNameAndDate.text = usernameAndDateText
    }

    private fun setUserNameProfileImageAndAnswerText(answerUserUid: String, answerFromUserUid: String,
                                                     confessedUserName: String, answerText: String) {
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