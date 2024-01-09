package com.example.confessme.presentation.ui

import android.annotation.SuppressLint
import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.viewModels
import com.example.confessme.R
import com.example.confessme.databinding.FragmentLoginBinding
import com.example.confessme.presentation.ConfessMeDialog
import com.example.confessme.presentation.LoginViewModel
import com.example.confessme.util.Constants
import com.example.confessme.util.Constants.Companion.RC_SIGN_IN
import com.example.confessme.util.MyPreferences
import com.example.confessme.util.MyUtils
import com.example.confessme.util.ShareHelper
import com.example.confessme.util.UiState
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private lateinit var binding: FragmentLoginBinding
    private lateinit var navRegister: FragmentNavigation
    private val viewModel: LoginViewModel by viewModels()
    private var isUserLoggedIn: Boolean = true
    private lateinit var dialogHelper: ConfessMeDialog
    private lateinit var myPreferences: MyPreferences
    private lateinit var shareHelper: ShareHelper
    private lateinit var myUserName: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentLoginBinding.inflate(inflater, container, false)
        navRegister = activity as FragmentNavigation
        isUserLoggedIn = viewModel.isUserLoggedIn
        dialogHelper = ConfessMeDialog(requireContext())
        myPreferences = MyPreferences(requireContext())
        shareHelper = ShareHelper(requireContext())

        checkIfUserLoggedIn()
        setGoogleSignInButtonDesign()
        setRegisterTvClickListener()
        setSignInClickListener()
        observeSignIn()
        setOutsideTouchListener()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.googleSignInButton.setOnClickListener {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

            val signInIntent = GoogleSignIn.getClient(requireActivity(), gso).signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }
    }

    private fun observeSignIn() {
        viewModel.signInState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    setLoadingState(isLoading = true)
                }

                is UiState.Failure -> {
                    setLoadingState(isLoading = false)
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarSignIn.visibility = View.GONE
                    myUserName = state.data
                    navRegister.navigateFrag(SearchFragment(), false)
                    dialogHelper.showDialog(
                        getString(R.string.share_profile),
                        getString(R.string.share_your_profile_with_your_friends_for),
                        getString(R.string.share),
                        getString(R.string.maybe_later),
                        { shareProfile() })
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
                    getString(R.string.email_or_password_cannot_be_left_blank),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val googleSignInAccount = GoogleSignIn.getLastSignedInAccount(requireContext())

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    val idToken = account.idToken
                    if (idToken != null) {
                        viewModel.googleSignIn(idToken, googleSignInAccount)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.google_sign_in_failed_please_try_again),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: ApiException) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.google_sign_in_failed_please_try_again),
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

    private fun setLoadingState(isLoading: Boolean) {
        val alpha = if(isLoading) 0.5f else 1f
        val isEnabled = !isLoading
        val progressBarVisibility = if(isLoading) View.VISIBLE else View.GONE

        binding.progressBarSignIn.visibility = progressBarVisibility
        binding.button.isEnabled = isEnabled
        binding.button.alpha = alpha
        binding.emailEt.isEnabled = isEnabled
        binding.emailEt.alpha = alpha
        binding.passET.isEnabled = isEnabled
        binding.passET.alpha = alpha
        binding.textView2.isEnabled = isEnabled
        binding.textView2.alpha = alpha
        binding.googleSignInButton.isEnabled = isEnabled
        binding.googleSignInButton.alpha = alpha
    }

    private fun shareProfile() {
        if(!myUserName.isNullOrEmpty()) {
            shareHelper.shareImage(myUserName)
        } else {
            Toast.makeText(context, getString(R.string.share_error), Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setGoogleSignInButtonDesign() {
        binding.googleSignInButton.getChildAt(0)?.let {
            val smaller = Math.min(it.paddingLeft, it.paddingRight)
            it.setPadding(smaller, it.paddingTop, smaller, it.paddingBottom)
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
                binding.passET.clearFocus()
            }
            false
        }
    }

    private fun checkIfUserLoggedIn() {
        if (isUserLoggedIn) {
            navRegister.navigateFrag(HomeFragment(), false)
        }
    }
}