package com.example.confessme.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.example.confessme.R
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MyUtils {
    fun calculateTimeSinceConfession(confessionTimestamp: Timestamp, context: Context): String {
        val currentTime = Timestamp.now()
        val timeDifference = currentTime.seconds - confessionTimestamp.seconds
        
        val minutes = timeDifference / 60
        val hours = minutes / 60
        val days = hours / 24
        val weeks = days / 7
        val diffMonths = days / 30
        val diffYears = days / 365

        return when {
            timeDifference < 60 -> "$timeDifference " + context.getString(R.string.seconds_ago)
            minutes < 60 -> "$minutes " + context.getString(R.string.minutes_ago)
            hours < 24 -> "$hours " + context.getString(R.string.hours_ago)
            days < 7 -> {
                if (days == 1L) {
                    context.getString(R.string._1_day_ago)
                } else {
                    "$days " + context.getString(R.string.days_ago)
                }
            }
            days < 30 -> {
                if (weeks == 1L) {
                    context.getString(R.string._1_week_ago)
                } else {
                    "$weeks " + context.getString(R.string.weeks_ago)
                }
            }
            diffYears == 1L -> context.getString(R.string._1_year_ago)
            diffYears > 1 -> "$diffYears " + context.getString(R.string.years_ago)
            else -> {
                if (diffMonths == 1L) {
                    context.getString(R.string._1_month_ago)
                } else {
                    "$diffMonths " +  context.getString(R.string.months_ago)
                }
            }
        }
    }

    fun convertFirestoreTimestampToReadableDate(timestamp: Any?, context: Context): String {
        return try {
            if (timestamp is Timestamp) {
                val seconds = timestamp.seconds
                val nanoseconds = timestamp.nanoseconds

                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                val date = Date(seconds * 1000 + nanoseconds / 1000000)
                sdf.format(date)
            } else {
                context.getString(R.string.invalid_date_format)
            }
        } catch (e: Exception) {
            context.getString(R.string.invalid_date_format)
        }
    }

    fun applyAppTheme(myPreferences: MyPreferences) {
        val isDarkModeEnabled = myPreferences.isNightModeEnabled()

        if (isDarkModeEnabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
}
