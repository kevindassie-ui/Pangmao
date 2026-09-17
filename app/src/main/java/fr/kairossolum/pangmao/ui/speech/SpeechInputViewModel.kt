package fr.kairossolum.pangmao.ui.speech

import androidx.lifecycle.ViewModel
import fr.kairossolum.pangmao.domain.speech.ShortSpeechRecognitionCallback
import fr.kairossolum.pangmao.domain.speech.ShortSpeechRecognitionEngine
import fr.kairossolum.pangmao.domain.speech.ShortSpeechRecognitionRequest
import fr.kairossolum.pangmao.domain.speech.SpeechCapturePhase
import fr.kairossolum.pangmao.domain.speech.SpeechCaptureState
import fr.kairossolum.pangmao.domain.speech.SpeechRecognitionEngineMode
import fr.kairossolum.pangmao.domain.speech.SpeechRecognitionFailure
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SpeechInputViewModel(
    private val engine: ShortSpeechRecognitionEngine,
) : ViewModel() {
    private val _state = MutableStateFlow(
        SpeechCaptureState(engineMode = engine.mode)
    )
    val state: StateFlow<SpeechCaptureState> = _state.asStateFlow()

    private val callback = object : ShortSpeechRecognitionCallback {
        override fun onPartialResults(candidates: List<String>) {
            _state.update { it.withPartialResults(candidates) }
        }

        override fun onProcessing() {
            _state.update { current ->
                if (current.phase == SpeechCapturePhase.LISTENING) current.processing() else current
            }
        }

        override fun onFinalResults(candidates: List<String>) {
            _state.update { it.withFinalResults(candidates) }
        }

        override fun onFailure(failure: SpeechRecognitionFailure) {
            _state.update { it.failed(failure) }
        }
    }

    fun startCapture() {
        if (_state.value.isBusy) return
        if (engine.mode == SpeechRecognitionEngineMode.UNAVAILABLE) {
            _state.update { it.failed(SpeechRecognitionFailure.UNAVAILABLE) }
            return
        }
        _state.update { it.beginListening(engine.mode) }
        runCatching {
            engine.start(ShortSpeechRecognitionRequest(), callback)
        }.onFailure {
            _state.update { it.failed(SpeechRecognitionFailure.CLIENT) }
        }
    }

    fun stopCapture() {
        if (_state.value.phase != SpeechCapturePhase.LISTENING) return
        _state.update { it.processing() }
        engine.stop()
    }

    fun cancelCapture() {
        engine.cancel()
        _state.update { it.cancelled() }
    }

    fun reportPermissionDenied() {
        _state.update { it.failed(SpeechRecognitionFailure.PERMISSION_DENIED) }
    }

    fun editTranscript(value: String) {
        if (_state.value.isBusy) return
        _state.update { it.edited(value) }
    }

    fun selectCandidate(value: String) {
        if (_state.value.isBusy) return
        _state.update { it.selectCandidate(value) }
    }

    fun clear() {
        cancelCapture()
        _state.update { it.cleared() }
    }

    override fun onCleared() {
        engine.close()
    }
}
