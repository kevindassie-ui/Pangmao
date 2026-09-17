package fr.kairossolum.pangmao.domain.speech

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShortSpeechRecognitionTest {
    @Test
    fun `candidates are trimmed deduplicated and limited`() {
        assertEquals(
            listOf("你好", "您好", "你好吗"),
            normalizeSpeechCandidates(
                listOf(" 你好 ", "", "您好", "你好", "你好吗", "早上好"),
            ),
        )
    }

    @Test
    fun `partial and final results drive the capture state`() {
        val listening = SpeechCaptureState()
            .beginListening(SpeechRecognitionEngineMode.ON_DEVICE)
            .withPartialResults(listOf("你好"))

        assertEquals(SpeechCapturePhase.LISTENING, listening.phase)
        assertEquals("你好", listening.transcript)
        assertTrue(listening.isBusy)
        assertFalse(listening.canSubmit)

        val ready = listening.withFinalResults(listOf("你好", "您好"))

        assertEquals(SpeechCapturePhase.READY, ready.phase)
        assertEquals(listOf("你好", "您好"), ready.candidates)
        assertFalse(ready.isBusy)
        assertTrue(ready.canSubmit)
    }

    @Test
    fun `empty partial result does not erase an existing transcript`() {
        val initial = SpeechCaptureState(
            phase = SpeechCapturePhase.LISTENING,
            transcript = "已有文字",
        )

        assertEquals(initial, initial.withPartialResults(listOf(" ")))
    }

    @Test
    fun `manual correction clears stale alternatives and failure`() {
        val corrected = SpeechCaptureState(
            phase = SpeechCapturePhase.ERROR,
            transcript = "你号",
            candidates = listOf("你号", "你好"),
            failure = SpeechRecognitionFailure.NO_MATCH,
        ).edited("你好")

        assertEquals(SpeechCapturePhase.READY, corrected.phase)
        assertEquals("你好", corrected.transcript)
        assertTrue(corrected.candidates.isEmpty())
        assertNull(corrected.failure)
        assertTrue(corrected.canSubmit)
    }

    @Test
    fun `error retains transcript and clear retains selected engine`() {
        val failed = SpeechCaptureState(
            phase = SpeechCapturePhase.READY,
            engineMode = SpeechRecognitionEngineMode.SYSTEM,
            transcript = "中文",
            candidates = listOf("中文"),
        ).failed(SpeechRecognitionFailure.NETWORK)

        assertEquals("中文", failed.transcript)
        assertTrue(failed.canSubmit)

        val cleared = failed.cleared()
        assertEquals(SpeechCapturePhase.IDLE, cleared.phase)
        assertEquals("", cleared.transcript)
        assertEquals(SpeechRecognitionEngineMode.SYSTEM, cleared.engineMode)
        assertNull(cleared.failure)
    }

    @Test
    fun `processing state cannot be submitted`() {
        val processing = SpeechCaptureState(
            phase = SpeechCapturePhase.LISTENING,
            transcript = "你好",
        ).processing()

        assertEquals(SpeechCapturePhase.PROCESSING, processing.phase)
        assertTrue(processing.isBusy)
        assertFalse(processing.canSubmit)
    }
}
