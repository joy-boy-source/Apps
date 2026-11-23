package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.QuizAttempt
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.MascotCheekCoral
import com.example.ui.theme.MascotYellow
import com.example.ui.theme.MascotYellowDeep
import com.example.ui.theme.OnWarmSurface
import com.example.ui.theme.ParentMintBar
import com.example.ui.theme.QuizzyTheme
import com.example.ui.theme.TealTertiary
import com.example.ui.theme.VioletAiAccent
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun subjectPastelSurface(subject: String): Color {
    val colors = QuizzyTheme.colors
    return when (subject) {
        "Français" -> colors.pastelMint
        "Mathématiques" -> colors.pastelPeach
        "Sciences" -> colors.pastelLavender
        "Histoire-Géo" -> colors.pastelYellow
        "Anglais" -> colors.pastelSky
        "Physique-Chimie" -> colors.pastelSky
        "SVT" -> colors.pastelMint
        else -> colors.pastelYellow
    }
}

fun subjectShortLabel(subject: String): String = when (subject) {
    "Mathématiques" -> "Maths"
    "Histoire-Géo" -> "Hist.-Géo"
    "Physique-Chimie" -> "Physique"
    else -> subject
}

/**
 * Animated Quizzy explorer fennec fox mascot (backward-compatible entry point).
 */
@Composable
fun QuizzyStarMascot(
    modifier: Modifier = Modifier,
    size: Dp = 88.dp,
    mood: QuizzyMascotMood = QuizzyMascotMood.IDLE,
    interactive: Boolean = true
) {
    QuizzyAnimatedMascot(
        mood = mood,
        size = size,
        modifier = modifier,
        interactive = interactive
    )
}

/**
 * Warm peach speech bubble paired next to the star mascot.
 */
@Composable
fun MascotSpeechBubble(
    text: String,
    modifier: Modifier = Modifier
) {
    val bubbleColor = QuizzyTheme.colors.speechBubble
    val inkColor = QuizzyTheme.colors.bodyInk
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(width = 10.dp, height = 16.dp)) {
            val tail = Path().apply {
                moveTo(size.width, 0f)
                lineTo(0f, size.height * 0.65f)
                lineTo(size.width, size.height)
                close()
            }
            drawPath(tail, color = bubbleColor)
        }
        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(22.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = inkColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
    }
}

/**
 * Friendly illustrated checklist clipboard + yellow pencil for the "Quiz du jour" card.
 */
@Composable
fun QuizClipboardIllustration(
    modifier: Modifier = Modifier,
    size: Dp = 62.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Clipboard board
        rotate(degrees = -8f, pivot = Offset(w * 0.44f, h * 0.52f)) {
            drawRoundRect(
                color = Color(0xFF8395E8),
                topLeft = Offset(w * 0.14f, h * 0.16f),
                size = Size(w * 0.58f, h * 0.72f),
                cornerRadius = CornerRadius(w * 0.10f, w * 0.10f)
            )
            // White paper sheet
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(w * 0.19f, h * 0.22f),
                size = Size(w * 0.48f, h * 0.60f),
                cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
            )
            // Top clip
            drawRoundRect(
                color = TealTertiary,
                topLeft = Offset(w * 0.31f, h * 0.10f),
                size = Size(w * 0.24f, h * 0.12f),
                cornerRadius = CornerRadius(w * 0.05f, w * 0.05f)
            )
            // Checkmarks + lines
            val rowYs = listOf(0.36f, 0.52f, 0.68f)
            rowYs.forEach { fracY ->
                val y = h * fracY
                drawCircle(
                    color = Color(0xFF0E7480),
                    radius = w * 0.036f,
                    center = Offset(w * 0.29f, y)
                )
                drawLine(
                    color = TealTertiary.copy(alpha = 0.75f),
                    start = Offset(w * 0.37f, y),
                    end = Offset(w * 0.59f, y),
                    strokeWidth = w * 0.045f,
                    cap = StrokeCap.Round
                )
            }
        }

        // Yellow pencil tilted on bottom-right
        rotate(degrees = 32f, pivot = Offset(w * 0.72f, h * 0.56f)) {
            drawRoundRect(
                color = MascotYellow,
                topLeft = Offset(w * 0.64f, h * 0.30f),
                size = Size(w * 0.15f, h * 0.44f),
                cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
            )
            // Eraser cap
            drawRoundRect(
                color = CoralAccent,
                topLeft = Offset(w * 0.64f, h * 0.24f),
                size = Size(w * 0.15f, h * 0.10f),
                cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
            )
            // Tip
            val tip = Path().apply {
                moveTo(w * 0.64f, h * 0.74f)
                lineTo(w * 0.79f, h * 0.74f)
                lineTo(w * 0.715f, h * 0.88f)
                close()
            }
            drawPath(tip, color = Color(0xFFF3CFA5))
            val lead = Path().apply {
                moveTo(w * 0.69f, h * 0.83f)
                lineTo(w * 0.74f, h * 0.83f)
                lineTo(w * 0.715f, h * 0.88f)
                close()
            }
            drawPath(lead, color = OnWarmSurface)
        }
    }
}

/**
 * Circular progress gauge matching the pastel subject cards ("75%", "60%", "50%").
 */
@Composable
fun SubjectCircularRing(
    percent: Int,
    hasData: Boolean,
    ringColor: Color,
    size: Dp = 56.dp,
    modifier: Modifier = Modifier
) {
    val animatedSweep by animateFloatAsState(
        targetValue = if (hasData) (percent.coerceIn(0, 100) / 100f) * 360f else 0f,
        animationSpec = tween(durationMillis = 650),
        label = "subject_ring"
    )
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 5.5.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(
                color = ringColor.copy(alpha = 0.20f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            if (animatedSweep > 0f) {
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = animatedSweep,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }
        Text(
            text = if (hasData) "$percent%" else "0%",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
            color = QuizzyTheme.colors.bodyInk
        )
    }
}

/**
 * Large pastel rounded card for the child's "Mes matières" grid/row.
 */
@Composable
fun SubjectPastelCard(
    subject: String,
    percent: Int,
    hasData: Boolean,
    lessonCount: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pastelBg = subjectPastelSurface(subject)
    val accent = subjectColor(subject)
    Surface(
        onClick = onClick,
        modifier = modifier
            .width(114.dp)
            .testTag("subject_pastel_card_$subject"),
        shape = RoundedCornerShape(24.dp),
        color = pastelBg,
        border = if (selected) BorderStroke(2.dp, accent) else BorderStroke(1.dp, accent.copy(alpha = 0.24f)),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = subjectIcon(subject),
                contentDescription = subject,
                tint = accent,
                modifier = Modifier.size(30.dp)
            )
            Text(
                text = subjectShortLabel(subject),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = QuizzyTheme.colors.bodyInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            SubjectCircularRing(
                percent = percent,
                hasData = hasData,
                ringColor = accent,
                size = 54.dp
            )
            Text(
                text = "$lessonCount cours",
                style = MaterialTheme.typography.labelSmall,
                color = QuizzyTheme.colors.secondaryInk
            )
        }
    }
}

/**
 * Soft illustrated banner for the Lesson Synthesis screen.
 */
@Composable
fun LessonHeroIllustrationCard(
    subject: String,
    gradeLabel: String,
    modifier: Modifier = Modifier
) {
    val accent = subjectColor(subject)
    val pastel = subjectPastelSurface(subject)
    val qColors = QuizzyTheme.colors
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = pastel,
        border = BorderStroke(1.dp, accent.copy(alpha = if (qColors.isDark) 0.30f else 0.14f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(116.dp)
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(
                    color = if (qColors.isDark) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.55f),
                    radius = size.height * 0.72f,
                    center = Offset(size.width * 0.86f, size.height * 0.20f)
                )
                drawCircle(
                    color = accent.copy(alpha = 0.14f),
                    radius = size.height * 0.55f,
                    center = Offset(size.width * 0.12f, size.height * 0.90f)
                )
                // Friendly playful accent strokes
                drawLine(
                    color = accent.copy(alpha = 0.55f),
                    start = Offset(size.width * 0.72f, size.height * 0.28f),
                    end = Offset(size.width * 0.76f, size.height * 0.22f),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = MascotYellowDeep,
                    start = Offset(size.width * 0.75f, size.height * 0.44f),
                    end = Offset(size.width * 0.80f, size.height * 0.43f),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = qColors.chipOnTint,
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.22f)),
                    shadowElevation = 0.dp,
                    modifier = Modifier.size(68.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = subjectIcon(subject),
                            contentDescription = subject,
                            tint = accent,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        color = qColors.chipOnTint,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.22f))
                    ) {
                        Text(
                            text = "Fiche de synthèse",
                            style = MaterialTheme.typography.labelMedium,
                            color = accent,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = gradeLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = qColors.bodyInk,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                QuizzyStarMascot(size = 74.dp, mood = QuizzyMascotMood.THINKING)
            }
        }
    }
}

/**
 * Celebratory trophy + stars emblem for the Quiz Result screen.
 */
@Composable
fun TrophySuccessIllustration(
    modifier: Modifier = Modifier,
    size: Dp = 88.dp
) {
    val haloColor = QuizzyTheme.colors.pastelYellow
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = haloColor,
                radius = this.size.minDimension * 0.46f
            )
            // Soft sparkle dots around the badge
            drawCircle(
                color = ParentMintBar,
                radius = 4.dp.toPx(),
                center = Offset(this.size.width * 0.14f, this.size.height * 0.24f)
            )
            drawCircle(
                color = CoralAccent,
                radius = 3.5.dp.toPx(),
                center = Offset(this.size.width * 0.88f, this.size.height * 0.26f)
            )
            drawCircle(
                color = MascotYellowDeep,
                radius = 3.dp.toPx(),
                center = Offset(this.size.width * 0.84f, this.size.height * 0.78f)
            )
            drawCircle(
                color = VioletAiAccent,
                radius = 3.dp.toPx(),
                center = Offset(this.size.width * 0.16f, this.size.height * 0.76f)
            )
        }
        Surface(
            shape = CircleShape,
            color = MascotYellow,
            shadowElevation = 0.dp,
            modifier = Modifier.size(size * 0.68f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.EmojiEvents,
                    contentDescription = "Badge de réussite",
                    tint = TealTertiary,
                    modifier = Modifier.size(size * 0.38f)
                )
            }
        }
    }
}

/**
 * Clean 5-day (Lun–Ven) weekly bar chart with 0%-100% labels matching the Parent Dashboard mockup.
 */
@Composable
fun WeeklyProgressBarChart(
    attempts: List<QuizAttempt>,
    modifier: Modifier = Modifier
) {
    val dayLabels = remember { listOf("Lun", "Mar", "Mer", "Jeu", "Ven") }
    val dayValues = remember(attempts) {
        // Compute average score per weekday (Mon..Fri) from actual attempts; if weekend attempts exist, fold into Fri
        val buckets = MutableList(5) { mutableListOf<Int>() }
        attempts.forEach { attempt ->
            val date = attempt.createdAt?.toDate()
            val idx = if (date != null) {
                val cal = Calendar.getInstance().apply { time = date }
                when (cal.get(Calendar.DAY_OF_WEEK)) {
                    Calendar.MONDAY -> 0
                    Calendar.TUESDAY -> 1
                    Calendar.WEDNESDAY -> 2
                    Calendar.THURSDAY -> 3
                    else -> 4
                }
            } else {
                // Keep attempts without server timestamp visible in latest slot
                4
            }
            buckets[idx].add(attempt.percentage.toInt().coerceIn(0, 100))
        }
        buckets.map { list ->
            if (list.isEmpty()) 0 else (list.sum() / list.size)
        }
    }
    val completedGoalDays = remember(dayValues) { dayValues.count { it > 0 } }
    val yAxisLabels = remember { listOf("100 %", "75 %", "50 %", "25 %", "0 %") }

    val qColors = QuizzyTheme.colors
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("parent_weekly_chart_card"),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Réussites de la semaine",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = qColors.headingInk,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(qColors.pastelMint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.EmojiEvents,
                            contentDescription = null,
                            tint = qColors.successInk,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "$completedGoalDays/5",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = qColors.successInk
                        )
                        Text(
                            text = "jours actifs",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Y-axis legend
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(bottom = 22.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    yAxisLabels.forEach { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))

                // 5 Bars
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    dayLabels.forEachIndexed { idx, dayLabel ->
                        val pct = dayValues[idx]
                        val animatedFrac by animateFloatAsState(
                            targetValue = (pct / 100f).coerceIn(0.06f, 1f),
                            animationSpec = tween(600),
                            label = "bar_$idx"
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                // Subtle empty background column
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                                        .background(qColors.parentSkyAccent.copy(alpha = if (qColors.isDark) 0.14f else 0.08f))
                                )
                                // Filled mint-turquoise bar
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(if (pct > 0) animatedFrac else 0.08f)
                                        .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                                        .background(
                                            if (pct > 0) ParentMintBar
                                            else qColors.parentSkyAccent.copy(alpha = if (qColors.isDark) 0.25f else 0.16f)
                                        )
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = dayLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = qColors.bodyInk
                            )
                        }
                    }
                }
            }
        }
    }
}
