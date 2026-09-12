package fr.kairossolum.pangmao.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class DictionarySegmenterTest {
    @Test
    fun `uses longest dictionary match and preserves punctuation`() {
        val tokenizer = DictionarySegmenter(
            mapOf(
                "中" to HeadwordMatch(1L, 500),
                "中国" to HeadwordMatch(2L, 400),
                "中国人" to HeadwordMatch(3L, 300),
                "学习" to HeadwordMatch(4L, 200),
            )
        )

        val tokens = tokenizer.tokenize("中国人学习。")

        assertEquals(listOf("中国人", "学习", "。"), tokens.map { it.text })
        assertEquals(listOf(3L, 4L, null), tokens.map { it.entryId })
    }

    @Test
    fun `groups adjacent unknown text`() {
        val tokenizer = DictionarySegmenter(mapOf("中国" to HeadwordMatch(2L)))

        assertEquals(listOf("abc ", "中国", "!"), tokenizer.tokenize("abc 中国!").map { it.text })
    }

    @Test
    fun `frequency resolves overlapping words into the most plausible path`() {
        val tokenizer = DictionarySegmenter(
            mapOf(
                "一起" to HeadwordMatch(1L, 30),
                "床" to HeadwordMatch(2L, 2),
                "一" to HeadwordMatch(3L, 1_000),
                "起床" to HeadwordMatch(4L, 100),
            )
        )

        assertEquals(listOf("一", "起床"), tokenizer.tokenize("一起床").map { it.text })
    }

    @Test
    fun `decomposes unknown expression into useful dictionary blocks`() {
        val tokenizer = DictionarySegmenter(
            mapOf(
                "大" to HeadwordMatch(1L, 1_000),
                "大笨" to HeadwordMatch(2L),
                "笨蛋" to HeadwordMatch(3L, 60),
                "蛋" to HeadwordMatch(4L, 100),
            )
        )

        assertEquals(listOf("大", "笨蛋"), tokenizer.tokenize("大笨蛋").map { it.text })
    }
}
