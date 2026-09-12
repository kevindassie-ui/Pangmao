package fr.kairossolum.pangmao.domain

import fr.kairossolum.pangmao.domain.model.AnalyzedToken
import fr.kairossolum.pangmao.domain.model.DictionaryEntry
import fr.kairossolum.pangmao.domain.model.TextAnalysis
import fr.kairossolum.pangmao.domain.model.TextToken
import org.junit.Assert.assertEquals
import org.junit.Test

class TextReadingTest {
    @Test
    fun `builds continuous pinyin and keeps punctuation`() {
        val analysis = TextAnalysis(
            sourceText = "今天，我们学习。",
            exactEntry = null,
            exactExample = null,
            tokens = listOf(
                analyzed("今天", "jin1 tian1"),
                AnalyzedToken(TextToken("，"), null),
                analyzed("我们", "wo3 men5"),
                analyzed("学习", "xue2 xi2"),
                AnalyzedToken(TextToken("。"), null),
            ),
        )

        assertEquals("jin1 tian1， wo3 men5 xue2 xi2。", analysis.numberedPinyinReading())
    }

    private fun analyzed(text: String, pinyin: String): AnalyzedToken = AnalyzedToken(
        token = TextToken(text = text, entryId = text.hashCode().toLong(), isChinese = true),
        entry = DictionaryEntry(
            id = text.hashCode().toLong(),
            traditional = text,
            simplified = text,
            pinyin = pinyin,
            definitionsEnglish = emptyList(),
            definitionsFrench = emptyList(),
            sources = "test",
            frequency = 1,
        ),
    )
}
