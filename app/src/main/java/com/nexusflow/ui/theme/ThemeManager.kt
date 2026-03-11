package com.nexusflow.ui.theme

import android.content.Context
import com.nexusflow.R

object ThemeManager {
    private const val PREFS_NAME = "nexus_theme_prefs"
    private const val KEY_FAMILY = "theme_family"
    private const val KEY_DARK_MODE = "is_dark_mode"

    enum class ThemeFamily(val displayName: String) {
        FIRE("Fire"),
        SOLAR("Solar"),
        PASTEL("Pastel")
    }

    fun setThemeFamily(context: Context, family: ThemeFamily) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_FAMILY, family.name).apply()
    }

    fun getThemeFamily(context: Context): ThemeFamily {
        val name = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_FAMILY, ThemeFamily.PASTEL.name) ?: ThemeFamily.PASTEL.name
        return ThemeFamily.valueOf(name)
    }

    fun setDarkMode(context: Context, isDark: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_DARK_MODE, isDark).apply()
    }

    fun isDarkMode(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_DARK_MODE, true)
    }

    fun applyTheme(context: Context) {
        val family = getThemeFamily(context)
        val isDark = isDarkMode(context)

        val themeRes = when (family) {
            ThemeFamily.FIRE -> if (isDark) R.style.Theme_NexusFlow_Fire_Dark else R.style.Theme_NexusFlow_Fire_Light
            ThemeFamily.SOLAR -> if (isDark) R.style.Theme_NexusFlow_Solar_Dark else R.style.Theme_NexusFlow_Solar_Light
            ThemeFamily.PASTEL -> if (isDark) R.style.Theme_NexusFlow_Pastel_Dark else R.style.Theme_NexusFlow_Pastel_Light
        }
        context.setTheme(themeRes)
    }
}