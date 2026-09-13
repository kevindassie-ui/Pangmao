package fr.kairossolum.pangmao.ui.common

import androidx.compose.runtime.staticCompositionLocalOf
import fr.kairossolum.pangmao.domain.model.LearningLanguage
import fr.kairossolum.pangmao.domain.model.presentation

val LocalLearningProfile = staticCompositionLocalOf { LearningLanguage.CHINESE.presentation }
