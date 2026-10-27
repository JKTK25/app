package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("PureShield", appName)
    }

    @Test
    fun `default theme is midnight mint`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val currentTheme = ThemeManager.current(context)
        assertEquals(PsTheme.MINT, currentTheme)
        assertEquals("Midnight Mint", currentTheme.label)
    }

    @Test
    fun `theme switching updates preference and flow`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        ThemeManager.setDirect(context, PsTheme.OCEAN)
        val updatedTheme = ThemeManager.current(context)
        assertEquals(PsTheme.OCEAN, updatedTheme)
        assertEquals("Ocean Blue", updatedTheme.label)
        assertEquals(PsTheme.OCEAN, ThemeManager.themeFlow.value)
    }

    @Test
    fun `all six themes have valid tokens`() {
        assertEquals(6, PsTheme.entries.size)
        PsTheme.entries.forEach { theme ->
            assertNotNull(theme.label)
            assertNotNull(theme.composeBg)
            assertNotNull(theme.composeSurface)
            assertNotNull(theme.composeBorder)
            assertNotNull(theme.composeAccent)
            assertNotNull(theme.composeAccentContainer)
        }
    }
}
