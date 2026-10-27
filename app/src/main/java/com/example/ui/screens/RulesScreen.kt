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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.BorderStroke
import com.example.data.model.CustomRule
import com.example.data.model.RuleCategory
import com.example.data.model.RuleType
import com.example.ui.theme.PsDanger
import com.example.ui.theme.PsOnAccent
import com.example.ui.theme.PsText
import com.example.ui.theme.PsTextMuted
import com.example.ui.viewmodel.ShieldViewModel

@Composable
fun RulesScreen(
    viewModel: ShieldViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val customRules by viewModel.allCustomRules.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }

    val blockedRules = remember(customRules) { customRules.filter { it.ruleType == RuleType.BLOCK } }
    val allowedRules = remember(customRules) { customRules.filter { it.ruleType == RuleType.ALLOW } }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = PsOnAccent,
                shape = RoundedCornerShape(14.dp), // PS.Fab
                modifier = Modifier.testTag("add_rule_fab")
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Add Rule")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                            text = "Rules",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp,
                            color = PsText
                        )
                        Text(
                            text = "${blockedRules.size} blocked • ${allowedRules.size} allowed",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 13.sp,
                            color = PsTextMuted
                        )
                    }

                    Button(
                        onClick = { showAddDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = PsOnAccent
                        ),
                        modifier = Modifier.testTag("header_add_rule_button")
                    ) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            if (uiState.pinProtectionEnabled) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = "Locked",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "PIN Protected",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Tab bar for Blocked vs Allowed
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clip(RoundedCornerShape(14.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                text = "BLOCKED (${blockedRules.size})",
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == 0) PsDanger else PsTextMuted
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                text = "ALLOWED (${allowedRules.size})",
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == 1) MaterialTheme.colorScheme.primary else PsTextMuted
                            )
                        }
                    )
                }
            }

            val currentDisplayList = if (selectedTab == 0) blockedRules else allowedRules

            if (currentDisplayList.isEmpty()) {
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
                            Text(
                                text = "No rules added.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PsTextMuted
                            )
                        }
                    }
                }
            } else {
                items(currentDisplayList) { rule ->
                    CustomRuleItem(
                        rule = rule,
                        onToggle = { viewModel.toggleCustomRule(rule.id, it) },
                        onDelete = { viewModel.deleteCustomRule(rule.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    if (showAddDialog) {
        AddRuleDialog(
            defaultType = if (selectedTab == 0) RuleType.BLOCK else RuleType.ALLOW,
            onAddRule = { pattern, type, category ->
                viewModel.addCustomRule(pattern, type, category)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
fun BuiltInPackRow(name: String, count: String, isArmed: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Armed",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = PsText
                )
                Text(
                    text = count,
                    style = MaterialTheme.typography.labelSmall,
                    color = PsTextMuted
                )
            }
        }
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Text(
                text = "ACTIVE",
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp
            )
        }
    }
}

@Composable
fun CustomRuleItem(
    rule: CustomRule,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val isBlock = rule.ruleType == RuleType.BLOCK
    val accentColor = if (isBlock) PsDanger else MaterialTheme.colorScheme.primary

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
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isBlock) Icons.Filled.Block else Icons.Filled.Check,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rule.pattern,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = PsText
                )
                Text(
                    text = "${rule.category.displayName} • ${if (isBlock) "BLOCK" else "ALLOW"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = PsTextMuted
                )
            }

            Switch(
                checked = rule.isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = PsOnAccent,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = PsTextMuted,
                    uncheckedTrackColor = MaterialTheme.colorScheme.outline
                )
            )

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete",
                    tint = PsTextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun AddRuleDialog(
    defaultType: RuleType,
    onAddRule: (pattern: String, type: RuleType, category: RuleCategory) -> Unit,
    onDismiss: () -> Unit
) {
    var patternText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(defaultType) }
    var selectedCategory by remember { mutableStateOf(RuleCategory.DOMAIN) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Add Rule",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = PsText
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = patternText,
                    onValueChange = { patternText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_rule_input"),
                    placeholder = { Text("domain.com or keyword", color = PsTextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = PsText,
                        unfocusedTextColor = PsText
                    ),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Rule Type Selection
                Text(
                    text = "TYPE",
                    style = MaterialTheme.typography.labelSmall,
                    color = PsTextMuted,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = { selectedType = RuleType.BLOCK },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedType == RuleType.BLOCK) PsDanger.copy(alpha = 0.15f) else MaterialTheme.colorScheme.background,
                        border = BorderStroke(
                            1.dp,
                            if (selectedType == RuleType.BLOCK) PsDanger else MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "BLOCK",
                            modifier = Modifier.padding(10.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedType == RuleType.BLOCK) PsDanger else PsTextMuted
                        )
                    }

                    Surface(
                        onClick = { selectedType = RuleType.ALLOW },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedType == RuleType.ALLOW) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background,
                        border = BorderStroke(
                            1.dp,
                            if (selectedType == RuleType.ALLOW) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "ALLOW",
                            modifier = Modifier.padding(10.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedType == RuleType.ALLOW) MaterialTheme.colorScheme.primary else PsTextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Rule Category Selection
                Text(
                    text = "CATEGORY",
                    style = MaterialTheme.typography.labelSmall,
                    color = PsTextMuted,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RuleCategory.entries.forEach { cat ->
                        val isSelected = selectedCategory == cat
                        Surface(
                            onClick = { selectedCategory = cat },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = cat.displayName,
                                modifier = Modifier.padding(8.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else PsTextMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = PsTextMuted)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (patternText.isNotBlank()) {
                                onAddRule(patternText, selectedType, selectedCategory)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = PsOnAccent
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("save_rule_button")
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
