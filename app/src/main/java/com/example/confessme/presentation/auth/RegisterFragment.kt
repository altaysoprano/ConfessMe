package com.example.confessme.presentation.auth

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import com.example.confessme.databinding.FragmentRegisterBinding
import com.example.confessme.presentation.profile.edit_set_profile.SetProfileFragment
import com.example.confessme.presentation.utils.FragmentNavigation
import com.example.confessme.presentation.utils.UiState
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

        setSignInTvClickListener()
        setSignUpButtonClickListener()
        observeSignUp()
        setOutsideTouchListener()

        return binding.root
    }

    private fun setSignUpButtonClickListener() {
        binding.button.setOnClickListener {
            val email = binding.emailEt.text.toString().trim()
            val pass = binding.passwordEt.text.toString()
            val confirmPass = binding.passwordAgainEt.text.toString()

            viewModel.signUp(email, pass, confirmPass)
        }
    }

    private fun observeSignUp() {
        viewModel.signUpState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBarSignUp.visibility = View.VISIBLE
                    binding.button.isEnabled = false
                    binding.button.alpha = 0.5f
                    binding.emailEt.isEnabled = false
                    binding.emailEt.alpha = 0.5f
                    binding.passwordEt.isEnabled = false
                    binding.passwordEt.alpha = 0.5f
                    binding.passwordAgainEt.isEnabled = false
                    binding.passwordAgainEt.alpha = 0.5f
                    binding.textView2.isEnabled = false
                    binding.textView2.alpha = 0.5f
                }

                is UiState.Failure -> {
                    binding.progressBarSignUp.visibility = View.GONE
                    binding.button.isEnabled = true
                    binding.button.alpha = 1f
                    binding.emailEt.isEnabled = true
                    binding.emailEt.alpha = 1f
                    binding.passwordEt.isEnabled = true
                    binding.passwordEt.alpha = 1f
                    binding.passwordAgainEt.isEnabled = true
                    binding.passwordAgainEt.alpha = 1f
                    binding.textView2.isEnabled = true
                    binding.textView2.alpha = 1f
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarSignUp.visibility = View.GONE
                    Toast.makeText(requireContext(), state.data, Toast.LENGTH_SHORT)
                        .show()
                    navRegister.navigateFrag(SetProfileFragment(), false)
                }

                else -> {
                }
            }
        }
    }

    private fun setSignInTvClickListener() {
        binding.textView2.setOnClickListener {
            navRegister.navigateFrag(LoginFragment(), false)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setOutsideTouchListener() {
        val rootLayout = binding.root
        rootLayout.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val inputMethodManager = requireContext().getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(rootLayout.windowToken, 0)

                binding.emailEt.clearFocus()
                binding.passwordEt.clearFocus()
                binding.passwordAgainEt.clearFocus()
            }
            false
        }
    }

}