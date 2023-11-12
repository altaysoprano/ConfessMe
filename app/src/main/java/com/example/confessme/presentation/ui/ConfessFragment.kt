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
import com.example.confessme.databinding.FragmentConfessBinding
import com.example.confessme.presentation.ConfessViewModel
import com.example.confessme.util.UiState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ConfessFragment : Fragment() {

    private lateinit var binding: FragmentConfessBinding
    private lateinit var navRegister: FragmentNavigation
    private val viewModel: ConfessViewModel by viewModels()
    private lateinit var userUid: String
    private var isConfessButtonEnabled = false

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

        setTextField()
        observeAddConfession()

        return binding.root
    }

    private fun setTextField() {
        val maxLength = 560
        binding.confessEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val currentLength = s?.length ?: 0
                val isTextEmpty = s?.isEmpty()

                if(isTextEmpty == true) {
                    isConfessButtonEnabled = false
                    requireActivity().invalidateOptionsMenu()
                }
                else if (currentLength > maxLength) {
                    isConfessButtonEnabled = false
                    requireActivity().invalidateOptionsMenu()
                    binding.confessEditText.error = "Confession is too long (max $maxLength characters)"
                } else {
                    binding.confessEditText.error = null
                    isConfessButtonEnabled = true
                    requireActivity().invalidateOptionsMenu()
                }
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })
    }

    private fun observeAddConfession() {
        viewModel.addConfessionState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBarConfess.visibility = View.VISIBLE
                    isConfessButtonEnabled = false
                    requireActivity().invalidateOptionsMenu()
                }

                is UiState.Failure -> {
                    binding.progressBarConfess.visibility = View.GONE
                    isConfessButtonEnabled = true
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
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                requireActivity().onBackPressed()
                return true
            }

            R.id.action_confess -> {
                val confessionText = binding.confessEditText.text.toString()
                viewModel.addConfession(userUid, confessionText)
                return true
            }
        }
        return false
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.confess_menu, menu)
        val confessMenuItem = menu.findItem(R.id.action_confess)
        confessMenuItem.isEnabled = isConfessButtonEnabled
        confessMenuItem.icon?.alpha =
            if (isConfessButtonEnabled) (1f * 255).toInt() else (0.5f * 255).toInt()
        super.onCreateOptionsMenu(menu, inflater)
    }

}