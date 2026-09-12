package fr.kairossolum.pangmao.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.kairossolum.pangmao.data.dictionary.DictionaryRepository
import fr.kairossolum.pangmao.data.user.StudyRepository
import fr.kairossolum.pangmao.domain.containsHan
import fr.kairossolum.pangmao.domain.model.DictionaryEntry
import fr.kairossolum.pangmao.domain.model.TextAnalysis
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SearchUiState(
    val results: List<DictionaryEntry> = emptyList(),
    val analysis: TextAnalysis? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val dictionary: DictionaryRepository,
    study: StudyRepository,
) : ViewModel() {
    val query = MutableStateFlow("")
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    val history = study.history().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            query.debounce { value -> if (value.isBlank()) 0L else 160L }.collectLatest { value ->
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                runCatching {
                    val normalized = value.trim()
                    val results = if (normalized.isBlank()) dictionary.popular() else dictionary.search(normalized)
                    val analysis = normalized
                        .takeIf { it.codePointCount(0, it.length) > 1 && containsHan(it) }
                        ?.let { dictionary.analyze(it) }
                    results to analysis
                }.onSuccess { (results, analysis) ->
                    _uiState.value = SearchUiState(results = results, analysis = analysis, isLoading = false)
                }.onFailure { error ->
                    _uiState.value = SearchUiState(
                        isLoading = false,
                        error = error.message ?: "Impossible d’ouvrir le dictionnaire hors ligne.",
                    )
                }
            }
        }
    }

    fun setQuery(value: String) {
        query.value = value
    }
}
