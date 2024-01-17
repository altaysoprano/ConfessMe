package com.example.confessme.presentation.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.example.confessme.R
import com.example.confessme.databinding.FragmentChangePasswordBinding
import com.example.confessme.databinding.FragmentConfirmPasswordBinding
import com.example.confessme.presentation.BottomNavBarControl
import com.example.confessme.presentation.SettingsViewModel
import com.example.confessme.util.MyUtils.disable
import com.example.confessme.util.MyUtils.enable
import com.example.confessme.util.UiState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ConfirmPasswordFragment : DialogFragment() {

    private lateinit var binding: FragmentConfirmPasswordBinding
    private val viewModel: SettingsViewModel by viewModels()
    private var bottomNavBarControl: BottomNavBarControl? = null

    override fun onStart() {
        super.onStart()
        dialog?.getWindow()?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        );
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentConfirmPasswordBinding.inflate(inflater, container, false)

        observeConfirmPassword()
        setOnConfirmClickListener()

        return binding.root
    }

    fun observeConfirmPassword() {
        viewModel.deleteAccountState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBarChangePassword.visibility = View.VISIBLE
                    dialog?.setCancelable(false)
                    setInputsEnabled(false)
                    setHomeScreenDisabled(true)
                }

                is UiState.Failure -> {
                    binding.progressBarChangePassword.visibility = View.GONE
                    dialog?.setCancelable(true)
                    setInputsEnabled(true)
                    setHomeScreenDisabled(false)

                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarChangePassword.visibility = View.GONE
                    dialog?.setCancelable(true)
                    setInputsEnabled(true)
                    setHomeScreenDisabled(false)

                    val fragmentManager = parentFragmentManager
                    for (i in 0 until fragmentManager.backStackEntryCount) {
                        fragmentManager.popBackStack()
                    }
                    fragmentManager.beginTransaction()
                        .replace(R.id.coordinator, LoginFragment())
                        .commit()

                    Toast.makeText(requireContext(), state.data, Toast.LENGTH_SHORT).show()
                    dismiss()
                }
            }
        }
    }

    private fun setOnConfirmClickListener() {
        binding.confirmButton.setOnClickListener {
            val confirmPassword = binding.confirmPasswordEt.text.toString()

            if (confirmPassword.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.please_fill_in_all_fields),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                viewModel.deleteAccountWithConfessionsAndSignOut(confirmPassword, null)
            }
        }
    }

    private fun setInputsEnabled(enabled: Boolean) {
        if (enabled) {
            binding.confirmButton.enable()
            binding.confirmPasswordEt.enable()
        } else {
            binding.confirmButton.disable()
            binding.confirmPasswordEt.disable()
        }
    }

    private fun setHomeScreenDisabled(disabled: Boolean) {
        if (disabled) {
            disableBottomNavigationBarInActivity()
            binding.root.alpha = 0.5f
            enableDisableViewGroup(requireView() as ViewGroup, false)
        } else {
            enableBottomNavigationBarInActivity()
            binding.root.alpha = 1f
            enableDisableViewGroup(requireView() as ViewGroup, true)
        }
    }

    fun enableDisableViewGroup(viewGroup: ViewGroup, enabled: Boolean) {
        val childCount = viewGroup.childCount
        for (i in 0 until childCount) {
            val view = viewGroup.getChildAt(i)
            view.isEnabled = enabled
            if (view is ViewGroup) {
                enableDisableViewGroup(view, enabled)
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is BottomNavBarControl) {
            bottomNavBarControl = context
        }
    }

    override fun onDetach() {
        super.onDetach()
        bottomNavBarControl = null
    }

    private fun disableBottomNavigationBarInActivity() {
        bottomNavBarControl?.disableBottomNavigationBar()
    }

    private fun enableBottomNavigationBarInActivity() {
        bottomNavBarControl?.enableBottomNavigationBar()
    }
}
