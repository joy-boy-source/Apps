package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
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

// Realistic studio material palette (mature editorial tones instead of flat cartoons)
private val SlateInk = Color(0xFF1E293B)
private val BrassLight = Color(0xFFF3D58D)
private val BrassMid = Color(0xFFC89B3C)
private val BrassDark = Color(0xFF7D5A1C)
private val OakLight = Color(0xFFD6A87C)
private val OakMid = Color(0xFFA66F42)
private val OakDark = Color(0xFF6B4223)
private val SlateFabricLight = Color(0xFF5B7699)
private val SlateFabricDark = Color(0xFF2E4361)
private val TerracottaLight = Color(0xFFE06D53)
private val TerracottaDark = Color(0xFF9C3824)
private val CrimsonLight = Color(0xFFDC3B3B)
private val CrimsonDark = Color(0xFF8C1D1D)
private val OceanLight = Color(0xFF38BDF8)
private val OceanMid = Color(0xFF0284C7)
private val OceanDark = Color(0xFF0C4A6E)
private val EmeraldLight = Color(0xFF4ADE80)
private val EmeraldMid = Color(0xFF15803D)
private val EmeraldDark = Color(0xFF14532D)
private val StoneLight = Color(0xFFE5DFD3)
private val StoneMid = Color(0xFFB8AEA0)
private val StoneDark = Color(0xFF7C7367)
private val SteelLight = Color(0xFFE2E8F0)
private val SteelDark = Color(0xFF64748B)
private val CreamPaper = Color(0xFFFAF7F0)
private val TealAccent = Color(0xFF0D9488)
private val AmberGlow = Color(0xFFF59E0B)

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
 * Interactive 2x2 grid of clickable, mature shaded editorial visual cards so the student
 * can immediately identify each object or scientific diagram with crystal clarity.
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
    modifier: Modifier = Modifier,
    isMultiSelect: Boolean = false,
    selectedIndices: Set<Int> = emptySet(),
    correctIndices: Set<Int> = setOf(correctIndex)
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
                    text = if (isMultiSelect) {
                        "Plusieurs images attendues : coche toutes les bonnes propositions."
                    } else {
                        "Observe attentivement chaque visuel et sélectionne la bonne image."
                    },
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
                    val isSelected = if (isMultiSelect) {
                        optionIndex in selectedIndices
                    } else {
                        selectedIndex == optionIndex
                    }
                    val wasTriedWrong = !isMultiSelect && optionIndex in triedWrongIndices
                    val showAsCorrect = isQuestionSolved && optionIndex in correctIndices
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
                            .heightIn(min = 144.dp)
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
                                    .padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                if (resolvedKey != "custom") {
                                    QuizVisualOptionCanvas(
                                        visualKey = resolvedKey,
                                        outlineColor = qColors.drawingStroke,
                                        isDark = qColors.isDark,
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
                            } else if (isMultiSelect) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                                    contentDescription = if (isSelected) "Image cochée" else "Image non cochée",
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
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
    outlineColor: Color = SlateInk,
    isDark: Boolean = false,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val s = minOf(w, h)
        val cx = w / 2f
        val cy = h / 2f
        val fineStroke = Stroke(
            width = (s * 0.026f).coerceAtLeast(1.8f),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
        val shadowColor = (if (isDark) Color.Black else Color(0xFF0F172A)).copy(alpha = if (isDark) 0.35f else 0.14f)

        // Subtle soft studio backdrop halo for depth and immediate figure-ground separation
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    (if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)).copy(alpha = 0.45f),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = s * 0.48f
            ),
            radius = s * 0.48f,
            center = Offset(cx, cy)
        )

        when (visualKey) {
            "fraction_half" -> drawFractionDiagram(cx, cy, s * 0.39f, 1, 2, outlineColor, fineStroke)
            "fraction_quarter" -> drawFractionDiagram(cx, cy, s * 0.39f, 1, 4, outlineColor, fineStroke)
            "fraction_three_quarters" -> drawFractionDiagram(cx, cy, s * 0.39f, 3, 4, outlineColor, fineStroke)
            "fraction_third" -> drawFractionDiagram(cx, cy, s * 0.39f, 1, 3, outlineColor, fineStroke)
            "fraction_two_thirds" -> drawFractionDiagram(cx, cy, s * 0.39f, 2, 3, outlineColor, fineStroke)

            "triangle" -> {
                drawStudioShadow(cx, cy + s * 0.36f, s * 0.68f, s * 0.08f, shadowColor)
                val path = Path().apply {
                    moveTo(cx, cy - s * 0.36f)
                    lineTo(cx + s * 0.38f, cy + s * 0.32f)
                    lineTo(cx - s * 0.38f, cy + s * 0.32f)
                    close()
                }
                drawPath(
                    path = path,
                    brush = Brush.linearGradient(
                        listOf(TerracottaLight, TerracottaDark),
                        start = Offset(cx - s * 0.2f, cy - s * 0.35f),
                        end = Offset(cx + s * 0.35f, cy + s * 0.32f)
                    )
                )
                drawPath(path, color = outlineColor, style = fineStroke)
                // Technical vertex points
                listOf(
                    Offset(cx, cy - s * 0.36f),
                    Offset(cx + s * 0.38f, cy + s * 0.32f),
                    Offset(cx - s * 0.38f, cy + s * 0.32f)
                ).forEach { pt ->
                    drawCircle(Color.White, s * 0.03f, pt)
                    drawCircle(outlineColor, s * 0.03f, pt, style = Stroke(s * 0.015f))
                }
            }

            "square" -> {
                val side = s * 0.62f
                drawStudioShadow(cx, cy + side / 2 + s * 0.05f, side * 0.94f, s * 0.08f, shadowColor)
                drawRoundRect(
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF2DD4BF), TealAccent, Color(0xFF115E59)),
                        start = Offset(cx - side / 2, cy - side / 2),
                        end = Offset(cx + side / 2, cy + side / 2)
                    ),
                    topLeft = Offset(cx - side / 2, cy - side / 2),
                    size = Size(side, side),
                    cornerRadius = CornerRadius(6f)
                )
                drawRoundRect(
                    color = outlineColor,
                    topLeft = Offset(cx - side / 2, cy - side / 2),
                    size = Size(side, side),
                    cornerRadius = CornerRadius(6f),
                    style = fineStroke
                )
                // Equal-side tick marks for geometric clarity
                val tick = s * 0.05f
                drawLine(Color.White, Offset(cx, cy - side / 2 - tick), Offset(cx, cy - side / 2 + tick), s * 0.022f, StrokeCap.Round)
                drawLine(Color.White, Offset(cx, cy + side / 2 - tick), Offset(cx, cy + side / 2 + tick), s * 0.022f, StrokeCap.Round)
                drawLine(Color.White, Offset(cx - side / 2 - tick, cy), Offset(cx - side / 2 + tick, cy), s * 0.022f, StrokeCap.Round)
                drawLine(Color.White, Offset(cx + side / 2 - tick, cy), Offset(cx + side / 2 + tick, cy), s * 0.022f, StrokeCap.Round)
            }

            "rectangle" -> {
                val rw = s * 0.78f
                val rh = s * 0.44f
                drawStudioShadow(cx, cy + rh / 2 + s * 0.06f, rw * 0.94f, s * 0.08f, shadowColor)
                drawRoundRect(
                    brush = Brush.linearGradient(
                        listOf(BrassLight, BrassMid, BrassDark),
                        start = Offset(cx - rw / 2, cy - rh / 2),
                        end = Offset(cx + rw / 2, cy + rh / 2)
                    ),
                    topLeft = Offset(cx - rw / 2, cy - rh / 2),
                    size = Size(rw, rh),
                    cornerRadius = CornerRadius(6f)
                )
                drawRoundRect(
                    color = outlineColor,
                    topLeft = Offset(cx - rw / 2, cy - rh / 2),
                    size = Size(rw, rh),
                    cornerRadius = CornerRadius(6f),
                    style = fineStroke
                )
            }

            "circle" -> {
                val rad = s * 0.36f
                drawStudioShadow(cx, cy + rad + s * 0.05f, rad * 1.6f, s * 0.08f, shadowColor)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFBAE6FD), OceanLight, OceanMid, OceanDark),
                        center = Offset(cx - rad * 0.3f, cy - rad * 0.3f),
                        radius = rad * 1.4f
                    ),
                    radius = rad,
                    center = Offset(cx, cy)
                )
                drawCircle(outlineColor, rad, Offset(cx, cy), style = fineStroke)
                // Subtle center & radius line for geometric precision
                drawLine(
                    color = Color.White.copy(alpha = 0.75f),
                    start = Offset(cx, cy),
                    end = Offset(cx + rad * 0.95f, cy),
                    strokeWidth = s * 0.02f,
                    cap = StrokeCap.Round
                )
                drawCircle(Color.White, s * 0.03f, Offset(cx, cy))
            }

            "right_angle" -> {
                val origin = Offset(cx - s * 0.28f, cy + s * 0.28f)
                val topPt = Offset(origin.x, cy - s * 0.32f)
                val rightPt = Offset(cx + s * 0.34f, origin.y)
                val sq = s * 0.17f
                // Subtle drafting grid
                for (g in 1..3) {
                    val step = s * 0.14f * g
                    drawLine(
                        color = outlineColor.copy(alpha = 0.10f),
                        start = Offset(origin.x, origin.y - step),
                        end = Offset(rightPt.x, origin.y - step),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = outlineColor.copy(alpha = 0.10f),
                        start = Offset(origin.x + step, origin.y),
                        end = Offset(origin.x + step, topPt.y),
                        strokeWidth = 1f
                    )
                }
                drawRect(
                    brush = Brush.linearGradient(listOf(TerracottaLight, TerracottaDark)),
                    topLeft = Offset(origin.x, origin.y - sq),
                    size = Size(sq, sq)
                )
                drawRect(outlineColor, Offset(origin.x, origin.y - sq), Size(sq, sq), style = fineStroke)
                drawLine(outlineColor, origin, topPt, strokeWidth = s * 0.048f, cap = StrokeCap.Round)
                drawLine(outlineColor, origin, rightPt, strokeWidth = s * 0.048f, cap = StrokeCap.Round)
                drawCircle(TerracottaLight, s * 0.04f, origin)
                drawCircle(outlineColor, s * 0.04f, origin, style = fineStroke)
            }

            "clock_3" -> drawRealisticClock(cx, cy, s * 0.39f, hour = 3, outlineColor = outlineColor)
            "clock_6" -> drawRealisticClock(cx, cy, s * 0.39f, hour = 6, outlineColor = outlineColor)
            "clock_9" -> drawRealisticClock(cx, cy, s * 0.39f, hour = 9, outlineColor = outlineColor)

            "sofa" -> {
                // Realistic architectural mid-century upholstered sofa with depth cushions & oak legs
                drawStudioShadow(cx, cy + s * 0.33f, s * 0.84f, s * 0.09f, shadowColor)
                // Tapered oak legs
                drawLine(OakDark, Offset(cx - s * 0.34f, cy + s * 0.18f), Offset(cx - s * 0.37f, cy + s * 0.32f), s * 0.048f, StrokeCap.Round)
                drawLine(OakDark, Offset(cx + s * 0.34f, cy + s * 0.18f), Offset(cx + s * 0.37f, cy + s * 0.32f), s * 0.048f, StrokeCap.Round)
                // Backrest cushions (two distinct tufted cushions)
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(SlateFabricLight, SlateFabricDark)),
                    topLeft = Offset(cx - s * 0.36f, cy - s * 0.24f),
                    size = Size(s * 0.35f, s * 0.30f),
                    cornerRadius = CornerRadius(s * 0.08f)
                )
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(SlateFabricLight, SlateFabricDark)),
                    topLeft = Offset(cx + s * 0.01f, cy - s * 0.24f),
                    size = Size(s * 0.35f, s * 0.30f),
                    cornerRadius = CornerRadius(s * 0.08f)
                )
                // Seat deck & double cushions
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(Color(0xFF7B93B4), SlateFabricLight)),
                    topLeft = Offset(cx - s * 0.36f, cy + s * 0.01f),
                    size = Size(s * 0.72f, s * 0.16f),
                    cornerRadius = CornerRadius(s * 0.05f)
                )
                drawLine(SlateFabricDark, Offset(cx, cy + s * 0.01f), Offset(cx, cy + s * 0.16f), s * 0.02f)
                // Wooden base plinth
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(OakDark, OakLight, OakDark)),
                    topLeft = Offset(cx - s * 0.40f, cy + s * 0.15f),
                    size = Size(s * 0.80f, s * 0.06f),
                    cornerRadius = CornerRadius(4f)
                )
                // Sculpted armrests
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(SlateFabricLight, SlateFabricDark)),
                    topLeft = Offset(cx - s * 0.44f, cy - s * 0.07f),
                    size = Size(s * 0.12f, s * 0.24f),
                    cornerRadius = CornerRadius(s * 0.05f)
                )
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(SlateFabricLight, SlateFabricDark)),
                    topLeft = Offset(cx + s * 0.32f, cy - s * 0.07f),
                    size = Size(s * 0.12f, s * 0.24f),
                    cornerRadius = CornerRadius(s * 0.05f)
                )
                // Accent throw pillow
                drawRoundRect(
                    brush = Brush.linearGradient(listOf(TerracottaLight, TerracottaDark)),
                    topLeft = Offset(cx - s * 0.30f, cy - s * 0.12f),
                    size = Size(s * 0.16f, s * 0.14f),
                    cornerRadius = CornerRadius(s * 0.04f)
                )
            }

            "chair" -> {
                // Realistic oak wooden dining chair with slatted backrest & angled legs
                drawStudioShadow(cx, cy + s * 0.37f, s * 0.56f, s * 0.08f, shadowColor)
                // Back uprights
                drawLine(OakDark, Offset(cx - s * 0.20f, cy - s * 0.35f), Offset(cx - s * 0.20f, cy + s * 0.35f), s * 0.045f, StrokeCap.Round)
                drawLine(OakDark, Offset(cx + s * 0.20f, cy - s * 0.35f), Offset(cx + s * 0.20f, cy + s * 0.35f), s * 0.045f, StrokeCap.Round)
                // Curved backrest slats
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(OakDark, OakLight, OakMid)),
                    topLeft = Offset(cx - s * 0.21f, cy - s * 0.33f),
                    size = Size(s * 0.42f, s * 0.10f),
                    cornerRadius = CornerRadius(4f)
                )
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(OakDark, OakLight, OakMid)),
                    topLeft = Offset(cx - s * 0.20f, cy - s * 0.18f),
                    size = Size(s * 0.40f, s * 0.08f),
                    cornerRadius = CornerRadius(4f)
                )
                // Seat perspective slab + cushion
                val seat = Path().apply {
                    moveTo(cx - s * 0.23f, cy - s * 0.01f)
                    lineTo(cx + s * 0.23f, cy - s * 0.01f)
                    lineTo(cx + s * 0.28f, cy + s * 0.10f)
                    lineTo(cx - s * 0.28f, cy + s * 0.10f)
                    close()
                }
                drawPath(seat, brush = Brush.verticalGradient(listOf(OakLight, OakDark)))
                drawPath(seat, color = outlineColor.copy(alpha = 0.65f), style = fineStroke)
                // Front legs & cross stretcher
                drawLine(OakMid, Offset(cx - s * 0.24f, cy + s * 0.10f), Offset(cx - s * 0.26f, cy + s * 0.36f), s * 0.045f, StrokeCap.Round)
                drawLine(OakMid, Offset(cx + s * 0.24f, cy + s * 0.10f), Offset(cx + s * 0.26f, cy + s * 0.36f), s * 0.045f, StrokeCap.Round)
                drawLine(OakDark, Offset(cx - s * 0.22f, cy + s * 0.24f), Offset(cx + s * 0.22f, cy + s * 0.24f), s * 0.03f, StrokeCap.Round)
            }

            "bed" -> {
                // Realistic wooden bed frame with layered mattress, crisp linen duvet & pillow
                drawStudioShadow(cx, cy + s * 0.32f, s * 0.86f, s * 0.08f, shadowColor)
                // Wooden headboard & footboard
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(OakLight, OakDark)),
                    topLeft = Offset(cx - s * 0.42f, cy - s * 0.24f),
                    size = Size(s * 0.10f, s * 0.54f),
                    cornerRadius = CornerRadius(5f)
                )
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(OakLight, OakDark)),
                    topLeft = Offset(cx + s * 0.34f, cy + s * 0.02f),
                    size = Size(s * 0.08f, s * 0.28f),
                    cornerRadius = CornerRadius(4f)
                )
                // Pillow
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(Color.White, Color(0xFFE2E8F0))),
                    topLeft = Offset(cx - s * 0.31f, cy - s * 0.12f),
                    size = Size(s * 0.22f, s * 0.13f),
                    cornerRadius = CornerRadius(s * 0.06f)
                )
                // Mattress
                drawRoundRect(
                    color = CreamPaper,
                    topLeft = Offset(cx - s * 0.32f, cy),
                    size = Size(s * 0.66f, s * 0.16f),
                    cornerRadius = CornerRadius(4f)
                )
                // Folded navy-blue duvet blanket
                drawRoundRect(
                    brush = Brush.linearGradient(listOf(SlateFabricLight, SlateFabricDark)),
                    topLeft = Offset(cx - s * 0.12f, cy - s * 0.02f),
                    size = Size(s * 0.46f, s * 0.18f),
                    cornerRadius = CornerRadius(s * 0.05f)
                )
                // Bed rail
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(OakDark, OakMid, OakDark)),
                    topLeft = Offset(cx - s * 0.40f, cy + s * 0.14f),
                    size = Size(s * 0.80f, s * 0.09f),
                    cornerRadius = CornerRadius(3f)
                )
            }

            "table" -> {
                // Realistic oak dining table in crisp 3/4 perspective
                drawStudioShadow(cx, cy + s * 0.34f, s * 0.82f, s * 0.08f, shadowColor)
                // Back legs (darker for depth)
                drawLine(OakDark, Offset(cx - s * 0.28f, cy - s * 0.02f), Offset(cx - s * 0.28f, cy + s * 0.26f), s * 0.042f, StrokeCap.Round)
                drawLine(OakDark, Offset(cx + s * 0.28f, cy - s * 0.02f), Offset(cx + s * 0.28f, cy + s * 0.26f), s * 0.042f, StrokeCap.Round)
                // Tabletop surface
                val topSurface = Path().apply {
                    moveTo(cx - s * 0.34f, cy - s * 0.16f)
                    lineTo(cx + s * 0.34f, cy - s * 0.16f)
                    lineTo(cx + s * 0.44f, cy - s * 0.02f)
                    lineTo(cx - s * 0.44f, cy - s * 0.02f)
                    close()
                }
                drawPath(topSurface, brush = Brush.verticalGradient(listOf(OakLight, OakMid)))
                drawPath(topSurface, color = OakDark.copy(alpha = 0.7f), style = fineStroke)
                // Tabletop apron thickness
                drawRect(
                    brush = Brush.horizontalGradient(listOf(OakDark, OakMid, OakDark)),
                    topLeft = Offset(cx - s * 0.44f, cy - s * 0.02f),
                    size = Size(s * 0.88f, s * 0.065f)
                )
                // Front legs
                drawLine(OakMid, Offset(cx - s * 0.37f, cy + s * 0.04f), Offset(cx - s * 0.37f, cy + s * 0.34f), s * 0.05f, StrokeCap.Round)
                drawLine(OakMid, Offset(cx + s * 0.37f, cy + s * 0.04f), Offset(cx + s * 0.37f, cy + s * 0.34f), s * 0.05f, StrokeCap.Round)
            }

            "house" -> {
                // Architectural European stone/stucco house with tiled roof, chimney, door & sunlit windows
                drawStudioShadow(cx, cy + s * 0.35f, s * 0.78f, s * 0.08f, shadowColor)
                // Chimney
                drawRect(StoneDark, Offset(cx + s * 0.16f, cy - s * 0.32f), Size(s * 0.09f, s * 0.20f))
                // Facade walls
                drawRect(
                    brush = Brush.verticalGradient(listOf(CreamPaper, StoneLight)),
                    topLeft = Offset(cx - s * 0.30f, cy - s * 0.04f),
                    size = Size(s * 0.60f, s * 0.38f)
                )
                drawRect(StoneDark.copy(alpha = 0.6f), Offset(cx - s * 0.30f, cy - s * 0.04f), Size(s * 0.60f, s * 0.38f), style = fineStroke)
                // Pitched terracotta roof
                val roof = Path().apply {
                    moveTo(cx, cy - s * 0.36f)
                    lineTo(cx + s * 0.38f, cy - s * 0.02f)
                    lineTo(cx - s * 0.38f, cy - s * 0.02f)
                    close()
                }
                drawPath(roof, brush = Brush.verticalGradient(listOf(TerracottaLight, TerracottaDark)))
                drawPath(roof, color = outlineColor.copy(alpha = 0.7f), style = fineStroke)
                // Solid wood door with brass handle
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(OakMid, OakDark)),
                    topLeft = Offset(cx - s * 0.08f, cy + s * 0.11f),
                    size = Size(s * 0.16f, s * 0.23f),
                    cornerRadius = CornerRadius(4f)
                )
                drawCircle(BrassLight, s * 0.016f, Offset(cx + s * 0.04f, cy + s * 0.23f))
                // Two paned windows
                listOf(cx - s * 0.24f, cx + s * 0.12f).forEach { wx ->
                    drawRect(
                        brush = Brush.linearGradient(listOf(Color(0xFFBAE6FD), OceanMid)),
                        topLeft = Offset(wx, cy + s * 0.04f),
                        size = Size(s * 0.12f, s * 0.12f)
                    )
                    drawRect(SlateInk, Offset(wx, cy + s * 0.04f), Size(s * 0.12f, s * 0.12f), style = Stroke(s * 0.015f))
                    drawLine(Color.White, Offset(wx + s * 0.06f, cy + s * 0.04f), Offset(wx + s * 0.06f, cy + s * 0.16f), s * 0.012f)
                    drawLine(Color.White, Offset(wx, cy + s * 0.10f), Offset(wx + s * 0.12f, cy + s * 0.10f), s * 0.012f)
                }
            }

            "cat" -> {
                // Realistic shaded silhouette portrait of a seated tabby cat (mature editorial look)
                drawStudioShadow(cx, cy + s * 0.35f, s * 0.62f, s * 0.08f, shadowColor)
                // Curved tail
                val tail = Path().apply {
                    moveTo(cx + s * 0.18f, cy + s * 0.30f)
                    cubicTo(cx + s * 0.42f, cy + s * 0.26f, cx + s * 0.42f, cy + s * 0.02f, cx + s * 0.28f, cy + s * 0.04f)
                }
                drawPath(tail, color = OakMid, style = Stroke(s * 0.07f, cap = StrokeCap.Round))
                // Seated torso
                drawOval(
                    brush = Brush.verticalGradient(listOf(OakLight, OakMid, OakDark)),
                    topLeft = Offset(cx - s * 0.20f, cy - s * 0.02f),
                    size = Size(s * 0.40f, s * 0.37f)
                )
                // Pointed ears
                val lEar = Path().apply {
                    moveTo(cx - s * 0.18f, cy - s * 0.14f)
                    lineTo(cx - s * 0.16f, cy - s * 0.36f)
                    lineTo(cx - s * 0.04f, cy - s * 0.24f)
                    close()
                }
                val rEar = Path().apply {
                    moveTo(cx + s * 0.18f, cy - s * 0.14f)
                    lineTo(cx + s * 0.16f, cy - s * 0.36f)
                    lineTo(cx + s * 0.04f, cy - s * 0.24f)
                    close()
                }
                drawPath(lEar, OakDark)
                drawPath(rEar, OakDark)
                // Head with natural shading
                drawCircle(
                    brush = Brush.radialGradient(listOf(OakLight, OakMid, OakDark), Offset(cx - s * 0.05f, cy - s * 0.18f), s * 0.24f),
                    radius = s * 0.18f,
                    center = Offset(cx, cy - s * 0.14f)
                )
                // Amber eyes & delicate whiskers
                drawOval(EmeraldLight, Offset(cx - s * 0.10f, cy - s * 0.18f), Size(s * 0.06f, s * 0.045f))
                drawOval(EmeraldLight, Offset(cx + s * 0.04f, cy - s * 0.18f), Size(s * 0.06f, s * 0.045f))
                drawCircle(SlateInk, s * 0.015f, Offset(cx - s * 0.07f, cy - s * 0.158f))
                drawCircle(SlateInk, s * 0.015f, Offset(cx + s * 0.07f, cy - s * 0.158f))
                drawCircle(TerracottaDark, s * 0.022f, Offset(cx, cy - s * 0.10f))
                for (wy in listOf(-0.11f, -0.08f)) {
                    drawLine(outlineColor.copy(alpha = 0.65f), Offset(cx - s * 0.11f, cy + s * wy), Offset(cx - s * 0.27f, cy + s * (wy - 0.015f)), s * 0.012f)
                    drawLine(outlineColor.copy(alpha = 0.65f), Offset(cx + s * 0.11f, cy + s * wy), Offset(cx + s * 0.27f, cy + s * (wy - 0.015f)), s * 0.012f)
                }
            }

            "dog" -> {
                // Realistic shaded golden retriever side/front portrait
                drawStudioShadow(cx, cy + s * 0.35f, s * 0.66f, s * 0.08f, shadowColor)
                // Floppy shaded ears
                drawOval(OakDark, Offset(cx - s * 0.32f, cy - s * 0.16f), Size(s * 0.16f, s * 0.34f))
                drawOval(OakDark, Offset(cx + s * 0.16f, cy - s * 0.16f), Size(s * 0.16f, s * 0.34f))
                // Neck & chest
                drawOval(
                    brush = Brush.verticalGradient(listOf(BrassLight, OakLight)),
                    topLeft = Offset(cx - s * 0.22f, cy + s * 0.08f),
                    size = Size(s * 0.44f, s * 0.28f)
                )
                // Head
                drawOval(
                    brush = Brush.radialGradient(listOf(Color(0xFFF6DFB3), BrassLight, OakMid), Offset(cx - s * 0.05f, cy - s * 0.12f), s * 0.32f),
                    topLeft = Offset(cx - s * 0.24f, cy - s * 0.26f),
                    size = Size(s * 0.48f, s * 0.46f)
                )
                // Snout
                drawOval(CreamPaper, Offset(cx - s * 0.12f, cy - s * 0.01f), Size(s * 0.24f, s * 0.17f))
                // Realistic dark eyes & shiny nose
                drawCircle(SlateInk, s * 0.032f, Offset(cx - s * 0.09f, cy - s * 0.08f))
                drawCircle(Color.White, s * 0.01f, Offset(cx - s * 0.10f, cy - s * 0.09f))
                drawCircle(SlateInk, s * 0.032f, Offset(cx + s * 0.09f, cy - s * 0.08f))
                drawCircle(Color.White, s * 0.01f, Offset(cx + s * 0.08f, cy - s * 0.09f))
                drawOval(SlateInk, Offset(cx - s * 0.065f, cy + s * 0.01f), Size(s * 0.13f, s * 0.085f))
                drawOval(Color.White.copy(alpha = 0.45f), Offset(cx - s * 0.03f, cy + s * 0.025f), Size(s * 0.04f, s * 0.02f))
            }

            "bus" -> {
                // Iconic realistic London double-decker red bus with 2 decks of reflective glass & alloy wheels
                drawStudioShadow(cx, cy + s * 0.32f, s * 0.86f, s * 0.08f, shadowColor)
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(CrimsonLight, CrimsonDark)),
                    topLeft = Offset(cx - s * 0.40f, cy - s * 0.28f),
                    size = Size(s * 0.80f, s * 0.50f),
                    cornerRadius = CornerRadius(s * 0.08f)
                )
                // Upper deck windows
                for (i in 0..2) {
                    val wx = cx - s * 0.34f + i * s * 0.23f
                    drawRoundRect(
                        brush = Brush.verticalGradient(listOf(Color(0xFFBAE6FD), OceanDark)),
                        topLeft = Offset(wx, cy - s * 0.22f),
                        size = Size(s * 0.20f, s * 0.14f),
                        cornerRadius = CornerRadius(3f)
                    )
                }
                // Cream divider stripe
                drawRect(CreamPaper, Offset(cx - s * 0.40f, cy - s * 0.05f), Size(s * 0.80f, s * 0.035f))
                // Lower deck windows
                for (i in 0..2) {
                    val wx = cx - s * 0.34f + i * s * 0.23f
                    drawRoundRect(
                        brush = Brush.verticalGradient(listOf(Color(0xFFBAE6FD), OceanDark)),
                        topLeft = Offset(wx, cy + s * 0.01f),
                        size = Size(s * 0.20f, s * 0.13f),
                        cornerRadius = CornerRadius(3f)
                    )
                }
                // Rubber tires + steel rims
                listOf(cx - s * 0.22f, cx + s * 0.22f).forEach { wx ->
                    drawCircle(SlateInk, s * 0.095f, Offset(wx, cy + s * 0.23f))
                    drawCircle(SteelLight, s * 0.045f, Offset(wx, cy + s * 0.23f))
                }
            }

            "apple" -> {
                // Photorealistic shaded red apple with curved stem, specular highlight & veined leaf
                drawStudioShadow(cx, cy + s * 0.35f, s * 0.54f, s * 0.08f, shadowColor)
                val applePath = Path().apply {
                    moveTo(cx, cy - s * 0.16f)
                    cubicTo(cx - s * 0.30f, cy - s * 0.28f, cx - s * 0.38f, cy + s * 0.18f, cx - s * 0.12f, cy + s * 0.32f)
                    quadraticTo(cx, cy + s * 0.28f, cx + s * 0.12f, cy + s * 0.32f)
                    cubicTo(cx + s * 0.38f, cy + s * 0.18f, cx + s * 0.30f, cy - s * 0.28f, cx, cy - s * 0.16f)
                    close()
                }
                drawPath(
                    path = applePath,
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFF7A6B), CrimsonLight, CrimsonDark),
                        center = Offset(cx - s * 0.10f, cy - s * 0.06f),
                        radius = s * 0.42f
                    )
                )
                // Gloss specular highlight
                drawOval(Color.White.copy(alpha = 0.42f), Offset(cx - s * 0.18f, cy - s * 0.10f), Size(s * 0.08f, s * 0.16f))
                // Woody stem
                drawLine(OakDark, Offset(cx, cy - s * 0.16f), Offset(cx + s * 0.05f, cy - s * 0.34f), s * 0.036f, StrokeCap.Round)
                // Botanical leaf
                val leaf = Path().apply {
                    moveTo(cx + s * 0.03f, cy - s * 0.26f)
                    quadraticTo(cx + s * 0.22f, cy - s * 0.36f, cx + s * 0.28f, cy - s * 0.22f)
                    quadraticTo(cx + s * 0.14f, cy - s * 0.16f, cx + s * 0.03f, cy - s * 0.26f)
                    close()
                }
                drawPath(leaf, brush = Brush.linearGradient(listOf(EmeraldLight, EmeraldDark)))
            }

            "sun" -> {
                // Radiant realistic solar corona & glowing photosphere
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFF7CC), AmberGlow.copy(alpha = 0.55f), Color.Transparent),
                        center = Offset(cx, cy),
                        radius = s * 0.46f
                    ),
                    radius = s * 0.46f,
                    center = Offset(cx, cy)
                )
                for (i in 0 until 12) {
                    val a = i * PI / 6.0
                    val r1 = s * 0.26f
                    val r2 = if (i % 2 == 0) s * 0.40f else s * 0.34f
                    drawLine(
                        color = if (i % 2 == 0) AmberGlow else BrassLight,
                        start = Offset(cx + (r1 * cos(a)).toFloat(), cy + (r1 * sin(a)).toFloat()),
                        end = Offset(cx + (r2 * cos(a)).toFloat(), cy + (r2 * sin(a)).toFloat()),
                        strokeWidth = s * 0.032f,
                        cap = StrokeCap.Round
                    )
                }
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFFBEB), Color(0xFFFDE047), AmberGlow, TerracottaLight),
                        center = Offset(cx - s * 0.06f, cy - s * 0.06f),
                        radius = s * 0.25f
                    ),
                    radius = s * 0.22f,
                    center = Offset(cx, cy)
                )
            }

            "rain" -> {
                // Atmospheric layered raincloud with translucent falling rain streaks
                val cloudGrad = Brush.verticalGradient(listOf(Color(0xFFF1F5F9), Color(0xFF94A3B8), Color(0xFF64748B)))
                drawCircle(cloudGrad, s * 0.17f, Offset(cx - s * 0.16f, cy - s * 0.08f))
                drawCircle(cloudGrad, s * 0.22f, Offset(cx + s * 0.01f, cy - s * 0.16f))
                drawCircle(cloudGrad, s * 0.16f, Offset(cx + s * 0.19f, cy - s * 0.07f))
                drawRoundRect(cloudGrad, Offset(cx - s * 0.30f, cy - s * 0.10f), Size(s * 0.60f, s * 0.18f), CornerRadius(s * 0.09f))
                for (i in -1..1) {
                    val rx = cx + i * s * 0.16f
                    drawLine(
                        brush = Brush.verticalGradient(listOf(OceanLight, OceanDark)),
                        start = Offset(rx, cy + s * 0.14f),
                        end = Offset(rx - s * 0.06f, cy + s * 0.34f),
                        strokeWidth = s * 0.038f,
                        cap = StrokeCap.Round
                    )
                }
            }

            "book" -> {
                // Realistic hardcover academic textbook open at an angle with pages & bookmark
                drawStudioShadow(cx, cy + s * 0.31f, s * 0.80f, s * 0.08f, shadowColor)
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(SlateFabricDark, SlateFabricLight, SlateFabricDark)),
                    topLeft = Offset(cx - s * 0.40f, cy - s * 0.22f),
                    size = Size(s * 0.80f, s * 0.50f),
                    cornerRadius = CornerRadius(6f)
                )
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(StoneLight, CreamPaper)),
                    topLeft = Offset(cx - s * 0.36f, cy - s * 0.25f),
                    size = Size(s * 0.355f, s * 0.46f),
                    cornerRadius = CornerRadius(4f)
                )
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(CreamPaper, StoneLight)),
                    topLeft = Offset(cx + s * 0.005f, cy - s * 0.25f),
                    size = Size(s * 0.355f, s * 0.46f),
                    cornerRadius = CornerRadius(4f)
                )
                // Printed text lines
                for (r in 0..3) {
                    val ly = cy - s * 0.16f + r * s * 0.085f
                    drawLine(SteelDark.copy(alpha = 0.55f), Offset(cx - s * 0.30f, ly), Offset(cx - s * 0.07f, ly), s * 0.018f, StrokeCap.Round)
                    drawLine(SteelDark.copy(alpha = 0.55f), Offset(cx + s * 0.07f, ly), Offset(cx + s * 0.30f, ly), s * 0.018f, StrokeCap.Round)
                }
                drawLine(SlateInk.copy(alpha = 0.5f), Offset(cx, cy - s * 0.25f), Offset(cx, cy + s * 0.22f), s * 0.025f)
            }

            "pencil" -> {
                // Realistic hexagonal drafting pencil with brushed metal ferrule, eraser & graphite tip
                drawStudioShadow(cx, cy + s * 0.38f, s * 0.36f, s * 0.06f, shadowColor)
                // Eraser
                drawRoundRect(TerracottaLight, Offset(cx - s * 0.09f, cy - s * 0.36f), Size(s * 0.18f, s * 0.09f), CornerRadius(6f))
                // Metallic ferrule
                drawRect(
                    brush = Brush.horizontalGradient(listOf(SteelDark, SteelLight, SteelDark)),
                    topLeft = Offset(cx - s * 0.09f, cy - s * 0.28f),
                    size = Size(s * 0.18f, s * 0.08f)
                )
                // Hexagonal lacquered body (3 shaded facets)
                drawRect(BrassDark, Offset(cx - s * 0.09f, cy - s * 0.20f), Size(s * 0.06f, s * 0.36f))
                drawRect(BrassLight, Offset(cx - s * 0.03f, cy - s * 0.20f), Size(s * 0.06f, s * 0.36f))
                drawRect(BrassMid, Offset(cx + s * 0.03f, cy - s * 0.20f), Size(s * 0.06f, s * 0.36f))
                // Sharpened wood cone
                val cone = Path().apply {
                    moveTo(cx - s * 0.09f, cy + s * 0.16f)
                    lineTo(cx + s * 0.09f, cy + s * 0.16f)
                    lineTo(cx, cy + s * 0.36f)
                    close()
                }
                drawPath(cone, brush = Brush.horizontalGradient(listOf(OakMid, OakLight, OakDark)))
                // Graphite lead point
                val lead = Path().apply {
                    moveTo(cx - s * 0.032f, cy + s * 0.29f)
                    lineTo(cx + s * 0.032f, cy + s * 0.29f)
                    lineTo(cx, cy + s * 0.36f)
                    close()
                }
                drawPath(lead, SlateInk)
            }

            "plant" -> {
                // Realistic terracotta potted botanical plant with shaded foliage
                drawStudioShadow(cx, cy + s * 0.36f, s * 0.48f, s * 0.07f, shadowColor)
                // Stem
                drawLine(EmeraldDark, Offset(cx, cy + s * 0.10f), Offset(cx, cy - s * 0.26f), s * 0.036f, StrokeCap.Round)
                // Botanical leaves
                val leftLeaf = Path().apply {
                    moveTo(cx, cy - s * 0.04f)
                    quadraticTo(cx - s * 0.28f, cy - s * 0.24f, cx - s * 0.30f, cy - s * 0.06f)
                    quadraticTo(cx - s * 0.16f, cy + s * 0.04f, cx, cy - s * 0.04f)
                    close()
                }
                val rightLeaf = Path().apply {
                    moveTo(cx, cy - s * 0.12f)
                    quadraticTo(cx + s * 0.28f, cy - s * 0.34f, cx + s * 0.30f, cy - s * 0.14f)
                    quadraticTo(cx + s * 0.16f, cy - s * 0.04f, cx, cy - s * 0.12f)
                    close()
                }
                val topLeaf = Path().apply {
                    moveTo(cx, cy - s * 0.18f)
                    quadraticTo(cx - s * 0.10f, cy - s * 0.38f, cx, cy - s * 0.40f)
                    quadraticTo(cx + s * 0.10f, cy - s * 0.38f, cx, cy - s * 0.18f)
                    close()
                }
                drawPath(leftLeaf, brush = Brush.linearGradient(listOf(EmeraldLight, EmeraldDark)))
                drawPath(rightLeaf, brush = Brush.linearGradient(listOf(EmeraldLight, EmeraldMid)))
                drawPath(topLeaf, brush = Brush.verticalGradient(listOf(EmeraldLight, EmeraldDark)))
                // Terracotta pot
                val pot = Path().apply {
                    moveTo(cx - s * 0.20f, cy + s * 0.08f)
                    lineTo(cx + s * 0.20f, cy + s * 0.08f)
                    lineTo(cx + s * 0.14f, cy + s * 0.35f)
                    lineTo(cx - s * 0.14f, cy + s * 0.35f)
                    close()
                }
                drawPath(pot, brush = Brush.horizontalGradient(listOf(TerracottaDark, TerracottaLight, TerracottaDark)))
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(TerracottaDark, TerracottaLight, TerracottaDark)),
                    topLeft = Offset(cx - s * 0.23f, cy + s * 0.04f),
                    size = Size(s * 0.46f, s * 0.08f),
                    cornerRadius = CornerRadius(4f)
                )
            }

            "water_drop" -> {
                drawStudioShadow(cx, cy + s * 0.38f, s * 0.44f, s * 0.07f, shadowColor)
                val drop = Path().apply {
                    moveTo(cx, cy - s * 0.36f)
                    cubicTo(cx + s * 0.34f, cy + s * 0.04f, cx + s * 0.28f, cy + s * 0.35f, cx, cy + s * 0.35f)
                    cubicTo(cx - s * 0.28f, cy + s * 0.35f, cx - s * 0.34f, cy + s * 0.04f, cx, cy - s * 0.36f)
                    close()
                }
                drawPath(
                    path = drop,
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFE0F2FE), OceanLight, OceanMid, OceanDark),
                        center = Offset(cx - s * 0.08f, cy + s * 0.02f),
                        radius = s * 0.42f
                    )
                )
                drawOval(Color.White.copy(alpha = 0.6f), Offset(cx - s * 0.12f, cy + s * 0.02f), Size(s * 0.07f, s * 0.16f))
            }

            "ice_cube" -> {
                // Realistic 3D isometric translucent crystal ice cube
                drawStudioShadow(cx, cy + s * 0.36f, s * 0.62f, s * 0.09f, shadowColor)
                val topFace = Path().apply {
                    moveTo(cx, cy - s * 0.34f)
                    lineTo(cx + s * 0.30f, cy - s * 0.18f)
                    lineTo(cx, cy - s * 0.02f)
                    lineTo(cx - s * 0.30f, cy - s * 0.18f)
                    close()
                }
                val leftFace = Path().apply {
                    moveTo(cx - s * 0.30f, cy - s * 0.18f)
                    lineTo(cx, cy - s * 0.02f)
                    lineTo(cx, cy + s * 0.32f)
                    lineTo(cx - s * 0.30f, cy + s * 0.16f)
                    close()
                }
                val rightFace = Path().apply {
                    moveTo(cx + s * 0.30f, cy - s * 0.18f)
                    lineTo(cx, cy - s * 0.02f)
                    lineTo(cx, cy + s * 0.32f)
                    lineTo(cx + s * 0.30f, cy + s * 0.16f)
                    close()
                }
                drawPath(topFace, brush = Brush.linearGradient(listOf(Color.White, Color(0xFFBAE6FD))))
                drawPath(leftFace, brush = Brush.verticalGradient(listOf(Color(0xFF7DD3FC), OceanMid)))
                drawPath(rightFace, brush = Brush.verticalGradient(listOf(Color(0xFF38BDF8), OceanDark)))
                drawPath(topFace, Color.White.copy(alpha = 0.8f), style = fineStroke)
                drawPath(leftFace, OceanDark.copy(alpha = 0.45f), style = fineStroke)
                drawPath(rightFace, OceanDark.copy(alpha = 0.55f), style = fineStroke)
            }

            "cloud_steam" -> {
                // Rising warm vapor wisps & translucent steam cloud
                for (i in -1..1) {
                    val sx = cx + i * s * 0.14f
                    val wave = Path().apply {
                        moveTo(sx, cy + s * 0.34f)
                        quadraticTo(sx - s * 0.08f, cy + s * 0.18f, sx, cy + s * 0.06f)
                        quadraticTo(sx + s * 0.08f, cy - s * 0.06f, sx, cy - s * 0.18f)
                    }
                    drawPath(wave, color = OceanLight.copy(alpha = 0.7f), style = Stroke(s * 0.032f, cap = StrokeCap.Round))
                }
                val vaporGrad = Brush.verticalGradient(listOf(Color.White, Color(0xFFCBD5E1)))
                drawCircle(vaporGrad, s * 0.15f, Offset(cx - s * 0.15f, cy - s * 0.10f))
                drawCircle(vaporGrad, s * 0.20f, Offset(cx, cy - s * 0.18f))
                drawCircle(vaporGrad, s * 0.14f, Offset(cx + s * 0.16f, cy - s * 0.09f))
            }

            "planet_saturn" -> {
                // Realistic banded gas giant with tilted luminous ring system
                // Back ring arc
                drawArc(
                    brush = Brush.horizontalGradient(listOf(BrassDark, BrassLight, BrassDark)),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(cx - s * 0.44f, cy - s * 0.10f),
                    size = Size(s * 0.88f, s * 0.20f),
                    style = Stroke(s * 0.055f)
                )
                // Planet sphere
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFEF3C7), BrassLight, OakMid, Color(0xFF451A03)),
                        center = Offset(cx - s * 0.08f, cy - s * 0.08f),
                        radius = s * 0.30f
                    ),
                    radius = s * 0.24f,
                    center = Offset(cx, cy)
                )
                // Front ring arc overlapping planet
                drawArc(
                    brush = Brush.horizontalGradient(listOf(BrassMid, Color(0xFFFEF3C7), BrassMid)),
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(cx - s * 0.44f, cy - s * 0.10f),
                    size = Size(s * 0.88f, s * 0.20f),
                    style = Stroke(s * 0.06f)
                )
            }

            "heart" -> {
                drawStudioShadow(cx, cy + s * 0.36f, s * 0.48f, s * 0.07f, shadowColor)
                val heart = Path().apply {
                    moveTo(cx, cy + s * 0.32f)
                    cubicTo(cx - s * 0.44f, cy + s * 0.04f, cx - s * 0.34f, cy - s * 0.34f, cx, cy - s * 0.12f)
                    cubicTo(cx + s * 0.34f, cy - s * 0.34f, cx + s * 0.44f, cy + s * 0.04f, cx, cy + s * 0.32f)
                    close()
                }
                drawPath(
                    path = heart,
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFF7A6B), CrimsonLight, CrimsonDark),
                        center = Offset(cx - s * 0.10f, cy - s * 0.10f),
                        radius = s * 0.46f
                    )
                )
                drawOval(Color.White.copy(alpha = 0.35f), Offset(cx - s * 0.20f, cy - s * 0.16f), Size(s * 0.08f, s * 0.12f))
            }

            "volcano" -> {
                // Realistic volcanic cone with glowing magma crater & ash plume
                drawCircle(StoneMid.copy(alpha = 0.45f), s * 0.13f, Offset(cx - s * 0.06f, cy - s * 0.30f))
                drawCircle(StoneMid.copy(alpha = 0.35f), s * 0.16f, Offset(cx + s * 0.09f, cy - s * 0.34f))
                val mtn = Path().apply {
                    moveTo(cx - s * 0.42f, cy + s * 0.34f)
                    lineTo(cx - s * 0.12f, cy - s * 0.16f)
                    lineTo(cx + s * 0.12f, cy - s * 0.16f)
                    lineTo(cx + s * 0.42f, cy + s * 0.34f)
                    close()
                }
                drawPath(mtn, brush = Brush.verticalGradient(listOf(Color(0xFF475569), Color(0xFF1E293B))))
                // Glowing magma stream
                val lava = Path().apply {
                    moveTo(cx - s * 0.10f, cy - s * 0.16f)
                    lineTo(cx + s * 0.10f, cy - s * 0.16f)
                    lineTo(cx + s * 0.04f, cy + s * 0.08f)
                    lineTo(cx - s * 0.02f, cy - s * 0.02f)
                    lineTo(cx - s * 0.08f, cy + s * 0.12f)
                    close()
                }
                drawPath(lava, brush = Brush.verticalGradient(listOf(Color(0xFFFDE047), AmberGlow, CrimsonLight)))
            }

            "magnet" -> {
                // Realistic brushed metallic U-magnet with red/blue poles & silver tips
                val strokeW = s * 0.14f
                drawArc(
                    brush = Brush.horizontalGradient(listOf(CrimsonLight, CrimsonDark, OceanDark, OceanLight)),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(cx - s * 0.24f, cy - s * 0.24f),
                    size = Size(s * 0.48f, s * 0.48f),
                    style = Stroke(strokeW)
                )
                drawLine(CrimsonLight, Offset(cx - s * 0.24f, cy), Offset(cx - s * 0.24f, cy + s * 0.16f), strokeW)
                drawLine(OceanMid, Offset(cx + s * 0.24f, cy), Offset(cx + s * 0.24f, cy + s * 0.16f), strokeW)
                // Brushed steel tips
                drawLine(SteelLight, Offset(cx - s * 0.24f, cy + s * 0.16f), Offset(cx - s * 0.24f, cy + s * 0.28f), strokeW)
                drawLine(SteelLight, Offset(cx + s * 0.24f, cy + s * 0.16f), Offset(cx + s * 0.24f, cy + s * 0.28f), strokeW)
            }

            "castle" -> {
                // Realistic medieval stone fortress with crenellated towers, arched portcullis & masonry shading
                drawStudioShadow(cx, cy + s * 0.35f, s * 0.80f, s * 0.08f, shadowColor)
                // Central curtain wall
                drawRect(
                    brush = Brush.verticalGradient(listOf(StoneLight, StoneMid)),
                    topLeft = Offset(cx - s * 0.28f, cy - s * 0.06f),
                    size = Size(s * 0.56f, s * 0.40f)
                )
                // Flanking watchtowers
                listOf(cx - s * 0.38f, cx + s * 0.18f).forEach { tx ->
                    drawRect(
                        brush = Brush.horizontalGradient(listOf(StoneDark, StoneLight, StoneMid)),
                        topLeft = Offset(tx, cy - s * 0.26f),
                        size = Size(s * 0.20f, s * 0.60f)
                    )
                    // Battlements
                    drawRect(StoneLight, Offset(tx - s * 0.02f, cy - s * 0.32f), Size(s * 0.07f, s * 0.07f))
                    drawRect(StoneLight, Offset(tx + s * 0.07f, cy - s * 0.32f), Size(s * 0.06f, s * 0.07f))
                    drawRect(StoneLight, Offset(tx + s * 0.15f, cy - s * 0.32f), Size(s * 0.07f, s * 0.07f))
                    // Arrow slit
                    drawRect(SlateInk, Offset(tx + s * 0.085f, cy - s * 0.12f), Size(s * 0.03f, s * 0.11f))
                }
                // Arched wooden gate
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(OakMid, OakDark)),
                    topLeft = Offset(cx - s * 0.10f, cy + s * 0.11f),
                    size = Size(s * 0.20f, s * 0.23f),
                    cornerRadius = CornerRadius(s * 0.10f)
                )
            }

            "pyramid" -> {
                // Realistic sun-lit Egyptian Giza pyramid with light/shadow faces & horizon
                drawStudioShadow(cx, cy + s * 0.34f, s * 0.86f, s * 0.08f, shadowColor)
                val sunlitFace = Path().apply {
                    moveTo(cx, cy - s * 0.34f)
                    lineTo(cx + s * 0.08f, cy + s * 0.32f)
                    lineTo(cx - s * 0.40f, cy + s * 0.32f)
                    close()
                }
                val shadowFace = Path().apply {
                    moveTo(cx, cy - s * 0.34f)
                    lineTo(cx + s * 0.40f, cy + s * 0.32f)
                    lineTo(cx + s * 0.08f, cy + s * 0.32f)
                    close()
                }
                drawPath(sunlitFace, brush = Brush.verticalGradient(listOf(Color(0xFFFDE68A), BrassLight, BrassMid)))
                drawPath(shadowFace, brush = Brush.verticalGradient(listOf(BrassMid, BrassDark)))
                // Horizontal stone courses
                for (r in 1..4) {
                    val y = cy - s * 0.34f + r * s * 0.13f
                    val frac = r / 5f
                    drawLine(
                        color = BrassDark.copy(alpha = 0.28f),
                        start = Offset(cx - s * 0.40f * frac, y),
                        end = Offset(cx + s * 0.40f * frac, y),
                        strokeWidth = 1.2f
                    )
                }
            }

            "compass" -> {
                // Realistic antique brass nautical compass with graduated rose & north/south needle
                drawStudioShadow(cx, cy + s * 0.38f, s * 0.62f, s * 0.08f, shadowColor)
                drawCircle(
                    brush = Brush.linearGradient(listOf(BrassLight, BrassMid, BrassDark)),
                    radius = s * 0.37f,
                    center = Offset(cx, cy)
                )
                drawCircle(CreamPaper, s * 0.30f, Offset(cx, cy))
                // 8 compass ticks
                for (i in 0 until 8) {
                    val a = i * PI / 4.0
                    val rIn = if (i % 2 == 0) s * 0.22f else s * 0.25f
                    drawLine(
                        SlateInk.copy(alpha = 0.7f),
                        Offset(cx + (rIn * cos(a)).toFloat(), cy + (rIn * sin(a)).toFloat()),
                        Offset(cx + (s * 0.29f * cos(a)).toFloat(), cy + (s * 0.29f * sin(a)).toFloat()),
                        strokeWidth = if (i % 2 == 0) s * 0.022f else s * 0.013f
                    )
                }
                val northNeedle = Path().apply {
                    moveTo(cx, cy - s * 0.24f)
                    lineTo(cx + s * 0.06f, cy)
                    lineTo(cx - s * 0.06f, cy)
                    close()
                }
                val southNeedle = Path().apply {
                    moveTo(cx, cy + s * 0.24f)
                    lineTo(cx + s * 0.06f, cy)
                    lineTo(cx - s * 0.06f, cy)
                    close()
                }
                drawPath(northNeedle, CrimsonLight)
                drawPath(southNeedle, SlateInk)
                drawCircle(BrassMid, s * 0.045f, Offset(cx, cy))
            }

            "globe" -> {
                // Realistic illuminated Earth globe mounted on brass meridian arc
                drawStudioShadow(cx, cy + s * 0.38f, s * 0.56f, s * 0.07f, shadowColor)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF7DD3FC), OceanMid, OceanDark),
                        center = Offset(cx - s * 0.10f, cy - s * 0.10f),
                        radius = s * 0.42f
                    ),
                    radius = s * 0.32f,
                    center = Offset(cx, cy - s * 0.02f)
                )
                // Shaded continents
                drawOval(EmeraldMid, Offset(cx - s * 0.20f, cy - s * 0.20f), Size(s * 0.20f, s * 0.24f))
                drawOval(EmeraldLight, Offset(cx + s * 0.03f, cy - s * 0.12f), Size(s * 0.18f, s * 0.16f))
                drawOval(EmeraldDark, Offset(cx + s * 0.08f, cy + s * 0.06f), Size(s * 0.14f, s * 0.12f))
                // Brass meridian cradle & stand
                drawArc(
                    color = BrassMid,
                    startAngle = -55f,
                    sweepAngle = 210f,
                    useCenter = false,
                    topLeft = Offset(cx - s * 0.37f, cy - s * 0.39f),
                    size = Size(s * 0.74f, s * 0.74f),
                    style = Stroke(s * 0.032f, cap = StrokeCap.Round)
                )
                drawLine(BrassDark, Offset(cx, cy + s * 0.34f), Offset(cx, cy + s * 0.40f), s * 0.04f)
                drawLine(BrassMid, Offset(cx - s * 0.16f, cy + s * 0.40f), Offset(cx + s * 0.16f, cy + s * 0.40f), s * 0.045f, StrokeCap.Round)
            }

            "mountain" -> {
                // Realistic snow-capped alpine peaks with sunlit/shadow ridges
                val leftPeak = Path().apply {
                    moveTo(cx - s * 0.44f, cy + s * 0.32f)
                    lineTo(cx - s * 0.08f, cy - s * 0.32f)
                    lineTo(cx + s * 0.24f, cy + s * 0.32f)
                    close()
                }
                val rightPeak = Path().apply {
                    moveTo(cx - s * 0.06f, cy + s * 0.32f)
                    lineTo(cx + s * 0.22f, cy - s * 0.18f)
                    lineTo(cx + s * 0.44f, cy + s * 0.32f)
                    close()
                }
                drawPath(rightPeak, brush = Brush.verticalGradient(listOf(Color(0xFF64748B), Color(0xFF334155))))
                drawPath(leftPeak, brush = Brush.horizontalGradient(listOf(Color(0xFF475569), Color(0xFF94A3B8))))
                // Snow cap
                val snow = Path().apply {
                    moveTo(cx - s * 0.08f, cy - s * 0.32f)
                    lineTo(cx + s * 0.03f, cy - s * 0.10f)
                    lineTo(cx - s * 0.04f, cy - s * 0.14f)
                    lineTo(cx - s * 0.11f, cy - s * 0.08f)
                    lineTo(cx - s * 0.19f, cy - s * 0.11f)
                    close()
                }
                drawPath(snow, Color.White)
            }

            "crown" -> {
                // Realistic burnished gold royal crown with velvet cap & gemstones
                drawStudioShadow(cx, cy + s * 0.29f, s * 0.72f, s * 0.08f, shadowColor)
                val crown = Path().apply {
                    moveTo(cx - s * 0.34f, cy + s * 0.22f)
                    lineTo(cx - s * 0.36f, cy - s * 0.16f)
                    lineTo(cx - s * 0.14f, cy + s * 0.01f)
                    lineTo(cx, cy - s * 0.28f)
                    lineTo(cx + s * 0.14f, cy + s * 0.01f)
                    lineTo(cx + s * 0.36f, cy - s * 0.16f)
                    lineTo(cx + s * 0.34f, cy + s * 0.22f)
                    close()
                }
                drawPath(crown, brush = Brush.verticalGradient(listOf(Color(0xFFFEF08A), BrassMid, BrassDark)))
                drawPath(crown, color = BrassDark, style = fineStroke)
                // Ruby & sapphire jewels on band
                drawCircle(CrimsonLight, s * 0.042f, Offset(cx, cy + s * 0.11f))
                drawCircle(OceanMid, s * 0.035f, Offset(cx - s * 0.18f, cy + s * 0.11f))
                drawCircle(OceanMid, s * 0.035f, Offset(cx + s * 0.18f, cy + s * 0.11f))
                drawCircle(BrassLight, s * 0.035f, Offset(cx, cy - s * 0.29f))
            }

            "hourglass" -> {
                // Realistic wooden & blown-glass sand hourglass
                drawStudioShadow(cx, cy + s * 0.36f, s * 0.56f, s * 0.07f, shadowColor)
                // Glass bulbs
                val topGlass = Path().apply {
                    moveTo(cx - s * 0.22f, cy - s * 0.28f)
                    lineTo(cx + s * 0.22f, cy - s * 0.28f)
                    quadraticTo(cx + s * 0.20f, cy - s * 0.06f, cx, cy)
                    quadraticTo(cx - s * 0.20f, cy - s * 0.06f, cx - s * 0.22f, cy - s * 0.28f)
                    close()
                }
                val botGlass = Path().apply {
                    moveTo(cx - s * 0.22f, cy + s * 0.28f)
                    lineTo(cx + s * 0.22f, cy + s * 0.28f)
                    quadraticTo(cx + s * 0.20f, cy + s * 0.06f, cx, cy)
                    quadraticTo(cx - s * 0.20f, cy + s * 0.06f, cx - s * 0.22f, cy + s * 0.28f)
                    close()
                }
                drawPath(topGlass, Color(0xFFE0F2FE).copy(alpha = 0.55f))
                drawPath(botGlass, Color(0xFFE0F2FE).copy(alpha = 0.55f))
                // Golden sand
                val topSand = Path().apply {
                    moveTo(cx - s * 0.14f, cy - s * 0.16f)
                    lineTo(cx + s * 0.14f, cy - s * 0.16f)
                    lineTo(cx, cy - s * 0.01f)
                    close()
                }
                val botSand = Path().apply {
                    moveTo(cx - s * 0.19f, cy + s * 0.28f)
                    lineTo(cx + s * 0.19f, cy + s * 0.28f)
                    lineTo(cx, cy + s * 0.10f)
                    close()
                }
                drawPath(topSand, BrassLight)
                drawPath(botSand, brush = Brush.verticalGradient(listOf(BrassLight, BrassMid)))
                drawLine(BrassLight, Offset(cx, cy), Offset(cx, cy + s * 0.16f), s * 0.02f)
                drawPath(topGlass, SteelDark.copy(alpha = 0.65f), style = fineStroke)
                drawPath(botGlass, SteelDark.copy(alpha = 0.65f), style = fineStroke)
                // Turned mahogany caps
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(OakDark, OakLight, OakDark)),
                    topLeft = Offset(cx - s * 0.28f, cy - s * 0.34f),
                    size = Size(s * 0.56f, s * 0.07f),
                    cornerRadius = CornerRadius(4f)
                )
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(OakDark, OakLight, OakDark)),
                    topLeft = Offset(cx - s * 0.28f, cy + s * 0.27f),
                    size = Size(s * 0.56f, s * 0.07f),
                    cornerRadius = CornerRadius(4f)
                )
            }
        }
    }
}

private fun DrawScope.drawStudioShadow(
    cx: Float,
    cy: Float,
    width: Float,
    height: Float,
    color: Color
) {
    drawOval(
        color = color,
        topLeft = Offset(cx - width / 2f, cy - height / 2f),
        size = Size(width, height)
    )
}

private fun DrawScope.drawFractionDiagram(
    cx: Float,
    cy: Float,
    radius: Float,
    numerator: Int,
    denominator: Int,
    outlineColor: Color,
    stroke: Stroke
) {
    val topLeft = Offset(cx - radius, cy - radius)
    val arcSize = Size(radius * 2, radius * 2)
    drawCircle(CreamPaper, radius, Offset(cx, cy))
    val sweepPerSlice = 360f / denominator.coerceAtLeast(1)
    drawArc(
        brush = Brush.radialGradient(
            colors = listOf(TerracottaLight, TerracottaDark),
            center = Offset(cx - radius * 0.2f, cy - radius * 0.2f),
            radius = radius * 1.3f
        ),
        startAngle = -90f,
        sweepAngle = sweepPerSlice * numerator,
        useCenter = true,
        topLeft = topLeft,
        size = arcSize
    )
    drawCircle(outlineColor, radius, Offset(cx, cy), style = stroke)
    for (i in 0 until denominator) {
        val angleRad = (-90.0 + i * sweepPerSlice) * PI / 180.0
        val endX = cx + (radius * cos(angleRad)).toFloat()
        val endY = cy + (radius * sin(angleRad)).toFloat()
        drawLine(outlineColor, Offset(cx, cy), Offset(endX, endY), strokeWidth = stroke.width * 0.9f)
    }
    drawCircle(outlineColor, radius * 0.07f, Offset(cx, cy))
}

private fun DrawScope.drawRealisticClock(
    cx: Float,
    cy: Float,
    radius: Float,
    hour: Int,
    outlineColor: Color
) {
    // Metallic brushed bezel
    drawCircle(
        brush = Brush.linearGradient(listOf(SteelLight, SteelDark)),
        radius = radius,
        center = Offset(cx, cy)
    )
    drawCircle(CreamPaper, radius * 0.87f, Offset(cx, cy))
    // 12 precision hour markers
    for (i in 0 until 12) {
        val a = (-90.0 + i * 30.0) * PI / 180.0
        val isMajor = i % 3 == 0
        val r1 = radius * if (isMajor) 0.67f else 0.74f
        val r2 = radius * 0.82f
        drawLine(
            color = SlateInk,
            start = Offset(cx + (r1 * cos(a)).toFloat(), cy + (r1 * sin(a)).toFloat()),
            end = Offset(cx + (r2 * cos(a)).toFloat(), cy + (r2 * sin(a)).toFloat()),
            strokeWidth = if (isMajor) radius * 0.085f else radius * 0.04f,
            cap = StrokeCap.Round
        )
    }
    // Minute hand at 12
    drawLine(
        color = TealAccent,
        start = Offset(cx, cy),
        end = Offset(cx, cy - radius * 0.64f),
        strokeWidth = radius * 0.075f,
        cap = StrokeCap.Round
    )
    // Hour hand
    val angle = (-90.0 + hour * 30.0) * PI / 180.0
    drawLine(
        color = CrimsonDark,
        start = Offset(cx, cy),
        end = Offset(cx + (radius * 0.46f * cos(angle)).toFloat(), cy + (radius * 0.46f * sin(angle)).toFloat()),
        strokeWidth = radius * 0.105f,
        cap = StrokeCap.Round
    )
    drawCircle(outlineColor, radius * 0.09f, Offset(cx, cy))
    drawCircle(BrassLight, radius * 0.045f, Offset(cx, cy))
}
