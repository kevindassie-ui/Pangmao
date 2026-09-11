package fr.kairossolum.pangmao.ui.common

import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import fr.kairossolum.pangmao.domain.model.TextToken

@Suppress("DEPRECATION")
@Composable
fun TokenizedText(
    tokens: List<TextToken>,
    onTokenClick: (TextToken) -> Unit,
    modifier: Modifier = Modifier,
) {
    val linkedColor = MaterialTheme.colorScheme.primary
    val plainColor = MaterialTheme.colorScheme.onSurface
    val annotated = buildAnnotatedString {
        tokens.forEachIndexed { index, token ->
            if (token.entryId != null) pushStringAnnotation("token", index.toString())
            withStyle(
                SpanStyle(
                    color = if (token.entryId != null) linkedColor else plainColor,
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
