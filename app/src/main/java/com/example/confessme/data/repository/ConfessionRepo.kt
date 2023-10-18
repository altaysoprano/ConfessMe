package com.example.confessme.data.repository

import com.example.confessme.data.model.Confession
import com.example.confessme.util.ConfessionCategory
import com.example.confessme.util.UiState

interface ConfessionRepo {

    fun addConfession(userUid: String, confessionText: String, result: (UiState<String>) -> Unit)
    fun fetchConfessions(
        userUid: String,
        limit: Long,
        confessionCategory: ConfessionCategory,
        result: (UiState<List<Confession>>) -> Unit
    )

    fun addAnswer(
        confessionId: String,
        answerText: String,
        result: (UiState<Confession?>) -> Unit
    )

    fun addFavorite(
        confessionId: String,
        callback: (UiState<Confession?>) -> Unit
    )

    fun favoriteAnswer(confessionId: String, result: (UiState<Confession?>) -> Unit)
    fun deleteAnswer(confessionId: String, result: (UiState<Confession?>) -> Unit)
    fun deleteConfession(confessionId: String, result: (UiState<Confession?>) -> Unit)
}