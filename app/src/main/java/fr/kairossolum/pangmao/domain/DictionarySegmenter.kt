package fr.kairossolum.pangmao.domain

import fr.kairossolum.pangmao.domain.model.TextToken
import kotlin.math.ln

data class HeadwordMatch(
    val entryId: Long,
    val frequency: Int = 0,
)

/** Finds the highest-scoring dictionary path through each run of Han characters. */
class DictionarySegmenter(
    private val headwords: Map<String, HeadwordMatch>,
    private val maximumWordLength: Int = headwords.keys
        .maxOfOrNull { it.codePointCount(0, it.length) }
        ?.coerceAtMost(8)
        ?: 1,
) {
    fun tokenize(text: String): List<TextToken> {
        if (text.isEmpty()) return emptyList()
        val result = mutableListOf<TextToken>()
        var start = 0
        while (start < text.length) {
            val isHan = isHanCodePoint(text.codePointAt(start))
            var end = start + Character.charCount(text.codePointAt(start))
            while (end < text.length && isHanCodePoint(text.codePointAt(end)) == isHan) {
                end += Character.charCount(text.codePointAt(end))
            }
            if (isHan) {
                segmentHanRun(text.substring(start, end)).forEach { result.append(it) }
            } else {
                result.append(TextToken(text.substring(start, end)))
            }
            start = end
        }
        return result
    }

    private fun segmentHanRun(run: String): List<TextToken> {
        val offsets = buildList {
            add(0)
            var offset = 0
            while (offset < run.length) {
                offset += Character.charCount(run.codePointAt(offset))
                add(offset)
            }
        }
        val count = offsets.lastIndex
        val scores = DoubleArray(count + 1) { Double.NEGATIVE_INFINITY }
        val next = IntArray(count + 1)
        val matches = arrayOfNulls<HeadwordMatch>(count)
        scores[count] = 0.0

        for (index in count - 1 downTo 0) {
            next[index] = index + 1
            scores[index] = UNKNOWN_CHARACTER_SCORE + scores[index + 1]
            val upper = minOf(count, index + maximumWordLength)
            for (end in index + 1..upper) {
                val word = run.substring(offsets[index], offsets[end])
                val match = headwords[word] ?: continue
                val candidateScore = score(match, end - index) + scores[end]
                val currentLength = next[index] - index
                if (
                    candidateScore > scores[index] + SCORE_EPSILON ||
                    (kotlin.math.abs(candidateScore - scores[index]) <= SCORE_EPSILON && end - index > currentLength)
                ) {
                    scores[index] = candidateScore
                    next[index] = end
                    matches[index] = match
                }
            }
        }

        return buildList {
            var index = 0
            while (index < count) {
                val end = next[index]
                val word = run.substring(offsets[index], offsets[end])
                val match = matches[index]
                add(TextToken(word, match?.entryId, isChinese = true))
                index = end
            }
        }
    }

    private fun score(match: HeadwordMatch, codePointLength: Int): Double =
        ln(match.frequency.coerceAtLeast(0) + 2.0) +
            (codePointLength - 1) * MULTI_CHARACTER_BONUS -
            TOKEN_PENALTY

    private fun MutableList<TextToken>.append(token: TextToken) {
        val previous = lastOrNull()
        if (
            token.entryId == null &&
            previous != null &&
            previous.entryId == null &&
            previous.isChinese == token.isChinese
        ) {
            this[lastIndex] = previous.copy(text = previous.text + token.text)
        } else {
            add(token)
        }
    }

    private fun isHanCodePoint(codepoint: Int): Boolean =
        codepoint in 0x3400..0x4DBF ||
            codepoint in 0x4E00..0x9FFF ||
            codepoint in 0xF900..0xFAFF ||
            codepoint in 0x20000..0x323AF

    private companion object {
        const val UNKNOWN_CHARACTER_SCORE = -8.0
        const val MULTI_CHARACTER_BONUS = 3.0
        const val TOKEN_PENALTY = 0.35
        const val SCORE_EPSILON = 0.000001
    }
}
