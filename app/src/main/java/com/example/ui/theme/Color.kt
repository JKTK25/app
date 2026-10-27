package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import com.example.ThemeManager

// PureShield Design Tokens.
// Tokens marked `get()` follow the active theme (ThemeManager.active), so every screen re-skins live.
val PsText = Color(0xFFE8ECEF)
val PsTextMuted = Color(0xFF8A939C)
val PsOnAccent = Color(0xFF0B0D10)
val PsDanger: Color get() = ThemeManager.active.composeDanger
val PsOnDanger = Color(0xFF0B0D10) // dark text on danger red: ~6.6:1 contrast vs ~3:1 for white
val PsBorderStrong = Color(0xFF2A313A)

// Default / Midnight Mint Palette
val ShieldPrimary: Color get() = ThemeManager.active.composeAccent         // PureShield Accent
val ShieldPrimaryLight: Color get() = ThemeManager.active.composeAccent
val ShieldPrimaryDark: Color get() = ThemeManager.active.composeAccentContainer
val ShieldSecondary: Color get() = ThemeManager.active.composeAccent
val ShieldTertiary: Color get() = ThemeManager.active.composeTertiary
val ShieldAlt: Color get() = ThemeManager.active.composeAlt

val ShieldDarkBg: Color get() = ThemeManager.active.composeBg          // Midnight Canvas
val ShieldDarkSurface: Color get() = ThemeManager.active.composeSurface     // Flat Surface
val ShieldDarkSurfaceVariant: Color get() = ThemeManager.active.composeBorder // 1dp Border / Separator
val ShieldDarkCard: Color get() = ThemeManager.active.composeSurface        // Flat Card Container
val ShieldBorder: Color get() = ThemeManager.active.composeBorder          // Thin Border

val ShieldCrimsonDanger: Color get() = ThemeManager.active.composeDanger    // Threat Block Red
val ShieldCrimsonLight: Color get() = lerp(ThemeManager.active.composeDanger, Color.White, 0.4f)
val ShieldAmberWarning: Color get() = ThemeManager.active.composeWarning
val ShieldEmeraldSafe: Color get() = ThemeManager.active.composeAccent      // Verified Safe Accent

val TextPrimary = Color(0xFFE8ECEF)           // ps_text
val TextSecondary = Color(0xFF8A939C)         // ps_text_muted
val TextMuted = Color(0xFF8A939C)

val AccentGradientStart: Color get() = ThemeManager.active.composeAccentContainer
val AccentGradientMiddle: Color get() = ThemeManager.active.composeAccent
val AccentGradientEnd: Color get() = ThemeManager.active.composeAccent

val DangerGradientStart: Color get() = lerp(ThemeManager.active.composeBg, ThemeManager.active.composeDanger, 0.15f)
val DangerGradientEnd: Color get() = ThemeManager.active.composeDanger

/** A deeper, more urgent red for the most serious threats (e.g. bypass attempts). */
val PsDangerStrong: Color
    get() {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(ThemeManager.active.composeDanger.toArgb(), hsl)
        return Color.hsl(hsl[0], 0.85f, 0.45f)
    }

/**
 * Eight milestone-medal colours derived from the accent: lightness climbs tier by tier
 * while the hue drifts, so every tier is distinct yet the set always matches the theme.
 */
fun medalColors(accent: Color): List<Color> {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(accent.toArgb(), hsl)
    return List(8) { i ->
        val t = i / 7f
        Color.hsl(
            hue = (hsl[0] + i * 14f) % 360f,
            saturation = (hsl[1] * (0.75f + 0.25f * t)).coerceIn(0f, 1f),
            lightness = 0.50f + 0.36f * t
        )
    }
}

@Immutable
data class PureShieldCustomColors(
    val bg: Color,
    val surface: Color,
    val border: Color,
    val accent: Color,
    val accentContainer: Color,
    val text: Color = PsText,
    val textMuted: Color = PsTextMuted,
    val onAccent: Color = PsOnAccent,
    val danger: Color = PsDanger,
    val borderStrong: Color = PsBorderStrong
)

val LocalPureShieldColors = staticCompositionLocalOf {
    PureShieldCustomColors(
        bg = Color(0xFF0B0D10),
        surface = Color(0xFF13161B),
        border = Color(0xFF1D2229),
        accent = Color(0xFF7CE3C4),
        accentContainer = Color(0xFF10231E)
    )
}
