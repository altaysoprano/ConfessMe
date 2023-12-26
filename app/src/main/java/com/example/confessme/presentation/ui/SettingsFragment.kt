package com.example.confessme.presentation.ui

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
import com.example.confessme.presentation.SettingsViewModel
import com.example.confessme.util.MyPreferences
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.core.view.Change
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private lateinit var binding: FragmentSettingsBinding
    private lateinit var navRegister: FragmentNavigation
    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        (activity as AppCompatActivity?)!!.title = context?.getString(R.string.settings)
        (activity as AppCompatActivity?)!!.setSupportActionBar(binding.settingsToolbar)
        navRegister = activity as FragmentNavigation
        setHasOptionsMenu(true)

        (activity as AppCompatActivity?)!!.supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_back)
        }

        binding.changePasswordButton.setOnClickListener {
            val changePasswordFragment = ChangePasswordFragment()

            changePasswordFragment.show(requireActivity().supportFragmentManager, "ConfessAnswerFragment")
        }

        binding.selectLanguageButton.setOnClickListener {
            showSelectLanguageDialog()
        }

        return binding.root
    }

    private fun showSelectLanguageDialog() {
        val options = arrayOf("English", "Türkçe", "中文", "Español", "Français", "日本語", "Deutsch", "العربية")

        val builder = AlertDialog.Builder(requireContext())
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> {
                    setLocale("en")
                    Toast.makeText(requireContext(), R.string.application_language_set_to_english, Toast.LENGTH_SHORT).show()
                }
                1 -> {
                    setLocale("tr")
                    Toast.makeText(requireContext(), R.string.application_language_set_to_turkish, Toast.LENGTH_SHORT).show()
                }
                2 -> {
                    setLocale("zh")
                    Toast.makeText(requireContext(), getString(R.string.application_language_set_to_chinese), Toast.LENGTH_SHORT).show()
                }
                3 -> {
                    setLocale("es")
                    Toast.makeText(requireContext(), getString(R.string.application_language_set_to_spanish), Toast.LENGTH_SHORT).show()
                }
                4 -> {
                    setLocale("fr")
                    Toast.makeText(requireContext(), getString(R.string.application_language_set_to_french), Toast.LENGTH_SHORT).show()
                }
                5 -> {
                    setLocale("ja")
                    Toast.makeText(requireContext(), getString(R.string.application_language_set_to_japanese), Toast.LENGTH_SHORT).show()
                }
                6 -> {
                    setLocale("de")
                    Toast.makeText(requireContext(), getString(R.string.application_language_set_to_german), Toast.LENGTH_SHORT).show()
                }
                7 -> {
                    setLocale("ar")
                    Toast.makeText(requireContext(), getString(R.string.application_language_set_to_arabic), Toast.LENGTH_SHORT).show()
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
        requireContext().resources.updateConfiguration(configuration, requireContext().resources.displayMetrics)

        val myPreferences = MyPreferences(requireContext())
        myPreferences.saveSelectedLanguage(languageCode)
        updateUserLanguage(languageCode)

        (activity as? MainActivity)?.restartActivity()
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