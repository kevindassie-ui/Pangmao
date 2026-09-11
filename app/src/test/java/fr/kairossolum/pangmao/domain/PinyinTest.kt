package fr.kairossolum.pangmao.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class PinyinTest {
    @Test
    fun `numbered pinyin becomes tone marks`() {
        assertEquals("Zhōng guó", Pinyin.withToneMarks("Zhong1 guo2"))
        assertEquals("nǚ ér", Pinyin.withToneMarks("nu:3 er2"))
        assertEquals("liú", Pinyin.withToneMarks("liu2"))
        assertEquals("ma", Pinyin.withToneMarks("ma5"))
    }

    @Test
    fun `plain pinyin ignores tones spaces and umlaut spelling`() {
        assertEquals("zhongguo", Pinyin.plain("Zhōng guó"))
        assertEquals("nver", Pinyin.plain("nü3 er2"))
        assertEquals("nver", Pinyin.plain("nu:3 er2"))
    }

    @Test
    fun `tone can be read from numbered or marked syllable`() {
        assertEquals(3, Pinyin.toneOf("hao3"))
        assertEquals(3, Pinyin.toneOf("hǎo"))
        assertEquals(5, Pinyin.toneOf("ma"))
    }
}

