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
    private var speechChunks: List<String> = emptyList()
    private var currentChunkIndex = 0
    private val mainHandler = Handler(Looper.getMainLooper())
    internal var engine: TextToSpeech? = null
    var status by mutableStateOf(SpeakerStatus.INITIALIZING)
        internal set
    var playbackState by mutableStateOf(SpeakerPlaybackState.IDLE)
        internal set

    val ready: Boolean
        get() = status == SpeakerStatus.READY

    fun speak(text: String): Boolean {
        val current = engine ?: return false
        if (!ready || text.isBlank()) return false
        val chunks = chunkSpeechText(text)
        if (chunks.isEmpty()) return false
        speechChunks = chunks
        currentChunkIndex = 0
        return queueFrom(current, currentChunkIndex)
    }

    fun pause(): Boolean {
        if (playbackState != SpeakerPlaybackState.PLAYING || speechChunks.isEmpty()) return false
        playbackGeneration += 1
        engine?.stop()
        playbackState = SpeakerPlaybackState.PAUSED
        return true
    }

    fun resume(): Boolean {
        val current = engine ?: return false
        if (!ready || playbackState != SpeakerPlaybackState.PAUSED || speechChunks.isEmpty()) {
            return false
        }
        return queueFrom(current, currentChunkIndex)
    }

    fun stop() {
        playbackGeneration += 1
        engine?.stop()
        speechChunks = emptyList()
        currentChunkIndex = 0
        playbackState = SpeakerPlaybackState.IDLE
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
        speechChunks = emptyList()
        currentChunkIndex = 0
        playbackState = SpeakerPlaybackState.IDLE
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

    private fun queueFrom(current: TextToSpeech, startIndex: Int): Boolean {
        if (startIndex !in speechChunks.indices) return false
        playbackGeneration += 1
        val generation = playbackGeneration
        current.stop()
        runCatching { current.setSpeechRate(speechRate) }
        for (index in startIndex..speechChunks.lastIndex) {
            val queueMode = if (index == startIndex) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            val result = current.speak(
                speechChunks[index],
                queueMode,
                null,
                utteranceId(generation, index),
            )
            if (result != TextToSpeech.SUCCESS) {
                playbackGeneration += 1
                current.stop()
                speechChunks = emptyList()
                currentChunkIndex = 0
                playbackState = SpeakerPlaybackState.IDLE
                return false
            }
        }
        currentChunkIndex = startIndex
        playbackState = SpeakerPlaybackState.PLAYING
        return true
    }

    private fun installProgressListener(current: TextToSpeech) {
        current.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    val progress = utteranceId?.toPlaybackProgress() ?: return
                    mainHandler.post {
                        if (progress.first == playbackGeneration) {
                            currentChunkIndex = progress.second
                            playbackState = SpeakerPlaybackState.PLAYING
                        }
                    }
                }

                override fun onDone(utteranceId: String?) {
                    val progress = utteranceId?.toPlaybackProgress() ?: return
                    mainHandler.post {
                        if (
                            progress.first == playbackGeneration &&
                            progress.second == speechChunks.lastIndex
                        ) {
                            speechChunks = emptyList()
                            currentChunkIndex = 0
                            playbackState = SpeakerPlaybackState.IDLE
                        }
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    val progress = utteranceId?.toPlaybackProgress() ?: return
                    mainHandler.post {
                        if (progress.first == playbackGeneration) {
                            speechChunks = emptyList()
                            currentChunkIndex = 0
                            playbackState = SpeakerPlaybackState.IDLE
                        }
                    }
                }

                override fun onStop(utteranceId: String?, interrupted: Boolean) {
                    val progress = utteranceId?.toPlaybackProgress() ?: return
                    mainHandler.post {
                        if (progress.first == playbackGeneration) {
                            speechChunks = emptyList()
                            currentChunkIndex = 0
                            playbackState = SpeakerPlaybackState.IDLE
                        }
                    }
                }
            }
        )
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

private fun utteranceId(generation: Long, chunkIndex: Int): String =
    "pangmao:$generation:$chunkIndex"

private fun String.toPlaybackProgress(): Pair<Long, Int>? {
    val components = split(':')
    if (components.size != 3 || components[0] != "pangmao") return null
    return components[1].toLongOrNull()?.let { generation ->
        components[2].toIntOrNull()?.let { index -> generation to index }
    }
}

internal fun chunkSpeechText(text: String, maximumLength: Int = 180): List<String> {
    require(maximumLength > 0)
    val chunks = mutableListOf<String>()
    val current = StringBuilder()

    fun flush() {
        current.toString().trim().takeIf(String::isNotEmpty)?.let(chunks::add)
        current.clear()
    }

    text.codePoints().forEachOrdered { codePoint ->
        current.appendCodePoint(codePoint)
        if (codePoint.isSpeechBoundary() || current.length >= maximumLength) flush()
    }
    flush()
    return chunks
}

private fun Int.isSpeechBoundary(): Boolean = when (this) {
    '\n'.code, '。'.code, '！'.code, '？'.code, '!'.code, '?'.code, '；'.code, ';'.code -> true
    else -> false
}

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
