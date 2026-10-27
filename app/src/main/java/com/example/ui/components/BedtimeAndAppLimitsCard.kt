package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppUsageLimitItem
import com.example.data.model.BedtimeLockdownAction
import com.example.data.model.BedtimeScheduleHelper
import com.example.data.model.CurfewExemptApp
import com.example.ui.theme.ShieldAmberWarning
import com.example.ui.theme.ShieldBorder
import com.example.ui.theme.ShieldCrimsonDanger
import com.example.ui.theme.ShieldDarkCard
import com.example.ui.theme.ShieldDarkSurfaceVariant
import com.example.ui.theme.ShieldEmeraldSafe
import com.example.ui.theme.ShieldPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Message
import com.example.ui.theme.PsOnAccent
import com.example.ui.theme.PsText

@Composable
fun BedtimeAndAppLimitsCard(
    isBedtimeEnabled: Boolean,
    startHour: Int,
    startMinute: Int,
    endHour: Int,
    endMinute: Int,
    bedtimeAction: BedtimeLockdownAction,
    isBedtimeActiveNow: Boolean,
    appLimits: List<AppUsageLimitItem>,
    curfewExemptApps: List<CurfewExemptApp>,
    globalScreenTimeLimitEnabled: Boolean,
    globalScreenTimeLimitMinutes: Int,
    onToggleBedtime: (Boolean) -> Unit,
    onUpdateBedtimeSchedule: (startH: Int, startM: Int, endH: Int, endM: Int) -> Unit,
    onSelectBedtimeAction: (BedtimeLockdownAction) -> Unit,
    onToggleCurfewAppExempt: (packageName: String, isAllowed: Boolean) -> Unit,
    onUpdateAppLimit: (packageName: String, appName: String, minutes: Int, isEnabled: Boolean) -> Unit,
    onToggleGlobalScreenTimeLimit: (Boolean) -> Unit,
    onUpdateGlobalScreenTimeLimit: (minutes: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var editingApp by remember { mutableStateOf<AppUsageLimitItem?>(null) }
    var showScheduleDialog by remember { mutableStateOf(false) }

    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "bedtime_chevron_rotation"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("bedtime_and_app_limits_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ShieldDarkCard),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isBedtimeActiveNow) ShieldAmberWarning.copy(alpha = 0.8f) else if (isBedtimeEnabled) ShieldPrimary.copy(alpha = 0.5f) else ShieldBorder
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                Brush.linearGradient(listOf(ShieldPrimary.copy(alpha = 0.7f), ShieldPrimary)),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = "Bedtime Lockdown",
                            tint = PsOnAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Bedtime & Screen Time Limits",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        val timeFormatted = "${BedtimeScheduleHelper.formatTime(startHour, startMinute)} – ${BedtimeScheduleHelper.formatTime(endHour, endMinute)}"
                        Text(
                            text = if (isBedtimeActiveNow) "🌙 CURFEW ACTIVE NOW ($timeFormatted)" else if (isBedtimeEnabled) "Curfew set for $timeFormatted" else "Night lockdown paused",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isBedtimeActiveNow) ShieldAmberWarning else if (isBedtimeEnabled) ShieldEmeraldSafe else TextMuted
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = isBedtimeEnabled,
                        onCheckedChange = onToggleBedtime,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PsOnAccent,
                            checkedTrackColor = ShieldEmeraldSafe,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = ShieldDarkSurfaceVariant
                        ),
                        modifier = Modifier.testTag("bedtime_enable_switch")
                    )

                    IconButton(onClick = { isExpanded = !isExpanded }) {
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = "Expand Bedtime Options",
                            tint = TextMuted,
                            modifier = Modifier.rotate(chevronRotation)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    HorizontalDivider(color = ShieldBorder.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Active Curfew Banner if active
                    if (isBedtimeActiveNow) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ShieldAmberWarning.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ShieldAmberWarning.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Nightlight,
                                    contentDescription = "Night Curfew",
                                    tint = ShieldAmberWarning,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Vulnerable Hours Active",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = ShieldAmberWarning
                                    )
                                    Text(
                                        text = "Browser access locked & AI sensitivity tightened to prevent late-night relapse.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = PsText.copy(alpha = 0.85f),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Schedule Configuration Card
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = ShieldDarkSurfaceVariant.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ShieldBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = "Schedule",
                                        tint = ShieldPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Night Curfew Schedule",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = PsText
                                    )
                                }

                                TextButton(
                                    onClick = { showScheduleDialog = true },
                                    modifier = Modifier.testTag("edit_bedtime_schedule_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Schedule",
                                        modifier = Modifier.size(14.dp),
                                        tint = ShieldPrimary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Change", color = ShieldPrimary, fontSize = 12.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "STARTS (LOCKDOWN)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                    Text(
                                        text = BedtimeScheduleHelper.formatTime(startHour, startMinute),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = PsText
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "ENDS (MORNING)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                    Text(
                                        text = BedtimeScheduleHelper.formatTime(endHour, endMinute),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = PsText
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = ShieldBorder.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "CURFEW DEFENSE ACTION",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = ShieldPrimary,
                                letterSpacing = 1.sp
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            BedtimeLockdownAction.values().forEach { action ->
                                val isSelected = bedtimeAction == action
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectBedtimeAction(action) }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { onSelectBedtimeAction(action) },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = ShieldPrimary,
                                            unselectedColor = TextMuted
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = action.displayName,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isSelected) PsText else TextPrimary
                                        )
                                        Text(
                                            text = action.description,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted
                                        )
                                    }
                                }
                            }

                            // 📞 Curfew Exemptions (Calls, Messages & Emergency)
                            if (bedtimeAction == BedtimeLockdownAction.ENTIRE_DEVICE_LOCK) {
                                Spacer(modifier = Modifier.height(14.dp))
                                HorizontalDivider(color = ShieldBorder.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "CURFEW ALLOWED APPS (EXEMPTIONS)",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = ShieldEmeraldSafe,
                                            letterSpacing = 1.sp
                                        )
                                        Text(
                                            text = "Calls, SMS & Emergency are always kept accessible",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = "Essential Apps Protected",
                                        tint = ShieldEmeraldSafe,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                curfewExemptApps.forEach { app ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = ShieldDarkCard.copy(alpha = 0.6f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, ShieldBorder.copy(alpha = 0.4f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                val icon = when {
                                                    app.packageName.contains("dialer") || app.packageName.contains("phone") -> Icons.Default.Call
                                                    app.packageName.contains("messaging") || app.packageName.contains("mms") -> Icons.Default.Message
                                                    app.packageName.contains("clock") -> Icons.Default.Schedule
                                                    else -> Icons.Default.PhoneAndroid
                                                }
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = app.appName,
                                                    tint = if (app.isAllowed) ShieldEmeraldSafe else TextMuted,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = app.appName,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            fontWeight = FontWeight.Medium,
                                                            color = if (app.isAllowed) PsText else TextMuted
                                                        )
                                                        if (app.isSystemEssential) {
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Surface(
                                                                shape = RoundedCornerShape(4.dp),
                                                                color = ShieldEmeraldSafe.copy(alpha = 0.2f)
                                                            ) {
                                                                Text(
                                                                    text = "ESSENTIAL",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = ShieldEmeraldSafe,
                                                                    fontSize = 9.sp,
                                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                    Text(
                                                        text = if (app.isAllowed) "Unrestricted during curfew lockdown" else "Blocked during curfew lockdown",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = TextMuted,
                                                        fontSize = 10.sp
                                                    )
                                                }
                                            }

                                            if (!app.isSystemEssential) {
                                                Switch(
                                                    checked = app.isAllowed,
                                                    onCheckedChange = { onToggleCurfewAppExempt(app.packageName, it) },
                                                    colors = SwitchDefaults.colors(
                                                        checkedThumbColor = PsOnAccent,
                                                        checkedTrackColor = ShieldEmeraldSafe,
                                                        uncheckedThumbColor = TextMuted,
                                                        uncheckedTrackColor = ShieldDarkSurfaceVariant
                                                    ),
                                                    modifier = Modifier.size(36.dp)
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "Always Allowed",
                                                    tint = ShieldEmeraldSafe,
                                                    modifier = Modifier.size(20.dp).padding(end = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Global Screen Time Limit Section
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = ShieldDarkSurfaceVariant.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ShieldBorder)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.HourglassBottom,
                                        contentDescription = "Global Screen Time",
                                        tint = ShieldPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "GLOBAL SCREEN TIME LIMIT",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = ShieldPrimary,
                                        letterSpacing = 1.sp
                                    )
                                }
                                Switch(
                                    checked = globalScreenTimeLimitEnabled,
                                    onCheckedChange = onToggleGlobalScreenTimeLimit,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = PsOnAccent,
                                        checkedTrackColor = ShieldEmeraldSafe,
                                        uncheckedThumbColor = TextMuted,
                                        uncheckedTrackColor = ShieldDarkSurfaceVariant
                                    )
                                )
                            }
                            
                            AnimatedVisibility(visible = globalScreenTimeLimitEnabled) {
                                Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Daily Total Limit",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "${globalScreenTimeLimitMinutes / 60}h ${globalScreenTimeLimitMinutes % 60}m",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = PsText
                                        )
                                    }
                                    
                                    Slider(
                                        value = globalScreenTimeLimitMinutes.toFloat(),
                                        onValueChange = { onUpdateGlobalScreenTimeLimit(it.toInt()) },
                                        valueRange = 30f..480f,
                                        steps = 15,
                                        colors = SliderDefaults.colors(
                                            thumbColor = ShieldPrimary,
                                            activeTrackColor = ShieldPrimary,
                                            inactiveTrackColor = ShieldDarkSurfaceVariant
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // App Usage Limits Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "APP USAGE LIMITS (DAILY SCREEN TIME)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ShieldPrimary,
                            letterSpacing = 1.sp
                        )

                        Text(
                            text = "${appLimits.count { it.isEnabled }} active",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    appLimits.forEach { item ->
                        val usedFraction = if (item.dailyLimitMinutes > 0) {
                            (item.usedMinutesToday.toFloat() / item.dailyLimitMinutes.toFloat()).coerceIn(0f, 1f)
                        } else 0f
                        val isExceeded = item.dailyLimitMinutes > 0 && item.usedMinutesToday >= item.dailyLimitMinutes

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = ShieldDarkSurfaceVariant.copy(alpha = 0.4f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isExceeded && item.isEnabled) ShieldCrimsonDanger.copy(alpha = 0.6f) else ShieldBorder
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PhoneAndroid,
                                            contentDescription = item.appName,
                                            tint = if (item.isEnabled) ShieldPrimary else TextMuted,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = item.appName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = PsText
                                            )
                                            Text(
                                                text = if (item.isEnabled) "${item.usedMinutesToday} / ${item.dailyLimitMinutes} min used" else "Limit paused",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isExceeded && item.isEnabled) ShieldCrimsonDanger else TextMuted,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { editingApp = item },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit Limit",
                                                tint = TextMuted,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        Switch(
                                            checked = item.isEnabled,
                                            onCheckedChange = { isChecked ->
                                                onUpdateAppLimit(item.packageName, item.appName, item.dailyLimitMinutes, isChecked)
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = PsOnAccent,
                                                checkedTrackColor = ShieldEmeraldSafe,
                                                uncheckedThumbColor = TextMuted,
                                                uncheckedTrackColor = ShieldDarkSurfaceVariant
                                            )
                                        )
                                    }
                                }

                                if (item.isEnabled && item.dailyLimitMinutes > 0) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { usedFraction },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp),
                                        color = if (isExceeded) ShieldCrimsonDanger else if (usedFraction > 0.8f) ShieldAmberWarning else ShieldPrimary,
                                        trackColor = ShieldDarkCard
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    // Schedule Dialog
    if (showScheduleDialog) {
        var sHour by remember { mutableIntStateOf(startHour) }
        var eHour by remember { mutableIntStateOf(endHour) }

        AlertDialog(
            onDismissRequest = { showScheduleDialog = false },
            title = {
                Text(
                    text = "Configure Vulnerable Hours",
                    fontWeight = FontWeight.Bold,
                    color = PsText
                )
            },
            text = {
                Column {
                    Text(
                        text = "Set the evening start time and morning end time for curfew lockdown.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Bedtime Starts: ${BedtimeScheduleHelper.formatTime(sHour, 0)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = PsText
                    )
                    Slider(
                        value = sHour.toFloat(),
                        onValueChange = { sHour = it.toInt() },
                        valueRange = 18f..23f,
                        steps = 5,
                        colors = SliderDefaults.colors(
                            thumbColor = ShieldPrimary,
                            activeTrackColor = ShieldPrimary,
                            inactiveTrackColor = ShieldBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Curfew Ends: ${BedtimeScheduleHelper.formatTime(eHour, 0)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = PsText
                    )
                    Slider(
                        value = eHour.toFloat(),
                        onValueChange = { eHour = it.toInt() },
                        valueRange = 4f..10f,
                        steps = 5,
                        colors = SliderDefaults.colors(
                            thumbColor = ShieldPrimary,
                            activeTrackColor = ShieldPrimary,
                            inactiveTrackColor = ShieldBorder
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateBedtimeSchedule(sHour, 0, eHour, 0)
                        showScheduleDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ShieldPrimary)
                ) {
                    Text("Save Schedule")
                }
            },
            dismissButton = {
                TextButton(onClick = { showScheduleDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = ShieldDarkCard,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // App Limit Dialog
    editingApp?.let { appItem ->
        var limitSliderVal by remember { mutableFloatStateOf(appItem.dailyLimitMinutes.toFloat()) }

        AlertDialog(
            onDismissRequest = { editingApp = null },
            title = {
                Text(
                    text = "Screen Time Limit: ${appItem.appName}",
                    fontWeight = FontWeight.Bold,
                    color = PsText
                )
            },
            text = {
                Column {
                    Text(
                        text = "Set daily maximum time allocation. App will be automatically closed when limit is reached.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Daily Limit: ${limitSliderVal.toInt()} minutes",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = ShieldPrimary
                    )

                    Slider(
                        value = limitSliderVal,
                        onValueChange = { limitSliderVal = it },
                        valueRange = 5f..120f,
                        steps = 22,
                        colors = SliderDefaults.colors(
                            thumbColor = ShieldPrimary,
                            activeTrackColor = ShieldPrimary,
                            inactiveTrackColor = ShieldBorder
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateAppLimit(appItem.packageName, appItem.appName, limitSliderVal.toInt(), true)
                        editingApp = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ShieldPrimary)
                ) {
                    Text("Save Limit")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingApp = null }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = ShieldDarkCard,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
