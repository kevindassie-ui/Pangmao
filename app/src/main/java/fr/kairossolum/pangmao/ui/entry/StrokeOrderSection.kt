package fr.kairossolum.pangmao.ui.entry

import android.graphics.Matrix
import android.graphics.Path as AndroidPath
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.graphics.PathParser
import fr.kairossolum.pangmao.R
import fr.kairossolum.pangmao.domain.StrokeEvaluation
import fr.kairossolum.pangmao.domain.StrokeEvaluationKind
import fr.kairossolum.pangmao.domain.StrokeEvaluator
import fr.kairossolum.pangmao.domain.model.CharacterStroke
import fr.kairossolum.pangmao.domain.model.PracticePoint
import fr.kairossolum.pangmao.domain.model.StrokeOrder
import fr.kairossolum.pangmao.domain.model.StrokePoint
import kotlinx.coroutines.delay
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt

private enum class StrokePanelMode { ANIMATION, PRACTICE }

@Composable
internal fun StrokeOrderSection(
    strokeOrder: StrokeOrder?,
    loadError: String?,
) {
    when {
        loadError != null -> Text(
            stringResource(R.string.stroke_order_error),
            color = MaterialTheme.colorScheme.error,
        )
        strokeOrder == null -> Text(
            stringResource(R.string.stroke_order_unavailable),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        else -> {
            var mode by remember(strokeOrder.character) { mutableStateOf(StrokePanelMode.ANIMATION) }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(R.string.stroke_order_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = mode == StrokePanelMode.ANIMATION,
                        onClick = { mode = StrokePanelMode.ANIMATION },
                        label = { Text(stringResource(R.string.stroke_mode_animation)) },
                    )
                    FilterChip(
                        selected = mode == StrokePanelMode.PRACTICE,
                        onClick = { mode = StrokePanelMode.PRACTICE },
                        label = { Text(stringResource(R.string.stroke_mode_practice)) },
                    )
                }
                when (mode) {
                    StrokePanelMode.ANIMATION -> StrokeAnimation(strokeOrder)
                    StrokePanelMode.PRACTICE -> StrokePractice(strokeOrder)
                }
            }
        }
    }
}

@Composable
private fun StrokeAnimation(strokeOrder: StrokeOrder) {
    var completedStrokes by remember(strokeOrder.character) { mutableIntStateOf(0) }
    var strokeProgress by remember(strokeOrder.character) { mutableFloatStateOf(0f) }
    var playing by remember(strokeOrder.character) { mutableStateOf(true) }
    var animationGeneration by remember(strokeOrder.character) { mutableIntStateOf(0) }
    val total = strokeOrder.strokes.size

    LaunchedEffect(strokeOrder.character, playing, animationGeneration) {
        if (!playing) return@LaunchedEffect
        if (completedStrokes >= total) {
            completedStrokes = 0
            strokeProgress = 0f
        }
        while (completedStrokes < total) {
            val stroke = strokeOrder.strokes[completedStrokes]
            val remaining = (1f - strokeProgress).coerceIn(0f, 1f)
            val animation = Animatable(strokeProgress)
            animation.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = (strokeDurationMillis(stroke) * remaining)
                        .roundToInt()
                        .coerceAtLeast(1),
                    easing = LinearEasing,
                ),
            ) { strokeProgress = value }
            completedStrokes += 1
            strokeProgress = 0f
            if (completedStrokes < total) delay(130)
        }
        playing = false
    }

    CharacterStrokeCanvas(
        strokeOrder = strokeOrder,
        completedStrokes = completedStrokes,
        activeStrokeProgress = strokeProgress,
        activeHintIndex = null,
        activeTrace = emptyList(),
        rejectedTrace = emptyList(),
        contentDescription = stringResource(
            R.string.stroke_animation_canvas,
            strokeOrder.character,
        ),
    )
    Text(
        if (completedStrokes >= total) {
            stringResource(R.string.stroke_progress_complete, total)
        } else {
            stringResource(R.string.stroke_progress, completedStrokes + 1, total)
        },
        modifier = Modifier.fillMaxWidth(),
        fontWeight = FontWeight.SemiBold,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = {
                playing = false
                completedStrokes = (completedStrokes - 1).coerceAtLeast(0)
                strokeProgress = 0f
            },
            enabled = completedStrokes > 0 || strokeProgress > 0f,
        ) {
            Icon(Icons.Outlined.SkipPrevious, stringResource(R.string.stroke_previous))
        }
        IconButton(
            onClick = {
                playing = false
                completedStrokes = 0
                strokeProgress = 0f
                animationGeneration += 1
                playing = true
            },
        ) {
            Icon(Icons.Outlined.Replay, stringResource(R.string.stroke_restart))
        }
        IconButton(
            onClick = {
                if (completedStrokes >= total) {
                    completedStrokes = 0
                    strokeProgress = 0f
                    animationGeneration += 1
                    playing = true
                } else {
                    playing = !playing
                }
            },
        ) {
            Icon(
                if (playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                stringResource(if (playing) R.string.stroke_pause else R.string.stroke_play),
            )
        }
        IconButton(
            onClick = {
                playing = false
                completedStrokes = (completedStrokes + 1).coerceAtMost(total)
                strokeProgress = 0f
            },
            enabled = completedStrokes < total,
        ) {
            Icon(Icons.Outlined.SkipNext, stringResource(R.string.stroke_next))
        }
    }
}

@Composable
private fun StrokePractice(strokeOrder: StrokeOrder) {
    var completedStrokes by remember(strokeOrder.character) { mutableIntStateOf(0) }
    var activeTrace by remember(strokeOrder.character) { mutableStateOf(emptyList<PracticePoint>()) }
    var rejectedTrace by remember(strokeOrder.character) { mutableStateOf(emptyList<PracticePoint>()) }
    var evaluation by remember(strokeOrder.character) { mutableStateOf<StrokeEvaluation?>(null) }
    var showHint by remember(strokeOrder.character) { mutableStateOf(false) }
    val total = strokeOrder.strokes.size
    val complete = completedStrokes >= total

    val finishTrace: (List<PracticePoint>) -> Unit = { trace ->
        activeTrace = emptyList()
        if (trace.size >= 2 && !complete) {
            val result = StrokeEvaluator.evaluate(trace, strokeOrder, completedStrokes)
            evaluation = result
            if (result.kind == StrokeEvaluationKind.MATCH) {
                completedStrokes += 1
                rejectedTrace = emptyList()
                showHint = false
            } else {
                rejectedTrace = trace
            }
        }
    }

    CharacterStrokeCanvas(
        strokeOrder = strokeOrder,
        completedStrokes = completedStrokes,
        activeStrokeProgress = 0f,
        activeHintIndex = completedStrokes.takeIf { !complete },
        showFullHint = showHint,
        activeTrace = activeTrace,
        rejectedTrace = rejectedTrace,
        contentDescription = stringResource(R.string.stroke_practice_canvas, strokeOrder.character),
        onTraceChanged = { trace ->
            activeTrace = trace
            rejectedTrace = emptyList()
            evaluation = null
        },
        onTraceFinished = finishTrace,
        inputEnabled = !complete,
    )

    when {
        complete -> Text(
            stringResource(R.string.stroke_practice_complete),
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        evaluation?.kind == StrokeEvaluationKind.MATCH -> Text(
            stringResource(R.string.stroke_practice_correct),
            color = MaterialTheme.colorScheme.primary,
        )
        evaluation?.kind == StrokeEvaluationKind.WRONG_DIRECTION -> Text(
            stringResource(R.string.stroke_practice_wrong_direction),
            color = MaterialTheme.colorScheme.error,
        )
        evaluation?.kind == StrokeEvaluationKind.OUT_OF_ORDER -> Text(
            stringResource(
                R.string.stroke_practice_out_of_order,
                (evaluation?.closestIndex ?: completedStrokes) + 1,
            ),
            color = MaterialTheme.colorScheme.error,
        )
        evaluation?.kind == StrokeEvaluationKind.TRY_AGAIN -> Text(
            stringResource(R.string.stroke_practice_retry),
            color = MaterialTheme.colorScheme.error,
        )
        else -> Text(stringResource(R.string.stroke_practice_instruction, completedStrokes + 1, total))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(
            onClick = { showHint = !showHint },
            modifier = Modifier.weight(1f),
            enabled = !complete,
        ) {
            Icon(Icons.Outlined.Lightbulb, contentDescription = null)
            Text(
                "  ${stringResource(if (showHint) R.string.stroke_practice_hide_hint else R.string.stroke_practice_show_hint)}",
                maxLines = 1,
            )
        }
        OutlinedButton(
            onClick = {
                completedStrokes = 0
                activeTrace = emptyList()
                rejectedTrace = emptyList()
                evaluation = null
                showHint = false
            },
            modifier = Modifier.weight(1f),
            enabled = completedStrokes > 0 || evaluation != null,
        ) {
            Icon(Icons.Outlined.Replay, contentDescription = null)
            Text("  ${stringResource(R.string.stroke_practice_reset)}", maxLines = 1)
        }
    }
    Text(
        stringResource(R.string.stroke_practice_notice),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun CharacterStrokeCanvas(
    strokeOrder: StrokeOrder,
    completedStrokes: Int,
    activeStrokeProgress: Float,
    activeHintIndex: Int?,
    activeTrace: List<PracticePoint>,
    rejectedTrace: List<PracticePoint>,
    contentDescription: String,
    showFullHint: Boolean = false,
    inputEnabled: Boolean = false,
    onTraceChanged: (List<PracticePoint>) -> Unit = {},
    onTraceFinished: (List<PracticePoint>) -> Unit = {},
) {
    var canvasSize by remember(strokeOrder.character) { mutableStateOf(IntSize.Zero) }
    val paths = remember(strokeOrder, canvasSize) { displayPaths(strokeOrder, canvasSize) }
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val guideColor = MaterialTheme.colorScheme.onSurfaceVariant
    val completedColor = MaterialTheme.colorScheme.primary
    val activeColor = MaterialTheme.colorScheme.tertiary
    val errorColor = MaterialTheme.colorScheme.error
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
    val inputModifier = if (inputEnabled) {
        Modifier.pointerInput(strokeOrder.character, completedStrokes) {
            var gesture = emptyList<PracticePoint>()
            detectDragGestures(
                onDragStart = { offset ->
                    gesture = listOf(offset.toPracticePoint(size.width, size.height))
                    onTraceChanged(gesture)
                },
                onDrag = { change, _ ->
                    gesture = gesture + change.position.toPracticePoint(size.width, size.height)
                    onTraceChanged(gesture)
                    change.consume()
                },
                onDragEnd = {
                    onTraceFinished(gesture)
                    gesture = emptyList()
                },
                onDragCancel = {
                    gesture = emptyList()
                    onTraceChanged(emptyList())
                },
            )
        }
    } else {
        Modifier
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = surfaceColor,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .onSizeChanged { canvasSize = it }
                    .semantics { this.contentDescription = contentDescription }
                    .then(inputModifier),
            ) {
                val dash = PathEffect.dashPathEffect(floatArrayOf(13f, 13f))
                drawLine(gridColor, Offset(size.width / 2f, 0f), Offset(size.width / 2f, size.height), 2f, pathEffect = dash)
                drawLine(gridColor, Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), 2f, pathEffect = dash)
                drawLine(gridColor, Offset.Zero, Offset(size.width, size.height), 1.5f, pathEffect = dash)
                drawLine(gridColor, Offset(size.width, 0f), Offset(0f, size.height), 1.5f, pathEffect = dash)

                paths.forEachIndexed { index, path ->
                    drawPath(path, guideColor.copy(alpha = 0.10f))
                    drawPath(
                        path,
                        guideColor.copy(alpha = 0.26f),
                        style = Stroke(width = 1.5f),
                    )
                    if (index < completedStrokes) drawPath(path, completedColor)
                }

                if (activeHintIndex != null && activeHintIndex in paths.indices) {
                    val path = paths[activeHintIndex]
                    drawPath(
                        path,
                        activeColor.copy(alpha = if (showFullHint) 0.34f else 0.08f),
                    )
                    drawPath(path, activeColor, style = Stroke(width = 2.5f))
                    val median = strokeOrder.strokes[activeHintIndex].median
                    if (showFullHint) {
                        clipPath(path) {
                            drawPath(
                                partialMedianPath(median, 1f, canvasSize),
                                activeColor,
                                style = Stroke(
                                    width = min(size.width, size.height) * 0.09f,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round,
                                ),
                            )
                        }
                    }
                    median.firstOrNull()?.let { point ->
                        drawCircle(
                            activeColor,
                            radius = min(size.width, size.height) * 0.022f,
                            center = point.toCanvas(canvasSize),
                        )
                    }
                } else if (completedStrokes in paths.indices && activeStrokeProgress > 0f) {
                    val path = paths[completedStrokes]
                    clipPath(path) {
                        drawPath(
                            partialMedianPath(
                                strokeOrder.strokes[completedStrokes].median,
                                activeStrokeProgress,
                                canvasSize,
                            ),
                            activeColor,
                            style = Stroke(
                                width = min(size.width, size.height) * 0.11f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round,
                            ),
                        )
                    }
                }

                drawPracticeTrace(rejectedTrace, errorColor)
                drawPracticeTrace(activeTrace, completedColor)
            }
        }
    }
}

private fun DrawScope.drawPracticeTrace(points: List<PracticePoint>, color: Color) {
    val width = min(size.width, size.height) * 0.025f
    points.zipWithNext().forEach { (start, end) ->
        drawLine(
            color = color,
            start = Offset(start.x * size.width, start.y * size.height),
            end = Offset(end.x * size.width, end.y * size.height),
            strokeWidth = width,
            cap = StrokeCap.Round,
        )
    }
    points.singleOrNull()?.let { point ->
        drawCircle(color, width / 2f, Offset(point.x * size.width, point.y * size.height))
    }
}

private fun displayPaths(strokeOrder: StrokeOrder, size: IntSize): List<Path> {
    if (size.width <= 0 || size.height <= 0) return emptyList()
    val side = min(size.width, size.height).toFloat()
    val scale = side / 1024f
    val left = (size.width - side) / 2f
    val top = (size.height - side) / 2f
    val matrix = Matrix().apply {
        setValues(
            floatArrayOf(
                scale, 0f, left,
                0f, -scale, top + 900f * scale,
                0f, 0f, 1f,
            )
        )
    }
    return strokeOrder.strokes.map { stroke ->
        val source = PathParser.createPathFromPathData(stroke.pathData) ?: AndroidPath()
        AndroidPath(source).apply { transform(matrix) }.asComposePath()
    }
}

private fun partialMedianPath(
    median: List<StrokePoint>,
    progress: Float,
    size: IntSize,
): Path {
    val points = median.map { it.toCanvas(size) }
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points.first().x, points.first().y)
    val lengths = points.zipWithNext().map { (start, end) ->
        hypot(end.x - start.x, end.y - start.y)
    }
    var remaining = lengths.sum() * progress.coerceIn(0f, 1f)
    points.zipWithNext().forEachIndexed { index, (start, end) ->
        if (remaining <= 0f) return@forEachIndexed
        val length = lengths[index]
        if (remaining >= length) {
            path.lineTo(end.x, end.y)
            remaining -= length
        } else {
            val fraction = if (length <= 0f) 0f else remaining / length
            path.lineTo(
                start.x + (end.x - start.x) * fraction,
                start.y + (end.y - start.y) * fraction,
            )
            remaining = 0f
        }
    }
    return path
}

private fun StrokePoint.toCanvas(size: IntSize): Offset {
    val side = min(size.width, size.height).toFloat()
    val left = (size.width - side) / 2f
    val top = (size.height - side) / 2f
    return Offset(
        x = left + x * side / 1024f,
        y = top + (900f - y) * side / 1024f,
    )
}

private fun Offset.toPracticePoint(width: Int, height: Int): PracticePoint = PracticePoint(
    x = if (width == 0) 0f else (x / width).coerceIn(0f, 1f),
    y = if (height == 0) 0f else (y / height).coerceIn(0f, 1f),
)

private fun strokeDurationMillis(stroke: CharacterStroke): Float {
    val length = stroke.median.zipWithNext().sumOf { (start, end) ->
        hypot(end.x - start.x, end.y - start.y).toDouble()
    }.toFloat()
    return (420f + length * 0.72f).coerceIn(520f, 1_250f)
}
