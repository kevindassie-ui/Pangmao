package fr.kairossolum.pangmao.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.kairossolum.pangmao.data.dictionary.DictionaryRepository
import fr.kairossolum.pangmao.data.settings.DefinitionLanguage
import fr.kairossolum.pangmao.data.settings.SettingsRepository
import fr.kairossolum.pangmao.data.translation.TranslationRepository
import fr.kairossolum.pangmao.domain.model.AnalyzedToken
import fr.kairossolum.pangmao.domain.model.DictionaryEntry
import fr.kairossolum.pangmao.domain.model.TextAnalysis
import fr.kairossolum.pangmao.ui.common.TextTranslationState
import fr.kairossolum.pangmao.ui.common.resolveTextTranslation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private data class ReaderRequest(
    val text: String,
    val definitionLanguage: DefinitionLanguage,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ReaderViewModel(
    private val dictionary: DictionaryRepository,
    settings: SettingsRepository,
    private val translation: TranslationRepository,
) : ViewModel() {
    private val _text = MutableStateFlow("今天我们一起学习中文。认识一个新词时，轻触它即可查看释义。")
    val text: StateFlow<String> = _text.asStateFlow()
    private val _analysis = MutableStateFlow<TextAnalysis?>(null)
    val analysis: StateFlow<TextAnalysis?> = _analysis.asStateFlow()
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()
    private val _translationState = MutableStateFlow(TextTranslationState())
    val translationState: StateFlow<TextTranslationState> = _translationState.asStateFlow()
    private val _selectedEntry = MutableStateFlow<DictionaryEntry?>(null)
    val selectedEntry: StateFlow<DictionaryEntry?> = _selectedEntry.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private val translationRefresh = MutableStateFlow(0L)

    init {
        viewModelScope.launch {
            combine(
                _text.debounce { value -> if (value.length < 2) 0L else 220L },
                settings.settings.map { it.definitionLanguage }.distinctUntilChanged(),
                translationRefresh,
            ) { value, language, _ -> ReaderRequest(value, language) }
                .collectLatest(::analyze)
        }
    }

    fun setText(value: String) {
        _text.value = value.take(MAX_TEXT_LENGTH)
        _error.value = if (value.length > MAX_TEXT_LENGTH) {
            "Le texte a été limité à ${MAX_TEXT_LENGTH / 1_000} 000 caractères pour préserver la fluidité."
        } else null
    }

    fun select(token: AnalyzedToken) {
        _selectedEntry.value = token.entry
    }

    fun dismissSelection() {
        _selectedEntry.value = null
    }

    fun downloadTranslationModels() {
        val missing = _translationState.value.missingTargets
        if (missing.isEmpty() || _translationState.value.isDownloading) return
        viewModelScope.launch {
            _translationState.value = _translationState.value.copy(
                isChecking = false,
                isDownloading = true,
                error = null,
            )
            try {
                for (target in missing) translation.download(target)
                translationRefresh.value += 1
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                _translationState.value = _translationState.value.copy(
                    isDownloading = false,
                    error = error.message ?: error.javaClass.simpleName,
                )
            }
        }
    }

    private suspend fun analyze(request: ReaderRequest) {
        if (request.text.isBlank()) {
            _analysis.value = null
            _translationState.value = TextTranslationState()
            _isAnalyzing.value = false
            return
        }
        _isAnalyzing.value = true
        try {
            val result = dictionary.analyze(request.text)
            _analysis.value = result
            _isAnalyzing.value = false
            resolveTextTranslation(
                analysis = result,
                definitionLanguage = request.definitionLanguage,
                translation = translation,
                update = { _translationState.value = it },
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            _analysis.value = null
            _isAnalyzing.value = false
            _translationState.value = TextTranslationState()
            _error.value = error.message ?: error.javaClass.simpleName
        }
    }

    private companion object {
        const val MAX_TEXT_LENGTH = 20_000
    }
}
