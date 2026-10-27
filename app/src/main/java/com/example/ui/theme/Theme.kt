package com.example.ui.theme

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.PsTheme
import com.example.ThemeManager

@Composable
fun MyApplicationTheme(
    theme: PsTheme? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        ThemeManager.current(context)
    }

    val activeTheme = theme ?: ThemeManager.active

    val customColors = PureShieldCustomColors(
        bg = activeTheme.composeBg,
        surface = activeTheme.composeSurface,
        border = activeTheme.composeBorder,
        accent = activeTheme.composeAccent,
        accentContainer = activeTheme.composeAccentContainer,
        text = PsText,
        textMuted = PsTextMuted,
        onAccent = PsOnAccent,
        danger = PsDanger,
        borderStrong = PsBorderStrong
    )

    val colorScheme = darkColorScheme(
        primary = activeTheme.composeAccent,
        onPrimary = PsOnAccent,
        primaryContainer = activeTheme.composeAccentContainer,
        onPrimaryContainer = activeTheme.composeAccent,
        secondary = activeTheme.composeAccent,
        onSecondary = PsOnAccent,
        secondaryContainer = activeTheme.composeAccentContainer,
        onSecondaryContainer = activeTheme.composeAccent,
        background = activeTheme.composeBg,
        onBackground = PsText,
        surface = activeTheme.composeSurface,
        onSurface = PsText,
        surfaceVariant = activeTheme.composeSurface,
        onSurfaceVariant = PsTextMuted,
        outline = activeTheme.composeBorder,
        outlineVariant = activeTheme.composeBorder,
        error = PsDanger,
        onError = PsOnDanger,
        errorContainer = lerp(activeTheme.composeBg, PsDanger, 0.15f),
        onErrorContainer = PsDanger,
        tertiary = activeTheme.composeAccent,
        onTertiary = PsOnAccent,
        tertiaryContainer = activeTheme.composeAccentContainer,
        onTertiaryContainer = activeTheme.composeAccent,
        surfaceDim = activeTheme.composeBg,
        surfaceBright = activeTheme.composeBorder,
        surfaceContainerLowest = activeTheme.composeBg,
        surfaceContainerLow = activeTheme.composeSurface,
        surfaceContainer = activeTheme.composeSurface,
        surfaceContainerHigh = activeTheme.composeSurface,
        surfaceContainerHighest = activeTheme.composeBorder,
        inverseSurface = PsText,
        inverseOnSurface = activeTheme.composeBg,
        inversePrimary = activeTheme.composeAccent
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.setBackgroundDrawable(ColorDrawable(activeTheme.composeBg.toArgb()))
                window.statusBarColor = activeTheme.composeBg.toArgb()
                window.navigationBarColor = activeTheme.composeBg.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = false
                controller.isAppearanceLightNavigationBars = false
            }
        }
    }

    CompositionLocalProvider(
        LocalPureShieldColors provides customColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

@Composable
fun PureShieldTheme(
    theme: PsTheme? = null,
    content: @Composable () -> Unit
) = MyApplicationTheme(theme = theme, content = content)
