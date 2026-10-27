package com.example.data.repository

import com.example.data.db.BlockedIncidentDao
import com.example.data.db.CustomRuleDao
import com.example.data.db.NoFapDao
import com.example.data.db.UnlockLogDao
import com.example.data.model.BlockedIncident
import com.example.data.model.CustomRule
import com.example.data.model.NoFapCheckIn
import com.example.data.model.NoFapStreak
import com.example.data.model.ThreatCategory
import com.example.data.model.UnlockLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

data class CategoryStat(
    val category: ThreatCategory,
    val count: Int,
    val percentage: Float
)

data class DailyBlockStat(
    val dayLabel: String,
    val count: Int
)

data class ShieldAnalytics(
    val totalBlocked: Int,
    val blockedToday: Int,
    val blockedThisWeek: Int,
    val categoryBreakdown: List<CategoryStat>,
    val weeklyTrend: List<DailyBlockStat>,
    val peakHour: String,
    val unlockScansCount: Int
)

class ShieldRepository(
    private val incidentDao: BlockedIncidentDao,
    private val customRuleDao: CustomRuleDao,
    private val unlockLogDao: UnlockLogDao,
    private val noFapDao: NoFapDao
) {
    val allIncidents: Flow<List<BlockedIncident>> = incidentDao.getAllIncidents()
    val recentIncidents: Flow<List<BlockedIncident>> = incidentDao.getRecentIncidents()
    val totalBlockedCount: Flow<Int> = incidentDao.getTotalBlockedCount()
    val allCustomRules: Flow<List<CustomRule>> = customRuleDao.getAllRules()
    val recentUnlockLogs: Flow<List<UnlockLog>> = unlockLogDao.getRecentUnlockLogs()
    val unlockScansCount: Flow<Int> = unlockLogDao.getUnlockCount()
    val activeNoFapStreak: Flow<NoFapStreak?> = noFapDao.getActiveStreak()
    val allNoFapCheckIns: Flow<List<NoFapCheckIn>> = noFapDao.getAllCheckIns()
    val allNoFapStreaksHistory: Flow<List<NoFapStreak>> = noFapDao.getAllStreaks()

    suspend fun getActiveRulesList(): List<CustomRule> = customRuleDao.getActiveRulesList()

    suspend fun recordIncident(incident: BlockedIncident): Long {
        return incidentDao.insertIncident(incident)
    }

    suspend fun deleteIncident(id: Long) {
        incidentDao.deleteIncidentById(id)
    }

    suspend fun clearAllIncidents() {
        incidentDao.clearAllIncidents()
    }

    suspend fun addCustomRule(rule: CustomRule): Long {
        return customRuleDao.insertRule(rule)
    }

    suspend fun toggleRule(id: Long, isEnabled: Boolean) {
        customRuleDao.setRuleEnabled(id, isEnabled)
    }

    suspend fun deleteCustomRule(id: Long) {
        customRuleDao.deleteRuleById(id)
    }

    suspend fun recordUnlockEvent(details: String = "Device unlocked. Aggressive porn filter active."): Long {
        val log = UnlockLog(
            timestamp = System.currentTimeMillis(),
            eventType = "PHONE_UNLOCKED",
            shieldStatus = "ACTIVE_MONITORING",
            details = details
        )
        return unlockLogDao.insertLog(log)
    }

    suspend fun clearUnlockLogs() {
        unlockLogDao.clearLogs()
    }

    // No-Fap Database Operations
    suspend fun ensureActiveStreak(defaultGoal: Int = 30): NoFapStreak {
        val newStreak = NoFapStreak(
            startTimestamp = System.currentTimeMillis(),
            goalDays = defaultGoal,
            personalMotivation = "Build self-discipline, mental clarity, and victory over urges",
            longestStreakDays = 0,
            totalResets = 0,
            lastCheckInTimestamp = System.currentTimeMillis(),
            isActive = true
        )
        val id = noFapDao.insertStreak(newStreak)
        return newStreak.copy(id = id)
    }

    suspend fun saveOrUpdateNoFapStreak(streak: NoFapStreak) {
        if (streak.id == 0L) {
            noFapDao.insertStreak(streak)
        } else {
            noFapDao.updateStreak(streak)
        }
    }

    suspend fun resetNoFapStreak(currentStreak: NoFapStreak?, newMotivation: String? = null): Long {
        val now = System.currentTimeMillis()
        val prevDays = if (currentStreak != null) {
            val diffMs = now - currentStreak.startTimestamp
            (diffMs / (1000L * 60 * 60 * 24)).toInt().coerceAtLeast(0)
        } else 0

        val longest = maxOf(currentStreak?.longestStreakDays ?: 0, prevDays)
        val resets = (currentStreak?.totalResets ?: 0) + 1
        val goal = currentStreak?.goalDays ?: 30
        val motivation = newMotivation ?: currentStreak?.personalMotivation ?: "Build self-discipline and mental clarity"

        if (currentStreak != null && currentStreak.id != 0L) {
            noFapDao.deactivateStreak(currentStreak.id)
        }

        val freshStreak = NoFapStreak(
            startTimestamp = now,
            goalDays = goal,
            personalMotivation = motivation,
            longestStreakDays = longest,
            totalResets = resets,
            lastCheckInTimestamp = now,
            isActive = true
        )
        return noFapDao.insertStreak(freshStreak)
    }

    suspend fun updateNoFapGoal(streakId: Long, goalDays: Int) {
        noFapDao.updateGoalDays(streakId, goalDays)
    }

    suspend fun updateNoFapMotivation(streakId: Long, motivation: String) {
        noFapDao.updateMotivation(streakId, motivation)
    }

    suspend fun recordDailyCheckIn(streakId: Long, dayNumber: Int, urgeLevel: Int, reflection: String, mood: String): Long {
        val checkIn = NoFapCheckIn(
            streakId = streakId,
            timestamp = System.currentTimeMillis(),
            dayNumber = dayNumber,
            urgeLevel = urgeLevel,
            reflectionNote = reflection,
            mood = mood
        )
        return noFapDao.insertCheckIn(checkIn)
    }

    val analyticsStream: Flow<ShieldAnalytics> = incidentDao.getAllIncidents().map { incidents ->
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfToday = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val startOfWeek = calendar.timeInMillis

        val total = incidents.size
        val todayCount = incidents.count { it.timestamp >= startOfToday }
        val weekCount = incidents.count { it.timestamp >= startOfWeek }

        // Category breakdown
        val categoryCounts = ThreatCategory.entries.map { cat ->
            val count = incidents.count { it.category == cat }
            val pct = if (total > 0) (count.toFloat() / total.toFloat()) else 0f
            CategoryStat(cat, count, pct)
        }.filter { it.count > 0 || total == 0 }

        // 7-day trend
        val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val cal = Calendar.getInstance()
        val currentDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon...

        val weeklyStats = (6 downTo 0).map { dayOffset ->
            val dayCal = Calendar.getInstance()
            dayCal.add(Calendar.DAY_OF_YEAR, -dayOffset)
            val dayStart = dayCal.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }.timeInMillis
            val dayEnd = dayStart + 86400000L

            val dayName = when (dayCal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> "Mon"
                Calendar.TUESDAY -> "Tue"
                Calendar.WEDNESDAY -> "Wed"
                Calendar.THURSDAY -> "Thu"
                Calendar.FRIDAY -> "Fri"
                Calendar.SATURDAY -> "Sat"
                Calendar.SUNDAY -> "Sun"
                else -> "Day"
            }
            val count = incidents.count { it.timestamp in dayStart until dayEnd }
            DailyBlockStat(dayName, count)
        }

        // Peak hour computation
        val hourBuckets = IntArray(24)
        incidents.forEach { inc ->
            val incCal = Calendar.getInstance().apply { timeInMillis = inc.timestamp }
            val hour = incCal.get(Calendar.HOUR_OF_DAY)
            if (hour in 0..23) hourBuckets[hour]++
        }
        val maxHourIdx = hourBuckets.indices.maxByOrNull { hourBuckets[it] } ?: 22
        val peakHourStr = String.format("%02d:00 - %02d:00", maxHourIdx, (maxHourIdx + 1) % 24)

        ShieldAnalytics(
            totalBlocked = total,
            blockedToday = todayCount,
            blockedThisWeek = weekCount,
            categoryBreakdown = if (categoryCounts.isEmpty()) {
                listOf(CategoryStat(ThreatCategory.ADULT_WEBSITE, 0, 0f))
            } else {
                categoryCounts
            },
            weeklyTrend = weeklyStats,
            peakHour = if (total == 0) "10:00 PM - 11:00 PM" else peakHourStr,
            unlockScansCount = 0 // Combined in ViewModel
        )
    }
}
