package com.example

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PsTheme(
    val label: String,
    val styleRes: Int,
    val bg: Int,
    val surface: Int,
    val accent: Int,
    val composeBg: Color,
    val composeSurface: Color,
    val composeBorder: Color,
    val composeAccent: Color,
    val composeAccentContainer: Color,
    // Semantic colours tuned per theme so they never collide with that theme's accent.
    val composeDanger: Color = Color(0xFFE5737F),
    val composeWarning: Color = Color(0xFFFFC46B),
    val composeTertiary: Color = Color(0xFF7CB8FF),
    val composeAlt: Color = Color(0xFFB388FF)
) {
    MINT(
        label = "Midnight Mint",
        styleRes = R.style.Theme_PureShield_Mint,
        bg = R.color.mint_bg,
        surface = R.color.mint_surface,
        accent = R.color.mint_accent,
        composeBg = Color(0xFF0B0D10),
        composeSurface = Color(0xFF13161B),
        composeBorder = Color(0xFF1D2229),
        composeAccent = Color(0xFF7CE3C4),
        composeAccentContainer = Color(0xFF10231E)
    ),
    OCEAN(
        label = "Ocean Blue",
        styleRes = R.style.Theme_PureShield_Ocean,
        bg = R.color.ocean_bg,
        surface = R.color.ocean_surface,
        accent = R.color.ocean_accent,
        composeBg = Color(0xFF0A0E14),
        composeSurface = Color(0xFF111722),
        composeBorder = Color(0xFF1B2433),
        composeAccent = Color(0xFF7CB8FF),
        composeAccentContainer = Color(0xFF101C2E),
        composeTertiary = Color(0xFF7CE3C4) // accent is already blue
    ),
    VIOLET(
        label = "Violet Dusk",
        styleRes = R.style.Theme_PureShield_Violet,
        bg = R.color.violet_bg,
        surface = R.color.violet_surface,
        accent = R.color.violet_accent,
        composeBg = Color(0xFF0E0C14),
        composeSurface = Color(0xFF161320),
        composeBorder = Color(0xFF231F33),
        composeAccent = Color(0xFFB7A1FF),
        composeAccentContainer = Color(0xFF1C1830),
        composeAlt = Color(0xFFFF8FD0) // accent is already violet
    ),
    AMBER(
        label = "Amber Ember",
        styleRes = R.style.Theme_PureShield_Amber,
        bg = R.color.amber_bg,
        surface = R.color.amber_surface,
        accent = R.color.amber_accent,
        composeBg = Color(0xFF100E0A),
        composeSurface = Color(0xFF181510),
        composeBorder = Color(0xFF272218),
        composeAccent = Color(0xFFFFC46B),
        composeAccentContainer = Color(0xFF2B2210),
        composeWarning = Color(0xFFFF8A4C) // accent is already amber
    ),
    ROSE(
        label = "Rose Quartz",
        styleRes = R.style.Theme_PureShield_Rose,
        bg = R.color.rose_bg,
        surface = R.color.rose_surface,
        accent = R.color.rose_accent,
        composeBg = Color(0xFF110C0E),
        composeSurface = Color(0xFF1A1316),
        composeBorder = Color(0xFF2A1F23),
        composeAccent = Color(0xFFFF9DB1),
        composeAccentContainer = Color(0xFF2B1519),
        composeDanger = Color(0xFFEF4B5A) // accent is already pink-red
    ),
    AMOLED(
        label = "AMOLED Black",
        styleRes = R.style.Theme_PureShield_Amoled,
        bg = R.color.amoled_bg,
        surface = R.color.amoled_surface,
        accent = R.color.amoled_accent,
        composeBg = Color(0xFF000000),
        composeSurface = Color(0xFF0A0A0A),
        composeBorder = Color(0xFF1A1A1A),
        composeAccent = Color(0xFF7CE3C4),
        composeAccentContainer = Color(0xFF10231E)
    );

    companion object {
        val CommonText = Color(0xFFE8ECEF)
        val CommonTextMuted = Color(0xFF8A939C)
        val CommonOnAccent = Color(0xFF0B0D10)
        val CommonDanger = Color(0xFFE5737F)
        val CommonBorderStrong = Color(0xFF2A313A)
    }
}

object ThemeManager {
    private const val PREFS = "pureshield_prefs"
    private const val KEY = "theme"

    /** Snapshot state: every colour that reads it recomposes the moment a theme is applied. */
    var active by mutableStateOf(PsTheme.MINT)
        private set

    private val _themeFlow = MutableStateFlow(PsTheme.MINT)
    val themeFlow: StateFlow<PsTheme> = _themeFlow.asStateFlow()

    fun current(ctx: Context): PsTheme {
        val name = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, PsTheme.MINT.name)
        val resolved = PsTheme.entries.firstOrNull { it.name == name } ?: PsTheme.MINT
        if (_themeFlow.value != resolved) {
            _themeFlow.value = resolved
        }
        if (active != resolved) active = resolved
        return resolved
    }

    /** Call in every Activity BEFORE super.onCreate(). */
    fun apply(activity: Activity) {
        val theme = current(activity)
        activity.setTheme(theme.styleRes)
    }

    /** Call from the Themes sheet "Apply theme" button. The whole app re-skins. */
    fun set(activity: Activity, theme: PsTheme) {
        activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, theme.name)
            .apply()
        _themeFlow.value = theme
        active = theme // live re-skin, no activity restart
    }

    fun setDirect(ctx: Context, theme: PsTheme) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, theme.name)
            .apply()
        _themeFlow.value = theme
        active = theme
    }
}
