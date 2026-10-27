package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ThreatCategory
import com.example.data.repository.CategoryStat
import com.example.data.repository.DailyBlockStat
import com.example.data.repository.ShieldAnalytics
import com.example.ui.theme.ShieldAmberWarning
import com.example.ui.theme.ShieldBorder
import com.example.ui.theme.ShieldCrimsonDanger
import com.example.ui.theme.ShieldDarkCard
import com.example.ui.theme.ShieldDarkSurfaceVariant
import com.example.ui.theme.ShieldEmeraldSafe
import com.example.ui.theme.ShieldPrimary
import com.example.ui.theme.ShieldPrimaryDark
import com.example.ui.theme.ShieldTertiary
import com.example.ui.theme.PsText
import androidx.compose.ui.graphics.lerp
import com.example.ui.theme.PsDangerStrong
import com.example.ui.theme.ShieldAlt

@Composable
fun AnalyticsOverviewCards(
    analytics: ShieldAnalytics,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "BLOCKED TODAY",
                value = "${analytics.blockedToday}",
                subtitle = "Active real-time blocks",
                icon = Icons.Filled.Shield,
                accentColor = ShieldCrimsonDanger,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "THIS WEEK",
                value = "${analytics.blockedThisWeek}",
                subtitle = "Total intercepted",
                icon = Icons.Filled.Timeline,
                accentColor = ShieldPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "ALL-TIME BLOCKS",
                value = "${analytics.totalBlocked}",
                subtitle = "Threats deflected",
                icon = Icons.Filled.BarChart,
                accentColor = ShieldEmeraldSafe,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "UNLOCK AUDITS",
                value = "${analytics.unlockScansCount}",
                subtitle = "Screen unlock scans",
                icon = Icons.Filled.LockClock,
                accentColor = ShieldAmberWarning,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ShieldDarkCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, ShieldBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = PsText
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun WeeklyTrendChartCard(
    weeklyTrend: List<DailyBlockStat>,
    modifier: Modifier = Modifier
) {
    val maxCount = (weeklyTrend.maxOfOrNull { it.count } ?: 1).coerceAtLeast(1)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ShieldDarkCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, ShieldBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "7-DAY THREAT DEFENSE TREND",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PsText
                    )
                    Text(
                        text = "Aggressive pornographic link interceptions per day",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ShieldPrimary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "DAILY STATS",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = ShieldPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Bars Chart Container
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                weeklyTrend.forEach { stat ->
                    val fraction = (stat.count.toFloat() / maxCount.toFloat()).coerceIn(0.08f, 1f)
                    val animatedHeight by animateFloatAsState(
                        targetValue = fraction,
                        animationSpec = tween(durationMillis = 600),
                        label = "BarHeight"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(36.dp)
                    ) {
                        Text(
                            text = "${stat.count}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (stat.count > 0) ShieldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .height((100 * animatedHeight).dp)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    if (stat.count > 0) {
                                        Brush.verticalGradient(
                                            listOf(ShieldPrimary, ShieldPrimaryDark)
                                        )
                                    } else {
                                        Brush.verticalGradient(
                                            listOf(ShieldDarkSurfaceVariant, ShieldDarkSurfaceVariant.copy(alpha = 0.5f))
                                        )
                                    }
                                )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = stat.dayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryBreakdownCard(
    categoryStats: List<CategoryStat>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ShieldDarkCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, ShieldBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "THREAT CATEGORY DISTRIBUTION",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PsText
                )
                Icon(
                    imageVector = Icons.Filled.PieChart,
                    contentDescription = null,
                    tint = ShieldPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (categoryStats.isEmpty() || categoryStats.all { it.count == 0 }) {
                Text(
                    text = "No threats recorded yet. PureShield is armed and monitoring all outbound traffic.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                categoryStats.forEach { stat ->
                    val color = when (stat.category) {
                        ThreatCategory.ADULT_WEBSITE -> ShieldCrimsonDanger
                        ThreatCategory.EXPLICIT_SEARCH -> ShieldPrimary
                        ThreatCategory.RISKY_REDIRECT -> ShieldAmberWarning
                        ThreatCategory.ADULT_AD -> ShieldTertiary
                        ThreatCategory.EXPLICIT_KEYWORD -> lerp(ShieldPrimary, PsText, 0.5f)
                        ThreatCategory.P2P_NETWORK -> lerp(ShieldAmberWarning, ShieldCrimsonDanger, 0.5f)
                        ThreatCategory.AI_SEMANTIC_THREAT -> ShieldAlt
                        ThreatCategory.BYPASS_ATTEMPT -> PsDangerStrong
                        else -> ShieldPrimary
                    }

                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stat.category.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = PsText
                            )
                            Text(
                                text = "${stat.count} (${(stat.percentage * 100).toInt()}%)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = color
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        LinearProgressIndicator(
                            progress = { stat.percentage.coerceIn(0.02f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = color,
                            trackColor = ShieldDarkSurfaceVariant,
                            strokeCap = StrokeCap.Round
                        )
                    }
                }
            }
        }
    }
}
