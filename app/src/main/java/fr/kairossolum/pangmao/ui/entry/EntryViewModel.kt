package fr.kairossolum.pangmao.ui.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.kairossolum.pangmao.data.dictionary.DictionaryRepository
import fr.kairossolum.pangmao.data.user.StudyRepository
import fr.kairossolum.pangmao.domain.model.CharacterInfo
import fr.kairossolum.pangmao.domain.model.DictionaryEntry
import fr.kairossolum.pangmao.domain.model.ExampleSentence
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EntryUiState(
    val entry: DictionaryEntry? = null,
    val examples: List<ExampleSentence> = emptyList(),
    val characters: List<CharacterInfo> = emptyList(),
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

    init {
        viewModelScope.launch {
            runCatching {
                val entry = dictionary.entry(entryId) ?: error("Entrée introuvable")
                val examples = async { dictionary.examples(entry.simplified) }
                val characters = async { dictionary.characters(entry.simplified) }
                study.recordHistory(entryId)
                EntryUiState(
                    entry = entry,
                    examples = examples.await(),
                    characters = characters.await(),
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
}

