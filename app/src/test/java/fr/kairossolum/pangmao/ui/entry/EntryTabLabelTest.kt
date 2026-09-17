package fr.kairossolum.pangmao.ui.entry

import org.junit.Assert.assertEquals
import org.junit.Test

class EntryTabLabelTest {
    @Test
    fun `fallback wraps French label only between words`() {
        assertEquals("Mots\nassociés", entryTabFallbackLabel("Mots associés"))
    }

    @Test
    fun `fallback wraps English label only between words`() {
        assertEquals("Related\nwords", entryTabFallbackLabel("Related words"))
    }

    @Test
    fun `fallback never splits a Chinese label`() {
        assertEquals("相关词语", entryTabFallbackLabel("相关词语"))
    }
}
