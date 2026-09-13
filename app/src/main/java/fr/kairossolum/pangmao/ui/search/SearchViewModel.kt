package fr.kairossolum.pangmao.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.kairossolum.pangmao.data.dictionary.DictionaryRepository
import fr.kairossolum.pangmao.data.settings.DefinitionLanguage
import fr.kairossolum.pangmao.data.settings.SettingsRepository
import fr.kairossolum.pangmao.data.translation.TranslationRepository
import fr.kairossolum.pangmao.data.user.StudyRepository
import fr.kairossolum.pangmao.domain.containsHan
import fr.kairossolum.pangmao.domain.model.DictionaryEntry
import fr.kairossolum.pangmao.domain.model.LearningDictionaryEntry
import fr.kairossolum.pangmao.domain.model.LearningLanguage
import fr.kairossolum.pangmao.domain.model.TextAnalysis
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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SearchUiState(
    val results: List<DictionaryEntry> = emptyList(),
    val learningResults: List<LearningDictionaryEntry> = emptyList(),
    val analysis: TextAnalysis? = null,
    val translation: TextTranslationState = TextTranslationState(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val dictionary: DictionaryRepository,
    private val study: StudyRepository,
    private val settings: SettingsRepository,
    private val translation: TranslationRepository,
) : ViewModel() {
    val query = MutableStateFlow("")
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    val history = study.history().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val queryHistory = study.queryHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val learningLanguage = settings.settings
        .map { it.learningLanguage }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LearningLanguage.CHINESE)
    private val translationRefresh = MutableStateFlow(0L)

    init {
        viewModelScope.launch {
            combine(
                query.debounce { value -> if (value.isBlank()) 0L else 180L },
                settings.settings
                    .map { it.definitionLanguage to it.learningLanguage }
                    .distinctUntilChanged(),
                translationRefresh,
            ) { value, selected, _ -> Triple(value, selected.first, selected.second) }
                .collectLatest { (value, definitionLanguage, learningLanguage) ->
                    load(value, definitionLanguage, learningLanguage)
                }
        }
        viewModelScope.launch {
            query
                .map(String::trim)
                .debounce(1_000)
                .filter(String::isNotBlank)
                .distinctUntilChanged()
                .collectLatest(study::recordQuery)
        }
    }

    fun setQuery(value: String) {
        query.value = value
    }

    fun setLearningLanguage(value: LearningLanguage) {
        if (value == learningLanguage.value) return
        query.value = ""
        viewModelScope.launch { settings.setLearningLanguage(value) }
    }

    fun deleteQuery(value: String) {
        viewModelScope.launch { study.deleteQuery(value) }
    }

    fun clearQueryHistory() {
        viewModelScope.launch { study.clearQueryHistory() }
    }

    fun downloadTranslationModels() {
        val missing = _uiState.value.translation.missingTargets
        if (missing.isEmpty() || _uiState.value.translation.isDownloading) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                translation = _uiState.value.translation.copy(
                    isChecking = false,
                    isDownloading = true,
                    error = null,
                )
            )
            try {
                missing.forEach { translation.download(it) }
                translationRefresh.value += 1
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                _uiState.value = _uiState.value.copy(
                    translation = _uiState.value.translation.copy(
                        isDownloading = false,
                        error = error.message ?: error.javaClass.simpleName,
                    )
                )
            }
        }
    }

    private suspend fun load(
        value: String,
        definitionLanguage: DefinitionLanguage,
        learningLanguage: LearningLanguage,
    ) {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            translation = TextTranslationState(),
            error = null,
        )
        try {
            val normalized = value.trim()
            val results = if (learningLanguage == LearningLanguage.CHINESE) {
                if (normalized.isBlank()) dictionary.popular() else dictionary.search(normalized)
            } else {
                emptyList()
            }
            val learningResults = if (
                learningLanguage != LearningLanguage.CHINESE && normalized.isNotBlank()
            ) {
                dictionary.searchLearning(learningLanguage, normalized)
            } else {
                emptyList()
            }
            val analysis = normalized
                .takeIf {
                    learningLanguage == LearningLanguage.CHINESE &&
                        it.codePointCount(0, it.length) > 1 &&
                        containsHan(it)
                }
                ?.let { dictionary.analyze(it) }
            _uiState.value = SearchUiState(
                results = results,
                learningResults = learningResults,
                analysis = analysis,
                isLoading = false,
            )
            if (analysis != null && analysis.exactEntry == null) {
                resolveTextTranslation(
                    analysis = analysis,
                    definitionLanguage = definitionLanguage,
                    translation = translation,
                    update = { state -> _uiState.value = _uiState.value.copy(translation = state) },
                )
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            _uiState.value = SearchUiState(
                isLoading = false,
                error = error.message ?: "Impossible d’ouvrir le dictionnaire hors ligne.",
            )
        }
    }
}
