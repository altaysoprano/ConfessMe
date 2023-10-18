package com.example.confessme.presentation

import android.app.AlertDialog
import android.content.Context

class DialogHelper(private val context: Context?) {
    fun showDeleteConfessionDialog(itemType: String, onConfirmDelete: () -> Unit) {
        val alertDialog = AlertDialog.Builder(context)
            .setTitle("DELETE ${itemType.uppercase()}")
            .setMessage("Are you sure you really want to delete this $itemType?")
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
