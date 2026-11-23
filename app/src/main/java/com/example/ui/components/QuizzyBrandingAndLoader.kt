package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.QuizzyTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Displays the official Quizzy wordmark logo, automatically choosing the dark-mode
 * or light-mode version for high contrast against the active theme surface.
 */
@Composable
fun QuizzyWordmarkLogo(
    modifier: Modifier = Modifier,
    height: Dp = 44.dp,
    forceDarkSurface: Boolean = false
) {
    val useDarkAsset = forceDarkSurface || QuizzyTheme.colors.isDark
    val resId = if (useDarkAsset) R.drawable.logo_quizzy_dark else R.drawable.logo_quizzy_light

    Image(
        painter = painterResource(id = resId),
        contentDescription = "Quizzy",
        contentScale = ContentScale.Fit,
        modifier = modifier
            .height(height)
            .testTag("quizzy_wordmark_logo")
    )
}

/**
 * Premium 3D open-book page-turning loader for Quizzy.
 * Features a beveled hardcover spine, layered paper stacks on both sides,
 * realistic curved Bezier flipping pages with dynamic lighting & miniature ink lines,
 * warm ambient glow, and subtle academic gold sparkles.
 */
@Composable
fun QuizzyBookLoader(
    modifier: Modifier = Modifier,
    size: Dp = 92.dp,
    message: String? = null
) {
    val qColors = QuizzyTheme.colors
    val isDark = qColors.isDark
    val transition = rememberInfiniteTransition(label = "quizzy_book_loader_v2")

    // Smooth continuous page turn cycle (0 -> 1)
    val pagePhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1280, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "book_page_phase"
    )

    // Gentle levitation breathing for the book
    val floatBob by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "book_float_bob"
    )

    // Sparkle shimmer phase
    val sparklePhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "book_sparkle_phase"
    )

    val coverPrimary = if (isDark) Color(0xFF1F4D99) else Color(0xFF0E347C)
    val coverHighlight = if (isDark) Color(0xFF38BDF8) else Color(0xFF1A56B8)
    val coverRim = if (isDark) Color(0xFF0C224A) else Color(0xFF071E4D)
    val pageFrontBright = if (isDark) Color(0xFFF3F7FF) else Color(0xFFFFFEFA)
    val pageWarmCream = if (isDark) Color(0xFFDCE7FA) else Color(0xFFF4EFE4)
    val pageShade = if (isDark) Color(0xFFB3C7E6) else Color(0xFFDCD3C2)
    val pageLineInk = if (isDark) Color(0xFF7B94B8) else Color(0xFFB6C2D4)
    val accentTeal = if (isDark) Color(0xFF2DD4BF) else Color(0xFF0AD2C0)
    val accentGold = Color(0xFFFFD34E)
    val accentCoral = Color(0xFFFF6B57)
    val ribbonRed = if (isDark) Color(0xFFFF8066) else Color(0xFFE94E3D)

    Column(
        modifier = modifier.testTag("quizzy_book_loader"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val cx = w / 2f
            val bobOffset = floatBob * h * 0.022f
            val baseY = h * 0.78f + bobOffset
            val topY = h * 0.28f + bobOffset
            val halfSpan = w * 0.37f
            val spineArch = h * 0.065f

            // 1. Soft ambient knowledge halo & ground shadow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentTeal.copy(alpha = if (isDark) 0.24f else 0.16f),
                        accentGold.copy(alpha = if (isDark) 0.10f else 0.08f),
                        Color.Transparent
                    ),
                    center = Offset(cx, h * 0.50f),
                    radius = w * 0.48f
                ),
                radius = w * 0.48f,
                center = Offset(cx, h * 0.50f)
            )
            // Soft cast shadow under floating book
            val shadowScale = 1f - floatBob * 0.04f
            drawOval(
                color = (if (isDark) Color.Black else Color(0xFF0B2E7A)).copy(alpha = if (isDark) 0.36f else 0.14f),
                topLeft = Offset(cx - halfSpan * 0.92f * shadowScale, h * 0.85f),
                size = Size(halfSpan * 1.84f * shadowScale, h * 0.075f)
            )

            // 2. Hardcover 3D binding boards (left & right with gradient bevels)
            val coverThickness = h * 0.06f
            val leftBoard = Path().apply {
                moveTo(cx, baseY + coverThickness * 0.7f)
                quadraticTo(cx - halfSpan * 0.5f, baseY + coverThickness * 1.15f, cx - halfSpan - w * 0.045f, baseY + coverThickness * 0.35f)
                lineTo(cx - halfSpan - w * 0.03f, topY + h * 0.08f)
                quadraticTo(cx - halfSpan * 0.5f, topY + h * 0.02f, cx, topY + spineArch * 1.3f)
                close()
            }
            val rightBoard = Path().apply {
                moveTo(cx, baseY + coverThickness * 0.7f)
                quadraticTo(cx + halfSpan * 0.5f, baseY + coverThickness * 1.15f, cx + halfSpan + w * 0.045f, baseY + coverThickness * 0.35f)
                lineTo(cx + halfSpan + w * 0.03f, topY + h * 0.08f)
                quadraticTo(cx + halfSpan * 0.5f, topY + h * 0.02f, cx, topY + spineArch * 1.3f)
                close()
            }
            drawPath(
                path = leftBoard,
                brush = Brush.linearGradient(
                    colors = listOf(coverRim, coverPrimary, coverHighlight),
                    start = Offset(cx - halfSpan, baseY),
                    end = Offset(cx, topY)
                )
            )
            drawPath(
                path = rightBoard,
                brush = Brush.linearGradient(
                    colors = listOf(coverHighlight, coverPrimary, coverRim),
                    start = Offset(cx, topY),
                    end = Offset(cx + halfSpan, baseY)
                )
            )

            // Bookmark ribbon peeking neatly below the spine
            val ribbonPath = Path().apply {
                moveTo(cx - w * 0.026f, baseY + coverThickness * 0.4f)
                lineTo(cx + w * 0.026f, baseY + coverThickness * 0.4f)
                lineTo(cx + w * 0.026f, baseY + coverThickness * 2.15f)
                lineTo(cx, baseY + coverThickness * 1.72f)
                lineTo(cx - w * 0.026f, baseY + coverThickness * 2.15f)
                close()
            }
            drawPath(path = ribbonPath, color = ribbonRed)

            // 3. Layered paper stacks (3 thin realistic edges on left & right)
            for (layer in 2 downTo 0) {
                val inset = layer * w * 0.014f
                val yLift = layer * h * 0.017f
                val fillColor = when (layer) {
                    2 -> pageShade
                    1 -> pageWarmCream
                    else -> pageFrontBright
                }
                val leftStack = Path().apply {
                    moveTo(cx, baseY - yLift)
                    quadraticTo(cx - halfSpan * 0.52f, baseY - spineArch * 0.9f - yLift, cx - halfSpan + inset, baseY - yLift - h * 0.02f)
                    lineTo(cx - halfSpan + inset + w * 0.01f, topY + h * 0.04f - yLift * 0.35f)
                    quadraticTo(cx - halfSpan * 0.52f, topY - spineArch * 0.55f - yLift * 0.35f, cx, topY + spineArch * 0.9f - yLift * 0.35f)
                    close()
                }
                val rightStack = Path().apply {
                    moveTo(cx, baseY - yLift)
                    quadraticTo(cx + halfSpan * 0.52f, baseY - spineArch * 0.9f - yLift, cx + halfSpan - inset, baseY - yLift - h * 0.02f)
                    lineTo(cx + halfSpan - inset - w * 0.01f, topY + h * 0.04f - yLift * 0.35f)
                    quadraticTo(cx + halfSpan * 0.52f, topY - spineArch * 0.55f - yLift * 0.35f, cx, topY + spineArch * 0.9f - yLift * 0.35f)
                    close()
                }
                drawPath(path = leftStack, color = fillColor)
                drawPath(path = rightStack, color = fillColor)
                if (layer == 0) {
                    drawPath(
                        path = leftStack,
                        color = pageShade.copy(alpha = 0.7f),
                        style = Stroke(width = w * 0.011f, join = StrokeJoin.Round)
                    )
                    drawPath(
                        path = rightStack,
                        color = pageShade.copy(alpha = 0.7f),
                        style = Stroke(width = w * 0.011f, join = StrokeJoin.Round)
                    )
                }
            }

            // 4. Printed text lines & micro-diagram on static open pages
            val lineStroke = (w * 0.017f).coerceAtLeast(1.4f)
            for (row in 0..3) {
                val frac = 0.25f + row * 0.165f
                val ly = topY + (baseY - topY) * frac
                val leftEnd = if (row == 3) cx - halfSpan * 0.36f else cx - halfSpan * 0.19f
                val rightEnd = if (row == 0) cx + halfSpan * 0.62f else cx + halfSpan * 0.76f
                drawLine(
                    color = if (row == 0) accentTeal.copy(alpha = 0.65f) else pageLineInk.copy(alpha = 0.55f),
                    start = Offset(cx - halfSpan * 0.76f, ly),
                    end = Offset(leftEnd, ly - spineArch * 0.18f),
                    strokeWidth = if (row == 0) lineStroke * 1.25f else lineStroke,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = if (row == 0) accentCoral.copy(alpha = 0.55f) else pageLineInk.copy(alpha = 0.55f),
                    start = Offset(cx + halfSpan * 0.18f, ly - spineArch * 0.18f),
                    end = Offset(rightEnd, ly),
                    strokeWidth = if (row == 0) lineStroke * 1.25f else lineStroke,
                    cap = StrokeCap.Round
                )
            }

            // Central gutter shadow for realistic depth
            drawLine(
                brush = Brush.verticalGradient(
                    colors = listOf(coverRim.copy(alpha = 0.38f), coverRim.copy(alpha = 0.65f)),
                    startY = topY + spineArch * 0.8f,
                    endY = baseY
                ),
                start = Offset(cx, topY + spineArch * 0.85f),
                end = Offset(cx, baseY + coverThickness * 0.35f),
                strokeWidth = w * 0.026f,
                cap = StrokeCap.Round
            )

            // 5. Two staggered smoothly turning 3D curved sheets
            val sheetPhases = listOf(pagePhase, (pagePhase + 0.5f) % 1f)
            sheetPhases
                .sortedBy { p -> sin(p * PI).toFloat() }
                .forEach { rawP ->
                    val eased = easeInOutCubic(rawP)
                    drawCurvedFlippingSheet(
                        cx = cx,
                        topY = topY + spineArch * 0.45f,
                        baseY = baseY - h * 0.03f,
                        halfSpan = halfSpan * 0.96f,
                        liftMax = h * 0.21f,
                        progress = eased,
                        brightColor = pageFrontBright,
                        midColor = pageWarmCream,
                        shadowColor = pageShade,
                        borderColor = accentTeal.copy(alpha = 0.58f),
                        inkColor = pageLineInk.copy(alpha = 0.62f),
                        accentColor = accentTeal
                    )
                }

            // 6. Floating academic four-point sparkles above the turning pages
            drawSparkleStar(
                center = Offset(cx - w * 0.20f, topY - h * 0.09f - sin(sparklePhase * 2 * PI).toFloat() * h * 0.03f),
                radius = w * (0.042f + 0.015f * sin(sparklePhase * 2 * PI).toFloat()),
                color = accentGold.copy(alpha = 0.88f)
            )
            drawSparkleStar(
                center = Offset(cx + w * 0.22f, topY - h * 0.13f + cos(sparklePhase * 2 * PI).toFloat() * h * 0.025f),
                radius = w * (0.034f + 0.014f * cos(sparklePhase * 2 * PI).toFloat()),
                color = accentTeal.copy(alpha = 0.85f)
            )
            drawSparkleStar(
                center = Offset(cx + w * 0.02f, topY - h * 0.18f - sin((sparklePhase + 0.35f) * 2 * PI).toFloat() * h * 0.022f),
                radius = w * (0.026f + 0.010f * sin((sparklePhase + 0.35f) * 2 * PI).toFloat()),
                color = accentCoral.copy(alpha = 0.82f)
            )
        }

        if (!message.isNullOrBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = qColors.bodyInk,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Refined mini page-turning book loader designed specifically for generation buttons
 * (Quiz, Cours, Exercice, Image IA, Validation IA).
 * Renders a crisp 3D miniature open book with beveled cover cradle, layered pages,
 * curved flipping sheet with sheen, and a golden sparkle accent.
 */
@Composable
fun QuizzyInlineLoader(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    val transition = rememberInfiniteTransition(label = "quizzy_inline_book_v2")
    val flip by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 980, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "inline_book_flip"
    )

    Canvas(
        modifier = modifier
            .size(size)
            .testTag("quizzy_inline_book_loader")
    ) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val topY = h * 0.25f
        val baseY = h * 0.80f
        val halfSpan = w * 0.40f
        val strokeW = (w * 0.075f).coerceAtLeast(1.5f)
        val goldAccent = Color(0xFFFFD34E)

        // Subtle glowing cradle beneath the spine
        val coverPath = Path().apply {
            moveTo(cx - halfSpan - w * 0.05f, baseY + h * 0.06f)
            quadraticTo(cx - halfSpan * 0.45f, baseY + h * 0.13f, cx, baseY + h * 0.04f)
            quadraticTo(cx + halfSpan * 0.45f, baseY + h * 0.13f, cx + halfSpan + w * 0.05f, baseY + h * 0.06f)
        }
        drawPath(
            path = coverPath,
            color = tint.copy(alpha = 0.88f),
            style = Stroke(width = strokeW * 1.15f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Left & right open pages with smooth book-arch curvature
        val leftPage = Path().apply {
            moveTo(cx, baseY - h * 0.01f)
            quadraticTo(cx - halfSpan * 0.48f, baseY - h * 0.09f, cx - halfSpan, baseY - h * 0.02f)
            lineTo(cx - halfSpan * 0.94f, topY + h * 0.05f)
            quadraticTo(cx - halfSpan * 0.48f, topY - h * 0.03f, cx, topY + h * 0.07f)
            close()
        }
        val rightPage = Path().apply {
            moveTo(cx, baseY - h * 0.01f)
            quadraticTo(cx + halfSpan * 0.48f, baseY - h * 0.09f, cx + halfSpan, baseY - h * 0.02f)
            lineTo(cx + halfSpan * 0.94f, topY + h * 0.05f)
            quadraticTo(cx + halfSpan * 0.48f, topY - h * 0.03f, cx, topY + h * 0.07f)
            close()
        }
        drawPath(leftPage, color = tint.copy(alpha = 0.24f))
        drawPath(rightPage, color = tint.copy(alpha = 0.18f))
        drawPath(leftPage, color = tint, style = Stroke(width = strokeW * 0.85f, join = StrokeJoin.Round))
        drawPath(rightPage, color = tint, style = Stroke(width = strokeW * 0.85f, join = StrokeJoin.Round))

        // Animated curved turning page (eased right-to-left)
        val eased = easeInOutCubic(flip)
        val angle = eased * PI
        val tipX = cx + (halfSpan * 0.94f * cos(angle)).toFloat()
        val archLift = (h * 0.24f * sin(angle)).toFloat()
        val ctrlX = cx + (tipX - cx) * 0.52f

        val turningSheet = Path().apply {
            moveTo(cx, baseY - h * 0.01f)
            quadraticTo(ctrlX, baseY - archLift * 0.65f, tipX, baseY - h * 0.03f - archLift * 0.45f)
            lineTo(tipX, topY + h * 0.04f - archLift * 0.65f)
            quadraticTo(ctrlX, topY - archLift * 0.95f, cx, topY + h * 0.07f)
            close()
        }
        drawPath(turningSheet, color = tint.copy(alpha = 0.55f + 0.35f * sin(angle).toFloat()))
        drawPath(turningSheet, color = tint, style = Stroke(width = strokeW * 0.9f, join = StrokeJoin.Round))

        // Crisp center spine highlight
        drawLine(
            color = tint,
            start = Offset(cx, topY + h * 0.07f),
            end = Offset(cx, baseY + h * 0.04f),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )

        // Subtle animated gold sparkle above the turning sheet
        val sparkleAlpha = (sin(angle).toFloat()).coerceIn(0.2f, 1f)
        drawSparkleStar(
            center = Offset(cx + (tipX - cx) * 0.25f, topY - h * 0.10f - archLift * 0.32f),
            radius = w * 0.095f * (0.75f + 0.35f * sparkleAlpha),
            color = goldAccent.copy(alpha = sparkleAlpha)
        )
    }
}

private fun DrawScope.drawCurvedFlippingSheet(
    cx: Float,
    topY: Float,
    baseY: Float,
    halfSpan: Float,
    liftMax: Float,
    progress: Float,
    brightColor: Color,
    midColor: Color,
    shadowColor: Color,
    borderColor: Color,
    inkColor: Color,
    accentColor: Color
) {
    val angle = progress * PI
    val cosA = cos(angle).toFloat()
    val sinA = sin(angle).toFloat()
    val tipX = cx + halfSpan * cosA
    val lift = liftMax * sinA
    val midX = cx + (tipX - cx) * 0.52f

    // Soft dynamic shadow cast onto the underlying pages
    if (sinA > 0.08f) {
        val shadowEndX = cx + halfSpan * 0.85f * cosA
        val shadowPath = Path().apply {
            moveTo(cx, baseY)
            lineTo(shadowEndX, baseY - lift * 0.06f)
            lineTo(shadowEndX, topY + (baseY - topY) * 0.08f)
            lineTo(cx, topY)
            close()
        }
        drawPath(
            path = shadowPath,
            color = Color.Black.copy(alpha = 0.08f * sinA)
        )
    }

    val sheetPath = Path().apply {
        moveTo(cx, baseY)
        quadraticTo(
            midX,
            baseY - lift * 0.82f,
            tipX,
            baseY - lift * 0.34f
        )
        lineTo(
            tipX,
            topY - lift * 0.46f
        )
        quadraticTo(
            midX,
            topY - lift * 0.96f,
            cx,
            topY
        )
        close()
    }

    // Dynamic 3D lighting: brighter on right half, gently shaded as it curls over left
    val nearSpineColor = if (cosA >= 0f) midColor else shadowColor
    val edgeColor = if (cosA >= 0f) brightColor else midColor
    drawPath(
        path = sheetPath,
        brush = Brush.linearGradient(
            colors = listOf(nearSpineColor, edgeColor),
            start = Offset(cx, (topY + baseY) / 2f),
            end = Offset(tipX, (topY + baseY) / 2f)
        )
    )

    // Perspective-foreshortened text lines on the turning sheet
    if (kotlin.math.abs(cosA) > 0.24f) {
        val lineW = (size.width * 0.013f).coerceAtLeast(1.1f)
        for (row in 0..2) {
            val f = 0.28f + row * 0.20f
            val ySpine = topY + (baseY - topY) * f
            val yEdge = (topY - lift * 0.46f) + ((baseY - lift * 0.34f) - (topY - lift * 0.46f)) * f
            val x1 = cx + (tipX - cx) * 0.20f
            val x2 = cx + (tipX - cx) * (if (row == 0) 0.68f else 0.78f)
            val y1 = ySpine + (yEdge - ySpine) * 0.20f - lift * 0.18f
            val y2 = ySpine + (yEdge - ySpine) * 0.78f - lift * 0.10f
            drawLine(
                color = if (row == 0) accentColor.copy(alpha = 0.55f) else inkColor,
                start = Offset(x1, y1),
                end = Offset(x2, y2),
                strokeWidth = lineW,
                cap = StrokeCap.Round
            )
        }
    }

    drawPath(
        path = sheetPath,
        color = borderColor,
        style = Stroke(width = (size.width * 0.015f).coerceAtLeast(1.2f), join = StrokeJoin.Round)
    )
}

private fun DrawScope.drawSparkleStar(
    center: Offset,
    radius: Float,
    color: Color
) {
    if (radius <= 0.5f) return
    val inner = radius * 0.30f
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        lineTo(center.x + inner, center.y - inner)
        lineTo(center.x + radius, center.y)
        lineTo(center.x + inner, center.y + inner)
        lineTo(center.x, center.y + radius)
        lineTo(center.x - inner, center.y + inner)
        lineTo(center.x - radius, center.y)
        lineTo(center.x - inner, center.y - inner)
        close()
    }
    drawPath(path = path, color = color)
}

private fun easeInOutCubic(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    return if (x < 0.5f) 4f * x * x * x else 1f - (-2f * x + 2f) * (-2f * x + 2f) * (-2f * x + 2f) / 2f
}
