package com.example.ui.components

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.PsTheme
import com.example.ThemeManager
import com.example.ui.theme.PsBorderStrong
import com.example.ui.theme.PsOnAccent
import com.example.ui.theme.PsText
import com.example.ui.theme.PsTextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemesBottomSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val context = LocalContext.current
    var selectedTheme by remember { mutableStateOf(ThemeManager.active) }

    FloatingPanel(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
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
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ThemeManager.active.composeAccentContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Palette,
                        contentDescription = null,
                        tint = ThemeManager.active.composeAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "PureShield Themes",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = PsText
                    )
                    Text(
                        text = "Tap any theme for instant live preview",
                        fontSize = 13.sp,
                        color = PsTextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Interactive Live Theme Preview Showcase Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("active_theme_showcase"),
                shape = RoundedCornerShape(18.dp),
                color = selectedTheme.composeSurface,
                border = BorderStroke(1.5.dp, selectedTheme.composeAccent)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(selectedTheme.composeAccentContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Shield,
                                    contentDescription = null,
                                    tint = selectedTheme.composeAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = selectedTheme.label,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = PsText
                                )
                                Text(
                                    text = "Live preview active across entire app",
                                    fontSize = 11.sp,
                                    color = selectedTheme.composeAccent
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = selectedTheme.composeAccentContainer,
                            border = BorderStroke(1.dp, selectedTheme.composeAccent.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "ACTIVE",
                                color = selectedTheme.composeAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Palette color swatches strip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PaletteSwatchChip("Accent", selectedTheme.composeAccent)
                        PaletteSwatchChip("Container", selectedTheme.composeAccentContainer)
                        PaletteSwatchChip("Surface", selectedTheme.composeSurface)
                        PaletteSwatchChip("Canvas", selectedTheme.composeBg)
                        PaletteSwatchChip("Border", selectedTheme.composeBorder)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2-Column Theme Grid
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val themeList = PsTheme.entries
                for (rowStart in 0 until themeList.size step 2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (i in 0 until 2) {
                            val index = rowStart + i
                            if (index < themeList.size) {
                                val theme = themeList[index]
                                ThemePreviewTile(
                                    theme = theme,
                                    isSelected = selectedTheme == theme,
                                    isCurrentlyApplied = ThemeManager.active == theme,
                                    onClick = {
                                        selectedTheme = theme
                                        // Live re-skin instantly on tap so the change is 100% visible immediately in preview
                                        val activity = context as? Activity
                                        if (activity != null) {
                                            ThemeManager.set(activity, theme)
                                        } else {
                                            ThemeManager.setDirect(context, theme)
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Done / Apply Theme Button
            Button(
                onClick = {
                    val activity = context as? Activity
                    if (activity != null) {
                        ThemeManager.set(activity, selectedTheme)
                    } else {
                        ThemeManager.setDirect(context, selectedTheme)
                    }
                    onDismissRequest()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("apply_theme_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = selectedTheme.composeAccent,
                    contentColor = PsOnAccent
                )
            ) {
                Text(
                    text = "Done • ${selectedTheme.label} Active",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = PsOnAccent
                )
            }
        }
    }
}

@Composable
fun ThemePreviewTile(
    theme: PsTheme,
    isSelected: Boolean,
    isCurrentlyApplied: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = theme.composeSurface,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) theme.composeAccent else theme.composeBorder
        ),
        modifier = modifier.testTag("theme_tile_${theme.name.lowercase()}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Accent Color Top Strip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(theme.composeAccent)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Theme Mini Window Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(theme.composeBg)
                    .border(BorderStroke(1.dp, if (isSelected) theme.composeAccent.copy(alpha = 0.5f) else theme.composeBorder), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Mini surface card with shield
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.86f)
                        .height(44.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = theme.composeSurface,
                    border = BorderStroke(1.dp, theme.composeBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(theme.composeAccentContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Shield,
                                    contentDescription = null,
                                    tint = theme.composeAccent,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(verticalArrangement = Arrangement.Center) {
                                Box(
                                    modifier = Modifier
                                        .width(24.dp)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(theme.composeAccent)
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Box(
                                    modifier = Modifier
                                        .width(16.dp)
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(1.5.dp))
                                        .background(PsTextMuted.copy(alpha = 0.4f))
                                )
                            }
                        }

                        // Mini Accent Dot
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(theme.composeAccent)
                        )
                    }
                }

                // Checkmark badge when selected/active
                if (isSelected || isCurrentlyApplied) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(5.dp)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(theme.composeAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = PsOnAccent,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Label and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = theme.label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = PsText,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )

                // Mini Color Accent dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(theme.composeAccent)
                )
            }

            Text(
                text = when {
                    isCurrentlyApplied -> "Active"
                    isSelected -> "Previewing"
                    else -> "Tap to preview"
                },
                fontSize = 11.sp,
                color = if (isCurrentlyApplied || isSelected) theme.composeAccent else PsTextMuted,
                fontWeight = if (isCurrentlyApplied || isSelected) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun PaletteSwatchChip(label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(color)
                .border(BorderStroke(1.dp, PsBorderStrong), CircleShape)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            color = PsTextMuted,
            fontWeight = FontWeight.Medium
        )
    }
}
