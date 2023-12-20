package com.example.confessme.presentation

import android.app.AlertDialog
import android.content.Context
import com.example.confessme.R

class ConfessMeDialog(private val context: Context?) {
    fun showDialog(title: String, message: String, onConfirm: () -> Unit) {
        val alertDialog = AlertDialog.Builder(context)
            .setTitle(title.toUpperCase())
            .setMessage(message)
            .setPositiveButton(context?.getString(R.string.yes)) { _, _ ->
                onConfirm()
            }
            .setNegativeButton(context?.getString(R.string.no)) { _, _ ->

            }
            .create()
        alertDialog.show()
    }
}
