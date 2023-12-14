package com.example.confessme.util

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MyUtils {
    fun calculateTimeSinceConfession(confessionTimestamp: Timestamp): String {
        val currentTime = Timestamp.now()
        val timeDifference = currentTime.seconds - confessionTimestamp.seconds

        val minutes = timeDifference / 60
        val hours = minutes / 60
        val days = hours / 24
        val weeks = days / 7
        val diffMonths = days / 30
        val diffYears = days / 365

        return when {
            timeDifference < 60 -> "$timeDifference seconds ago"
            minutes < 60 -> "$minutes minutes ago"
            hours < 24 -> "$hours hours ago"
            days < 7 -> {
                if (days == 1L) {
                    "1 day ago"
                } else {
                    "$days days ago"
                }
            }
            days < 30 -> {
                if (weeks == 1L) {
                    "1 week ago"
                } else {
                    "$weeks weeks ago"
                }
            }
            diffYears == 1L -> "1 year ago"
            diffYears > 1 -> "$diffYears years ago"
            else -> {
                if (diffMonths == 1L) {
                    "1 month ago"
                } else {
                    "$diffMonths months ago"
                }
            }
        }
    }

    fun convertFirestoreTimestampToReadableDate(timestamp: Any?): String {
        return try {
            if (timestamp is Timestamp) {
                val seconds = timestamp.seconds
                val nanoseconds = timestamp.nanoseconds

                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                val date = Date(seconds * 1000 + nanoseconds / 1000000)
                sdf.format(date)
            } else {
                "Invalid date format"
            }
        } catch (e: Exception) {
            "Invalid date format"
        }
    }
}
