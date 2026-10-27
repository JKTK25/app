package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.PureShieldApplication
import com.example.data.model.BedtimeLockdownAction
import com.example.data.model.BedtimeScheduleHelper
import com.example.data.model.BlockedIncident
import com.example.data.model.InterceptSource
import com.example.data.model.ThreatCategory
import com.example.data.model.ThreatSeverity
import com.example.engine.BlockEngine
import com.example.engine.FamilyDohProvider
import com.example.engine.FamilyDohResolver
import com.example.engine.GeminiThreatAnalyzer
import com.example.ui.ParentalBlockActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PureShieldAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastInterceptedUrl: String = ""
    private var lastInterceptTime: Long = 0L
    private var lastTamperInterceptTime: Long = 0L
    private var lastBedtimeInterceptTime: Long = 0L
    private var lastAppLimitInterceptTime: Long = 0L

    // Active foreground app tracking for Digital Wellbeing
    private var currentForegroundPackage: String = ""
    private var foregroundStartTime: Long = System.currentTimeMillis()

    companion object {
        private const val TAG = "PureShieldAccessService"

        @Volatile
        var isServiceRunning: Boolean = false
            private set

        @Volatile
        var isScreenActive: Boolean = true

        @Volatile
        var isAuthorizedTamperSession: Boolean = false

        @Volatile
        private var activeServiceInstance: java.lang.ref.WeakReference<PureShieldAccessibilityService>? = null

        fun onScreenStateChanged(context: Context?, screenOn: Boolean) {
            isScreenActive = screenOn
            activeServiceInstance?.get()?.handleScreenStateChange(screenOn)
        }

        // Temporary session-overridden targets (target string -> expiration timestamp)
        val sessionOverriddenTargets = java.util.concurrent.ConcurrentHashMap<String, Long>()

        fun grantParentalOverride(target: String, durationMinutes: Int = 60) {
            // Strict Anti-Pornography policy: Adult content can NEVER be overridden with password
            if (BlockEngine.isPornographyOrAdultThreat(target)) {
                Log.w(TAG, "Override denied: Strict Zero-Tolerance Policy forbids unblocking pornography [$target]")
                return
            }

            val normalized = target.lowercase(Locale.ROOT).trim()
            val expiry = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
            sessionOverriddenTargets[normalized] = expiry
            if (normalized.startsWith("http://") || normalized.startsWith("https://") || normalized.contains(".")) {
                val cleanDomain = try {
                    val uri = android.net.Uri.parse(if (!normalized.startsWith("http")) "https://$normalized" else normalized)
                    uri.host ?: normalized
                } catch (_: Exception) { normalized }
                sessionOverriddenTargets[cleanDomain.lowercase(Locale.ROOT)] = expiry
            }
        }

        fun isTargetOverridden(target: String): Boolean {
            if (target.isBlank()) return false
            val lower = target.lowercase(Locale.ROOT).trim()
            val now = System.currentTimeMillis()

            val directExp = sessionOverriddenTargets[lower]
            if (directExp != null) {
                if (now < directExp) return true
                else sessionOverriddenTargets.remove(lower)
            }

            for ((key, exp) in sessionOverriddenTargets) {
                if (now < exp) {
                    if (lower.contains(key) || key.contains(lower)) {
                        return true
                    }
                } else {
                    sessionOverriddenTargets.remove(key)
                }
            }
            return false
        }

        val SETTINGS_AND_INSTALLER_PACKAGES = setOf(
            "com.android.settings",
            "com.google.android.packageinstaller",
            "com.android.packageinstaller",
            "com.samsung.android.packageinstaller",
            "com.google.android.permissioncontroller",
            "com.android.permissioncontroller",
            "com.miui.securitycenter",
            "com.miui.cleanmaster",
            "com.coloros.safecenter",
            "com.vivo.permissionmanager",
            "com.huawei.systemmanager",
            "com.sec.android.app.launcher",
            "com.google.android.apps.nexuslauncher",
            "com.android.launcher3",
            "com.mi.android.globallauncher",
            "com.oppo.launcher",
            "com.android.vending",
            "android"
        )

        val ESSENTIAL_COMMUNICATION_PACKAGES = setOf(
            // Phone, Calls & Dialer
            "com.google.android.dialer",
            "com.android.dialer",
            "com.android.phone",
            "com.samsung.android.dialer",
            "com.samsung.android.incallui",
            "com.sec.android.app.myphone",
            "com.coloros.phoneno",
            "com.vivo.phone",
            "com.miui.telecom",
            // SMS & Messages
            "com.google.android.apps.messaging",
            "com.android.mms",
            "com.samsung.android.messaging",
            "com.android.messaging",
            // Emergency & SOS Services
            "com.android.emergency",
            "com.google.android.apps.safetyhub",
            "com.android.server.telecom",
            // Clock & Alarms
            "com.google.android.deskclock",
            "com.sec.android.app.clockpackage",
            "com.android.deskclock",
            "com.coloros.alarmclock",
            // Contacts
            "com.google.android.contacts",
            "com.android.contacts",
            "com.samsung.android.app.contacts"
        )

        fun isEssentialCommunicationPackage(pkg: String): Boolean {
            if (ESSENTIAL_COMMUNICATION_PACKAGES.contains(pkg)) return true
            val lower = pkg.lowercase(Locale.ROOT)
            return lower.contains("dialer") ||
                    lower.contains("incall") ||
                    lower.contains("telecom") ||
                    lower.contains("emergency") ||
                    lower.contains("safetyhub") ||
                    lower.contains("deskclock") ||
                    lower.contains("alarmclock") ||
                    (lower.contains("mms") && !lower.contains("browser")) ||
                    (lower.contains("messaging") && (lower.contains("android") || lower.contains("google") || lower.contains("samsung")))
        }

        fun isSettingsOrInstallerPackage(pkg: String): Boolean {
            if (SETTINGS_AND_INSTALLER_PACKAGES.contains(pkg)) return true
            val lower = pkg.lowercase(Locale.ROOT)
            return lower.contains("packageinstaller") ||
                    lower.contains("settings") ||
                    lower.contains("securitycenter") ||
                    lower.contains("permissioncontroller") ||
                    lower.contains("safecenter") ||
                    lower.contains("launcher")
        }

        val MONITORED_BROWSER_PACKAGES = setOf(
            "com.android.chrome",
            "com.chrome.beta",
            "com.chrome.dev",
            "com.chrome.canary",
            "com.sec.android.app.sbrowser",
            "com.sec.android.app.sbrowser.beta",
            "org.mozilla.firefox",
            "org.mozilla.firefox_beta",
            "org.mozilla.fenix",
            "org.mozilla.focus",
            "org.mozilla.klar",
            "com.microsoft.emmx",
            "com.microsoft.emmx.canary",
            "com.brave.browser",
            "com.brave.browser_nightly",
            "com.brave.browser_beta",
            "com.opera.browser",
            "com.opera.mini.native",
            "com.opera.touch",
            "com.opera.gx",
            "com.opera.cryptobrowser",
            "com.duckduckgo.mobile.android",
            "com.vivaldi.browser",
            "com.vivaldi.browser.snapshot",
            "com.UCMobile.intl",
            "com.uc.browser.en",
            "com.kiwibrowser.browser",
            "com.yandex.browser",
            "com.yandex.browser.alpha",
            "com.yandex.browser.beta",
            "com.android.browser",
            "com.google.android.googlequicksearchbox",
            "com.google.android.apps.searchlite",
            "mark.via.gp",
            "com.soul.browser",
            "com.aloha.browser",
            "com.cloudmosa.puffinFree",
            "com.mi.globalbrowser",
            "com.coloros.browser",
            "com.heytap.browser",
            "com.vivo.browser",
            "com.huawei.browser",
            "com.transsion.phoenix",
            "com.mx.browser",
            // Search, Social & Media Apps (Telegram, WhatsApp, X, Reddit, TikTok, Discord, etc.)
            "com.google.android.youtube",
            "com.reddit.frontpage",
            "com.instagram.android",
            "com.instagram.barcelona",
            "com.twitter.android",
            "com.x.android",
            "org.telegram.messenger",
            "org.telegram.messenger.web",
            "org.thunderdog.challegram",
            "org.telegram.plus",
            "com.whatsapp",
            "com.whatsapp.w4b",
            "com.snapchat.android",
            "com.facebook.katana",
            "com.facebook.orca",
            "com.facebook.lite",
            "com.zhiliaoapp.musically",
            "com.pinterest",
            "com.tumblr",
            "com.discord",
            "com.android.vending"
        )

        fun isAccessibilityServiceEnabled(context: Context): Boolean {
            val expectedServiceName = "${context.packageName}/${PureShieldAccessibilityService::class.java.canonicalName}"
            val enabledServicesSetting = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServicesSetting)
            while (colonSplitter.hasNext()) {
                val componentName = colonSplitter.next()
                if (componentName.equals(expectedServiceName, ignoreCase = true)) {
                    return true
                }
            }
            return isServiceRunning
        }

        fun openAccessibilitySettings(context: Context) {
            try {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open accessibility settings", e)
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceRunning = true
        activeServiceInstance = java.lang.ref.WeakReference(this)
        startActiveUsageTicker()
        Log.i(TAG, "PureShield Cross-Browser, Anti-Tamper & DoH Service connected.")

        PureShieldForegroundService.startService(applicationContext)

        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_FOCUSED or
                    AccessibilityEvent.TYPE_VIEW_CLICKED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 20
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val packageName = event.packageName?.toString() ?: return

        if (!isScreenActive) return

        val now = System.currentTimeMillis()
        val prefs = getSharedPreferences("pureshield_prefs", Context.MODE_PRIVATE)
        val isShieldActive = prefs.getBoolean("is_shield_active", true)
        val preventUninstall = prefs.getBoolean("prevent_app_uninstall", true)
        val securityPin = prefs.getString("security_pin", "") ?: ""

        // 1. Anti-Uninstall / Anti-Tamper Protection Interception
        if (preventUninstall && !isAuthorizedTamperSession && isSettingsOrInstallerPackage(packageName)) {
            val rootNode = rootInActiveWindow
            if (rootNode != null) {
                try {
                    if (isPureShieldUninstallOrTamperScreen(rootNode, packageName)) {
                        handleUninstallTamperAttempt(packageName)
                        return
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking uninstall tamper", e)
                } finally {
                    try { rootNode.recycle() } catch (_: Exception) {}
                }
            }
        }

        if (!isShieldActive) return

        // 2. Track Digital Wellbeing screen time per app on window change
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            updateAppUsageTime(packageName, now)
        }

        // 3. Bedtime & Vulnerable Hours Lockdown Enforcement
        val isBedtimeEnabled = prefs.getBoolean("bedtime_lockdown_enabled", true)
        val startHour = prefs.getInt("bedtime_start_hour", 22)
        val startMin = prefs.getInt("bedtime_start_minute", 0)
        val endHour = prefs.getInt("bedtime_end_hour", 6)
        val endMin = prefs.getInt("bedtime_end_minute", 0)
        val bedtimeActionStr = prefs.getString("bedtime_action", BedtimeLockdownAction.FULL_BROWSER_LOCK.name) ?: BedtimeLockdownAction.FULL_BROWSER_LOCK.name

        val isBedtimeNow = isBedtimeEnabled && BedtimeScheduleHelper.isBedtimeActiveNow(startHour, startMin, endHour, endMin)
        val isBrowserOrWeb = isBrowserOrWebPackage(packageName)

        if (isBedtimeNow) {
            if (bedtimeActionStr == BedtimeLockdownAction.ENTIRE_DEVICE_LOCK.name) {
                val isMyPackage = packageName == applicationContext.packageName || packageName == "com.example"
                val isEssentialApp = isEssentialCommunicationPackage(packageName)
                val isSystemCore = packageName == "com.android.systemui" || isSettingsOrInstallerPackage(packageName)
                val isUserExempt = prefs.getBoolean("curfew_exempt_$packageName", false)

                if (!isMyPackage && !isEssentialApp && !isSystemCore && !isUserExempt) {
                    handleBedtimeCurfewBlock(packageName, startHour, startMin, endHour, endMin)
                    return
                }
            } else if (bedtimeActionStr == BedtimeLockdownAction.FULL_BROWSER_LOCK.name && isBrowserOrWeb) {
                handleBedtimeCurfewBlock(packageName, startHour, startMin, endHour, endMin)
                return
            }
        }

        // 4. Digital Wellbeing App Daily Limit & Global Limit Check
        val todayDateKey = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date(now))
        val limitMinutes = prefs.getInt("limit_minutes_$packageName", 0)
        val isLimitEnabled = prefs.getBoolean("limit_enabled_$packageName", false)
        if (isLimitEnabled && limitMinutes > 0) {
            val currentUsage = prefs.getInt("usage_${todayDateKey}_$packageName", 0)
            if (currentUsage >= limitMinutes) {
                handleAppLimitExceededBlock(packageName, currentUsage, limitMinutes)
                return
            }
        }

        val globalLimitEnabled = prefs.getBoolean("global_screen_time_limit_enabled", false)
        if (globalLimitEnabled) {
            val globalLimitMinutes = prefs.getInt("global_screen_time_limit_minutes", 120)
            val globalUsage = prefs.getInt("global_usage_${todayDateKey}", 0)
            if (globalUsage >= globalLimitMinutes) {
                if (packageName != "com.example" && packageName != "com.android.systemui" && !isSettingsOrInstallerPackage(packageName)) {
                    handleGlobalScreenTimeExceededBlock(globalUsage, globalLimitMinutes)
                    return
                }
            }
        }

        // 5. Cross-Browser & In-App Web Adult Content Interception
        if (!isBrowserOrWeb) return

        val blockAdult = prefs.getBoolean("block_adult_websites", true)
        if (!blockAdult) return

        val rootNode = rootInActiveWindow ?: return
        try {
            val detectedUrlOrQuery = extractUrlOrKeywordsFromNode(rootNode, packageName)
            if (!detectedUrlOrQuery.isNullOrBlank()) {
                inspectAndEnforce(detectedUrlOrQuery, packageName, isBedtimeStrict = isBedtimeNow)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in accessibility inspection", e)
        } finally {
            try {
                rootNode.recycle()
            } catch (_: Exception) {}
        }
    }

    fun handleScreenStateChange(screenOn: Boolean) {
        val now = System.currentTimeMillis()
        if (!screenOn) {
            // Screen OFF: User is no longer actively using the phone!
            // Flush only recent active usage (within 2 minutes) and do not count idle time.
            if (currentForegroundPackage.isNotBlank() && foregroundStartTime > 0L) {
                val elapsedMs = now - foregroundStartTime
                val elapsedMinutes = (elapsedMs / 60000).toInt()
                if (elapsedMinutes in 1..2) {
                    recordUsageMinutes(currentForegroundPackage, elapsedMinutes, now)
                }
            }
            foregroundStartTime = 0L
            lastActiveMinuteTickTime = now
            Log.d(TAG, "Screen turned OFF. Active screen usage tracking paused (idle mode).")
        } else {
            // Screen ON: User picked up phone and unlocked
            foregroundStartTime = now
            lastActiveMinuteTickTime = now
            Log.d(TAG, "Screen turned ON. Active screen usage tracking resumed.")
        }
    }

    private var activeUsageTickerJob: kotlinx.coroutines.Job? = null
    private var lastActiveMinuteTickTime: Long = System.currentTimeMillis()

    private fun startActiveUsageTicker() {
        activeUsageTickerJob?.cancel()
        activeUsageTickerJob = serviceScope.launch {
            while (isActive) {
                kotlinx.coroutines.delay(30_000L) // check every 30 seconds
                checkAndIncrementActiveScreenUsage()
            }
        }
    }

    private fun checkAndIncrementActiveScreenUsage() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isInteractive = powerManager?.isInteractive ?: true
        if (!isScreenActive || !isInteractive) {
            lastActiveMinuteTickTime = System.currentTimeMillis()
            foregroundStartTime = 0L
            return
        }

        val now = System.currentTimeMillis()
        val elapsedSinceLastTick = now - lastActiveMinuteTickTime
        if (elapsedSinceLastTick >= 60_000L && currentForegroundPackage.isNotBlank()) {
            val minutesToAdd = (elapsedSinceLastTick / 60_000L).toInt()
            lastActiveMinuteTickTime = now
            if (minutesToAdd in 1..2) {
                recordUsageMinutes(currentForegroundPackage, minutesToAdd, now)
                checkAppAndGlobalLimits(currentForegroundPackage, now)
            }
        }
    }

    private fun updateAppUsageTime(newPackage: String, now: Long) {
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isInteractive = powerManager?.isInteractive ?: true
        if (!isInteractive || !isScreenActive) {
            foregroundStartTime = 0L
            lastActiveMinuteTickTime = now
            return
        }

        if (foregroundStartTime == 0L) {
            foregroundStartTime = now
            lastActiveMinuteTickTime = now
            currentForegroundPackage = newPackage
            return
        }

        if (currentForegroundPackage.isNotBlank() && currentForegroundPackage != newPackage) {
            val elapsedMs = now - foregroundStartTime
            val elapsedMinutes = (elapsedMs / 60000).toInt()
            // Only count active minutes if realistic (at most 15 minutes, never accumulate idle phone gaps)
            if (elapsedMinutes in 1..15) {
                recordUsageMinutes(currentForegroundPackage, elapsedMinutes, now)
            }
            currentForegroundPackage = newPackage
            foregroundStartTime = now
            lastActiveMinuteTickTime = now
        }
    }

    private fun recordUsageMinutes(packageName: String, minutes: Int, now: Long) {
        if (minutes <= 0) return
        val prefs = getSharedPreferences("pureshield_prefs", Context.MODE_PRIVATE)
        val todayDateKey = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date(now))

        val key = "usage_${todayDateKey}_$packageName"
        val existing = prefs.getInt(key, 0)
        prefs.edit().putInt(key, existing + minutes).apply()

        // PureShield itself and core system UI should not consume user's screen time limit
        if (packageName != applicationContext.packageName &&
            packageName != "com.example" &&
            packageName != "com.android.systemui" &&
            !isSettingsOrInstallerPackage(packageName)
        ) {
            val globalKey = "global_usage_${todayDateKey}"
            val existingGlobal = prefs.getInt(globalKey, 0)
            prefs.edit().putInt(globalKey, existingGlobal + minutes).apply()
        }
    }

    private fun checkAppAndGlobalLimits(packageName: String, now: Long) {
        val prefs = getSharedPreferences("pureshield_prefs", Context.MODE_PRIVATE)
        val todayDateKey = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date(now))

        val limitMinutes = prefs.getInt("limit_minutes_$packageName", 0)
        val isLimitEnabled = prefs.getBoolean("limit_enabled_$packageName", false)
        if (isLimitEnabled && limitMinutes > 0) {
            val currentUsage = prefs.getInt("usage_${todayDateKey}_$packageName", 0)
            if (currentUsage >= limitMinutes) {
                handleAppLimitExceededBlock(packageName, currentUsage, limitMinutes)
                return
            }
        }

        val globalLimitEnabled = prefs.getBoolean("global_screen_time_limit_enabled", false)
        if (globalLimitEnabled) {
            val globalLimitMinutes = prefs.getInt("global_screen_time_limit_minutes", 120)
            val globalUsage = prefs.getInt("global_usage_${todayDateKey}", 0)
            if (globalUsage >= globalLimitMinutes) {
                if (packageName != "com.example" &&
                    packageName != applicationContext.packageName &&
                    packageName != "com.android.systemui" &&
                    !isSettingsOrInstallerPackage(packageName)
                ) {
                    handleGlobalScreenTimeExceededBlock(globalUsage, globalLimitMinutes)
                }
            }
        }
    }

    private var lastGlobalLimitInterceptTime: Long = 0

    private fun handleGlobalScreenTimeExceededBlock(usedMinutes: Int, limitMinutes: Int) {
        val now = System.currentTimeMillis()
        if (now - lastGlobalLimitInterceptTime < 2500) return
        lastGlobalLimitInterceptTime = now

        performGlobalAction(GLOBAL_ACTION_HOME)
        triggerThreatVibration()

        val app = application as? PureShieldApplication
        app?.let {
            serviceScope.launch {
                it.repository.recordIncident(
                    BlockedIncident(
                        target = "Global Device Screen Time ($limitMinutes min)",
                        category = ThreatCategory.APP_USAGE_LIMIT,
                        severity = ThreatSeverity.MEDIUM,
                        source = InterceptSource.APP_USAGE_LIMIT_EXCEEDED,
                        triggerReason = "Daily global device screen time reached: ${usedMinutes}m / ${limitMinutes}m.",
                        timestamp = now,
                        appSourcePackage = "system"
                    )
                )
            }
        }

        val lockIntent = Intent(applicationContext, ParentalBlockActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            putExtra("BLOCKED_TARGET", "Global Device Limit Exceeded")
            putExtra("BLOCKED_CATEGORY", "Daily Screen Time Exceeded")
            putExtra("BLOCKED_REASON", "You have reached the maximum allowed screen time for today (${usedMinutes}m / ${limitMinutes}m). The device is now locked for non-essential apps.")
            putExtra("BROWSER_NAME", "System UI")
        }
        startActivity(lockIntent)
    }

    private fun handleBedtimeCurfewBlock(packageName: String, startHour: Int, startMin: Int, endHour: Int, endMin: Int) {
        val now = System.currentTimeMillis()
        if (now - lastBedtimeInterceptTime < 2500) return
        lastBedtimeInterceptTime = now

        performGlobalAction(GLOBAL_ACTION_HOME)
        triggerThreatVibration()

        val timeWindow = "${BedtimeScheduleHelper.formatTime(startHour, startMin)} – ${BedtimeScheduleHelper.formatTime(endHour, endMin)}"
        val app = application as? PureShieldApplication
        app?.let {
            serviceScope.launch {
                it.repository.recordIncident(
                    BlockedIncident(
                        target = "Bedtime Lockdown: ${getReadableBrowserName(packageName)}",
                        category = ThreatCategory.BEDTIME_CURFEW,
                        severity = ThreatSeverity.HIGH,
                        source = InterceptSource.BEDTIME_LOCKDOWN,
                        triggerReason = "Bedtime Curfew Active ($timeWindow). Web browsers locked during vulnerable nighttime hours.",
                        timestamp = now,
                        appSourcePackage = packageName
                    )
                )
            }
        }

        val lockIntent = Intent(applicationContext, ParentalBlockActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            putExtra("BLOCKED_TARGET", "Bedtime Lockdown Active ($timeWindow)")
            putExtra("BLOCKED_CATEGORY", "Bedtime Curfew")
            putExtra("BLOCKED_REASON", "Web access and media platforms are locked during bedtime ($timeWindow) to prevent late-night relapse.")
            putExtra("BROWSER_NAME", getReadableBrowserName(packageName))
        }
        startActivity(lockIntent)
    }

    private fun handleAppLimitExceededBlock(packageName: String, usedMinutes: Int, limitMinutes: Int) {
        val now = System.currentTimeMillis()
        if (now - lastAppLimitInterceptTime < 2500) return
        lastAppLimitInterceptTime = now

        performGlobalAction(GLOBAL_ACTION_HOME)
        triggerThreatVibration()

        val readableName = getReadableBrowserName(packageName)
        val app = application as? PureShieldApplication
        app?.let {
            serviceScope.launch {
                it.repository.recordIncident(
                    BlockedIncident(
                        target = "$readableName (Time Limit Exceeded)",
                        category = ThreatCategory.APP_USAGE_LIMIT,
                        severity = ThreatSeverity.MEDIUM,
                        source = InterceptSource.APP_USAGE_LIMIT_EXCEEDED,
                        triggerReason = "Daily limit reached: $usedMinutes min of $limitMinutes min allocated used today.",
                        timestamp = now,
                        appSourcePackage = packageName
                    )
                )
            }
        }

        val lockIntent = Intent(applicationContext, ParentalBlockActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            putExtra("BLOCKED_TARGET", "$readableName (Daily Limit: $limitMinutes min)")
            putExtra("BLOCKED_CATEGORY", "Daily App Limit Exceeded")
            putExtra("BLOCKED_REASON", "You have reached your daily screen time allocation ($limitMinutes min) for $readableName.")
            putExtra("BROWSER_NAME", readableName)
        }
        startActivity(lockIntent)
    }

    private fun isBrowserOrWebPackage(packageName: String): Boolean {
        if (MONITORED_BROWSER_PACKAGES.contains(packageName)) return true
        val lower = packageName.lowercase(Locale.ROOT)
        return lower.contains("browser") ||
                lower.contains("chrome") ||
                lower.contains("firefox") ||
                lower.contains("opera") ||
                lower.contains("searchbox") ||
                lower.contains("telegram") ||
                lower.contains("whatsapp") ||
                lower.contains("twitter") ||
                lower.contains("instagram") ||
                lower.contains("reddit") ||
                lower.contains("tiktok") ||
                lower.contains("snapchat") ||
                lower.contains("discord") ||
                lower.contains("facebook") ||
                lower.contains("threads") ||
                lower.contains("tumblr") ||
                lower.contains("web")
    }

    private fun isPureShieldUninstallOrTamperScreen(root: AccessibilityNodeInfo, sourcePackage: String): Boolean {
        val collectedTexts = mutableListOf<String>()
        collectNodeTexts(root, collectedTexts, maxDepth = 12, maxCount = 40)
        val combined = collectedTexts.joinToString(" ").lowercase(Locale.ROOT)

        val myPkg = applicationContext.packageName.lowercase(Locale.ROOT)
        val targetTokens = listOf("pureshield", "pure shield", myPkg, "com.example", "com.aistudio")
        val hasTargetReference = targetTokens.any { combined.contains(it) }

        val isInstaller = sourcePackage.contains("packageinstaller") ||
                sourcePackage.contains("permissioncontroller") ||
                sourcePackage == "android"

        // 1. Package Installer Uninstall Dialog (triggered via long press -> Uninstall or drag-and-drop)
        if (isInstaller) {
            val isUninstallPrompt = combined.contains("uninstall") ||
                    combined.contains("desinstalar") ||
                    combined.contains("désinstaller") ||
                    combined.contains("deinstallieren") ||
                    combined.contains("do you want to uninstall") ||
                    combined.contains("remove this app") ||
                    combined.contains("delete this app")

            if (isUninstallPrompt && (hasTargetReference || combined.contains("shield") || combined.contains("pure"))) {
                return true
            }
        }

        // 2. Settings App Info Page (Uninstall / Force Stop / Clear Storage / Disable / Accessibility Tamper)
        if (sourcePackage.contains("settings") || sourcePackage.contains("securitycenter") || sourcePackage.contains("safecenter") || sourcePackage.contains("systemmanager")) {
            val isTamperAction = combined.contains("uninstall") ||
                    combined.contains("force stop") ||
                    combined.contains("forzar detención") ||
                    combined.contains("forçar parada") ||
                    combined.contains("forcer l'arrêt") ||
                    combined.contains("beenden erzwingen") ||
                    combined.contains("stoppen erzwingen") ||
                    combined.contains("arresto forzato") ||
                    combined.contains("强制停止") ||
                    combined.contains("强行停止") ||
                    combined.contains("clear data") ||
                    combined.contains("clear storage") ||
                    combined.contains("borrar datos") ||
                    combined.contains("effacer les données") ||
                    combined.contains("speicherinhalt löschen") ||
                    combined.contains("manage space") ||
                    combined.contains("modify system settings") ||
                    combined.contains("device admin") ||
                    combined.contains("deactivate") ||
                    combined.contains("desactivar") ||
                    combined.contains("désactiver") ||
                    combined.contains("turn off") ||
                    combined.contains("accessibility") ||
                    combined.contains("accesibilidad") ||
                    combined.contains("accessibilité") ||
                    combined.contains("battery optimization") ||
                    combined.contains("optimización de batería") ||
                    combined.contains("disable")

            if (hasTargetReference && isTamperAction) {
                return true
            }
        }

        // 3. Launcher Drop Target / Context Menu Uninstall Action
        if (sourcePackage.contains("launcher")) {
            val isLauncherUninstall = (combined.contains("uninstall") || combined.contains("remove")) && hasTargetReference
            if (isLauncherUninstall) {
                return true
            }
        }

        return false
    }

    private fun collectNodeTexts(node: AccessibilityNodeInfo, list: MutableList<String>, maxDepth: Int, maxCount: Int, currentDepth: Int = 0) {
        if (currentDepth > maxDepth || list.size >= maxCount) return

        val text = node.text?.toString()
        val desc = node.contentDescription?.toString()
        if (!text.isNullOrBlank()) list.add(text)
        if (!desc.isNullOrBlank()) list.add(desc)

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectNodeTexts(child, list, maxDepth, maxCount, currentDepth + 1)
        }
    }

    private fun handleUninstallTamperAttempt(sourcePackage: String) {
        val now = System.currentTimeMillis()
        if (now - lastTamperInterceptTime < 2500) return
        lastTamperInterceptTime = now

        // Immediately kill the installer / settings window
        performGlobalAction(GLOBAL_ACTION_HOME)
        performGlobalAction(GLOBAL_ACTION_BACK)
        triggerThreatVibration()

        val app = application as? PureShieldApplication
        app?.let {
            serviceScope.launch {
                it.repository.recordIncident(
                    BlockedIncident(
                        target = "PureShield Anti-Tamper & Force-Stop Protection",
                        category = ThreatCategory.BYPASS_ATTEMPT,
                        severity = ThreatSeverity.CRITICAL,
                        source = InterceptSource.ANTI_TAMPER_DAEMON,
                        triggerReason = "Anti-Tamper Lock: Prevented attempt to force stop, kill process, clear data, or uninstall PureShield without Master PIN. [Package: $sourcePackage]",
                        timestamp = now,
                        appSourcePackage = sourcePackage
                    )
                )
            }
        }

        val lockIntent = Intent(applicationContext, ParentalBlockActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            putExtra("BLOCKED_TARGET", "Anti-Force Stop & Tamper Lock")
            putExtra("BLOCKED_CATEGORY", "Tamper & Force Stop Blocked")
            putExtra("BLOCKED_REASON", "Master Password required to force stop, kill background service, clear data, or deactivate PureShield.")
            putExtra("BROWSER_NAME", "Android System / App Settings")
        }
        startActivity(lockIntent)
    }

    private fun extractUrlOrKeywordsFromNode(root: AccessibilityNodeInfo, packageName: String): String? {
        val possibleBarIds = listOf(
            "$packageName:id/url_bar",
            "$packageName:id/search_box_text",
            "$packageName:id/url",
            "$packageName:id/location_bar_edit_text",
            "$packageName:id/mozac_browser_toolbar_url_view",
            "$packageName:id/toolbar_edit",
            "$packageName:id/address_bar",
            "$packageName:id/search_edit_frame",
            "$packageName:id/search_box",
            "$packageName:id/query",
            "$packageName:id/search_src_text",
            "$packageName:id/omnibox",
            "$packageName:id/url_edit",
            "$packageName:id/search_view",
            "$packageName:id/input_box",
            "$packageName:id/search_input",
            // Social Media & Messaging IDs (Telegram, WhatsApp, X/Twitter, Reddit, Discord)
            "$packageName:id/search_query_text",
            "$packageName:id/title_text",
            "$packageName:id/chat_title",
            "$packageName:id/message_text",
            "$packageName:id/caption",
            "$packageName:id/conversation_contact_name",
            "$packageName:id/entry",
            "$packageName:id/tweet_text",
            "$packageName:id/subreddit_name",
            "$packageName:id/post_title",
            "$packageName:id/text_content"
        )

        for (id in possibleBarIds) {
            val nodes = root.findAccessibilityNodeInfosByViewId(id)
            if (nodes.isNotEmpty()) {
                val node = nodes[0]
                val text = node.text?.toString()
                if (!text.isNullOrBlank()) {
                    return text
                }
            }
        }

        return searchNodeRecursive(root)
    }

    private fun searchNodeRecursive(node: AccessibilityNodeInfo): String? {
        val text = node.text?.toString()
        val desc = node.contentDescription?.toString()
        val combined = when {
            !text.isNullOrBlank() -> text
            !desc.isNullOrBlank() -> desc
            else -> ""
        }

        if (combined.isNotBlank() && combined.length >= 2) {
            if (isTargetOverridden(combined)) {
                return null
            }
            val eval = BlockEngine.evaluate(combined, aggressiveMode = true)
            if (eval.isBlocked) {
                return combined
            }
            val lower = combined.lowercase(Locale.ROOT).trim()
            if (lower.startsWith("http://") || lower.startsWith("https://") ||
                lower.contains(".com") || lower.contains(".net") || lower.contains(".org") ||
                lower.contains(".xxx") || lower.contains(".adult") || lower.contains(".tv") ||
                lower.contains("t.me/") || lower.contains("telegram.me/") || lower.contains("chat.whatsapp.com/") ||
                lower.contains("pornhub") || lower.contains("xvideos") || lower.contains("xnxx") ||
                lower.contains("chaturbate") || lower.contains("stripchat") || lower.contains("hentai") ||
                lower.contains("sex") || lower.contains("porn") || lower.contains("nude") || lower.contains("nsfw") ||
                lower.contains("erotic") || lower.contains("cam4") || lower.contains("brazzers") ||
                lower.contains("kuma") || lower.contains("matako") || lower.contains("kukuana") || lower.contains("mboro") ||
                lower.contains("mujulus") || lower.contains("kutomba") || lower.contains("kudinya")
            ) {
                return combined
            }
        }

        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = node.getChild(i) ?: continue
            val found = searchNodeRecursive(child)
            if (found != null) return found
        }
        return null
    }

    private fun inspectAndEnforce(urlOrQuery: String, browserPackage: String, isBedtimeStrict: Boolean = false) {
        val normalized = urlOrQuery.trim()
        val now = System.currentTimeMillis()

        if (isTargetOverridden(normalized)) {
            return
        }

        if (normalized.equals(lastInterceptedUrl, ignoreCase = true) && (now - lastInterceptTime < 400)) {
            return
        }

        val app = application as? PureShieldApplication ?: return
        val prefs = getSharedPreferences("pureshield_prefs", Context.MODE_PRIVATE)
        serviceScope.launch {
            val activeRules = app.repository.getActiveRulesList()
            val evaluation = BlockEngine.evaluate(normalized, aggressiveMode = true, customRules = activeRules)

            if (evaluation.isBlocked) {
                lastInterceptedUrl = normalized
                lastInterceptTime = now

                performGlobalAction(GLOBAL_ACTION_BACK)
                triggerThreatVibration()

                val incident = BlockedIncident(
                    target = normalized,
                    category = evaluation.category,
                    severity = evaluation.severity,
                    source = InterceptSource.CROSS_BROWSER_GUARDIAN,
                    triggerReason = "${evaluation.triggerReason} [Browser/App: ${getReadableBrowserName(browserPackage)}]",
                    timestamp = now,
                    appSourcePackage = browserPackage
                )
                app.repository.recordIncident(incident)

                val isPornography = BlockEngine.isPornographyOrAdultThreat(
                    target = normalized,
                    category = evaluation.category,
                    reason = evaluation.triggerReason
                )
                val strictPornPolicy = prefs.getBoolean("strict_pornography_password_policy", true)
                val allowOverride = prefs.getBoolean("allow_parental_override", true) && (!isPornography || !strictPornPolicy)

                val lockIntent = Intent(applicationContext, ParentalBlockActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    putExtra("BLOCKED_TARGET", normalized)
                    putExtra("BLOCKED_CATEGORY", evaluation.category.displayName)
                    putExtra("BLOCKED_REASON", evaluation.triggerReason)
                    putExtra("BROWSER_NAME", getReadableBrowserName(browserPackage))
                    putExtra("IS_PORNOGRAPHY_THREAT", isPornography)
                    putExtra("ALLOW_PARENTAL_OVERRIDE", allowOverride)
                }
                startActivity(lockIntent)
                return@launch
            }

            // 6. Dual-Layer DNS-over-HTTPS (DoH) Family Resolver Interception
            val isDohEnabled = prefs.getBoolean("doh_family_shield_enabled", true)
            val dohProviderName = prefs.getString("doh_provider", FamilyDohProvider.CLOUDFLARE_FAMILY.name) ?: FamilyDohProvider.CLOUDFLARE_FAMILY.name
            val dohProvider = try { FamilyDohProvider.valueOf(dohProviderName) } catch (_: Exception) { FamilyDohProvider.CLOUDFLARE_FAMILY }

            if (isDohEnabled && (normalized.contains(".") || normalized.contains("://"))) {
                val cleanDomain = FamilyDohResolver.sanitizeDomain(normalized)
                if (cleanDomain.isNotBlank()) {
                    val dohResult = FamilyDohResolver.checkDomainWithDoh(cleanDomain, dohProvider)
                    if (dohResult.isBlocked) {
                        lastInterceptedUrl = normalized
                        lastInterceptTime = now

                        performGlobalAction(GLOBAL_ACTION_BACK)
                        triggerThreatVibration()

                        val incident = BlockedIncident(
                            target = cleanDomain,
                            category = ThreatCategory.ADULT_WEBSITE,
                            severity = ThreatSeverity.HIGH,
                            source = InterceptSource.FAMILY_DOH_SHIELD,
                            triggerReason = "Dual-Layer DoH Block: Domain blocked by ${dohProvider.displayName}",
                            timestamp = now,
                            appSourcePackage = browserPackage
                        )
                        app.repository.recordIncident(incident)

                        val strictPornPolicy = prefs.getBoolean("strict_pornography_password_policy", true)
                        val allowOverride = prefs.getBoolean("allow_parental_override", true) && !strictPornPolicy

                        val lockIntent = Intent(applicationContext, ParentalBlockActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                            putExtra("BLOCKED_TARGET", cleanDomain)
                            putExtra("BLOCKED_CATEGORY", "DoH Family Shield Block")
                            putExtra("BLOCKED_REASON", "Domain identified as adult content by ${dohProvider.displayName}")
                            putExtra("BROWSER_NAME", getReadableBrowserName(browserPackage))
                            putExtra("IS_PORNOGRAPHY_THREAT", true)
                            putExtra("ALLOW_PARENTAL_OVERRIDE", allowOverride)
                        }
                        startActivity(lockIntent)
                        return@launch
                    }
                }
            }

            // 7. Tough AI Semantic Threat & User Query Search Analysis
            val isAiEnabled = prefs.getBoolean("ai_semantic_enforcement_enabled", true)
            val searchExtraction = GeminiThreatAnalyzer.extractSearchQuery(normalized)

            val shouldAnalyzeWithAi = isAiEnabled && normalized.length >= 3
            if (shouldAnalyzeWithAi) {
                val aiResult = GeminiThreatAnalyzer.analyzeTextOrUrl(
                    input = normalized,
                    contextInfo = "Source App: $browserPackage${if (isBedtimeStrict) " [VULNERABLE BEDTIME HOURS]" else ""}"
                )

                val confidenceThreshold = if (isBedtimeStrict) 0.45f else 0.60f
                if (aiResult.isThreat && aiResult.confidence >= confidenceThreshold) {
                    lastInterceptedUrl = normalized
                    lastInterceptTime = now

                    performGlobalAction(GLOBAL_ACTION_BACK)
                    triggerThreatVibration()

                    val incident = BlockedIncident(
                        target = if (searchExtraction.isSearchQuery) "[Search Query] ${searchExtraction.extractedKeywords}" else normalized,
                        category = ThreatCategory.AI_SEMANTIC_THREAT,
                        severity = aiResult.severity,
                        source = InterceptSource.AI_SEMANTIC_ANALYZER,
                        triggerReason = "Tough AI Semantic Detection: ${aiResult.semanticBreakdown} [${searchExtraction.searchEngineOrSite}]",
                        timestamp = now,
                        appSourcePackage = browserPackage
                    )
                    app.repository.recordIncident(incident)

                    val isPornography = BlockEngine.isPornographyOrAdultThreat(
                        target = normalized,
                        category = ThreatCategory.AI_SEMANTIC_THREAT,
                        reason = aiResult.semanticBreakdown
                    )
                    val strictPornPolicy = prefs.getBoolean("strict_pornography_password_policy", true)
                    val allowOverride = prefs.getBoolean("allow_parental_override", true) && (!isPornography || !strictPornPolicy)

                    val lockIntent = Intent(applicationContext, ParentalBlockActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                        putExtra("BLOCKED_TARGET", if (searchExtraction.isSearchQuery) "${searchExtraction.searchEngineOrSite}: ${searchExtraction.extractedKeywords}" else normalized)
                        putExtra("BLOCKED_CATEGORY", "AI Semantic Threat")
                        putExtra("BLOCKED_REASON", aiResult.semanticBreakdown)
                        putExtra("BROWSER_NAME", getReadableBrowserName(browserPackage))
                        putExtra("IS_PORNOGRAPHY_THREAT", isPornography)
                        putExtra("ALLOW_PARENTAL_OVERRIDE", allowOverride)
                    }
                    startActivity(lockIntent)
                }
            }
        }
    }

    private fun triggerThreatVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 150, 100, 250), -1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 150, 100, 250), -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(250)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to vibrate", e)
        }
    }

    private fun getReadableBrowserName(packageName: String): String {
        return when (packageName) {
            "com.android.chrome" -> "Google Chrome"
            "com.sec.android.app.sbrowser" -> "Samsung Internet"
            "org.mozilla.firefox" -> "Firefox"
            "com.brave.browser" -> "Brave Browser"
            "com.microsoft.emmx" -> "Microsoft Edge"
            "com.opera.browser" -> "Opera Browser"
            "com.opera.mini.native" -> "Opera Mini"
            "com.duckduckgo.mobile.android" -> "DuckDuckGo"
            "com.google.android.googlequicksearchbox" -> "Google App / Search"
            "com.google.android.youtube" -> "YouTube"
            "com.reddit.frontpage" -> "Reddit"
            "com.instagram.android" -> "Instagram"
            "com.instagram.barcelona" -> "Threads"
            "com.twitter.android", "com.x.android" -> "X / Twitter"
            "org.telegram.messenger", "org.telegram.messenger.web", "org.thunderdog.challegram", "org.telegram.plus" -> "Telegram"
            "com.whatsapp", "com.whatsapp.w4b" -> "WhatsApp"
            "com.snapchat.android" -> "Snapchat"
            "com.facebook.katana", "com.facebook.orca", "com.facebook.lite" -> "Facebook / Messenger"
            "com.discord" -> "Discord"
            "com.zhiliaoapp.musically" -> "TikTok"
            else -> packageName.substringAfterLast('.')
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "PureShield Accessibility Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        activeUsageTickerJob?.cancel()
        if (activeServiceInstance?.get() == this) {
            activeServiceInstance = null
        }
        Log.i(TAG, "PureShield Accessibility Service destroyed")
    }
}
