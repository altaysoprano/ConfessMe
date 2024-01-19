package com.example.confessme.di

import android.app.Application
import com.example.confessme.utils.MyPreferences
import com.example.confessme.utils.MyUtils
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