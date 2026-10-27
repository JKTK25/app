package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UnlockLog
import com.example.ui.theme.PsDanger
import com.example.ui.theme.PsText
import com.example.ui.theme.PsTextMuted
import com.example.ui.viewmodel.ShieldViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MonitorScreen(
    viewModel: ShieldViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val unlockLogs by viewModel.recentUnlockLogs.collectAsState()
    var showAllLogs by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Monitor",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                color = PsText
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Live System Status Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ACTIVE MODULES",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = PsText
                        )
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "MONITORING",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    ProtectionModuleItem(
                        name = "Phone Unlock Watchdog",
                        desc = "Unlock scan",
                        isActive = uiState.autoOpenOnUnlock
                    )
                    ProtectionModuleItem(
                        name = "Adult URL & Domain Filter",
                        desc = "Explicit domains",
                        isActive = uiState.blockAdultWebsites
                    )
                    ProtectionModuleItem(
                        name = "Strict SafeSearch Enforcer",
                        desc = "Search engines",
                        isActive = uiState.forceSafeSearch
                    )
                    ProtectionModuleItem(
                        name = "Explicit Query Interceptor",
                        desc = "Keywords",
                        isActive = uiState.blockExplicitSearch
                    )
                    ProtectionModuleItem(
                        name = "Cross-Browser Guardian",
                        desc = "Browsers",
                        isActive = uiState.isAccessibilityServiceActive && uiState.isCrossBrowserProtectionEnabled
                    )
                    ProtectionModuleItem(
                        name = "AI Semantic Defense",
                        desc = "Slang & leetspeak",
                        isActive = uiState.aiSemanticEnforcementEnabled
                    )
                    ProtectionModuleItem(
                        name = "Anti-Tamper Device Defense",
                        desc = "Vault lock",
                        isActive = uiState.pinProtectionEnabled
                    )
                }
            }
        }

        // Phone Unlock Audit Logs Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "DEVICE UNLOCK AUDIT",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = PsText
                    )
                    Text(
                        text = "Recorded events",
                        fontSize = 12.sp,
                        color = PsTextMuted
                    )
                }
                IconButton(
                    onClick = { viewModel.recordUnlockAudit("Manual unlock verification scan triggered") },
                    modifier = Modifier.testTag("record_manual_unlock_scan_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Trigger Scan",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (unlockLogs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No unlock audits recorded.",
                            style = MaterialTheme.typography.bodySmall,
                            color = PsTextMuted
                        )
                    }
                }
            }
        } else {
            // Section 10: Show first three entries with a "View all" button
            val displayedLogs = if (showAllLogs) unlockLogs else unlockLogs.take(3)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    displayedLogs.forEach { log ->
                        UnlockLogItem(log = log)
                    }
                }

                if (unlockLogs.size > 3) {
                    Spacer(modifier = Modifier.height(10.dp))
                    TextButton(
                        onClick = { showAllLogs = !showAllLogs },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("toggle_view_all_logs_button"),
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            text = if (showAllLogs) "Show less" else "View all (${unlockLogs.size} total)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
fun ProtectionModuleItem(
    name: String,
    desc: String,
    isActive: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isActive) MaterialTheme.colorScheme.primary else PsDanger)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = PsText
            )
            Text(
                text = desc,
                fontSize = 12.sp,
                color = PsTextMuted
            )
        }
    }
}

@Composable
fun UnlockLogItem(log: UnlockLog) {
    val timeFormat = SimpleDateFormat("MMM dd, hh:mm:ss a", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(log.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.LockOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.details,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PsText
                )
                Text(
                    text = "$formattedTime • ${log.shieldStatus}",
                    fontSize = 11.sp,
                    color = PsTextMuted
                )
            }
        }
    }
}
