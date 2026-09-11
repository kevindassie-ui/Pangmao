package fr.kairossolum.pangmao.ui.common

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import fr.kairossolum.pangmao.domain.Pinyin

private val toneColors = mapOf(
    1 to Color(0xFFD32F2F),
    2 to Color(0xFF2E7D32),
    3 to Color(0xFF1565C0),
    4 to Color(0xFF7B1FA2),
    5 to Color(0xFF6D6D6D),
)

fun coloredPinyin(numbered: String, bold: Boolean = false): AnnotatedString = buildAnnotatedString {
    val parts = numbered.split(Regex("(\\s+)"))
    var searchStart = 0
    for (part in parts) {
        if (part.isEmpty()) continue
        val sourceIndex = numbered.indexOf(part, searchStart)
        if (sourceIndex > searchStart) append(numbered.substring(searchStart, sourceIndex))
        val tone = Pinyin.toneOf(part)
        withStyle(
            SpanStyle(
                color = toneColors.getValue(tone),
                fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
            )
        ) {
            append(Pinyin.withToneMarks(part))
        }
        searchStart = sourceIndex + part.length
    }
    if (searchStart < numbered.length) append(numbered.substring(searchStart))
}

@Composable
fun PinyinText(
    numbered: String,
    fontSize: TextUnit = TextUnit.Unspecified,
    bold: Boolean = false,
) {
    Text(
        text = coloredPinyin(numbered, bold),
        fontSize = fontSize,
        color = LocalContentColor.current,
    )
}

