package fr.kairossolum.pangmao.domain

import fr.kairossolum.pangmao.domain.model.LearningLanguage
import fr.kairossolum.pangmao.domain.model.LexicalFeature
import fr.kairossolum.pangmao.domain.model.MeaningPolicy
import fr.kairossolum.pangmao.domain.model.PronunciationSystem
import fr.kairossolum.pangmao.domain.model.WritingSystem
import fr.kairossolum.pangmao.domain.model.presentation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningProfileTest {
    @Test
    fun `Chinese profile preserves the current dictionary hierarchy`() {
        val presentation = LearningLanguage.CHINESE.presentation

        assertEquals(WritingSystem.HANZI, presentation.writingSystem)
        assertEquals(PronunciationSystem.PINYIN, presentation.pronunciationSystem)
        assertEquals(MeaningPolicy.DEFINITION_LANGUAGE_SETTING, presentation.meaningPolicy)
        assertTrue(LexicalFeature.TONE_COLORS in presentation.lexicalFeatures)
        assertTrue(LexicalFeature.CHARACTER_BREAKDOWN in presentation.lexicalFeatures)
    }

    @Test
    fun `French profile exposes French lexical fields and Chinese meanings`() {
        val presentation = LearningLanguage.FRENCH.presentation

        assertEquals(WritingSystem.LATIN, presentation.writingSystem)
        assertEquals(PronunciationSystem.IPA, presentation.pronunciationSystem)
        assertEquals(MeaningPolicy.CHINESE, presentation.meaningPolicy)
        assertTrue(LexicalFeature.GRAMMATICAL_GENDER in presentation.lexicalFeatures)
        assertTrue(LexicalFeature.CONJUGATION in presentation.lexicalFeatures)
        assertFalse(LexicalFeature.TONE_COLORS in presentation.lexicalFeatures)
    }

    @Test
    fun `English profile exposes forms and collocations with Chinese meanings`() {
        val presentation = LearningLanguage.ENGLISH.presentation

        assertEquals(WritingSystem.LATIN, presentation.writingSystem)
        assertEquals(PronunciationSystem.IPA, presentation.pronunciationSystem)
        assertEquals(MeaningPolicy.CHINESE, presentation.meaningPolicy)
        assertTrue(LexicalFeature.INFLECTIONS in presentation.lexicalFeatures)
        assertTrue(LexicalFeature.COLLOCATIONS in presentation.lexicalFeatures)
        assertFalse(LexicalFeature.CHARACTER_BREAKDOWN in presentation.lexicalFeatures)
    }
}
