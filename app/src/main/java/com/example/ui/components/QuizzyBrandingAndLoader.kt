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
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val BrandBookNavy = Color(0xFF0B2E7A)
private val BrandBookNavyDark = Color(0xFF5D87F0)
private val BrandPageTurquoise = Color(0xFF0AD2C0)
private val BrandPageTurquoiseSoft = Color(0xFF7BEAE0)
private val BrandPageCream = Color(0xFFFFFBF2)
private val BrandStarCoral = Color(0xFFFF644E)

/**
 * Displays the official Quizzy transparent wordmark logo, automatically switching
 * between light mode and dark mode variants.
 */
@Composable
fun QuizzyWordmarkLogo(
    modifier: Modifier = Modifier,
    height: Dp = 46.dp,
    forceDarkVariant: Boolean? = null
) {
    val useDarkLogo = forceDarkVariant
        ?: (isSystemInDarkTheme() || MaterialTheme.colorScheme.background.luminance() < 0.5f)
    val resId = if (useDarkLogo) R.drawable.logo_quizzy_dark else R.drawable.logo_quizzy_light

    Image(
        painter = painterResource(id = resId),
        contentDescription = "Logo Quizzy",
        contentScale = ContentScale.Fit,
        modifier = modifier
            .height(height)
            .testTag("quizzy_wordmark_logo")
    )
}

/**
 * Modern animated page-turning book loader tailored to Quizzy's art direction:
 * deep navy open book cover, turquoise pages, two staggered 3D-arc flipping pages,
 * and a bobbing coral star with 3 pulsing sparkle rays.
 */
@Composable
fun QuizzyBookLoader(
    modifier: Modifier = Modifier,
    size: Dp = 88.dp,
    message: String? = null
) {
    val isDark = isSystemInDarkTheme() || MaterialTheme.colorScheme.background.luminance() < 0.5f
    val coverColor = if (isDark) BrandBookNavyDark else BrandBookNavy
    val pageSheetColor = if (isDark) Color(0xFFE9F4FF) else BrandPageCream

    val transition = rememberInfiniteTransition(label = "quizzy_book_loader")

    // Primary flipping page progress: 0f (right) -> 1f (left)
    val page1Progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "book_page_1"
    )

    // Secondary staggered flipping page (half cycle offset)
    val page2Progress = (page1Progress + 0.5f) % 1f

    // Star gentle vertical float & sparkle pulse
    val starPulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "star_pulse"
    )

    Column(
        modifier = modifier.testTag("quizzy_book_loader"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val cx = w * 0.50f
            val spineY = h * 0.80f
            val spineTopY = h * 0.50f
            val bookHalfW = w * 0.36f
            val pageH = h * 0.30f

            // 1. Soft grounding shadow under the book
            drawOval(
                color = coverColor.copy(alpha = 0.11f),
                topLeft = Offset(cx - bookHalfW * 0.92f, h * 0.87f),
                size = androidx.compose.ui.geometry.Size(bookHalfW * 1.84f, h * 0.08f)
            )

            // 2. Deep Navy Open Book Cover Cradle (mirrors the 3rd Quizzy app icon)
            val coverPath = Path().apply {
                moveTo(cx - bookHalfW - w * 0.055f, spineTopY + h * 0.06f)
                lineTo(cx - bookHalfW - w * 0.055f, spineY - h * 0.02f)
                quadraticTo(
                    cx - bookHalfW * 0.45f, spineY - h * 0.03f,
                    cx, spineY + h * 0.055f
                )
                quadraticTo(
                    cx + bookHalfW * 0.45f, spineY - h * 0.03f,
                    cx + bookHalfW + w * 0.055f, spineY - h * 0.02f
                )
                lineTo(cx + bookHalfW + w * 0.055f, spineTopY + h * 0.06f)
            }
            drawPath(
                path = coverPath,
                color = coverColor,
                style = Stroke(width = w * 0.072f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // 3. Static Turquoise Left & Right Base Pages
            val leftPage = Path().apply {
                moveTo(cx - w * 0.015f, spineY)
                quadraticTo(cx - bookHalfW * 0.52f, spineY - h * 0.06f, cx - bookHalfW, spineY - h * 0.035f)
                lineTo(cx - bookHalfW, spineTopY)
                quadraticTo(cx - bookHalfW * 0.50f, spineTopY - h * 0.065f, cx - w * 0.015f, spineTopY + h * 0.035f)
                close()
            }
            val rightPage = Path().apply {
                moveTo(cx + w * 0.015f, spineY)
                quadraticTo(cx + bookHalfW * 0.52f, spineY - h * 0.06f, cx + bookHalfW, spineY - h * 0.035f)
                lineTo(cx + bookHalfW, spineTopY)
                quadraticTo(cx + bookHalfW * 0.50f, spineTopY - h * 0.065f, cx + w * 0.015f, spineTopY + h * 0.035f)
                close()
            }
            drawPath(leftPage, color = BrandPageTurquoise)
            drawPath(rightPage, color = BrandPageTurquoise)

            // 4. Staggered Animated Flipping Pages sweeping right -> left with 3D perspective lift
            fun drawFlippingPage(progress: Float, fillColor: Color, strokeColor: Color) {
                val angle = progress * PI.toFloat() // 0 (right) -> PI (left)
                val cosA = cos(angle)               // +1 -> -1
                val sinA = sin(angle)               // 0 -> 1 -> 0 (arch height)

                val tipX = cx + bookHalfW * cosA
                val archLift = sinA * pageH * 0.62f
                val bottomControlX = cx + bookHalfW * 0.52f * cosA
                val topControlX = cx + bookHalfW * 0.52f * cosA

                val flipPath = Path().apply {
                    moveTo(cx, spineY - h * 0.01f)
                    quadraticTo(
                        bottomControlX,
                        spineY - h * 0.05f - archLift * 0.45f,
                        tipX,
                        spineY - h * 0.04f - archLift * 0.68f
                    )
                    lineTo(tipX, spineTopY - archLift)
                    quadraticTo(
                        topControlX,
                        spineTopY - h * 0.06f - archLift * 0.72f,
                        cx,
                        spineTopY + h * 0.025f
                    )
                    close()
                }
                val pageAlpha = (0.35f + 0.65f * sinA).coerceIn(0.35f, 1f)
                drawPath(flipPath, color = fillColor.copy(alpha = pageAlpha))
                drawPath(
                    flipPath,
                    color = strokeColor.copy(alpha = pageAlpha * 0.85f),
                    style = Stroke(width = w * 0.022f, join = StrokeJoin.Round)
                )
            }

            // Draw whichever page is further along first so depth ordering feels natural
            if (page1Progress < page2Progress) {
                drawFlippingPage(page2Progress, BrandPageTurquoiseSoft, coverColor.copy(alpha = 0.35f))
                drawFlippingPage(page1Progress, pageSheetColor, BrandPageTurquoise)
            } else {
                drawFlippingPage(page1Progress, pageSheetColor, BrandPageTurquoise)
                drawFlippingPage(page2Progress, BrandPageTurquoiseSoft, coverColor.copy(alpha = 0.35f))
            }

            // 5. Floating Coral Star + 3 Sparkle Rays above the book spine (echoing the app icon!)
            val starCy = h * 0.31f - starPulse * h * 0.055f
            val starOuterR = w * (0.125f + starPulse * 0.015f)
            val starInnerR = starOuterR * 0.58f

            val vertices = List(10) { i ->
                val rad = (-90.0 + i * 36.0) * PI / 180.0
                val r = if (i % 2 == 0) starOuterR else starInnerR
                Offset(cx + (r * cos(rad)).toFloat(), starCy + (r * sin(rad)).toFloat())
            }
            val midpoints = vertices.indices.map { i ->
                val a = vertices[i]
                val b = vertices[(i + 1) % vertices.size]
                Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f)
            }
            val starPath = Path().apply {
                moveTo(midpoints.last().x, midpoints.last().y)
                for (i in vertices.indices) {
                    quadraticTo(vertices[i].x, vertices[i].y, midpoints[i].x, midpoints[i].y)
                }
                close()
            }
            drawPath(starPath, color = BrandStarCoral)

            // 3 Coral Sparkle Rays radiating above the star
            val rayAlpha = 0.45f + 0.55f * starPulse
            val rayStroke = w * 0.034f
            val rayAngles = listOf(-128.0, -90.0, -52.0)
            rayAngles.forEach { deg ->
                val rad = deg * PI / 180.0
                val rStart = starOuterR * 1.28f
                val rEnd = starOuterR * (1.72f + 0.18f * starPulse)
                drawLine(
                    color = BrandStarCoral.copy(alpha = rayAlpha),
                    start = Offset(cx + (rStart * cos(rad)).toFloat(), starCy + (rStart * sin(rad)).toFloat()),
                    end = Offset(cx + (rEnd * cos(rad)).toFloat(), starCy + (rEnd * sin(rad)).toFloat()),
                    strokeWidth = rayStroke,
                    cap = StrokeCap.Round
                )
            }
        }

        if (!message.isNullOrBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Compact animated flipping-book loader for buttons, chat indicators, and inline actions.
 */
@Composable
fun QuizzyInlineLoader(
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    tint: Color = Color.White
) {
    val transition = rememberInfiniteTransition(label = "quizzy_inline_loader")
    val flip by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "inline_flip"
    )

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w * 0.5f
        val baseY = h * 0.78f
        val topY = h * 0.28f
        val halfW = w * 0.40f
        val stroke = Stroke(width = (w * 0.10f).coerceAtLeast(1.8.dp.toPx()), cap = StrokeCap.Round, join = StrokeJoin.Round)

        // Open book base V
        val base = Path().apply {
            moveTo(cx - halfW, topY + h * 0.06f)
            lineTo(cx - halfW, baseY - h * 0.06f)
            quadraticTo(cx - halfW * 0.45f, baseY - h * 0.08f, cx, baseY)
            quadraticTo(cx + halfW * 0.45f, baseY - h * 0.08f, cx + halfW, baseY - h * 0.06f)
            lineTo(cx + halfW, topY + h * 0.06f)
        }
        drawPath(base, color = tint.copy(alpha = 0.85f), style = stroke)

        // Flipping page arc sweeping from right to left
        val angle = flip * PI.toFloat()
        val tipX = cx + halfW * 0.88f * cos(angle)
        val tipY = topY - sin(angle) * h * 0.18f
        drawLine(
            color = tint,
            start = Offset(cx, baseY - h * 0.06f),
            end = Offset(tipX, tipY),
            strokeWidth = (w * 0.11f).coerceAtLeast(1.8.dp.toPx()),
            cap = StrokeCap.Round
        )
    }
}
