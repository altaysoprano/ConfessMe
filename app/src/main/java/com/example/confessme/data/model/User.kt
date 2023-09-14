package com.example.confessme.data.model

data class User(
    val email: String = "",
    val password: String = "",
    val bio: String = "",
    val userName: String = "",
    val imageUrl: String = "",
    val followers: List<String> = emptyList(),
    val following: List<String> = emptyList()
)
