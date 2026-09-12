package fr.kairossolum.pangmao.ui.common

import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

enum class SpeakerStatus {
    INITIALIZING,
    READY,
    MISSING_CHINESE_VOICE,
    ERROR,
}

class MandarinSpeaker internal constructor() {
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

    fun openVoiceInstallation(context: Context) {
        val install = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(install) }
            .recoverCatching {
                context.startActivity(
                    Intent("com.android.settings.TTS_SETTINGS")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
    }

    internal fun initialize(statusCode: Int) {
        val current = engine
        if (statusCode != TextToSpeech.SUCCESS || current == null) {
            status = SpeakerStatus.ERROR
            return
        }
        val availability = current.setLanguage(Locale.SIMPLIFIED_CHINESE)
        if (availability < TextToSpeech.LANG_AVAILABLE) {
            status = SpeakerStatus.MISSING_CHINESE_VOICE
            return
        }
        val compatible = current.voices.orEmpty().filter { it.locale.language == Locale.CHINESE.language }
        val offline = compatible.filterNot { it.isNetworkConnectionRequired }
        (offline.ifEmpty { compatible })
            .sortedWith(compareByDescending<android.speech.tts.Voice> { it.quality }.thenBy { it.latency })
            .firstOrNull()
            ?.let { current.voice = it }
        status = SpeakerStatus.READY
    }
}

@Composable
fun rememberMandarinSpeaker(): MandarinSpeaker {
    val context = LocalContext.current
    val speaker = remember { MandarinSpeaker() }
    DisposableEffect(context) {
        speaker.status = SpeakerStatus.INITIALIZING
        speaker.engine = TextToSpeech(context.applicationContext) { status ->
            speaker.initialize(status)
        }
        onDispose {
            speaker.engine?.stop()
            speaker.engine?.shutdown()
            speaker.engine = null
            speaker.status = SpeakerStatus.INITIALIZING
        }
    }
    return speaker
}
