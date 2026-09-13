package fr.kairossolum.pangmao.ui.common

import android.speech.tts.TextToSpeech
import java.util.Locale
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
}
