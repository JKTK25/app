package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.BlockedIncident
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedIncidentDao {
    @Query("SELECT * FROM blocked_incidents ORDER BY timestamp DESC")
    fun getAllIncidents(): Flow<List<BlockedIncident>>

    @Query("SELECT * FROM blocked_incidents ORDER BY timestamp DESC LIMIT 10")
    fun getRecentIncidents(): Flow<List<BlockedIncident>>

    @Query("SELECT COUNT(*) FROM blocked_incidents")
    fun getTotalBlockedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM blocked_incidents WHERE timestamp >= :sinceTimestamp")
    fun getBlockedCountSince(sinceTimestamp: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncident(incident: BlockedIncident): Long

    @Query("DELETE FROM blocked_incidents WHERE id = :id")
    suspend fun deleteIncidentById(id: Long)

    @Query("DELETE FROM blocked_incidents")
    suspend fun clearAllIncidents()
}
