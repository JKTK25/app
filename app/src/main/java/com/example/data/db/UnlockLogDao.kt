package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.UnlockLog
import kotlinx.coroutines.flow.Flow

@Dao
interface UnlockLogDao {
    @Query("SELECT * FROM unlock_logs ORDER BY timestamp DESC LIMIT 50")
    fun getRecentUnlockLogs(): Flow<List<UnlockLog>>

    @Query("SELECT COUNT(*) FROM unlock_logs")
    fun getUnlockCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: UnlockLog): Long

    @Query("DELETE FROM unlock_logs")
    suspend fun clearLogs()
}
