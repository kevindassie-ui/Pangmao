package fr.kairossolum.pangmao.domain

import fr.kairossolum.pangmao.domain.model.TextToken

class LongestMatchTokenizer(
    private val headwords: Map<String, Long>,
    private val maximumWordLength: Int = headwords.keys.maxOfOrNull(String::length)?.coerceAtMost(8) ?: 1,
) {
    fun tokenize(text: String): List<TextToken> {
        if (text.isEmpty()) return emptyList()
        val result = mutableListOf<TextToken>()
        var index = 0
        while (index < text.length) {
            var match: String? = null
            val upper = minOf(maximumWordLength, text.length - index)
            for (length in upper downTo 1) {
                val candidate = text.substring(index, index + length)
                if (headwords.containsKey(candidate)) {
                    match = candidate
                    break
                }
            }
            val token = match ?: text[index].toString()
            val entryId = headwords[token]
            val isChinese = token.codePoints().anyMatch(::isHanCodePoint)
            val next = TextToken(token, entryId, isChinese)
            val previous = result.lastOrNull()
            if (entryId == null && previous != null && previous.entryId == null && previous.isChinese == isChinese) {
                result[result.lastIndex] = previous.copy(text = previous.text + token)
            } else {
                result += next
            }
            index += token.length
        }
        return result
    }

    private fun isHanCodePoint(codepoint: Int): Boolean =
        codepoint in 0x3400..0x4DBF ||
            codepoint in 0x4E00..0x9FFF ||
            codepoint in 0xF900..0xFAFF ||
            codepoint in 0x20000..0x323AF
}
