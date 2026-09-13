package fr.kairossolum.pangmao.data.dictionary

import fr.kairossolum.pangmao.domain.model.LearningLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class LearningDictionaryQueryTest {
    @Test
    fun `normalization is accent insensitive and preserves lexical punctuation`() {
        assertEquals("etre l'ami co-operer", LearningDictionaryQuery.normalize(" Être l’ami, co-opérer! "))
        assertEquals("你好", LearningDictionaryQuery.normalize("你好"))
    }

    @Test
    fun `profile maps only direct learning dictionaries`() {
        assertEquals("fr", LearningDictionaryQuery.languageCode(LearningLanguage.FRENCH))
        assertEquals("en", LearningDictionaryQuery.languageCode(LearningLanguage.ENGLISH))
        assertThrows(IllegalStateException::class.java) {
            LearningDictionaryQuery.languageCode(LearningLanguage.CHINESE)
        }
    }

    @Test
    fun `full text query is generated from words only`() {
        assertEquals("etre* ami*", LearningDictionaryQuery.fts("être + ami"))
        assertEquals("你好*", LearningDictionaryQuery.fts("你好"))
    }
}
