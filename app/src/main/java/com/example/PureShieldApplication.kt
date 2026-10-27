package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.data.db.AppDatabase
import com.example.data.repository.ShieldRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PureShieldApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy {
        ShieldRepository(
            database.blockedIncidentDao(),
            database.customRuleDao(),
            database.unlockLogDao(),
            database.noFapDao()
        )
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        ThemeManager.current(this)
        createNotificationChannels()
        seedInitialSampleDataIfEmpty()

        // Start 24/7 background guardian service
        try {
            val prefs = getSharedPreferences("pureshield_prefs", MODE_PRIVATE)
            val isShieldActive = prefs.getBoolean("is_shield_active", true)
            if (isShieldActive) {
                com.example.service.PureShieldForegroundService.startService(this)
            }
        } catch (_: Exception) {}
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_PROTECTION,
                "PureShield Protection & Threats",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time alerts when adult or explicit links and content are blocked"
                enableVibration(true)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun seedInitialSampleDataIfEmpty() {
        applicationScope.launch {
            // Record an initial unlock audit log
            repository.recordUnlockEvent("Shield initialized. Real-time pornographic content filter active.")
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_MODERATE) {
            com.example.engine.GeminiThreatAnalyzer.clearCache()
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        com.example.engine.GeminiThreatAnalyzer.clearCache()
    }

    companion object {
        const val CHANNEL_ID_PROTECTION = "pureshield_protection_channel"
        lateinit var instance: PureShieldApplication
            private set
    }
}
