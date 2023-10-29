package com.example.confessme.data.model

data class User(
    val uid: String = "",
    val email: String = "",
    val password: String = "",
    val bio: String = "",
    val userName: String = "",
    val imageUrl: String = "",
    val followCount: Int = 0,
    val followersCount: Int = 0,
    var timestampFollow: Any? = null,
    var isFollowing: Boolean = false,
    var isFollowingInProgress: Boolean = false
)
