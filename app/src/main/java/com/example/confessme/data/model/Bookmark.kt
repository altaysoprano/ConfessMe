package com.example.confessme.data.model

import com.google.firebase.Timestamp


data class Bookmark(
    val userId: String = "",
    val confessionId: String = "",
    val timestamp: Timestamp
)
