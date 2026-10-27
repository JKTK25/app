package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.BlockedIncident
import com.example.data.model.CustomRule
import com.example.data.model.NoFapCheckIn
import com.example.data.model.NoFapStreak
import com.example.data.model.UnlockLog

@Database(
    entities = [
        BlockedIncident::class,
        CustomRule::class,
        UnlockLog::class,
        NoFapStreak::class,
        NoFapCheckIn::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun blockedIncidentDao(): BlockedIncidentDao
    abstract fun customRuleDao(): CustomRuleDao
    abstract fun unlockLogDao(): UnlockLogDao
    abstract fun noFapDao(): NoFapDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pureshield_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
