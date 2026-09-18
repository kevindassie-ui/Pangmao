package fr.kairossolum.pangmao.ui.speech

import fr.kairossolum.pangmao.domain.speech.ShortSpeechRecognitionCallback
import fr.kairossolum.pangmao.domain.speech.ShortSpeechRecognitionEngine
import fr.kairossolum.pangmao.domain.speech.ShortSpeechRecognitionRequest
import fr.kairossolum.pangmao.domain.speech.SpeechCapturePhase
import fr.kairossolum.pangmao.domain.speech.SpeechRecognitionEngineMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechInputViewModelTest {
    @Test
    fun `system capture accepts activity results without starting embedded recognizer`() {
        val engine = FakeSpeechRecognitionEngine(SpeechRecognitionEngineMode.SYSTEM)
        val viewModel = SpeechInputViewModel(engine)

        assertTrue(viewModel.beginSystemCapture())
        assertEquals(SpeechCapturePhase.LISTENING, viewModel.state.value.phase)
        assertEquals(0, engine.startCount)

        viewModel.completeSystemCapture(listOf(" 你好 ", "您好"))

        assertEquals(SpeechCapturePhase.READY, viewModel.state.value.phase)
        assertEquals("你好", viewModel.state.value.transcript)
        assertEquals(listOf("你好", "您好"), viewModel.state.value.candidates)
        assertNull(viewModel.state.value.failure)
        assertEquals(0, engine.startCount)
    }

    @Test
    fun `cancelling system activity restores idle state and allows retry`() {
        val viewModel = SpeechInputViewModel(
            FakeSpeechRecognitionEngine(SpeechRecognitionEngineMode.SYSTEM),
        )

        assertTrue(viewModel.beginSystemCapture())
        assertFalse(viewModel.beginSystemCapture())

        viewModel.cancelSystemCapture()

        assertEquals(SpeechCapturePhase.IDLE, viewModel.state.value.phase)
        assertNull(viewModel.state.value.failure)
        assertTrue(viewModel.beginSystemCapture())
    }

    @Test
    fun `embedded capture is reserved for the on-device engine`() {
        val systemEngine = FakeSpeechRecognitionEngine(SpeechRecognitionEngineMode.SYSTEM)
        val systemViewModel = SpeechInputViewModel(systemEngine)

        systemViewModel.startOnDeviceCapture()

        assertEquals(SpeechCapturePhase.ERROR, systemViewModel.state.value.phase)
        assertEquals(0, systemEngine.startCount)

        val onDeviceEngine = FakeSpeechRecognitionEngine(SpeechRecognitionEngineMode.ON_DEVICE)
        val onDeviceViewModel = SpeechInputViewModel(onDeviceEngine)

        onDeviceViewModel.startOnDeviceCapture()

        assertEquals(SpeechCapturePhase.LISTENING, onDeviceViewModel.state.value.phase)
        assertEquals(1, onDeviceEngine.startCount)
    }
}

private class FakeSpeechRecognitionEngine(
    override val mode: SpeechRecognitionEngineMode,
) : ShortSpeechRecognitionEngine {
    var startCount: Int = 0

    override fun start(
        request: ShortSpeechRecognitionRequest,
        callback: ShortSpeechRecognitionCallback,
    ) {
        startCount += 1
    }

    override fun stop() = Unit

    override fun cancel() = Unit

    override fun close() = Unit
}
