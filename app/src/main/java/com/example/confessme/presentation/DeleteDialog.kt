package com.example.confessme.presentation

import android.app.AlertDialog
import android.content.Context

class DeleteDialog(private val context: Context?) {
    fun showDialog(title: String, message: String, onConfirmDelete: () -> Unit) {
        val alertDialog = AlertDialog.Builder(context)
            .setTitle(title.toUpperCase())
            .setMessage(message)
            .setPositiveButton("Yes") { _, _ ->
                onConfirmDelete()
            }
            .setNegativeButton("No") { _, _ ->

            }
            .create()

        alertDialog.show()
    }
}
