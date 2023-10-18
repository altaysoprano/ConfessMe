package com.example.confessme.presentation.ui

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import com.example.confessme.R
import com.example.confessme.databinding.FragmentLoginBinding
import com.example.confessme.databinding.FragmentRegisterBinding
import com.example.confessme.presentation.RegisterViewModel
import com.example.confessme.util.UiState
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegisterFragment : Fragment() {

    private lateinit var binding: FragmentRegisterBinding
    private val viewModel: RegisterViewModel by viewModels()
    private lateinit var navRegister: FragmentNavigation

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentRegisterBinding.inflate(inflater, container, false)
        navRegister = activity as FragmentNavigation

        binding.textView2.setOnClickListener {
            navRegister.navigateFrag(LoginFragment(), false)
        }

        binding.button.setOnClickListener {
            val email = binding.emailEt.text.toString().trim()
            val pass = binding.passwordEt.text.toString()
            val confirmPass = binding.passwordAgainEt.text.toString()

            viewModel.signUp(email, pass, confirmPass)
        }

        viewModel.signUpState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBarSignUp.visibility = View.VISIBLE
                }

                is UiState.Failure -> {
                    binding.progressBarSignUp.visibility = View.GONE
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarSignUp.visibility = View.GONE
                    Toast.makeText(requireContext(), state.data, Toast.LENGTH_SHORT)
                        .show()
                    navRegister.navigateFrag(LoginFragment(), false)
                }

                else -> {
                }
            }
        }

        return binding.root
    }

}