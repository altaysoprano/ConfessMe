package com.example.confessme.presentation.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
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
    private var isTextEmpty: Boolean? = true
    private lateinit var dialogHelper: ConfessMeDialog
    private var callback: OnBackPressedCallback? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentConfessBinding.inflate(inflater, container, false)
        (activity as AppCompatActivity?)!!.title = context?.getString(R.string.confess)
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

    @SuppressLint("ClickableViewAccessibility")
    private fun setTextField() {
        val maxLength = 560
        binding.counterTextView.text = "0/$maxLength"
        binding.confessEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val currentLength = s?.trim()?.length ?: 0
                isTextEmpty = s?.trim()?.isEmpty()
                binding.counterTextView.text = "$currentLength/$maxLength"

                if (isTextEmpty == true) {
                    isConfessButtonEnabled = false
                    binding.counterTextView.setTextColor(Color.parseColor("#B6B6B6"))
                    requireActivity().invalidateOptionsMenu()
                } else if (currentLength > maxLength) {
                    isConfessButtonEnabled = false
                    requireActivity().invalidateOptionsMenu()
                    binding.counterTextView.setTextColor(Color.RED)
                    binding.confessEditText.error =
                        getString(R.string.confession_is_too_long_max) + maxLength + getString(R.string.characters)
                } else {
                    binding.confessEditText.error = null
                    isConfessButtonEnabled = true
                    binding.counterTextView.setTextColor(Color.parseColor("#B6B6B6"))
                    requireActivity().invalidateOptionsMenu()
                }
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })

        binding.confessScrollView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    binding.confessEditText.requestFocus()
                    val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    inputMethodManager.showSoftInput(binding.confessEditText, InputMethodManager.SHOW_IMPLICIT)
                    true
                }
                else -> false
            }
        }
    }

    private fun observeAddConfession() {
        viewModel.addConfessionState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBarConfess.visibility = View.VISIBLE
                    isConfessButtonEnabled = false
                    callback?.isEnabled = false
                    callback = null
                    binding.anonymitySwitch.isEnabled = false
                    binding.anonymitySwitch.alpha = 0.5f
                    binding.confessEditText.isEnabled = false
                    binding.confessEditText.alpha = 0.5f
                    requireActivity().invalidateOptionsMenu()
                }

                is UiState.Failure -> {
                    binding.progressBarConfess.visibility = View.GONE
                    isConfessButtonEnabled = true
                    binding.anonymitySwitch.isEnabled = true
                    binding.anonymitySwitch.alpha = 1f
                    binding.confessEditText.isEnabled = true
                    binding.confessEditText.alpha = 1f
                    setOnBackPressed()
                    requireActivity().invalidateOptionsMenu()
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarConfess.visibility = View.GONE
                    requireActivity().onBackPressed()
                    Toast.makeText(requireContext(), getString(R.string.confessedwithsmile), Toast.LENGTH_SHORT)
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
                val confessionText = binding.confessEditText.text.toString().trim()
                viewModel.addConfession(userUid, confessionText, isAnonymous)
                return true
            }
        }
        return false
    }

    private fun setAnonymitySwitch() {
        binding.anonymitySwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                binding.anonymitySwitch.text = getString(R.string.anonymously)
                binding.anonymitySwitch.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.confessmered
                    )
                )
                binding.anonymitySwitch.alpha = 1f
                isAnonymous = true
            } else {
                binding.anonymitySwitch.text = getString(R.string.openly)
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
        val typedValue = TypedValue()
        val theme = requireContext().theme
        theme.resolveAttribute(com.google.android.material.R.attr.colorOnSecondary, typedValue, true)
        val colorOnSecondary = typedValue.data

        confessMenuItem.isEnabled = isConfessButtonEnabled
        confessMenuItem.icon?.apply {
            alpha = if (isConfessButtonEnabled) (1f * 255).toInt() else (0.3f * 255).toInt()
            val color = if (isConfessButtonEnabled) ContextCompat.getColor(requireContext(), R.color.confessmered)
                        else colorOnSecondary
            setTint(color)
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setOnBackPressed()
    }

    private fun setOnBackPressed() {
        callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isTextEmpty == false) {
                    dialogHelper.showDialog(
                        getString(R.string.conf_rm_ex_t),
                        getString(R.string.do_you_want_to_exit_without_sending_the_confession)
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