package fr.kairossolum.pangmao.data.settings

import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import fr.kairossolum.pangmao.domain.model.LearningLanguage
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsRepositoryTest {
    @Test
    fun `legacy preferences migrate to Chinese without changing existing choices`() {
        val preferences = mutablePreferencesOf(
            SettingsKeys.THEME to ThemeMode.DARK.name,
            SettingsKeys.LANGUAGE to AppLanguage.ENGLISH.name,
            SettingsKeys.DEFINITION_LANGUAGE to DefinitionLanguage.BOTH.name,
        )

        val settings = preferences.toAppSettings(systemLanguage = "fr")

        assertEquals(ThemeMode.DARK, settings.themeMode)
        assertEquals(AppLanguage.ENGLISH, settings.language)
        assertEquals(DefinitionLanguage.BOTH, settings.definitionLanguage)
        assertEquals(LearningLanguage.CHINESE, settings.learningLanguage)
        assertEquals(SpeechRate.NORMAL, settings.speechRate)
    }

    @Test
    fun `learning language is restored independently from interface language`() {
        val frenchInterface = mutablePreferencesOf(
            SettingsKeys.LANGUAGE to AppLanguage.FRENCH.name,
            SettingsKeys.LEARNING_LANGUAGE to LearningLanguage.ENGLISH.name,
        ).toAppSettings()
        val chineseInterface = mutablePreferencesOf(
            SettingsKeys.LANGUAGE to AppLanguage.CHINESE.name,
            SettingsKeys.LEARNING_LANGUAGE to LearningLanguage.ENGLISH.name,
        ).toAppSettings()

        assertEquals(LearningLanguage.ENGLISH, frenchInterface.learningLanguage)
        assertEquals(LearningLanguage.ENGLISH, chineseInterface.learningLanguage)
    }

    @Test
    fun `unknown or absent profile safely falls back to Chinese`() {
        val absent = emptyPreferences().toAppSettings(systemLanguage = "fr")
        val unknown = mutablePreferencesOf(
            SettingsKeys.LEARNING_LANGUAGE to "KLINGON",
        ).toAppSettings(systemLanguage = "fr")

        assertEquals(LearningLanguage.CHINESE, absent.learningLanguage)
        assertEquals(LearningLanguage.CHINESE, unknown.learningLanguage)
    }

    @Test
    fun `explicit definition language remains independent from interface language`() {
        val settings = mutablePreferencesOf(
            SettingsKeys.LANGUAGE to AppLanguage.CHINESE.name,
            SettingsKeys.DEFINITION_LANGUAGE to DefinitionLanguage.ENGLISH.name,
            SettingsKeys.LEARNING_LANGUAGE to LearningLanguage.FRENCH.name,
        ).toAppSettings(systemLanguage = "zh")

        assertEquals(DefinitionLanguage.ENGLISH, settings.definitionLanguage)
        assertEquals(LearningLanguage.FRENCH, settings.learningLanguage)
    }

    @Test
    fun `speech rate persists and invalid legacy value falls back safely`() {
        val fast = mutablePreferencesOf(
            SettingsKeys.SPEECH_RATE to SpeechRate.FAST.name,
        ).toAppSettings()
        val verySlow = mutablePreferencesOf(
            SettingsKeys.SPEECH_RATE to SpeechRate.VERY_SLOW.name,
        ).toAppSettings()
        val invalid = mutablePreferencesOf(
            SettingsKeys.SPEECH_RATE to "TURBO",
        ).toAppSettings()

        assertEquals(SpeechRate.FAST, fast.speechRate)
        assertEquals(SpeechRate.VERY_SLOW, verySlow.speechRate)
        assertEquals(SpeechRate.NORMAL, invalid.speechRate)
    }
}
