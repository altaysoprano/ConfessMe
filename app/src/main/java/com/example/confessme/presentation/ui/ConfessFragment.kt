package com.example.confessme.presentation.ui

import android.content.Context
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
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.example.confessme.R
import com.example.confessme.databinding.FragmentConfessBinding
import com.example.confessme.presentation.ConfessMeDialog
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
    private var isAnonymous = false
    private var confessText = ""
    private lateinit var dialogHelper: ConfessMeDialog
    private var callback: OnBackPressedCallback? = null

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
        dialogHelper = ConfessMeDialog(requireContext())

        (activity as AppCompatActivity?)!!.supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_back)
        }

        setAnonymitySwitch()
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
                val isTextEmpty = s?.trim()?.isEmpty()
                confessText = s?.toString() ?: ""

                if (isTextEmpty == true) {
                    isConfessButtonEnabled = false
                    requireActivity().invalidateOptionsMenu()
                } else if (currentLength > maxLength) {
                    isConfessButtonEnabled = false
                    requireActivity().invalidateOptionsMenu()
                    binding.confessEditText.error =
                        "Confession is too long (max $maxLength characters)"
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
                    callback?.isEnabled = false
                    callback = null
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
                viewModel.addConfession(userUid, confessionText, isAnonymous)
                return true
            }
        }
        return false
    }

    private fun setAnonymitySwitch() {
        binding.anonymitySwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                binding.anonymitySwitch.text = "anonymously"
                binding.anonymitySwitch.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.confessmered
                    )
                )
                binding.anonymitySwitch.alpha = 1f
                isAnonymous = true
            } else {
                binding.anonymitySwitch.text = "openly"
                binding.anonymitySwitch.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        android.R.color.black
                    )
                )
                binding.anonymitySwitch.alpha = 0.5f
                isAnonymous = false
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.confess_menu, menu)
        val confessMenuItem = menu.findItem(R.id.action_confess)
        confessMenuItem.isEnabled = isConfessButtonEnabled
        confessMenuItem.icon?.alpha =
            if (isConfessButtonEnabled) (1f * 255).toInt() else (0.5f * 255).toInt()
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!confessText.isEmpty()) {
                    dialogHelper.showDialog(
                        "confırm exıt",
                        "Do you want to exit without sending the confession?"
                    ) {
                        isEnabled = false
                        hideKeyboard()
                        requireActivity().onBackPressed()
                    }
                } else {
                    isEnabled = false
                    hideKeyboard()
                    requireActivity().onBackPressed()
                }
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback!!)
    }

    override fun onDestroyView() {
        callback?.isEnabled = false
        callback = null
        super.onDestroyView()
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val currentFocusedView = requireActivity().currentFocus
        if (currentFocusedView != null) {
            imm.hideSoftInputFromWindow(currentFocusedView.windowToken, 0)
        }
    }

}