package com.example.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.ShieldBorder
import com.example.ui.theme.ShieldCrimsonDanger
import com.example.ui.theme.ShieldDarkBg
import com.example.ui.theme.ShieldDarkSurfaceVariant
import com.example.ui.theme.ShieldPrimary
import com.example.ui.theme.ShieldPrimaryLight
import com.example.ui.theme.PsText

@Composable
fun PinLockDialog(
    purposeDescription: String = "",
    isSettingNewPin: Boolean = false,
    onPinSuccess: (pin: String) -> Boolean,
    onDismiss: () -> Unit
) {
    var pinText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, ShieldPrimary.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                .testTag("pin_lock_dialog"),
            color = ShieldDarkBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = ShieldPrimary,
                    modifier = Modifier.size(36.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (isSettingNewPin) "SET MASTER PASSWORD" else "ENTER MASTER PASSWORD",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = PsText
                )

                Text(
                    text = if (purposeDescription.isNotBlank()) {
                        "Enter your Master Password to: $purposeDescription"
                    } else if (isSettingNewPin) {
                        "Create a Master PIN to lock pausing and uninstalling."
                    } else {
                        "Enter your Master Password to modify protection settings."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )

                if (isSettingNewPin) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        onClick = {
                            val randomPin = (100000 + java.security.SecureRandom().nextInt(900000)).toString()
                            pinText = randomPin
                            errorMessage = null
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = ShieldPrimary.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ShieldPrimary.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auto_assign_pin_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(imageVector = Icons.Filled.Security, contentDescription = null, tint = ShieldPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("⚡ Auto-Assign Secure 6-Digit PIN", color = ShieldPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = pinText,
                    onValueChange = {
                        if (it.length <= 8 && it.all { char -> char.isDigit() }) {
                            pinText = it
                            errorMessage = null
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pin_input_field"),
                    placeholder = { Text("4-6 digit PIN", fontSize = 13.sp) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (pinText.length >= 4) {
                                val ok = onPinSuccess(pinText)
                                if (!ok) {
                                    errorMessage = "Incorrect Master Password"
                                }
                            } else {
                                errorMessage = "Password must be at least 4 digits"
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ShieldPrimary,
                        unfocusedBorderColor = ShieldDarkSurfaceVariant,
                        focusedTextColor = PsText,
                        unfocusedTextColor = PsText
                    )
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = ShieldCrimsonDanger
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCEL", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (pinText.length >= 4) {
                                val ok = onPinSuccess(pinText)
                                if (!ok) {
                                    errorMessage = "Incorrect Master Password"
                                }
                            } else {
                                errorMessage = "Password must be at least 4 digits"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ShieldPrimary,
                            contentColor = ShieldDarkBg
                        ),
                        modifier = Modifier.testTag("submit_pin_button")
                    ) {
                        Text("CONFIRM", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SetupMasterPasswordDialog(
    onPasswordConfigured: (pin: String) -> Unit
) {
    var pinText by remember { mutableStateOf("") }
    var confirmPinText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = { /* Mandatory setup, cannot dismiss by tapping outside */ }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(20.dp))
                .border(2.dp, ShieldPrimary, RoundedCornerShape(20.dp))
                .testTag("setup_master_password_dialog"),
            color = ShieldDarkBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = null,
                    tint = ShieldPrimary,
                    modifier = Modifier.size(42.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "CREATE MASTER PASSWORD",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = PsText,
                    letterSpacing = 0.5.sp
                )

                Text(
                    text = "PureShield requires a Master Password immediately upon install. This password protects the app from being paused, disabled, or uninstalled.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    onClick = {
                        val randomPin = (100000 + java.security.SecureRandom().nextInt(900000)).toString()
                        pinText = randomPin
                        confirmPinText = randomPin
                        errorMessage = null
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = ShieldPrimary.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ShieldPrimary.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auto_assign_setup_pin_button")
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(imageVector = Icons.Filled.Security, contentDescription = null, tint = ShieldPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("⚡ Auto-Assign Secure 6-Digit PIN", color = ShieldPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = pinText,
                    onValueChange = {
                        if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                            pinText = it
                            errorMessage = null
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_master_pin_input"),
                    label = { Text("Master PIN (4-6 digits)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = confirmPinText,
                    onValueChange = {
                        if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                            confirmPinText = it
                            errorMessage = null
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("confirm_master_pin_input"),
                    label = { Text("Confirm Master PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
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

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = ShieldCrimsonDanger
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (pinText.length < 4) {
                            errorMessage = "PIN must be at least 4 digits."
                        } else if (pinText != confirmPinText) {
                            errorMessage = "PINs do not match."
                        } else {
                            onPasswordConfigured(pinText)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("confirm_master_password_setup_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ShieldPrimary,
                        contentColor = ShieldDarkBg
                    )
                ) {
                    Text("SET MASTER PASSWORD & ACTIVATE", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
