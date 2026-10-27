package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.ThemeManager
import kotlin.random.Random

object PureShieldNotificationManager {

    private const val CHANNEL_ID = "pureshield_encouragement_channel"
    private const val CHANNEL_NAME = "PureShield Daily Encouragement & AI Insights"
    private const val CHANNEL_DESC = "Inspiring motivation, streak affirmations, and recovery support"

    private val ENCOURAGING_QUOTES = listOf(
        "Stay strong and focused. You are in control of your digital peace of mind today!",
        "Every clean day builds resilience and mental clarity. Keep going strong!",
        "Discipline is choosing between what you want now and what you want most.",
        "Your future self will thank you for the boundaries you hold firm today.",
        "Victory is built one urge defeated at a time. PureShield stands guard with you!",
        "A clear mind leads to a powerful life. Keep your focus on your long-term goals.",
        "Small daily victories lead to monumental transformation. Stay consistent!",
        "Protect your peace, protect your mind. PureShield active defense is on watch."
    )

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESC
                setShowBadge(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun sendEncouragingNotification(context: Context, customMessage: String? = null) {
        try {
            createNotificationChannel(context)

            val message = customMessage ?: ENCOURAGING_QUOTES[Random.nextInt(ENCOURAGING_QUOTES.size)]

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_shield)
                .setColor(ThemeManager.active.composeAccent.toArgb())
                .setContentTitle("🛡️ PureShield AI Daily Motivation")
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(1001, builder.build())
        } catch (_: Exception) {}
    }
}
