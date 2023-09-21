package com.example.confessme.data.model

data class Confession(
    val text: String = "",
    val username: String = "",
    val fromUserUsername: String = "",
    val fromUserImageUrl: String = "",
    val imageUrl: String = "",
    val timestamp: Any? = null
)
