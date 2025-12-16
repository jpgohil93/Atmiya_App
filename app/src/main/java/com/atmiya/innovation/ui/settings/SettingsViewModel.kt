package com.atmiya.innovation.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.atmiya.innovation.ui.theme.ThemeManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val themeManager = ThemeManager(application)
    
    val theme = themeManager.themeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "system"
        )

    fun setTheme(mode: String) {
        viewModelScope.launch {
            themeManager.setTheme(mode)
        }
    }
}
