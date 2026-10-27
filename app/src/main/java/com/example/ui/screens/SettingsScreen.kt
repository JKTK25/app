package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import com.example.ui.components.FloatingPanel
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.PsTheme
import com.example.ThemeManager
import com.example.ui.components.AntiUninstallProtectionCard
import com.example.ui.components.BackgroundDataGuardCard
import com.example.ui.components.BedtimeAndAppLimitsCard
import com.example.ui.components.CrossBrowserGuardianCard
import com.example.ui.components.DualLayerDohCard
import com.example.ui.components.PinLockDialog
import com.example.ui.components.ThemesBottomSheet
import com.example.ui.theme.PsBorderStrong
import com.example.ui.theme.PsDanger
import com.example.ui.theme.PsOnAccent
import com.example.ui.theme.PsText
import com.example.ui.theme.PsTextMuted
import com.example.ui.theme.ShieldAmberWarning
import com.example.ui.theme.ShieldCrimsonDanger
import com.example.ui.theme.ShieldEmeraldSafe
import com.example.ui.theme.ShieldPrimary
import com.example.ui.viewmodel.ProtectionMode
import com.example.ui.viewmodel.ShieldViewModel

enum class SettingsCategory {
    THEMES,
    PROTECTION_LEVEL,
    BROWSER_GUARDIAN,
    FILTERS,
    UNLOCK_STARTUP,
    SECURITY_PIN
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ShieldViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val currentTheme by ThemeManager.themeFlow.collectAsState()

    var activeSheetCategory by remember { mutableStateOf<SettingsCategory?>(null) }
    var showPinDialog by remember { mutableStateOf(false) }
    var isSettingNewPin by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 22.dp), // @dimen/ps_screen_pad
        verticalArrangement = Arrangement.spacedBy(12.dp) // @dimen/ps_gap
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            // Section Header matching PS.Text.Title
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                color = PsText
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        // 1. Themes Category Card
        item {
            CategoryCard(
                icon = Icons.Filled.Palette,
                title = "Themes",
                summary = currentTheme.label,
                onClick = { activeSheetCategory = SettingsCategory.THEMES },
                tag = "category_themes"
            )
        }

        // 2. Protection level Category Card
        item {
            CategoryCard(
                icon = Icons.Filled.Speed,
                title = "Protection level",
                summary = uiState.protectionMode.title,
                onClick = { activeSheetCategory = SettingsCategory.PROTECTION_LEVEL },
                tag = "category_protection_level"
            )
        }

        // 3. Browser guardian Category Card
        item {
            CategoryCard(
                icon = Icons.Filled.Language,
                title = "Browser guardian",
                summary = if (uiState.isAccessibilityServiceActive) "Active" else "Setup required",
                onClick = { activeSheetCategory = SettingsCategory.BROWSER_GUARDIAN },
                tag = "category_browser_guardian"
            )
        }

        // 4. Filters Category Card
        item {
            CategoryCard(
                icon = Icons.Filled.FilterList,
                title = "Filters",
                summary = "DNS & Limits",
                onClick = { activeSheetCategory = SettingsCategory.FILTERS },
                tag = "category_filters"
            )
        }

        // 5. Unlock and startup Category Card
        item {
            CategoryCard(
                icon = Icons.Filled.PowerSettingsNew,
                title = "Unlock and startup",
                summary = if (uiState.autoOpenOnUnlock) "Watchdog active" else "Manual",
                onClick = { activeSheetCategory = SettingsCategory.UNLOCK_STARTUP },
                tag = "category_unlock_startup"
            )
        }

        // 6. Security and PIN Category Card
        item {
            CategoryCard(
                icon = Icons.Filled.Lock,
                title = "Security and PIN",
                summary = if (uiState.pinProtectionEnabled) "PIN locked" else "Not set",
                onClick = { activeSheetCategory = SettingsCategory.SECURITY_PIN },
                tag = "category_security_pin"
            )
        }

        // About PureShield Card
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "PURESHIELD",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PsTextMuted,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Client-side protection and watchdog.",
                        fontSize = 13.sp,
                        color = PsTextMuted,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Active Category Bottom Sheets
    when (activeSheetCategory) {
        SettingsCategory.THEMES -> {
            ThemesBottomSheet(
                onDismissRequest = { activeSheetCategory = null }
            )
        }

        SettingsCategory.PROTECTION_LEVEL -> {
            ProtectionLevelBottomSheet(
                protectionMode = uiState.protectionMode,
                onSelectMode = { viewModel.setProtectionMode(it) },
                onDismissRequest = { activeSheetCategory = null }
            )
        }

        SettingsCategory.BROWSER_GUARDIAN -> {
            BrowserGuardianBottomSheet(
                viewModel = viewModel,
                onDismissRequest = { activeSheetCategory = null }
            )
        }

        SettingsCategory.FILTERS -> {
            FiltersBottomSheet(
                viewModel = viewModel,
                onDismissRequest = { activeSheetCategory = null }
            )
        }

        SettingsCategory.UNLOCK_STARTUP -> {
            UnlockStartupBottomSheet(
                viewModel = viewModel,
                onDismissRequest = { activeSheetCategory = null }
            )
        }

        SettingsCategory.SECURITY_PIN -> {
            SecurityPinBottomSheet(
                viewModel = viewModel,
                onOpenSetPinDialog = { isNew ->
                    isSettingNewPin = isNew
                    showPinDialog = true
                },
                onDismissRequest = { activeSheetCategory = null }
            )
        }

        null -> {}
    }

    // Master PIN Dialog
    if (showPinDialog) {
        PinLockDialog(
            isSettingNewPin = isSettingNewPin,
            onPinSuccess = { pin ->
                if (isSettingNewPin) {
                    viewModel.setPin(pin)
                }
                showPinDialog = false
                true
            },
            onDismiss = { showPinDialog = false }
        )
    }

    // Auto-Assigned Sealed Vault Dialog
    if (uiState.autoAssignedPinDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissAutoAssignedPinDialog() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Password Sealed", fontWeight = FontWeight.Bold, color = PsText)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Master Password locked into vault.",
                        style = MaterialTheme.typography.bodySmall,
                        color = PsTextMuted
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.background,
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 18.dp, horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "• • • • • •",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 8.sp,
                                modifier = Modifier.testTag("auto_assigned_pin_display")
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ZERO-EXPOSURE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Policy,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Zero-exposure: hidden from all users.",
                                style = MaterialTheme.typography.labelSmall,
                                color = PsTextMuted
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissAutoAssignedPinDialog() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = PsOnAccent
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("dismiss_auto_pin_button")
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

/**
 * Standard Category Card adhering to layout/item_category.xml:
 * Surface fill, 1dp border, no shadows, 20dp corners, 18dp padding.
 */
@Composable
fun CategoryCard(
    icon: ImageVector,
    title: String,
    summary: String,
    onClick: () -> Unit,
    tag: String
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp), // @dimen/ps_radius_card
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp), // @dimen/ps_card_pad
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Tile
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Title & One-line Summary
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp, // PS.Text.Body
                    color = PsText
                )
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 13.sp, // PS.Text.Caption
                    color = PsTextMuted,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Chevron
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = PsTextMuted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProtectionLevelBottomSheet(
    protectionMode: ProtectionMode,
    onSelectMode: (ProtectionMode) -> Unit,
    onDismissRequest: () -> Unit
) {
    FloatingPanel(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(PsBorderStrong)
            )
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Speed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Protection level", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PsText)
                    Text("Defense strictness", fontSize = 13.sp, color = PsTextMuted)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            ProtectionMode.entries.forEach { mode ->
                val isSelected = protectionMode == mode
                Surface(
                    onClick = { onSelectMode(mode) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .testTag("mode_${mode.name.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = mode.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PsText
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = mode.levelTag,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Text(
                                text = mode.description,
                                fontSize = 12.sp,
                                color = PsTextMuted,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserGuardianBottomSheet(
    viewModel: ShieldViewModel,
    onDismissRequest: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    FloatingPanel(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(PsBorderStrong)
            )
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Browser guardian", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PsText)
                    Text("Browsers & SafeSearch", fontSize = 13.sp, color = PsTextMuted)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            CrossBrowserGuardianCard(
                isAccessibilityActive = uiState.isAccessibilityServiceActive,
                isCrossBrowserEnabled = uiState.isCrossBrowserProtectionEnabled,
                isStrictParentalEnabled = uiState.strictParentalControlEnabled,
                onToggleCrossBrowser = { viewModel.setCrossBrowserProtectionEnabled(it) },
                onToggleStrictParental = { viewModel.setStrictParentalControlEnabled(it) },
                onRefreshStatus = { viewModel.refreshAccessibilityStatus() }
            )

            Spacer(modifier = Modifier.height(14.dp))

            SettingToggleRow(
                title = "AI Semantic Defense",
                subtitle = "Slang and phrases",
                isChecked = uiState.aiSemanticEnforcementEnabled,
                onCheckedChange = { viewModel.setAiSemanticEnforcementEnabled(it) },
                tag = "ai_semantic_toggle"
            )

            SettingToggleRow(
                title = "Force Strict SafeSearch",
                subtitle = "Search engines",
                isChecked = uiState.forceSafeSearch,
                onCheckedChange = { viewModel.setForceSafeSearch(it) },
                tag = "force_safesearch_toggle"
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltersBottomSheet(
    viewModel: ShieldViewModel,
    onDismissRequest: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    FloatingPanel(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(PsBorderStrong)
            )
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.FilterList,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Filters", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PsText)
                    Text("DNS & Limits", fontSize = 13.sp, color = PsTextMuted)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            DualLayerDohCard(
                isEnabled = uiState.dohFamilyShieldEnabled,
                activeProvider = uiState.dohProvider,
                testResult = uiState.dohTestResult,
                isTesting = uiState.isDohTesting,
                onToggleEnabled = { viewModel.setDohFamilyShieldEnabled(it) },
                onSelectProvider = { viewModel.setDohProvider(it) },
                onRunTest = { domain -> viewModel.testDohLookup(domain) },
                onClearTest = { viewModel.clearDohTestResult() }
            )

            Spacer(modifier = Modifier.height(14.dp))

            BedtimeAndAppLimitsCard(
                isBedtimeEnabled = uiState.bedtimeLockdownEnabled,
                startHour = uiState.bedtimeStartHour,
                startMinute = uiState.bedtimeStartMinute,
                endHour = uiState.bedtimeEndHour,
                endMinute = uiState.bedtimeEndMinute,
                bedtimeAction = uiState.bedtimeAction,
                isBedtimeActiveNow = uiState.isBedtimeActiveNow,
                appLimits = uiState.appUsageLimits,
                curfewExemptApps = uiState.curfewExemptApps,
                globalScreenTimeLimitEnabled = uiState.globalScreenTimeLimitEnabled,
                globalScreenTimeLimitMinutes = uiState.globalScreenTimeLimitMinutes,
                onToggleBedtime = { viewModel.setBedtimeLockdownEnabled(it) },
                onUpdateBedtimeSchedule = { sH, sM, eH, eM -> viewModel.setBedtimeSchedule(sH, sM, eH, eM) },
                onSelectBedtimeAction = { viewModel.setBedtimeAction(it) },
                onToggleCurfewAppExempt = { pkg, allowed -> viewModel.setCurfewAppExempt(pkg, allowed) },
                onUpdateAppLimit = { pkg, name, mins, enabled -> viewModel.setAppUsageLimit(pkg, name, mins, enabled) },
                onToggleGlobalScreenTimeLimit = { viewModel.setGlobalScreenTimeLimitEnabled(it) },
                onUpdateGlobalScreenTimeLimit = { viewModel.setGlobalScreenTimeLimitMinutes(it) }
            )

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.background,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ENFORCEMENT RULES",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PsTextMuted,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    SettingToggleRow(
                        title = "Block Adult Websites",
                        subtitle = "Explicit domains",
                        isChecked = uiState.blockAdultWebsites,
                        onCheckedChange = { viewModel.setBlockAdultWebsites(it) },
                        tag = "block_adult_websites_toggle"
                    )

                    SettingToggleRow(
                        title = "Block Adult Ads & Redirects",
                        subtitle = "Popups & hops",
                        isChecked = uiState.blockAdultPopups,
                        onCheckedChange = { viewModel.setBlockAdultPopups(it) },
                        tag = "block_adult_popups_toggle"
                    )

                    SettingToggleRow(
                        title = "Allow Dating Apps",
                        subtitle = "Tinder, Bumble",
                        isChecked = uiState.allowDatingAppsAccess,
                        onCheckedChange = { viewModel.setAllowDatingAppsAccess(it) },
                        tag = "allow_dating_apps_toggle"
                    )

                    SettingToggleRow(
                        title = "Allow Social Media",
                        subtitle = "Social feeds",
                        isChecked = uiState.allowSocialMediaAccess,
                        onCheckedChange = { viewModel.setAllowSocialMediaAccess(it) },
                        tag = "allow_social_media_toggle"
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnlockStartupBottomSheet(
    viewModel: ShieldViewModel,
    onDismissRequest: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    FloatingPanel(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(PsBorderStrong)
            )
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PowerSettingsNew,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Unlock and startup", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PsText)
                    Text("Watchdog on unlock", fontSize = 13.sp, color = PsTextMuted)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.background,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "WATCHDOG CONTROLS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PsTextMuted,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    SettingToggleRow(
                        title = "Auto-Open on Phone Unlock",
                        subtitle = "Verification on unlock",
                        isChecked = uiState.autoOpenOnUnlock,
                        onCheckedChange = { viewModel.setAutoOpenOnUnlock(it) },
                        tag = "auto_open_unlock_toggle"
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            BackgroundDataGuardCard(
                dataControlState = uiState.backgroundDataControl,
                onToggleDataFreeze = { viewModel.setBackgroundDataFreezeEnabled(it) },
                onToggleBlockWhenScreenOff = { viewModel.setBlockDataWhenScreenOff(it) },
                onToggleBlockAllBackground = { viewModel.setBlockAllBackgroundData(it) },
                onToggleAppBackgroundBlocked = { pkg, blocked -> viewModel.setAppBackgroundDataBlocked(pkg, blocked) },
                onSetAllAppsBackgroundBlocked = { blocked -> viewModel.setAllAppsBackgroundDataBlocked(blocked) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityPinBottomSheet(
    viewModel: ShieldViewModel,
    onOpenSetPinDialog: (Boolean) -> Unit,
    onDismissRequest: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    FloatingPanel(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(PsBorderStrong)
            )
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Security and PIN", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PsText)
                    Text("Vault & anti-tamper", fontSize = 13.sp, color = PsTextMuted)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Master PIN Box
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.background,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "PIN LOCK",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = PsText
                            )
                            Text(
                                text = if (uiState.pinProtectionEnabled) "Active" else "Not set",
                                fontSize = 12.sp,
                                color = if (uiState.pinProtectionEnabled) MaterialTheme.colorScheme.primary else ShieldAmberWarning
                            )
                        }
                        Icon(
                            imageVector = if (uiState.pinProtectionEnabled) Icons.Filled.Lock else Icons.Filled.LockOpen,
                            contentDescription = null,
                            tint = if (uiState.pinProtectionEnabled) MaterialTheme.colorScheme.primary else ShieldAmberWarning
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (!uiState.pinProtectionEnabled) {
                        Button(
                            onClick = { onOpenSetPinDialog(true) },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("set_pin_button")
                        ) {
                            Text("SET PIN", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { viewModel.autoAssignMasterPassword() },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = PsOnAccent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("auto_assign_password_button")
                        ) {
                            Icon(Icons.Filled.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("AUTO-ASSIGN PASSWORD", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onOpenSetPinDialog(true) },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("change_pin_button")
                            ) {
                                Text("CHANGE PIN", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { viewModel.autoAssignMasterPassword() },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = PsOnAccent
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("auto_assign_password_button")
                            ) {
                                Text("⚡ AUTO PIN", fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { viewModel.removePin() },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PsDanger.copy(alpha = 0.15f),
                                contentColor = PsDanger
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("remove_pin_button")
                        ) {
                            Text("REMOVE PIN", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Strict Anti-Pornography Policy Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.background,
                border = BorderStroke(
                    1.dp,
                    if (uiState.strictPornographyPasswordPolicy) PsDanger.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "STRICT ENFORCEMENT",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = PsText
                            )
                            Text(
                                text = "Permanent lock on adult content",
                                fontSize = 12.sp,
                                color = PsTextMuted
                            )
                        }
                        Switch(
                            checked = uiState.strictPornographyPasswordPolicy,
                            onCheckedChange = { viewModel.setStrictPornographyPasswordPolicy(it) },
                            modifier = Modifier.testTag("strict_pornography_policy_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PsOnAccent,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                uncheckedThumbColor = PsTextMuted,
                                uncheckedTrackColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Anti-Uninstall Device Administrator Defense
            AntiUninstallProtectionCard(
                isDeviceAdminActive = uiState.isDeviceAdminActive,
                isPreventUninstallEnabled = uiState.preventAppUninstall,
                isAntiForceStopEnabled = uiState.antiForceStopProtectionEnabled,
                isPinProtected = uiState.pinProtectionEnabled,
                onTogglePreventUninstall = { viewModel.setPreventAppUninstall(it) },
                onToggleAntiForceStop = { viewModel.setAntiForceStopProtectionEnabled(it) },
                onRequestActivateDeviceAdmin = { ctx -> viewModel.requestDeviceAdminActivation(ctx) },
                onRefreshStatus = { viewModel.refreshDeviceAdminStatus() }
            )
        }
    }
}

@Composable
fun SettingToggleRow(
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = PsText
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = PsTextMuted,
                modifier = Modifier.padding(top = 1.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = PsOnAccent,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = PsTextMuted,
                uncheckedTrackColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}
