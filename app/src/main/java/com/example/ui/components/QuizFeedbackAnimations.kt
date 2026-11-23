package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.QuizzyTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Multi-variant educational feedback animation aligned with Quizzy's visual identity
 * (deep navy, turquoise, warm gold, soft coral).
 *
 * - Success variants (4 rotating styles):
 *   0: Academic Starburst & Confetti Celebration
 *   1: Graduation Cap (Mortarboard) Toss with Gold Tassel
 *   2: Glowing Knowledge Lightbulb & Idea Waves
 *   3: Ascending Academic Pencil-Rocket with Stardust
 *
 * - Failure / Encouragement variants (3 rotating kind, non-guilting styles):
 *   0: Guiding Brass Compass Searching for the Right Path
 *   1: Open Study Book Gentle Re-Check Ripple
 *   2: Curious Magnifying Glass Scanning Clue Waves
 */
@Composable
fun QuizResultFeedbackAnimation(
    isSuccess: Boolean,
    variantIndex: Int,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    isPartial: Boolean = false
) {
    val qColors = QuizzyTheme.colors
    val isDark = qColors.isDark
    val normalizedVariant = if (isSuccess) {
        ((variantIndex % 4) + 4) % 4
    } else {
        ((variantIndex % 3) + 3) % 3
    }

    var appeared by remember(isSuccess, isPartial, variantIndex) { mutableStateOf(false) }
    LaunchedEffect(isSuccess, isPartial, variantIndex) {
        appeared = true
    }

    val popScale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.55f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "feedback_anim_pop"
    )

    val infinite = rememberInfiniteTransition(label = "quiz_feedback_anim_loop")
    val pulse by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "feedback_pulse"
    )
    val orbit by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "feedback_orbit"
    )

    val navyInk = if (isDark) Color(0xFF93C5FD) else Color(0xFF0B2E7A)
    val deepNavy = if (isDark) Color(0xFF1E3A8A) else Color(0xFF0E347C)
    val teal = if (isDark) Color(0xFF2DD4BF) else Color(0xFF0AD2C0)
    val emerald = if (isDark) Color(0xFF34D399) else Color(0xFF159A63)
    val gold = Color(0xFFFFD34E)
    val amber = if (isDark) Color(0xFFFBBF24) else Color(0xFFE08B1A)
    val coral = if (isDark) Color(0xFFFF8674) else Color(0xFFFF644E)
    val cream = if (isDark) Color(0xFFEAF1FF) else Color(0xFFFFFDF8)

    val tagName = when {
        isPartial -> "quiz_feedback_anim_partial"
        isSuccess -> "quiz_feedback_anim_success_$normalizedVariant"
        else -> "quiz_feedback_anim_failure_$normalizedVariant"
    }

    Canvas(
        modifier = modifier
            .size(size)
            .scale(popScale)
            .testTag(tagName)
    ) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f
        val r = minOf(w, h) * 0.44f

        when {
            isPartial -> {
                // Warm half-filled laurel & glowing balance dial for partially correct exercise answers
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(amber.copy(alpha = 0.28f), Color.Transparent),
                        center = Offset(cx, cy),
                        radius = r * (1.05f + 0.12f * pulse)
                    ),
                    radius = r * (1.05f + 0.12f * pulse),
                    center = Offset(cx, cy)
                )
                drawArc(
                    color = amber.copy(alpha = 0.26f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(cx - r * 0.78f, cy - r * 0.78f),
                    size = Size(r * 1.56f, r * 1.56f),
                    style = Stroke(width = r * 0.16f, cap = StrokeCap.Round)
                )
                drawArc(
                    color = amber,
                    startAngle = -90f,
                    sweepAngle = 215f + 20f * pulse,
                    useCenter = false,
                    topLeft = Offset(cx - r * 0.78f, cy - r * 0.78f),
                    size = Size(r * 1.56f, r * 1.56f),
                    style = Stroke(width = r * 0.16f, cap = StrokeCap.Round)
                )
                drawFivePointStar(
                    center = Offset(cx, cy),
                    outerRadius = r * (0.40f + 0.05f * pulse),
                    color = gold
                )
            }

            isSuccess -> when (normalizedVariant) {
                0 -> {
                    // Variant 0: Academic Starburst & Confetti Ribbons
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(gold.copy(alpha = 0.34f), teal.copy(alpha = 0.12f), Color.Transparent),
                            center = Offset(cx, cy),
                            radius = r * (1.05f + 0.15f * pulse)
                        ),
                        radius = r * (1.05f + 0.15f * pulse),
                        center = Offset(cx, cy)
                    )
                    // Orbiting confetti particles
                    val confettiColors = listOf(teal, coral, gold, emerald, navyInk, gold)
                    for (i in 0 until 6) {
                        val a = (i * 60.0 + orbit * 360.0) * PI / 180.0
                        val dist = r * (0.82f + 0.12f * sin((orbit * 2 + i * 0.35f) * PI).toFloat())
                        val px = cx + (dist * cos(a)).toFloat()
                        val py = cy + (dist * sin(a)).toFloat()
                        drawCircle(
                            color = confettiColors[i],
                            radius = r * (if (i % 2 == 0) 0.11f else 0.085f),
                            center = Offset(px, py)
                        )
                    }
                    drawCircle(
                        brush = Brush.linearGradient(listOf(emerald, teal)),
                        radius = r * 0.62f,
                        center = Offset(cx, cy)
                    )
                    drawFivePointStar(
                        center = Offset(cx, cy),
                        outerRadius = r * (0.43f + 0.06f * pulse),
                        color = gold
                    )
                }

                1 -> {
                    // Variant 1: Graduation Cap (Mortarboard) Toss with Gold Tassel
                    val bounceY = -r * 0.12f * pulse
                    drawCircle(
                        color = teal.copy(alpha = 0.16f + 0.08f * pulse),
                        radius = r * 0.94f,
                        center = Offset(cx, cy)
                    )
                    // Radiating celebratory ticks
                    for (i in 0 until 6) {
                        val a = (-150.0 + i * 24.0) * PI / 180.0
                        val p1 = Offset(cx + (r * 0.72f * cos(a)).toFloat(), cy + bounceY + (r * 0.72f * sin(a)).toFloat())
                        val p2 = Offset(cx + (r * (0.90f + 0.10f * pulse) * cos(a)).toFloat(), cy + bounceY + (r * (0.90f + 0.10f * pulse) * sin(a)).toFloat())
                        drawLine(gold, p1, p2, strokeWidth = r * 0.08f, cap = StrokeCap.Round)
                    }
                    // Mortarboard top diamond
                    val capY = cy - r * 0.08f + bounceY
                    val capTop = Path().apply {
                        moveTo(cx, capY - r * 0.40f)
                        lineTo(cx + r * 0.72f, capY - r * 0.06f)
                        lineTo(cx, capY + r * 0.24f)
                        lineTo(cx - r * 0.72f, capY - r * 0.06f)
                        close()
                    }
                    drawPath(capTop, color = deepNavy)
                    drawPath(capTop, color = teal, style = Stroke(width = r * 0.055f, join = StrokeJoin.Round))
                    // Cap skull base
                    val capBase = Path().apply {
                        moveTo(cx - r * 0.42f, capY + r * 0.10f)
                        lineTo(cx - r * 0.36f, capY + r * 0.44f)
                        quadraticTo(cx, capY + r * 0.62f, cx + r * 0.36f, capY + r * 0.44f)
                        lineTo(cx + r * 0.42f, capY + r * 0.10f)
                        close()
                    }
                    drawPath(capBase, color = navyInk.copy(alpha = 0.85f))
                    // Swaying gold tassel
                    val tasselSway = r * 0.10f * sin(orbit * 2 * PI).toFloat()
                    drawLine(
                        color = gold,
                        start = Offset(cx + r * 0.56f, capY + r * 0.02f),
                        end = Offset(cx + r * 0.60f + tasselSway, capY + r * 0.48f),
                        strokeWidth = r * 0.08f,
                        cap = StrokeCap.Round
                    )
                    drawCircle(gold, radius = r * 0.10f, center = Offset(cx + r * 0.60f + tasselSway, capY + r * 0.50f))
                }

                2 -> {
                    // Variant 2: Glowing Knowledge Lightbulb & Idea Waves
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(gold.copy(alpha = 0.45f), teal.copy(alpha = 0.14f), Color.Transparent),
                            center = Offset(cx, cy - r * 0.12f),
                            radius = r * (0.95f + 0.18f * pulse)
                        ),
                        radius = r * (0.95f + 0.18f * pulse),
                        center = Offset(cx, cy - r * 0.12f)
                    )
                    // Idea rays pulsing outward
                    for (i in 0 until 7) {
                        val a = (-210.0 + i * 40.0) * PI / 180.0
                        val inner = r * 0.66f
                        val outer = r * (0.85f + 0.14f * pulse)
                        drawLine(
                            color = if (i % 2 == 0) gold else teal,
                            start = Offset(cx + (inner * cos(a)).toFloat(), cy - r * 0.12f + (inner * sin(a)).toFloat()),
                            end = Offset(cx + (outer * cos(a)).toFloat(), cy - r * 0.12f + (outer * sin(a)).toFloat()),
                            strokeWidth = r * 0.085f,
                            cap = StrokeCap.Round
                        )
                    }
                    // Bulb glass
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFFF7CC), gold, amber),
                            center = Offset(cx - r * 0.12f, cy - r * 0.24f),
                            radius = r * 0.58f
                        ),
                        radius = r * 0.46f,
                        center = Offset(cx, cy - r * 0.12f)
                    )
                    // Screw base
                    drawRoundRect(
                        color = deepNavy,
                        topLeft = Offset(cx - r * 0.22f, cy + r * 0.30f),
                        size = Size(r * 0.44f, r * 0.26f),
                        cornerRadius = CornerRadius(r * 0.08f)
                    )
                    drawLine(
                        color = teal,
                        start = Offset(cx - r * 0.18f, cy + r * 0.42f),
                        end = Offset(cx + r * 0.18f, cy + r * 0.42f),
                        strokeWidth = r * 0.06f,
                        cap = StrokeCap.Round
                    )
                    // Checkmark filament inside bulb
                    val check = Path().apply {
                        moveTo(cx - r * 0.16f, cy - r * 0.12f)
                        lineTo(cx - r * 0.03f, cy + r * 0.02f)
                        lineTo(cx + r * 0.19f, cy - r * 0.22f)
                    }
                    drawPath(check, color = deepNavy, style = Stroke(width = r * 0.095f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }

                else -> {
                    // Variant 3: Ascending Academic Rocket & Golden Trail
                    val lift = -r * 0.14f * pulse
                    drawCircle(
                        color = emerald.copy(alpha = 0.16f),
                        radius = r * 0.92f,
                        center = Offset(cx, cy)
                    )
                    // Exhaust flame
                    val flame = Path().apply {
                        moveTo(cx - r * 0.16f, cy + r * 0.34f + lift)
                        lineTo(cx, cy + r * (0.76f + 0.14f * pulse) + lift)
                        lineTo(cx + r * 0.16f, cy + r * 0.34f + lift)
                        close()
                    }
                    drawPath(flame, brush = Brush.verticalGradient(listOf(gold, coral)))
                    // Rocket fins
                    val leftFin = Path().apply {
                        moveTo(cx - r * 0.24f, cy + r * 0.12f + lift)
                        lineTo(cx - r * 0.48f, cy + r * 0.40f + lift)
                        lineTo(cx - r * 0.20f, cy + r * 0.36f + lift)
                        close()
                    }
                    val rightFin = Path().apply {
                        moveTo(cx + r * 0.24f, cy + r * 0.12f + lift)
                        lineTo(cx + r * 0.48f, cy + r * 0.40f + lift)
                        lineTo(cx + r * 0.20f, cy + r * 0.36f + lift)
                        close()
                    }
                    drawPath(leftFin, color = coral)
                    drawPath(rightFin, color = coral)
                    // Rocket fuselage
                    val body = Path().apply {
                        moveTo(cx, cy - r * 0.66f + lift)
                        quadraticTo(cx + r * 0.34f, cy - r * 0.18f + lift, cx + r * 0.24f, cy + r * 0.36f + lift)
                        lineTo(cx - r * 0.24f, cy + r * 0.36f + lift)
                        quadraticTo(cx - r * 0.34f, cy - r * 0.18f + lift, cx, cy - r * 0.66f + lift)
                        close()
                    }
                    drawPath(body, brush = Brush.linearGradient(listOf(cream, teal.copy(alpha = 0.85f))))
                    drawPath(body, color = deepNavy, style = Stroke(width = r * 0.065f, join = StrokeJoin.Round))
                    // Window porthole
                    drawCircle(deepNavy, radius = r * 0.16f, center = Offset(cx, cy - r * 0.06f + lift))
                    drawCircle(gold, radius = r * 0.09f, center = Offset(cx, cy - r * 0.06f + lift))
                }
            }

            else -> when (normalizedVariant) {
                0 -> {
                    // Failure Variant 0: Guiding Brass Compass Finding the Way
                    drawCircle(
                        color = coral.copy(alpha = 0.14f + 0.08f * pulse),
                        radius = r * (0.94f + 0.08f * pulse),
                        center = Offset(cx, cy)
                    )
                    // Brass bezel
                    drawCircle(
                        brush = Brush.linearGradient(listOf(gold, amber)),
                        radius = r * 0.78f,
                        center = Offset(cx, cy)
                    )
                    drawCircle(
                        color = cream,
                        radius = r * 0.62f,
                        center = Offset(cx, cy)
                    )
                    // Cardinal tick marks
                    for (i in 0 until 4) {
                        val a = i * PI / 2.0
                        drawLine(
                            color = deepNavy.copy(alpha = 0.55f),
                            start = Offset(cx + (r * 0.44f * cos(a)).toFloat(), cy + (r * 0.44f * sin(a)).toFloat()),
                            end = Offset(cx + (r * 0.58f * cos(a)).toFloat(), cy + (r * 0.58f * sin(a)).toFloat()),
                            strokeWidth = r * 0.065f,
                            cap = StrokeCap.Round
                        )
                    }
                    // Gently oscillating compass needle
                    val needleAngle = -28f + 56f * pulse
                    rotate(degrees = needleAngle, pivot = Offset(cx, cy)) {
                        val north = Path().apply {
                            moveTo(cx, cy - r * 0.46f)
                            lineTo(cx + r * 0.13f, cy)
                            lineTo(cx - r * 0.13f, cy)
                            close()
                        }
                        val south = Path().apply {
                            moveTo(cx, cy + r * 0.46f)
                            lineTo(cx + r * 0.13f, cy)
                            lineTo(cx - r * 0.13f, cy)
                            close()
                        }
                        drawPath(north, color = coral)
                        drawPath(south, color = deepNavy)
                    }
                    drawCircle(gold, radius = r * 0.09f, center = Offset(cx, cy))
                }

                1 -> {
                    // Failure Variant 1: Gently Tilting Open Book Re-Check Ripple
                    val rippleR = r * (0.72f + 0.32f * orbit)
                    drawCircle(
                        color = amber.copy(alpha = (1f - orbit) * 0.35f),
                        radius = rippleR,
                        center = Offset(cx, cy),
                        style = Stroke(width = r * 0.07f)
                    )
                    val tilt = -5f + 10f * pulse
                    rotate(degrees = tilt, pivot = Offset(cx, cy)) {
                        val bw = r * 0.72f
                        val bh = r * 0.56f
                        val leftP = Path().apply {
                            moveTo(cx, cy + bh * 0.48f)
                            quadraticTo(cx - bw * 0.5f, cy + bh * 0.32f, cx - bw, cy + bh * 0.48f)
                            lineTo(cx - bw * 0.92f, cy - bh * 0.42f)
                            quadraticTo(cx - bw * 0.48f, cy - bh * 0.58f, cx, cy - bh * 0.36f)
                            close()
                        }
                        val rightP = Path().apply {
                            moveTo(cx, cy + bh * 0.48f)
                            quadraticTo(cx + bw * 0.5f, cy + bh * 0.32f, cx + bw, cy + bh * 0.48f)
                            lineTo(cx + bw * 0.92f, cy - bh * 0.42f)
                            quadraticTo(cx + bw * 0.48f, cy - bh * 0.58f, cx, cy - bh * 0.36f)
                            close()
                        }
                        drawPath(leftP, color = cream)
                        drawPath(rightP, color = cream)
                        drawPath(leftP, color = coral, style = Stroke(width = r * 0.075f, join = StrokeJoin.Round))
                        drawPath(rightP, color = coral, style = Stroke(width = r * 0.075f, join = StrokeJoin.Round))
                        // Subtle clue lines
                        drawLine(amber, Offset(cx - bw * 0.68f, cy - bh * 0.10f), Offset(cx - bw * 0.22f, cy - bh * 0.06f), r * 0.065f, StrokeCap.Round)
                        drawLine(teal, Offset(cx + bw * 0.22f, cy - bh * 0.06f), Offset(cx + bw * 0.68f, cy - bh * 0.10f), r * 0.065f, StrokeCap.Round)
                        drawLine(deepNavy.copy(alpha = 0.45f), Offset(cx - bw * 0.68f, cy + bh * 0.14f), Offset(cx - bw * 0.26f, cy + bh * 0.16f), r * 0.055f, StrokeCap.Round)
                        drawLine(deepNavy.copy(alpha = 0.45f), Offset(cx + bw * 0.24f, cy + bh * 0.16f), Offset(cx + bw * 0.64f, cy + bh * 0.14f), r * 0.055f, StrokeCap.Round)
                    }
                }

                else -> {
                    // Failure Variant 2: Curious Magnifying Glass Scanning for Clues
                    drawCircle(
                        color = amber.copy(alpha = 0.15f),
                        radius = r * 0.92f,
                        center = Offset(cx, cy)
                    )
                    // Subtle notebook lines behind the lens
                    for (i in -1..1) {
                        val ly = cy + i * r * 0.24f
                        drawLine(
                            color = deepNavy.copy(alpha = 0.22f),
                            start = Offset(cx - r * 0.56f, ly),
                            end = Offset(cx + r * 0.56f, ly),
                            strokeWidth = r * 0.055f,
                            cap = StrokeCap.Round
                        )
                    }
                    // Orbiting magnifier lens
                    val dx = cos(orbit * 2 * PI).toFloat() * r * 0.14f
                    val dy = sin(orbit * 2 * PI).toFloat() * r * 0.10f
                    val lensCenter = Offset(cx - r * 0.10f + dx, cy - r * 0.12f + dy)
                    val lensRadius = r * 0.42f
                    // Handle
                    drawLine(
                        color = coral,
                        start = Offset(lensCenter.x + lensRadius * 0.72f, lensCenter.y + lensRadius * 0.72f),
                        end = Offset(lensCenter.x + lensRadius * 1.42f, lensCenter.y + lensRadius * 1.42f),
                        strokeWidth = r * 0.16f,
                        cap = StrokeCap.Round
                    )
                    // Glass fill + glare
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.85f), teal.copy(alpha = 0.32f)),
                            center = Offset(lensCenter.x - lensRadius * 0.25f, lensCenter.y - lensRadius * 0.25f),
                            radius = lensRadius
                        ),
                        radius = lensRadius,
                        center = lensCenter
                    )
                    drawCircle(
                        color = deepNavy,
                        radius = lensRadius,
                        center = lensCenter,
                        style = Stroke(width = r * 0.10f)
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawFivePointStar(
    center: Offset,
    outerRadius: Float,
    color: Color
) {
    val innerRadius = outerRadius * 0.44f
    val path = Path()
    for (i in 0 until 10) {
        val radius = if (i % 2 == 0) outerRadius else innerRadius
        val angle = (-90.0 + i * 36.0) * PI / 180.0
        val x = center.x + (radius * cos(angle)).toFloat()
        val y = center.y + (radius * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path = path, color = color)
}
