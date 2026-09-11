package fr.kairossolum.pangmao.ui.common

import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

class MandarinSpeaker internal constructor() {
    internal var engine: TextToSpeech? = null
    internal var ready by mutableStateOf(false)

    fun speak(text: String) {
        val current = engine ?: return
        current.language = Locale.SIMPLIFIED_CHINESE
        current.speak(text, TextToSpeech.QUEUE_FLUSH, null, "pangmao-${text.hashCode()}")
    }
}

@Composable
fun rememberMandarinSpeaker(): MandarinSpeaker {
    val context = LocalContext.current
    val speaker = remember { MandarinSpeaker() }
    DisposableEffect(context) {
        speaker.engine = TextToSpeech(context.applicationContext) { status ->
            speaker.ready = status == TextToSpeech.SUCCESS
            if (speaker.ready) speaker.engine?.language = Locale.SIMPLIFIED_CHINESE
        }
        onDispose {
            speaker.engine?.stop()
            speaker.engine?.shutdown()
            speaker.engine = null
        }
    }
    return speaker
}

