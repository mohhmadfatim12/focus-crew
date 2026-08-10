package com.focuscrew.app.domain.streak

data class StreakStats(
    val currentStreakDays: Int,
    val bestStreakDays: Int,
    val totalCompletedSessions: Int,
    val totalFocusMinutes: Long,
    val totalDistractionsParked: Int,
    val badges: List<MilestoneBadge>
)

data class MilestoneBadge(
    val id: String,
    val title: String,
    val icon: String,
    val description: String,
    val isUnlocked: Boolean,
    val progressCurrent: Int,
    val progressMax: Int,
    val progressText: String
)
