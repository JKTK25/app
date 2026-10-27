package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "nofap_streaks")
data class NoFapStreak(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTimestamp: Long = System.currentTimeMillis(),
    val goalDays: Int = 30,
    val personalMotivation: String = "Building self-discipline, mental clarity, and healthy habits",
    val longestStreakDays: Int = 0,
    val totalResets: Int = 0,
    val lastCheckInTimestamp: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

@Entity(tableName = "nofap_checkins")
data class NoFapCheckIn(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val streakId: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val dayNumber: Int = 1,
    val urgeLevel: Int = 1, // 1 (Low) to 5 (High)
    val reflectionNote: String = "",
    val mood: String = "Energized" // Energized, Focused, Calm, Struggling, Victorious
)
