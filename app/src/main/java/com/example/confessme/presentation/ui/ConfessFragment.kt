package com.example.confessme.presentation.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import com.example.confessme.databinding.FragmentConfessBinding
import com.example.confessme.presentation.ConfessViewModel
import com.example.confessme.presentation.SharedViewModel
import com.example.confessme.util.UiState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ConfessFragment : Fragment() {

    private lateinit var binding: FragmentConfessBinding
    private lateinit var navRegister: FragmentNavigation
    private val viewModel: ConfessViewModel by viewModels()
    private lateinit var userUid: String
    private var isConfessButtonEnabled = true
    private val sharedViewModel: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentConfessBinding.inflate(inflater, container, false)
        (activity as AppCompatActivity?)!!.title = "Confess"
        (activity as AppCompatActivity?)!!.setSupportActionBar(binding.confessToolbar)
        navRegister = activity as FragmentNavigation
        setHasOptionsMenu(true)
        userUid = arguments?.getString("userUid") ?: ""

        (activity as AppCompatActivity?)!!.supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_back)
        }

        val maxLength = 560
        binding.confessEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                val currentLength = s?.length ?: 0
                if (currentLength > maxLength) {
                    binding.confessEditText.error = "Character limit exceeded"
                    isConfessButtonEnabled = false
                    requireActivity().invalidateOptionsMenu()
                } else {
                    binding.confessEditText.error = null
                    isConfessButtonEnabled = true
                    requireActivity().invalidateOptionsMenu()
                }
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })

        viewModel.addConfessionState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBarConfess.visibility = View.VISIBLE
                }

                is UiState.Failure -> {
                    binding.progressBarConfess.visibility = View.GONE
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarConfess.visibility = View.GONE
                    requireActivity().onBackPressed()
                    Toast.makeText(requireContext(), "Confessed ;)", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

        return binding.root
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                requireActivity().onBackPressed()
                return true
            }
            R.id.action_confess -> {
                val confessionText = binding.confessEditText.text.toString()

                if (confessionText.isNotEmpty()) {
                    viewModel.addConfession(userUid, confessionText)
                } else {
                    Toast.makeText(requireContext(), "Confession text cannot be left blank", Toast.LENGTH_SHORT).show()
                }
                return true
            }
        }
        return false
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.confess_menu, menu)
        val confessMenuItem = menu.findItem(R.id.action_confess)
        confessMenuItem.isEnabled = isConfessButtonEnabled
        super.onCreateOptionsMenu(menu, inflater)
    }

}