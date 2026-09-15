package fr.kairossolum.pangmao.ui.about

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.kairossolum.pangmao.data.dictionary.DictionaryRepository
import fr.kairossolum.pangmao.data.strokes.StrokeOrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AboutViewModel(
    dictionary: DictionaryRepository,
    strokeOrders: StrokeOrderRepository,
) : ViewModel() {
    private val _metadata = MutableStateFlow<Map<String, String>>(emptyMap())
    val metadata: StateFlow<Map<String, String>> = _metadata.asStateFlow()

    init {
        viewModelScope.launch {
            val dictionaryMetadata = dictionary.metadata()
            val strokeMetadata = runCatching { strokeOrders.metadata() }.getOrDefault(emptyMap())
                .mapKeys { (key, _) -> "stroke_$key" }
            _metadata.value = dictionaryMetadata + strokeMetadata
        }
    }
}
