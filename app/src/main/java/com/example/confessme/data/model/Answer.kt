package com.example.confessme.data.model

data class Answer(
    var id: String = "",
    val text: String = "",
    val username: String = "",
    val fromUserUsername: String = "",
    val fromUserImageUrl: String = "",
    val imageUrl: String = "",
    val timestamp: Any? = null,
    var isExpanded: Boolean = false,
    var isFavorited: Boolean = false
)
