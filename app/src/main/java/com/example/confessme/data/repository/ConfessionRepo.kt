package com.example.confessme.data.repository

import com.example.confessme.data.model.Confession
import com.example.confessme.util.ConfessionCategory
import com.example.confessme.util.UiState
import com.google.firebase.firestore.DocumentReference

interface ConfessionRepo {

    fun addConfession(userUid: String, confessionText: String, result: (UiState<String>) -> Unit)
    fun fetchConfessions(
        userUid: String,
        limit: Long,
        confessionCategory: ConfessionCategory,
        result: (UiState<List<Confession>>) -> Unit
    )
    fun getConfession(confessionId: String, result: (UiState<Confession?>) -> Unit)
    fun addAnswer(
        confessionId: String,
        answerText: String,
        result: (UiState<Confession?>) -> Unit
    )
    fun addFavorite(
        favorited: Boolean,
        confessionId: String,
        callback: (UiState<Confession?>) -> Unit
    )
    suspend fun favoriteAnswer(isFavorited: Boolean, confessionId: String, result: (UiState<Confession?>) -> Unit)
    fun deleteAnswer(confessionId: String, result: (UiState<Confession?>) -> Unit)
    fun deleteConfession(confessionId: String, result: (UiState<Confession?>) -> Unit)
    fun addBookmark(confessionId: String, timestamp: String, userUid: String, result: (UiState<String>) -> Unit)
    fun fetchBookmarks(limit: Long, result: (UiState<List<Confession?>>) -> Unit)
    fun removeBookmark(confessionId: String, result: (UiState<DocumentReference>) -> Unit)
    fun fetchFollowedUsersConfessions(
        limit: Long,
        result: (UiState<List<Confession>>) -> Unit
    )
}
