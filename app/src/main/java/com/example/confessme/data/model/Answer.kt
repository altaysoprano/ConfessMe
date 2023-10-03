package com.example.confessme.data.model

data class Answer(
    val text: String = "",
    val username: String = "",
    val fromUserUsername: String = "",
    val email: String = "",
    val fromUserEmail: String = "",
    val fromUserImageUrl: String = "",
    val imageUrl: String = "",
    val timestamp: Any? = null,
    var isExpanded: Boolean = false,
    var favorited: Boolean = false
)
