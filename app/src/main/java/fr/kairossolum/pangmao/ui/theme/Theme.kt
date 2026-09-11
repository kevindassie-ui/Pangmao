package fr.kairossolum.pangmao.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val PangmaoRed = Color(0xFF9D241F)
val PangmaoGold = Color(0xFFD78B28)
val Ink = Color(0xFF241C1A)
val Paper = Color(0xFFFFF9F5)

private val LightColors = lightColorScheme(
    primary = PangmaoRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD4),
    onPrimaryContainer = Color(0xFF410002),
    secondary = Color(0xFF77574F),
    tertiary = PangmaoGold,
    background = Paper,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF4DED9),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB4A9),
    onPrimary = Color(0xFF610006),
    primaryContainer = Color(0xFF7E1112),
    secondary = Color(0xFFE7BDB3),
    tertiary = Color(0xFFF8BD6F),
    background = Color(0xFF1B1110),
    surface = Color(0xFF1B1110),
    onSurface = Color(0xFFF2DFDB),
)

@Composable
fun PangmaoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}

