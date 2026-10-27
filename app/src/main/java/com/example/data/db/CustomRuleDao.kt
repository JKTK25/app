package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.CustomRule
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomRuleDao {
    @Query("SELECT * FROM custom_rules ORDER BY createdAt DESC")
    fun getAllRules(): Flow<List<CustomRule>>

    @Query("SELECT * FROM custom_rules WHERE isEnabled = 1")
    suspend fun getActiveRulesList(): List<CustomRule>

    @Query("SELECT * FROM custom_rules WHERE isEnabled = 1")
    fun getActiveRules(): Flow<List<CustomRule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: CustomRule): Long

    @Query("UPDATE custom_rules SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun setRuleEnabled(id: Long, isEnabled: Boolean)

    @Query("DELETE FROM custom_rules WHERE id = :id")
    suspend fun deleteRuleById(id: Long)
}
