package fr.kairossolum.pangmao.ui.common

import fr.kairossolum.pangmao.data.settings.DefinitionLanguage
import fr.kairossolum.pangmao.data.translation.TranslationRepository
import fr.kairossolum.pangmao.data.translation.TranslationTarget
import fr.kairossolum.pangmao.domain.model.DictionaryEntry
import fr.kairossolum.pangmao.domain.model.ExampleSentence
import fr.kairossolum.pangmao.domain.model.TextAnalysis
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class TextTranslationStateTest {
    @Test
    fun `exact dictionary entry never requires a translation model`() = runTest {
        val repository = RecordingTranslationRepository()
        var state = TextTranslationState()

        resolveTextTranslation(
            analysis = analysis("笨蛋", exactEntry = entry("笨蛋", "imbécile", "idiot")),
            definitionLanguage = DefinitionLanguage.BOTH,
            translation = repository,
            update = { state = it },
        )

        assertEquals("imbécile", state.french)
        assertEquals("idiot", state.english)
        assertEquals(0, repository.readinessChecks)
        assertEquals(0, repository.translations)
        assertFalse(state.hasAutomaticTranslation)
    }

    @Test
    fun `curated correction bypasses translation models`() = runTest {
        val repository = RecordingTranslationRepository()
        var state = TextTranslationState()

        resolveTextTranslation(
            analysis = analysis("好喜欢"),
            definitionLanguage = DefinitionLanguage.FRENCH,
            translation = repository,
            update = { state = it },
        )

        assertEquals("aimer beaucoup ; adorer", state.french)
        assertEquals(0, repository.readinessChecks)
        assertEquals(0, repository.translations)
    }

    @Test
    fun `reviewed bilingual example bypasses both translation models`() = runTest {
        val repository = RecordingTranslationRepository()
        var state = TextTranslationState()
        val example = ExampleSentence(
            id = 1,
            chinese = "猫在睡觉。",
            pinyin = "mao1 zai4 shui4 jiao4",
            english = "The cat is sleeping.",
            french = "Le chat dort.",
            tatoebaChineseId = 0,
            tatoebaEnglishId = 0,
            tatoebaFrenchId = 0,
            chineseSource = "Pangmao",
            englishSource = "Pangmao",
            frenchSource = "Pangmao",
        )

        resolveTextTranslation(
            analysis = TextAnalysis(
                sourceText = example.chinese,
                exactEntry = null,
                exactExample = example,
                tokens = emptyList(),
            ),
            definitionLanguage = DefinitionLanguage.BOTH,
            translation = repository,
            update = { state = it },
        )

        assertEquals(example.french, state.french)
        assertEquals(example.english, state.english)
        assertEquals(0, repository.readinessChecks)
        assertEquals(0, repository.translations)
    }

    @Test
    fun `cultural food name is protected before automatic sentence translation`() = runTest {
        val repository = RecordingTranslationRepository()

        resolveTextTranslation(
            analysis = analysis("我买了两个肉夹馍"),
            definitionLanguage = DefinitionLanguage.BOTH,
            translation = repository,
            update = {},
        )

        assertEquals(2, repository.translations)
        assertEquals(
            listOf("我买了两个 roujiamo", "我买了两个 roujiamo"),
            repository.translatedTexts,
        )
    }

    private fun analysis(source: String, exactEntry: DictionaryEntry? = null) = TextAnalysis(
        sourceText = source,
        exactEntry = exactEntry,
        exactExample = null,
        tokens = emptyList(),
    )

    private fun entry(source: String, french: String, english: String) = DictionaryEntry(
        id = 1,
        traditional = source,
        simplified = source,
        pinyin = "",
        definitionsEnglish = listOf(english),
        definitionsFrench = listOf(french),
        sources = "test",
        frequency = 1,
    )

    private class RecordingTranslationRepository : TranslationRepository {
        var readinessChecks = 0
        var translations = 0
        val translatedTexts = mutableListOf<String>()

        override suspend fun isReady(target: TranslationTarget): Boolean {
            readinessChecks += 1
            return true
        }

        override suspend fun download(target: TranslationTarget) = Unit

        override suspend fun translateChinese(text: String, target: TranslationTarget): String {
            translations += 1
            translatedTexts += text
            return "automatic"
        }
    }
}
