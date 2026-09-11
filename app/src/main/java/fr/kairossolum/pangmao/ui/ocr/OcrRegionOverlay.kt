package fr.kairossolum.pangmao.ui.ocr

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.MaterialTheme
import kotlin.math.max

@Composable
fun OcrRegionOverlay(
    frame: OcrFrame?,
    selectedRegionId: Int?,
    onSelectRegion: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (frame == null || frame.regions.isEmpty()) return
    val outline = MaterialTheme.colorScheme.primary
    val selected = MaterialTheme.colorScheme.tertiary
    Canvas(
        modifier = modifier.pointerInput(frame, selectedRegionId) {
            detectTapGestures { point ->
                val transformed = frame.regions.map { it to it.displayRect(frame, size.width.toFloat(), size.height.toFloat()) }
                transformed
                    .filter { (_, rect) -> rect.inflate(12f).contains(point) }
                    .minByOrNull { (_, rect) -> rect.width * rect.height }
                    ?.first
                    ?.let { onSelectRegion(it.id) }
            }
        },
    ) {
        frame.regions.forEach { region ->
            val rect = region.displayRect(frame, size.width, size.height)
            val color = if (region.id == selectedRegionId) selected else outline
            if (region.id == selectedRegionId) drawRect(color.copy(alpha = 0.18f), rect.topLeft, rect.size)
            drawRect(color, rect.topLeft, rect.size, style = Stroke(width = if (region.id == selectedRegionId) 5f else 3f))
        }
    }
}

private fun OcrRegion.displayRect(frame: OcrFrame, viewWidth: Float, viewHeight: Float): Rect {
    val scale = max(viewWidth / frame.width, viewHeight / frame.height)
    val displayedWidth = frame.width * scale
    val displayedHeight = frame.height * scale
    val offset = Offset((viewWidth - displayedWidth) / 2f, (viewHeight - displayedHeight) / 2f)
    return Rect(
        left = offset.x + left * displayedWidth,
        top = offset.y + top * displayedHeight,
        right = offset.x + right * displayedWidth,
        bottom = offset.y + bottom * displayedHeight,
    )
}

private fun Rect.inflate(value: Float) = Rect(left - value, top - value, right + value, bottom + value)
