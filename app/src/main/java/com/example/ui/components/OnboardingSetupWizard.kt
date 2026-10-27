package com.example.ui.components

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.service.PureShieldAccessibilityService
import com.example.ui.theme.ShieldAmberWarning
import com.example.ui.theme.ShieldBorder
import com.example.ui.theme.ShieldCrimsonDanger
import com.example.ui.theme.ShieldDarkBg
import com.example.ui.theme.ShieldDarkCard
import com.example.ui.theme.ShieldDarkSurfaceVariant
import com.example.ui.theme.ShieldEmeraldSafe
import com.example.ui.theme.ShieldPrimary
import com.example.ui.theme.ShieldPrimaryLight
import com.example.ui.theme.PsOnAccent
import com.example.ui.theme.PsText

@Composable
fun OnboardingSetupWizard(
    isAccessibilityActive: Boolean,
    onCompleteSetup: (pin: String) -> Unit
) {
    val context = LocalContext.current
    var currentStep by remember { mutableStateOf(1) }
    var pinText by remember { mutableStateOf("") }
    var confirmPinText by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var notificationPermissionGranted by remember { mutableStateOf(false) }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            notificationPermissionGranted = isGranted
        }
    )

    Dialog(
        onDismissRequest = { /* Mandatory First-Time Setup */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(24.dp))
                .border(2.dp, ShieldPrimary, RoundedCornerShape(24.dp))
                .testTag("onboarding_setup_wizard"),
            color = ShieldDarkBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Badge & Title
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(ShieldPrimary.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = null,
                        tint = ShieldPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "PURESHIELD SETUP",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = PsText
                )

                Text(
                    text = "Quick setup to protect your device against adult content and malicious links.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Progress Step Indicator (2-Step Clean Flow)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StepChip(step = 1, label = "Master PIN", isCurrent = currentStep == 1, isCompleted = currentStep > 1)
                    StepChip(step = 2, label = "Permissions & Protection", isCurrent = currentStep == 2, isCompleted = false)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // STEP 1: CREATE MASTER PIN
                if (currentStep == 1) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = ShieldDarkCard),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ShieldBorder)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Lock, contentDescription = null, tint = ShieldPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Step 1: Set Master Security PIN",
                                    fontWeight = FontWeight.Bold,
                                    color = PsText
                                )
                            }
                            Text(
                                text = "Create a 4-6 digit PIN to protect your settings and custom filter rules.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                            )

                            Surface(
                                onClick = {
                                    val randomPin = (100000 + java.security.SecureRandom().nextInt(900000)).toString()
                                    pinText = randomPin
                                    confirmPinText = randomPin
                                    pinError = null
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = ShieldPrimary.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ShieldPrimary.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                                    .testTag("onboarding_auto_assign_pin_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = ShieldPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("⚡ Auto-Assign Secure 6-Digit PIN", color = ShieldPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            OutlinedTextField(
                                value = pinText,
                                onValueChange = {
                                    if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                                        pinText = it
                                        pinError = null
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("onboarding_pin_input"),
                                label = { Text("Enter 4-6 digit Master PIN") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ShieldPrimary,
                                    unfocusedBorderColor = ShieldDarkSurfaceVariant,
                                    focusedTextColor = PsText,
                                    unfocusedTextColor = PsText
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = confirmPinText,
                                onValueChange = {
                                    if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                                        confirmPinText = it
                                        pinError = null
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("onboarding_pin_confirm_input"),
                                label = { Text("Confirm Master PIN") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ShieldPrimary,
                                    unfocusedBorderColor = ShieldDarkSurfaceVariant,
                                    focusedTextColor = PsText,
                                    unfocusedTextColor = PsText
                                )
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Password is encrypted & completely concealed from user and bystanders",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (pinError != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = pinError ?: "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ShieldCrimsonDanger
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    if (pinText.length < 4) {
                                        pinError = "PIN must be at least 4 digits."
                                    } else if (pinText != confirmPinText) {
                                        pinError = "PINs do not match."
                                    } else {
                                        // Auto trigger notification permission request on Android 13+
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            try {
                                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            } catch (_: Exception) {}
                                        }
                                        currentStep = 2
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("onboarding_step1_next_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ShieldPrimary)
                            ) {
                                Text("CONTINUE TO PERMISSIONS", fontWeight = FontWeight.Bold, color = PsOnAccent)
                            }
                        }
                    }
                }

                // STEP 2: NOTIFICATIONS & CROSS-BROWSER ACCESSIBILITY (FINAL STEP)
                if (currentStep == 2) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = ShieldDarkCard),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ShieldBorder)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Notifications, contentDescription = null, tint = ShieldPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Step 2: Protection Permissions",
                                    fontWeight = FontWeight.Bold,
                                    color = PsText
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Notification Permission Button
                            PermissionItemCard(
                                icon = Icons.Filled.Notifications,
                                title = "1. Security Threat Notifications",
                                description = "Alerts you when explicit links, search terms, or popups are blocked.",
                                isGranted = notificationPermissionGranted || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU,
                                buttonLabel = "Allow Notifications",
                                onAction = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Accessibility Guardian Button
                            PermissionItemCard(
                                icon = Icons.Filled.AccessibilityNew,
                                title = "2. Cross-Browser Accessibility Protection",
                                description = "Allows PureShield to inspect and block adult URLs across Chrome, Firefox, Samsung Internet, Edge, and Opera.",
                                isGranted = isAccessibilityActive,
                                buttonLabel = "Open Accessibility Settings",
                                onAction = {
                                    PureShieldAccessibilityService.openAccessibilitySettings(context)
                                }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                OutlinedButton(
                                    onClick = { currentStep = 1 },
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, ShieldBorder)
                                ) {
                                    Text("BACK", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                Button(
                                    onClick = {
                                        onCompleteSetup(pinText)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 10.dp)
                                        .testTag("onboarding_complete_setup_button"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = ShieldEmeraldSafe)
                                ) {
                                    Text("COMPLETE SETUP & ENTER", fontWeight = FontWeight.Bold, color = PsOnAccent)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepChip(step: Int, label: String, isCurrent: Boolean, isCompleted: Boolean) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = when {
            isCompleted -> ShieldEmeraldSafe.copy(alpha = 0.2f)
            isCurrent -> ShieldPrimary.copy(alpha = 0.2f)
            else -> ShieldDarkSurfaceVariant
        },
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            when {
                isCompleted -> ShieldEmeraldSafe
                isCurrent -> ShieldPrimary
                else -> ShieldBorder
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isCompleted) "✓" else "$step",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = when {
                    isCompleted -> ShieldEmeraldSafe
                    isCurrent -> ShieldPrimaryLight
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                color = if (isCurrent) PsText else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PermissionItemCard(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    buttonLabel: String,
    onAction: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = ShieldDarkSurfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isGranted) ShieldEmeraldSafe.copy(alpha = 0.4f) else ShieldBorder
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isGranted) ShieldEmeraldSafe else ShieldAmberWarning,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = PsText
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isGranted) ShieldEmeraldSafe.copy(alpha = 0.15f) else ShieldAmberWarning.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (isGranted) "GRANTED" else "ACTION REQUIRED",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isGranted) ShieldEmeraldSafe else ShieldAmberWarning,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                }
            }

            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )

            if (!isGranted) {
                Button(
                    onClick = onAction,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ShieldPrimary)
                ) {
                    Text(
                        text = buttonLabel,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = PsOnAccent
                    )
                }
            }
        }
    }
}
