package fr.kairossolum.pangmao.ui.handwriting

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.kairossolum.pangmao.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HandwritingScreen(
    viewModel: HandwritingViewModel,
    onBack: () -> Unit,
    onSelectCharacter: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text(stringResource(R.string.handwriting_title), fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.handwriting_subtitle), style = MaterialTheme.typography.labelSmall)
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back))
                }
            },
        )
        when (state.modelState) {
            InkModelState.CHECKING -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            InkModelState.NEEDS_DOWNLOAD, InkModelState.DOWNLOADING, InkModelState.ERROR -> ModelDownloadCard(
                downloading = state.modelState == InkModelState.DOWNLOADING,
                error = state.error,
                onDownload = viewModel::downloadModel,
            )
            InkModelState.READY -> WritingContent(state, viewModel, onSelectCharacter)
        }
    }
}

@Composable
private fun ModelDownloadCard(downloading: Boolean, error: String?, onDownload: () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth().padding(20.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.handwriting_download_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.handwriting_download_body), color = MaterialTheme.colorScheme.onSurfaceVariant)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            Button(onClick = onDownload, enabled = !downloading) {
                if (downloading) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text("  ${stringResource(R.string.handwriting_downloading)}")
                } else Text(stringResource(R.string.handwriting_download))
            }
        }
    }
}

@Composable
private fun WritingContent(
    state: HandwritingUiState,
    viewModel: HandwritingViewModel,
    onSelectCharacter: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.handwriting_ready), color = MaterialTheme.colorScheme.onSurfaceVariant)
        WritingCanvas(state.strokes, viewModel::setWritingArea, viewModel::addStroke)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = viewModel::undo, enabled = state.strokes.isNotEmpty(), modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.Undo, null)
                Text("  ${stringResource(R.string.handwriting_undo)}", maxLines = 1)
            }
            OutlinedButton(onClick = viewModel::clear, enabled = state.strokes.isNotEmpty()) {
                Icon(Icons.Outlined.Clear, stringResource(R.string.clear))
            }
            Button(onClick = viewModel::recognize, enabled = state.strokes.isNotEmpty() && !state.isRecognizing) {
                if (state.isRecognizing) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text(stringResource(R.string.handwriting_recognize))
            }
        }
        if (state.candidates.isNotEmpty()) {
            Text(stringResource(R.string.handwriting_candidates), modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold)
            LazyRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                items(state.candidates.size) { index ->
                    val candidate = state.candidates[index]
                    Surface(
                        modifier = Modifier.size(54.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        onClick = { onSelectCharacter(candidate) },
                    ) {
                        Box(contentAlignment = Alignment.Center) { Text(candidate, fontSize = 26.sp) }
                    }
                }
            }
        } else if (state.strokes.isNotEmpty() && !state.isRecognizing) {
            Text(stringResource(R.string.handwriting_no_result), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WritingCanvas(
    strokes: List<DrawnStroke>,
    onSizeChanged: (Float, Float) -> Unit,
    onStroke: (List<InkPoint>) -> Unit,
) {
    var activeStroke by remember { mutableStateOf<List<InkPoint>>(emptyList()) }
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val inkColor = MaterialTheme.colorScheme.onSurface
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), MaterialTheme.shapes.large)
            .onSizeChanged { onSizeChanged(it.width.toFloat(), it.height.toFloat()) }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset -> activeStroke = listOf(InkPoint(offset.x, offset.y, System.currentTimeMillis())) },
                    onDrag = { change, _ ->
                        activeStroke = activeStroke + InkPoint(change.position.x, change.position.y, System.currentTimeMillis())
                        change.consume()
                    },
                    onDragEnd = {
                        onStroke(activeStroke)
                        activeStroke = emptyList()
                    },
                    onDragCancel = { activeStroke = emptyList() },
                )
            },
    ) {
        val dash = PathEffect.dashPathEffect(floatArrayOf(12f, 12f))
        drawLine(gridColor, Offset(size.width / 2f, 0f), Offset(size.width / 2f, size.height), 2f, pathEffect = dash)
        drawLine(gridColor, Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), 2f, pathEffect = dash)
        drawLine(gridColor, Offset.Zero, Offset(size.width, size.height), 1.5f, pathEffect = dash)
        drawLine(gridColor, Offset(size.width, 0f), Offset(0f, size.height), 1.5f, pathEffect = dash)
        (strokes.map { it.points } + listOf(activeStroke)).forEach { points ->
            points.zipWithNext().forEach { (start, end) ->
                drawLine(inkColor, Offset(start.x, start.y), Offset(end.x, end.y), 9f, cap = StrokeCap.Round)
            }
            points.singleOrNull()?.let { drawCircle(inkColor, 4.5f, Offset(it.x, it.y)) }
        }
    }
}
