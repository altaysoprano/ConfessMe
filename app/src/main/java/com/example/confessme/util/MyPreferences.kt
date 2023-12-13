package com.example.confessme.util

import android.content.Context
import android.content.SharedPreferences

class MyPreferences(context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "MyPrefs"
        private const val NIGHT_MODE = "NightMode"
    }

    fun setNightMode(isNightMode: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean(NIGHT_MODE, isNightMode)
        editor.apply()
    }

    fun isNightModeEnabled(): Boolean {
        return sharedPreferences.getBoolean(NIGHT_MODE, false)
    }
}
