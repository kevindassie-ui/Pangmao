package fr.kairossolum.pangmao.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.kairossolum.pangmao.data.dictionary.DictionaryRepository
import fr.kairossolum.pangmao.domain.model.DictionaryEntry
import fr.kairossolum.pangmao.domain.model.TextToken
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ReaderUiState(
    val tokens: List<TextToken> = emptyList(),
    val isSegmenting: Boolean = false,
    val selectedEntry: DictionaryEntry? = null,
    val error: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ReaderViewModel(private val dictionary: DictionaryRepository) : ViewModel() {
    private val _text = MutableStateFlow("今天我们一起学习中文。认识一个新词时，轻触它即可查看释义。")
    val text: StateFlow<String> = _text.asStateFlow()
    private val _selectedEntry = MutableStateFlow<DictionaryEntry?>(null)
    private val _error = MutableStateFlow<String?>(null)

    val tokens = _text
        .debounce { value -> if (value.length < 2) 0L else 180L }
        .mapLatest { value -> if (value.isBlank()) emptyList() else dictionary.tokenize(value) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selectedEntry: StateFlow<DictionaryEntry?> = _selectedEntry.asStateFlow()
    val error: StateFlow<String?> = _error.asStateFlow()

    fun setText(value: String) {
        _text.value = value.take(MAX_TEXT_LENGTH)
        _error.value = if (value.length > MAX_TEXT_LENGTH) {
            "Le texte a été limité à ${MAX_TEXT_LENGTH / 1_000} 000 caractères pour préserver la fluidité."
        } else null
    }

    fun select(token: TextToken) {
        val identifier = token.entryId ?: return
        viewModelScope.launch { _selectedEntry.value = dictionary.entry(identifier) }
    }

    fun dismissSelection() {
        _selectedEntry.value = null
    }

    companion object {
        private const val MAX_TEXT_LENGTH = 100_000
    }
}

