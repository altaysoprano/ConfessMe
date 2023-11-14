package com.example.confessme.presentation

import android.app.AlertDialog
import android.content.Context

class ConfessMeDialog(private val context: Context?) {
    fun showDialog(title: String, message: String, onConfirm: () -> Unit) {
        val alertDialog = AlertDialog.Builder(context)
            .setTitle(title.toUpperCase())
            .setMessage(message)
            .setPositiveButton("Yes") { _, _ ->
                onConfirm()
            }
            .setNegativeButton("No") { _, _ ->

            }
            .create()
        alertDialog.show()
    }
}
