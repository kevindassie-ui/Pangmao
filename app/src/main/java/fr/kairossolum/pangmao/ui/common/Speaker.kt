package fr.kairossolum.pangmao.ui.common

import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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

class MandarinSpeaker internal constructor() {
    private var initializationGeneration = 0
    internal var engine: TextToSpeech? = null
    var status by mutableStateOf(SpeakerStatus.INITIALIZING)
        internal set

    val ready: Boolean
        get() = status == SpeakerStatus.READY

    fun speak(text: String): Boolean {
        val current = engine ?: return false
        if (!ready || text.isBlank()) return false
        val queued = current.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "pangmao-${text.hashCode()}",
        ) == TextToSpeech.SUCCESS
        if (!queued) status = SpeakerStatus.ERROR
        return queued
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
        engine?.stop()
        engine?.shutdown()
        engine = null
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
        return languageAvailable || selectedVoice != null
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
