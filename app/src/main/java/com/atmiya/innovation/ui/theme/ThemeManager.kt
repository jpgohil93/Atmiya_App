package com.atmiya.innovation.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "settings")

class ThemeManager(private val context: Context) {
    companion object {
        val THEME_KEY = stringPreferencesKey("theme_preference")
    }

    val themeFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[THEME_KEY] ?: "system"
        }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme
        }
    }
}
