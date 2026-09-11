package fr.kairossolum.pangmao.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.kairossolum.pangmao.data.settings.AppLanguage
import fr.kairossolum.pangmao.data.settings.AppSettings
import fr.kairossolum.pangmao.data.settings.SettingsRepository
import fr.kairossolum.pangmao.data.settings.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {
    val settings = repository.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AppSettings(),
    )

    fun setLanguage(value: AppLanguage) = viewModelScope.launch { repository.setLanguage(value) }

    fun setTheme(value: ThemeMode) = viewModelScope.launch { repository.setThemeMode(value) }
}
