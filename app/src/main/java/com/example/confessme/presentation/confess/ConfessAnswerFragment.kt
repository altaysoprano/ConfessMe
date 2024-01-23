package com.example.confessme.presentation.confess

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
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
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.example.confessme.R
import com.example.confessme.data.model.Confession
import com.example.confessme.databinding.FragmentConfessAnswerBinding
import com.example.confessme.presentation.utils.ConfessMeDialog
import com.example.confessme.presentation.utils.FragmentNavigation
import com.example.confessme.presentation.profile.other_user_profile.OtherUserProfileFragment
import com.example.confessme.utils.MyUtils
import com.example.confessme.presentation.utils.ShareHelper
import com.example.confessme.presentation.utils.UiState
import com.example.confessme.utils.MyUtils.disable
import com.example.confessme.utils.MyUtils.enable
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
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
    private var isTextEmpty: Boolean? = true
    private lateinit var currentUserUid: String
    private lateinit var confessionId: String
    private lateinit var dialogHelper: ConfessMeDialog
    private lateinit var answeredUserName: String
    private lateinit var answerUserName: String
    private lateinit var answerFromUsername: String
    private lateinit var shareHelper: ShareHelper
    private var answerDataListener: AnswerDataListener? = null

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
        dialogHelper = ConfessMeDialog(requireContext())
        shareHelper = ShareHelper(requireContext())

        getConfession(confessionId)

        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.AppTheme_Dialog_Custom)
        observeGetConfession()
        observeAddAnswer()
        observeFavorite()
        observeDeleteAnswer()
    }

    override fun onResume() {
        super.onResume()
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun observeGetConfession() {
        viewModel.getConfessionState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBarConfessAnswer.visibility = View.VISIBLE
                    binding.answerConfessionRelativeLayout.visibility = View.INVISIBLE
                    binding.answerIcFavorite.visibility = View.INVISIBLE
                    binding.answerIcDelete.visibility = View.INVISIBLE
                    binding.answerIcEdit.visibility = View.INVISIBLE
                    binding.replyButton.visibility = View.INVISIBLE
                    binding.answerScreenProfileImage.visibility = View.INVISIBLE
                }

                is UiState.Failure -> {
                    binding.progressBarConfessAnswer.visibility = View.GONE
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarConfessAnswer.visibility = View.GONE
                    binding.answerConfessionRelativeLayout.visibility = View.VISIBLE

                    val answerText = state.data?.answer?.text ?: ""
                    val confessionText = state.data?.text ?: ""
                    val confessionUserName = state.data?.fromUserUsername ?: ""
                    val confessionTimestamp = state.data?.timestamp
                    isAnswerFavorited = state.data?.answer?.favorited ?: false
                    val answerFromUserUid = state.data?.fromUserId ?: ""
                    answerFromUsername = state.data?.fromUserUsername ?: ""
                    answerUserName = state.data?.username ?: ""
                    val userToken = state.data?.fromUserToken ?: ""
                    val confessionFromUserUid = state.data?.fromUserId ?: ""
                    val confessionUserUid = state.data?.userId ?: ""
                    val confessionFromUserToken = state.data?.fromUserToken ?: ""
                    val confessionUserToken = state.data?.userToken ?: ""
                    val fromUserToken = state.data?.userToken ?: ""
                    val answerUserUid = state.data?.userId ?: ""
                    val isConfessionAnswered = state.data?.answered ?: false
                    answeredUserName = state.data?.answer?.fromUserUsername ?: ""
                    val answerTimeStamp = state.data?.answer?.timestamp
                    val confessedUserName = state.data?.answer?.username ?: ""
                    val anonymousId = state.data?.anonymousId ?: ""

                    setUsersImages(
                        state.data?.answer?.fromUserImageUrl,
                        state.data?.fromUserImageUrl
                    )
                    setSaveButton()
                    setImageAndTextStates(
                        isConfessionAnswered, answerText, answeredUserName,
                        answerTimeStamp, answerUserUid, answerFromUserUid,
                        answerFromUsername, answerUserName, userToken,
                        fromUserToken, confessedUserName, confessionText,
                        confessionUserName, confessionTimestamp, confessionUserUid,
                        confessionFromUserUid, confessionFromUserToken, confessionUserToken
                    )
                    setFavoriteDeleteEditReplyDismissStates(
                        answerText, answerFromUserUid,
                        answerUserUid, anonymousId, isConfessionAnswered
                    )
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

                    updatedConfession?.answered?.let { answerDataListener?.onAnswerDataReceived(it) }

                    if (updatedConfession != null) {
                        showRepliedSnackbar(updatedConfession.text, updatedConfession.answer.text)
                        if (position != null && position != -1) {
                            onUpdateItem(position, updatedConfession)
                        }
                    }
                }
            }
        }
    }

    fun setDataListener(listener: AnswerDataListener) {
        this.answerDataListener = listener
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

                    updatedConfession?.answered?.let { answerDataListener?.onAnswerDataReceived(it) }

                    if (position != -1) {
                        if (updatedConfession != null) {
                            if (position != null) {
                                onUpdateItem(position, updatedConfession)
                            }
                        }
                    }

                    Toast.makeText(
                        requireContext(),
                        getString(R.string.answer_deleted_successfully),
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
            }
        }
    }

    private fun setUsersImages(fromUserImageUrl: String?, userImageUrl: String?) {
        if (userImageUrl?.isNotEmpty() == true) {
            Glide.with(requireContext())
                .load(userImageUrl)
                .into(binding.answerScreenConfessionProfileImage)
        } else {
            binding.answerScreenConfessionProfileImage.setImageResource(R.drawable.empty_profile_photo)
        }
        if (fromUserImageUrl?.isNotEmpty() == true) {
            Glide.with(requireContext())
                .load(fromUserImageUrl)
                .into(binding.answerScreenProfileImage)
        } else {
            binding.answerScreenProfileImage.setImageResource(R.drawable.empty_profile_photo)
        }
    }

    private fun setFavorite(favorited: Boolean?) {
        if (favorited == true) {
            binding.answerIcFavorite.setColorFilter(resources.getColor(R.color.confessmered))
        } else {
            binding.answerIcFavorite.setColorFilter(Color.parseColor("#B8B8B8"))
        }
    }

    private fun setFavoriteDeleteEditReplyDismissStates(
        answerText: String, answerFromUserUid: String,
        answerUserUid: String, anonymousId: String,
        isConfessionAnswered: Boolean
    ) {
        val confessionId = arguments?.getString("confessionId", "")

        binding.replyButton.setOnClickListener {
            val answerEditText = binding.confessAnswerEditText.text?.trim().toString()

            viewModel.addAnswer(confessionId ?: "", answerEditText)
        }

        binding.answerIcEdit.setOnClickListener {
            setOnEditAnswer(binding.answerIcEdit, answerText)
        }

        binding.answerIcFavorite.setOnClickListener {
            isAnswerFavorited = !isAnswerFavorited
            viewModel.addAnswerFavorite(isAnswerFavorited, confessionId ?: "")
        }

        binding.answerIcDelete.setOnClickListener {
            dialogHelper.showDialog(
                getString(R.string.delete_answer),
                getString(R.string.are_you_sure_you_really_want_to_delete_this_answer),
                getString(R.string.yes),
                getString(R.string.no),
                { viewModel.deleteAnswer(confessionId ?: "") }
            )
        }

        if (currentUserUid == answerFromUserUid || currentUserUid == anonymousId) {
            binding.replyButton.visibility = View.GONE
            binding.answerIcEdit.visibility = View.GONE
            binding.answerIcFavorite.visibility = View.VISIBLE
            binding.answerIcDelete.visibility = View.GONE

            if (isAnswerFavorited) {
                binding.answerIcFavorite.setColorFilter(resources.getColor(R.color.confessmered))
            } else {
                binding.answerIcFavorite.setColorFilter(Color.parseColor("#B8B8B8"))
            }
        } else if (currentUserUid == answerUserUid) {
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

        binding.closeButton.setOnClickListener {
            dismiss()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setImageAndTextStates(
        isConfessionAnswered: Boolean, answerText: String, answeredUserName: String,
        answerTimestamp: Any?, answerUserUid: String, answerFromUserUid: String,
        answerFromUsername: String, answerUserName: String, userToken: String,
        fromUserToken: String, confessedUserName: String, confessionText: String,
        confessionUserName: String, confessionTimeStamp: Any?, confessionUserUid: String,
        confessionFromUserUid: String, confessionFromUserToken: String, confessionUserToken: String
    ) {
        setConfessionTexts(
            confessionUserName, answerUserName, confessionText, confessionTimeStamp,
            confessionUserUid, confessionFromUserUid, confessionFromUserToken, confessionUserToken
        )

        if (isConfessionAnswered == true && !isEditAnswer) {
            binding.answerScreenProfileImage.visibility = View.VISIBLE
            binding.confessAnswerTextInputLayout.visibility = View.GONE
            binding.answerCounterTextView.visibility = View.GONE
            binding.confessAnswerTextView.visibility = View.VISIBLE
            binding.answerScreenAnswerUsernameAndTimestampLayout.visibility = View.VISIBLE
            setUserNameProfileImageAndAnswerText(
                answerUserUid, answerFromUserUid, answerFromUsername, answerUserName,
                userToken, fromUserToken, confessedUserName, answerText
            )
            setAnswerUsernameAndDateText(answeredUserName, answerTimestamp)
        } else {
            binding.confessAnswerTextInputLayout.apply {
                visibility = View.VISIBLE
                requestFocus()
                MyUtils.showKeyboard(requireActivity(), this.findFocus())
            }
            binding.answerCounterTextView.visibility = View.GONE
            binding.confessAnswerTextView.visibility = View.GONE
            binding.answerScreenProfileImage.visibility = View.GONE
        }

        val maxLength = 560
        binding.confessAnswerEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val currentLength = s?.trim()?.length ?: 0
                isTextEmpty = s?.trim()?.isEmpty()
                binding.answerCounterTextView.apply {
                    visibility = View.VISIBLE
                    text = "$currentLength/$maxLength"
                }

                if (isTextEmpty == true) {
                    isAnswerButtonEnabled = false
                    binding.answerCounterTextView.setTextColor(Color.parseColor("#B6B6B6"))
                    setSaveButton()
                } else if (currentLength > maxLength) {
                    isAnswerButtonEnabled = false
                    setSaveButton()
                    binding.answerCounterTextView.setTextColor(Color.RED)
                    binding.confessAnswerEditText.error =
                        getString(R.string.answer_is_too_long_max) + maxLength + getString(R.string.characters)
                } else {
                    binding.confessAnswerEditText.error = null
                    isAnswerButtonEnabled = true
                    binding.answerCounterTextView.setTextColor(Color.parseColor("#B6B6B6"))
                    setSaveButton()
                }

                if (s?.trim()?.toString() == answerText) {
                    isAnswerButtonEnabled = false
                    setSaveButton()
                }
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })
        binding.confessAnswerTextView.setOnLongClickListener {
            MyUtils.copyTextToClipboard(answerText, requireContext())
        }
        binding.answerScreenConfession.setOnLongClickListener {
            MyUtils.copyTextToClipboard(confessionText, requireContext())
        }
    }

    private fun setSaveButton() {
        if (isAnswerButtonEnabled) {
            binding.replyButton.alpha = 1f
            binding.replyButton.isClickable = true
        } else {
            binding.replyButton.alpha = 0.5f
            binding.replyButton.isClickable = false
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setAnswerUsernameAndDateText(answeredUserName: String, answerTimestamp: Any?) {
        val answerDate = MyUtils.convertFirestoreTimestampToReadableDate(
            answerTimestamp,
            requireContext()
        )
        binding.answerScreenAnswerTimestamp.tooltipText = answerDate
        binding.answerScreenAnswerUsername.text = answeredUserName

        val answerElapsedTime = if (answerTimestamp != null) MyUtils.calculateTimeSinceConfession(
            answerTimestamp as Timestamp,
            requireContext()
        ) else "-"

        binding.answerScreenAnswerTimestamp.text = answerElapsedTime
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setConfessionTexts(
        confessionUserName: String, toUserName: String, confessionText: String,
        confessionTimeStamp: Any?, confessionUserUid: String, confessionFromUserUid: String,
        confessionFromUserToken: String, confessionUserToken: String
    ) {
        val maxUsernameLength = 18
        val ellipsis = "..."

        val username = toUserName
        val truncatedUsername = if (username.length > maxUsernameLength) {
            username.substring(0, maxUsernameLength - ellipsis.length) + ellipsis
        } else {
            username
        }

        val toUserName = "@${truncatedUsername} "
        val spannable = SpannableString("$toUserName${confessionText}")

        val usernameColor = ContextCompat.getColor(requireContext(), R.color.confessmered)
        val usernameStart = 0
        val usernameEnd = toUserName.length

        spannable.setSpan(
            object : ClickableSpan() {
                override fun onClick(widget: View) {
                    goToOtherUserProfile(confessionUserUid, answerUserName, confessionUserToken)
                }

                override fun updateDrawState(ds: TextPaint) {
                    ds.color = usernameColor
                    ds.isUnderlineText = false
                }
            },
            usernameStart,
            usernameEnd,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        spannable.setSpan(
            StyleSpan(Typeface.BOLD),
            usernameStart,
            usernameEnd,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        val usernameTv = binding.answerScreenConfessionUsername
        usernameTv.text = confessionUserName
        if (confessionUserName.equals("Anonymous")) {
            usernameTv.setBackgroundResource(R.drawable.anonymous_username_background)
        } else {
            usernameTv.setBackgroundColor(Color.TRANSPARENT)
        }
        binding.answerScreenConfession.text = spannable
        binding.answerScreenConfession.movementMethod = LinkMovementMethod.getInstance()
        binding.answerScreenConfession.highlightColor = Color.TRANSPARENT
        binding.answerScreenConfessionTimestamp.text =
            MyUtils.calculateTimeSinceConfession(confessionTimeStamp as Timestamp, requireContext())

        val date =
            MyUtils.convertFirestoreTimestampToReadableDate(confessionTimeStamp, requireContext())
        binding.answerScreenConfessionTimestamp.tooltipText = date

        binding.answerScreenConfessionProfileImage.setOnClickListener {
            goToOtherUserProfile(confessionFromUserUid, answerFromUsername, confessionFromUserToken)
        }
    }

    private fun setUserNameProfileImageAndAnswerText(
        answerUserUid: String, answerFromUserUid: String,
        answerFromUsername: String, answerUserName: String,
        userToken: String, fromUserToken: String,
        confessedUserName: String, answerText: String
    ) {
        val maxUsernameLength = 18
        val ellipsis = "..."

        val username = confessedUserName
        val truncatedUsername = if (username.length > maxUsernameLength) {
            username.substring(0, maxUsernameLength - ellipsis.length) + ellipsis
        } else {
            username
        }

        val confessedUserNameBold = SpannableString("@${truncatedUsername}")
        confessedUserNameBold.setSpan(
            StyleSpan(Typeface.BOLD),
            0,
            truncatedUsername.length + 1,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        confessedUserNameBold.setSpan(
            ForegroundColorSpan(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.confessmered
                )
            ), 0, truncatedUsername.length + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(view: View) {
                goToOtherUserProfile(answerFromUserUid, answerUserName, userToken)
            }

            override fun updateDrawState(ds: TextPaint) {
                ds.color = ContextCompat.getColor(requireContext(), R.color.confessmered)
                ds.isUnderlineText = false
            }
        }
        confessedUserNameBold.setSpan(
            clickableSpan,
            0,
            truncatedUsername.length + 1,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        val usernameAndAnswerText = TextUtils.concat(confessedUserNameBold, " ", answerText)

        binding.confessAnswerTextView.text = usernameAndAnswerText
        binding.confessAnswerTextView.movementMethod = LinkMovementMethod.getInstance()
        binding.confessAnswerTextView.highlightColor = Color.TRANSPARENT

        binding.answerScreenProfileImage.setOnClickListener {
            goToOtherUserProfile(answerUserUid, answerFromUsername, fromUserToken)
        }
    }

    private fun setOnEditAnswer(icEdit: ImageButton, answerText: String) {
        if (!isEditAnswer) {
            binding.confessAnswerTextView.visibility = View.GONE
            binding.answerScreenProfileImage.visibility = View.GONE
            binding.answerScreenAnswerUsernameAndTimestampLayout.visibility = View.GONE
            binding.replyButton.visibility = View.VISIBLE
            binding.answerCounterTextView.visibility = View.VISIBLE
            binding.confessAnswerTextInputLayout.visibility = View.VISIBLE
            binding.confessAnswerEditText.let {
                it.visibility = View.VISIBLE
                it.setText(answerText)
                it.text?.let { answerText -> it.setSelection(answerText.length) }
                it.requestFocus()
                MyUtils.showKeyboard(requireActivity(), it.findFocus())
            }
            icEdit.setImageResource(R.drawable.ic_back)
            binding.confessAnswerTextInputLayout.hint = getString(R.string.edit_answer)
            binding.answerIcDelete.disable()
            isEditAnswer = true
        } else {
            binding.confessAnswerTextView.visibility = View.VISIBLE
            binding.answerScreenProfileImage.visibility = View.VISIBLE
            binding.answerScreenAnswerUsernameAndTimestampLayout.visibility = View.VISIBLE
            binding.replyButton.visibility = View.GONE
            binding.answerCounterTextView.visibility = View.GONE
            binding.confessAnswerEditText.let {
                MyUtils.hideKeyboard(requireActivity(), it.findFocus())
                it.visibility = View.GONE
            }
            binding.confessAnswerTextInputLayout.visibility = View.GONE
            icEdit.setImageResource(R.drawable.ic_edit)
            binding.confessAnswerTextInputLayout.hint = getString(R.string.reply_to_confession)
            binding.answerIcDelete.enable()
            isEditAnswer = false
        }
    }

    private fun goToOtherUserProfile(userId: String, username: String, userToken: String) {
        if (currentUserUid != userId && userId != "") {
            val bundle = Bundle()
            bundle.putString("userUid", userId)
            bundle.putString("userName", username)
            bundle.putString("userToken", userToken)

            val profileFragment = OtherUserProfileFragment()
            profileFragment.arguments = bundle

            dismiss()
            navRegister.navigateFrag(profileFragment, true)
        }
    }

    private fun showRepliedSnackbar(confessionText: String, answerText: String) {
        val snackbar = Snackbar.make(
            requireActivity().window.decorView.rootView,
            getString(R.string.answered_successfully),
            Snackbar.LENGTH_LONG
        )
        snackbar.setAction(getString(R.string.share)) {
            shareHelper.shareTextAndImage(
                confessionText,
                answerText,
                answerFromUsername,
                answerUserName
            )
        }
        val bottomNavigationView = requireActivity().findViewById<View>(R.id.bottomNavigationView)
        snackbar.setAnchorView(bottomNavigationView)
        snackbar.show()
    }
}