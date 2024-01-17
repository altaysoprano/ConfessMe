package com.example.confessme.presentation.ui

import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import com.example.confessme.MainActivity
import com.example.confessme.R
import com.example.confessme.databinding.FragmentProfileBinding
import com.example.confessme.databinding.FragmentSettingsBinding
import com.example.confessme.presentation.ConfessMeDialog
import com.example.confessme.presentation.SettingsViewModel
import com.example.confessme.util.Constants
import com.example.confessme.util.MyPreferences
import com.example.confessme.util.UiState
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.core.view.Change
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private lateinit var binding: FragmentSettingsBinding
    private lateinit var navRegister: FragmentNavigation
    private lateinit var dialogHelper: ConfessMeDialog
    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        (activity as AppCompatActivity?)!!.title = context?.getString(R.string.settings)
        (activity as AppCompatActivity?)!!.setSupportActionBar(binding.settingsToolbar)
        navRegister = activity as FragmentNavigation
        dialogHelper = ConfessMeDialog(requireContext())
        setHasOptionsMenu(true)

        (activity as AppCompatActivity?)!!.supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_back)
        }

        setOnClickListeners()
        observeCheckIfGoogleSignIn()
        observeConfirmGoogleAccount()

        return binding.root
    }

    private fun observeCheckIfGoogleSignIn() {
        viewModel.checkIfGoogleSignInState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBarSettings.visibility = View.VISIBLE
                }

                is UiState.Failure -> {
                    binding.progressBarSettings.visibility = View.GONE
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarSettings.visibility = View.GONE
                    val isGoogleSignIn = state.data

                    if(isGoogleSignIn) {
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(getString(R.string.default_web_client_id))
                            .requestEmail()
                            .build()

                        val signInIntent = GoogleSignIn.getClient(requireActivity(), gso).signInIntent
                        startActivityForResult(signInIntent, Constants.RC_SIGN_IN)
                    } else {
                        val confirmPasswordFragment = ConfirmPasswordFragment()

                        confirmPasswordFragment.show(
                            requireActivity().supportFragmentManager,
                            "ConfirmPasswordFragment"
                        )
                    }
                }
            }
        }
    }

    fun observeConfirmGoogleAccount() {
        viewModel.deleteAccountState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBarSettings.visibility = View.VISIBLE
                }

                is UiState.Failure -> {
                    binding.progressBarSettings.visibility = View.GONE

                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT)
                        .show()
                }

                is UiState.Success -> {
                    binding.progressBarSettings.visibility = View.GONE

                    val fragmentManager = parentFragmentManager
                    for (i in 0 until fragmentManager.backStackEntryCount) {
                        fragmentManager.popBackStack()
                    }
                    fragmentManager.beginTransaction()
                        .replace(R.id.coordinator, LoginFragment())
                        .commit()

                    Toast.makeText(requireContext(), state.data, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val googleSignInAccount = GoogleSignIn.getLastSignedInAccount(requireContext())

        if (requestCode == Constants.RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    val idToken = account.idToken
                    if (idToken != null) {
                        viewModel.deleteAccountWithConfessionsAndSignOut("", googleSignInAccount)
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

    private fun showSelectLanguageDialog() {
        val options = arrayOf(
            "English",
            "Türkçe",
            "中文",
            "Español",
            "Français",
            "日本語",
            "Deutsch",
            "العربية"
        )

        val builder = AlertDialog.Builder(requireContext())
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> {
                    setLocale("en")
                    Toast.makeText(
                        requireContext(),
                        R.string.application_language_set_to_english,
                        Toast.LENGTH_SHORT
                    ).show()
                }

                1 -> {
                    setLocale("tr")
                    Toast.makeText(
                        requireContext(),
                        R.string.application_language_set_to_turkish,
                        Toast.LENGTH_SHORT
                    ).show()
                }

                2 -> {
                    setLocale("zh")
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.application_language_set_to_chinese),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                3 -> {
                    setLocale("es")
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.application_language_set_to_spanish),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                4 -> {
                    setLocale("fr")
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.application_language_set_to_french),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                5 -> {
                    setLocale("ja")
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.application_language_set_to_japanese),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                6 -> {
                    setLocale("de")
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.application_language_set_to_german),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                7 -> {
                    setLocale("ar")
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.application_language_set_to_arabic),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun setLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val configuration = Configuration()
        configuration.setLocale(locale)
        requireContext().resources.updateConfiguration(
            configuration,
            requireContext().resources.displayMetrics
        )

        val myPreferences = MyPreferences(requireContext())
        myPreferences.saveSelectedLanguage(languageCode)
        updateUserLanguage(languageCode)

        (activity as? MainActivity)?.restartActivity()
    }

    private fun setOnClickListeners(){
        binding.changePasswordButton.setOnClickListener {
            val changePasswordFragment = ChangePasswordFragment()

            changePasswordFragment.show(
                requireActivity().supportFragmentManager,
                "ConfessAnswerFragment"
            )
        }

        binding.selectLanguageButton.setOnClickListener {
            showSelectLanguageDialog()
        }

        binding.deleteAccountButton.setOnClickListener {
            dialogHelper.showDialog(
                title = getString(R.string.delete_account),
                message = getString(R.string.delete_account_confirmation),
                positiveButtonText = getString(R.string.yes),
                negativeButtonText = getString(R.string.no),
                onConfirm = {
                    viewModel.checkIfGoogleSignIn()
                }
            )
        }

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                requireActivity().onBackPressed()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun updateUserLanguage(language: String) {
        viewModel.updateLanguage(language)
    }
}