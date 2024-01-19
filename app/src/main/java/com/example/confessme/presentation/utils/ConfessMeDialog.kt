package com.example.confessme.presentation.utils

import android.app.AlertDialog
import android.content.Context

class ConfessMeDialog(private val context: Context?) {
    fun showDialog(title: String, message: String, positiveButtonText: String,
                   negativeButtonText: String, onConfirm: () -> Unit) {
        val alertDialog = AlertDialog.Builder(context)
            .setTitle(title.toUpperCase())
            .setMessage(message)
            .setPositiveButton(positiveButtonText) { _, _ ->
                onConfirm()
            }
            .setNegativeButton(negativeButtonText) { _, _ ->

            }
            .create()
        alertDialog.show()
    }
}
