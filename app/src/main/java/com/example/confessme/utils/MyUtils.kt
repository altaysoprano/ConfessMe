package com.example.confessme.utils

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import com.example.confessme.R
import com.example.confessme.presentation.home.notifications.NotificationType
import com.google.android.material.snackbar.Snackbar
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

    fun applyAppTheme(myPreferences: MyPreferences, context: Context) {
        val isDarkModeEnabled = myPreferences.isNightModeEnabled(context)

        if (isDarkModeEnabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    fun setAppLanguage(myPreferences: MyPreferences, context: Context) {
        val selectedLanguage = myPreferences.getSelectedLanguage()

        val locale = if (selectedLanguage.isNotEmpty()) {
            Locale(selectedLanguage)
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Resources.getSystem().configuration.locales[0]
            } else {
                Resources.getSystem().configuration.locale
            }
        }

        Locale.setDefault(locale)
        val configuration = Configuration()
        configuration.setLocale(locale)
        context.resources.updateConfiguration(configuration, context.resources.displayMetrics)
    }

    fun getAppLanguage(myPreferences: MyPreferences): String {
        val selectedLanguage = myPreferences.getSelectedLanguage()

        return if (selectedLanguage.isNotEmpty()) {
            selectedLanguage
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Resources.getSystem().configuration.locales[0].language
            } else {
                Resources.getSystem().configuration.locale.language
            }
        }
    }

    fun getNotificationText(languageCode: String, notificationType: NotificationType): String {
        return when (notificationType) {
            NotificationType.Confessed -> when (languageCode) {
                "en" -> "confessed"
                "tr" -> "itiraf etti"
                "zh" -> "坦白了"
                "es" -> "confesó"
                "fr" -> "a confessé"
                "ja" -> "告白しました"
                "de" -> "hat gebeichtet"
                "ar" -> "اعترف"
                else -> "confessed"
            }
            NotificationType.ConfessionLike -> when (languageCode) {
                "en" -> "liked this confession"
                "tr" -> "bu itirafı beğendi"
                "zh" -> "喜欢了这个坦白"
                "es" -> "le gustó esta confesión"
                "fr" -> "a aimé cette confession"
                "ja" -> "この告白をいいね！しました"
                "de" -> "hat dieses Geständnis gemocht"
                "ar" -> "أعجب بهذا الاعتراف"
                else -> "liked this confession"
            }
            NotificationType.AnswerLike -> when (languageCode) {
                "en" -> "liked this answer"
                "tr" -> "şu cevabı beğendi"
                "zh" -> "喜欢了这个回答"
                "es" -> "le gustó esta respuesta"
                "fr" -> "a aimé cette réponse"
                "ja" -> "この回答をいいね！しました"
                "de" -> "hat diese Antwort gemocht"
                "ar" -> "أعجب بهذه الإجابة"
                else -> "liked this answer"
            }
            NotificationType.Followed -> when (languageCode) {
                "en" -> "followed you"
                "tr" -> "seni takip etti"
                "zh" -> "关注了你"
                "es" -> "te ha seguido"
                "fr" -> "vous a suivi"
                "ja" -> "あなたをフォローしました"
                "de" -> "folgt dir"
                "ar" -> "قام بمتابعتك"
                else -> "followed you"
            }
            NotificationType.ConfessionReply -> when (languageCode) {
                "en" -> "replied to this confession"
                "tr" -> "bu itirafa yanıt verdi"
                "zh" -> "回复了这个坦白"
                "es" -> "respondió a esta confesión"
                "fr" -> "a répondu à cette confession"
                "ja" -> "この告白に返信しました"
                "de" -> "hat auf dieses Geständnis geantwortet"
                "ar" -> "رد على هذا الاعتراف"
                else -> "replied to this confession"
            }
        }
    }

    fun showSnackbar(
        rootView: View,
        context: Context,
        descriptionText: String,
        buttonText: String,
        onButtonClicked: () -> Unit,
        activity: Activity
    ) {
        val snackbar = Snackbar.make(
            rootView,
            descriptionText,
            Snackbar.LENGTH_LONG
        )
        snackbar.setAction(buttonText) {
            onButtonClicked()
        }

        val bottomNavigationView = activity.findViewById<View>(R.id.bottomNavigationView)
        snackbar.setAnchorView(bottomNavigationView)
        snackbar.show()
    }

    fun copyTextToClipboard(text: String, context: Context): Boolean {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("Answer", text)
        clipboardManager.setPrimaryClip(clipData)
        Toast.makeText(context, "Answer copied to clipboard", Toast.LENGTH_SHORT).show()
        return true
    }

    fun View.disable() {
        this.isEnabled = false
        this.alpha = 0.5f
    }

    fun View.enable() {
        this.isEnabled = true
        this.alpha = 1f
    }

    fun showKeyboard(activity: Activity, view: View) {
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    fun hideKeyboard(activity: Activity, view: View) {
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
    }
}
