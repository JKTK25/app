package com.example.data.model

import java.util.Calendar

enum class BedtimeLockdownAction(val title: String, val description: String) {
    ENTIRE_DEVICE_LOCK(
        "Total Device Lockdown (Shutdown Screen)",
        "Blocks all non-essential apps on the entire phone during curfew hours."
    ),
    FULL_BROWSER_LOCK(
        "Complete Web & Search Lockdown",
        "Blocks all web browsers, search engines, and media apps during curfew hours."
    ),
    ULTRA_STRICT_AI(
        "Titanium Zero-Tolerance AI Filter",
        "Tightens AI semantic analysis and blocks any suggestive or non-educational content."
    );

    val displayName: String
        get() = title
}

data class AppUsageLimitItem(
    val packageName: String,
    val appName: String,
    val dailyLimitMinutes: Int,
    val usedMinutesToday: Int = 0,
    val isEnabled: Boolean = true
) {
    val remainingMinutes: Int
        get() = (dailyLimitMinutes - usedMinutesToday).coerceAtLeast(0)

    val isLimitReached: Boolean
        get() = isEnabled && dailyLimitMinutes > 0 && usedMinutesToday >= dailyLimitMinutes

    val progressFraction: Float
        get() = if (dailyLimitMinutes > 0) {
            (usedMinutesToday.toFloat() / dailyLimitMinutes.toFloat()).coerceIn(0f, 1f)
        } else 0f
}

data class CurfewExemptApp(
    val packageName: String,
    val appName: String,
    val isSystemEssential: Boolean = false,
    val isAllowed: Boolean = true
)

data class BackgroundBlockedApp(
    val packageName: String,
    val appName: String,
    val isBlocked: Boolean = true,
    val category: String = "Social & Media",
    val blockedSocketsCount: Int = 8
)

data class BackgroundDataControlState(
    val isDataFreezeActive: Boolean = true,
    val blockWhenScreenOff: Boolean = true,
    val blockAllBackgroundData: Boolean = true,
    val backgroundSocketsBlockedCount: Int = 42,
    val megabytesSaved: Float = 148.5f,
    val blockedApps: List<BackgroundBlockedApp> = emptyList()
)

object BedtimeScheduleHelper {

    fun isBedtimeActiveNow(
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        calendar: Calendar = Calendar.getInstance()
    ): Boolean {
        val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val startTotal = startHour * 60 + startMinute
        val endTotal = endHour * 60 + endMinute

        return if (startTotal <= endTotal) {
            // Same day interval (e.g. 13:00 to 17:00)
            currentMinutes in startTotal until endTotal
        } else {
            // Overnights (e.g. 22:00 to 06:00)
            currentMinutes >= startTotal || currentMinutes < endTotal
        }
    }

    fun formatTime(hour: Int, minute: Int): String {
        val amPm = if (hour < 12) "AM" else "PM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return String.format("%d:%02d %s", displayHour, minute, amPm)
    }
}
