package fr.kairossolum.pangmao.ui.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.kairossolum.pangmao.data.dictionary.DictionaryRepository
import fr.kairossolum.pangmao.domain.model.LearningDictionaryEntry
import fr.kairossolum.pangmao.domain.model.LearningLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LearningEntryUiState(
    val entry: LearningDictionaryEntry? = null,
    val chineseEntryIds: Map<String, Long> = emptyMap(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

class LearningEntryViewModel(
    private val entryId: Long,
    private val expectedLanguage: LearningLanguage,
    private val dictionary: DictionaryRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LearningEntryUiState())
    val uiState: StateFlow<LearningEntryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching {
                val entry = dictionary.learningEntry(entryId)
                    ?: error("Entrée d’apprentissage introuvable")
                check(entry.language == expectedLanguage) {
                    "Le profil de l’entrée ne correspond pas à la navigation"
                }
                val chineseEntryIds = entry.chineseMeanings.mapNotNull { meaning ->
                    dictionary.lookupExact(meaning)?.id?.let { meaning to it }
                }.toMap()
                LearningEntryUiState(
                    entry = entry,
                    chineseEntryIds = chineseEntryIds,
                    isLoading = false,
                )
            }.onSuccess { _uiState.value = it }
                .onFailure { error ->
                    _uiState.value = LearningEntryUiState(
                        isLoading = false,
                        error = error.message ?: "Impossible de charger cette entrée.",
                    )
                }
        }
    }
}
