package fr.kairossolum.pangmao.domain

import fr.kairossolum.pangmao.domain.model.AnalyzedToken
import fr.kairossolum.pangmao.domain.model.WordKnowledgeStatus

enum class ReadingDifficultyBand {
    UNRATED,
    ACCESSIBLE,
    STIMULATING,
    DENSE,
}

data class ReadingCoverage(
    val recognizedOccurrences: Int,
    val distinctRecognizedWords: Int,
    val unknownChineseBlocks: Int,
    val knownOccurrences: Int,
    val learningOccurrences: Int,
    val unmarkedOccurrences: Int,
    val knownDistinctWords: Int,
    val learningDistinctWords: Int,
    val unmarkedDistinctWords: Int,
    val frequentUnmarkedOccurrences: Int,
    val classifiedOccurrenceRatio: Double,
    val classifiedDistinctRatio: Double,
    val knownCoverageRatio: Double,
    val difficulty: ReadingDifficultyBand,
)

fun calculateReadingCoverage(
    tokens: List<AnalyzedToken>,
    knowledgeByEntryId: Map<Long, WordKnowledgeStatus>,
): ReadingCoverage {
    val chineseTokens = tokens.filter { token -> token.token.isChinese }
    val recognized = chineseTokens.mapNotNull { analyzed ->
        val entryId = analyzed.token.entryId ?: return@mapNotNull null
        val entry = analyzed.entry ?: return@mapNotNull null
        RecognizedWordOccurrence(entryId = entryId, frequency = entry.frequency)
    }
    val statusByOccurrence = recognized.map { occurrence ->
        knowledgeByEntryId[occurrence.entryId] ?: WordKnowledgeStatus.UNMARKED
    }
    val distinctEntries = recognized
        .map { occurrence -> occurrence.entryId }
        .distinct()
    val statusByDistinctEntry = distinctEntries.map { entryId ->
        knowledgeByEntryId[entryId] ?: WordKnowledgeStatus.UNMARKED
    }

    val knownOccurrences = statusByOccurrence.count { it == WordKnowledgeStatus.KNOWN }
    val learningOccurrences = statusByOccurrence.count { it == WordKnowledgeStatus.LEARNING }
    val unmarkedOccurrences = statusByOccurrence.count { it == WordKnowledgeStatus.UNMARKED }
    val knownDistinctWords = statusByDistinctEntry.count { it == WordKnowledgeStatus.KNOWN }
    val learningDistinctWords = statusByDistinctEntry.count { it == WordKnowledgeStatus.LEARNING }
    val unmarkedDistinctWords = statusByDistinctEntry.count { it == WordKnowledgeStatus.UNMARKED }
    val recognizedOccurrences = recognized.size
    val unknownChineseBlocks = chineseTokens.size - recognizedOccurrences
    val classifiedOccurrenceRatio = ratio(knownOccurrences + learningOccurrences, recognizedOccurrences)
    val classifiedDistinctRatio = ratio(knownDistinctWords + learningDistinctWords, distinctEntries.size)
    val knownCoverageRatio = ratio(knownOccurrences, chineseTokens.size)
    val hasEnoughClassification = classifiedOccurrenceRatio >= MIN_CLASSIFIED_RATIO &&
        classifiedDistinctRatio >= MIN_CLASSIFIED_RATIO

    val difficulty = when {
        recognizedOccurrences == 0 || !hasEnoughClassification -> ReadingDifficultyBand.UNRATED
        knownCoverageRatio >= ACCESSIBLE_RATIO -> ReadingDifficultyBand.ACCESSIBLE
        knownCoverageRatio >= STIMULATING_RATIO -> ReadingDifficultyBand.STIMULATING
        else -> ReadingDifficultyBand.DENSE
    }

    return ReadingCoverage(
        recognizedOccurrences = recognizedOccurrences,
        distinctRecognizedWords = distinctEntries.size,
        unknownChineseBlocks = unknownChineseBlocks,
        knownOccurrences = knownOccurrences,
        learningOccurrences = learningOccurrences,
        unmarkedOccurrences = unmarkedOccurrences,
        knownDistinctWords = knownDistinctWords,
        learningDistinctWords = learningDistinctWords,
        unmarkedDistinctWords = unmarkedDistinctWords,
        frequentUnmarkedOccurrences = recognized.count { occurrence ->
            occurrence.frequency > 0 &&
                (knowledgeByEntryId[occurrence.entryId] ?: WordKnowledgeStatus.UNMARKED) ==
                WordKnowledgeStatus.UNMARKED
        },
        classifiedOccurrenceRatio = classifiedOccurrenceRatio,
        classifiedDistinctRatio = classifiedDistinctRatio,
        knownCoverageRatio = knownCoverageRatio,
        difficulty = difficulty,
    )
}

private fun ratio(part: Int, whole: Int): Double =
    if (whole == 0) 0.0 else part.toDouble() / whole

private data class RecognizedWordOccurrence(
    val entryId: Long,
    val frequency: Int,
)

private const val MIN_CLASSIFIED_RATIO = 0.8
private const val ACCESSIBLE_RATIO = 0.9
private const val STIMULATING_RATIO = 0.7
