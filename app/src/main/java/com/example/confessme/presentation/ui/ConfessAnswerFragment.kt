package com.example.confessme.presentation.ui

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
    private var confessionCategory: ConfessionCategory = ConfessionCategory.CONFESSIONS_TO_ME
    private var isAnswerFavorited: Boolean = false
    private var isConfessionAnswered: Boolean = false
    private var answerDate: String = ""
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
        confessionCategory = arguments?.getSerializable("confessionCategory") as? ConfessionCategory
            ?: ConfessionCategory.CONFESSIONS_TO_ME
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
                    isAnswerFavorited = state.data?.answer?.favorited == true
                    val updatedConfession = state.data

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
                    requireActivity().invalidateOptionsMenu()
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
            viewModel.addAnswerFavorite(confessionId ?: "")
        }

        binding.answerIcDelete.setOnClickListener {
            dialogHelper.showDeleteConfessionDialog("answer", {
                viewModel.deleteAnswer(confessionId ?: "")
            })
        }

        if (confessionCategory == ConfessionCategory.MY_CONFESSIONS) {
            binding.replyButton.visibility = View.GONE
            binding.answerIcEdit.visibility = View.GONE
            binding.answerIcFavorite.visibility = View.VISIBLE
            binding.answerIcDelete.visibility = View.GONE

            if (isAnswerFavorited) {
                binding.answerIcFavorite.setColorFilter(resources.getColor(R.color.confessmered))
            } else {
                binding.answerIcFavorite.setColorFilter(Color.parseColor("#B8B8B8"))
            }
        } else {
            binding.answerIcFavorite.isClickable = false
            binding.answerIcFavorite.isEnabled = false

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
        }
    }

    private fun setTextStates() {

        if (isConfessionAnswered == true && !isEditAnswer) {
            binding.confessAnswerEditText.visibility = View.GONE
            binding.confessAnswerTextView.visibility = View.VISIBLE
            binding.confessAnswerDate.visibility = View.VISIBLE
            binding.confessAnswerTextView.text = answerText
            binding.confessAnswerDate.text = "Answered $answerDate"
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
}