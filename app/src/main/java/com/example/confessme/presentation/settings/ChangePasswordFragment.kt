package com.example.confessme.presentation.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.example.confessme.R
import com.example.confessme.databinding.FragmentChangePasswordBinding
import com.example.confessme.utils.MyUtils.disable
import com.example.confessme.utils.MyUtils.enable
import com.example.confessme.presentation.utils.UiState
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

        setOnSaveClickListener()
        observeUpdatePassword()

        return binding.root
    }

    fun observeUpdatePassword() {
        viewModel.updatePasswordState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBarChangePassword.visibility = View.VISIBLE
                    setInputsEnabled(false)
                }

                is UiState.Failure -> {
                    binding.progressBarChangePassword.visibility = View.GONE
                    setInputsEnabled(true)

                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarChangePassword.visibility = View.GONE
                    setInputsEnabled(true)

                    Toast.makeText(requireContext(), state.data, Toast.LENGTH_SHORT).show()
                    dismiss()
                }
            }
        }
    }

    private fun setOnSaveClickListener() {
        binding.saveButton.setOnClickListener {
            val previousPassword = binding.previousPasswordEt.text.toString()
            val newPassword = binding.newPasswordEt.text.toString()
            val newPasswordAgain = binding.newPasswordAgainEt.text.toString()

            if (newPassword != newPasswordAgain) {
                Toast.makeText(requireContext(), getString(R.string.passwords_do_not_match), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if(previousPassword.isEmpty() || newPassword.isEmpty() || newPasswordAgain.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.fields_cannot_be_left_empty), Toast.LENGTH_SHORT).show()
            } else {
                viewModel.updatePassword(previousPassword, newPassword)
            }
        }
    }

    private fun setInputsEnabled(enabled: Boolean) {
        if (enabled) {
            binding.saveButton.enable()
            binding.previousPasswordEt.enable()
            binding.newPasswordEt.enable()
            binding.newPasswordAgainEt.enable()
        } else {
            binding.saveButton.disable()
            binding.previousPasswordEt.disable()
            binding.newPasswordEt.disable()
            binding.newPasswordAgainEt.disable()
        }
    }
}