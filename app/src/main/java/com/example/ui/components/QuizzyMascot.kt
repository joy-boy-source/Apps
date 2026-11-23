package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.MascotYellow
import com.example.ui.theme.MascotYellowDeep
import com.example.ui.theme.QuizzyTheme
import com.example.ui.theme.TealTertiary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Emotional moods for Quizzy's animated fennec fox mascot.
 */
enum class QuizzyMascotMood(
    @DrawableRes val drawableRes: Int,
    val defaultDescription: String
) {
    IDLE(
        drawableRes = R.drawable.img_mascot_idle,
        defaultDescription = "Mascotte Quizzy prête à apprendre"
    ),
    WAVE(
        drawableRes = R.drawable.img_mascot_wave,
        defaultDescription = "Mascotte Quizzy qui te salue avec enthousiasme"
    ),
    THINKING(
        drawableRes = R.drawable.img_mascot_thinking,
        defaultDescription = "Mascotte Quizzy qui réfléchit avec une idée lumineuse"
    ),
    VICTORY(
        drawableRes = R.drawable.img_mascot_victory,
        defaultDescription = "Mascotte Quizzy qui saute de joie"
    ),
    ENCOURAGE(
        drawableRes = R.drawable.img_mascot_encourage,
        defaultDescription = "Mascotte Quizzy qui t'encourage avec le pouce levé"
    )
}

/**
 * Silky-smooth animated mascot component with per-mood motion physics, glowing aura
 * (tuned for both Light and Dark themes), floating mood particles, and optional tap-to-cheer bounce.
 */
@Composable
fun QuizzyAnimatedMascot(
    mood: QuizzyMascotMood = QuizzyMascotMood.IDLE,
    size: Dp = 96.dp,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
    contentDescription: String? = mood.defaultDescription
) {
    val qColors = QuizzyTheme.colors
    val isDark = qColors.isDark

    val infiniteTransition = rememberInfiniteTransition(label = "quizzy_mascot_motion")

    // Core breathing & float oscillation
    val breathPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (mood) {
                    QuizzyMascotMood.VICTORY -> 1150
                    QuizzyMascotMood.WAVE -> 1650
                    QuizzyMascotMood.THINKING -> 2400
                    QuizzyMascotMood.ENCOURAGE -> 1950
                    QuizzyMascotMood.IDLE -> 2200
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "mascot_breath_phase"
    )

    // Secondary particle & sparkle cycle
    val sparklePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "mascot_sparkle_phase"
    )

    // Aura gentle pulse
    val auraPulse by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mascot_aura_pulse"
    )

    // Playful tap-to-cheer spring impulse
    var tapCount by remember { mutableIntStateOf(0) }
    val tapBounce = remember { Animatable(0f) }
    val tapBurst = remember { Animatable(0f) }

    LaunchedEffect(tapCount) {
        if (tapCount > 0) {
            tapBurst.snapTo(1f)
            tapBounce.snapTo(0f)
            tapBounce.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing)
            )
            tapBounce.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
            tapBurst.animateTo(0f, animationSpec = tween(durationMillis = 480))
        }
    }

    val sinVal = sin(breathPhase.toDouble()).toFloat()
    val cosVal = cos(breathPhase.toDouble()).toFloat()

    // Per-mood organic physics
    val baseTranslationYDp = when (mood) {
        QuizzyMascotMood.IDLE -> sinVal * 2.5f
        QuizzyMascotMood.WAVE -> sinVal * 3.5f
        QuizzyMascotMood.THINKING -> sinVal * 2.2f
        QuizzyMascotMood.VICTORY -> -kotlin.math.abs(sinVal) * 7.5f
        QuizzyMascotMood.ENCOURAGE -> sinVal * 2.6f
    }
    val tapHopDp = -tapBounce.value * 9f

    val baseScaleX = when (mood) {
        QuizzyMascotMood.IDLE -> 1f + sinVal * 0.014f
        QuizzyMascotMood.WAVE -> 1f + sinVal * 0.018f
        QuizzyMascotMood.THINKING -> 1f + sinVal * 0.010f
        QuizzyMascotMood.VICTORY -> 1f + cosVal * 0.032f
        QuizzyMascotMood.ENCOURAGE -> 1f + sinVal * 0.020f
    }
    val baseScaleY = when (mood) {
        QuizzyMascotMood.IDLE -> 1f - sinVal * 0.016f
        QuizzyMascotMood.WAVE -> 1f - sinVal * 0.020f
        QuizzyMascotMood.THINKING -> 1f - sinVal * 0.012f
        QuizzyMascotMood.VICTORY -> 1f - cosVal * 0.035f
        QuizzyMascotMood.ENCOURAGE -> 1f - sinVal * 0.018f
    }
    val tapScaleBoost = 1f + tapBounce.value * 0.09f

    val rotationDeg = when (mood) {
        QuizzyMascotMood.IDLE -> sinVal * 1.4f
        QuizzyMascotMood.WAVE -> sinVal * 3.6f
        QuizzyMascotMood.THINKING -> -1.8f + sinVal * 3.2f
        QuizzyMascotMood.VICTORY -> cosVal * 4.2f
        QuizzyMascotMood.ENCOURAGE -> sinVal * 2.4f
    } + (tapBounce.value * if (tapCount % 2 == 0) 6f else -6f)

    // Mood-matched aura palette (luminous enough to lift the teal silhouette cleanly on Dark Mode)
    val auraPrimaryColor = when (mood) {
        QuizzyMascotMood.IDLE -> if (isDark) Color(0xFF4ADAD0) else TealTertiary
        QuizzyMascotMood.WAVE -> if (isDark) MascotYellow else MascotYellowDeep
        QuizzyMascotMood.THINKING -> MascotYellow
        QuizzyMascotMood.VICTORY -> MascotYellow
        QuizzyMascotMood.ENCOURAGE -> if (isDark) Color(0xFFFFC86B) else CoralAccent
    }
    val auraSecondaryColor = when (mood) {
        QuizzyMascotMood.VICTORY -> CoralAccent
        QuizzyMascotMood.ENCOURAGE -> MascotYellow
        QuizzyMascotMood.THINKING -> if (isDark) Color(0xFF56E0D6) else TealTertiary
        else -> if (isDark) Color(0xFF4ADAD0) else MascotYellow
    }
    val auraAlpha = if (isDark) 0.34f else 0.22f

    val interactionSource = remember { MutableInteractionSource() }
    val effectiveSize = if (interactive && size < 48.dp) 48.dp else size

    Box(
        modifier = modifier
            .size(effectiveSize)
            .testTag("quizzy_animated_mascot")
            .then(
                if (interactive) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        role = Role.Button,
                        onClickLabel = "Animer la mascotte Quizzy"
                    ) {
                        tapCount += 1
                    }
                } else {
                    Modifier
                }
            )
            .semantics {
                if (!contentDescription.isNullOrBlank()) {
                    this.contentDescription = contentDescription
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // 1. Soft breathing radial halo + animated mood particles behind/around the mascot
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = this.size.width / 2f
            val cy = this.size.height * 0.52f
            val baseRadius = this.size.minDimension * 0.46f * auraPulse

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        auraPrimaryColor.copy(alpha = auraAlpha),
                        auraSecondaryColor.copy(alpha = auraAlpha * 0.45f),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = baseRadius.coerceAtLeast(1f)
                ),
                radius = baseRadius,
                center = Offset(cx, cy)
            )

            // Mood-specific floating ambient particles
            when (mood) {
                QuizzyMascotMood.VICTORY -> {
                    for (i in 0 until 6) {
                        val angle = (i * 60f + sparklePhase * 360f) * (PI / 180.0)
                        val orbitR = this.size.minDimension * (0.41f + 0.05f * sin((sparklePhase * 2 * PI + i).toDouble()).toFloat())
                        val px = cx + (cos(angle) * orbitR).toFloat()
                        val py = cy * 0.88f + (sin(angle) * orbitR * 0.78f).toFloat()
                        val starSize = this.size.minDimension * (0.045f + 0.018f * ((i + 1) % 2))
                        val starColor = if (i % 2 == 0) MascotYellow else CoralAccent
                        drawFourPointSparkle(
                            center = Offset(px, py),
                            radius = starSize,
                            color = starColor.copy(alpha = 0.88f)
                        )
                    }
                }
                QuizzyMascotMood.THINKING -> {
                    // Soft rising golden idea sparkles on the left side near the lightbulb
                    for (i in 0 until 3) {
                        val t = (sparklePhase + i * 0.33f) % 1f
                        val px = this.size.width * (0.14f + i * 0.06f)
                        val py = this.size.height * (0.36f - t * 0.22f)
                        val alpha = sin(t * PI).toFloat().coerceIn(0f, 1f) * 0.85f
                        drawFourPointSparkle(
                            center = Offset(px, py),
                            radius = this.size.minDimension * (0.036f + (1f - t) * 0.015f),
                            color = MascotYellow.copy(alpha = alpha)
                        )
                    }
                }
                QuizzyMascotMood.ENCOURAGE -> {
                    // Expanding gentle reassuring warmth ring
                    val ringProgress = sparklePhase
                    val ringRadius = this.size.minDimension * (0.28f + ringProgress * 0.20f)
                    val ringAlpha = (1f - ringProgress) * (if (isDark) 0.38f else 0.28f)
                    drawCircle(
                        color = MascotYellow.copy(alpha = ringAlpha),
                        radius = ringRadius,
                        center = Offset(cx, cy),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = this.size.minDimension * 0.02f)
                    )
                }
                QuizzyMascotMood.WAVE, QuizzyMascotMood.IDLE -> {
                    // Subtle twinkling accent sparkles
                    val twinkle1 = (0.45f + 0.55f * sin(sparklePhase * 2 * PI).toFloat()).coerceIn(0f, 1f)
                    val twinkle2 = (0.45f + 0.55f * cos(sparklePhase * 2 * PI).toFloat()).coerceIn(0f, 1f)
                    drawFourPointSparkle(
                        center = Offset(this.size.width * 0.14f, this.size.height * 0.24f),
                        radius = this.size.minDimension * 0.038f * twinkle1,
                        color = MascotYellow.copy(alpha = 0.85f * twinkle1)
                    )
                    drawFourPointSparkle(
                        center = Offset(this.size.width * 0.88f, this.size.height * 0.22f),
                        radius = this.size.minDimension * 0.032f * twinkle2,
                        color = CoralAccent.copy(alpha = 0.78f * twinkle2)
                    )
                }
            }

            // Extra burst sparkles when user taps the mascot!
            if (tapBurst.value > 0.02f) {
                val burstProgress = 1f - tapBurst.value
                for (i in 0 until 5) {
                    val angle = (i * 72f - 90f) * (PI / 180.0)
                    val dist = this.size.minDimension * (0.24f + burstProgress * 0.24f)
                    val px = cx + (cos(angle) * dist).toFloat()
                    val py = cy + (sin(angle) * dist).toFloat()
                    drawFourPointSparkle(
                        center = Offset(px, py),
                        radius = this.size.minDimension * 0.055f * tapBurst.value,
                        color = if (i % 2 == 0) MascotYellow.copy(alpha = tapBurst.value)
                        else CoralAccent.copy(alpha = tapBurst.value)
                    )
                }
            }
        }

        // 2. Smooth mood-switching animated mascot sprite
        AnimatedContent(
            targetState = mood,
            transitionSpec = {
                (fadeIn(animationSpec = tween(220)) + scaleIn(
                    initialScale = 0.88f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )).togetherWith(fadeOut(animationSpec = tween(160)))
            },
            label = "quizzy_mascot_mood_switch"
        ) { activeMood ->
            Image(
                painter = painterResource(id = activeMood.drawableRes),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(size)
                    .testTag("quizzy_mascot_pose_${activeMood.name.lowercase()}")
                    .graphicsLayer {
                        translationY = (baseTranslationYDp + tapHopDp).dp.toPx()
                        scaleX = baseScaleX * tapScaleBoost
                        scaleY = baseScaleY * tapScaleBoost
                        rotationZ = rotationDeg
                        transformOrigin = TransformOrigin(0.5f, 0.72f)
                    }
            )
        }
    }
}

private fun DrawScope.drawFourPointSparkle(
    center: Offset,
    radius: Float,
    color: Color
) {
    if (radius <= 0.5f) return
    val inner = radius * 0.34f
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        quadraticTo(center.x + inner * 0.4f, center.y - inner * 0.4f, center.x + radius, center.y)
        quadraticTo(center.x + inner * 0.4f, center.y + inner * 0.4f, center.x, center.y + radius)
        quadraticTo(center.x - inner * 0.4f, center.y + inner * 0.4f, center.x - radius, center.y)
        quadraticTo(center.x - inner * 0.4f, center.y - inner * 0.4f, center.x, center.y - radius)
        close()
    }
    drawPath(path = path, color = color)
}
