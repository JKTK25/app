package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ThreatCategory(val displayName: String, val iconName: String) {
    ADULT_WEBSITE("Adult Website", "Language"),
    EXPLICIT_SEARCH("Explicit Search", "Search"),
    RISKY_REDIRECT("Adult Redirect", "AltRoute"),
    ADULT_AD("Adult Ad / Tracker", "Dangerous"),
    EXPLICIT_KEYWORD("Explicit Keyword", "Spellcheck"),
    P2P_NETWORK("P2P Explicit Content", "Hub"),
    AI_SEMANTIC_THREAT("AI Semantic Threat", "Psychology"),
    BYPASS_ATTEMPT("Bypass / Evasion", "ShieldAlert"),
    BEDTIME_CURFEW("Bedtime Lockdown Curfew", "Bedtime"),
    APP_USAGE_LIMIT("Daily App Limit Exceeded", "HourglassBottom")
}

enum class ThreatSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

enum class InterceptSource(val displayName: String) {
    IN_APP_BROWSER("In-App Safe Browser"),
    LINK_CHECKER("Direct URL Scanner"),
    CLIPBOARD_SCANNER("Clipboard Guardian"),
    WATCHDOG_INTERCEPT("Security Watchdog"),
    SYSTEM_MONITOR("System Monitor"),
    CROSS_BROWSER_GUARDIAN("Cross-Browser Guardian"),
    AI_SEMANTIC_ANALYZER("Gemini AI Semantic Engine"),
    PARENTAL_CONTROL("Strict Parental Control"),
    ANTI_TAMPER_DAEMON("Anti-Tamper Daemon"),
    FAMILY_DOH_SHIELD("Dual-Layer Family DoH"),
    BEDTIME_LOCKDOWN("Bedtime Vulnerable Hours Lock"),
    APP_USAGE_LIMIT_EXCEEDED("Digital Wellbeing App Limit")
}

@Entity(tableName = "blocked_incidents")
data class BlockedIncident(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val target: String,
    val category: ThreatCategory,
    val severity: ThreatSeverity,
    val source: InterceptSource,
    val triggerReason: String,
    val timestamp: Long = System.currentTimeMillis(),
    val appSourcePackage: String? = null
)
