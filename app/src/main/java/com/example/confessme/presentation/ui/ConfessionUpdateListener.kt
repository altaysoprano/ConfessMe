package com.example.confessme.presentation.ui

import com.example.confessme.data.model.Confession

interface ConfessionUpdateListener {
    fun updateConfessionItem(position: Int, updatedConfession: Confession)
    fun findPositionById(confessionId: String): Int
}
