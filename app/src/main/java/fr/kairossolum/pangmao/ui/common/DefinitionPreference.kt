package fr.kairossolum.pangmao.ui.common

import androidx.compose.runtime.staticCompositionLocalOf
import fr.kairossolum.pangmao.data.settings.DefinitionLanguage
import fr.kairossolum.pangmao.domain.model.DictionaryEntry

val LocalDefinitionLanguage = staticCompositionLocalOf { DefinitionLanguage.FRENCH }

fun DictionaryEntry.primaryDefinition(language: DefinitionLanguage): String = when (language) {
    DefinitionLanguage.FRENCH -> definitionsFrench.firstOrNull()
    DefinitionLanguage.ENGLISH -> definitionsEnglish.firstOrNull()
    DefinitionLanguage.BOTH -> definitionsFrench.firstOrNull() ?: definitionsEnglish.firstOrNull()
} ?: "—"
