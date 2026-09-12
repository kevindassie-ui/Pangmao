package fr.kairossolum.pangmao.ui.common

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
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

fun toneColor(tone: Int): Color = toneColors[tone] ?: toneColors.getValue(5)

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
                color = toneColor(tone),
                fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
            )
        ) {
            append(Pinyin.withToneMarks(part))
        }
        searchStart = sourceIndex + part.length
    }
    if (searchStart < numbered.length) append(numbered.substring(searchStart))
}

fun coloredHanzi(
    hanzi: String,
    numberedPinyin: String,
    bold: Boolean = false,
): AnnotatedString = buildAnnotatedString {
    val syllables = numberedPinyin.trim().split(Regex("\\s+")).filter(String::isNotBlank)
    var syllableIndex = 0
    val codePoints = hanzi.codePoints().iterator()
    while (codePoints.hasNext()) {
        val codePoint = codePoints.nextInt()
        val value = String(Character.toChars(codePoint))
        if (Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN) {
            val tone = syllables.getOrNull(syllableIndex)?.let(Pinyin::toneOf) ?: 5
            syllableIndex += 1
            withStyle(
                SpanStyle(
                    color = toneColor(tone),
                    fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
                )
            ) {
                append(value)
            }
        } else {
            append(value)
        }
    }
}

@Composable
fun PinyinText(
    numbered: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = TextUnit.Unspecified,
    bold: Boolean = false,
) {
    Text(
        text = coloredPinyin(numbered, bold),
        modifier = modifier,
        fontSize = fontSize,
        color = LocalContentColor.current,
    )
}

@Composable
fun HanziText(
    hanzi: String,
    numberedPinyin: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = TextUnit.Unspecified,
    bold: Boolean = false,
) {
    Text(
        text = coloredHanzi(hanzi, numberedPinyin, bold),
        modifier = modifier,
        fontSize = fontSize,
        color = LocalContentColor.current,
    )
}
