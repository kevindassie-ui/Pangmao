package fr.kairossolum.pangmao.ui.ocr

import android.content.Context
import android.net.Uri
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import fr.kairossolum.pangmao.data.dictionary.DictionaryRepository
import fr.kairossolum.pangmao.domain.model.DictionaryEntry
import fr.kairossolum.pangmao.domain.model.TextToken
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class OcrViewModel(private val dictionary: DictionaryRepository) : ViewModel() {
    private val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    private val inFlight = AtomicBoolean(false)
    private var lastAnalysisAt = 0L

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()
    private val _liveEnabled = MutableStateFlow(true)
    val liveEnabled: StateFlow<Boolean> = _liveEnabled.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private val _selectedEntry = MutableStateFlow<DictionaryEntry?>(null)
    val selectedEntry: StateFlow<DictionaryEntry?> = _selectedEntry.asStateFlow()

    val tokens = _recognizedText
        .debounce(80)
        .mapLatest { text -> if (text.isBlank()) emptyList() else dictionary.tokenize(text) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @androidx.annotation.OptIn(markerClass = [ExperimentalGetImage::class])
    fun analyze(imageProxy: ImageProxy) {
        val now = System.currentTimeMillis()
        if (!_liveEnabled.value || now - lastAnalysisAt < ANALYSIS_INTERVAL_MS || !inFlight.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }
        lastAnalysisAt = now
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            inFlight.set(false)
            imageProxy.close()
            return
        }
        _isProcessing.value = true
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        recognizer.process(image)
            .addOnSuccessListener { result ->
                val text = result.text.trim()
                if (text.isNotBlank()) _recognizedText.value = text
                _error.value = null
            }
            .addOnFailureListener { throwable ->
                _error.value = throwable.message ?: "La reconnaissance de cette image a échoué."
            }
            .addOnCompleteListener {
                _isProcessing.value = false
                inFlight.set(false)
                imageProxy.close()
            }
    }

    fun recognizeImage(context: Context, uri: Uri) {
        if (!inFlight.compareAndSet(false, true)) return
        _liveEnabled.value = false
        _isProcessing.value = true
        _error.value = null
        val image = runCatching { InputImage.fromFilePath(context, uri) }.getOrElse {
            _error.value = "Impossible d’ouvrir cette image."
            _isProcessing.value = false
            inFlight.set(false)
            return
        }
        recognizer.process(image)
            .addOnSuccessListener { result ->
                _recognizedText.value = result.text.trim()
                if (result.text.isBlank()) _error.value = "Aucun texte n’a été détecté dans l’image."
            }
            .addOnFailureListener { throwable ->
                _error.value = throwable.message ?: "La reconnaissance de cette image a échoué."
            }
            .addOnCompleteListener {
                _isProcessing.value = false
                inFlight.set(false)
            }
    }

    fun toggleLive() {
        _liveEnabled.value = !_liveEnabled.value
        _error.value = null
    }

    fun clear() {
        _recognizedText.value = ""
        _error.value = null
    }

    fun select(token: TextToken) {
        val identifier = token.entryId ?: return
        viewModelScope.launch { _selectedEntry.value = dictionary.entry(identifier) }
    }

    fun dismissSelection() {
        _selectedEntry.value = null
    }

    override fun onCleared() {
        recognizer.close()
        super.onCleared()
    }

    companion object {
        private const val ANALYSIS_INTERVAL_MS = 450L
    }
}
