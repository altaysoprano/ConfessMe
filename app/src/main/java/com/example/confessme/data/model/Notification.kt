package com.example.confessme.data.model

data class Notification(
    val id: String = "",
    val type: String = "",
    val confessionId: String = "",
    val userId: String = "",
    val fromUserId: String = "",
    val fromUserToken: String = "",
    val text: String = "",
    val fromUserUsername: String = "",
    val fromUserImageUrl: String = "",
    var timestamp: Any? = null,
    var seen: Boolean = false
)
