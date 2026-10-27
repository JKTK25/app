package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.PureShieldApplication
import com.example.R
import com.example.ThemeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * High-Reliability Persistent Foreground Service.
 * Keeps the PureShield background interception engine alive 24/7 so that adult
 * websites and explicit content cannot bypass the barrier when the app UI is closed.
 * 
 * Implements Smart Screen State Awareness:
 * - High Alert Active Mode: When phone is in use (screen on / unlocked)
 * - Power-Saving Dormant Mode: When phone is not in use (screen off)
 */
class PureShieldForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var screenStateReceiver: BroadcastReceiver? = null
    private var isScreenCurrentlyOn: Boolean = true

    companion object {
        private const val TAG = "PureShieldGuardianSvc"
        private const val NOTIFICATION_ID = 9001
        const val CHANNEL_ID = "pureshield_guardian_channel"

        const val ACTION_START = "com.example.service.action.START_GUARDIAN"
        const val ACTION_STOP = "com.example.service.action.STOP_GUARDIAN"
        const val ACTION_UPDATE_STATUS = "com.example.service.action.UPDATE_STATUS"

        @Volatile
        var isServiceRunning: Boolean = false
            private set

        fun startService(context: Context) {
            try {
                val intent = Intent(context, PureShieldForegroundService::class.java).apply {
                    action = ACTION_START
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(context, intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start PureShieldForegroundService", e)
            }
        }

        fun stopService(context: Context) {
            try {
                val intent = Intent(context, PureShieldForegroundService::class.java).apply {
                    action = ACTION_STOP
                }
                context.stopService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop PureShieldForegroundService", e)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        createNotificationChannel()
        registerScreenStateReceiver()
        checkInitialScreenState()
        Log.i(TAG, "PureShield Continuous Guardian Service created.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        if (action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        // Start Foreground immediately with high-alert notification
        val notification = buildGuardianNotification(isActiveInUse = isScreenCurrentlyOn)
        startForeground(NOTIFICATION_ID, notification)

        // Ensure accessibility service state is active
        PureShieldAccessibilityService.isScreenActive = isScreenCurrentlyOn

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun checkInitialScreenState() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        isScreenCurrentlyOn = powerManager?.isInteractive ?: true
        PureShieldAccessibilityService.isScreenActive = isScreenCurrentlyOn
    }

    private fun registerScreenStateReceiver() {
        if (screenStateReceiver != null) return

        screenStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_ON,
                    Intent.ACTION_USER_PRESENT -> {
                        isScreenCurrentlyOn = true
                        PureShieldAccessibilityService.onScreenStateChanged(context, true)
                        updateNotification(isActiveInUse = true)
                        Log.d(TAG, "Phone in use (Screen ON). PureShield armed in HIGH-ALERT active interception mode.")
                    }
                    Intent.ACTION_SCREEN_OFF -> {
                        isScreenCurrentlyOn = false
                        PureShieldAccessibilityService.onScreenStateChanged(context, false)
                        updateNotification(isActiveInUse = false)
                        Log.d(TAG, "Phone not in use (Screen OFF). PureShield switched to POWER-SAVING STANDBY mode.")
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(screenStateReceiver, filter)
    }

    private fun unregisterScreenStateReceiver() {
        screenStateReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (_: Exception) {}
            screenStateReceiver = null
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "PureShield Active Protection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Continuous background protection ensuring zero adult content bypass"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildGuardianNotification(isActiveInUse: Boolean): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isActiveInUse) {
            "🛡️ PureShield Active • Armed & Watching"
        } else {
            "🛡️ PureShield Standby • Conserving Power"
        }

        val body = if (isActiveInUse) {
            "Real-time guardian active. Intercepting adult content and malicious links."
        } else {
            "Screen off. Power conserved. Auto-arming upon next wake."
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_shield)
            .setColor(ThemeManager.active.composeAccent.toArgb())
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun updateNotification(isActiveInUse: Boolean) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(NOTIFICATION_ID, buildGuardianNotification(isActiveInUse))
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // If the user swipes away MainActivity from Recent Apps, restart service immediately
        val prefs = getSharedPreferences("pureshield_prefs", Context.MODE_PRIVATE)
        val isShieldActive = prefs.getBoolean("is_shield_active", true)
        if (isShieldActive) {
            val restartIntent = Intent(applicationContext, PureShieldForegroundService::class.java).apply {
                action = ACTION_START
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(applicationContext, restartIntent)
                } else {
                    startService(restartIntent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restart service on task removed", e)
            }
        }
    }

    override fun onDestroy() {
        isServiceRunning = false
        unregisterScreenStateReceiver()
        serviceScope.cancel()

        // Anti-kill fallback: restart service if shield is supposed to be active
        val prefs = getSharedPreferences("pureshield_prefs", Context.MODE_PRIVATE)
        val isShieldActive = prefs.getBoolean("is_shield_active", true)
        if (isShieldActive) {
            startService(applicationContext)
        }

        super.onDestroy()
    }
}
