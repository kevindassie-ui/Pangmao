package fr.kairossolum.pangmao.data.speech

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import fr.kairossolum.pangmao.domain.speech.ShortSpeechRecognitionCallback
import fr.kairossolum.pangmao.domain.speech.ShortSpeechRecognitionEngine
import fr.kairossolum.pangmao.domain.speech.ShortSpeechRecognitionRequest
import fr.kairossolum.pangmao.domain.speech.SpeechRecognitionEngineMode
import fr.kairossolum.pangmao.domain.speech.SpeechRecognitionFailure

class AndroidShortSpeechRecognitionEngine private constructor(
    override val mode: SpeechRecognitionEngineMode,
    private val recognizer: SpeechRecognizer?,
) : ShortSpeechRecognitionEngine {
    private var callback: ShortSpeechRecognitionCallback? = null
    private var isClosed = false

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            callback?.onReady()
        }

        override fun onBeginningOfSpeech() {
            callback?.onSpeechStarted()
        }

        override fun onRmsChanged(rmsdB: Float) = Unit

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() {
            callback?.onProcessing()
        }

        override fun onError(error: Int) {
            val activeCallback = callback
            callback = null
            activeCallback?.onFailure(error.toSpeechRecognitionFailure())
        }

        override fun onResults(results: Bundle?) {
            val activeCallback = callback
            callback = null
            activeCallback?.onFinalResults(results.recognitionCandidates())
        }

        override fun onPartialResults(partialResults: Bundle?) {
            callback?.onPartialResults(partialResults.recognitionCandidates())
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    override fun start(
        request: ShortSpeechRecognitionRequest,
        callback: ShortSpeechRecognitionCallback,
    ) {
        val activeRecognizer = recognizer
        if (isClosed || activeRecognizer == null || mode == SpeechRecognitionEngineMode.UNAVAILABLE) {
            callback.onFailure(SpeechRecognitionFailure.UNAVAILABLE)
            return
        }

        this.callback = callback
        activeRecognizer.setRecognitionListener(listener)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, request.languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, request.maxResults)
        }
        try {
            activeRecognizer.startListening(intent)
        } catch (_: SecurityException) {
            this.callback = null
            callback.onFailure(SpeechRecognitionFailure.PERMISSION_DENIED)
        } catch (_: Throwable) {
            this.callback = null
            callback.onFailure(SpeechRecognitionFailure.CLIENT)
        }
    }

    override fun stop() {
        if (!isClosed) runCatching { recognizer?.stopListening() }
    }

    override fun cancel() {
        callback = null
        if (!isClosed) runCatching { recognizer?.cancel() }
    }

    override fun close() {
        if (isClosed) return
        isClosed = true
        callback = null
        runCatching { recognizer?.destroy() }
    }

    companion object {
        fun create(context: Context): AndroidShortSpeechRecognitionEngine {
            val applicationContext = context.applicationContext
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                SpeechRecognizer.isOnDeviceRecognitionAvailable(applicationContext)
            ) {
                runCatching {
                    SpeechRecognizer.createOnDeviceSpeechRecognizer(applicationContext)
                }.getOrNull()?.let { recognizer ->
                    return AndroidShortSpeechRecognitionEngine(
                        mode = SpeechRecognitionEngineMode.ON_DEVICE,
                        recognizer = recognizer,
                    )
                }
            }

            if (SpeechRecognizer.isRecognitionAvailable(applicationContext)) {
                runCatching {
                    SpeechRecognizer.createSpeechRecognizer(applicationContext)
                }.getOrNull()?.let { recognizer ->
                    return AndroidShortSpeechRecognitionEngine(
                        mode = SpeechRecognitionEngineMode.SYSTEM,
                        recognizer = recognizer,
                    )
                }
            }

            return AndroidShortSpeechRecognitionEngine(
                mode = SpeechRecognitionEngineMode.UNAVAILABLE,
                recognizer = null,
            )
        }
    }
}

private fun Bundle?.recognitionCandidates(): List<String> =
    this?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()

private fun Int.toSpeechRecognitionFailure(): SpeechRecognitionFailure = when (this) {
    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> SpeechRecognitionFailure.PERMISSION_DENIED
    SpeechRecognizer.ERROR_NO_MATCH -> SpeechRecognitionFailure.NO_MATCH
    SpeechRecognizer.ERROR_NETWORK,
    SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
    SpeechRecognizer.ERROR_SERVER,
    SpeechRecognizer.ERROR_SERVER_DISCONNECTED,
    -> SpeechRecognitionFailure.NETWORK
    SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
    SpeechRecognizer.ERROR_TOO_MANY_REQUESTS,
    -> SpeechRecognitionFailure.BUSY
    SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
    SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE,
    -> SpeechRecognitionFailure.UNAVAILABLE
    SpeechRecognizer.ERROR_AUDIO -> SpeechRecognitionFailure.AUDIO
    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> SpeechRecognitionFailure.TIMEOUT
    SpeechRecognizer.ERROR_CLIENT -> SpeechRecognitionFailure.CLIENT
    else -> SpeechRecognitionFailure.UNKNOWN
}
