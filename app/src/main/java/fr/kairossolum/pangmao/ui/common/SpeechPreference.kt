package fr.kairossolum.pangmao.ui.common

import androidx.compose.runtime.staticCompositionLocalOf
import fr.kairossolum.pangmao.data.settings.SpeechRate

val LocalSpeechRate = staticCompositionLocalOf { SpeechRate.NORMAL }
