package com.example.confessme.presentation

import android.app.AlertDialog
import android.content.Context

class DialogHelper(private val context: Context?) {
    fun showDialog(title: String, message: String, onConfirmDelete: () -> Unit) {
        val alertDialog = AlertDialog.Builder(context)
            .setTitle(title.toUpperCase())
            .setMessage(message)
            .setPositiveButton("Yes") { _, _ ->
                onConfirmDelete()
            }
            .setNegativeButton("No") { _, _ ->
                // Silme işlemi iptal edildi.
            }
            .create()

        alertDialog.show()
    }
}
