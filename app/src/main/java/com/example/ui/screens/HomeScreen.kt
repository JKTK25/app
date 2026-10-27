package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BlockedIncident
import com.example.data.model.ThreatCategory
import com.example.ui.components.CrossBrowserGuardianCard
import com.example.ui.components.NoFapTrackerCard
import com.example.ui.components.ShieldStatusRadar
import com.example.ui.theme.PsDanger
import com.example.ui.theme.PsText
import com.example.ui.theme.PsTextMuted
import com.example.ui.viewmodel.ShieldViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material.icons.filled.Shield
import com.example.ui.theme.ShieldPrimaryDark

@Composable
fun HomeScreen(
    viewModel: ShieldViewModel,
    onNavigateToMonitor: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val recentIncidents by viewModel.recentIncidents.collectAsState()
    val analytics by viewModel.analytics.collectAsState()
    val customRules by viewModel.allCustomRules.collectAsState()

    var isRecentThreatsExpanded by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 22.dp), // @dimen/ps_screen_pad
        verticalArrangement = Arrangement.spacedBy(12.dp) // @dimen/ps_gap
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            // Compact Status Header Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp)
                    .testTag("hero_banner_card"),
                shape = RoundedCornerShape(20.dp), // @dimen/ps_radius_card
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(ShieldPrimaryDark, MaterialTheme.colorScheme.surface)
                                )
                            )
                    )
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 18.dp)
                            .size(64.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "PURESHIELD",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = PsText
                            )
                            Text(
                                text = if (uiState.isShieldActive) "Active" else "Paused",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (uiState.isShieldActive) MaterialTheme.colorScheme.primary else PsDanger
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = if (uiState.isShieldActive) MaterialTheme.colorScheme.primaryContainer else PsDanger.copy(alpha = 0.15f),
                            border = BorderStroke(
                                1.dp,
                                if (uiState.isShieldActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else PsDanger.copy(alpha = 0.5f)
                            )
                        ) {
                            Text(
                                text = if (uiState.isShieldActive) "ARMED" else "PAUSED",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.isShieldActive) MaterialTheme.colorScheme.primary else PsDanger
                            )
                        }
                    }
                }
            }
        }

        // Radar Pulse & Shield Toggle
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
                        .padding(vertical = 18.dp, horizontal = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ShieldStatusRadar(
                        isActive = uiState.isShieldActive,
                        mode = uiState.protectionMode,
                        threatsBlockedCount = uiState.totalThreatsBlocked,
                        onToggle = {
                            viewModel.toggleShieldState()
                        }
                    )
                }
            }
        }

        // Live Quick Stats Strip
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickStatItem(
                    label = "BLOCKED",
                    value = "${analytics.blockedToday}",
                    accent = PsDanger,
                    modifier = Modifier.weight(1f)
                )
                QuickStatItem(
                    label = "SCANS",
                    value = "${analytics.unlockScansCount}",
                    accent = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                QuickStatItem(
                    label = "RULES",
                    value = "${customRules.size}",
                    accent = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Cross-Browser Parental Guardian Hub
        item {
            CrossBrowserGuardianCard(
                isAccessibilityActive = uiState.isAccessibilityServiceActive,
                isCrossBrowserEnabled = uiState.isCrossBrowserProtectionEnabled,
                isStrictParentalEnabled = uiState.strictParentalControlEnabled,
                onToggleCrossBrowser = { viewModel.setCrossBrowserProtectionEnabled(it) },
                onToggleStrictParental = { viewModel.setStrictParentalControlEnabled(it) },
                onRefreshStatus = { viewModel.refreshAccessibilityStatus() }
            )
        }

        // No-Fap Clean Streak & AI Motivation Tracker
        item {
            NoFapTrackerCard(
                streakDays = uiState.noFapStreakDays,
                streakWeeks = uiState.noFapStreakWeeks,
                goalDays = uiState.noFapGoalDays,
                longestStreakDays = uiState.noFapLongestStreakDays,
                totalResets = uiState.noFapTotalResets,
                personalMotivation = uiState.noFapPersonalMotivation,
                checkIns = uiState.noFapCheckIns,
                aiEncouragementEnabled = uiState.aiDailyEncouragementEnabled,
                onResetStreak = { reason -> viewModel.resetNoFapStreak(reason) },
                onSetGoal = { goal, motivation -> viewModel.setNoFapGoalDays(goal, motivation) },
                onUpdateMotivation = { motivation -> viewModel.updatePersonalMotivation(motivation) },
                onRecordCheckIn = { urge, reflection, mood -> viewModel.recordDailyCheckIn(urge, reflection, mood) },
                onToggleAiEncouragement = { enabled -> viewModel.setAiDailyEncouragementEnabled(enabled) },
                onSendTestMotivation = { viewModel.sendInstantAiEncouragementNotification() }
            )
        }

        // RECENT INTERCEPTED THREATS IN ONE COLLAPSIBLE CARD
        item {
            CollapsibleRecentThreatsCard(
                recentIncidents = recentIncidents,
                isExpanded = isRecentThreatsExpanded,
                onToggleExpand = { isRecentThreatsExpanded = !isRecentThreatsExpanded },
                onViewAll = onNavigateToMonitor
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun CollapsibleRecentThreatsCard(
    recentIncidents: List<BlockedIncident>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "expand_icon_rotation"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .testTag("collapsible_recent_threats_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row (Clickable for toggling expand/collapse)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(18.dp)
                    .testTag("toggle_recent_threats_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(PsDanger.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.History,
                            contentDescription = null,
                            tint = PsDanger,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Recent Threats",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = PsText
                        )
                        Text(
                            text = "${recentIncidents.size} logged",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 13.sp,
                            color = PsTextMuted
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (recentIncidents.isNotEmpty()) PsDanger.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer,
                        border = BorderStroke(
                            1.dp,
                            if (recentIncidents.isNotEmpty()) PsDanger.copy(alpha = 0.35f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                        )
                    ) {
                        Text(
                            text = if (recentIncidents.isNotEmpty()) "${recentIncidents.size}" else "Clean",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (recentIncidents.isNotEmpty()) PsDanger else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = onToggleExpand,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = PsTextMuted,
                            modifier = Modifier.rotate(rotationAngle)
                        )
                    }
                }
            }

            // Expandable Content
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .padding(bottom = 18.dp)
                ) {
                    HorizontalDivider(
                        modifier = Modifier.padding(bottom = 12.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outline
                    )

                    if (recentIncidents.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "No threats detected",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = PsTextMuted
                            )
                        }
                    } else {
                        // Section 10: Show first three entries with a "View all" button
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            recentIncidents.take(3).forEach { incident ->
                                CollapsedThreatRow(incident = incident)
                            }
                        }

                        if (recentIncidents.size > 3) {
                            Spacer(modifier = Modifier.height(10.dp))
                            TextButton(
                                onClick = onViewAll,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("view_all_threats_button"),
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text(
                                    text = "View all (${recentIncidents.size})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CollapsedThreatRow(incident: BlockedIncident) {
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(incident.timestamp))

    // Sensitive content rule from Section 10:
    // "shows blocked threats only as 'Explicit keyword blocked' and never prints the searched text"
    val isKeywordOrSearch = incident.category == ThreatCategory.EXPLICIT_KEYWORD ||
            incident.category == ThreatCategory.EXPLICIT_SEARCH ||
            incident.triggerReason.contains("Keyword", ignoreCase = true) ||
            incident.triggerReason.contains("Explicit", ignoreCase = true) ||
            incident.triggerReason.contains("Search", ignoreCase = true)

    val displayTitle = if (isKeywordOrSearch) "Explicit keyword blocked" else incident.target

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(PsDanger) // @drawable/dot_threat
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = PsText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${incident.category.displayName} • ${incident.triggerReason}",
                    style = MaterialTheme.typography.labelSmall,
                    color = PsTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = formattedTime,
                style = MaterialTheme.typography.labelSmall,
                color = PsTextMuted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun QuickStatItem(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 24.sp, // PS.Text.Number
                fontWeight = FontWeight.Bold,
                color = accent
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 11.sp, // PS.Text.Label
                fontWeight = FontWeight.SemiBold,
                color = PsTextMuted,
                letterSpacing = 0.5.sp,
                maxLines = 1
            )
        }
    }
}
