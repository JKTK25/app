package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.ui.draw.rotate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NoFapCheckIn
import com.example.ui.theme.ShieldAmberWarning
import com.example.ui.theme.ShieldBorder
import com.example.ui.theme.ShieldCrimsonDanger
import com.example.ui.theme.ShieldDarkBg
import com.example.ui.theme.ShieldDarkCard
import com.example.ui.theme.ShieldDarkSurfaceVariant
import com.example.ui.theme.ShieldEmeraldSafe
import com.example.ui.theme.ShieldPrimary
import com.example.ui.theme.ShieldPrimaryLight
import com.example.ui.theme.ShieldTertiary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.ui.theme.PsOnAccent
import com.example.ui.theme.PsText
import com.example.ui.theme.PsOnDanger
import com.example.ui.theme.medalColors

data class MilestoneBadge(
    val dayThreshold: Int,
    val title: String,
    val rankTitle: String,
    val badgeColor: Color
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NoFapTrackerCard(
    streakDays: Int,
    streakWeeks: Int,
    goalDays: Int,
    longestStreakDays: Int,
    totalResets: Int,
    personalMotivation: String,
    checkIns: List<NoFapCheckIn>,
    aiEncouragementEnabled: Boolean,
    onResetStreak: (reason: String) -> Unit,
    onSetGoal: (Int, String?) -> Unit,
    onUpdateMotivation: (String) -> Unit,
    onRecordCheckIn: (urgeLevel: Int, reflection: String, mood: String) -> Unit,
    onToggleAiEncouragement: (Boolean) -> Unit,
    onSendTestMotivation: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var showCheckInDialog by remember { mutableStateOf(false) }
    var showMotivationEditDialog by remember { mutableStateOf(false) }
    var showHistoryExpanded by remember { mutableStateOf(false) }

    val progress = if (goalDays > 0) (streakDays.toFloat() / goalDays.toFloat()).coerceIn(0f, 1f) else 0f

    val medals = medalColors(ShieldPrimary)
    val milestones = remember(medals) {
        listOf(
            MilestoneBadge(3, "3 Days", "Iron Will", medals[0]),
            MilestoneBadge(7, "7 Days", "Bronze Guard", medals[1]),
            MilestoneBadge(14, "14 Days", "Silver Focus", medals[2]),
            MilestoneBadge(30, "30 Days", "Golden Clarity", medals[3]),
            MilestoneBadge(60, "60 Days", "Emerald Shield", medals[4]),
            MilestoneBadge(90, "90 Days", "Platinum Titan", medals[5]),
            MilestoneBadge(180, "180 Days", "Diamond Purity", medals[6]),
            MilestoneBadge(365, "365 Days", "Titanium Master", medals[7])
        )
    }

    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "expand_rotation"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("no_fap_tracker_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = ShieldDarkCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, ShieldPrimary.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Title, Clean Streak Pill, and Expand/Collapse Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(ShieldPrimary, ShieldTertiary)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SelfImprovement,
                            contentDescription = "No-Fap Icon",
                            tint = PsOnAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Clean Streak",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PsText
                        )
                        Text(
                            text = "$streakDays days clean",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ShieldEmeraldSafe.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ShieldEmeraldSafe.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocalFireDepartment,
                                contentDescription = null,
                                tint = ShieldAmberWarning,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "$streakDays D CLEAN",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = PsText,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse Card" else "Expand Card",
                            tint = ShieldPrimaryLight,
                            modifier = Modifier.rotate(rotationAngle)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(14.dp))

            // Primary Key Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StreakMetricBox(
                    label = "ACTIVE STREAK",
                    value = "$streakDays",
                    subtitle = if (streakDays == 1) "Clean Day" else "Clean Days",
                    accentColor = ShieldPrimaryLight,
                    modifier = Modifier.weight(1f)
                )

                StreakMetricBox(
                    label = "WEEKS",
                    value = "$streakWeeks",
                    subtitle = if (streakWeeks == 1) "Week Clean" else "Weeks Clean",
                    accentColor = ShieldEmeraldSafe,
                    modifier = Modifier.weight(1f)
                )

                StreakMetricBox(
                    label = "ALL-TIME BEST",
                    value = "$longestStreakDays",
                    subtitle = "Longest Days",
                    accentColor = ShieldAmberWarning,
                    modifier = Modifier.weight(1f)
                )

                StreakMetricBox(
                    label = "GOAL",
                    value = "$goalDays",
                    subtitle = "Day Target",
                    accentColor = ShieldTertiary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Goal Progress Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ShieldDarkBg)
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.EmojiEvents,
                            contentDescription = null,
                            tint = ShieldAmberWarning,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Personal Target: $goalDays Days",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = PsText
                        )
                    }

                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = ShieldPrimaryLight
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape),
                    color = ShieldPrimary,
                    trackColor = ShieldDarkSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                val remainingDays = (goalDays - streakDays).coerceAtLeast(0)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (remainingDays == 0) "🎉 Milestone Reached! Set a higher target." else "$remainingDays days remaining to reach milestone",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (remainingDays == 0) ShieldEmeraldSafe else TextMuted
                    )

                    Text(
                        text = "$streakDays / $goalDays d",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = PsText
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Personal Goal & Motivation Purpose Box
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showMotivationEditDialog = true },
                color = ShieldDarkBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, ShieldBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FormatQuote,
                            contentDescription = null,
                            tint = ShieldPrimaryLight,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "PERSONAL PURPOSE & MOTIVATION",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = ShieldPrimaryLight,
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = personalMotivation.ifBlank { "Tap to define your core reason for staying clean..." },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (personalMotivation.isNotBlank()) PsText else TextMuted,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = { showMotivationEditDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit Motivation",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Milestone Badges Carousel
            Text(
                text = "PURITY MILESTONES & RANKS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(milestones) { milestone ->
                    val isUnlocked = streakDays >= milestone.dayThreshold
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isUnlocked) milestone.badgeColor.copy(alpha = 0.15f) else ShieldDarkBg,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isUnlocked) milestone.badgeColor.copy(alpha = 0.6f) else ShieldBorder
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = if (isUnlocked) Icons.Filled.EmojiEvents else Icons.Filled.Lock,
                                contentDescription = null,
                                tint = if (isUnlocked) milestone.badgeColor else TextMuted.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = milestone.title,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isUnlocked) PsText else TextMuted
                            )
                            Text(
                                text = milestone.rankTitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isUnlocked) milestone.badgeColor else TextMuted.copy(alpha = 0.6f),
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons Row: Daily Check-In, Set Goal, Reset
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showCheckInDialog = true },
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("daily_checkin_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ShieldPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = PsOnAccent
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Daily Check-In",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = PsOnAccent
                    )
                }

                OutlinedButton(
                    onClick = { showGoalDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("set_goal_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ShieldPrimaryLight),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ShieldPrimary.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Filled.Flag,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Goal",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = { showResetDialog = true },
                    modifier = Modifier
                        .weight(0.9f)
                        .testTag("reset_streak_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ShieldCrimsonDanger),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ShieldCrimsonDanger.copy(alpha = 0.4f))
                ) {
                    Icon(
                        imageVector = Icons.Filled.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Reset",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Recent Check-In History Collapse / Expand
            if (checkIns.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showHistoryExpanded = !showHistoryExpanded },
                    color = ShieldDarkBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ShieldBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.History,
                                    contentDescription = null,
                                    tint = ShieldPrimaryLight,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Accountability Check-In Log (${checkIns.size})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PsText
                                )
                            }

                            Text(
                                text = if (showHistoryExpanded) "Hide" else "View All",
                                style = MaterialTheme.typography.labelSmall,
                                color = ShieldPrimaryLight,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        AnimatedVisibility(
                            visible = showHistoryExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                checkIns.take(5).forEach { entry ->
                                    val dateStr = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(entry.timestamp))
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        color = ShieldDarkCard,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, ShieldBorder.copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = "Day ${entry.dayNumber} • ${entry.mood}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = ShieldEmeraldSafe
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "Urge: ${entry.urgeLevel}/5",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (entry.urgeLevel >= 4) ShieldCrimsonDanger else ShieldAmberWarning
                                                    )
                                                }
                                                if (entry.reflectionNote.isNotBlank()) {
                                                    Text(
                                                        text = entry.reflectionNote,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = PsText,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                            Text(
                                                text = dateStr,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextMuted,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // AI Daily Encouragement Notification Controller
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = ShieldDarkBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, ShieldBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(ShieldTertiary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.NotificationsActive,
                                    contentDescription = null,
                                    tint = ShieldTertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "AI Daily Encouraging Affirmations",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PsText
                                )
                                Text(
                                    text = "Sends streak reinforcement and resilience reminders",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                        }

                        Switch(
                            checked = aiEncouragementEnabled,
                            onCheckedChange = onToggleAiEncouragement,
                            modifier = Modifier.testTag("ai_encouragement_toggle"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ShieldPrimary,
                                checkedTrackColor = ShieldDarkSurfaceVariant,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = ShieldDarkBg
                            )
                        )
                    }

                    if (aiEncouragementEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onSendTestMotivation,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("send_encouragement_button"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ShieldTertiary.copy(alpha = 0.2f),
                                contentColor = ShieldPrimaryLight
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.VolunteerActivism,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Send Instant AI Motivation Notification",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
        }
    }

    // Daily Check-In Dialog
    if (showCheckInDialog) {
        var selectedMood by remember { mutableStateOf("Victorious") }
        var urgeSlider by remember { mutableFloatStateOf(1f) }
        var reflectionText by remember { mutableStateOf("") }
        val moods = listOf("Victorious", "Energized", "Calm", "Focused", "Struggling")

        AlertDialog(
            onDismissRequest = { showCheckInDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = ShieldEmeraldSafe,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Daily Clean Check-In",
                        fontWeight = FontWeight.Bold,
                        color = PsText
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Log Day ${streakDays + 1} accountability in Room database:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Current State & Mood:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = PsText
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        moods.forEach { mood ->
                            Surface(
                                onClick = { selectedMood = mood },
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedMood == mood) ShieldPrimary else ShieldDarkBg,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (selectedMood == mood) ShieldPrimary else ShieldBorder
                                )
                            ) {
                                Text(
                                    text = mood,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedMood == mood) PsOnAccent else PsText
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Urge Level:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = PsText
                        )
                        Text(
                            text = "${urgeSlider.toInt()} / 5 (${if (urgeSlider.toInt() <= 2) "Low" else if (urgeSlider.toInt() <= 3) "Moderate" else "Intense"})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (urgeSlider.toInt() >= 4) ShieldCrimsonDanger else ShieldAmberWarning
                        )
                    }

                    Slider(
                        value = urgeSlider,
                        onValueChange = { urgeSlider = it },
                        valueRange = 1f..5f,
                        steps = 3,
                        colors = SliderDefaults.colors(
                            thumbColor = ShieldPrimary,
                            activeTrackColor = ShieldPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = reflectionText,
                        onValueChange = { reflectionText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Reflection Notes (Optional)") },
                        placeholder = { Text("How did you overcome temptations today?") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ShieldPrimary,
                            unfocusedBorderColor = ShieldBorder,
                            focusedTextColor = PsText,
                            unfocusedTextColor = PsText
                        ),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRecordCheckIn(urgeSlider.toInt(), reflectionText.trim(), selectedMood)
                        showCheckInDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ShieldPrimary)
                ) {
                    Text("Save Check-In", fontWeight = FontWeight.Bold, color = PsOnAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCheckInDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = ShieldDarkCard
        )
    }

    // Set Goal & Personal Motivation Dialog
    if (showGoalDialog) {
        val presetGoals = listOf(3, 7, 14, 30, 60, 90, 180, 365)
        var selectedGoal by remember { mutableIntStateOf(goalDays) }
        var motivationInput by remember { mutableStateOf(personalMotivation) }

        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            title = {
                Text(
                    text = "Set Purity Goal & Milestones",
                    fontWeight = FontWeight.Bold,
                    color = PsText
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Choose your next target clean streak milestone:",
                        color = TextMuted,
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    presetGoals.chunked(4).forEach { rowGoals ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowGoals.forEach { target ->
                                Surface(
                                    onClick = { selectedGoal = target },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (selectedGoal == target) ShieldPrimary else ShieldDarkBg,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (selectedGoal == target) ShieldPrimary else ShieldBorder
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "$target d",
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedGoal == target) PsOnAccent else PsText,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = motivationInput,
                        onValueChange = { motivationInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Core Reason / Personal Goal") },
                        placeholder = { Text("Why is this goal important to you?") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ShieldPrimary,
                            unfocusedBorderColor = ShieldBorder,
                            focusedTextColor = PsText,
                            unfocusedTextColor = PsText
                        ),
                        maxLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSetGoal(selectedGoal, motivationInput.trim())
                        showGoalDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ShieldPrimary)
                ) {
                    Text("Save Target", fontWeight = FontWeight.Bold, color = PsOnAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoalDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = ShieldDarkCard
        )
    }

    // Edit Motivation Statement Dialog
    if (showMotivationEditDialog) {
        var statementText by remember { mutableStateOf(personalMotivation) }

        AlertDialog(
            onDismissRequest = { showMotivationEditDialog = false },
            title = {
                Text(
                    text = "Edit Personal Purpose",
                    fontWeight = FontWeight.Bold,
                    color = PsText
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Write your declaration for quitting adult content and living with clarity:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = statementText,
                        onValueChange = { statementText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Personal Purpose") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ShieldPrimary,
                            unfocusedBorderColor = ShieldBorder,
                            focusedTextColor = PsText,
                            unfocusedTextColor = PsText
                        ),
                        minLines = 2,
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateMotivation(statementText.trim())
                        showMotivationEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ShieldPrimary)
                ) {
                    Text("Update", fontWeight = FontWeight.Bold, color = PsOnAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMotivationEditDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = ShieldDarkCard
        )
    }

    // Reset Confirmation Dialog with Reason Tracking
    if (showResetDialog) {
        var relapseReason by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(
                    text = "Reset Clean Streak?",
                    fontWeight = FontWeight.Bold,
                    color = PsText
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Resetting records the previous streak ($streakDays days) to your Room history and starts Day 0 anew. Remember: relapse is not failure if you learn from the trigger.",
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = relapseReason,
                        onValueChange = { relapseReason = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("What caused the trigger? (Optional)") },
                        placeholder = { Text("e.g. Late night browsing, stress, fatigue") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ShieldCrimsonDanger,
                            unfocusedBorderColor = ShieldBorder,
                            focusedTextColor = PsText,
                            unfocusedTextColor = PsText
                        ),
                        maxLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetStreak(relapseReason.trim())
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ShieldCrimsonDanger)
                ) {
                    Text("Reset Streak", fontWeight = FontWeight.Bold, color = PsOnDanger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = ShieldDarkCard
        )
    }
}

@Composable
fun StreakMetricBox(
    label: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = ShieldDarkBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, ShieldBorder)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = accentColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = PsText,
                fontSize = 9.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                fontSize = 8.sp,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}
