package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.PureShieldApplication
import com.example.data.model.AppUsageLimitItem
import com.example.data.model.BackgroundBlockedApp
import com.example.data.model.BackgroundDataControlState
import com.example.data.model.BedtimeLockdownAction
import com.example.data.model.BedtimeScheduleHelper
import com.example.data.model.BlockedIncident
import com.example.data.model.CurfewExemptApp
import com.example.data.model.CustomRule
import com.example.data.model.InterceptSource
import com.example.data.model.NoFapCheckIn
import com.example.data.model.NoFapStreak
import com.example.data.model.RuleCategory
import com.example.data.model.RuleType
import com.example.data.model.ThreatCategory
import com.example.data.model.ThreatSeverity
import com.example.data.model.UnlockLog
import com.example.data.repository.ShieldAnalytics
import com.example.data.repository.ShieldRepository
import com.example.engine.AiAnalysisResult
import com.example.engine.AiParentalReport
import com.example.engine.BlockEngine
import com.example.engine.DohTestResult
import com.example.engine.EvaluationResult
import com.example.engine.FamilyDohProvider
import com.example.engine.FamilyDohResolver
import com.example.engine.GeminiThreatAnalyzer
import com.example.service.PureShieldAccessibilityService
import com.example.service.PureShieldAdminReceiver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ProtectionMode(val title: String, val description: String, val levelTag: String) {
    AGGRESSIVE(
        "Aggressive Shield",
        "Deep heuristic inspection of all domains, links, keywords, and redirect patterns. Zero tolerance.",
        "RECOMMENDED"
    ),
    TITANIUM_STRICT(
        "Titanium Lockdown",
        "Military-grade filter. Disables incognito, enforces rigid safe-search, requires password to pause or disable.",
        "MAXIMUM"
    ),
    STANDARD(
        "Standard Filter",
        "Blocks known adult top-level domains and explicit websites while minimizing false positives.",
        "BASIC"
    )
}

data class ShieldUiState(
    val isShieldActive: Boolean = true,
    val protectionMode: ProtectionMode = ProtectionMode.TITANIUM_STRICT,
    val autoOpenOnUnlock: Boolean = true,
    val blockAdultWebsites: Boolean = true,
    val forceSafeSearch: Boolean = true,
    val blockExplicitSearch: Boolean = true,
    val blockAdultPopups: Boolean = true,
    val pinProtectionEnabled: Boolean = false,
    val securityPin: String = "",
    val isFirstLaunchPinSetupRequired: Boolean = false,
    val isPinDialogOpen: Boolean = false,
    val pinDialogPurpose: String = "",
    val isPinVerifiedThisSession: Boolean = false,
    val activeThreatModal: BlockedIncident? = null,
    val testUrlInput: String = "",
    val lastEvaluationResult: EvaluationResult? = null,
    val isEvaluating: Boolean = false,
    val totalThreatsBlocked: Int = 0,
    val unlockScansCount: Int = 0,
    // Cross-Browser & Strict Parental Control State
    val isCrossBrowserProtectionEnabled: Boolean = true,
    val isAccessibilityServiceActive: Boolean = false,
    val isDeviceAdminActive: Boolean = false,
    val strictParentalControlEnabled: Boolean = true,
    val blockIncognitoBrowsers: Boolean = true,
    val preventAppUninstall: Boolean = true,
    // AI Semantic Engine & Analytics State
    val aiSemanticEnforcementEnabled: Boolean = true,
    val aiAnalysisResult: AiAnalysisResult? = null,
    val isAiAnalyzing: Boolean = false,
    val aiParentalReport: AiParentalReport? = null,
    val isAiReportGenerating: Boolean = false,
    val isTamperSessionAuthorized: Boolean = false,
    // No-Fap & Motivation Room State
    val activeNoFapStreak: NoFapStreak? = null,
    val noFapStreakDays: Int = 0,
    val noFapStreakWeeks: Int = 0,
    val noFapGoalDays: Int = 30,
    val noFapLongestStreakDays: Int = 0,
    val noFapTotalResets: Int = 0,
    val noFapPersonalMotivation: String = "Building self-discipline, mental clarity, and victory over urges",
    val noFapCheckIns: List<NoFapCheckIn> = emptyList(),
    val aiDailyEncouragementEnabled: Boolean = true,
    // Social Media & Dating Platforms Selective Access
    val allowSocialMediaAccess: Boolean = true,
    val allowDatingAppsAccess: Boolean = true,
    // Dual-Layer DNS-over-HTTPS (DoH) Family Shield State
    val dohFamilyShieldEnabled: Boolean = true,
    val dohProvider: FamilyDohProvider = FamilyDohProvider.CLOUDFLARE_FAMILY,
    val dohTestResult: DohTestResult? = null,
    val isDohTesting: Boolean = false,
    // Bedtime & Vulnerable Hours Lockdown State
    val bedtimeLockdownEnabled: Boolean = true,
    val bedtimeStartHour: Int = 22,
    val bedtimeStartMinute: Int = 0,
    val bedtimeEndHour: Int = 6,
    val bedtimeEndMinute: Int = 0,
    val bedtimeAction: BedtimeLockdownAction = BedtimeLockdownAction.FULL_BROWSER_LOCK,
    val isBedtimeActiveNow: Boolean = false,
    // Digital Wellbeing App Screen Time Limits
    val appUsageLimits: List<AppUsageLimitItem> = emptyList(),
    val globalScreenTimeLimitEnabled: Boolean = false,
    val globalScreenTimeLimitMinutes: Int = 120, // 2 hours default
    val allowParentalOverride: Boolean = true,
    // Curfew Whitelist Exemptions
    val curfewExemptApps: List<CurfewExemptApp> = emptyList(),
    // Background Data & Screen-Off Firewall Guard
    val backgroundDataControl: BackgroundDataControlState = BackgroundDataControlState(),
    val antiForceStopProtectionEnabled: Boolean = true,
    // Strict Zero-Tolerance Pornography Policy
    val strictPornographyPasswordPolicy: Boolean = true,
    val autoAssignedPinDialog: Boolean = false
)

class ShieldViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ShieldRepository = (application as PureShieldApplication).repository
    private val prefs = application.getSharedPreferences("pureshield_prefs", Context.MODE_PRIVATE)

    private var pendingPinAction: (() -> Unit)? = null

    private val initialPin = prefs.getString("security_pin", "") ?: ""

    private val initialStartHour = prefs.getInt("bedtime_start_hour", 22)
    private val initialStartMin = prefs.getInt("bedtime_start_minute", 0)
    private val initialEndHour = prefs.getInt("bedtime_end_hour", 6)
    private val initialEndMin = prefs.getInt("bedtime_end_minute", 0)
    private val initialBedtimeEnabled = prefs.getBoolean("bedtime_lockdown_enabled", true)
    private val initialBedtimeActionStr = prefs.getString("bedtime_action", BedtimeLockdownAction.FULL_BROWSER_LOCK.name) ?: BedtimeLockdownAction.FULL_BROWSER_LOCK.name
    private val initialBedtimeAction = try { BedtimeLockdownAction.valueOf(initialBedtimeActionStr) } catch (_: Exception) { BedtimeLockdownAction.FULL_BROWSER_LOCK }

    private val initialDohEnabled = prefs.getBoolean("doh_family_shield_enabled", true)
    private val initialDohProviderStr = prefs.getString("doh_provider", FamilyDohProvider.CLOUDFLARE_FAMILY.name) ?: FamilyDohProvider.CLOUDFLARE_FAMILY.name
    private val initialDohProvider = try { FamilyDohProvider.valueOf(initialDohProviderStr) } catch (_: Exception) { FamilyDohProvider.CLOUDFLARE_FAMILY }

    private val _uiState = MutableStateFlow(
        ShieldUiState(
            isShieldActive = prefs.getBoolean("is_shield_active", true),
            protectionMode = ProtectionMode.valueOf(
                prefs.getString("protection_mode", ProtectionMode.TITANIUM_STRICT.name) ?: ProtectionMode.TITANIUM_STRICT.name
            ),
            autoOpenOnUnlock = prefs.getBoolean("auto_open_on_unlock", true),
            blockAdultWebsites = prefs.getBoolean("block_adult_websites", true),
            forceSafeSearch = prefs.getBoolean("force_safe_search", true),
            blockExplicitSearch = prefs.getBoolean("block_explicit_search", true),
            blockAdultPopups = prefs.getBoolean("block_adult_popups", true),
            pinProtectionEnabled = initialPin.isNotBlank() || prefs.getBoolean("pin_protection_enabled", false),
            securityPin = initialPin,
            isFirstLaunchPinSetupRequired = initialPin.isBlank(),
            isCrossBrowserProtectionEnabled = prefs.getBoolean("cross_browser_protection_enabled", true),
            strictParentalControlEnabled = prefs.getBoolean("strict_parental_control_enabled", true),
            blockIncognitoBrowsers = prefs.getBoolean("block_incognito_browsers", true),
            preventAppUninstall = prefs.getBoolean("prevent_app_uninstall", true),
            aiSemanticEnforcementEnabled = prefs.getBoolean("ai_semantic_enforcement_enabled", true),
            isAccessibilityServiceActive = PureShieldAccessibilityService.isAccessibilityServiceEnabled(application),
            isDeviceAdminActive = PureShieldAdminReceiver.isDeviceAdminActive(application),
            noFapStreakDays = calculateNoFapStreakDays(prefs.getLong("nofap_start_timestamp", System.currentTimeMillis())),
            noFapStreakWeeks = calculateNoFapStreakDays(prefs.getLong("nofap_start_timestamp", System.currentTimeMillis())) / 7,
            noFapGoalDays = prefs.getInt("nofap_goal_days", 30),
            aiDailyEncouragementEnabled = prefs.getBoolean("ai_encouragement_enabled", true),
            allowSocialMediaAccess = prefs.getBoolean("allow_social_media_access", true),
            allowDatingAppsAccess = prefs.getBoolean("allow_dating_apps_access", true),
            allowParentalOverride = prefs.getBoolean("allow_parental_override", true),
            strictPornographyPasswordPolicy = prefs.getBoolean("strict_pornography_password_policy", true),
            globalScreenTimeLimitEnabled = prefs.getBoolean("global_screen_time_limit_enabled", false),
            globalScreenTimeLimitMinutes = prefs.getInt("global_screen_time_limit_minutes", 120),
            // DoH
            dohFamilyShieldEnabled = initialDohEnabled,
            dohProvider = initialDohProvider,
            // Bedtime
            bedtimeLockdownEnabled = initialBedtimeEnabled,
            bedtimeStartHour = initialStartHour,
            bedtimeStartMinute = initialStartMin,
            bedtimeEndHour = initialEndHour,
            bedtimeEndMinute = initialEndMin,
            bedtimeAction = initialBedtimeAction,
            isBedtimeActiveNow = initialBedtimeEnabled && BedtimeScheduleHelper.isBedtimeActiveNow(
                initialStartHour, initialStartMin, initialEndHour, initialEndMin
            )
        )
    )
    val uiState: StateFlow<ShieldUiState> = _uiState.asStateFlow()

    val allIncidents: StateFlow<List<BlockedIncident>> = repository.allIncidents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentIncidents: StateFlow<List<BlockedIncident>> = repository.recentIncidents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCustomRules: StateFlow<List<CustomRule>> = repository.allCustomRules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentUnlockLogs: StateFlow<List<UnlockLog>> = repository.recentUnlockLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeNoFapStreak: StateFlow<NoFapStreak?> = repository.activeNoFapStreak
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allNoFapCheckIns: StateFlow<List<NoFapCheckIn>> = repository.allNoFapCheckIns
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val analytics: StateFlow<ShieldAnalytics> = combine(
        repository.analyticsStream,
        repository.unlockScansCount
    ) { baseAnalytics, unlockCount ->
        baseAnalytics.copy(unlockScansCount = unlockCount)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ShieldAnalytics(0, 0, 0, emptyList(), emptyList(), "10:00 PM - 11:00 PM", 0)
    )

    init {
        // Refresh digital wellbeing app usage
        refreshAppUsageStats()
        refreshCurfewExemptions()
        refreshBackgroundDataControlState()

        // Observe and sync total count
        viewModelScope.launch {
            repository.totalBlockedCount.collect { count ->
                _uiState.value = _uiState.value.copy(totalThreatsBlocked = count)
            }
        }
        viewModelScope.launch {
            repository.unlockScansCount.collect { count ->
                _uiState.value = _uiState.value.copy(unlockScansCount = count)
            }
        }
        // Observe and sync Room NoFap Streak
        viewModelScope.launch {
            repository.activeNoFapStreak.collect { streak ->
                if (streak == null) {
                    repository.ensureActiveStreak(_uiState.value.noFapGoalDays)
                } else {
                    val days = calculateNoFapStreakDays(streak.startTimestamp)
                    val weeks = days / 7
                    _uiState.value = _uiState.value.copy(
                        activeNoFapStreak = streak,
                        noFapStreakDays = days,
                        noFapStreakWeeks = weeks,
                        noFapGoalDays = streak.goalDays,
                        noFapLongestStreakDays = maxOf(streak.longestStreakDays, days),
                        noFapTotalResets = streak.totalResets,
                        noFapPersonalMotivation = streak.personalMotivation
                    )
                }
            }
        }
        // Observe Room NoFap CheckIns
        viewModelScope.launch {
            repository.allNoFapCheckIns.collect { list ->
                _uiState.value = _uiState.value.copy(noFapCheckIns = list)
            }
        }
    }

    /**
     * Executes an action protected by the Master PIN.
     * If PIN is configured and action is sensitive (e.g. pausing shield), prompts for PIN.
     */
    fun performWithPinGuard(purposeDescription: String, action: () -> Unit) {
        val currentPin = _uiState.value.securityPin
        if (currentPin.isBlank()) {
            action()
        } else {
            pendingPinAction = action
            _uiState.value = _uiState.value.copy(
                isPinDialogOpen = true,
                pinDialogPurpose = purposeDescription
            )
        }
    }

    fun submitPinVerification(enteredPin: String): Boolean {
        val currentPin = _uiState.value.securityPin
        if (currentPin == enteredPin) {
            _uiState.value = _uiState.value.copy(
                isPinDialogOpen = false,
                isPinVerifiedThisSession = true
            )
            val actionToRun = pendingPinAction
            pendingPinAction = null
            actionToRun?.invoke()
            return true
        }
        return false
    }

    fun dismissPinDialog() {
        pendingPinAction = null
        _uiState.value = _uiState.value.copy(isPinDialogOpen = false)
    }

    fun setupMasterPassword(pin: String) {
        val enabled = pin.isNotBlank()
        prefs.edit()
            .putBoolean("pin_protection_enabled", enabled)
            .putString("security_pin", pin)
            .apply()
        _uiState.value = _uiState.value.copy(
            pinProtectionEnabled = enabled,
            securityPin = pin,
            isFirstLaunchPinSetupRequired = false,
            isPinVerifiedThisSession = true
        )
    }

    fun toggleShieldState() {
        val current = _uiState.value.isShieldActive
        if (current) {
            performWithPinGuard("Pause PureShield Active Defense") {
                setShieldActiveInternal(false)
            }
        } else {
            setShieldActiveInternal(true)
        }
    }

    private fun setShieldActiveInternal(active: Boolean) {
        prefs.edit().putBoolean("is_shield_active", active).apply()
        _uiState.value = _uiState.value.copy(isShieldActive = active)
        val app = getApplication<PureShieldApplication>()
        if (active) {
            com.example.service.PureShieldForegroundService.startService(app)
        } else {
            com.example.service.PureShieldForegroundService.stopService(app)
        }
    }

    fun setProtectionMode(mode: ProtectionMode) {
        if (mode == ProtectionMode.STANDARD && _uiState.value.securityPin.isNotBlank()) {
            performWithPinGuard("Downgrade Protection Mode") {
                prefs.edit().putString("protection_mode", mode.name).apply()
                _uiState.value = _uiState.value.copy(protectionMode = mode)
            }
        } else {
            prefs.edit().putString("protection_mode", mode.name).apply()
            _uiState.value = _uiState.value.copy(protectionMode = mode)
        }
    }

    fun setAutoOpenOnUnlock(enabled: Boolean) {
        prefs.edit().putBoolean("auto_open_on_unlock", enabled).apply()
        _uiState.value = _uiState.value.copy(autoOpenOnUnlock = enabled)
    }

    fun setBlockAdultWebsites(enabled: Boolean) {
        if (!enabled && _uiState.value.securityPin.isNotBlank()) {
            performWithPinGuard("Disable Adult Website Filter") {
                prefs.edit().putBoolean("block_adult_websites", false).apply()
                _uiState.value = _uiState.value.copy(blockAdultWebsites = false)
            }
        } else {
            prefs.edit().putBoolean("block_adult_websites", enabled).apply()
            _uiState.value = _uiState.value.copy(blockAdultWebsites = enabled)
        }
    }

    fun setForceSafeSearch(enabled: Boolean) {
        prefs.edit().putBoolean("force_safe_search", enabled).apply()
        _uiState.value = _uiState.value.copy(forceSafeSearch = enabled)
    }

    fun setBlockExplicitSearch(enabled: Boolean) {
        if (!enabled && _uiState.value.securityPin.isNotBlank()) {
            performWithPinGuard("Disable Explicit Search Filter") {
                prefs.edit().putBoolean("block_explicit_search", false).apply()
                _uiState.value = _uiState.value.copy(blockExplicitSearch = false)
            }
        } else {
            prefs.edit().putBoolean("block_explicit_search", enabled).apply()
            _uiState.value = _uiState.value.copy(blockExplicitSearch = enabled)
        }
    }

    fun setBlockAdultPopups(enabled: Boolean) {
        prefs.edit().putBoolean("block_adult_popups", enabled).apply()
        _uiState.value = _uiState.value.copy(blockAdultPopups = enabled)
    }

    fun setAllowParentalOverride(enabled: Boolean) {
        performWithPinGuard(if (enabled) "Enable Parental Override" else "Disable Parental Override") {
            prefs.edit().putBoolean("allow_parental_override", enabled).apply()
            _uiState.value = _uiState.value.copy(allowParentalOverride = enabled)
        }
    }

    fun setGlobalScreenTimeLimitEnabled(enabled: Boolean) {
        performWithPinGuard(if (enabled) "Enable Screen Time Limit" else "Disable Screen Time Limit") {
            prefs.edit().putBoolean("global_screen_time_limit_enabled", enabled).apply()
            _uiState.value = _uiState.value.copy(globalScreenTimeLimitEnabled = enabled)
        }
    }

    fun setGlobalScreenTimeLimitMinutes(minutes: Int) {
        prefs.edit().putInt("global_screen_time_limit_minutes", minutes).apply()
        _uiState.value = _uiState.value.copy(globalScreenTimeLimitMinutes = minutes)
    }

    fun setPin(pin: String) {
        setupMasterPassword(pin)
    }

    /**
     * Auto-assigns a cryptographically strong 6-digit random password/PIN.
     * Persists the PIN, enables PIN tamper protection, and notifies the user with the generated code.
     */
    fun autoAssignMasterPassword(): String {
        val random = java.security.SecureRandom()
        val num = 100000 + random.nextInt(900000) // Guarantees exactly 6 digits
        val generatedPin = num.toString()

        prefs.edit()
            .putBoolean("pin_protection_enabled", true)
            .putString("security_pin", generatedPin)
            .apply()

        _uiState.value = _uiState.value.copy(
            pinProtectionEnabled = true,
            securityPin = generatedPin,
            isFirstLaunchPinSetupRequired = false,
            isPinVerifiedThisSession = true,
            autoAssignedPinDialog = true
        )
        return generatedPin
    }

    fun dismissAutoAssignedPinDialog() {
        _uiState.value = _uiState.value.copy(autoAssignedPinDialog = false)
    }

    fun generateRandomPin(): String {
        val random = java.security.SecureRandom()
        return (100000 + random.nextInt(900000)).toString()
    }

    fun setStrictPornographyPasswordPolicy(enabled: Boolean) {
        prefs.edit().putBoolean("strict_pornography_password_policy", enabled).apply()
        _uiState.value = _uiState.value.copy(strictPornographyPasswordPolicy = enabled)
    }

    fun removePin() {
        performWithPinGuard("Remove Master Security Password") {
            prefs.edit()
                .putBoolean("pin_protection_enabled", false)
                .putString("security_pin", "")
                .apply()
            _uiState.value = _uiState.value.copy(
                pinProtectionEnabled = false,
                securityPin = "",
                isPinVerifiedThisSession = false
            )
        }
    }

    fun verifyPin(enteredPin: String): Boolean {
        val correct = _uiState.value.securityPin == enteredPin
        if (correct) {
            _uiState.value = _uiState.value.copy(isPinVerifiedThisSession = true)
        }
        return correct
    }

    fun setTestUrlInput(input: String) {
        _uiState.value = _uiState.value.copy(testUrlInput = input)
    }

    fun evaluateContent(
        input: String,
        source: InterceptSource = InterceptSource.LINK_CHECKER,
        showModalOnBlock: Boolean = true
    ): EvaluationResult {
        if (!_uiState.value.isShieldActive) {
            return EvaluationResult(isBlocked = false, triggerReason = "Shield is paused")
        }

        val rules = allCustomRules.value
        val aggressive = _uiState.value.protectionMode != ProtectionMode.STANDARD
        val result = BlockEngine.evaluate(input, aggressive, rules)

        _uiState.value = _uiState.value.copy(lastEvaluationResult = result)

        if (result.isBlocked) {
            val incident = BlockedIncident(
                target = input,
                category = result.category,
                severity = result.severity,
                source = source,
                triggerReason = result.triggerReason,
                timestamp = System.currentTimeMillis()
            )
            viewModelScope.launch {
                repository.recordIncident(incident)
            }
            if (showModalOnBlock) {
                _uiState.value = _uiState.value.copy(activeThreatModal = incident)
            }
        }
        return result
    }

    fun dismissThreatModal() {
        _uiState.value = _uiState.value.copy(activeThreatModal = null)
    }

    fun addCustomRule(pattern: String, type: RuleType, category: RuleCategory) {
        if (pattern.isBlank()) return
        if (_uiState.value.securityPin.isNotBlank()) {
            performWithPinGuard("Add New Custom Protection Rule") {
                viewModelScope.launch {
                    repository.addCustomRule(
                        CustomRule(
                            pattern = pattern.trim(),
                            ruleType = type,
                            category = category,
                            isEnabled = true
                        )
                    )
                }
            }
        } else {
            viewModelScope.launch {
                repository.addCustomRule(
                    CustomRule(
                        pattern = pattern.trim(),
                        ruleType = type,
                        category = category,
                        isEnabled = true
                    )
                )
            }
        }
    }

    fun toggleCustomRule(id: Long, isEnabled: Boolean) {
        if (!isEnabled && _uiState.value.securityPin.isNotBlank()) {
            performWithPinGuard("Disable Protection Rule") {
                viewModelScope.launch {
                    repository.toggleRule(id, false)
                }
            }
        } else {
            viewModelScope.launch {
                repository.toggleRule(id, isEnabled)
            }
        }
    }

    fun deleteCustomRule(id: Long) {
        if (_uiState.value.securityPin.isNotBlank()) {
            performWithPinGuard("Delete Custom Protection Rule") {
                viewModelScope.launch {
                    repository.deleteCustomRule(id)
                }
            }
        } else {
            viewModelScope.launch {
                repository.deleteCustomRule(id)
            }
        }
    }

    fun deleteIncident(id: Long) {
        viewModelScope.launch {
            repository.deleteIncident(id)
        }
    }

    fun clearAllLogs() {
        if (_uiState.value.securityPin.isNotBlank()) {
            performWithPinGuard("Clear All Security Incident Logs") {
                viewModelScope.launch {
                    repository.clearAllIncidents()
                    repository.clearUnlockLogs()
                }
            }
        } else {
            viewModelScope.launch {
                repository.clearAllIncidents()
                repository.clearUnlockLogs()
            }
        }
    }

    fun recordUnlockAudit(reason: String = "Watchdog verification scan upon device unlock") {
        viewModelScope.launch {
            repository.recordUnlockEvent(reason)
        }
    }

    fun refreshAccessibilityStatus() {
        val active = PureShieldAccessibilityService.isAccessibilityServiceEnabled(getApplication())
        _uiState.value = _uiState.value.copy(isAccessibilityServiceActive = active)
    }

    fun refreshDeviceAdminStatus() {
        val active = PureShieldAdminReceiver.isDeviceAdminActive(getApplication())
        _uiState.value = _uiState.value.copy(isDeviceAdminActive = active)
    }

    fun requestDeviceAdminActivation(context: Context) {
        try {
            val intent = PureShieldAdminReceiver.createDeviceAdminIntent(context)
            context.startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("ShieldViewModel", "Failed to launch device admin enrollment", e)
        }
    }

    fun setCrossBrowserProtectionEnabled(enabled: Boolean) {
        if (!enabled && _uiState.value.securityPin.isNotBlank()) {
            performWithPinGuard("Disable Cross-Browser Protection") {
                prefs.edit().putBoolean("cross_browser_protection_enabled", false).apply()
                _uiState.value = _uiState.value.copy(isCrossBrowserProtectionEnabled = false)
            }
        } else {
            prefs.edit().putBoolean("cross_browser_protection_enabled", enabled).apply()
            _uiState.value = _uiState.value.copy(isCrossBrowserProtectionEnabled = enabled)
        }
    }

    fun setStrictParentalControlEnabled(enabled: Boolean) {
        if (!enabled && _uiState.value.securityPin.isNotBlank()) {
            performWithPinGuard("Disable Strict Parental Control") {
                prefs.edit().putBoolean("strict_parental_control_enabled", false).apply()
                _uiState.value = _uiState.value.copy(strictParentalControlEnabled = false)
            }
        } else {
            prefs.edit().putBoolean("strict_parental_control_enabled", enabled).apply()
            _uiState.value = _uiState.value.copy(strictParentalControlEnabled = enabled)
        }
    }

    fun setBlockIncognitoBrowsers(enabled: Boolean) {
        prefs.edit().putBoolean("block_incognito_browsers", enabled).apply()
        _uiState.value = _uiState.value.copy(blockIncognitoBrowsers = enabled)
    }

    fun setPreventAppUninstall(enabled: Boolean) {
        if (!enabled && _uiState.value.securityPin.isNotBlank()) {
            performWithPinGuard("Disable Anti-Uninstall Protection") {
                prefs.edit().putBoolean("prevent_app_uninstall", false).apply()
                _uiState.value = _uiState.value.copy(preventAppUninstall = false)
            }
        } else {
            prefs.edit().putBoolean("prevent_app_uninstall", enabled).apply()
            _uiState.value = _uiState.value.copy(preventAppUninstall = enabled)
        }
    }

    fun setAiSemanticEnforcementEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("ai_semantic_enforcement_enabled", enabled).apply()
        _uiState.value = _uiState.value.copy(aiSemanticEnforcementEnabled = enabled)
    }

    fun authorizeTamperSession(allow: Boolean) {
        PureShieldAccessibilityService.isAuthorizedTamperSession = allow
        _uiState.value = _uiState.value.copy(isTamperSessionAuthorized = allow)
    }

    fun analyzeWithGeminiAi(input: String) {
        if (input.isBlank()) return
        _uiState.value = _uiState.value.copy(isAiAnalyzing = true, aiAnalysisResult = null)
        viewModelScope.launch {
            val result = GeminiThreatAnalyzer.analyzeTextOrUrl(input.trim())
            _uiState.value = _uiState.value.copy(
                isAiAnalyzing = false,
                aiAnalysisResult = result
            )
        }
    }

    fun clearAiAnalysis() {
        _uiState.value = _uiState.value.copy(aiAnalysisResult = null)
    }

    fun generateAiParentalReport() {
        _uiState.value = _uiState.value.copy(isAiReportGenerating = true)
        viewModelScope.launch {
            val incidents = recentIncidents.value
            val total = _uiState.value.totalThreatsBlocked
            val report = GeminiThreatAnalyzer.generateParentalReport(incidents, total)
            _uiState.value = _uiState.value.copy(
                isAiReportGenerating = false,
                aiParentalReport = report
            )
        }
    }

    fun applyAiRecommendedRules(rules: List<CustomRule>) {
        viewModelScope.launch {
            rules.forEach { rule ->
                repository.addCustomRule(rule)
            }
        }
    }

    fun applyAiRule(pattern: String, category: RuleCategory = RuleCategory.DOMAIN) {
        if (pattern.isBlank()) return
        viewModelScope.launch {
            repository.addCustomRule(
                CustomRule(
                    pattern = pattern.trim(),
                    ruleType = RuleType.BLOCK,
                    category = category,
                    isEnabled = true
                )
            )
        }
    }

    fun resetNoFapStreak(resetReason: String = "") {
        viewModelScope.launch {
            val currentStreak = _uiState.value.activeNoFapStreak
            repository.resetNoFapStreak(currentStreak, if (resetReason.isNotBlank()) resetReason else null)
        }
    }

    fun setNoFapGoalDays(goalDays: Int, motivation: String? = null) {
        viewModelScope.launch {
            val currentStreak = _uiState.value.activeNoFapStreak
            if (currentStreak != null && currentStreak.id != 0L) {
                repository.updateNoFapGoal(currentStreak.id, goalDays)
                if (motivation != null) {
                    repository.updateNoFapMotivation(currentStreak.id, motivation)
                }
            } else {
                val newStreak = NoFapStreak(
                    startTimestamp = System.currentTimeMillis(),
                    goalDays = goalDays,
                    personalMotivation = motivation ?: _uiState.value.noFapPersonalMotivation,
                    isActive = true
                )
                repository.saveOrUpdateNoFapStreak(newStreak)
            }
        }
    }

    fun updatePersonalMotivation(motivation: String) {
        if (motivation.isBlank()) return
        viewModelScope.launch {
            val currentStreak = _uiState.value.activeNoFapStreak
            if (currentStreak != null && currentStreak.id != 0L) {
                repository.updateNoFapMotivation(currentStreak.id, motivation)
            }
        }
    }

    fun recordDailyCheckIn(urgeLevel: Int, reflection: String, mood: String) {
        viewModelScope.launch {
            val currentStreak = _uiState.value.activeNoFapStreak
            val streakId = currentStreak?.id ?: 0L
            val dayNum = _uiState.value.noFapStreakDays + 1
            repository.recordDailyCheckIn(
                streakId = streakId,
                dayNumber = dayNum,
                urgeLevel = urgeLevel,
                reflection = reflection,
                mood = mood
            )
        }
    }

    fun setAiDailyEncouragementEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("ai_encouragement_enabled", enabled).apply()
        _uiState.value = _uiState.value.copy(aiDailyEncouragementEnabled = enabled)
    }

    fun sendInstantAiEncouragementNotification() {
        val app = getApplication<Application>()
        val streak = _uiState.value.noFapStreakDays
        val goal = _uiState.value.noFapGoalDays
        val customMessage = if (streak > 0) {
            "🌟 Day $streak Clean! You're on track to crush your $goal-day purity goal. Stay focused and keep guarding your peace."
        } else {
            "🛡️ Every journey starts with day one. PureShield stands guard with you today. Stay strong!"
        }
        com.example.service.PureShieldNotificationManager.sendEncouragingNotification(app, customMessage)
    }

    fun setAllowSocialMediaAccess(allow: Boolean) {
        prefs.edit().putBoolean("allow_social_media_access", allow).apply()
        _uiState.value = _uiState.value.copy(allowSocialMediaAccess = allow)
    }

    fun setAllowDatingAppsAccess(allow: Boolean) {
        prefs.edit().putBoolean("allow_dating_apps_access", allow).apply()
        _uiState.value = _uiState.value.copy(allowDatingAppsAccess = allow)
    }

    // ==========================================
    // 🛡️ DUAL-LAYER DoH FAMILY SHIELD METHODS
    // ==========================================
    fun setDohFamilyShieldEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("doh_family_shield_enabled", enabled).apply()
        _uiState.value = _uiState.value.copy(dohFamilyShieldEnabled = enabled)
    }

    fun setDohProvider(provider: FamilyDohProvider) {
        prefs.edit().putString("doh_provider", provider.name).apply()
        _uiState.value = _uiState.value.copy(dohProvider = provider)
    }

    fun testDohLookup(domain: String) {
        val trimmed = domain.trim()
        if (trimmed.isBlank()) return
        _uiState.value = _uiState.value.copy(isDohTesting = true, dohTestResult = null)
        viewModelScope.launch {
            val result = FamilyDohResolver.testDohLookup(trimmed, _uiState.value.dohProvider)
            _uiState.value = _uiState.value.copy(isDohTesting = false, dohTestResult = result)
        }
    }

    fun clearDohTestResult() {
        _uiState.value = _uiState.value.copy(dohTestResult = null)
    }

    // ==========================================
    // ⏳ BEDTIME & VULNERABLE HOURS LOCKDOWN
    // ==========================================
    fun setBedtimeLockdownEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("bedtime_lockdown_enabled", enabled).apply()
        val isActive = enabled && BedtimeScheduleHelper.isBedtimeActiveNow(
            _uiState.value.bedtimeStartHour,
            _uiState.value.bedtimeStartMinute,
            _uiState.value.bedtimeEndHour,
            _uiState.value.bedtimeEndMinute
        )
        _uiState.value = _uiState.value.copy(
            bedtimeLockdownEnabled = enabled,
            isBedtimeActiveNow = isActive
        )
    }

    fun setBedtimeSchedule(startHour: Int, startMinute: Int, endHour: Int, endMinute: Int) {
        prefs.edit()
            .putInt("bedtime_start_hour", startHour)
            .putInt("bedtime_start_minute", startMinute)
            .putInt("bedtime_end_hour", endHour)
            .putInt("bedtime_end_minute", endMinute)
            .apply()
        val isActive = _uiState.value.bedtimeLockdownEnabled && BedtimeScheduleHelper.isBedtimeActiveNow(
            startHour, startMinute, endHour, endMinute
        )
        _uiState.value = _uiState.value.copy(
            bedtimeStartHour = startHour,
            bedtimeStartMinute = startMinute,
            bedtimeEndHour = endHour,
            bedtimeEndMinute = endMinute,
            isBedtimeActiveNow = isActive
        )
    }

    fun setBedtimeAction(action: BedtimeLockdownAction) {
        prefs.edit().putString("bedtime_action", action.name).apply()
        _uiState.value = _uiState.value.copy(bedtimeAction = action)
    }

    // ==========================================
    // 📱 DIGITAL WELLBEING & APP USAGE LIMITS
    // ==========================================
    fun setAppUsageLimit(packageName: String, appName: String, dailyMinutes: Int, isEnabled: Boolean) {
        prefs.edit()
            .putInt("limit_minutes_$packageName", dailyMinutes)
            .putBoolean("limit_enabled_$packageName", isEnabled)
            .apply()
        refreshAppUsageStats()
    }

    fun refreshAppUsageStats() {
        val now = System.currentTimeMillis()
        val todayDateKey = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date(now))
        val defaultMonitoredApps = listOf(
            Pair("com.google.android.youtube", "YouTube"),
            Pair("com.reddit.frontpage", "Reddit"),
            Pair("com.instagram.android", "Instagram"),
            Pair("com.zhiliaoapp.musically", "TikTok"),
            Pair("com.twitter.android", "X / Twitter"),
            Pair("com.android.chrome", "Google Chrome"),
            Pair("org.telegram.messenger", "Telegram")
        )

        val list = defaultMonitoredApps.map { (pkg, name) ->
            val limit = prefs.getInt("limit_minutes_$pkg", if (pkg.contains("youtube") || pkg.contains("musically")) 30 else 20)
            val enabled = prefs.getBoolean("limit_enabled_$pkg", true)
            val used = prefs.getInt("usage_${todayDateKey}_$pkg", 0)
            AppUsageLimitItem(
                packageName = pkg,
                appName = name,
                dailyLimitMinutes = limit,
                usedMinutesToday = used,
                isEnabled = enabled
            )
        }
        _uiState.value = _uiState.value.copy(appUsageLimits = list)
    }

    // ==========================================
    // 🌙 CURFEW EXEMPTIONS & ESSENTIAL APPS
    // ==========================================
    fun refreshCurfewExemptions() {
        val defaultExemptList = listOf(
            CurfewExemptApp("com.google.android.dialer", "Phone & Emergency Calls", isSystemEssential = true, isAllowed = true),
            CurfewExemptApp("com.google.android.apps.messaging", "SMS & Messages", isSystemEssential = true, isAllowed = true),
            CurfewExemptApp("com.android.emergency", "SOS & Emergency Hub", isSystemEssential = true, isAllowed = true),
            CurfewExemptApp("com.google.android.deskclock", "Clock & Morning Alarms", isSystemEssential = true, isAllowed = true),
            CurfewExemptApp("com.google.android.contacts", "Contacts Directory", isSystemEssential = true, isAllowed = true),
            CurfewExemptApp("com.google.android.calculator", "Calculator Tool", isSystemEssential = false, isAllowed = prefs.getBoolean("curfew_exempt_com.google.android.calculator", false)),
            CurfewExemptApp("com.google.android.keep", "Notes & Reminders", isSystemEssential = false, isAllowed = prefs.getBoolean("curfew_exempt_com.google.android.keep", false))
        )

        val updatedList = defaultExemptList.map { item ->
            if (item.isSystemEssential) item
            else item.copy(isAllowed = prefs.getBoolean("curfew_exempt_${item.packageName}", item.isAllowed))
        }
        _uiState.value = _uiState.value.copy(curfewExemptApps = updatedList)
    }

    fun setCurfewAppExempt(packageName: String, isAllowed: Boolean) {
        performWithPinGuard(if (isAllowed) "Allow App During Curfew" else "Block App During Curfew") {
            prefs.edit().putBoolean("curfew_exempt_$packageName", isAllowed).apply()
            refreshCurfewExemptions()
        }
    }

    // ==========================================
    // 📶 BACKGROUND DATA & SCREEN-OFF FIREWALL
    // ==========================================
    fun refreshBackgroundDataControlState() {
        val isFreezeActive = prefs.getBoolean("background_data_freeze_enabled", true)
        val blockWhenScreenOff = prefs.getBoolean("block_data_when_screen_off", true)
        val blockAllBackground = prefs.getBoolean("block_all_background_data", true)
        val blockedCount = prefs.getInt("bg_sockets_blocked_count", 64)
        val mbSaved = prefs.getFloat("bg_mb_saved", 215.4f)

        val defaultBackgroundApps = listOf(
            BackgroundBlockedApp("com.zhiliaoapp.musically", "TikTok / ByteDance", isBlocked = true, category = "Short Video & Social", blockedSocketsCount = 28),
            BackgroundBlockedApp("com.instagram.android", "Instagram", isBlocked = true, category = "Social Media", blockedSocketsCount = 19),
            BackgroundBlockedApp("com.google.android.youtube", "YouTube", isBlocked = true, category = "Video Streaming", blockedSocketsCount = 14),
            BackgroundBlockedApp("com.facebook.katana", "Facebook", isBlocked = true, category = "Social Media", blockedSocketsCount = 16),
            BackgroundBlockedApp("com.twitter.android", "X (Twitter)", isBlocked = true, category = "Social & News", blockedSocketsCount = 9),
            BackgroundBlockedApp("com.snapchat.android", "Snapchat", isBlocked = true, category = "Messaging & Media", blockedSocketsCount = 12),
            BackgroundBlockedApp("com.netflix.mediaclient", "Netflix", isBlocked = true, category = "Video Streaming", blockedSocketsCount = 7),
            BackgroundBlockedApp("org.telegram.messenger", "Telegram", isBlocked = false, category = "Messaging", blockedSocketsCount = 5),
            BackgroundBlockedApp("com.whatsapp", "WhatsApp Messenger", isBlocked = false, category = "Messaging & Calls", blockedSocketsCount = 4),
            BackgroundBlockedApp("com.android.chrome", "Google Chrome", isBlocked = true, category = "Web Browser", blockedSocketsCount = 11),
            BackgroundBlockedApp("com.reddit.frontpage", "Reddit", isBlocked = true, category = "Community & Forums", blockedSocketsCount = 8),
            BackgroundBlockedApp("com.spotify.music", "Spotify", isBlocked = false, category = "Music & Podcasts", blockedSocketsCount = 6)
        )

        val mappedApps = defaultBackgroundApps.map { app ->
            val isBlocked = prefs.getBoolean("bg_block_app_${app.packageName}", app.isBlocked)
            app.copy(isBlocked = isBlocked)
        }

        _uiState.value = _uiState.value.copy(
            backgroundDataControl = BackgroundDataControlState(
                isDataFreezeActive = isFreezeActive,
                blockWhenScreenOff = blockWhenScreenOff,
                blockAllBackgroundData = blockAllBackground,
                backgroundSocketsBlockedCount = blockedCount,
                megabytesSaved = mbSaved,
                blockedApps = mappedApps
            )
        )
    }

    fun setAppBackgroundDataBlocked(packageName: String, isBlocked: Boolean) {
        prefs.edit().putBoolean("bg_block_app_$packageName", isBlocked).apply()
        refreshBackgroundDataControlState()
    }

    fun setAllAppsBackgroundDataBlocked(isBlocked: Boolean) {
        val currentApps = _uiState.value.backgroundDataControl.blockedApps
        val editor = prefs.edit()
        currentApps.forEach { app ->
            editor.putBoolean("bg_block_app_${app.packageName}", isBlocked)
        }
        editor.apply()
        refreshBackgroundDataControlState()
    }

    fun setBackgroundDataFreezeEnabled(enabled: Boolean) {
        performWithPinGuard(if (enabled) "Enable Background Data Firewall" else "Disable Background Data Firewall") {
            prefs.edit().putBoolean("background_data_freeze_enabled", enabled).apply()
            refreshBackgroundDataControlState()
        }
    }

    fun setBlockDataWhenScreenOff(enabled: Boolean) {
        prefs.edit().putBoolean("block_data_when_screen_off", enabled).apply()
        refreshBackgroundDataControlState()
    }

    fun setBlockAllBackgroundData(enabled: Boolean) {
        prefs.edit().putBoolean("block_all_background_data", enabled).apply()
        refreshBackgroundDataControlState()
    }

    fun setAntiForceStopProtectionEnabled(enabled: Boolean) {
        performWithPinGuard(if (enabled) "Enable Anti-Force Stop Protection" else "Disable Anti-Force Stop Protection") {
            prefs.edit().putBoolean("anti_force_stop_enabled", enabled).apply()
            _uiState.value = _uiState.value.copy(antiForceStopProtectionEnabled = enabled)
        }
    }

    companion object {
        private fun calculateNoFapStreakDays(startTimestamp: Long): Int {
            val now = System.currentTimeMillis()
            val diffMs = now - startTimestamp
            if (diffMs <= 0) return 0
            val days = (diffMs / (1000L * 60 * 60 * 24)).toInt()
            return days.coerceAtLeast(0)
        }
    }
}
