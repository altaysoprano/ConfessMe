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
import com.example.confessme.presentation.LoginViewModel
import com.example.confessme.util.UiState
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private lateinit var binding: FragmentLoginBinding
    private lateinit var navRegister: FragmentNavigation
    private val viewModel: LoginViewModel by viewModels()
    private var isUserLoggedIn: Boolean = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentLoginBinding.inflate(inflater, container, false)
        navRegister = activity as FragmentNavigation
        isUserLoggedIn = viewModel.isUserLoggedIn

        if (isUserLoggedIn) {
            navRegister.navigateFrag(HomeFragment(), false)
        }

        setRegisterTvClickListener()
        setSignInClickListener()
        observeSignIn()

        return binding.root
    }

    private fun observeSignIn() {
        viewModel.signInState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBarSignIn.visibility = View.VISIBLE
                    binding.button.isEnabled = false
                    binding.button.alpha = 0.5f
                    binding.emailEt.isEnabled = false
                    binding.emailEt.alpha = 0.5f
                    binding.passET.isEnabled = false
                    binding.passET.alpha = 0.5f
                    binding.textView2.isEnabled = false
                    binding.textView2.alpha = 0.5f
                }

                is UiState.Failure -> {
                    binding.progressBarSignIn.visibility = View.GONE
                    binding.button.isEnabled = true
                    binding.button.alpha = 1f
                    binding.emailEt.isEnabled = true
                    binding.emailEt.alpha = 1f
                    binding.passET.isEnabled = true
                    binding.passET.alpha = 1f
                    binding.textView2.isEnabled = true
                    binding.textView2.alpha = 1f
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarSignIn.visibility = View.GONE
                    navRegister.navigateFrag(HomeFragment(), false)
                }
            }
        }
    }

    private fun setSignInClickListener() {
        binding.button.setOnClickListener {
            val email = binding.emailEt.text.toString()
            val pass = binding.passET.text.toString()

            if (email.isNotEmpty() && pass.isNotEmpty()) {
                viewModel.signIn(email, pass)
            } else {
                Toast.makeText(
                    requireContext(),
                    "Email or password cannot be left blank!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setRegisterTvClickListener() {
        binding.textView2.setOnClickListener {
            navRegister.navigateFrag(RegisterFragment(), false)
        }
    }

}