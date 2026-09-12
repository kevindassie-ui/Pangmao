package fr.kairossolum.pangmao.ui.handwriting

import androidx.lifecycle.ViewModel
import com.google.mlkit.common.MlKitException
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.recognition.Ink
import fr.kairossolum.pangmao.domain.firstHanCharacter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class InkPoint(val x: Float, val y: Float, val time: Long)
data class DrawnStroke(val points: List<InkPoint>)

enum class InkModelState { CHECKING, NEEDS_DOWNLOAD, DOWNLOADING, READY, ERROR }

data class HandwritingUiState(
    val modelState: InkModelState = InkModelState.CHECKING,
    val strokes: List<DrawnStroke> = emptyList(),
    val candidates: List<String> = emptyList(),
    val isRecognizing: Boolean = false,
    val recognitionAttempted: Boolean = false,
    val error: String? = null,
)

class HandwritingViewModel : ViewModel() {
    private val identifier = try {
        DigitalInkRecognitionModelIdentifier.fromLanguageTag(LANGUAGE_TAG)
    } catch (_: MlKitException) {
        null
    } ?: error("Chinese handwriting model unavailable")
    private val model = DigitalInkRecognitionModel.builder(identifier).build()
    private val modelManager = RemoteModelManager.getInstance()
    private val recognizer = DigitalInkRecognition.getClient(
        DigitalInkRecognizerOptions.builder(model).build(),
    )
    private val _uiState = MutableStateFlow(HandwritingUiState())
    val uiState: StateFlow<HandwritingUiState> = _uiState.asStateFlow()
    private var recognitionRequest = 0L

    init {
        modelManager.isModelDownloaded(model)
            .addOnSuccessListener { ready ->
                _uiState.value = _uiState.value.copy(
                    modelState = if (ready) InkModelState.READY else InkModelState.NEEDS_DOWNLOAD,
                )
            }
            .addOnFailureListener { error ->
                _uiState.value = _uiState.value.copy(modelState = InkModelState.ERROR, error = error.message)
            }
    }

    fun downloadModel() {
        if (_uiState.value.modelState == InkModelState.DOWNLOADING) return
        _uiState.value = _uiState.value.copy(modelState = InkModelState.DOWNLOADING, error = null)
        modelManager.download(model, DownloadConditions.Builder().build())
            .addOnSuccessListener {
                _uiState.value = _uiState.value.copy(modelState = InkModelState.READY)
            }
            .addOnFailureListener { error ->
                _uiState.value = _uiState.value.copy(modelState = InkModelState.ERROR, error = error.message)
            }
    }

    fun addStroke(points: List<InkPoint>) {
        if (points.isEmpty() || _uiState.value.modelState != InkModelState.READY) return
        _uiState.value = _uiState.value.copy(
            strokes = _uiState.value.strokes + DrawnStroke(points),
            candidates = emptyList(),
            recognitionAttempted = false,
            error = null,
        )
    }

    fun undo() {
        recognitionRequest += 1
        val strokes = _uiState.value.strokes.dropLast(1)
        _uiState.value = _uiState.value.copy(
            strokes = strokes,
            candidates = emptyList(),
            isRecognizing = false,
            recognitionAttempted = false,
            error = null,
        )
    }

    fun clear() {
        recognitionRequest += 1
        _uiState.value = _uiState.value.copy(
            strokes = emptyList(),
            candidates = emptyList(),
            isRecognizing = false,
            recognitionAttempted = false,
            error = null,
        )
    }

    fun recognize() {
        val strokes = _uiState.value.strokes
        if (
            strokes.isEmpty() ||
            _uiState.value.modelState != InkModelState.READY ||
            _uiState.value.isRecognizing
        ) return
        val request = ++recognitionRequest
        val ink = runCatching {
            Ink.builder().apply {
                strokes.forEach { drawn ->
                    addStroke(Ink.Stroke.builder().apply {
                        drawn.points.forEach { addPoint(Ink.Point.create(it.x, it.y, it.time)) }
                    }.build())
                }
            }.build()
        }.getOrElse { error ->
            showRecognitionError(request, error)
            return
        }
        _uiState.value = _uiState.value.copy(
            isRecognizing = true,
            recognitionAttempted = false,
            error = null,
        )
        val task = runCatching { recognizer.recognize(ink) }.getOrElse { error ->
            showRecognitionError(request, error)
            return
        }
        task
            .addOnSuccessListener { result ->
                if (request != recognitionRequest) return@addOnSuccessListener
                val candidates = result.candidates.mapNotNull { firstHanCharacter(it.text) }.distinct().take(8)
                _uiState.value = _uiState.value.copy(
                    candidates = candidates,
                    isRecognizing = false,
                    recognitionAttempted = true,
                )
            }
            .addOnFailureListener { error ->
                showRecognitionError(request, error)
            }
    }

    private fun showRecognitionError(request: Long, error: Throwable) {
        if (request != recognitionRequest) return
        _uiState.value = _uiState.value.copy(
            isRecognizing = false,
            candidates = emptyList(),
            recognitionAttempted = false,
            error = error.message ?: error.javaClass.simpleName,
        )
    }

    override fun onCleared() {
        recognitionRequest += 1
        recognizer.close()
        super.onCleared()
    }

    private companion object {
        const val LANGUAGE_TAG = "zh-Hani-CN"
    }
}
