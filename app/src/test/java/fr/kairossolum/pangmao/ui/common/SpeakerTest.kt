package fr.kairossolum.pangmao.ui.common

import android.speech.tts.TextToSpeech
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeakerTest {
    @Test
    fun `recognizes Chinese and Mandarin locale tags`() {
        assertTrue(isMandarinLocale(Locale.SIMPLIFIED_CHINESE))
        assertTrue(isMandarinLocale(Locale.forLanguageTag("cmn-CN")))
        assertFalse(isMandarinLocale(Locale.forLanguageTag("yue-HK")))
        assertFalse(isMandarinLocale(Locale.ENGLISH))
    }

    @Test
    fun `accepts every successful Android language availability level`() {
        assertTrue(isTtsLanguageResultUsable(TextToSpeech.LANG_AVAILABLE))
        assertTrue(isTtsLanguageResultUsable(TextToSpeech.LANG_COUNTRY_AVAILABLE))
        assertTrue(isTtsLanguageResultUsable(TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE))
        assertFalse(isTtsLanguageResultUsable(TextToSpeech.LANG_MISSING_DATA))
        assertFalse(isTtsLanguageResultUsable(TextToSpeech.LANG_NOT_SUPPORTED))
    }

    @Test
    fun `speech text is split at sentence boundaries without losing content`() {
        val text = "你好。今天学习！明天继续"

        val chunks = chunkSpeechText(text)

        assertEquals(listOf("你好。", "今天学习！", "明天继续"), chunks)
        assertEquals(text, chunks.joinToString(""))
    }

    @Test
    fun `long unpunctuated text is kept below the requested chunk size`() {
        assertEquals(
            listOf("中文学习", "没有句号"),
            chunkSpeechText("中文学习没有句号", maximumLength = 4),
        )
        assertEquals(emptyList<String>(), chunkSpeechText("   \n  "))
    }

    @Test
    fun `utterance identifiers preserve generation segment and source offset`() {
        val identifier = utteranceId(generation = 12, segmentIndex = 3, sourceStart = 27)

        assertEquals(PlaybackProgress(12, 3, 27), identifier.toPlaybackProgress())
        assertEquals(null, "pangmao:12:3".toPlaybackProgress())
        assertEquals(null, "pangmao:12:-1:27".toPlaybackProgress())
        assertEquals(null, "foreign:12:3:27".toPlaybackProgress())
    }
}
