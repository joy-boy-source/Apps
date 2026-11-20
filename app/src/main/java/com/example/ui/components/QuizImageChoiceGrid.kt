package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.QuizAnswerEvaluator
import com.example.model.QuizImageOption
import com.example.ui.theme.QuizzyTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val NavyStroke = Color(0xFF102247)
private val SoftTurquoise = Color(0xFF14B8A6)
private val SoftSky = Color(0xFF38BDF8)
private val WarmCoral = Color(0xFFFF7A59)
private val SunnyYellow = Color(0xFFFBBF24)
private val LeafGreen = Color(0xFF22C55E)
private val WarmWood = Color(0xFFC2824E)
private val SoftCream = Color(0xFFFFFBF2)

internal fun resolveVisualOptionKey(visualKey: String, label: String): String {
    val combined = QuizAnswerEvaluator.stripAccents("${visualKey.trim()} ${label.trim()}".lowercase())
    return when {
        combined.contains("fraction_three_quarters") || combined.contains("3/4") || combined.contains("trois quart") -> "fraction_three_quarters"
        combined.contains("fraction_two_thirds") || combined.contains("2/3") || combined.contains("deux tier") -> "fraction_two_thirds"
        combined.contains("fraction_quarter") || combined.contains("1/4") || combined.contains("un quart") -> "fraction_quarter"
        combined.contains("fraction_third") || combined.contains("1/3") || combined.contains("un tier") -> "fraction_third"
        combined.contains("fraction_half") || combined.contains("1/2") || combined.contains("moitie") || combined.contains("demi") -> "fraction_half"
        combined.contains("right_angle") || combined.contains("angle droit") || combined.contains("90") -> "right_angle"
        combined.contains("triangle") -> "triangle"
        combined.contains("square") || combined.contains("carre") -> "square"
        combined.contains("rectangle") -> "rectangle"
        combined.contains("circle") || combined.contains("cercle") || combined.contains("rond") -> "circle"
        combined.contains("clock_3") || combined.contains("3h") || combined.contains("15h") -> "clock_3"
        combined.contains("clock_6") || combined.contains("6h") || combined.contains("18h") -> "clock_6"
        combined.contains("clock_9") || combined.contains("9h") || combined.contains("21h") -> "clock_9"
        combined.contains("sofa") || combined.contains("couch") || combined.contains("canape") -> "sofa"
        combined.contains("chair") || combined.contains("chaise") -> "chair"
        combined.contains("bed") || combined.contains("lit") -> "bed"
        combined.contains("table") -> "table"
        combined.contains("house") || combined.contains("maison") || combined.contains("home") -> "house"
        combined.contains("cat") || combined.contains("chat") -> "cat"
        combined.contains("dog") || combined.contains("chien") -> "dog"
        combined.contains("bus") -> "bus"
        combined.contains("apple") || combined.contains("pomme") -> "apple"
        combined.contains("sun") || combined.contains("soleil") -> "sun"
        combined.contains("rain") || combined.contains("pluie") -> "rain"
        combined.contains("book") || combined.contains("livre") || combined.contains("cahier") -> "book"
        combined.contains("pencil") || combined.contains("crayon") || combined.contains("stylo") -> "pencil"
        combined.contains("plant") || combined.contains("plante") || combined.contains("fleur") || combined.contains("arbre") -> "plant"
        combined.contains("ice") || combined.contains("glac") || combined.contains("solide") -> "ice_cube"
        combined.contains("steam") || combined.contains("vapeur") || combined.contains("gaz") || combined.contains("nuage") -> "cloud_steam"
        combined.contains("water") || combined.contains("eau") || combined.contains("goutte") || combined.contains("liquide") -> "water_drop"
        combined.contains("planet") || combined.contains("saturn") || combined.contains("planete") || combined.contains("espace") -> "planet_saturn"
        combined.contains("heart") || combined.contains("coeur") -> "heart"
        combined.contains("volcano") || combined.contains("volcan") -> "volcano"
        combined.contains("magnet") || combined.contains("aimant") -> "magnet"
        combined.contains("castle") || combined.contains("chateau") -> "castle"
        combined.contains("pyramid") || combined.contains("pyramide") -> "pyramid"
        combined.contains("compass") || combined.contains("boussole") -> "compass"
        combined.contains("globe") || combined.contains("terre") || combined.contains("monde") -> "globe"
        combined.contains("mountain") || combined.contains("montagne") -> "mountain"
        combined.contains("crown") || combined.contains("couronne") || combined.contains("roi") -> "crown"
        combined.contains("hourglass") || combined.contains("sablier") || combined.contains("temps") -> "hourglass"
        else -> "custom"
    }
}

/**
 * Interactive 2x2 grid of clickable illustrated cards so the child taps directly on the right image!
 */
@Composable
fun ImageChoiceMiniGame(
    imageOptions: List<QuizImageOption>,
    selectedIndex: Int?,
    triedWrongIndices: Set<Int>,
    correctIndex: Int,
    isQuestionResolved: Boolean,
    isQuestionSolved: Boolean,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val qColors = QuizzyTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("image_choice_grid"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = qColors.pastelYellow,
            border = BorderStroke(1.dp, qColors.warningInk.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Image,
                    contentDescription = null,
                    tint = qColors.coralInk,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Observe bien chaque illustration et clique sur la bonne image !",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = qColors.headingInk
                )
            }
        }

        val rows = imageOptions.chunked(2)
        rows.forEachIndexed { rowIdx, rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEachIndexed { colIdx, option ->
                    val optionIndex = rowIdx * 2 + colIdx
                    val isSelected = selectedIndex == optionIndex
                    val wasTriedWrong = optionIndex in triedWrongIndices
                    val showAsCorrect = isQuestionSolved && optionIndex == correctIndex
                    val resolvedKey = remember(option.visualKey, option.label) {
                        resolveVisualOptionKey(option.visualKey, option.label)
                    }

                    val cardScale by animateFloatAsState(
                        targetValue = when {
                            isSelected || showAsCorrect -> 1.03f
                            else -> 1f
                        },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "img_opt_scale_$optionIndex"
                    )
                    val borderColor by animateColorAsState(
                        targetValue = when {
                            showAsCorrect -> qColors.successInk
                            wasTriedWrong -> qColors.coralInk
                            isSelected -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.outlineVariant
                        },
                        label = "img_opt_border_$optionIndex"
                    )
                    val cardBg = when {
                        showAsCorrect -> qColors.pastelMint
                        wasTriedWrong -> qColors.pastelPeach
                        isSelected -> qColors.pastelMint
                        else -> MaterialTheme.colorScheme.surface
                    }

                    Surface(
                        onClick = { onSelect(optionIndex) },
                        enabled = !isQuestionResolved && !wasTriedWrong,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .heightIn(min = 138.dp)
                            .scale(cardScale)
                            .semantics {
                                contentDescription = option.label.ifBlank { "Image ${optionIndex + 1}" }
                            }
                            .testTag("quiz_image_option_$optionIndex"),
                        shape = RoundedCornerShape(22.dp),
                        color = cardBg,
                        border = BorderStroke(
                            width = if (isSelected || showAsCorrect || wasTriedWrong) 2.5.dp else 1.dp,
                            color = borderColor
                        ),
                        shadowElevation = 0.dp
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                if (resolvedKey != "custom") {
                                    QuizVisualOptionCanvas(
                                        visualKey = resolvedKey,
                                        outlineColor = qColors.drawingStroke,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                            .padding(4.dp)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(qColors.pastelYellow),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.AutoAwesome,
                                            contentDescription = null,
                                            tint = qColors.headingInk,
                                            modifier = Modifier.size(34.dp)
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = option.label,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = qColors.bodyInk,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Top-start index pill
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp)
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            showAsCorrect -> qColors.successInk
                                            wasTriedWrong -> qColors.coralInk
                                            isSelected -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${optionIndex + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = when {
                                        showAsCorrect || wasTriedWrong -> if (qColors.isDark) Color(0xFF0E1726) else Color.White
                                        isSelected -> MaterialTheme.colorScheme.onPrimary
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }

                            // Status icon in top-end corner
                            if (showAsCorrect) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Bonne image",
                                    tint = qColors.successInk,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .size(24.dp)
                                )
                            } else if (wasTriedWrong) {
                                Icon(
                                    imageVector = Icons.Filled.Cancel,
                                    contentDescription = "Image déjà essayée",
                                    tint = qColors.coralInk,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .size(24.dp)
                                )
                            }
                        }
                    }
                }
                if (rowItems.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun QuizVisualOptionCanvas(
    visualKey: String,
    outlineColor: Color = NavyStroke,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val s = minOf(w, h)
        val cx = w / 2f
        val cy = h / 2f
        val stroke = Stroke(width = (s * 0.045f).coerceAtLeast(2.5f), cap = StrokeCap.Round)
        val outerStroke = outlineColor

        when (visualKey) {
            "fraction_half" -> drawFractionPie(cx, cy, s * 0.4f, 1, 2, stroke)
            "fraction_quarter" -> drawFractionPie(cx, cy, s * 0.4f, 1, 4, stroke)
            "fraction_three_quarters" -> drawFractionPie(cx, cy, s * 0.4f, 3, 4, stroke)
            "fraction_third" -> drawFractionPie(cx, cy, s * 0.4f, 1, 3, stroke)
            "fraction_two_thirds" -> drawFractionPie(cx, cy, s * 0.4f, 2, 3, stroke)
            "triangle" -> {
                val path = Path().apply {
                    moveTo(cx, cy - s * 0.36f)
                    lineTo(cx + s * 0.38f, cy + s * 0.32f)
                    lineTo(cx - s * 0.38f, cy + s * 0.32f)
                    close()
                }
                drawPath(path, color = WarmCoral)
                drawPath(path, color = NavyStroke, style = stroke)
            }
            "square" -> {
                val side = s * 0.64f
                drawRoundRect(SoftTurquoise, Offset(cx - side / 2, cy - side / 2), Size(side, side), CornerRadius(8f))
                drawRoundRect(NavyStroke, Offset(cx - side / 2, cy - side / 2), Size(side, side), CornerRadius(8f), style = stroke)
            }
            "rectangle" -> {
                val rw = s * 0.78f
                val rh = s * 0.46f
                drawRoundRect(SunnyYellow, Offset(cx - rw / 2, cy - rh / 2), Size(rw, rh), CornerRadius(8f))
                drawRoundRect(NavyStroke, Offset(cx - rw / 2, cy - rh / 2), Size(rw, rh), CornerRadius(8f), style = stroke)
            }
            "circle" -> {
                drawCircle(SoftSky, radius = s * 0.36f, center = Offset(cx, cy))
                drawCircle(NavyStroke, radius = s * 0.36f, center = Offset(cx, cy), style = stroke)
            }
            "right_angle" -> {
                val origin = Offset(cx - s * 0.28f, cy + s * 0.28f)
                val topPt = Offset(origin.x, cy - s * 0.3f)
                val rightPt = Offset(cx + s * 0.32f, origin.y)
                val sq = s * 0.16f
                drawRect(WarmCoral.copy(alpha = 0.7f), Offset(origin.x, origin.y - sq), Size(sq, sq))
                drawRect(outerStroke, Offset(origin.x, origin.y - sq), Size(sq, sq), style = stroke)
                drawLine(outerStroke, origin, topPt, strokeWidth = stroke.width * 1.3f, cap = StrokeCap.Round)
                drawLine(outerStroke, origin, rightPt, strokeWidth = stroke.width * 1.3f, cap = StrokeCap.Round)
            }
            "clock_3" -> drawClock(cx, cy, s * 0.38f, hour = 3, stroke = stroke)
            "clock_6" -> drawClock(cx, cy, s * 0.38f, hour = 6, stroke = stroke)
            "clock_9" -> drawClock(cx, cy, s * 0.38f, hour = 9, stroke = stroke)
            "sofa" -> {
                // Cozy coral sofa with backrest, seat cushion, armrests & legs
                drawRoundRect(WarmCoral, Offset(cx - s * 0.36f, cy - s * 0.22f), Size(s * 0.72f, s * 0.3f), CornerRadius(14f))
                drawRoundRect(outerStroke, Offset(cx - s * 0.36f, cy - s * 0.22f), Size(s * 0.72f, s * 0.3f), CornerRadius(14f), style = stroke)
                drawRoundRect(SunnyYellow, Offset(cx - s * 0.32f, cy - s * 0.02f), Size(s * 0.64f, s * 0.22f), CornerRadius(10f))
                drawRoundRect(outerStroke, Offset(cx - s * 0.32f, cy - s * 0.02f), Size(s * 0.64f, s * 0.22f), CornerRadius(10f), style = stroke)
                // Armrests
                drawRoundRect(WarmCoral, Offset(cx - s * 0.43f, cy - s * 0.08f), Size(s * 0.15f, s * 0.28f), CornerRadius(10f))
                drawRoundRect(outerStroke, Offset(cx - s * 0.43f, cy - s * 0.08f), Size(s * 0.15f, s * 0.28f), CornerRadius(10f), style = stroke)
                drawRoundRect(WarmCoral, Offset(cx + s * 0.28f, cy - s * 0.08f), Size(s * 0.15f, s * 0.28f), CornerRadius(10f))
                drawRoundRect(outerStroke, Offset(cx + s * 0.28f, cy - s * 0.08f), Size(s * 0.15f, s * 0.28f), CornerRadius(10f), style = stroke)
                // Legs
                drawLine(WarmWood, Offset(cx - s * 0.34f, cy + s * 0.2f), Offset(cx - s * 0.34f, cy + s * 0.32f), stroke.width * 1.4f, StrokeCap.Round)
                drawLine(WarmWood, Offset(cx + s * 0.34f, cy + s * 0.2f), Offset(cx + s * 0.34f, cy + s * 0.32f), stroke.width * 1.4f, StrokeCap.Round)
            }
            "chair" -> {
                drawRoundRect(SoftTurquoise, Offset(cx - s * 0.2f, cy - s * 0.34f), Size(s * 0.4f, s * 0.32f), CornerRadius(10f))
                drawRoundRect(outerStroke, Offset(cx - s * 0.2f, cy - s * 0.34f), Size(s * 0.4f, s * 0.32f), CornerRadius(10f), style = stroke)
                drawRoundRect(WarmWood, Offset(cx - s * 0.24f, cy - s * 0.02f), Size(s * 0.48f, s * 0.12f), CornerRadius(6f))
                drawRoundRect(outerStroke, Offset(cx - s * 0.24f, cy - s * 0.02f), Size(s * 0.48f, s * 0.12f), CornerRadius(6f), style = stroke)
                drawLine(outerStroke, Offset(cx - s * 0.18f, cy + s * 0.1f), Offset(cx - s * 0.18f, cy + s * 0.36f), stroke.width * 1.2f, StrokeCap.Round)
                drawLine(outerStroke, Offset(cx + s * 0.18f, cy + s * 0.1f), Offset(cx + s * 0.18f, cy + s * 0.36f), stroke.width * 1.2f, StrokeCap.Round)
            }
            "bed" -> {
                drawRoundRect(WarmWood, Offset(cx - s * 0.42f, cy - s * 0.16f), Size(s * 0.12f, s * 0.46f), CornerRadius(6f))
                drawRoundRect(SoftSky, Offset(cx - s * 0.3f, cy - s * 0.02f), Size(s * 0.7f, s * 0.22f), CornerRadius(10f))
                drawRoundRect(SoftCream, Offset(cx - s * 0.28f, cy - s * 0.12f), Size(s * 0.22f, s * 0.12f), CornerRadius(8f))
                drawRoundRect(NavyStroke, Offset(cx - s * 0.3f, cy - s * 0.02f), Size(s * 0.7f, s * 0.22f), CornerRadius(10f), style = stroke)
                drawLine(NavyStroke, Offset(cx + s * 0.36f, cy + s * 0.18f), Offset(cx + s * 0.36f, cy + s * 0.32f), stroke.width * 1.3f, StrokeCap.Round)
            }
            "table" -> {
                drawRoundRect(WarmWood, Offset(cx - s * 0.42f, cy - s * 0.12f), Size(s * 0.84f, s * 0.14f), CornerRadius(8f))
                drawRoundRect(NavyStroke, Offset(cx - s * 0.42f, cy - s * 0.12f), Size(s * 0.84f, s * 0.14f), CornerRadius(8f), style = stroke)
                drawLine(WarmWood, Offset(cx - s * 0.32f, cy + s * 0.02f), Offset(cx - s * 0.32f, cy + s * 0.34f), stroke.width * 1.5f, StrokeCap.Round)
                drawLine(WarmWood, Offset(cx + s * 0.32f, cy + s * 0.02f), Offset(cx + s * 0.32f, cy + s * 0.34f), stroke.width * 1.5f, StrokeCap.Round)
            }
            "house" -> {
                drawRoundRect(SunnyYellow.copy(alpha = 0.5f), Offset(cx - s * 0.28f, cy - s * 0.04f), Size(s * 0.56f, s * 0.38f), CornerRadius(6f))
                drawRoundRect(NavyStroke, Offset(cx - s * 0.28f, cy - s * 0.04f), Size(s * 0.56f, s * 0.38f), CornerRadius(6f), style = stroke)
                val roof = Path().apply {
                    moveTo(cx, cy - s * 0.36f)
                    lineTo(cx + s * 0.36f, cy - s * 0.02f)
                    lineTo(cx - s * 0.36f, cy - s * 0.02f)
                    close()
                }
                drawPath(roof, WarmCoral)
                drawPath(roof, NavyStroke, style = stroke)
                drawRoundRect(SoftTurquoise, Offset(cx - s * 0.08f, cy + s * 0.12f), Size(s * 0.16f, s * 0.22f), CornerRadius(4f))
            }
            "cat" -> {
                // Cute orange cat face with pointy ears & whiskers
                val leftEar = Path().apply {
                    moveTo(cx - s * 0.28f, cy - s * 0.1f)
                    lineTo(cx - s * 0.24f, cy - s * 0.36f)
                    lineTo(cx - s * 0.06f, cy - s * 0.22f)
                    close()
                }
                val rightEar = Path().apply {
                    moveTo(cx + s * 0.28f, cy - s * 0.1f)
                    lineTo(cx + s * 0.24f, cy - s * 0.36f)
                    lineTo(cx + s * 0.06f, cy - s * 0.22f)
                    close()
                }
                drawPath(leftEar, WarmCoral)
                drawPath(rightEar, WarmCoral)
                drawPath(leftEar, NavyStroke, style = stroke)
                drawPath(rightEar, NavyStroke, style = stroke)
                drawCircle(SunnyYellow, s * 0.28f, Offset(cx, cy + s * 0.04f))
                drawCircle(NavyStroke, s * 0.28f, Offset(cx, cy + s * 0.04f), style = stroke)
                drawCircle(NavyStroke, s * 0.035f, Offset(cx - s * 0.1f, cy))
                drawCircle(NavyStroke, s * 0.035f, Offset(cx + s * 0.1f, cy))
                drawCircle(WarmCoral, s * 0.038f, Offset(cx, cy + s * 0.08f))
            }
            "dog" -> {
                drawOval(WarmWood, Offset(cx - s * 0.38f, cy - s * 0.18f), Size(s * 0.18f, s * 0.38f))
                drawOval(WarmWood, Offset(cx + s * 0.20f, cy - s * 0.18f), Size(s * 0.18f, s * 0.38f))
                drawCircle(Color(0xFFF3C58B), s * 0.27f, Offset(cx, cy + s * 0.02f))
                drawCircle(NavyStroke, s * 0.27f, Offset(cx, cy + s * 0.02f), style = stroke)
                drawCircle(NavyStroke, s * 0.035f, Offset(cx - s * 0.09f, cy - s * 0.03f))
                drawCircle(NavyStroke, s * 0.035f, Offset(cx + s * 0.09f, cy - s * 0.03f))
                drawOval(NavyStroke, Offset(cx - s * 0.06f, cy + s * 0.04f), Size(s * 0.12f, s * 0.08f))
            }
            "bus" -> {
                drawRoundRect(WarmCoral, Offset(cx - s * 0.38f, cy - s * 0.26f), Size(s * 0.76f, s * 0.44f), CornerRadius(12f))
                drawRoundRect(NavyStroke, Offset(cx - s * 0.38f, cy - s * 0.26f), Size(s * 0.76f, s * 0.44f), CornerRadius(12f), style = stroke)
                drawRoundRect(SoftSky, Offset(cx - s * 0.3f, cy - s * 0.18f), Size(s * 0.24f, s * 0.16f), CornerRadius(4f))
                drawRoundRect(SoftSky, Offset(cx + s * 0.04f, cy - s * 0.18f), Size(s * 0.24f, s * 0.16f), CornerRadius(4f))
                drawCircle(NavyStroke, s * 0.09f, Offset(cx - s * 0.2f, cy + s * 0.22f))
                drawCircle(NavyStroke, s * 0.09f, Offset(cx + s * 0.2f, cy + s * 0.22f))
            }
            "apple" -> {
                drawCircle(WarmCoral, s * 0.28f, Offset(cx, cy + s * 0.06f))
                drawCircle(NavyStroke, s * 0.28f, Offset(cx, cy + s * 0.06f), style = stroke)
                drawLine(WarmWood, Offset(cx, cy - s * 0.22f), Offset(cx + s * 0.04f, cy - s * 0.36f), stroke.width, StrokeCap.Round)
                drawOval(LeafGreen, Offset(cx + s * 0.03f, cy - s * 0.34f), Size(s * 0.18f, s * 0.1f))
            }
            "sun" -> {
                drawCircle(SunnyYellow, s * 0.22f, Offset(cx, cy))
                drawCircle(NavyStroke, s * 0.22f, Offset(cx, cy), style = stroke)
                for (i in 0 until 8) {
                    val angle = i * PI / 4.0
                    val x1 = cx + (s * 0.28f * cos(angle)).toFloat()
                    val y1 = cy + (s * 0.28f * sin(angle)).toFloat()
                    val x2 = cx + (s * 0.38f * cos(angle)).toFloat()
                    val y2 = cy + (s * 0.38f * sin(angle)).toFloat()
                    drawLine(WarmCoral, Offset(x1, y1), Offset(x2, y2), stroke.width, StrokeCap.Round)
                }
            }
            "rain" -> {
                drawCircle(SoftSky, s * 0.16f, Offset(cx - s * 0.14f, cy - s * 0.08f))
                drawCircle(SoftSky, s * 0.2f, Offset(cx + s * 0.02f, cy - s * 0.14f))
                drawCircle(SoftSky, s * 0.15f, Offset(cx + s * 0.18f, cy - s * 0.08f))
                drawLine(SoftTurquoise, Offset(cx - s * 0.14f, cy + s * 0.14f), Offset(cx - s * 0.18f, cy + s * 0.3f), stroke.width, StrokeCap.Round)
                drawLine(SoftTurquoise, Offset(cx, cy + s * 0.14f), Offset(cx - s * 0.04f, cy + s * 0.32f), stroke.width, StrokeCap.Round)
                drawLine(SoftTurquoise, Offset(cx + s * 0.14f, cy + s * 0.14f), Offset(cx + s * 0.1f, cy + s * 0.3f), stroke.width, StrokeCap.Round)
            }
            "book" -> {
                drawRoundRect(SoftTurquoise, Offset(cx - s * 0.38f, cy - s * 0.24f), Size(s * 0.38f, s * 0.5f), CornerRadius(8f))
                drawRoundRect(SoftCream, Offset(cx, cy - s * 0.24f), Size(s * 0.38f, s * 0.5f), CornerRadius(8f))
                drawRoundRect(NavyStroke, Offset(cx - s * 0.38f, cy - s * 0.24f), Size(s * 0.76f, s * 0.5f), CornerRadius(8f), style = stroke)
                drawLine(NavyStroke, Offset(cx, cy - s * 0.24f), Offset(cx, cy + s * 0.26f), stroke.width)
            }
            "pencil" -> {
                drawRoundRect(SunnyYellow, Offset(cx - s * 0.1f, cy - s * 0.3f), Size(s * 0.2f, s * 0.48f), CornerRadius(4f))
                drawRoundRect(NavyStroke, Offset(cx - s * 0.1f, cy - s * 0.3f), Size(s * 0.2f, s * 0.48f), CornerRadius(4f), style = stroke)
                val tip = Path().apply {
                    moveTo(cx - s * 0.1f, cy + s * 0.18f)
                    lineTo(cx + s * 0.1f, cy + s * 0.18f)
                    lineTo(cx, cy + s * 0.38f)
                    close()
                }
                drawPath(tip, WarmCoral)
                drawPath(tip, NavyStroke, style = stroke)
            }
            "plant" -> {
                drawRoundRect(WarmCoral, Offset(cx - s * 0.16f, cy + s * 0.1f), Size(s * 0.32f, s * 0.26f), CornerRadius(6f))
                drawLine(LeafGreen, Offset(cx, cy + s * 0.1f), Offset(cx, cy - s * 0.26f), stroke.width * 1.4f, StrokeCap.Round)
                drawOval(LeafGreen, Offset(cx - s * 0.26f, cy - s * 0.22f), Size(s * 0.26f, s * 0.14f))
                drawOval(LeafGreen, Offset(cx, cy - s * 0.3f), Size(s * 0.26f, s * 0.14f))
            }
            "water_drop" -> {
                val drop = Path().apply {
                    moveTo(cx, cy - s * 0.36f)
                    cubicTo(cx + s * 0.34f, cy + s * 0.04f, cx + s * 0.28f, cy + s * 0.36f, cx, cy + s * 0.36f)
                    cubicTo(cx - s * 0.28f, cy + s * 0.36f, cx - s * 0.34f, cy + s * 0.04f, cx, cy - s * 0.36f)
                    close()
                }
                drawPath(drop, SoftSky)
                drawPath(drop, NavyStroke, style = stroke)
            }
            "ice_cube" -> {
                val side = s * 0.56f
                drawRoundRect(Color(0xFFBAE6FD), Offset(cx - side / 2, cy - side / 2), Size(side, side), CornerRadius(14f))
                drawRoundRect(SoftTurquoise, Offset(cx - side / 2, cy - side / 2), Size(side, side), CornerRadius(14f), style = stroke)
                drawLine(Color.White, Offset(cx - side * 0.28f, cy - side * 0.22f), Offset(cx - side * 0.06f, cy - side * 0.22f), stroke.width, StrokeCap.Round)
            }
            "cloud_steam" -> {
                drawCircle(Color(0xFFE2E8F0), s * 0.18f, Offset(cx - s * 0.16f, cy + s * 0.04f))
                drawCircle(Color(0xFFE2E8F0), s * 0.22f, Offset(cx, cy - s * 0.06f))
                drawCircle(Color(0xFFE2E8F0), s * 0.17f, Offset(cx + s * 0.18f, cy + s * 0.04f))
                drawLine(SoftTurquoise, Offset(cx - s * 0.1f, cy - s * 0.28f), Offset(cx - s * 0.04f, cy - s * 0.38f), stroke.width, StrokeCap.Round)
                drawLine(SoftTurquoise, Offset(cx + s * 0.1f, cy - s * 0.28f), Offset(cx + s * 0.14f, cy - s * 0.38f), stroke.width, StrokeCap.Round)
            }
            "planet_saturn" -> {
                drawCircle(SoftTurquoise, s * 0.24f, Offset(cx, cy))
                drawOval(SunnyYellow, Offset(cx - s * 0.42f, cy - s * 0.09f), Size(s * 0.84f, s * 0.18f), style = stroke)
                drawCircle(NavyStroke, s * 0.24f, Offset(cx, cy), style = stroke)
            }
            "heart" -> {
                val heart = Path().apply {
                    moveTo(cx, cy + s * 0.34f)
                    cubicTo(cx - s * 0.44f, cy + s * 0.04f, cx - s * 0.34f, cy - s * 0.36f, cx, cy - s * 0.12f)
                    cubicTo(cx + s * 0.34f, cy - s * 0.36f, cx + s * 0.44f, cy + s * 0.04f, cx, cy + s * 0.34f)
                    close()
                }
                drawPath(heart, WarmCoral)
                drawPath(heart, NavyStroke, style = stroke)
            }
            "volcano" -> {
                val mtn = Path().apply {
                    moveTo(cx - s * 0.4f, cy + s * 0.34f)
                    lineTo(cx - s * 0.12f, cy - s * 0.18f)
                    lineTo(cx + s * 0.12f, cy - s * 0.18f)
                    lineTo(cx + s * 0.4f, cy + s * 0.34f)
                    close()
                }
                drawPath(mtn, WarmWood)
                drawPath(mtn, NavyStroke, style = stroke)
                drawCircle(WarmCoral, s * 0.09f, Offset(cx, cy - s * 0.24f))
                drawCircle(SunnyYellow, s * 0.06f, Offset(cx + s * 0.12f, cy - s * 0.32f))
            }
            "magnet" -> {
                drawArc(WarmCoral, 180f, 180f, false, Offset(cx - s * 0.24f, cy - s * 0.24f), Size(s * 0.48f, s * 0.48f), style = Stroke(s * 0.12f))
                drawLine(WarmCoral, Offset(cx - s * 0.24f, cy), Offset(cx - s * 0.24f, cy + s * 0.26f), s * 0.12f)
                drawLine(SoftSky, Offset(cx + s * 0.24f, cy), Offset(cx + s * 0.24f, cy + s * 0.26f), s * 0.12f)
            }
            "castle" -> {
                drawRect(Color(0xFFE7DFD0), Offset(cx - s * 0.3f, cy - s * 0.1f), Size(s * 0.6f, s * 0.42f))
                drawRect(NavyStroke, Offset(cx - s * 0.3f, cy - s * 0.1f), Size(s * 0.6f, s * 0.42f), style = stroke)
                drawRect(Color(0xFFE7DFD0), Offset(cx - s * 0.36f, cy - s * 0.26f), Size(s * 0.18f, s * 0.58f))
                drawRect(Color(0xFFE7DFD0), Offset(cx + s * 0.18f, cy - s * 0.26f), Size(s * 0.18f, s * 0.58f))
                drawRoundRect(WarmWood, Offset(cx - s * 0.09f, cy + s * 0.1f), Size(s * 0.18f, s * 0.22f), CornerRadius(16f))
            }
            "pyramid" -> {
                val p = Path().apply {
                    moveTo(cx, cy - s * 0.34f)
                    lineTo(cx + s * 0.4f, cy + s * 0.32f)
                    lineTo(cx - s * 0.4f, cy + s * 0.32f)
                    close()
                }
                drawPath(p, SunnyYellow)
                drawPath(p, NavyStroke, style = stroke)
                drawLine(NavyStroke, Offset(cx, cy - s * 0.34f), Offset(cx + s * 0.08f, cy + s * 0.32f), stroke.width * 0.8f)
            }
            "compass" -> {
                drawCircle(SunnyYellow, s * 0.35f, Offset(cx, cy))
                drawCircle(SoftCream, s * 0.27f, Offset(cx, cy))
                drawCircle(NavyStroke, s * 0.35f, Offset(cx, cy), style = stroke)
                val needle = Path().apply {
                    moveTo(cx, cy - s * 0.22f)
                    lineTo(cx + s * 0.07f, cy)
                    lineTo(cx - s * 0.07f, cy)
                    close()
                }
                drawPath(needle, WarmCoral)
            }
            "globe" -> {
                drawCircle(SoftSky, s * 0.34f, Offset(cx, cy))
                drawOval(LeafGreen, Offset(cx - s * 0.2f, cy - s * 0.18f), Size(s * 0.22f, s * 0.28f))
                drawOval(LeafGreen, Offset(cx + s * 0.04f, cy - s * 0.04f), Size(s * 0.2f, s * 0.22f))
                drawCircle(NavyStroke, s * 0.34f, Offset(cx, cy), style = stroke)
            }
            "mountain" -> {
                val p1 = Path().apply {
                    moveTo(cx - s * 0.42f, cy + s * 0.32f)
                    lineTo(cx - s * 0.08f, cy - s * 0.3f)
                    lineTo(cx + s * 0.26f, cy + s * 0.32f)
                    close()
                }
                drawPath(p1, SoftTurquoise)
                drawPath(p1, NavyStroke, style = stroke)
            }
            "crown" -> {
                val c = Path().apply {
                    moveTo(cx - s * 0.34f, cy + s * 0.22f)
                    lineTo(cx - s * 0.34f, cy - s * 0.16f)
                    lineTo(cx - s * 0.14f, cy + s * 0.02f)
                    lineTo(cx, cy - s * 0.26f)
                    lineTo(cx + s * 0.14f, cy + s * 0.02f)
                    lineTo(cx + s * 0.34f, cy - s * 0.16f)
                    lineTo(cx + s * 0.34f, cy + s * 0.22f)
                    close()
                }
                drawPath(c, SunnyYellow)
                drawPath(c, NavyStroke, style = stroke)
            }
            "hourglass" -> {
                val topTri = Path().apply {
                    moveTo(cx - s * 0.26f, cy - s * 0.3f)
                    lineTo(cx + s * 0.26f, cy - s * 0.3f)
                    lineTo(cx, cy)
                    close()
                }
                val botTri = Path().apply {
                    moveTo(cx - s * 0.26f, cy + s * 0.3f)
                    lineTo(cx + s * 0.26f, cy + s * 0.3f)
                    lineTo(cx, cy)
                    close()
                }
                drawPath(topTri, SunnyYellow.copy(alpha = 0.6f))
                drawPath(botTri, SunnyYellow)
                drawPath(topTri, NavyStroke, style = stroke)
                drawPath(botTri, NavyStroke, style = stroke)
            }
        }
    }
}

private fun DrawScope.drawFractionPie(
    cx: Float,
    cy: Float,
    radius: Float,
    numerator: Int,
    denominator: Int,
    stroke: Stroke
) {
    val topLeft = Offset(cx - radius, cy - radius)
    val arcSize = Size(radius * 2, radius * 2)
    drawCircle(SoftCream, radius, Offset(cx, cy))
    val sweepPerSlice = 360f / denominator.coerceAtLeast(1)
    drawArc(
        color = WarmCoral,
        startAngle = -90f,
        sweepAngle = sweepPerSlice * numerator,
        useCenter = true,
        topLeft = topLeft,
        size = arcSize
    )
    drawCircle(NavyStroke, radius, Offset(cx, cy), style = stroke)
    for (i in 0 until denominator) {
        val angleRad = (-90.0 + i * sweepPerSlice) * PI / 180.0
        val endX = cx + (radius * cos(angleRad)).toFloat()
        val endY = cy + (radius * sin(angleRad)).toFloat()
        drawLine(NavyStroke, Offset(cx, cy), Offset(endX, endY), strokeWidth = stroke.width * 0.85f)
    }
}

private fun DrawScope.drawClock(
    cx: Float,
    cy: Float,
    radius: Float,
    hour: Int,
    stroke: Stroke
) {
    drawCircle(SoftCream, radius, Offset(cx, cy))
    drawCircle(NavyStroke, radius, Offset(cx, cy), style = stroke)
    // Minute hand at 12
    drawLine(SoftTurquoise, Offset(cx, cy), Offset(cx, cy - radius * 0.68f), stroke.width, StrokeCap.Round)
    // Hour hand
    val angle = (-90.0 + hour * 30.0) * PI / 180.0
    drawLine(
        WarmCoral,
        Offset(cx, cy),
        Offset(cx + (radius * 0.48f * cos(angle)).toFloat(), cy + (radius * 0.48f * sin(angle)).toFloat()),
        stroke.width * 1.3f,
        StrokeCap.Round
    )
    drawCircle(NavyStroke, radius * 0.08f, Offset(cx, cy))
}
