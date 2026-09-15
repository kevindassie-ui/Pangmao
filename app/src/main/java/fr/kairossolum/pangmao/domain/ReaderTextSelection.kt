package fr.kairossolum.pangmao.domain

import fr.kairossolum.pangmao.domain.model.AnalyzedToken
import fr.kairossolum.pangmao.domain.model.TextAnalysis

/** Resolve a UTF-16 display offset to the token produced from the trimmed analysis source. */
internal fun TextAnalysis.tokenAtDisplayOffset(
    displayedText: String,
    offset: Int,
): AnalyzedToken? {
    if (offset !in displayedText.indices || sourceText.isEmpty()) return null
    val analysisStart = displayedText.indexOf(sourceText)
    if (analysisStart < 0) return null

    var tokenStart = analysisStart
    for (analyzed in tokens) {
        val tokenEnd = tokenStart + analyzed.token.text.length
        if (offset in tokenStart until tokenEnd) return analyzed
        tokenStart = tokenEnd
    }
    return null
}

/** Return the complete Unicode code point touched by a UTF-16 text layout offset. */
internal fun String.codePointAtDisplayOffset(offset: Int): String? {
    if (offset !in indices) return null
    val codePointStart = if (
        this[offset].isLowSurrogate() &&
        offset > 0 &&
        this[offset - 1].isHighSurrogate()
    ) {
        offset - 1
    } else {
        offset
    }
    return String(Character.toChars(codePointAt(codePointStart)))
}
