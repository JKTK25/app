package com.example.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainActivity
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ShieldBorder
import com.example.ui.theme.ShieldCrimsonDanger
import com.example.ui.theme.ShieldDarkBg
import com.example.ui.theme.ShieldDarkCard
import com.example.ui.theme.ShieldDarkSurfaceVariant
import com.example.ui.theme.ShieldPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import androidx.lifecycle.lifecycleScope
import com.example.PureShieldApplication
import com.example.data.model.CustomRule
import com.example.data.model.RuleCategory
import com.example.data.model.RuleType
import com.example.service.PureShieldAccessibilityService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.ui.theme.PsText

class ParentalBlockActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        com.example.ThemeManager.apply(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val target = intent.getStringExtra("BLOCKED_TARGET") ?: "Explicit Adult Content"
        val category = intent.getStringExtra("BLOCKED_CATEGORY") ?: "Adult Website"
        val reason = intent.getStringExtra("BLOCKED_REASON") ?: "Matched Strict Parental Defense Filter"
        val browserName = intent.getStringExtra("BROWSER_NAME") ?: "External Browser"

        val prefs = getSharedPreferences("pureshield_prefs", Context.MODE_PRIVATE)
        val isPornIntentFlag = intent.getBooleanExtra("IS_PORNOGRAPHY_THREAT", false)
        val isPornEvaluated = com.example.engine.BlockEngine.isPornographyOrAdultThreat(target = target, reason = reason)
        val isPornographyThreat = isPornIntentFlag || isPornEvaluated
        val strictPornPolicy = prefs.getBoolean("strict_pornography_password_policy", true)
        val allowParentalOverride = intent.getBooleanExtra("ALLOW_PARENTAL_OVERRIDE", true) &&
                prefs.getBoolean("allow_parental_override", true) &&
                (!isPornographyThreat || !strictPornPolicy)

        setContent {
            MyApplicationTheme {
                ParentalBlockScreen(
                    target = target,
                    category = category,
                    reason = reason,
                    browserName = browserName,
                    allowParentalOverride = allowParentalOverride,
                    isPornographyThreat = isPornographyThreat,
                    strictPornPolicy = strictPornPolicy,
                    onDismiss = {
                        val homeIntent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        startActivity(homeIntent)
                        finish()
                    },
                    onVerifyParentPin = { pin ->
                        if (isPornographyThreat && strictPornPolicy) {
                            false
                        } else {
                            val storedPin = prefs.getString("security_pin", "") ?: ""
                            storedPin.isNotBlank() && storedPin == pin
                        }
                    },
                    onAllowTemporary = {
                        PureShieldAccessibilityService.grantParentalOverride(target, 60)
                        PureShieldAccessibilityService.isAuthorizedTamperSession = true
                        finish()
                    },
                    onAllowPermanent = {
                        lifecycleScope.launch {
                            val app = application as? PureShieldApplication
                            app?.repository?.addCustomRule(
                                CustomRule(
                                    pattern = target.trim(),
                                    ruleType = RuleType.ALLOW,
                                    category = if (target.contains(".") && !target.contains(" ")) RuleCategory.DOMAIN else RuleCategory.KEYWORD
                                )
                            )
                            PureShieldAccessibilityService.grantParentalOverride(target, 1440)
                            PureShieldAccessibilityService.isAuthorizedTamperSession = true
                            finish()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ParentalBlockScreen(
    target: String,
    category: String,
    reason: String,
    browserName: String,
    allowParentalOverride: Boolean,
    isPornographyThreat: Boolean = false,
    strictPornPolicy: Boolean = true,
    onDismiss: () -> Unit,
    onVerifyParentPin: (String) -> Boolean,
    onAllowTemporary: () -> Unit,
    onAllowPermanent: () -> Unit
) {
    var showPinInput by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }
    var aiAuditError by remember { mutableStateOf(false) }
    var isPinVerified by remember { mutableStateOf(false) }
    val formattedTime = remember {
        SimpleDateFormat("MMM dd, yyyy • HH:mm:ss", Locale.getDefault()).format(Date())
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ShieldDarkBg
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Warning Shield Icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(ShieldCrimsonDanger.copy(alpha = 0.15f))
                        .border(2.dp, ShieldCrimsonDanger, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = "Restricted",
                        tint = ShieldCrimsonDanger,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Title & Subtitle
                Text(
                    text = "ACCESS RESTRICTED",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = ShieldCrimsonDanger,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "PureShield Strict Parental Control Intercept",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Threat Details Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ShieldDarkCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ShieldBorder)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Policy,
                                    contentDescription = null,
                                    tint = ShieldPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Interception Details",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = ShieldDarkSurfaceVariant
                            ) {
                                Text(
                                    text = browserName,
                                    fontSize = 11.sp,
                                    color = ShieldPrimary,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Target Box
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = ShieldDarkBg,
                            border = androidx.compose.foundation.BorderStroke(1.dp, ShieldDarkSurfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "BLOCKED TARGET",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMuted,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = target,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = PsText,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Category & Reason
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Category:", fontSize = 12.sp, color = TextSecondary)
                            Text(text = category, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ShieldCrimsonDanger)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Time:", fontSize = 12.sp, color = TextSecondary)
                            Text(text = formattedTime, fontSize = 12.sp, color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = reason,
                            fontSize = 11.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // PIN Unlock & Parental Override Section
                AnimatedVisibility(visible = showPinInput) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = ShieldDarkSurfaceVariant.copy(alpha = 0.5f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ShieldBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (!isPinVerified) {
                                Text(
                                    text = "Enter Master PIN to Unblock",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = enteredPin,
                                    onValueChange = {
                                        if (it.length <= 8) {
                                            enteredPin = it
                                            pinError = false
                                        }
                                    },
                                    label = { Text("Parent PIN") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    singleLine = true,
                                    isError = pinError,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("parent_pin_override_input"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ShieldPrimary,
                                        unfocusedBorderColor = ShieldDarkSurfaceVariant,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    )
                                )
                                if (pinError) {
                                    Text(
                                        text = "Incorrect Parent PIN",
                                        color = ShieldCrimsonDanger,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        val success = onVerifyParentPin(enteredPin)
                                        if (success) {
                                            isPinVerified = true
                                            pinError = false
                                        } else {
                                            pinError = true
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("submit_parent_override_button"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = ShieldPrimary)
                                ) {
                                    Text("Verify Parent PIN")
                                }
                            } else {
                                // Verified Options
                                Text(
                                    text = "PIN Verified • Allow Access",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ShieldPrimary
                                )
                                Text(
                                    text = "Choose how you want to allow this target:",
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                                )
                                Button(
                                    onClick = onAllowTemporary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("override_allow_1hr_button"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = ShieldPrimary)
                                ) {
                                    Text("Allow for 1 Hour (Temporary)")
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = onAllowPermanent,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("override_allow_permanent_button"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, ShieldPrimary.copy(alpha = 0.5f))
                                ) {
                                    Text("Always Allow (Add to Whitelist)")
                                }
                            }
                        }
                    }
                }

                // Primary Action Buttons
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("parent_block_return_safe_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isPinVerified) ShieldDarkSurfaceVariant else ShieldPrimary)
                ) {
                    Icon(imageVector = Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Return to PureShield", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (allowParentalOverride) {
                    OutlinedButton(
                        onClick = {
                            if (isPornographyThreat && strictPornPolicy) {
                                aiAuditError = true
                                showPinInput = false
                            } else if (category.contains("Adult Website", ignoreCase = true) || 
                                category.contains("Pornography", ignoreCase = true) ||
                                category.contains("Explicit", ignoreCase = true) ||
                                reason.contains("Verified Adult Domain", ignoreCase = true)) {
                                aiAuditError = true
                                showPinInput = false
                            } else {
                                showPinInput = !showPinInput
                                if (!showPinInput) {
                                    isPinVerified = false
                                    enteredPin = ""
                                    pinError = false
                                }
                                aiAuditError = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("toggle_parent_pin_override_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ShieldDarkSurfaceVariant)
                    ) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (showPinInput) "Close Override" else "Parent Override (PIN)", fontSize = 13.sp)
                    }

                    AnimatedVisibility(visible = aiAuditError) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = ShieldCrimsonDanger.copy(alpha = 0.15f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ShieldCrimsonDanger.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Policy,
                                    contentDescription = null,
                                    tint = ShieldCrimsonDanger,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Strict Anti-Pornography Enforcement", fontWeight = FontWeight.Bold, color = ShieldCrimsonDanger, fontSize = 14.sp)
                                    Text("Overriding adult content or pornography is prohibited by strict security policy. Master Password bypass denied.", color = TextPrimary, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                } else if (isPornographyThreat && strictPornPolicy) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = ShieldCrimsonDanger.copy(alpha = 0.15f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ShieldCrimsonDanger.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = ShieldCrimsonDanger,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "ZERO-TOLERANCE PORNOGRAPHY LOCK",
                                    fontWeight = FontWeight.Bold,
                                    color = ShieldCrimsonDanger,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Strict Anti-Pornography Password Policy is active. Overrides and bypasses are completely disabled for explicit material.",
                                    color = TextPrimary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
