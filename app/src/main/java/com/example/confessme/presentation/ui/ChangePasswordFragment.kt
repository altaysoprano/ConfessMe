package com.example.confessme.presentation.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.example.confessme.R
import com.example.confessme.databinding.FragmentChangePasswordBinding
import com.example.confessme.presentation.SettingsViewModel
import com.example.confessme.util.UiState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChangePasswordFragment : DialogFragment() {

    private lateinit var binding: FragmentChangePasswordBinding
    private val viewModel: SettingsViewModel by viewModels()

    override fun onStart() {
        super.onStart()
        dialog?.getWindow()?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentChangePasswordBinding.inflate(inflater, container, false)

        binding.saveButton.setOnClickListener {
            val previousPassword = binding.previousPasswordEt.text.toString()
            val newPassword = binding.newPasswordEt.text.toString()
            val newPasswordAgain = binding.newPasswordAgainEt.text.toString()

            viewModel.updatePassword(previousPassword, newPassword, newPasswordAgain)
        }

        observeUpdatePassword()

        return binding.root
    }

    fun observeUpdatePassword() {
        viewModel.updatePasswordState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBarChangePassword.visibility = View.VISIBLE
                    binding.saveButton.isEnabled = false
                    binding.saveButton.alpha = 0.5f
                    binding.previousPasswordEt.isEnabled = false
                    binding.previousPasswordEt.alpha = 0.5f
                    binding.newPasswordEt.isEnabled = false
                    binding.newPasswordEt.alpha = 0.5f
                    binding.newPasswordAgainEt.isEnabled = false
                    binding.newPasswordAgainEt.alpha = 0.5f
                }

                is UiState.Failure -> {
                    binding.progressBarChangePassword.visibility = View.GONE
                    binding.saveButton.isEnabled = true
                    binding.saveButton.alpha = 1f
                    binding.previousPasswordEt.isEnabled = true
                    binding.previousPasswordEt.alpha = 1f
                    binding.newPasswordEt.isEnabled = true
                    binding.newPasswordEt.alpha = 1f
                    binding.newPasswordAgainEt.isEnabled = true
                    binding.newPasswordAgainEt.alpha = 1f

                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarChangePassword.visibility = View.GONE
                    binding.saveButton.isEnabled = true
                    binding.saveButton.alpha = 1f
                    binding.previousPasswordEt.isEnabled = true
                    binding.previousPasswordEt.alpha = 1f
                    binding.newPasswordEt.isEnabled = true
                    binding.newPasswordEt.alpha = 1f
                    binding.newPasswordAgainEt.isEnabled = true
                    binding.newPasswordAgainEt.alpha = 1f

                    Toast.makeText(requireContext(), state.data, Toast.LENGTH_SHORT).show()
                    dismiss()
                }
            }
        }
    }
}