package fr.kairossolum.pangmao.domain.speech

/** A speakable sentence or safe TTS chunk, located in the original UTF-16 text. */
internal data class SpeechSegment(
    val index: Int,
    val sourceStart: Int,
    val sourceEndExclusive: Int,
    val text: String,
) {
    init {
        require(index >= 0)
        require(sourceStart >= 0)
        require(sourceEndExclusive > sourceStart)
        require(text.isNotEmpty())
        require(text.length == sourceEndExclusive - sourceStart)
    }

    fun containsSourceOffset(offset: Int): Boolean = offset in sourceStart until sourceEndExclusive
}

/**
 * Immutable map between TTS utterances and the source shown by the Reader.
 *
 * Android reports ranges as UTF-16 offsets. Keeping the same unit here prevents a
 * supplementary character (for example an emoji) from shifting later highlights.
 */
internal data class SpeechDocument(
    val source: String,
    val segments: List<SpeechSegment>,
) {
    init {
        segments.forEachIndexed { expectedIndex, segment ->
            require(segment.index == expectedIndex)
            require(segment.sourceEndExclusive <= source.length)
            require(source.substring(segment.sourceStart, segment.sourceEndExclusive) == segment.text)
        }
        segments.zipWithNext().forEach { (left, right) ->
            require(left.sourceEndExclusive <= right.sourceStart)
        }
    }

    fun segmentAtSourceOffset(offset: Int): SpeechSegment? =
        segments.firstOrNull { it.containsSourceOffset(offset) }
}

/** Build a source-preserving speech document without splitting a UTF-16 surrogate pair. */
internal fun buildSpeechDocument(
    text: String,
    maximumSegmentLength: Int = 180,
): SpeechDocument {
    require(maximumSegmentLength >= 2)
    val segments = mutableListOf<SpeechSegment>()
    var rawStart = 0
    var cursor = 0

    fun flush(rawEndExclusive: Int) {
        var contentStart = rawStart
        while (contentStart < rawEndExclusive) {
            val codePoint = text.codePointAt(contentStart)
            if (!codePoint.isSpeechWhitespace()) break
            contentStart += Character.charCount(codePoint)
        }

        var contentEndExclusive = rawEndExclusive
        while (contentEndExclusive > contentStart) {
            val codePoint = text.codePointBefore(contentEndExclusive)
            if (!codePoint.isSpeechWhitespace()) break
            contentEndExclusive -= Character.charCount(codePoint)
        }

        if (contentStart < contentEndExclusive) {
            segments += SpeechSegment(
                index = segments.size,
                sourceStart = contentStart,
                sourceEndExclusive = contentEndExclusive,
                text = text.substring(contentStart, contentEndExclusive),
            )
        }
        rawStart = rawEndExclusive
    }

    while (cursor < text.length) {
        val codePoint = text.codePointAt(cursor)
        val codePointLength = Character.charCount(codePoint)
        if (cursor > rawStart && cursor - rawStart + codePointLength > maximumSegmentLength) {
            flush(cursor)
            continue
        }

        cursor += codePointLength
        if (codePoint.isSentenceBoundary()) {
            if (!codePoint.isLineBreak()) {
                while (cursor < text.length) {
                    val closing = text.codePointAt(cursor)
                    val closingLength = Character.charCount(closing)
                    if (
                        !closing.isClosingSentencePunctuation() ||
                        cursor - rawStart + closingLength > maximumSegmentLength
                    ) {
                        break
                    }
                    cursor += closingLength
                }
            }
            flush(cursor)
        } else if (cursor - rawStart >= maximumSegmentLength) {
            flush(cursor)
        }
    }
    flush(text.length)
    return SpeechDocument(text, segments)
}

internal enum class SpeechPlaybackPhase {
    IDLE,
    PLAYING,
    PAUSED,
}

internal enum class SpeechResumePrecision {
    /** The engine did not report a range, so playback resumes at the sentence start. */
    SENTENCE,

    /** The engine reported the range currently being spoken. */
    RANGE,
}

internal data class SpeechPosition(
    val segmentIndex: Int,
    val sourceOffset: Int,
    val precision: SpeechResumePrecision,
)

internal data class SpeechSourceRange(
    val segmentIndex: Int,
    val sourceStart: Int,
    val sourceEndExclusive: Int,
)

/** Pure playback state. Android callbacks will be adapted to it in v0.8 lot A1. */
internal data class SpeechPlaybackModel(
    val phase: SpeechPlaybackPhase = SpeechPlaybackPhase.IDLE,
    val position: SpeechPosition? = null,
    val spokenRange: SpeechSourceRange? = null,
) {
    fun startAt(document: SpeechDocument, segmentIndex: Int = 0): SpeechPlaybackModel {
        val segment = document.segments.getOrNull(segmentIndex) ?: return stopped()
        return SpeechPlaybackModel(
            phase = SpeechPlaybackPhase.PLAYING,
            position = segment.sentencePosition(),
        )
    }

    fun markSegmentStarted(
        document: SpeechDocument,
        segmentIndex: Int,
        utteranceSourceStart: Int,
    ): SpeechPlaybackModel {
        if (phase != SpeechPlaybackPhase.PLAYING) return this
        val segment = document.segments.getOrNull(segmentIndex) ?: return this
        if (!segment.containsSourceOffset(utteranceSourceStart)) return this
        val current = position
        val precision = if (
            current?.segmentIndex == segmentIndex &&
            current.sourceOffset == utteranceSourceStart &&
            current.precision == SpeechResumePrecision.RANGE
        ) {
            SpeechResumePrecision.RANGE
        } else if (utteranceSourceStart == segment.sourceStart) {
            SpeechResumePrecision.SENTENCE
        } else {
            SpeechResumePrecision.RANGE
        }
        return copy(
            position = SpeechPosition(segmentIndex, utteranceSourceStart, precision),
            spokenRange = null,
        )
    }

    /**
     * Record a range reported relative to the utterance sent to Android.
     * Invalid or stale callbacks are ignored rather than corrupting the resume point.
     */
    fun markSpokenRange(
        document: SpeechDocument,
        segmentIndex: Int,
        utteranceSourceStart: Int,
        rangeStart: Int,
        rangeEndExclusive: Int,
    ): SpeechPlaybackModel {
        if (phase != SpeechPlaybackPhase.PLAYING || position?.segmentIndex != segmentIndex) return this
        val segment = document.segments.getOrNull(segmentIndex) ?: return this
        if (rangeStart < 0 || rangeEndExclusive <= rangeStart) return this
        val absoluteStart = utteranceSourceStart + rangeStart
        val absoluteEndExclusive = utteranceSourceStart + rangeEndExclusive
        if (
            absoluteStart < segment.sourceStart ||
            absoluteEndExclusive > segment.sourceEndExclusive
        ) {
            return this
        }
        return copy(
            position = SpeechPosition(
                segmentIndex = segmentIndex,
                sourceOffset = absoluteStart,
                precision = SpeechResumePrecision.RANGE,
            ),
            spokenRange = SpeechSourceRange(
                segmentIndex = segmentIndex,
                sourceStart = absoluteStart,
                sourceEndExclusive = absoluteEndExclusive,
            ),
        )
    }

    fun pause(): SpeechPlaybackModel = if (phase == SpeechPlaybackPhase.PLAYING) {
        copy(phase = SpeechPlaybackPhase.PAUSED)
    } else {
        this
    }

    fun resume(): SpeechPlaybackModel = if (
        phase == SpeechPlaybackPhase.PAUSED && position != null
    ) {
        copy(phase = SpeechPlaybackPhase.PLAYING)
    } else {
        this
    }

    fun markSegmentDone(document: SpeechDocument, segmentIndex: Int): SpeechPlaybackModel {
        if (phase != SpeechPlaybackPhase.PLAYING || position?.segmentIndex != segmentIndex) return this
        val next = document.segments.getOrNull(segmentIndex + 1) ?: return stopped()
        return copy(position = next.sentencePosition(), spokenRange = null)
    }

    fun stopped(): SpeechPlaybackModel = SpeechPlaybackModel()
}

private fun SpeechSegment.sentencePosition(): SpeechPosition = SpeechPosition(
    segmentIndex = index,
    sourceOffset = sourceStart,
    precision = SpeechResumePrecision.SENTENCE,
)

private fun Int.isSpeechWhitespace(): Boolean =
    Character.isWhitespace(this) || Character.isSpaceChar(this)

private fun Int.isLineBreak(): Boolean = this == '\n'.code || this == '\r'.code

private fun Int.isSentenceBoundary(): Boolean = when (this) {
    '\n'.code,
    '\r'.code,
    '。'.code,
    '！'.code,
    '？'.code,
    '!'.code,
    '?'.code,
    ';'.code,
    '；'.code,
    '.'.code,
    -> true
    else -> false
}

private fun Int.isClosingSentencePunctuation(): Boolean = when (this) {
    '"'.code,
    '\''.code,
    '”'.code,
    '’'.code,
    '」'.code,
    '』'.code,
    '》'.code,
    '）'.code,
    ')'.code,
    '】'.code,
    ']'.code,
    -> true
    else -> false
}
