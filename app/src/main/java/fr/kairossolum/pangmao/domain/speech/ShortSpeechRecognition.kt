package fr.kairossolum.pangmao.domain.speech

data class ShortSpeechRecognitionRequest(
    val languageTag: String = "zh-CN",
    val maxResults: Int = 3,
)

enum class SpeechRecognitionEngineMode {
    ON_DEVICE,
    SYSTEM,
    UNAVAILABLE,
}

enum class SpeechCapturePhase {
    IDLE,
    LISTENING,
    PROCESSING,
    READY,
    ERROR,
}

enum class SpeechRecognitionFailure {
    PERMISSION_DENIED,
    NO_MATCH,
    NETWORK,
    BUSY,
    UNAVAILABLE,
    AUDIO,
    TIMEOUT,
    CLIENT,
    UNKNOWN,
}

interface ShortSpeechRecognitionCallback {
    fun onReady() = Unit
    fun onSpeechStarted() = Unit
    fun onPartialResults(candidates: List<String>) = Unit
    fun onProcessing() = Unit
    fun onFinalResults(candidates: List<String>) = Unit
    fun onFailure(failure: SpeechRecognitionFailure) = Unit
}

interface ShortSpeechRecognitionEngine : AutoCloseable {
    val mode: SpeechRecognitionEngineMode

    fun start(
        request: ShortSpeechRecognitionRequest,
        callback: ShortSpeechRecognitionCallback,
    )

    fun stop()
    fun cancel()
    override fun close()
}

data class SpeechCaptureState(
    val phase: SpeechCapturePhase = SpeechCapturePhase.IDLE,
    val engineMode: SpeechRecognitionEngineMode = SpeechRecognitionEngineMode.UNAVAILABLE,
    val transcript: String = "",
    val candidates: List<String> = emptyList(),
    val failure: SpeechRecognitionFailure? = null,
) {
    val isBusy: Boolean
        get() = phase == SpeechCapturePhase.LISTENING || phase == SpeechCapturePhase.PROCESSING

    val canSubmit: Boolean
        get() = transcript.isNotBlank() && !isBusy

    fun beginListening(mode: SpeechRecognitionEngineMode): SpeechCaptureState = copy(
        phase = SpeechCapturePhase.LISTENING,
        engineMode = mode,
        candidates = emptyList(),
        failure = null,
    )

    fun withPartialResults(values: List<String>): SpeechCaptureState {
        val normalized = normalizeSpeechCandidates(values)
        if (normalized.isEmpty()) return this
        return copy(
            phase = SpeechCapturePhase.LISTENING,
            transcript = normalized.first(),
            candidates = normalized,
            failure = null,
        )
    }

    fun processing(): SpeechCaptureState = copy(
        phase = SpeechCapturePhase.PROCESSING,
        failure = null,
    )

    fun withFinalResults(values: List<String>): SpeechCaptureState {
        val normalized = normalizeSpeechCandidates(values)
        return if (normalized.isEmpty()) {
            failed(SpeechRecognitionFailure.NO_MATCH)
        } else {
            copy(
                phase = SpeechCapturePhase.READY,
                transcript = normalized.first(),
                candidates = normalized,
                failure = null,
            )
        }
    }

    fun failed(value: SpeechRecognitionFailure): SpeechCaptureState = copy(
        phase = SpeechCapturePhase.ERROR,
        failure = value,
    )

    fun edited(value: String): SpeechCaptureState = copy(
        phase = if (value.isBlank()) SpeechCapturePhase.IDLE else SpeechCapturePhase.READY,
        transcript = value,
        candidates = emptyList(),
        failure = null,
    )

    fun selectCandidate(value: String): SpeechCaptureState {
        val normalized = value.trim()
        if (normalized.isEmpty()) return this
        return copy(
            phase = SpeechCapturePhase.READY,
            transcript = normalized,
            failure = null,
        )
    }

    fun cancelled(): SpeechCaptureState = copy(
        phase = if (transcript.isBlank()) SpeechCapturePhase.IDLE else SpeechCapturePhase.READY,
        failure = null,
    )

    fun cleared(): SpeechCaptureState = copy(
        phase = SpeechCapturePhase.IDLE,
        transcript = "",
        candidates = emptyList(),
        failure = null,
    )
}

fun normalizeSpeechCandidates(values: List<String>, maximum: Int = 3): List<String> = values
    .asSequence()
    .map(String::trim)
    .filter(String::isNotEmpty)
    .distinct()
    .take(maximum.coerceAtLeast(0))
    .toList()
