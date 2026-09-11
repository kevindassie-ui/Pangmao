package fr.kairossolum.pangmao.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fr.kairossolum.pangmao.data.settings.ThemeMode

val Jade = Color(0xFF166B5B)
val Porcelain = Color(0xFFF8FAF7)
val NightSeal = Color(0xFF07110F)

private val LightColors = lightColorScheme(
    primary = Jade,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7F2E9),
    onPrimaryContainer = Color(0xFF052019),
    secondary = Color(0xFF4F6A63),
    tertiary = Color(0xFF8A5B3D),
    background = Porcelain,
    surface = Color.White,
    onSurface = Color(0xFF18201D),
    onSurfaceVariant = Color(0xFF53605C),
    surfaceVariant = Color(0xFFEAF0EC),
    outline = Color(0xFF788680),
    outlineVariant = Color(0xFFCED8D3),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF75D8BB),
    onPrimary = Color(0xFF00382D),
    primaryContainer = Color(0xFF0B4F42),
    onPrimaryContainer = Color(0xFF9CF5D7),
    secondary = Color(0xFFB4CCC3),
    tertiary = Color(0xFFE8B78E),
    background = NightSeal,
    surface = Color(0xFF0E1A17),
    onSurface = Color(0xFFE4EEE9),
    onSurfaceVariant = Color(0xFFB9C8C2),
    surfaceVariant = Color(0xFF1A2B26),
    outline = Color(0xFF83958E),
    outlineVariant = Color(0xFF344B44),
)

private val PangmaoShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(34.dp),
)

@Composable
fun PangmaoTheme(themeMode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = PangmaoShapes,
        content = content,
    )
}
