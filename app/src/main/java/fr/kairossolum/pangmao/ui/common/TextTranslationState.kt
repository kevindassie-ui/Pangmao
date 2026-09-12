package fr.kairossolum.pangmao.ui.common

import fr.kairossolum.pangmao.data.settings.DefinitionLanguage
import fr.kairossolum.pangmao.data.translation.TranslationTarget
import fr.kairossolum.pangmao.data.translation.TranslationRepository
import fr.kairossolum.pangmao.domain.TranslationQuality
import fr.kairossolum.pangmao.domain.model.TextAnalysis
import kotlinx.coroutines.CancellationException

data class TextTranslationState(
    val french: String? = null,
    val english: String? = null,
    val englishIsAttested: Boolean = false,
    val hasAutomaticTranslation: Boolean = false,
    val missingTargets: Set<TranslationTarget> = emptySet(),
    val isChecking: Boolean = false,
    val isDownloading: Boolean = false,
    val isTranslating: Boolean = false,
    val error: String? = null,
) {
    val isBusy: Boolean
        get() = isChecking || isDownloading || isTranslating
}

fun DefinitionLanguage.translationTargets(): List<TranslationTarget> = when (this) {
    DefinitionLanguage.FRENCH -> listOf(TranslationTarget.FRENCH)
    DefinitionLanguage.ENGLISH -> listOf(TranslationTarget.ENGLISH)
    DefinitionLanguage.BOTH -> listOf(TranslationTarget.FRENCH, TranslationTarget.ENGLISH)
}

suspend fun resolveTextTranslation(
    analysis: TextAnalysis,
    definitionLanguage: DefinitionLanguage,
    translation: TranslationRepository,
    update: (TextTranslationState) -> Unit,
) {
    val requestedTargets = definitionLanguage.translationTargets()
    val curated = TranslationQuality.curated(analysis.sourceText)
    val reviewedFrench = analysis.exactExample?.french?.takeIf(String::isNotBlank)
    val trustedFrench = (curated?.french ?: reviewedFrench ?: analysis.exactEntry?.definitionsFrench?.firstOrNull())
        ?.takeIf { TranslationTarget.FRENCH in requestedTargets }
    val attestedEnglish = analysis.exactExample?.english
        ?.takeIf { TranslationTarget.ENGLISH in requestedTargets }
    val trustedEnglish = (curated?.english ?: analysis.exactEntry?.definitionsEnglish?.firstOrNull())
        ?.takeIf { TranslationTarget.ENGLISH in requestedTargets }
    val initialEnglish = attestedEnglish ?: trustedEnglish
    val automaticTargets = requestedTargets.filterNot {
        when (it) {
            TranslationTarget.FRENCH -> trustedFrench != null
            TranslationTarget.ENGLISH -> initialEnglish != null
        }
    }
    var state = TextTranslationState(
        french = trustedFrench,
        english = initialEnglish,
        englishIsAttested = attestedEnglish != null && analysis.exactExample?.englishSource == "Tatoeba",
        isChecking = automaticTargets.isNotEmpty(),
    )
    update(state)
    val missing = buildSet {
        for (target in automaticTargets) {
            if (!translation.isReady(target)) add(target)
        }
    }
    if (missing.isNotEmpty()) {
        update(state.copy(missingTargets = missing, isChecking = false))
        return
    }
    state = state.copy(isChecking = false, isTranslating = automaticTargets.isNotEmpty())
    update(state)
    var french = trustedFrench
    var english = initialEnglish
    try {
        val machineSource = TranslationQuality.prepareForMachineTranslation(analysis.sourceText)
        for (target in automaticTargets) {
            val translated = translation.translateChinese(machineSource, target)
            when (target) {
                TranslationTarget.FRENCH -> french = TranslationQuality.polishFrench(analysis.sourceText, translated)
                TranslationTarget.ENGLISH -> english = TranslationQuality.polishEnglish(analysis.sourceText, translated)
            }
        }
        update(
            state.copy(
                french = french,
                english = english,
                hasAutomaticTranslation = automaticTargets.isNotEmpty(),
                isTranslating = false,
            )
        )
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        update(
            state.copy(
                isTranslating = false,
                error = error.message ?: error.javaClass.simpleName,
            )
        )
    }
}
