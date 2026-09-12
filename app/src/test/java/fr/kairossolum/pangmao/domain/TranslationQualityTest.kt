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

    @Test
    fun `uses natural translations for reported reader sentences`() {
        val resemblance = TranslationQuality.curated("你很像你哥哥。")
        val food = TranslationQuality.curated("我上次去市中心买了三个肉夹馍")

        assertEquals("Tu ressembles beaucoup à ton frère aîné.", resemblance?.french)
        assertEquals("You look a lot like your older brother.", resemblance?.english)
        assertEquals("La dernière fois, je suis allé en centre-ville acheter trois roujiamos.", food?.french)
        assertEquals("Last time, I went downtown and bought three roujiamos.", food?.english)
    }

    @Test
    fun `protects roujiamo from literal model output`() {
        assertEquals(
            "J’ai acheté trois roujiamos.",
            TranslationQuality.polishFrench("我买了三个肉夹馍", "J’ai acheté trois pinces à viande."),
        )
        assertEquals(
            "I bought three roujiamos.",
            TranslationQuality.polishEnglish("我买了三个肉夹馍", "I bought three meat clamps."),
        )
    }
}
