package fr.kairossolum.pangmao.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

class PinyinTextTest {
    @Test
    fun `hanzi inherit the corresponding pinyin tone`() {
        val text = coloredHanzi("中国!", "Zhong1 guo2", bold = true)

        assertEquals("中国!", text.text)
        assertEquals(2, text.spanStyles.size)
        assertEquals(toneColor(1), text.spanStyles[0].item.color)
        assertEquals(0, text.spanStyles[0].start)
        assertEquals(1, text.spanStyles[0].end)
        assertEquals(toneColor(2), text.spanStyles[1].item.color)
        assertEquals(1, text.spanStyles[1].start)
        assertEquals(2, text.spanStyles[1].end)
    }

    @Test
    fun `missing syllables use the neutral tone`() {
        val text = coloredHanzi("中文", "zhong1")

        assertEquals(toneColor(1), text.spanStyles[0].item.color)
        assertEquals(toneColor(5), text.spanStyles[1].item.color)
    }
}
