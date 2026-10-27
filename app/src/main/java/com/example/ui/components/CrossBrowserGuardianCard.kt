package com.example.ui.components

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.PureShieldAccessibilityService
import com.example.ui.theme.PsDanger
import com.example.ui.theme.PsOnAccent
import com.example.ui.theme.PsText
import com.example.ui.theme.PsTextMuted

@Composable
fun CrossBrowserGuardianCard(
    isAccessibilityActive: Boolean,
    isCrossBrowserEnabled: Boolean,
    isStrictParentalEnabled: Boolean,
    onToggleCrossBrowser: (Boolean) -> Unit,
    onToggleStrictParental: (Boolean) -> Unit,
    onRefreshStatus: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isEffectivelyProtecting = isAccessibilityActive && isCrossBrowserEnabled

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            if (isEffectivelyProtecting) MaterialTheme.colorScheme.outline else PsDanger.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isEffectivelyProtecting) MaterialTheme.colorScheme.primaryContainer
                                else PsDanger.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = if (isEffectivelyProtecting) MaterialTheme.colorScheme.primary else PsDanger,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Browser Guardian",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = PsText
                        )
                        Text(
                            text = "Multi-browser defense",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 13.sp,
                            color = PsTextMuted
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (isEffectivelyProtecting) MaterialTheme.colorScheme.primaryContainer else PsDanger.copy(alpha = 0.15f),
                    border = BorderStroke(
                        1.dp,
                        if (isEffectivelyProtecting) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else PsDanger.copy(alpha = 0.5f)
                    )
                ) {
                    Text(
                        text = if (isEffectivelyProtecting) "ACTIVE" else "SETUP",
                        color = if (isEffectivelyProtecting) MaterialTheme.colorScheme.primary else PsDanger,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            if (!isAccessibilityActive) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        PureShieldAccessibilityService.openAccessibilitySettings(context)
                        onRefreshStatus()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("activate_accessibility_guardian_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = PsOnAccent
                    )
                ) {
                    Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Grant Permission", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Toggles
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "URL Inspection",
                    fontWeight = FontWeight.Medium,
                    color = PsText,
                    fontSize = 14.sp
                )
                Switch(
                    checked = isCrossBrowserEnabled,
                    onCheckedChange = onToggleCrossBrowser,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PsOnAccent,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = PsTextMuted,
                        uncheckedTrackColor = MaterialTheme.colorScheme.outline
                    )
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Strict SafeSearch",
                    fontWeight = FontWeight.Medium,
                    color = PsText,
                    fontSize = 14.sp
                )
                Switch(
                    checked = isStrictParentalEnabled,
                    onCheckedChange = onToggleStrictParental,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PsOnAccent,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = PsTextMuted,
                        uncheckedTrackColor = MaterialTheme.colorScheme.outline
                    )
                )
            }
        }
    }
}
