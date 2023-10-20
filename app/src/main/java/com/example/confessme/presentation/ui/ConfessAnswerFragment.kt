package com.example.confessme.presentation.ui

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextUtils
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.TextAppearanceSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.example.confessme.R
import com.example.confessme.data.model.Confession
import com.example.confessme.databinding.FragmentConfessAnswerBinding
import com.example.confessme.presentation.ConfessViewModel
import com.example.confessme.presentation.DialogHelper
import com.example.confessme.util.ConfessionCategory
import com.example.confessme.util.UiState
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
    private var isAnswerButtonEnabled = true
    private var isEditAnswer: Boolean = false
    private lateinit var currentUserUid: String
    private lateinit var answerUserUid: String
    private lateinit var answerFromUserUid: String
    private var isAnswerFavorited: Boolean = false
    private var isConfessionAnswered: Boolean = false
    private var answerDate: String = ""
    private lateinit var answeredUserName: String
    private lateinit var answerText: String
    private lateinit var dialogHelper: DialogHelper

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
        answeredUserName = arguments?.getString("answeredUserName", "") ?: ""
        isAnswerFavorited = arguments?.getBoolean("favorited", false) ?: false
        answerDate = arguments?.getString("answerDate", "") ?: ""
        dialogHelper = DialogHelper(requireContext())

        (activity as AppCompatActivity?)!!.supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_back)
        }

        setTextStates()
        setFavoriteDeleteEditReplyStates()
        observeAddAnswer()
        observeFavorite()
        observeDeleteAnswer()

        return binding.root
    }

    private fun observeFavorite() {
        viewModel.addFavoriteAnswer.observe(viewLifecycleOwner) { state ->
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
                    val updatedConfession = state.data

                    Log.d("Mesaj: ", "OnSuccesste şu an")

                    val position = updatedConfession?.let { findItemById(it.id) }

                    if (position != -1) {
                        if (updatedConfession != null) {
                            if (position != null) {
                                onUpdateItem(position, updatedConfession)
                            }
                        }
                    }

                    if (state.data?.answer?.favorited == true) {
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

            if (answerEditText.trim().isNotEmpty()) {
                viewModel.addAnswer(confessionId ?: "", answerEditText)
            } else {
                Toast.makeText(
                    requireContext(),
                    "Your answer cannot be blank",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.answerIcEdit.setOnClickListener {
            binding.confessAnswerTextView.visibility = View.GONE
            binding.replyButton.visibility = View.VISIBLE
            binding.confessAnswerEditText.let {
                it.visibility = View.VISIBLE
                it.setText(answerText)
            }
            isEditAnswer = true
        }

        binding.answerIcFavorite.setOnClickListener {
            isAnswerFavorited = !isAnswerFavorited
            if (isAnswerFavorited) {
                binding.answerIcFavorite.setColorFilter(resources.getColor(R.color.confessmered))
            } else {
                binding.answerIcFavorite.setColorFilter(Color.parseColor("#B8B8B8"))
            }
            viewModel.addAnswerFavorite(isAnswerFavorited, confessionId ?: "")
        }

        binding.answerIcDelete.setOnClickListener {
            dialogHelper.showDeleteConfessionDialog("answer", {
                viewModel.deleteAnswer(confessionId ?: "")
            })
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
                binding.replyButton.isEnabled = isAnswerButtonEnabled
                binding.replyButton.isClickable = isAnswerButtonEnabled
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

    private fun setTextStates() {

        if (isConfessionAnswered == true && !isEditAnswer) {
            binding.confessAnswerEditText.visibility = View.GONE
            binding.confessAnswerTextView.visibility = View.VISIBLE
            binding.confessAnswerUserNameAndDate.visibility = View.VISIBLE
            binding.confessAnswerTextView.text = answerText
            setUsernameAndDateText()
        } else {
            binding.confessAnswerEditText.visibility = View.VISIBLE
            binding.confessAnswerTextView.visibility = View.GONE
        }

        val maxLength = 560
        binding.confessAnswerEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val currentLength = s?.length ?: 0
                if (currentLength > maxLength) {
                    binding.confessAnswerEditText.error = "Character limit exceeded"
                    binding.replyButton.alpha = 0.5f
                    binding.replyButton.isClickable = false
                } else {
                    binding.confessAnswerEditText.error = null
                    binding.replyButton.isClickable = true
                    binding.replyButton.alpha = 1f
                }

                if(s.toString() == answerText) {
                    binding.replyButton.alpha = 0.5f
                    binding.replyButton.isClickable = false
                }
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })
    }

    private fun setUsernameAndDateText() {
        val answeredUserNameBold = SpannableString("@$answeredUserName")
        answeredUserNameBold.setSpan(StyleSpan(Typeface.BOLD), 0, answeredUserName.length + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        answeredUserNameBold.setSpan(ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.confessmered)), 0, answeredUserName.length + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        val answerDateBold = SpannableString(answerDate)
        answerDateBold.setSpan(StyleSpan(Typeface.BOLD), 0, answerDate.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        val usernameAndDateText = TextUtils.concat("Answered by ", answeredUserNameBold, " ", answerDateBold)
        binding.confessAnswerUserNameAndDate.text = usernameAndDateText
    }
}