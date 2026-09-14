package fr.kairossolum.pangmao.domain.speech

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class SpeechPlaybackModelTest {
    @Test
    fun `segments preserve source offsets while ignoring boundary whitespace`() {
        val source = "  你好。 \n今天学习！  明天继续  "

        val document = buildSpeechDocument(source)

        assertEquals(listOf("你好。", "今天学习！", "明天继续"), document.segments.map { it.text })
        document.segments.forEach { segment ->
            assertEquals(
                segment.text,
                source.substring(segment.sourceStart, segment.sourceEndExclusive),
            )
        }
        assertEquals(1, document.segmentAtSourceOffset(source.indexOf("学"))?.index)
        assertNull(document.segmentAtSourceOffset(source.indexOf('\n')))
    }

    @Test
    fun `closing punctuation stays with its sentence`() {
        val document = buildSpeechDocument("他说：“你好！”然后走。")

        assertEquals(listOf("他说：“你好！”", "然后走。"), document.segments.map { it.text })
    }

    @Test
    fun `hard limits never split a supplementary character`() {
        val document = buildSpeechDocument("学😀习。下一句", maximumSegmentLength = 3)

        assertEquals(listOf("学😀", "习。", "下一句"), document.segments.map { it.text })
        assertEquals(listOf(0, 3, 5), document.segments.map { it.sourceStart })
        assertEquals(listOf(3, 5, 8), document.segments.map { it.sourceEndExclusive })
        document.segments.forEach { segment ->
            assertEquals(
                segment.text,
                document.source.substring(segment.sourceStart, segment.sourceEndExclusive),
            )
        }
    }

    @Test
    fun `pause before a range resumes at the current sentence`() {
        val document = buildSpeechDocument("第一句。第二句。")
        val started = SpeechPlaybackModel().startAt(document, segmentIndex = 1)

        val paused = started.pause()
        val resumed = paused.resume()

        assertEquals(SpeechPlaybackPhase.PAUSED, paused.phase)
        assertEquals(document.segments[1].sourceStart, paused.position?.sourceOffset)
        assertEquals(SpeechResumePrecision.SENTENCE, paused.position?.precision)
        assertEquals(SpeechPlaybackPhase.PLAYING, resumed.phase)
        assertEquals(paused.position, resumed.position)
    }

    @Test
    fun `reported range becomes the precise resume point`() {
        val document = buildSpeechDocument("第一句。今天学习中文。")
        val segment = document.segments[1]
        val playing = SpeechPlaybackModel().startAt(document, segment.index)

        val progressed = playing.markSpokenRange(
            document = document,
            segmentIndex = segment.index,
            utteranceSourceStart = segment.sourceStart,
            rangeStart = 2,
            rangeEndExclusive = 4,
        )
        val paused = progressed.pause()

        assertEquals(segment.sourceStart + 2, paused.position?.sourceOffset)
        assertEquals(SpeechResumePrecision.RANGE, paused.position?.precision)
        assertEquals(
            SpeechSourceRange(segment.index, segment.sourceStart + 2, segment.sourceStart + 4),
            paused.spokenRange,
        )
    }

    @Test
    fun `ranges from a resumed suffix map back to absolute source offsets`() {
        val document = buildSpeechDocument("今天学习中文。")
        val segment = document.segments.single()
        val suffixStart = segment.sourceStart + 2
        val resumed = SpeechPlaybackModel()
            .startAt(document)
            .markSegmentStarted(document, segment.index, suffixStart)

        val progressed = resumed.markSpokenRange(
            document = document,
            segmentIndex = segment.index,
            utteranceSourceStart = suffixStart,
            rangeStart = 1,
            rangeEndExclusive = 3,
        )

        assertEquals(suffixStart + 1, progressed.position?.sourceOffset)
        assertEquals(suffixStart + 3, progressed.spokenRange?.sourceEndExclusive)
    }

    @Test
    fun `queue starts with the tracked suffix then continues with whole sentences`() {
        val document = buildSpeechDocument("第一句。今天学习中文。明天继续。")
        val second = document.segments[1]
        val position = SpeechPosition(
            segmentIndex = second.index,
            sourceOffset = second.sourceStart + 2,
            precision = SpeechResumePrecision.RANGE,
        )

        val utterances = document.utterancesFrom(position)

        assertEquals(listOf("学习中文。", "明天继续。"), utterances.map { it.text })
        assertEquals(second.sourceStart + 2, utterances.first().sourceStart)
        assertEquals(second.index, utterances.first().segmentIndex)
        assertEquals(2, utterances.size)
    }

    @Test
    fun `completion advances by sentence and final completion stops`() {
        val document = buildSpeechDocument("第一句。第二句。")
        val first = SpeechPlaybackModel().startAt(document)

        val second = first.markSegmentDone(document, segmentIndex = 0)
        val done = second.markSegmentDone(document, segmentIndex = 1)

        assertEquals(SpeechPlaybackPhase.PLAYING, second.phase)
        assertEquals(1, second.position?.segmentIndex)
        assertEquals(SpeechResumePrecision.SENTENCE, second.position?.precision)
        assertEquals(SpeechPlaybackPhase.IDLE, done.phase)
        assertNull(done.position)
    }

    @Test
    fun `invalid or stale progress cannot move the cursor`() {
        val document = buildSpeechDocument("第一句。第二句。")
        val playing = SpeechPlaybackModel().startAt(document)

        val stale = playing.markSpokenRange(
            document = document,
            segmentIndex = 1,
            utteranceSourceStart = document.segments[1].sourceStart,
            rangeStart = 0,
            rangeEndExclusive = 1,
        )
        val outside = playing.markSpokenRange(
            document = document,
            segmentIndex = 0,
            utteranceSourceStart = document.segments[0].sourceStart,
            rangeStart = 0,
            rangeEndExclusive = document.segments[0].text.length + 1,
        )
        val invalidBase = playing.markSpokenRange(
            document = document,
            segmentIndex = 0,
            utteranceSourceStart = document.segments[0].sourceStart - 1,
            rangeStart = 1,
            rangeEndExclusive = 2,
        )

        assertSame(playing, stale)
        assertSame(playing, outside)
        assertSame(playing, invalidBase)
    }
}
