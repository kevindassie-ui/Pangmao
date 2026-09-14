package fr.kairossolum.pangmao.data.settings

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import fr.kairossolum.pangmao.domain.model.LearningLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class DefinitionLanguage { FRENCH, ENGLISH, BOTH }

enum class SpeechRate(val multiplier: Float, val label: String) {
    SLOW(0.8f, "0.8×"),
    NORMAL(1.0f, "1×"),
    FAST(1.2f, "1.2×"),
}

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
    val learningLanguage: LearningLanguage = LearningLanguage.CHINESE,
    val speechRate: SpeechRate = SpeechRate.NORMAL,
)

class SettingsRepository(private val context: Context) {
    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { it.toAppSettings() }

    suspend fun setThemeMode(value: ThemeMode) {
        context.settingsDataStore.edit { it[SettingsKeys.THEME] = value.name }
    }

    suspend fun setLanguage(value: AppLanguage) {
        context.settingsDataStore.edit { it[SettingsKeys.LANGUAGE] = value.name }
        applyLanguage(value)
    }

    suspend fun setDefinitionLanguage(value: DefinitionLanguage) {
        context.settingsDataStore.edit { it[SettingsKeys.DEFINITION_LANGUAGE] = value.name }
    }

    suspend fun setLearningLanguage(value: LearningLanguage) {
        context.settingsDataStore.edit { it[SettingsKeys.LEARNING_LANGUAGE] = value.name }
    }

    suspend fun setSpeechRate(value: SpeechRate) {
        context.settingsDataStore.edit { it[SettingsKeys.SPEECH_RATE] = value.name }
    }

    fun applyLanguage(value: AppLanguage) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(value.languageTag))
    }
}

internal object SettingsKeys {
    val THEME = stringPreferencesKey("theme")
    val LANGUAGE = stringPreferencesKey("language")
    val DEFINITION_LANGUAGE = stringPreferencesKey("definition_language")
    val LEARNING_LANGUAGE = stringPreferencesKey("learning_language")
    val SPEECH_RATE = stringPreferencesKey("speech_rate")
}

internal fun Preferences.toAppSettings(systemLanguage: String = Locale.getDefault().language): AppSettings {
    val language = this[SettingsKeys.LANGUAGE].toEnumOrNull<AppLanguage>() ?: AppLanguage.SYSTEM
    return AppSettings(
        themeMode = this[SettingsKeys.THEME].toEnumOrNull<ThemeMode>() ?: ThemeMode.SYSTEM,
        language = language,
        definitionLanguage = this[SettingsKeys.DEFINITION_LANGUAGE].toEnumOrNull<DefinitionLanguage>()
            ?: defaultDefinitionLanguage(language, systemLanguage),
        learningLanguage = this[SettingsKeys.LEARNING_LANGUAGE].toEnumOrNull<LearningLanguage>()
            ?: LearningLanguage.CHINESE,
        speechRate = this[SettingsKeys.SPEECH_RATE].toEnumOrNull<SpeechRate>() ?: SpeechRate.NORMAL,
    )
}

private inline fun <reified T : Enum<T>> String?.toEnumOrNull(): T? =
    this?.let { stored -> enumValues<T>().firstOrNull { it.name == stored } }

private fun defaultDefinitionLanguage(
    language: AppLanguage,
    systemLanguage: String,
): DefinitionLanguage = when (language) {
    AppLanguage.FRENCH -> DefinitionLanguage.FRENCH
    AppLanguage.ENGLISH -> DefinitionLanguage.ENGLISH
    AppLanguage.CHINESE -> DefinitionLanguage.BOTH
    AppLanguage.SYSTEM -> when (systemLanguage) {
        "en" -> DefinitionLanguage.ENGLISH
        "zh" -> DefinitionLanguage.BOTH
        else -> DefinitionLanguage.FRENCH
    }
}
