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
        return current.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "pangmao-${text.hashCode()}",
        ) == TextToSpeech.SUCCESS
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
        var createdEngine: TextToSpeech? = null
        var earlyStatus: Int? = null
        createdEngine = TextToSpeech(context.applicationContext) { statusCode ->
            if (generation != initializationGeneration) return@TextToSpeech
            if (createdEngine == null) {
                earlyStatus = statusCode
            } else {
                engine = createdEngine
                initialize(statusCode)
            }
        }
        if (generation != initializationGeneration) {
            createdEngine?.shutdown()
            return
        }
        engine = createdEngine
        earlyStatus?.let(::initialize)
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

    internal fun initialize(statusCode: Int) {
        val current = engine
        if (statusCode != TextToSpeech.SUCCESS || current == null) {
            status = SpeakerStatus.ERROR
            return
        }
        val compatible = runCatching { current.voices.orEmpty() }
            .getOrDefault(emptySet())
            .filter { voice ->
                voice.locale.language.equals(Locale.CHINESE.language, ignoreCase = true) &&
                    voice.features?.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) != true
            }
            .sortedWith(chineseVoicePreference)
        val selectedVoice = compatible.firstOrNull { voice ->
            runCatching { current.setVoice(voice) == TextToSpeech.SUCCESS }.getOrDefault(false)
        }
        if (selectedVoice != null) {
            status = SpeakerStatus.READY
            return
        }

        val languageAvailable = listOf(Locale.SIMPLIFIED_CHINESE, Locale.CHINA, Locale.CHINESE)
            .any { locale ->
                runCatching { current.setLanguage(locale) >= TextToSpeech.LANG_AVAILABLE }
                    .getOrDefault(false)
            }
        status = when {
            languageAvailable -> SpeakerStatus.READY
            compatible.isNotEmpty() -> SpeakerStatus.ERROR
            else -> SpeakerStatus.MISSING_CHINESE_VOICE
        }
    }

    private companion object {
        val chineseVoicePreference: Comparator<Voice> =
            compareBy<Voice> { it.isNetworkConnectionRequired }
                .thenByDescending { it.locale.country.equals(Locale.CHINA.country, ignoreCase = true) }
                .thenByDescending { it.quality }
                .thenBy { it.latency }
    }
}

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
