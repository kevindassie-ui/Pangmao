package fr.kairossolum.pangmao.domain

import fr.kairossolum.pangmao.domain.model.AnalyzedToken
import fr.kairossolum.pangmao.domain.model.TextAnalysis
import fr.kairossolum.pangmao.domain.model.TextToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReaderTextSelectionTest {
    @Test
    fun `display offsets account for whitespace trimmed before analysis`() {
        val analysis = TextAnalysis(
            sourceText = "今天学习。",
            exactEntry = null,
            exactExample = null,
            tokens = listOf(
                AnalyzedToken(TextToken("今天", entryId = 1, isChinese = true), null),
                AnalyzedToken(TextToken("学习", entryId = 2, isChinese = true), null),
                AnalyzedToken(TextToken("。"), null),
            ),
        )
        val displayed = "  今天学习。  "

        assertEquals("今天", analysis.tokenAtDisplayOffset(displayed, displayed.indexOf('今'))?.token?.text)
        assertEquals("学习", analysis.tokenAtDisplayOffset(displayed, displayed.indexOf('习'))?.token?.text)
        assertNull(analysis.tokenAtDisplayOffset(displayed, 0))
    }

    @Test
    fun `code point lookup never returns half of a surrogate pair`() {
        val text = "学😀习"
        val emojiStart = text.indexOf("😀")

        assertEquals("😀", text.codePointAtDisplayOffset(emojiStart))
        assertEquals("😀", text.codePointAtDisplayOffset(emojiStart + 1))
        assertEquals("习", text.codePointAtDisplayOffset(text.indexOf('习')))
        assertNull(text.codePointAtDisplayOffset(text.length))
    }
}
