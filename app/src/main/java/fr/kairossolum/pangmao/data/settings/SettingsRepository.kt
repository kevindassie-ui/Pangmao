package fr.kairossolum.pangmao.data.settings

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class DefinitionLanguage { FRENCH, ENGLISH, BOTH }

enum class AppLanguage(val languageTag: String) {
    SYSTEM(""),
    FRENCH("fr"),
    ENGLISH("en"),
    CHINESE("zh-Hans"),
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val definitionLanguage: DefinitionLanguage = DefinitionLanguage.FRENCH,
)

class SettingsRepository(private val context: Context) {
    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { preferences ->
        val language = preferences[LANGUAGE]?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() }
            ?: AppLanguage.SYSTEM
        AppSettings(
            themeMode = preferences[THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            language = language,
            definitionLanguage = preferences[DEFINITION_LANGUAGE]
                ?.let { runCatching { DefinitionLanguage.valueOf(it) }.getOrNull() }
                ?: defaultDefinitionLanguage(language),
        )
    }

    suspend fun setThemeMode(value: ThemeMode) {
        context.settingsDataStore.edit { it[THEME] = value.name }
    }

    suspend fun setLanguage(value: AppLanguage) {
        context.settingsDataStore.edit { it[LANGUAGE] = value.name }
        applyLanguage(value)
    }

    suspend fun setDefinitionLanguage(value: DefinitionLanguage) {
        context.settingsDataStore.edit { it[DEFINITION_LANGUAGE] = value.name }
    }

    fun applyLanguage(value: AppLanguage) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(value.languageTag))
    }

    private companion object {
        val THEME = stringPreferencesKey("theme")
        val LANGUAGE = stringPreferencesKey("language")
        val DEFINITION_LANGUAGE = stringPreferencesKey("definition_language")

        fun defaultDefinitionLanguage(language: AppLanguage): DefinitionLanguage = when (language) {
            AppLanguage.FRENCH -> DefinitionLanguage.FRENCH
            AppLanguage.ENGLISH -> DefinitionLanguage.ENGLISH
            AppLanguage.CHINESE -> DefinitionLanguage.BOTH
            AppLanguage.SYSTEM -> when (Locale.getDefault().language) {
                "en" -> DefinitionLanguage.ENGLISH
                "zh" -> DefinitionLanguage.BOTH
                else -> DefinitionLanguage.FRENCH
            }
        }
    }
}
