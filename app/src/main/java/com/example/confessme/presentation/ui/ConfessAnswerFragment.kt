package com.example.confessme.presentation.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.net.Uri
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
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.example.confessme.R
import com.example.confessme.data.model.Confession
import com.example.confessme.databinding.FragmentConfessAnswerBinding
import com.example.confessme.presentation.ConfessViewModel
import com.example.confessme.presentation.ConfessMeDialog
import com.example.confessme.util.MyUtils
import com.example.confessme.util.UiState
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

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

    private fun observeGetConfession() {
        viewModel.getConfessionState.observe(this) { state ->
            when (state) {
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
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarConfessAnswer.visibility = View.GONE

                    val answerText = state.data?.answer?.text ?: ""
                    isAnswerFavorited = state.data?.answer?.favorited ?: false
                    val answerFromUserUid = state.data?.fromUserId ?: ""
                    val answerFromUsername = state.data?.fromUserUsername ?: ""
                    val answerUserName = state.data?.username ?: ""
                    val userToken = state.data?.fromUserToken ?: ""
                    val fromUserToken = state.data?.userToken ?: ""
                    val answerUserUid = state.data?.userId ?: ""
                    val isConfessionAnswered = state.data?.answered ?: false
                    val answeredUserName = state.data?.answer?.fromUserUsername ?: ""
                    val answerTimeStamp = state.data?.answer?.timestamp
                    val confessedUserName = state.data?.answer?.username ?: ""
                    val anonymousId = state.data?.anonymousId ?: ""

                    setUserImage(state.data?.answer?.fromUserImageUrl)
                    setSaveButton()
                    setImageAndTextStates(
                        isConfessionAnswered, answerText, answeredUserName,
                        answerTimeStamp, answerUserUid, answerFromUserUid,
                        answerFromUsername, answerUserName, userToken,
                        fromUserToken, confessedUserName
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
            binding.confessAnswerTextView.visibility = View.GONE
            binding.answerScreenProfileImage.visibility = View.GONE
            binding.confessAnswerUserNameAndDate.visibility = View.GONE
            binding.replyButton.visibility = View.VISIBLE
            binding.answerCounterTextView.visibility = View.VISIBLE
            binding.confessAnswerEditText.let {
                it.visibility = View.VISIBLE
                it.setText(answerText)
                it.requestFocus()
                showKeyboard(requireContext(), it)
            }
            isEditAnswer = true
        }

        binding.answerIcFavorite.setOnClickListener {
            isAnswerFavorited = !isAnswerFavorited
            viewModel.addAnswerFavorite(isAnswerFavorited, confessionId ?: "")
        }

        binding.answerIcDelete.setOnClickListener {
            dialogHelper.showDialog(
                getString(R.string.delete_answer),
                getString(R.string.are_you_sure_you_really_want_to_delete_this_answer),
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

    private fun setImageAndTextStates(
        isConfessionAnswered: Boolean, answerText: String, answeredUserName: String,
        answerTimestamp: Any?, answerUserUid: String, answerFromUserUid: String,
        answerFromUsername: String, answerUserName: String, userToken: String,
        fromUserToken: String, confessedUserName: String
    ) {

        if (isConfessionAnswered == true && !isEditAnswer) {
            binding.answerScreenProfileImage.visibility = View.VISIBLE
            binding.confessAnswerEditText.visibility = View.GONE
            binding.answerCounterTextView.visibility = View.GONE
            binding.confessAnswerTextView.visibility = View.VISIBLE
            binding.confessAnswerUserNameAndDate.visibility = View.VISIBLE
            setUserNameProfileImageAndAnswerText(
                answerUserUid, answerFromUserUid, answerFromUsername, answerUserName,
                userToken, fromUserToken, confessedUserName, answerText
            )
            setUsernameAndDateText(answeredUserName, answerTimestamp)
        } else {
            binding.confessAnswerEditText.apply {
                visibility = View.VISIBLE
                requestFocus()
                showKeyboard(requireContext(), this)
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

    private fun setUsernameAndDateText(answeredUserName: String, answerTimestamp: Any?) {
        val answeredUserNameBold = SpannableString(answeredUserName)
        answeredUserNameBold.setSpan(
            StyleSpan(Typeface.BOLD),
            0,
            answeredUserName.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        val answerElapsedTime = if (answerTimestamp != null) MyUtils.calculateTimeSinceConfession(
            answerTimestamp as Timestamp,
            requireContext()
        ) else "-"

        val answerDateBold = SpannableString(answerElapsedTime)
        answerDateBold.setSpan(object : ClickableSpan() {
            override fun onClick(view: View) {
                val date = MyUtils.convertFirestoreTimestampToReadableDate(
                    answerTimestamp,
                    requireContext()
                )
                Toast.makeText(view.context, date, Toast.LENGTH_SHORT).show()
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false
                ds.color = ContextCompat.getColor(requireContext(), R.color.grey600)
            }
        }, 0, answerElapsedTime.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        binding.confessAnswerUserNameAndDate.highlightColor = Color.TRANSPARENT
        val usernameAndDateText = TextUtils.concat(answeredUserNameBold, " · ", answerDateBold)
        binding.confessAnswerUserNameAndDate.text = usernameAndDateText
        binding.confessAnswerUserNameAndDate.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun setUserNameProfileImageAndAnswerText(
        answerUserUid: String, answerFromUserUid: String,
        answerFromUsername: String, answerUserName: String,
        userToken: String, fromUserToken: String,
        confessedUserName: String, answerText: String
    ) {
        val confessedUserNameBold = SpannableString("@$confessedUserName")
        confessedUserNameBold.setSpan(
            StyleSpan(Typeface.BOLD),
            0,
            confessedUserName.length + 1,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        confessedUserNameBold.setSpan(
            ForegroundColorSpan(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.confessmered
                )
            ), 0, confessedUserName.length + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(view: View) {
                if (currentUserUid != answerFromUserUid && answerFromUserUid != "") {
                    val bundle = Bundle()
                    bundle.putString("userUid", answerFromUserUid)
                    bundle.putString("userName", answerUserName)
                    bundle.putString("userToken", userToken)

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
        confessedUserNameBold.setSpan(
            clickableSpan,
            0,
            confessedUserName.length + 1,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        val usernameAndAnswerText = TextUtils.concat(confessedUserNameBold, " ", answerText)

        binding.confessAnswerTextView.text = usernameAndAnswerText
        binding.confessAnswerTextView.movementMethod = LinkMovementMethod.getInstance()
        binding.confessAnswerTextView.highlightColor = Color.TRANSPARENT

        binding.answerScreenProfileImage.setOnClickListener {
            if (currentUserUid != answerUserUid) {
                val bundle = Bundle()

                bundle.putString("userUid", answerUserUid)
                bundle.putString("userName", answerFromUsername)
                bundle.putString("userToken", fromUserToken)

                val profileFragment = OtherUserProfileFragment()
                profileFragment.arguments = bundle

                dismiss()
                navRegister.navigateFrag(profileFragment, true)
            }
        }
    }

    private fun showKeyboard(context: Context, view: View) {
        val inputMethodManager =
            context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun generateImageWithText(confessionText: String, answerText: String): Bitmap {
        val maxWidth = 500
        val maxHeight = 500

        val bitmap = Bitmap.createBitmap(maxWidth, maxHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()

        canvas.drawColor(Color.BLACK)

        paint.color = ContextCompat.getColor(requireContext(), R.color.confessmered)
        paint.textSize = 30f

        val confessionTextBound = Rect()
        paint.getTextBounds(confessionText, 0, confessionText.length, confessionTextBound)

        val answerTextBound = Rect()
        paint.getTextBounds(answerText, 0, answerText.length, answerTextBound)

        val confessionTextHeight = confessionTextBound.height()
        val answerTextHeight = answerTextBound.height()

        val confessionTextWidth = confessionTextBound.width()
        val answerTextWidth = answerTextBound.width()

        val confessionTextX = 50f
        val confessionTextY = 100f

        val answerTextX = 50f
        val answerTextY = 200f

        if (confessionTextWidth > maxWidth || confessionTextHeight > maxHeight) {
            paint.textSize *= (maxWidth.toFloat() / confessionTextWidth)
        }

        if (answerTextWidth > maxWidth || answerTextHeight > maxHeight) {
            paint.textSize *= (maxWidth.toFloat() / answerTextWidth)
        }

        canvas.drawText(confessionText, confessionTextX, confessionTextY, paint)
        canvas.drawText(answerText, answerTextX, answerTextY, paint)

        return bitmap
    }

    private fun saveBitmapToStorage(bitmap: Bitmap): Uri? {
        val imagesFolder = File(requireContext().cacheDir, "images")
        imagesFolder.mkdirs()

        val file = File(imagesFolder, "shared_image.png")
        try {
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
            stream.close()
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }

        return FileProvider.getUriForFile(
            requireContext(),
            requireContext().packageName + ".provider",
            file
        )
    }

    private fun shareTextAndImage(confessionText: String, answerText: String) {
        val generatedBitmap = generateImageWithText(confessionText, answerText)
        val imageUri = saveBitmapToStorage(generatedBitmap)

        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "image/*"
        shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri)
        shareIntent.putExtra(Intent.EXTRA_TEXT, "$confessionText - $answerText")

        val chooser = Intent.createChooser(shareIntent, getString(R.string.nerede_payla_acaks_n))
        if (shareIntent.resolveActivity(requireContext().packageManager) != null) {
            requireContext().startActivity(chooser)
        }
    }

    private fun showRepliedSnackbar(confessionText: String, answerText: String) {
        val snackbar = Snackbar.make(
            requireActivity().window.decorView.rootView,
            getString(R.string.answered_successfully),
            Snackbar.LENGTH_LONG
        )
        snackbar.setAction(getString(R.string.share)) {
            shareTextAndImage(confessionText, answerText)
        }
        val bottomNavigationView = requireActivity().findViewById<View>(R.id.bottomNavigationView)
        snackbar.setAnchorView(bottomNavigationView)
        snackbar.show()
    }
}