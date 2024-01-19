package com.example.confessme.data.repository

import com.example.confessme.data.model.Notification
import com.example.confessme.presentation.utils.UiState

interface NotificationRepo {
    fun fetchNotificationsForUser(
        limit: Long,
        forNotifications: Boolean,
        result: (UiState<List<Notification>>) -> Unit
    )
}