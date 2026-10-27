package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.model.InterceptSource
import com.example.ui.components.BlockedThreatModal
import com.example.ui.components.OnboardingSetupWizard
import com.example.ui.components.PinLockDialog
import com.example.ui.components.ThemesBottomSheet
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MonitorScreen
import com.example.ui.screens.RulesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ShieldCrimsonDanger
import com.example.ui.theme.ShieldDarkBg
import com.example.ui.theme.ShieldDarkCard
import com.example.ui.theme.ShieldDarkSurfaceVariant
import com.example.ui.theme.ShieldEmeraldSafe
import com.example.ui.theme.ShieldPrimary
import com.example.ui.theme.ShieldPrimaryLight
import com.example.ui.viewmodel.ShieldViewModel

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Home : Screen("home", "Shield", Icons.Filled.Shield)
    object Monitor : Screen("monitor", "Monitor", Icons.Filled.MonitorHeart)
    object Analytics : Screen("analytics", "Analytics", Icons.Filled.Analytics)
    object Rules : Screen("rules", "Rules", Icons.Filled.Rule)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings)
}

class MainActivity : ComponentActivity() {

    private val viewModel: ShieldViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permission result handled */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.apply(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request POST_NOTIFICATIONS on Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } catch (_: Exception) {}
        }

        // Ensure 24/7 background guardian service is active
        try {
            com.example.service.PureShieldForegroundService.startService(this)
        } catch (_: Exception) {}

        // Handle auto-open on unlock trigger
        if (intent?.getStringExtra("LAUNCH_TRIGGER") == "UNLOCK_WATCHDOG") {
            viewModel.recordUnlockAudit("Watchdog auto-launch executed upon phone unlock")
        }

        setContent {
            MyApplicationTheme {
                PureShieldApp(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            com.example.service.PureShieldForegroundService.startService(this)
        } catch (_: Exception) {}
        viewModel.refreshAccessibilityStatus()
        viewModel.refreshDeviceAdminStatus()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getStringExtra("LAUNCH_TRIGGER") == "UNLOCK_WATCHDOG") {
            viewModel.recordUnlockAudit("Watchdog auto-launch executed upon phone unlock")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PureShieldApp(viewModel: ShieldViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route

    val uiState by viewModel.uiState.collectAsState()

    val navItems = remember {
        listOf(
            Screen.Home,
            Screen.Monitor,
            Screen.Analytics,
            Screen.Rules,
            Screen.Settings
        )
    }

    var isThemesSheetOpen by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PURESHIELD",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    val activeColor = MaterialTheme.colorScheme.primary
                    val pausedColor = MaterialTheme.colorScheme.error

                    // Quick Theme switcher button
                    IconButton(
                        onClick = { isThemesSheetOpen = true },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("quick_theme_palette_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Palette,
                            contentDescription = "Themes",
                            tint = activeColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(2.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (uiState.isShieldActive) activeColor.copy(alpha = 0.15f) else pausedColor.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (uiState.isShieldActive) activeColor.copy(alpha = 0.4f) else pausedColor.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (uiState.isShieldActive) activeColor else pausedColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (uiState.isShieldActive) "BLOCKING" else "PAUSED",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.isShieldActive) activeColor else pausedColor,
                                fontSize = 10.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_navigation_bar")
            ) {
                navItems.forEach { screen ->
                    val isSelected = currentRoute == screen.route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(Screen.Home.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 10.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_item_${screen.route}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        viewModel = viewModel,
                        onNavigateToMonitor = { navController.navigate(Screen.Monitor.route) }
                    )
                }
                composable(Screen.Monitor.route) {
                    MonitorScreen(viewModel = viewModel)
                }
                composable(Screen.Analytics.route) {
                    AnalyticsScreen(viewModel = viewModel)
                }
                composable(Screen.Rules.route) {
                    RulesScreen(viewModel = viewModel)
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(viewModel = viewModel)
                }
            }

            // High-priority Alert Modal overlay when an adult threat is intercepted
            uiState.activeThreatModal?.let { incident ->
                BlockedThreatModal(
                    incident = incident,
                    onDismiss = { viewModel.dismissThreatModal() }
                )
            }

            // Mandatory Master Password & Permissions Setup on First Install
            if (uiState.isFirstLaunchPinSetupRequired) {
                OnboardingSetupWizard(
                    isAccessibilityActive = uiState.isAccessibilityServiceActive,
                    onCompleteSetup = { pin ->
                        viewModel.setupMasterPassword(pin)
                    }
                )
            }

            // PIN Lock / Protection Dialog for Pausing or Sensitive Settings Changes
            if (uiState.isPinDialogOpen) {
                PinLockDialog(
                    purposeDescription = uiState.pinDialogPurpose,
                    onPinSuccess = { enteredPin ->
                        viewModel.submitPinVerification(enteredPin)
                    },
                    onDismiss = {
                        viewModel.dismissPinDialog()
                    }
                )
            }

            // Interactive Themes bottom sheet
            if (isThemesSheetOpen) {
                ThemesBottomSheet(
                    onDismissRequest = { isThemesSheetOpen = false }
                )
            }
        }
    }
}
