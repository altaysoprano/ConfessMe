package com.example.confessme.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.example.confessme.databinding.FragmentChangePasswordBinding
import com.example.confessme.databinding.FragmentConfirmPasswordBinding
import com.example.confessme.presentation.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ConfirmPasswordFragment : DialogFragment() {

    private lateinit var binding: FragmentConfirmPasswordBinding
    private val viewModel: SettingsViewModel by viewModels()

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

        return binding.root
    }
}
