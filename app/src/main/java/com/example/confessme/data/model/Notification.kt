package com.example.confessme.data.model

import com.example.confessme.util.NotificationType

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
    val description: String = "",
    var timestamp: Any? = null,
    var seen: Boolean = false
)
