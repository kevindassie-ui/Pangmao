package fr.kairossolum.pangmao.data.translation

import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

enum class TranslationTarget(val languageTag: String) {
    FRENCH(TranslateLanguage.FRENCH),
    ENGLISH(TranslateLanguage.ENGLISH),
}

interface TranslationRepository {
    suspend fun isReady(target: TranslationTarget): Boolean
    suspend fun download(target: TranslationTarget)
    suspend fun translateChinese(text: String, target: TranslationTarget): String
}

class OnDeviceTranslationRepository : TranslationRepository {
    private val modelManager = RemoteModelManager.getInstance()
    private val knownReadyTargets = ConcurrentHashMap.newKeySet<TranslationTarget>()

    override suspend fun isReady(target: TranslationTarget): Boolean {
        if (target in knownReadyTargets) return true
        for (language in requiredRemoteLanguages(target)) {
            val downloaded = modelManager
                .isModelDownloaded(TranslateRemoteModel.Builder(language).build())
                .awaitResult()
            if (!downloaded) return false
        }
        knownReadyTargets += target
        return true
    }

    override suspend fun download(target: TranslationTarget) {
        val translator = translator(target)
        try {
            translator.downloadModelIfNeeded(DownloadConditions.Builder().build()).awaitResult()
            knownReadyTargets += target
        } finally {
            translator.close()
        }
    }

    override suspend fun translateChinese(text: String, target: TranslationTarget): String {
        check(isReady(target)) { "Translation model is not downloaded" }
        val translator = translator(target)
        return try {
            translator.translate(text.take(MAX_TRANSLATION_LENGTH)).awaitResult()
        } finally {
            translator.close()
        }
    }

    private fun translator(target: TranslationTarget): Translator = Translation.getClient(
        TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.CHINESE)
            .setTargetLanguage(target.languageTag)
            .build(),
    )

    private fun requiredRemoteLanguages(target: TranslationTarget): Set<String> = buildSet {
        add(TranslateLanguage.CHINESE)
        if (target != TranslationTarget.ENGLISH) add(target.languageTag)
    }

    private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result -> if (continuation.isActive) continuation.resume(result) }
        addOnFailureListener { error -> if (continuation.isActive) continuation.resumeWithException(error) }
        addOnCanceledListener { continuation.cancel() }
    }

    private companion object {
        const val MAX_TRANSLATION_LENGTH = 4_000
    }
}
