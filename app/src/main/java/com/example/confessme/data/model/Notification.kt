package com.example.confessme.data.model

data class Notification(
    val id: String = "",
    val confessionId: String = "",
    val userId: String = "",
    val fromUserId: String = "",
    val text: String = "",
    val fromUserUsername: String = "",
    val fromUserImageUrl: String = "",
    val description: String = "",
    var timestamp: Any? = null
)
