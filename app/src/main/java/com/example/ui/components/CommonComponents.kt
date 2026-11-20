package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.data.VoiceRecorder
import com.example.model.AttachedDocument
import com.example.model.ChildProfile
import com.example.model.GamificationCatalog
import com.example.model.QuizBadge
import com.example.model.SchoolCatalog
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.CoralError
import com.example.ui.theme.HeroDeepSapphire
import com.example.ui.theme.HeroEmeraldGlow
import com.example.ui.theme.HeroRoyalIndigo
import com.example.ui.theme.QuizzyTheme
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.VioletAiAccent
import com.example.ui.theme.WarningAmber

fun avatarIcon(key: String): ImageVector = when (key) {
    "owl" -> Icons.Filled.School
    "astronaut" -> Icons.Filled.Explore
    "fox" -> Icons.Filled.Pets
    "rocket" -> Icons.Filled.RocketLaunch
    "palette" -> Icons.Filled.ColorLens
    else -> Icons.Filled.Face
}

fun avatarColor(key: String): Color = when (key) {
    "owl" -> Color(0xFF0E7480)
    "astronaut" -> Color(0xFF2374E1)
    "fox" -> Color(0xFFF06A4B)
    "rocket" -> Color(0xFF6E59D9)
    "palette" -> Color(0xFFE8A417)
    else -> Color(0xFF0E7480)
}

fun subjectIcon(subject: String): ImageVector = when (subject) {
    "Mathématiques" -> Icons.Filled.Calculate
    "Français" -> Icons.Filled.Book
    "Histoire-Géo" -> Icons.Filled.Public
    "Sciences" -> Icons.Filled.Science
    "Anglais" -> Icons.Filled.Language
    "Physique-Chimie" -> Icons.Filled.AutoAwesome
    "SVT" -> Icons.Filled.Biotech
    else -> Icons.Filled.School
}

@Composable
fun subjectColor(subject: String): Color {
    val isDark = QuizzyTheme.colors.isDark
    return when (subject) {
        "Mathématiques" -> if (isDark) Color(0xFFFF8C73) else Color(0xFFF06A4B)
        "Français" -> if (isDark) Color(0xFF4BE0D3) else Color(0xFF0E7480)
        "Histoire-Géo" -> if (isDark) Color(0xFFFFCB57) else Color(0xFFD98A16)
        "Sciences" -> if (isDark) Color(0xFFA79BFF) else Color(0xFF5A4FCF)
        "Anglais" -> if (isDark) Color(0xFF69AEFF) else Color(0xFF2374E1)
        "Physique-Chimie" -> if (isDark) Color(0xFF4BD4EE) else Color(0xFF0B849E)
        "SVT" -> if (isDark) Color(0xFF52E5B7) else Color(0xFF1C9E75)
        else -> if (isDark) Color(0xFF93B4FF) else Color(0xFF1D2C6E)
    }
}

@Composable
fun scoreColor(percent: Int): Color = when {
    percent >= 75 -> QuizzyTheme.colors.successInk
    percent >= 50 -> QuizzyTheme.colors.warningInk
    else -> QuizzyTheme.colors.coralInk
}

@Composable
fun ModernCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            color = containerColor,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            border = BorderStroke(1.dp, borderColor)
        ) {
            Column(content = content)
        }
    } else {
        Surface(
            modifier = modifier,
            shape = shape,
            color = containerColor,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            border = BorderStroke(1.dp, borderColor)
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun ChildAvatarBadge(
    avatarKey: String,
    name: String,
    size: Dp = 56.dp,
    modifier: Modifier = Modifier
) {
    val bg = avatarColor(avatarKey)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bg.copy(alpha = 0.14f))
            .padding(3.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(bg, bg.copy(alpha = 0.72f))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = avatarIcon(avatarKey),
            contentDescription = "Avatar de $name",
            tint = Color.White,
            modifier = Modifier.size(size * 0.48f)
        )
    }
}

@Composable
fun SubjectBadge(
    subject: String,
    modifier: Modifier = Modifier
) {
    val color = subjectColor(subject)
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f)),
        shape = RoundedCornerShape(50)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = subjectIcon(subject),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = subject,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SuccessRateGauge(
    percent: Int,
    hasAttempts: Boolean,
    size: Dp = 72.dp,
    modifier: Modifier = Modifier
) {
    val animatedSweep by animateFloatAsState(
        targetValue = if (hasAttempts) (percent / 100f) * 360f else 0f,
        animationSpec = tween(durationMillis = 750),
        label = "gauge_sweep"
    )
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val activeColor = if (hasAttempts) scoreColor(percent) else MaterialTheme.colorScheme.outline

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = (size.toPx() * 0.11f).coerceIn(5.dp.toPx(), 10.dp.toPx())
            val inset = strokeWidth / 2f
            val arcSize = Size(this.size.width - strokeWidth, this.size.height - strokeWidth)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            if (animatedSweep > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(activeColor.copy(alpha = 0.7f), activeColor, activeColor)
                    ),
                    startAngle = -90f,
                    sweepAngle = animatedSweep,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }
        Text(
            text = if (hasAttempts) "$percent%" else "--",
            style = if (size >= 90.dp) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (hasAttempts) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GradeLevelSelector(
    selectedGrade: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        SchoolCatalog.gradeLevels.forEach { grade ->
            val selected = grade == selectedGrade
            FilterChip(
                selected = selected,
                onClick = { onSelect(grade) },
                label = { Text(grade, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.testTag("grade_chip_$grade")
            )
        }
    }
}

@Composable
fun PinPadDialog(
    title: String,
    subtitle: String,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onPinComplete: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("pin_close_button")
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Fermer")
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(20.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    repeat(4) { index ->
                        val filled = index < pin.length
                        val dotColor by animateColorAsState(
                            targetValue = if (filled) MaterialTheme.colorScheme.primary else Color.Transparent,
                            label = "pin_dot"
                        )
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                                .border(
                                    width = 2.dp,
                                    color = if (filled) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                )
                        )
                    }
                }

                if (!errorMessage.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(Modifier.height(22.dp))

                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("C", "0", "<")
                )
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    rows.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                            row.forEach { key ->
                                Surface(
                                    onClick = {
                                        when (key) {
                                            "C" -> pin = ""
                                            "<" -> if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                            else -> {
                                                if (pin.length < 4) {
                                                    val next = pin + key
                                                    pin = next
                                                    if (next.length == 4) {
                                                        onPinComplete(next)
                                                        pin = ""
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .size(62.dp)
                                        .minimumInteractiveComponentSize()
                                        .testTag("pin_key_$key"),
                                    shape = CircleShape,
                                    color = if (key == "C" || key == "<")
                                        MaterialTheme.colorScheme.surface
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (key == "<") {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                                contentDescription = "Effacer",
                                                tint = MaterialTheme.colorScheme.onSurface
                                            )
                                        } else {
                                            Text(
                                                text = key,
                                                style = MaterialTheme.typography.titleLarge,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudyHeroBanner(
    title: String,
    subtitle: String,
    badgeLabel: String? = null,
    icon: ImageVector = Icons.Filled.AutoAwesome,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        HeroDeepSapphire,
                        HeroRoyalIndigo,
                        HeroEmeraldGlow
                    )
                )
            )
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                color = Color(0xFFFBBF24).copy(alpha = 0.20f),
                radius = size.height * 0.72f,
                center = Offset(size.width * 0.94f, size.height * 0.12f)
            )
            drawCircle(
                color = Color(0xFF38BDF8).copy(alpha = 0.15f),
                radius = size.height * 0.55f,
                center = Offset(size.width * 0.08f, size.height * 0.96f)
            )
            // Subtle architectural lines
            drawLine(
                color = Color.White.copy(alpha = 0.08f),
                start = Offset(0f, size.height * 0.35f),
                end = Offset(size.width, size.height * 0.12f),
                strokeWidth = 1.5.dp.toPx()
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (!badgeLabel.isNullOrBlank()) {
                    Surface(
                        color = Color.White.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = badgeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFDE68A),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.90f)
                )
            }
            Spacer(Modifier.width(14.dp))
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.16f))
                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFFFDE68A),
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}

@Composable
fun ShimmerLoadingCard(
    message: String,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shift by transition.animateFloat(
        initialValue = -300f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_shift"
    )
    val baseColor = MaterialTheme.colorScheme.surfaceVariant
    val highlightColor = MaterialTheme.colorScheme.surface

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(baseColor, highlightColor, baseColor),
                        start = Offset(shift, 0f),
                        end = Offset(shift + 380f, 380f)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            QuizzyBookLoader(
                size = 86.dp,
                message = message,
                modifier = Modifier.padding(24.dp)
            )
        }
    }
}

@Composable
fun VoiceMicButton(
    isTranscribing: Boolean,
    enabled: Boolean = true,
    onAudioCaptured: (ByteArray) -> Unit,
    onPermissionDenied: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val recorder = remember(context) { VoiceRecorder(context) }
    var isRecording by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            recorder.cancel()
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            if (recorder.startRecording()) {
                isRecording = true
            } else {
                onPermissionDenied("Impossible de démarrer le micro sur cet appareil.")
            }
        } else {
            onPermissionDenied("Autorise le microphone pour dicter ta question à l'assistant.")
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(550),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_scale"
    )

    val containerColor = when {
        isRecording -> CoralError
        isTranscribing -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val iconTint = when {
        isRecording -> Color.White
        isTranscribing -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.primary
    }

    Surface(
        modifier = modifier
            .size(48.dp)
            .scale(if (isRecording) pulseScale else 1f)
            .clip(CircleShape)
            .minimumInteractiveComponentSize()
            .clickable(enabled = enabled && !isTranscribing) {
                if (isRecording) {
                    isRecording = false
                    val bytes = recorder.stopAndReadBytes()
                    if (bytes != null && bytes.isNotEmpty()) {
                        onAudioCaptured(bytes)
                    } else {
                        onPermissionDenied("Enregistrement trop court, réessaie en parlant un peu plus longtemps.")
                    }
                } else {
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    if (granted) {
                        if (recorder.startRecording()) {
                            isRecording = true
                        } else {
                            onPermissionDenied("Impossible d'accéder au microphone.")
                        }
                    } else {
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            },
        shape = CircleShape,
        color = containerColor,
        border = BorderStroke(1.dp, if (isRecording) CoralError else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isTranscribing) {
                QuizzyInlineLoader(
                    size = 20.dp,
                    tint = iconTint
                )
            } else {
                Icon(
                    imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                    contentDescription = if (isRecording) "Arrêter la dictée vocale" else "Dicter avec le micro",
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

fun badgeIcon(badgeId: String): ImageVector = when (badgeId) {
    "first_quiz" -> Icons.Filled.Flag
    "combo_3" -> Icons.Filled.LocalFireDepartment
    "perfect_100" -> Icons.Filled.WorkspacePremium
    "challenger" -> Icons.Filled.Bolt
    "quiz_5" -> Icons.Filled.Repeat
    "streak_5" -> Icons.Filled.Whatshot
    "scholar_300" -> Icons.Filled.AutoAwesome
    else -> Icons.Filled.EmojiEvents
}

@Composable
fun badgeAccentColor(badgeId: String): Color {
    val isDark = QuizzyTheme.colors.isDark
    return when (badgeId) {
        "first_quiz" -> if (isDark) Color(0xFF4BE0D3) else Color(0xFF0B9488)
        "combo_3" -> QuizzyTheme.colors.coralInk
        "perfect_100" -> QuizzyTheme.colors.warningInk
        "challenger" -> QuizzyTheme.colors.violetInk
        "quiz_5" -> if (isDark) Color(0xFF69AEFF) else Color(0xFF2347D4)
        "streak_5" -> MaterialTheme.colorScheme.error
        "scholar_300" -> QuizzyTheme.colors.successInk
        else -> if (isDark) Color(0xFF69AEFF) else Color(0xFF2347D4)
    }
}

@Composable
fun difficultyColor(difficulty: String): Color = when (difficulty) {
    "Défi" -> QuizzyTheme.colors.violetInk
    "Moyen" -> QuizzyTheme.colors.coralInk
    else -> QuizzyTheme.colors.successInk
}

@Composable
fun DifficultyMultiplierChip(
    difficulty: String,
    modifier: Modifier = Modifier
) {
    val normalized = SchoolCatalog.normalizeDifficulty(difficulty)
    val color = difficultyColor(normalized)
    val display = SchoolCatalog.difficultyDisplayLabel(normalized)
    val mult = GamificationCatalog.difficultyMultiplier(normalized)
    val multText = if (mult == 1.0f) "×1 XP" else "×$mult XP"
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.28f)),
        shape = RoundedCornerShape(50)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Bolt,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = "$display • $multText",
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun XpLevelProgressCard(
    child: ChildProfile,
    modifier: Modifier = Modifier
) {
    val progress = remember(child.xp) { GamificationCatalog.levelProgress(child.xp) }
    val animatedFraction by animateFloatAsState(
        targetValue = progress.progressFraction,
        animationSpec = tween(durationMillis = 700),
        label = "xp_level_progress"
    )
    val remainingXp = (progress.nextLevelXp - progress.totalXp).coerceAtLeast(0L)

    ModernCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("xp_level_progress_card"),
        cornerRadius = 22.dp,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(HeroDeepSapphire, HeroRoyalIndigo)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "N${progress.level}",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Column {
                        Text(
                            text = "Niveau ${progress.level} • ${progress.title}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${progress.totalXp} XP cumulés",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (child.bestStreak > 0L) {
                    val streakColor = QuizzyTheme.colors.coralInk
                    Surface(
                        color = streakColor.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, streakColor.copy(alpha = 0.30f)),
                        shape = RoundedCornerShape(50)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocalFireDepartment,
                                contentDescription = "Meilleure série",
                                tint = streakColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Record ${child.bestStreak}",
                                style = MaterialTheme.typography.labelMedium,
                                color = streakColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            LinearProgressIndicator(
                progress = { animatedFraction },
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${progress.levelStartXp} XP",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Encore $remainingXp XP pour le niveau ${progress.level + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${progress.nextLevelXp} XP",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun BadgeShowcaseRow(
    unlockedIds: List<String>,
    modifier: Modifier = Modifier
) {
    val sortedBadges = remember(unlockedIds) {
        val unlockedSet = unlockedIds.toSet()
        GamificationCatalog.allBadges.sortedByDescending { it.id in unlockedSet }
    }
    val unlockedCount = remember(unlockedIds) {
        GamificationCatalog.allBadges.count { it.id in unlockedIds }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("badge_showcase_row"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Badges & Trophées",
                style = MaterialTheme.typography.titleLarge
            )
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = "$unlockedCount / ${GamificationCatalog.allBadges.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(sortedBadges, key = { it.id }) { badge ->
                val isUnlocked = badge.id in unlockedIds
                BadgeMiniCard(badge = badge, isUnlocked = isUnlocked)
            }
        }
    }
}

@Composable
fun BadgeMiniCard(
    badge: QuizBadge,
    isUnlocked: Boolean,
    modifier: Modifier = Modifier
) {
    val accent = badgeAccentColor(badge.id)
    Surface(
        modifier = modifier
            .width(152.dp)
            .testTag("badge_card_${badge.id}"),
        shape = RoundedCornerShape(18.dp),
        color = if (isUnlocked) accent.copy(alpha = 0.12f)
        else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            width = if (isUnlocked) 1.5.dp else 1.dp,
            color = if (isUnlocked) accent.copy(alpha = 0.42f)
            else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (isUnlocked) accent
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = badgeIcon(badge.id),
                        contentDescription = badge.title,
                        tint = if (isUnlocked) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Icon(
                    imageVector = if (isUnlocked) Icons.Filled.CheckCircle else Icons.Filled.Lock,
                    contentDescription = if (isUnlocked) "Débloqué" else "Verrouillé",
                    tint = if (isUnlocked) accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = badge.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isUnlocked) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = badge.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
}

fun formatFileSize(sizeBytes: Long): String = when {
    sizeBytes <= 0L -> ""
    sizeBytes < 1024L -> "$sizeBytes o"
    sizeBytes < 1024L * 1024L -> "${(sizeBytes / 1024L).coerceAtLeast(1L)} Ko"
    else -> String.format("%.1f Mo", sizeBytes / (1024.0 * 1024.0))
}

fun documentFormatLabel(fileName: String, mimeType: String): String {
    val ext = fileName.substringAfterLast('.', "").uppercase()
    if (ext.isNotBlank() && ext.length <= 5) return ext
    return when {
        mimeType.contains("pdf") -> "PDF"
        mimeType.startsWith("image/") -> "IMG"
        mimeType.startsWith("text/") -> "TXT"
        else -> "DOC"
    }
}

fun documentIcon(fileName: String, mimeType: String): ImageVector {
    val ext = fileName.substringAfterLast('.', "").lowercase()
    return when {
        mimeType == "application/pdf" || ext == "pdf" -> Icons.Filled.PictureAsPdf
        mimeType.startsWith("image/") || ext in setOf("jpg", "jpeg", "png", "webp", "bmp", "heic") ->
            Icons.Filled.Image
        else -> Icons.Filled.Description
    }
}

@Composable
fun AttachedDocumentChipRow(
    fileName: String,
    mimeType: String,
    sizeBytes: Long,
    onRemove: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val format = documentFormatLabel(fileName, mimeType)
    val sizeLabel = formatFileSize(sizeBytes)
    val accent = if (format == "PDF") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = accent.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.24f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = documentIcon(fileName, mimeType),
                    contentDescription = format,
                    tint = accent,
                    modifier = Modifier.size(19.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = if (sizeLabel.isNotBlank()) "$format • $sizeLabel" else format,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (onRemove != null) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Retirer le fichier",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AttachedDocumentsCard(
    documents: List<AttachedDocument>,
    modifier: Modifier = Modifier
) {
    if (documents.isEmpty()) return
    ModernCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Documents déposés (${documents.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            documents.forEach { doc ->
                AttachedDocumentChipRow(
                    fileName = doc.fileName,
                    mimeType = doc.mimeType,
                    sizeBytes = doc.sizeBytes
                )
            }
        }
    }
}
