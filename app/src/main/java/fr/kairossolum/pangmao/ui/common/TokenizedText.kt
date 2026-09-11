package fr.kairossolum.pangmao.ui.common

import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import fr.kairossolum.pangmao.domain.model.TextToken
import fr.kairossolum.pangmao.ui.theme.PangmaoRed

@Suppress("DEPRECATION")
@Composable
fun TokenizedText(
    tokens: List<TextToken>,
    onTokenClick: (TextToken) -> Unit,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = false,
) {
    val annotated = buildAnnotatedString {
        tokens.forEachIndexed { index, token ->
            if (token.entryId != null) pushStringAnnotation("token", index.toString())
            withStyle(
                SpanStyle(
                    color = when {
                        token.entryId != null && darkTheme -> Color(0xFFFFB4A9)
                        token.entryId != null -> PangmaoRed
                        darkTheme -> Color(0xFFEBDDD9)
                        else -> Color(0xFF302725)
                    },
                    textDecoration = if (token.entryId != null) TextDecoration.Underline else TextDecoration.None,
                )
            ) {
                append(token.text)
            }
            if (token.entryId != null) pop()
        }
    }
    ClickableText(
        text = annotated,
        modifier = modifier,
        style = TextStyle(fontSize = 22.sp, lineHeight = 35.sp),
        onClick = { offset ->
            annotated.getStringAnnotations("token", offset, offset).firstOrNull()?.item
                ?.toIntOrNull()
                ?.let(tokens::getOrNull)
                ?.let(onTokenClick)
        },
    )
}

