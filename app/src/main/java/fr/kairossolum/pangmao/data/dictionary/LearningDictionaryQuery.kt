package fr.kairossolum.pangmao.data.dictionary

import fr.kairossolum.pangmao.domain.model.LearningLanguage
import java.text.Normalizer
import java.util.Locale

internal object LearningDictionaryQuery {
    fun languageCode(language: LearningLanguage): String = when (language) {
        LearningLanguage.FRENCH -> "fr"
        LearningLanguage.ENGLISH -> "en"
        LearningLanguage.CHINESE -> error("Chinese entries use the primary dictionary")
    }

    fun normalize(value: String): String = Normalizer
        .normalize(value.trim().lowercase(Locale.ROOT).replace('’', '\''), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .replace(Regex("[^\\p{L}\\p{N}'-]+"), " ")
        .trim()

    fun fts(value: String): String = Regex("[\\p{L}\\p{N}]+").findAll(normalize(value))
        .map { "${it.value}*" }
        .joinToString(" ")
}
