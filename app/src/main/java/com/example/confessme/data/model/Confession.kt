package com.example.confessme.data.model

data class Confession(
    val id: String = "",
    val userId: String = "",
    val fromUserId: String = "",
    val fromUserToken: String = "",
    val userToken: String = "",
    val anonymousId: String = "",
    val text: String = "",
    val username: String = "",
    val fromUserUsername: String = "",
    val email: String = "",
    val fromUserEmail: String = "",
    val fromUserImageUrl: String = "",
    val imageUrl: String = "",
    var timestamp: Any? = null,
    var isExpanded: Boolean = false,
    var favorited: Boolean = false,
    var answered: Boolean = false,
    var answer: Answer = Answer()
)
