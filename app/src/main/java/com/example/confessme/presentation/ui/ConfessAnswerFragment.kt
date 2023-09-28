package com.example.confessme.presentation.ui

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
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.example.confessme.R
import com.example.confessme.databinding.FragmentConfessAnswerBinding
import com.example.confessme.presentation.ConfessViewModel
import com.example.confessme.presentation.ProfileSearchSharedViewModel
import com.example.confessme.util.UiState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ConfessAnswerFragment : Fragment() {

    private lateinit var binding: FragmentConfessAnswerBinding
    private lateinit var navRegister: FragmentNavigation
    private val viewModel: ConfessViewModel by viewModels()
    private var isAnswerButtonEnabled = true
    private var isEditAnswer: Boolean = false
    private lateinit var answerText: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentConfessAnswerBinding.inflate(inflater, container, false)
        (activity as AppCompatActivity?)!!.title = "Reply To Confession"
        navRegister = activity as FragmentNavigation
        setHasOptionsMenu(true)
        val isConfessionAnswered = arguments?.getBoolean("isAnswered", false)
        answerText = arguments?.getString("answerText", "") ?: ""

        (activity as AppCompatActivity?)!!.supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_close)
        }

        if(isConfessionAnswered == true && !isEditAnswer) {
            binding.confessAnswerEditText.visibility = View.GONE
            binding.confessAnswerTextView.visibility = View.VISIBLE
            binding.confessAnswerTextView.text = answerText
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
                    isAnswerButtonEnabled = false
                    requireActivity().invalidateOptionsMenu()
                } else {
                    binding.confessAnswerEditText.error = null
                    isAnswerButtonEnabled = true
                    requireActivity().invalidateOptionsMenu()
                }
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })

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
                    requireActivity().onBackPressed()
                    Toast.makeText(requireContext(), state.data, Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

        return binding.root
    }

    fun String.toEditable(): Editable =  Editable.Factory.getInstance().newEditable(this)

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                requireActivity().onBackPressed()
                return true
            }

            R.id.action_confess -> {
                val answerEditText = binding.confessAnswerEditText.text.toString()
                val confessionId = arguments?.getString("confessionId", "")

                if (answerEditText.trim().isNotEmpty()) {
                    viewModel.addAnswer(confessionId ?: "", answerEditText)
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Your answer cannot be blank",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return true
            }
            R.id.action_edit_answer -> {
                (activity as AppCompatActivity?)!!.title = "Edit your answer"
                binding.confessAnswerTextView.visibility = View.GONE
                binding.confessAnswerEditText.let {
                    it.visibility = View.VISIBLE
                    it.setText(answerText)
                }
                isEditAnswer = true
                requireActivity().invalidateOptionsMenu()
            }
        }
        return false
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        val isConfessionAnswered = arguments?.getBoolean("isAnswered", false)

        if (isConfessionAnswered == true && !isEditAnswer) {
            inflater.inflate(R.menu.edit_answer_menu, menu)
            (activity as AppCompatActivity?)!!.title = "Your Answer"
        } else {
            inflater.inflate(R.menu.confess_menu, menu)
            val confessMenuItem = menu.findItem(R.id.action_confess)
            confessMenuItem.isEnabled = isAnswerButtonEnabled
        }
        super.onCreateOptionsMenu(menu, inflater)
    }
}