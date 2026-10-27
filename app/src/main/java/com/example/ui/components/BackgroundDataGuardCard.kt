package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DataSaverOn
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PowerOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BackgroundBlockedApp
import com.example.data.model.BackgroundDataControlState
import com.example.ui.theme.ShieldAmberWarning
import com.example.ui.theme.ShieldBorder
import com.example.ui.theme.ShieldCrimsonDanger
import com.example.ui.theme.ShieldDarkCard
import com.example.ui.theme.ShieldDarkSurfaceVariant
import com.example.ui.theme.ShieldEmeraldSafe
import com.example.ui.theme.ShieldPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.PsOnAccent
import com.example.ui.theme.PsText

@Composable
fun BackgroundDataGuardCard(
    dataControlState: BackgroundDataControlState,
    onToggleDataFreeze: (Boolean) -> Unit,
    onToggleBlockWhenScreenOff: (Boolean) -> Unit,
    onToggleBlockAllBackground: (Boolean) -> Unit,
    onToggleAppBackgroundBlocked: (packageName: String, isBlocked: Boolean) -> Unit = { _, _ -> },
    onSetAllAppsBackgroundBlocked: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "data_guard_chevron_rotation"
    )

    val blockedAppsCount = dataControlState.blockedApps.count { it.isBlocked }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("background_data_guard_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ShieldDarkCard),
        border = BorderStroke(
            1.dp,
            if (dataControlState.isDataFreezeActive) ShieldEmeraldSafe.copy(alpha = 0.5f) else ShieldBorder
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
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
                            .clip(CircleShape)
                            .background(
                                if (dataControlState.isDataFreezeActive) {
                                    Brush.radialGradient(
                                        colors = listOf(
                                            ShieldEmeraldSafe.copy(alpha = 0.35f),
                                            ShieldDarkSurfaceVariant
                                        )
                                    )
                                } else {
                                    Brush.radialGradient(
                                        colors = listOf(
                                            ShieldAmberWarning.copy(alpha = 0.2f),
                                            ShieldDarkSurfaceVariant
                                        )
                                    )
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WifiOff,
                            contentDescription = "Background Data Guard",
                            tint = if (dataControlState.isDataFreezeActive) ShieldEmeraldSafe else TextMuted,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Background Data & App Firewall",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PsText
                            )
                        }
                        Text(
                            text = if (dataControlState.isDataFreezeActive) {
                                "$blockedAppsCount apps blocked from background internet"
                            } else {
                                "Background Data Firewall Paused"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (dataControlState.isDataFreezeActive) ShieldEmeraldSafe else ShieldAmberWarning
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = dataControlState.isDataFreezeActive,
                        onCheckedChange = { onToggleDataFreeze(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PsOnAccent,
                            checkedTrackColor = ShieldEmeraldSafe,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = ShieldDarkSurfaceVariant
                        ),
                        modifier = Modifier.testTag("data_freeze_master_toggle")
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = "Expand Data Guard Settings",
                        tint = TextMuted,
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(chevronRotation)
                    )
                }
            }

            // Expanded Controls
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    HorizontalDivider(color = ShieldBorder.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Live Stats Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ShieldDarkSurfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, ShieldBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "SOCKETS INTERCEPTED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${dataControlState.backgroundSocketsBlockedCount} sockets",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ShieldEmeraldSafe
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ShieldDarkSurfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, ShieldBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "DATA SAVED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${String.format("%.1f", dataControlState.megabytesSaved)} MB",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ShieldPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Option 1: Block when screen is off
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ShieldDarkSurfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, ShieldBorder.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PowerOff,
                                    contentDescription = "Screen Off Data Block",
                                    tint = if (dataControlState.blockWhenScreenOff) ShieldEmeraldSafe else TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Cut Network When Screen is Turned Off",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Completely isolates Wi-Fi & Mobile data when phone is locked to prevent background syncing and tracking.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                }
                            }
                            Switch(
                                checked = dataControlState.blockWhenScreenOff,
                                onCheckedChange = { onToggleBlockWhenScreenOff(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = PsOnAccent,
                                    checkedTrackColor = ShieldEmeraldSafe,
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = ShieldDarkSurfaceVariant
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Option 2: Block all background data
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ShieldDarkSurfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, ShieldBorder.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DataSaverOn,
                                    contentDescription = "Background Data Saver",
                                    tint = if (dataControlState.blockAllBackgroundData) ShieldPrimary else TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Block Background App Data Sockets",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Restricts selected apps from opening background TCP/UDP sockets when minimized.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                }
                            }
                            Switch(
                                checked = dataControlState.blockAllBackgroundData,
                                onCheckedChange = { onToggleBlockAllBackground(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = PsOnAccent,
                                    checkedTrackColor = ShieldPrimary,
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = ShieldDarkSurfaceVariant
                                )
                            )
                        }
                    }

                    // 📱 APPS TO BE BLOCKED FROM BACKGROUND INTERNET
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = ShieldBorder.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "APPS BLOCKED FROM BACKGROUND INTERNET",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = ShieldPrimary,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = "$blockedAppsCount of ${dataControlState.blockedApps.size} apps restricted in background",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = ShieldCrimsonDanger.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, ShieldCrimsonDanger.copy(alpha = 0.3f)),
                                modifier = Modifier.clickable { onSetAllAppsBackgroundBlocked(true) }
                            ) {
                                Text(
                                    text = "Block All",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ShieldCrimsonDanger,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = ShieldEmeraldSafe.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, ShieldEmeraldSafe.copy(alpha = 0.3f)),
                                modifier = Modifier.clickable { onSetAllAppsBackgroundBlocked(false) }
                            ) {
                                Text(
                                    text = "Allow All",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ShieldEmeraldSafe,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    dataControlState.blockedApps.forEach { app ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ShieldDarkCard.copy(alpha = 0.7f),
                            border = BorderStroke(
                                1.dp,
                                if (app.isBlocked) ShieldCrimsonDanger.copy(alpha = 0.4f) else ShieldBorder.copy(alpha = 0.4f)
                            ),
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
                                        app.packageName.contains("musically") || app.packageName.contains("youtube") -> Icons.Default.Videocam
                                        app.packageName.contains("instagram") || app.packageName.contains("facebook") || app.packageName.contains("twitter") || app.packageName.contains("reddit") -> Icons.Default.People
                                        app.packageName.contains("netflix") -> Icons.Default.Movie
                                        app.packageName.contains("spotify") -> Icons.Default.MusicNote
                                        app.packageName.contains("chrome") -> Icons.Default.Language
                                        app.packageName.contains("telegram") || app.packageName.contains("whatsapp") || app.packageName.contains("snapchat") -> Icons.Default.Send
                                        else -> Icons.Default.PhoneAndroid
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (app.isBlocked) ShieldCrimsonDanger.copy(alpha = 0.15f)
                                                else ShieldEmeraldSafe.copy(alpha = 0.15f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = app.appName,
                                            tint = if (app.isBlocked) ShieldCrimsonDanger else ShieldEmeraldSafe,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = app.appName,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (app.isBlocked) PsText else TextMuted
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (app.isBlocked) ShieldCrimsonDanger.copy(alpha = 0.2f) else ShieldEmeraldSafe.copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = if (app.isBlocked) "BLOCKED" else "ALLOWED",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (app.isBlocked) ShieldCrimsonDanger else ShieldEmeraldSafe,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            Text(
                                                text = app.category,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextMuted,
                                                fontSize = 10.sp
                                            )
                                            if (app.isBlocked) {
                                                Text(
                                                    text = " • ${app.blockedSocketsCount} sockets blocked",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = ShieldAmberWarning,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                    }
                                }

                                Switch(
                                    checked = app.isBlocked,
                                    onCheckedChange = { onToggleAppBackgroundBlocked(app.packageName, it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = PsOnAccent,
                                        checkedTrackColor = ShieldCrimsonDanger,
                                        uncheckedThumbColor = TextMuted,
                                        uncheckedTrackColor = ShieldDarkSurfaceVariant
                                    ),
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
