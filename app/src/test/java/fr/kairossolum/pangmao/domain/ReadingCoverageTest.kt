package fr.kairossolum.pangmao.domain

import fr.kairossolum.pangmao.domain.model.AnalyzedToken
import fr.kairossolum.pangmao.domain.model.DictionaryEntry
import fr.kairossolum.pangmao.domain.model.TextToken
import fr.kairossolum.pangmao.domain.model.WordKnowledgeStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ReadingCoverageTest {
    @Test
    fun `empty or non-Chinese input stays unrated`() {
        val coverage = calculateReadingCoverage(
            tokens = listOf(AnalyzedToken(TextToken("Hello"), null)),
            knowledgeByEntryId = emptyMap(),
        )

        assertEquals(0, coverage.recognizedOccurrences)
        assertEquals(0, coverage.unknownChineseBlocks)
        assertEquals(ReadingDifficultyBand.UNRATED, coverage.difficulty)
    }

    @Test
    fun `occurrences distinct words and unknown blocks remain separate`() {
        val coverage = calculateReadingCoverage(
            tokens = listOf(
                analyzed(1L, frequency = 20),
                analyzed(1L, frequency = 20),
                analyzed(2L, frequency = 4),
                analyzed(3L, frequency = 2),
                unknownChinese(),
            ),
            knowledgeByEntryId = mapOf(
                1L to WordKnowledgeStatus.KNOWN,
                2L to WordKnowledgeStatus.LEARNING,
            ),
        )

        assertEquals(4, coverage.recognizedOccurrences)
        assertEquals(3, coverage.distinctRecognizedWords)
        assertEquals(1, coverage.unknownChineseBlocks)
        assertEquals(2, coverage.knownOccurrences)
        assertEquals(1, coverage.learningOccurrences)
        assertEquals(1, coverage.unmarkedOccurrences)
        assertEquals(1, coverage.frequentUnmarkedOccurrences)
        assertEquals(ReadingDifficultyBand.UNRATED, coverage.difficulty)
    }

    @Test
    fun `a repeated known word cannot manufacture enough profile coverage`() {
        val tokens = List(9) { analyzed(1L) } + analyzed(2L)
        val coverage = calculateReadingCoverage(
            tokens = tokens,
            knowledgeByEntryId = mapOf(1L to WordKnowledgeStatus.KNOWN),
        )

        assertEquals(0.9, coverage.classifiedOccurrenceRatio, 0.0001)
        assertEquals(0.5, coverage.classifiedDistinctRatio, 0.0001)
        assertEquals(ReadingDifficultyBand.UNRATED, coverage.difficulty)
    }

    @Test
    fun `fully classified texts receive descriptive coverage bands`() {
        assertEquals(ReadingDifficultyBand.ACCESSIBLE, bandFor(known = 9, learning = 1))
        assertEquals(ReadingDifficultyBand.STIMULATING, bandFor(known = 7, learning = 3))
        assertEquals(ReadingDifficultyBand.DENSE, bandFor(known = 6, learning = 4))
    }

    private fun bandFor(known: Int, learning: Int): ReadingDifficultyBand {
        val tokens = (1..known + learning).map { id -> analyzed(id.toLong()) }
        val statuses = buildMap {
            (1..known).forEach { id -> put(id.toLong(), WordKnowledgeStatus.KNOWN) }
            (known + 1..known + learning).forEach { id ->
                put(id.toLong(), WordKnowledgeStatus.LEARNING)
            }
        }
        return calculateReadingCoverage(tokens, statuses).difficulty
    }

    private fun analyzed(id: Long, frequency: Int = 1): AnalyzedToken = AnalyzedToken(
        token = TextToken(text = "词$id", entryId = id, isChinese = true),
        entry = DictionaryEntry(
            id = id,
            traditional = "词$id",
            simplified = "词$id",
            pinyin = "ci2",
            definitionsEnglish = emptyList(),
            definitionsFrench = emptyList(),
            sources = "test",
            frequency = frequency,
        ),
    )

    private fun unknownChinese(): AnalyzedToken = AnalyzedToken(
        token = TextToken(text = "𰻞", entryId = null, isChinese = true),
        entry = null,
    )
}
