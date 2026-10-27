package com.example.ui.components

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PsDanger
import com.example.ui.theme.PsOnAccent
import com.example.ui.theme.PsText
import com.example.ui.theme.PsTextMuted

@Composable
fun AntiUninstallProtectionCard(
    isDeviceAdminActive: Boolean,
    isPreventUninstallEnabled: Boolean,
    isAntiForceStopEnabled: Boolean = true,
    isPinProtected: Boolean,
    onTogglePreventUninstall: (Boolean) -> Unit,
    onToggleAntiForceStop: (Boolean) -> Unit = {},
    onRequestActivateDeviceAdmin: (Context) -> Unit,
    onRefreshStatus: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isFullyGuarded = isPreventUninstallEnabled && isDeviceAdminActive && isAntiForceStopEnabled

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("anti_uninstall_protection_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
            // Anti-Uninstall Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isPreventUninstallEnabled) MaterialTheme.colorScheme.primaryContainer
                                else PsDanger.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isDeviceAdminActive) Icons.Filled.AdminPanelSettings else Icons.Filled.Security,
                            contentDescription = "Anti-Uninstall",
                            tint = if (isPreventUninstallEnabled) MaterialTheme.colorScheme.primary else PsDanger,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Anti-Uninstall",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = PsText
                        )
                        Text(
                            text = when {
                                isPreventUninstallEnabled && isDeviceAdminActive -> "Device Admin active"
                                isPreventUninstallEnabled -> "Watchdog active"
                                else -> "Disabled"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 13.sp,
                            color = when {
                                isPreventUninstallEnabled && isDeviceAdminActive -> MaterialTheme.colorScheme.primary
                                isPreventUninstallEnabled -> MaterialTheme.colorScheme.primary
                                else -> PsTextMuted
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Switch(
                    checked = isPreventUninstallEnabled,
                    onCheckedChange = { checked ->
                        onTogglePreventUninstall(checked)
                        if (checked && !isDeviceAdminActive) {
                            onRequestActivateDeviceAdmin(context)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PsOnAccent,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = PsTextMuted,
                        uncheckedTrackColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.testTag("prevent_uninstall_toggle_switch")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.height(12.dp))

            // Anti-Force Stop Protection Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isAntiForceStopEnabled) MaterialTheme.colorScheme.primaryContainer
                                else PsDanger.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Anti-Force Stop",
                            tint = if (isAntiForceStopEnabled) MaterialTheme.colorScheme.primary else PsTextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Anti-Force Stop",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = PsText
                        )
                        Text(
                            text = if (isAntiForceStopEnabled) "Active" else "Disabled",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 13.sp,
                            color = if (isAntiForceStopEnabled) MaterialTheme.colorScheme.primary else PsTextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Switch(
                    checked = isAntiForceStopEnabled,
                    onCheckedChange = { checked ->
                        onToggleAntiForceStop(checked)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PsOnAccent,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = PsTextMuted,
                        uncheckedTrackColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.testTag("anti_force_stop_toggle_switch")
                )
            }
        }
    }
}
