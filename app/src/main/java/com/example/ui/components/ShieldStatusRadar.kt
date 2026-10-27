package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PsDanger
import com.example.ui.theme.PsText
import com.example.ui.theme.PsTextMuted
import com.example.ui.viewmodel.ProtectionMode

@Composable
fun ShieldStatusRadar(
    isActive: Boolean,
    mode: ProtectionMode,
    threatsBlockedCount: Int,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "RadarPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    val activeAccent = MaterialTheme.colorScheme.primary
    val activeContainer = MaterialTheme.colorScheme.primaryContainer
    val dangerAccent = MaterialTheme.colorScheme.error

    val activeColor = if (isActive) activeAccent else dangerAccent
    val fillColor = if (isActive) activeContainer else PsDanger.copy(alpha = 0.12f)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(190.dp)
                .testTag("shield_status_radar"),
            contentAlignment = Alignment.Center
        ) {
            // Pulse ring
            if (isActive) {
                Canvas(modifier = Modifier.size(190.dp)) {
                    val radius = (size.minDimension / 2f) * pulseScale
                    drawCircle(
                        color = activeColor.copy(alpha = pulseAlpha),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }

            // Outer flat ring (psAccentContainer fill, psAccent stroke)
            Surface(
                modifier = Modifier
                    .size(150.dp) // @dimen/ps_ring_size
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggle
                    )
                    .testTag("shield_toggle_button"),
                shape = CircleShape,
                color = fillColor,
                border = BorderStroke(2.dp, activeColor),
                shadowElevation = 0.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isActive) Icons.Filled.Shield else Icons.Filled.Warning,
                            contentDescription = if (isActive) "Shield Active" else "Shield Paused",
                            tint = activeColor,
                            modifier = Modifier.size(42.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isActive) "ARMED" else "PAUSED",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = activeColor,
                            letterSpacing = 1.2.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Status Pill (@drawable/bg_pill)
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isActive) activeAccent else dangerAccent)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isActive) mode.title.uppercase() else "PAUSED",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = PsText,
                    fontSize = 11.sp
                )
            }
        }
    }
}
