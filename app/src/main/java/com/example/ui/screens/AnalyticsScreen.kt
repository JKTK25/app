package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.unit.sp
import com.example.data.model.BlockedIncident
import com.example.data.model.ThreatCategory
import com.example.ui.components.AiParentalReportCard
import com.example.ui.components.AnalyticsOverviewCards
import com.example.ui.components.CategoryBreakdownCard
import com.example.ui.components.WeeklyTrendChartCard
import com.example.ui.theme.PsDanger
import com.example.ui.theme.PsOnAccent
import com.example.ui.theme.PsText
import com.example.ui.theme.PsTextMuted
import com.example.ui.viewmodel.ShieldViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: ShieldViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val analytics by viewModel.analytics.collectAsState()
    val allIncidents by viewModel.allIncidents.collectAsState()

    var selectedCategoryFilter by remember { mutableStateOf<ThreatCategory?>(null) }
    var showClearDialog by remember { mutableStateOf(false) }

    val filteredIncidents = remember(allIncidents, selectedCategoryFilter) {
        if (selectedCategoryFilter == null) {
            allIncidents
        } else {
            allIncidents.filter { it.category == selectedCategoryFilter }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Analytics",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        color = PsText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (allIncidents.isNotEmpty()) {
                    IconButton(
                        onClick = { showClearDialog = true },
                        modifier = Modifier.testTag("clear_logs_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteSweep,
                            contentDescription = "Clear All Logs",
                            tint = PsDanger
                        )
                    }
                }
            }
        }

        // 4 KPI Summary Cards
        item {
            AnalyticsOverviewCards(analytics = analytics)
        }

        // Gemini AI Parental Intelligence & Semantic Audit
        item {
            AiParentalReportCard(
                report = uiState.aiParentalReport,
                isLoading = uiState.isAiReportGenerating,
                onGenerateReport = { viewModel.generateAiParentalReport() },
                onApplySuggestedRules = { rules -> viewModel.applyAiRecommendedRules(rules) }
            )
        }

        // Peak Danger Hours Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccessTime,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PEAK WINDOW",
                            style = MaterialTheme.typography.labelSmall,
                            color = PsTextMuted,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = analytics.peakHour,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PsText
                        )
                    }
                }
            }
        }

        // 7-day trend chart
        item {
            WeeklyTrendChartCard(weeklyTrend = analytics.weeklyTrend)
        }

        // Category breakdown progress bars
        item {
            CategoryBreakdownCard(categoryStats = analytics.categoryBreakdown)
        }

        // Filterable Incident Logs Header
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AUDIT LOG (${filteredIncidents.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PsText
                    )
                    if (selectedCategoryFilter != null) {
                        TextButton(onClick = { selectedCategoryFilter = null }) {
                            Text("CLEAR", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Filter chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThreatCategory.entries.take(3).forEach { cat ->
                        val isSelected = selectedCategoryFilter == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedCategoryFilter = if (isSelected) null else cat
                            },
                            label = {
                                Text(
                                    text = cat.displayName,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = PsTextMuted
                            )
                        )
                    }
                }
            }
        }

        if (filteredIncidents.isEmpty()) {
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
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "No incidents recorded.",
                            style = MaterialTheme.typography.bodySmall,
                            color = PsTextMuted
                        )
                    }
                }
            }
        } else {
            items(filteredIncidents) { incident ->
                DetailedIncidentCard(
                    incident = incident,
                    onDelete = { viewModel.deleteIncident(incident.id) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear all logs?", color = PsText) },
            text = { Text("Erase all recorded threat and unlock logs.", color = PsTextMuted) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllLogs()
                        showClearDialog = false
                    }
                ) {
                    Text("Clear", color = PsDanger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = PsTextMuted)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
fun DetailedIncidentCard(
    incident: BlockedIncident,
    onDelete: () -> Unit
) {
    val timeFormat = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(incident.timestamp))

    // Sensitive content rule: shows blocked threats only as "Explicit keyword blocked" and never prints the searched text
    val isKeywordOrSearch = incident.category == ThreatCategory.EXPLICIT_KEYWORD ||
            incident.category == ThreatCategory.EXPLICIT_SEARCH ||
            incident.triggerReason.contains("Keyword", ignoreCase = true) ||
            incident.triggerReason.contains("Explicit", ignoreCase = true) ||
            incident.triggerReason.contains("Search", ignoreCase = true)

    val displayTarget = if (isKeywordOrSearch) "Explicit keyword blocked" else incident.target

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = PsDanger.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = incident.category.displayName.uppercase(),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = PsDanger,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = PsTextMuted
                    )
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete",
                            tint = PsTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = displayTarget,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = PsText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${incident.triggerReason} • ${incident.source.name}",
                style = MaterialTheme.typography.bodySmall,
                color = PsTextMuted,
                fontSize = 11.sp
            )
        }
    }
}
