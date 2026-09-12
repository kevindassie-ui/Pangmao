package fr.kairossolum.pangmao.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TranslationQualityTest {
    @Test
    fun `corrects reported slang translation`() {
        val value = TranslationQuality.curated("傻比")

        assertEquals("idiot ; imbécile (vulgaire)", value?.french)
        assertEquals("idiot; dumbass (vulgar)", value?.english)
    }

    @Test
    fun `corrects reported verbal expression instead of translating like as comme`() {
        val value = TranslationQuality.curated(" 好 喜欢 ")

        assertEquals("aimer beaucoup ; adorer", value?.french)
        assertEquals("really like; be very fond of", value?.english)
    }

    @Test
    fun `leaves unknown expressions to the translation model`() {
        assertNull(TranslationQuality.curated("天气很好"))
    }

    @Test
    fun `cleans automatic translation whitespace`() {
        assertEquals(
            "Une traduction utile",
            TranslationQuality.polishFrench("天气很好", "  Une   traduction\tutile  "),
        )
    }
}
