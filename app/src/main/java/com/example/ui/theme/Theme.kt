package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class QuizzyExtendedColors(
    val isDark: Boolean,
    val headingInk: Color,
    val bodyInk: Color,
    val secondaryInk: Color,
    val navyActionContainer: Color,
    val navyActionOnContainer: Color,
    val parentCanvas: Color,
    val parentSkyAccent: Color,
    val parentSkyContainer: Color,
    val lessonBackdrop: Color,
    val pastelMint: Color,
    val pastelPeach: Color,
    val pastelLavender: Color,
    val pastelSky: Color,
    val pastelYellow: Color,
    val speechBubble: Color,
    val chipOnTint: Color,
    val coralInk: Color,
    val successInk: Color,
    val warningInk: Color,
    val violetInk: Color,
    val progressTrack: Color,
    val drawingStroke: Color,
    val drawingPlate: Color
)

val LightQuizzyColors = QuizzyExtendedColors(
    isDark = false,
    headingInk = TealTertiary,
    bodyInk = OnWarmSurface,
    secondaryInk = OnWarmSurfaceVariant,
    navyActionContainer = TealTertiary,
    navyActionOnContainer = Color.White,
    parentCanvas = ParentCanvasBackground,
    parentSkyAccent = ParentSkyAccent,
    parentSkyContainer = ParentSkyContainer,
    lessonBackdrop = LessonLavenderBackdrop,
    pastelMint = PastelMintSurface,
    pastelPeach = PastelPeachSurface,
    pastelLavender = PastelLavenderSurface,
    pastelSky = PastelSkySurface,
    pastelYellow = PastelYellowSurface,
    speechBubble = SpeechBubbleCream,
    chipOnTint = Color.White,
    coralInk = Color(0xFFD84F30),
    successInk = Color(0xFF158462),
    warningInk = Color(0xFFB87B06),
    violetInk = Color(0xFF5A46C7),
    progressTrack = Color(0xFFE7DFD3),
    drawingStroke = Color(0xFF1D2C6E),
    drawingPlate = Color(0xFFFFFFFF)
)

val DarkQuizzyColors = QuizzyExtendedColors(
    isDark = true,
    headingInk = Color(0xFFF2F6FF),
    bodyInk = Color(0xFFEDF2FC),
    secondaryInk = Color(0xFFC5D0E6),
    navyActionContainer = Color(0xFF2D66E8),
    navyActionOnContainer = Color.White,
    parentCanvas = Color(0xFF0C1426),
    parentSkyAccent = Color(0xFF60A5FA),
    parentSkyContainer = Color(0xFF1A3154),
    lessonBackdrop = Color(0xFF181E38),
    pastelMint = Color(0xFF133638),
    pastelPeach = Color(0xFF3E2220),
    pastelLavender = Color(0xFF262146),
    pastelSky = Color(0xFF162D4A),
    pastelYellow = Color(0xFF382E14),
    speechBubble = Color(0xFF23314B),
    chipOnTint = Color(0xFF172238),
    coralInk = Color(0xFFFF9B82),
    successInk = Color(0xFF52E5B7),
    warningInk = Color(0xFFFFD46B),
    violetInk = Color(0xFFBAA9FF),
    progressTrack = Color(0xFF2D3E5E),
    drawingStroke = Color(0xFFE6EEFF),
    drawingPlate = Color(0xFF1E2B45)
)

val LocalQuizzyColors = staticCompositionLocalOf { LightQuizzyColors }

object QuizzyTheme {
    val colors: QuizzyExtendedColors
        @Composable
        get() = LocalQuizzyColors.current
}

private val LightColorScheme = lightColorScheme(
    primary = IndigoPrimary,
    onPrimary = IndigoOnPrimary,
    primaryContainer = IndigoPrimaryContainer,
    onPrimaryContainer = IndigoOnPrimaryContainer,
    secondary = AmberSecondary,
    onSecondary = AmberOnSecondary,
    secondaryContainer = AmberSecondaryContainer,
    onSecondaryContainer = AmberOnSecondaryContainer,
    tertiary = TealTertiary,
    onTertiary = TealOnTertiary,
    tertiaryContainer = TealTertiaryContainer,
    onTertiaryContainer = TealOnTertiaryContainer,
    background = CreamBackground,
    onBackground = OnWarmSurface,
    surface = CreamSurface,
    onSurface = OnWarmSurface,
    surfaceVariant = WarmSurfaceVariant,
    onSurfaceVariant = OnWarmSurfaceVariant,
    outline = WarmOutline,
    outlineVariant = WarmOutline.copy(alpha = 0.8f),
    error = CoralError,
    onError = Color.White,
    errorContainer = CoralErrorContainer,
    onErrorContainer = OnCoralErrorContainer
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkIndigoPrimary,
    onPrimary = DarkIndigoOnPrimary,
    primaryContainer = DarkIndigoContainer,
    onPrimaryContainer = DarkIndigoOnContainer,
    secondary = DarkAmberSecondary,
    onSecondary = DarkAmberOnSecondary,
    secondaryContainer = DarkAmberContainer,
    onSecondaryContainer = DarkAmberOnContainer,
    tertiary = DarkTealTertiary,
    onTertiary = DarkTealOnTertiary,
    tertiaryContainer = DarkTealContainer,
    onTertiaryContainer = DarkTealOnContainer,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutline.copy(alpha = 0.7f),
    error = DarkCoralError,
    onError = DarkCoralOnError,
    errorContainer = DarkCoralErrorContainer,
    onErrorContainer = DarkOnCoralErrorContainer
)

val EduFamilleShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun EduFamilleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkQuizzyColors else LightQuizzyColors

    CompositionLocalProvider(LocalQuizzyColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = EduFamilleShapes,
            content = content
        )
    }
}
