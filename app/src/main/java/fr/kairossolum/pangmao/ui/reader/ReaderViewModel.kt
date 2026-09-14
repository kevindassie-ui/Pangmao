package fr.kairossolum.pangmao.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.kairossolum.pangmao.data.dictionary.DictionaryRepository
import fr.kairossolum.pangmao.data.settings.DefinitionLanguage
import fr.kairossolum.pangmao.data.settings.SettingsRepository
import fr.kairossolum.pangmao.data.settings.SpeechRate
import fr.kairossolum.pangmao.data.translation.TranslationRepository
import fr.kairossolum.pangmao.data.user.StudyRepository
import fr.kairossolum.pangmao.domain.model.AnalyzedToken
import fr.kairossolum.pangmao.domain.model.DictionaryEntry
import fr.kairossolum.pangmao.domain.model.TextAnalysis
import fr.kairossolum.pangmao.domain.model.WordKnowledgeStatus
import fr.kairossolum.pangmao.domain.containsHan
import fr.kairossolum.pangmao.ui.common.TextTranslationState
import fr.kairossolum.pangmao.ui.common.resolveTextTranslation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private data class ReaderRequest(
    val text: String,
    val definitionLanguage: DefinitionLanguage,
)

data class SelectedFlashcardState(
    val entryId: Long? = null,
    val isFlashcard: Boolean = false,
    val isReady: Boolean = false,
)

data class SelectedWordKnowledgeState(
    val entryId: Long? = null,
    val status: WordKnowledgeStatus = WordKnowledgeStatus.UNMARKED,
    val isReady: Boolean = false,
)

data class ReaderSelectionLookup(
    val query: String,
    val entries: List<DictionaryEntry>,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ReaderViewModel(
    private val dictionary: DictionaryRepository,
    private val settings: SettingsRepository,
    private val translation: TranslationRepository,
    private val study: StudyRepository,
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
    private val _selectionLookup = MutableStateFlow<ReaderSelectionLookup?>(null)
    val selectionLookup: StateFlow<ReaderSelectionLookup?> = _selectionLookup.asStateFlow()
    private var selectionLookupJob: Job? = null
    val selectedFlashcard = _selectedEntry
        .flatMapLatest { entry ->
            if (entry == null) {
                flowOf(SelectedFlashcardState())
            } else {
                study.isFlashcard(entry.id)
                    .map { isFlashcard ->
                        SelectedFlashcardState(
                            entryId = entry.id,
                            isFlashcard = isFlashcard,
                            isReady = true,
                        )
                    }
                    .onStart { emit(SelectedFlashcardState(entryId = entry.id)) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SelectedFlashcardState())
    val selectedWordKnowledge = _selectedEntry
        .flatMapLatest { entry ->
            if (entry == null) {
                flowOf(SelectedWordKnowledgeState())
            } else {
                study.wordKnowledgeStatus(entry.id)
                    .map { status ->
                        SelectedWordKnowledgeState(
                            entryId = entry.id,
                            status = status,
                            isReady = true,
                        )
                    }
                    .onStart { emit(SelectedWordKnowledgeState(entryId = entry.id)) }
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            SelectedWordKnowledgeState(),
        )
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private val translationRefresh = MutableStateFlow(0L)
    val speechRate = settings.settings
        .map { it.speechRate }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SpeechRate.NORMAL)

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
        _selectionLookup.value = null
        _selectedEntry.value = token.entry
    }

    fun defineSelection(value: String) {
        val query = value.trim().takeCodePoints(MAX_SELECTION_CODE_POINTS)
        if (query.isBlank() || !containsHan(query)) return
        selectionLookupJob?.cancel()
        selectionLookupJob = viewModelScope.launch {
            try {
                val exact = dictionary.lookupExact(query)
                if (exact != null) {
                    _selectionLookup.value = null
                    _selectedEntry.value = exact
                } else {
                    val entries = dictionary.analyze(query)
                        .tokens
                        .mapNotNull(AnalyzedToken::entry)
                        .distinctBy(DictionaryEntry::id)
                        .take(MAX_SELECTION_BLOCKS)
                    _selectedEntry.value = null
                    _selectionLookup.value = ReaderSelectionLookup(query, entries)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                _error.value = error.message ?: error.javaClass.simpleName
            }
        }
    }

    fun dismissSelection() {
        _selectedEntry.value = null
    }

    fun dismissSelectionLookup() {
        _selectionLookup.value = null
    }

    fun toggleSelectedFlashcard() {
        val entry = _selectedEntry.value ?: return
        val membership = selectedFlashcard.value
        if (!membership.isReady || membership.entryId != entry.id) return
        viewModelScope.launch {
            if (membership.isFlashcard) {
                study.removeFlashcard(entry.id)
            } else {
                study.addFlashcard(entry.id)
            }
        }
    }

    fun setSelectedWordKnowledgeStatus(status: WordKnowledgeStatus) {
        val entry = _selectedEntry.value ?: return
        val selection = selectedWordKnowledge.value
        if (!selection.isReady || selection.entryId != entry.id) return
        viewModelScope.launch { study.setWordKnowledgeStatus(entry.id, status) }
    }

    fun setSpeechRate(value: SpeechRate) {
        viewModelScope.launch { settings.setSpeechRate(value) }
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
        const val MAX_SELECTION_CODE_POINTS = 64
        const val MAX_SELECTION_BLOCKS = 12
    }
}

private fun String.takeCodePoints(maximum: Int): String {
    val count = codePointCount(0, length).coerceAtMost(maximum)
    return substring(0, offsetByCodePoints(0, count))
}
