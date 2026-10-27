package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.NoFapCheckIn
import com.example.data.model.NoFapStreak
import kotlinx.coroutines.flow.Flow

@Dao
interface NoFapDao {

    @Query("SELECT * FROM nofap_streaks WHERE isActive = 1 ORDER BY id DESC LIMIT 1")
    fun getActiveStreak(): Flow<NoFapStreak?>

    @Query("SELECT * FROM nofap_streaks ORDER BY startTimestamp DESC")
    fun getAllStreaks(): Flow<List<NoFapStreak>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreak(streak: NoFapStreak): Long

    @Update
    suspend fun updateStreak(streak: NoFapStreak)

    @Query("UPDATE nofap_streaks SET isActive = 0 WHERE id = :streakId")
    suspend fun deactivateStreak(streakId: Long)

    @Query("UPDATE nofap_streaks SET goalDays = :goalDays WHERE id = :streakId")
    suspend fun updateGoalDays(streakId: Long, goalDays: Int)

    @Query("UPDATE nofap_streaks SET personalMotivation = :motivation WHERE id = :streakId")
    suspend fun updateMotivation(streakId: Long, motivation: String)

    @Query("UPDATE nofap_streaks SET longestStreakDays = :longestDays WHERE id = :streakId")
    suspend fun updateLongestStreak(streakId: Long, longestDays: Int)

    @Query("SELECT * FROM nofap_checkins ORDER BY timestamp DESC")
    fun getAllCheckIns(): Flow<List<NoFapCheckIn>>

    @Query("SELECT * FROM nofap_checkins WHERE streakId = :streakId ORDER BY timestamp DESC")
    fun getCheckInsForStreak(streakId: Long): Flow<List<NoFapCheckIn>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheckIn(checkIn: NoFapCheckIn): Long

    @Query("DELETE FROM nofap_checkins WHERE id = :id")
    suspend fun deleteCheckIn(id: Long)

    @Query("DELETE FROM nofap_streaks")
    suspend fun clearAllStreaks()

    @Query("DELETE FROM nofap_checkins")
    suspend fun clearAllCheckIns()
}
