package fr.kairossolum.pangmao.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class LongestMatchTokenizerTest {
    @Test
    fun `uses longest dictionary match and preserves punctuation`() {
        val tokenizer = LongestMatchTokenizer(
            mapOf("中" to 1L, "中国" to 2L, "中国人" to 3L, "学习" to 4L)
        )

        val tokens = tokenizer.tokenize("中国人学习。")

        assertEquals(listOf("中国人", "学习", "。"), tokens.map { it.text })
        assertEquals(listOf(3L, 4L, null), tokens.map { it.entryId })
    }

    @Test
    fun `groups adjacent unknown text`() {
        val tokenizer = LongestMatchTokenizer(mapOf("中国" to 2L))

        assertEquals(listOf("abc ", "中国", "!"), tokenizer.tokenize("abc 中国!").map { it.text })
    }
}

