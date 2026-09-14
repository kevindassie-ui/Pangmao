package fr.kairossolum.pangmao.ui.common

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import fr.kairossolum.pangmao.domain.speech.SpeechDocument
import fr.kairossolum.pangmao.domain.speech.SpeechPlaybackModel
import fr.kairossolum.pangmao.domain.speech.SpeechPlaybackPhase
import fr.kairossolum.pangmao.domain.speech.SpeechPosition
import fr.kairossolum.pangmao.domain.speech.buildSpeechDocument
import fr.kairossolum.pangmao.domain.speech.utterancesFrom
import java.util.Locale

enum class SpeakerStatus {
    INITIALIZING,
    READY,
    MISSING_CHINESE_VOICE,
    ERROR,
}

enum class SpeakerPlaybackState {
    IDLE,
    PLAYING,
    PAUSED,
}

class MandarinSpeaker internal constructor() {
    private var initializationGeneration = 0
    private var playbackGeneration = 0L
    private var speechRate = 1.0f
    private var speechDocument: SpeechDocument? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    internal var engine: TextToSpeech? = null
    var status by mutableStateOf(SpeakerStatus.INITIALIZING)
        internal set
    var playbackState by mutableStateOf(SpeakerPlaybackState.IDLE)
        internal set
    internal var playbackModel by mutableStateOf(SpeechPlaybackModel())
        private set

    val ready: Boolean
        get() = status == SpeakerStatus.READY

    fun speak(text: String): Boolean {
        val current = engine ?: return false
        if (!ready || text.isBlank()) return false
        val document = buildSpeechDocument(text)
        if (document.segments.isEmpty()) return false
        speechDocument = document
        updatePlayback(SpeechPlaybackModel().startAt(document))
        return queueFrom(current, document, checkNotNull(playbackModel.position))
    }

    fun pause(): Boolean {
        val current = engine ?: return false
        if (playbackModel.phase != SpeechPlaybackPhase.PLAYING || speechDocument == null) return false
        playbackGeneration += 1
        current.stop()
        updatePlayback(playbackModel.pause())
        return true
    }

    fun resume(): Boolean {
        val current = engine ?: return false
        val document = speechDocument
        if (
            !ready ||
            playbackModel.phase != SpeechPlaybackPhase.PAUSED ||
            document == null ||
            playbackModel.position == null
        ) {
            return false
        }
        updatePlayback(playbackModel.resume())
        return queueFrom(current, document, checkNotNull(playbackModel.position))
    }

    fun stop() {
        playbackGeneration += 1
        engine?.stop()
        clearPlayback()
    }

    fun setSpeechRate(value: Float) {
        speechRate = value.coerceIn(0.5f, 2.0f)
        engine?.let { current -> runCatching { current.setSpeechRate(speechRate) } }
    }

    fun retry(context: Context) = restart(context)

    fun openVoiceSettings(context: Context) {
        val settings = Intent("com.android.settings.TTS_SETTINGS")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(settings) }
            .recoverCatching {
                context.startActivity(
                    Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
    }

    internal fun restart(context: Context) {
        closeEngine()
        status = SpeakerStatus.INITIALIZING
        val generation = initializationGeneration
        val applicationContext = context.applicationContext
        val candidates = buildList<String?> {
            add(null)
            addAll(discoverTtsEngines(applicationContext))
        }
        tryEngine(applicationContext, generation, candidates, candidateIndex = 0, hadWorkingEngine = false)
    }

    private fun tryEngine(
        context: Context,
        generation: Int,
        candidates: List<String?>,
        candidateIndex: Int,
        hadWorkingEngine: Boolean,
    ) {
        if (generation != initializationGeneration) return
        if (candidateIndex >= candidates.size) {
            engine = null
            status = if (hadWorkingEngine) SpeakerStatus.MISSING_CHINESE_VOICE else SpeakerStatus.ERROR
            return
        }

        var createdEngine: TextToSpeech? = null
        var earlyStatus: Int? = null
        val listener = TextToSpeech.OnInitListener { statusCode ->
            if (generation != initializationGeneration) {
                createdEngine?.shutdown()
            } else if (createdEngine == null) {
                earlyStatus = statusCode
            } else {
                engine = createdEngine
                handleInitialization(
                    context = context,
                    generation = generation,
                    candidates = candidates,
                    candidateIndex = candidateIndex,
                    hadWorkingEngine = hadWorkingEngine,
                    createdEngine = checkNotNull(createdEngine),
                    statusCode = statusCode,
                )
            }
        }
        createdEngine = try {
            candidates[candidateIndex]?.let { packageName ->
                TextToSpeech(context, listener, packageName)
            } ?: TextToSpeech(context, listener)
        } catch (_: Throwable) {
            tryEngine(context, generation, candidates, candidateIndex + 1, hadWorkingEngine)
            return
        }
        if (generation != initializationGeneration) {
            createdEngine?.shutdown()
            return
        }
        engine = createdEngine
        earlyStatus?.let { statusCode ->
            handleInitialization(
                context = context,
                generation = generation,
                candidates = candidates,
                candidateIndex = candidateIndex,
                hadWorkingEngine = hadWorkingEngine,
                createdEngine = checkNotNull(createdEngine),
                statusCode = statusCode,
            )
        }
    }

    internal fun close() {
        closeEngine()
        status = SpeakerStatus.INITIALIZING
    }

    private fun closeEngine() {
        initializationGeneration += 1
        playbackGeneration += 1
        engine?.stop()
        engine?.shutdown()
        engine = null
        clearPlayback()
    }

    private fun handleInitialization(
        context: Context,
        generation: Int,
        candidates: List<String?>,
        candidateIndex: Int,
        hadWorkingEngine: Boolean,
        createdEngine: TextToSpeech,
        statusCode: Int,
    ) {
        if (generation != initializationGeneration || engine !== createdEngine) {
            createdEngine.shutdown()
            return
        }
        val initialized = statusCode == TextToSpeech.SUCCESS
        if (initialized && configureMandarin(createdEngine)) {
            status = SpeakerStatus.READY
            return
        }
        createdEngine.stop()
        createdEngine.shutdown()
        engine = null
        tryEngine(
            context = context,
            generation = generation,
            candidates = candidates,
            candidateIndex = candidateIndex + 1,
            hadWorkingEngine = hadWorkingEngine || initialized,
        )
    }

    private fun configureMandarin(current: TextToSpeech): Boolean {
        val compatible = runCatching { current.voices.orEmpty() }
            .getOrDefault(emptySet())
            .filter { voice -> isMandarinLocale(voice.locale) && voice.isInstalled }
            .sortedWith(chineseVoicePreference)
        val languageAvailable = mandarinLocales.any { locale ->
            runCatching { isTtsLanguageResultUsable(current.setLanguage(locale)) }
                .getOrDefault(false)
        }
        val selectedVoice = compatible.firstOrNull { voice ->
            runCatching { current.setVoice(voice) == TextToSpeech.SUCCESS }.getOrDefault(false)
        }
        val configured = languageAvailable || selectedVoice != null
        if (configured) {
            runCatching { current.setSpeechRate(speechRate) }
            installProgressListener(current)
        }
        return configured
    }

    private fun queueFrom(
        current: TextToSpeech,
        document: SpeechDocument,
        position: SpeechPosition,
    ): Boolean {
        val utterances = document.utterancesFrom(position)
        if (utterances.isEmpty()) {
            clearPlayback()
            return false
        }
        playbackGeneration += 1
        val generation = playbackGeneration
        current.stop()
        runCatching { current.setSpeechRate(speechRate) }
        utterances.forEachIndexed { queueIndex, utterance ->
            val queueMode = if (queueIndex == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            val result = current.speak(
                utterance.text,
                queueMode,
                null,
                utteranceId(generation, utterance.segmentIndex, utterance.sourceStart),
            )
            if (result != TextToSpeech.SUCCESS) {
                playbackGeneration += 1
                current.stop()
                clearPlayback()
                return false
            }
        }
        val first = utterances.first()
        updatePlayback(
            playbackModel.markSegmentStarted(
                document = document,
                segmentIndex = first.segmentIndex,
                utteranceSourceStart = first.sourceStart,
            )
        )
        return true
    }

    private fun installProgressListener(current: TextToSpeech) {
        current.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    val progress = utteranceId?.toPlaybackProgress() ?: return
                    mainHandler.post {
                        val document = speechDocument
                        if (progress.generation != playbackGeneration || document == null) return@post
                        updatePlayback(
                            playbackModel.markSegmentStarted(
                                document = document,
                                segmentIndex = progress.segmentIndex,
                                utteranceSourceStart = progress.sourceStart,
                            )
                        )
                    }
                }

                override fun onRangeStart(
                    utteranceId: String?,
                    start: Int,
                    end: Int,
                    frame: Int,
                ) {
                    val progress = utteranceId?.toPlaybackProgress() ?: return
                    mainHandler.post {
                        val document = speechDocument
                        if (progress.generation != playbackGeneration || document == null) return@post
                        updatePlayback(
                            playbackModel.markSpokenRange(
                                document = document,
                                segmentIndex = progress.segmentIndex,
                                utteranceSourceStart = progress.sourceStart,
                                rangeStart = start,
                                rangeEndExclusive = end,
                            )
                        )
                    }
                }

                override fun onDone(utteranceId: String?) {
                    val progress = utteranceId?.toPlaybackProgress() ?: return
                    mainHandler.post {
                        val document = speechDocument
                        if (progress.generation != playbackGeneration || document == null) return@post
                        val updated = playbackModel.markSegmentDone(document, progress.segmentIndex)
                        updatePlayback(updated)
                        if (updated.phase == SpeechPlaybackPhase.IDLE) {
                            speechDocument = null
                        }
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    val progress = utteranceId?.toPlaybackProgress() ?: return
                    mainHandler.post {
                        if (progress.generation == playbackGeneration) {
                            clearPlayback()
                        }
                    }
                }

                override fun onStop(utteranceId: String?, interrupted: Boolean) {
                    val progress = utteranceId?.toPlaybackProgress() ?: return
                    mainHandler.post {
                        if (progress.generation == playbackGeneration) {
                            clearPlayback()
                        }
                    }
                }
            }
        )
    }

    private fun updatePlayback(updated: SpeechPlaybackModel) {
        playbackModel = updated
        playbackState = when (updated.phase) {
            SpeechPlaybackPhase.IDLE -> SpeakerPlaybackState.IDLE
            SpeechPlaybackPhase.PLAYING -> SpeakerPlaybackState.PLAYING
            SpeechPlaybackPhase.PAUSED -> SpeakerPlaybackState.PAUSED
        }
    }

    private fun clearPlayback() {
        speechDocument = null
        updatePlayback(playbackModel.stopped())
    }

    private companion object {
        val mandarinLocales = listOf(
            Locale.SIMPLIFIED_CHINESE,
            Locale.forLanguageTag("cmn-CN"),
            Locale.CHINESE,
            Locale.TRADITIONAL_CHINESE,
        )

        val chineseVoicePreference: Comparator<Voice> =
            compareBy<Voice> { it.isNetworkConnectionRequired }
                .thenByDescending { it.locale.country.equals(Locale.CHINA.country, ignoreCase = true) }
                .thenByDescending { it.quality }
                .thenBy { it.latency }
    }
}

internal data class PlaybackProgress(
    val generation: Long,
    val segmentIndex: Int,
    val sourceStart: Int,
)

internal fun utteranceId(generation: Long, segmentIndex: Int, sourceStart: Int): String =
    "pangmao:$generation:$segmentIndex:$sourceStart"

internal fun String.toPlaybackProgress(): PlaybackProgress? {
    val components = split(':')
    if (components.size != 4 || components[0] != "pangmao") return null
    val generation = components[1].toLongOrNull() ?: return null
    val segmentIndex = components[2].toIntOrNull() ?: return null
    val sourceStart = components[3].toIntOrNull() ?: return null
    if (generation < 0 || segmentIndex < 0 || sourceStart < 0) return null
    return PlaybackProgress(generation, segmentIndex, sourceStart)
}

internal fun chunkSpeechText(text: String, maximumLength: Int = 180): List<String> =
    buildSpeechDocument(text, maximumLength).segments.map { it.text }

private val Voice.isInstalled: Boolean
    get() = features?.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) != true

internal fun isMandarinLocale(locale: Locale): Boolean =
    locale.language.equals("zh", ignoreCase = true) ||
        locale.language.equals("cmn", ignoreCase = true)

internal fun isTtsLanguageResultUsable(result: Int): Boolean = result >= TextToSpeech.LANG_AVAILABLE

@Suppress("DEPRECATION")
private fun discoverTtsEngines(context: Context): List<String> =
    context.packageManager
        .queryIntentServices(Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE), 0)
        .mapNotNull { it.serviceInfo?.packageName }
        .distinct()

@Composable
fun rememberMandarinSpeaker(): MandarinSpeaker {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val speaker = remember { MandarinSpeaker() }
    val speechRate = LocalSpeechRate.current
    LaunchedEffect(speechRate) {
        speaker.setSpeechRate(speechRate.multiplier)
    }
    DisposableEffect(context, lifecycleOwner) {
        speaker.restart(context)
        val observer = LifecycleEventObserver { _, event ->
            if (
                event == Lifecycle.Event.ON_RESUME &&
                speaker.status != SpeakerStatus.READY &&
                speaker.status != SpeakerStatus.INITIALIZING
            ) {
                speaker.restart(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            speaker.close()
        }
    }
    return speaker
}
