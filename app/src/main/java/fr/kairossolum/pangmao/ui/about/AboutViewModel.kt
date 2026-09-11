package fr.kairossolum.pangmao.ui.about

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.kairossolum.pangmao.data.dictionary.DictionaryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AboutViewModel(dictionary: DictionaryRepository) : ViewModel() {
    private val _metadata = MutableStateFlow<Map<String, String>>(emptyMap())
    val metadata: StateFlow<Map<String, String>> = _metadata.asStateFlow()

    init {
        viewModelScope.launch { _metadata.value = dictionary.metadata() }
    }
}

