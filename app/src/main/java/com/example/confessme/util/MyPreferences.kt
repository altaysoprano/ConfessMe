package com.example.confessme.util

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate

class MyPreferences(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "MyPrefs"
        private const val NIGHT_MODE = "NightMode"
        private const val SELECTED_LANGUAGE = "SelectedLanguage"
    }

    fun setNightMode(isNightMode: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean(NIGHT_MODE, isNightMode)
        editor.apply()
    }

    fun isNightModeEnabled(context: Context): Boolean {
        val uiModeFlags = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val systemNightMode = when (uiModeFlags) {
            Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }
        return sharedPreferences.getBoolean(NIGHT_MODE, systemNightMode)
    }


    fun saveSelectedLanguage(languageCode: String) {
        val editor = sharedPreferences.edit()
        editor.putString(SELECTED_LANGUAGE, languageCode)
        editor.apply()
    }

    fun getSelectedLanguage(): String {
        return sharedPreferences.getString(SELECTED_LANGUAGE, "") ?: ""
    }
}
