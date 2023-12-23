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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.confessme.MainActivity
import com.example.confessme.R
import com.example.confessme.databinding.FragmentProfileBinding
import com.example.confessme.databinding.FragmentSettingsBinding
import com.example.confessme.util.MyPreferences
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.core.view.Change
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private lateinit var binding: FragmentSettingsBinding
    private lateinit var navRegister: FragmentNavigation

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
        val options = arrayOf("English", "Türkçe")

        val builder = AlertDialog.Builder(requireContext())
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> setLocale("en")
                1 -> setLocale("tr")
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
}