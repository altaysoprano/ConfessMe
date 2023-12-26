package com.example.confessme.di

import android.app.Application
import android.graphics.Typeface
import android.util.Log
import com.example.confessme.util.MyPreferences
import com.example.confessme.util.MyUtils
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        val myPreferences = MyPreferences(applicationContext)
        MyUtils.applyAppTheme(myPreferences, applicationContext)
        MyUtils.setAppLanguage(myPreferences, applicationContext)
    }

}