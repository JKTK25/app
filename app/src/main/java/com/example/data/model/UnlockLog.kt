package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "unlock_logs")
data class UnlockLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String = "PHONE_UNLOCKED",
    val shieldStatus: String = "ACTIVE_MONITORING",
    val threatsBlockedCount: Int = 0,
    val details: String = "Device unlocked. Aggressive porn filter armed and scanning."
)
