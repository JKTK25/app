package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.MainActivity
import com.example.PureShieldApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UnlockBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val action = intent.action ?: return
        val app = context.applicationContext as? PureShieldApplication ?: return

        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_USER_PRESENT,
            Intent.ACTION_SCREEN_ON -> {
                // Ensure 24/7 background guardian service is active and responsive
                val prefs = context.getSharedPreferences("pureshield_prefs", Context.MODE_PRIVATE)
                val isShieldActive = prefs.getBoolean("is_shield_active", true)
                if (isShieldActive) {
                    PureShieldForegroundService.startService(context)
                }

                CoroutineScope(Dispatchers.IO).launch {
                    val description = when (action) {
                        Intent.ACTION_USER_PRESENT -> "Phone unlocked by user. PureShield aggressive scan verified."
                        Intent.ACTION_BOOT_COMPLETED -> "Device boot completed. Shield armed and real-time monitoring enabled."
                        Intent.ACTION_SCREEN_ON -> "Screen turned on. Watchdog active."
                        else -> "System event: $action"
                    }
                    app.repository.recordUnlockEvent(description)
                }

                // If user enabled auto-open on unlock, launch MainActivity
                val autoOpen = prefs.getBoolean("auto_open_on_unlock", true)
                if (autoOpen && (action == Intent.ACTION_USER_PRESENT || action == Intent.ACTION_BOOT_COMPLETED)) {
                    val launchIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP
                        putExtra("LAUNCH_TRIGGER", "UNLOCK_WATCHDOG")
                    }
                    try {
                        context.startActivity(launchIntent)
                    } catch (_: Exception) {
                        // Background activity launch restrictions handled gracefully
                    }
                }
            }
        }
    }
}
