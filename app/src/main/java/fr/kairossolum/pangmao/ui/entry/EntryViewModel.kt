package fr.kairossolum.pangmao.ui.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.kairossolum.pangmao.data.dictionary.DictionaryRepository
import fr.kairossolum.pangmao.data.user.StudyRepository
import fr.kairossolum.pangmao.domain.model.CharacterInfo
import fr.kairossolum.pangmao.domain.model.DictionaryEntry
import fr.kairossolum.pangmao.domain.model.ExampleSentence
import fr.kairossolum.pangmao.domain.model.RelatedWordPosition
import fr.kairossolum.pangmao.domain.model.RelatedWordSort
import fr.kairossolum.pangmao.domain.model.WordKnowledgeStatus
import kotlinx.coroutines.async
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.update

data class EntryUiState(
    val entry: DictionaryEntry? = null,
    val examples: List<ExampleSentence> = emptyList(),
    val characters: List<CharacterInfo> = emptyList(),
    val characterEntryIds: Map<String, Long> = emptyMap(),
    val relatedWords: List<DictionaryEntry> = emptyList(),
    val relatedWordPosition: RelatedWordPosition = RelatedWordPosition.CONTAINS,
    val relatedWordSort: RelatedWordSort = RelatedWordSort.FREQUENCY,
    val frequentWordsOnly: Boolean = true,
    val isWordsLoading: Boolean = false,
    val wordsError: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
)

class EntryViewModel(
    private val entryId: Long,
    private val dictionary: DictionaryRepository,
    private val study: StudyRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(EntryUiState())
    val uiState: StateFlow<EntryUiState> = _uiState.asStateFlow()
    val isFavorite = study.isFavorite(entryId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val isFlashcard = study.isFlashcard(entryId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val wordKnowledgeStatus = study.wordKnowledgeStatus(entryId)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            WordKnowledgeStatus.UNMARKED,
        )
    private var relatedWordsJob: Job? = null

    init {
        viewModelScope.launch {
            runCatching {
                val entry = dictionary.entry(entryId) ?: error("Entrée introuvable")
                val examples = async { dictionary.examples(entry.simplified) }
                val characters = async { dictionary.characters(entry.simplified) }
                val relatedWords = async {
                    if (entry.simplified.codePointCount(0, entry.simplified.length) == 1) {
                        dictionary.relatedWords(entry.simplified)
                    } else {
                        emptyList()
                    }
                }
                study.recordHistory(entryId)
                val loadedCharacters = characters.await()
                EntryUiState(
                    entry = entry,
                    examples = examples.await(),
                    characters = loadedCharacters,
                    characterEntryIds = loadedCharacters.mapNotNull { info ->
                        dictionary.lookupExact(info.character)?.id?.let { info.character to it }
                    }.toMap(),
                    relatedWords = relatedWords.await(),
                    isLoading = false,
                )
            }.onSuccess { _uiState.value = it }
                .onFailure {
                    _uiState.value = EntryUiState(
                        isLoading = false,
                        error = it.message ?: "Impossible de charger cette entrée.",
                    )
                }
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch { study.toggleFavorite(entryId, isFavorite.value) }
    }

    fun toggleFlashcard() {
        viewModelScope.launch {
            if (isFlashcard.value) study.removeFlashcard(entryId) else study.addFlashcard(entryId)
        }
    }

    fun addFlashcard() {
        viewModelScope.launch {
            if (!isFlashcard.value) study.addFlashcard(entryId)
        }
    }

    fun setWordKnowledgeStatus(status: WordKnowledgeStatus) {
        viewModelScope.launch { study.setWordKnowledgeStatus(entryId, status) }
    }

    fun setRelatedWordPosition(position: RelatedWordPosition) {
        if (_uiState.value.relatedWordPosition == position) return
        _uiState.update { it.copy(relatedWordPosition = position) }
        reloadRelatedWords()
    }

    fun setRelatedWordSort(sort: RelatedWordSort) {
        if (_uiState.value.relatedWordSort == sort) return
        _uiState.update { it.copy(relatedWordSort = sort) }
        reloadRelatedWords()
    }

    fun setFrequentWordsOnly(enabled: Boolean) {
        if (_uiState.value.frequentWordsOnly == enabled) return
        _uiState.update { it.copy(frequentWordsOnly = enabled) }
        reloadRelatedWords()
    }

    private fun reloadRelatedWords() {
        val snapshot = _uiState.value
        val character = snapshot.entry?.simplified
            ?.takeIf { it.codePointCount(0, it.length) == 1 }
            ?: return
        relatedWordsJob?.cancel()
        relatedWordsJob = viewModelScope.launch {
            _uiState.update { it.copy(isWordsLoading = true, wordsError = null) }
            try {
                val words = dictionary.relatedWords(
                    character = character,
                    position = _uiState.value.relatedWordPosition,
                    frequentOnly = _uiState.value.frequentWordsOnly,
                    sort = _uiState.value.relatedWordSort,
                )
                _uiState.update { it.copy(relatedWords = words, isWordsLoading = false) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isWordsLoading = false,
                        wordsError = error.message ?: "Impossible de charger les mots associés.",
                    )
                }
            }
        }
    }
}
