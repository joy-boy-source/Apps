package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.R
import com.example.model.QuizAnswerEvaluator
import com.example.model.QuizMatchPair
import com.example.model.QuizQuestion
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.QuizzyTheme
import kotlin.math.roundToInt

@DrawableRes
fun quizIllustrationDrawableForKey(key: String): Int = when (key.lowercase()) {
    QuizQuestion.ILLUS_MATH -> R.drawable.img_quiz_math_1795193516713
    QuizQuestion.ILLUS_ENGLISH -> R.drawable.img_quiz_english_1795193515993
    QuizQuestion.ILLUS_SCIENCE -> R.drawable.img_quiz_science_1795193516958
    QuizQuestion.ILLUS_HISTORY -> R.drawable.img_quiz_history_1795193516775
    else -> R.drawable.img_quiz_french_1795193516715
}

fun quizQuestionActivityMeta(question: QuizQuestion): Pair<String, ImageVector> = when {
    question.isImageChoice -> "Choisis la bonne image" to Icons.Filled.Image
    question.isDragMatch -> "Glisser-Déposer • Associer" to Icons.Filled.Extension
    question.isDragOrder -> "Glisser-Déposer • Ordre" to Icons.Filled.FormatListNumbered
    question.isFillBlank -> "Mot à saisir" to Icons.Filled.Edit
    else -> "Défi rapide" to Icons.Filled.TouchApp
}

/**
 * Mature photorealistic editorial visual banner shown on every quiz question so each
 * activity feels crisp, clear, and unmistakably recognizable.
 */
@Composable
fun QuizQuestionIllustrationCard(
    question: QuizQuestion,
    subject: String,
    modifier: Modifier = Modifier
) {
    val resolvedKey = remember(question.illustrationKey, subject, question.prompt) {
        QuizAnswerEvaluator.resolveIllustrationKey(
            explicitKey = question.illustrationKey,
            subject = subject,
            prompt = question.prompt
        )
    }
    val drawableRes = remember(resolvedKey) { quizIllustrationDrawableForKey(resolvedKey) }
    val (activityLabel, activityIcon) = remember(question) { quizQuestionActivityMeta(question) }
    val captionText = remember(question.illustrationCaption, resolvedKey) {
        question.illustrationCaption.trim().ifBlank {
            when (resolvedKey) {
                QuizQuestion.ILLUS_MATH -> "Observe, analyse et résous ce défi mathématique."
                QuizQuestion.ILLUS_ENGLISH -> "Prêt(e) à relever ce défi d'anglais ?"
                QuizQuestion.ILLUS_SCIENCE -> "Observe attentivement comme un vrai scientifique."
                QuizQuestion.ILLUS_HISTORY -> "Explore les repères historiques et géographiques."
                else -> "Analyse chaque détail et chaque nuance du texte."
            }
        }
    }

    val qColors = QuizzyTheme.colors
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(156.dp)
            .testTag("quiz_illustration_card"),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 0.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = drawableRes),
                contentDescription = "Illustration de la question : $captionText",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("quiz_illustration_image")
            )

            // Smooth editorial dark scrim at bottom for high-contrast legible caption
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(74.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xCC0A1833))
                        )
                    )
            )

            // Top-end interactive activity pill
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shadowElevation = 0.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = activityIcon,
                        contentDescription = null,
                        tint = if (question.isDragMatch || question.isDragOrder || question.isImageChoice) {
                            qColors.coralInk
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = activityLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = qColors.headingInk
                    )
                }
            }

            // Bottom contextual caption
            Text(
                text = captionText,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
    }
}

/**
 * Interactive word-typing / fill-in-the-blank card with live preview inside the sentence.
 */
@Composable
fun InteractiveFillBlankArea(
    sentenceTemplate: String,
    textAnswer: String,
    onTextChange: (String) -> Unit,
    isQuestionResolved: Boolean,
    isQuestionSolved: Boolean,
    onSubmitDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val qColors = QuizzyTheme.colors
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (sentenceTemplate.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = qColors.pastelMint,
                border = BorderStroke(
                    width = if (isQuestionSolved) 2.dp else 1.dp,
                    color = if (isQuestionSolved) qColors.successInk else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quiz_sentence_template_card")
            ) {
                val parts = sentenceTemplate.split("____")
                val liveFill = textAnswer.trim().ifBlank { "_______" }
                val styledSentence = buildAnnotatedString {
                    parts.forEachIndexed { idx, part ->
                        append(part)
                        if (idx < parts.lastIndex) {
                            withStyle(
                                SpanStyle(
                                    color = if (isQuestionSolved) qColors.successInk else MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.ExtraBold,
                                    background = if (textAnswer.isNotBlank()) {
                                        qColors.chipOnTint
                                    } else {
                                        Color.Transparent
                                    }
                                )
                            ) {
                                append(" $liveFill ")
                            }
                        }
                    }
                }
                Text(
                    text = styledSentence,
                    style = MaterialTheme.typography.titleMedium,
                    color = qColors.bodyInk,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        OutlinedTextField(
            value = textAnswer,
            onValueChange = { if (!isQuestionResolved) onTextChange(it) },
            enabled = !isQuestionResolved,
            label = { Text("Écris ta réponse") },
            placeholder = { Text("Tape le mot ou la réponse à compléter...") },
            singleLine = true,
            trailingIcon = {
                if (textAnswer.isNotEmpty() && !isQuestionResolved) {
                    IconButton(onClick = { onTextChange("") }) {
                        Icon(Icons.Filled.Close, contentDescription = "Effacer la saisie")
                    }
                }
            },
            shape = RoundedCornerShape(18.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmitDone() }),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("quiz_fill_blank_input")
        )
    }
}

/**
 * Interactive Drag-and-Drop Matching mini-game!
 * Supports BOTH real finger drag-and-drop (with floating ghost tile & live slot highlight)
 * AND tap-to-select / tap-to-drop for effortless accessibility.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DragMatchMiniGame(
    pairs: List<QuizMatchPair>,
    shuffledTargets: List<String>,
    assignments: Map<Int, String>,
    isLocked: Boolean,
    isSolved: Boolean,
    hasCheckedWrong: Boolean,
    onAssign: (slotIdx: Int, value: String) -> Unit,
    onRemove: (slotIdx: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var containerRootOrigin by remember { mutableStateOf(Offset.Zero) }
    val slotBounds = remember { mutableStateMapOf<Int, Rect>() }

    var draggingItem by remember { mutableStateOf<String?>(null) }
    var dragFingerInContainer by remember { mutableStateOf(Offset.Zero) }
    var hoveredSlotIndex by remember { mutableStateOf<Int?>(null) }
    var selectedBankItem by remember(assignments, isLocked) { mutableStateOf<String?>(null) }

    val placedValues = assignments.values.toList()
    // Handle possible duplicate labels safely by counting occurrences
    val remainingBankItems = remember(shuffledTargets, placedValues) {
        val mutablePlaced = placedValues.toMutableList()
        shuffledTargets.filter { candidate ->
            val idx = mutablePlaced.indexOf(candidate)
            if (idx >= 0) {
                mutablePlaced.removeAt(idx)
                false
            } else {
                true
            }
        }
    }

    fun findSlotForRootPoint(rootPt: Offset): Int? {
        return slotBounds.entries.firstOrNull { (_, rect) ->
            rect.inflate(22f).contains(rootPt)
        }?.key
    }

    val qColors = QuizzyTheme.colors

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
                containerRootOrigin = coords.positionInRoot()
            }
            .testTag("drag_match_container")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
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
                        imageVector = Icons.Filled.Extension,
                        contentDescription = null,
                        tint = qColors.coralInk,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (selectedBankItem != null) {
                            "Touche maintenant la case où placer « $selectedBankItem »"
                        } else {
                            "Glisse chaque étiquette dans la bonne case (ou touche-la puis touche la case)."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = qColors.headingInk
                    )
                }
            }

            // Target Pair Rows
            pairs.forEachIndexed { idx, pair ->
                val placed = assignments[idx]
                val isHovered = hoveredSlotIndex == idx
                val slotScale by animateFloatAsState(
                    targetValue = if (isHovered) 1.03f else 1f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "match_slot_scale_$idx"
                )
                val slotBorderColor by animateColorAsState(
                    targetValue = when {
                        isSolved -> qColors.successInk
                        isHovered -> qColors.coralInk
                        hasCheckedWrong && placed != null -> qColors.warningInk
                        placed != null -> MaterialTheme.colorScheme.primary
                        selectedBankItem != null -> qColors.coralInk.copy(alpha = 0.7f)
                        else -> MaterialTheme.colorScheme.outlineVariant
                    },
                    label = "match_slot_border_$idx"
                )
                val slotBgColor = when {
                    isSolved -> qColors.pastelMint
                    isHovered -> qColors.pastelPeach
                    placed != null -> qColors.pastelMint
                    else -> MaterialTheme.colorScheme.surface
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(slotScale),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Fixed Left Anchor Card
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shadowElevation = 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 56.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.CenterStart,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = pair.left,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = qColors.headingInk
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = if (isSolved) qColors.successInk else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )

                    // Right Drop Slot
                    Surface(
                        onClick = {
                            val picked = selectedBankItem
                            if (picked != null) {
                                onAssign(idx, picked)
                                selectedBankItem = null
                            } else if (placed != null) {
                                onRemove(idx)
                            }
                        },
                        enabled = !isLocked,
                        shape = RoundedCornerShape(18.dp),
                        color = slotBgColor,
                        border = BorderStroke(
                            width = if (isHovered || isSolved || placed != null) 2.dp else 1.5.dp,
                            color = slotBorderColor
                        ),
                        modifier = Modifier
                            .weight(1.1f)
                            .heightIn(min = 56.dp)
                            .onGloballyPositioned { coords ->
                                slotBounds[idx] = coords.boundsInRoot()
                            }
                            .testTag("drag_match_slot_$idx")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = placed ?: if (isHovered) "Dépose ici !" else "Déposer ici",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (placed != null) FontWeight.Bold else FontWeight.Medium,
                                color = if (placed != null) qColors.bodyInk else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSolved) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Associé",
                                    tint = qColors.successInk,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else if (placed != null && !isLocked) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Retirer l'étiquette",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // Draggable Bank of Labels
            Text(
                text = if (remainingBankItems.isEmpty()) {
                    "Toutes les étiquettes sont placées ! Touche une case si tu veux changer."
                } else {
                    "Étiquettes à glisser-déposer :"
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = qColors.headingInk
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                remainingBankItems.forEachIndexed { idx, item ->
                    val isSelected = selectedBankItem == item
                    val isBeingDragged = draggingItem == item
                    var chipRootOrigin by remember { mutableStateOf(Offset.Zero) }
                    val chipContentColor = when {
                        isBeingDragged -> MaterialTheme.colorScheme.onSurfaceVariant
                        isSelected -> Color.White
                        else -> MaterialTheme.colorScheme.onPrimary
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = when {
                            isBeingDragged -> MaterialTheme.colorScheme.surfaceVariant
                            isSelected -> CoralAccent
                            else -> MaterialTheme.colorScheme.primary
                        },
                        shadowElevation = 0.dp,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .onGloballyPositioned { coords ->
                                chipRootOrigin = coords.positionInRoot()
                            }
                            .pointerInput(item, isLocked) {
                                if (isLocked) return@pointerInput
                                var totalDrag = Offset.Zero
                                var startOffset = Offset.Zero
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        selectedBankItem = null
                                        draggingItem = item
                                        startOffset = offset
                                        totalDrag = Offset.Zero
                                        val rootPt = chipRootOrigin + startOffset
                                        dragFingerInContainer = rootPt - containerRootOrigin
                                        hoveredSlotIndex = findSlotForRootPoint(rootPt)
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        totalDrag += dragAmount
                                        val rootPt = chipRootOrigin + startOffset + totalDrag
                                        dragFingerInContainer = rootPt - containerRootOrigin
                                        hoveredSlotIndex = findSlotForRootPoint(rootPt)
                                    },
                                    onDragEnd = {
                                        val dropIdx = hoveredSlotIndex
                                        if (dropIdx != null) {
                                            onAssign(dropIdx, item)
                                        }
                                        draggingItem = null
                                        hoveredSlotIndex = null
                                    },
                                    onDragCancel = {
                                        draggingItem = null
                                        hoveredSlotIndex = null
                                    }
                                )
                            }
                            .clickable(enabled = !isLocked) {
                                selectedBankItem = if (isSelected) null else item
                            }
                            .testTag("drag_match_item_$idx")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DragIndicator,
                                contentDescription = null,
                                tint = chipContentColor.copy(alpha = 0.85f),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = item,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = chipContentColor
                            )
                        }
                    }
                }
            }
        }

        // Floating dragged ghost tile
        val activeGhost = draggingItem
        if (activeGhost != null) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = CoralAccent,
                shadowElevation = 10.dp,
                modifier = Modifier
                    .zIndex(20f)
                    .offset {
                        IntOffset(
                            x = (dragFingerInContainer.x - 80f).roundToInt(),
                            y = (dragFingerInContainer.y - 34f).roundToInt()
                        )
                    }
                    .scale(1.06f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.DragIndicator,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = activeGhost,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Interactive Drag-and-Drop Ordering mini-game!
 * Kids drag shuffled tiles into numbered positions 1..N (or tap to place & use arrows to reorder).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DragOrderMiniGame(
    shuffledBank: List<String>,
    slots: List<String?>,
    isLocked: Boolean,
    isSolved: Boolean,
    hasCheckedWrong: Boolean,
    onPlaceAt: (slotIdx: Int, item: String) -> Unit,
    onPlaceNextAvailable: (item: String) -> Unit,
    onRemoveAt: (slotIdx: Int) -> Unit,
    onSwap: (fromIdx: Int, toIdx: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var containerRootOrigin by remember { mutableStateOf(Offset.Zero) }
    val slotBounds = remember { mutableStateMapOf<Int, Rect>() }

    var draggingItem by remember { mutableStateOf<String?>(null) }
    var dragFingerInContainer by remember { mutableStateOf(Offset.Zero) }
    var hoveredSlotIndex by remember { mutableStateOf<Int?>(null) }

    val remainingBank = remember(shuffledBank, slots) {
        val placed = slots.filterNotNull().toMutableList()
        shuffledBank.filter { candidate ->
            val idx = placed.indexOf(candidate)
            if (idx >= 0) {
                placed.removeAt(idx)
                false
            } else {
                true
            }
        }
    }

    fun findSlotForRootPoint(rootPt: Offset): Int? {
        return slotBounds.entries.firstOrNull { (_, rect) ->
            rect.inflate(22f).contains(rootPt)
        }?.key
    }

    val qColors = QuizzyTheme.colors

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
                containerRootOrigin = coords.positionInRoot()
            }
            .testTag("drag_order_container")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                        imageVector = Icons.Filled.FormatListNumbered,
                        contentDescription = null,
                        tint = qColors.coralInk,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Glisse les étiquettes dans le bon ordre du 1er au dernier (ou touche-les).",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = qColors.headingInk
                    )
                }
            }

            // Numbered Target Slots
            slots.forEachIndexed { idx, placedItem ->
                val isHovered = hoveredSlotIndex == idx
                val borderColor by animateColorAsState(
                    targetValue = when {
                        isSolved -> qColors.successInk
                        isHovered -> qColors.coralInk
                        hasCheckedWrong && placedItem != null -> qColors.warningInk
                        placedItem != null -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.outlineVariant
                    },
                    label = "order_slot_border_$idx"
                )
                val bgColor = when {
                    isSolved -> qColors.pastelMint
                    isHovered -> qColors.pastelPeach
                    placedItem != null -> qColors.pastelMint
                    else -> MaterialTheme.colorScheme.surface
                }

                Surface(
                    onClick = { onRemoveAt(idx) },
                    enabled = !isLocked && placedItem != null,
                    shape = RoundedCornerShape(18.dp),
                    color = bgColor,
                    border = BorderStroke(if (isHovered || placedItem != null || isSolved) 2.dp else 1.5.dp, borderColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .onGloballyPositioned { coords ->
                            slotBounds[idx] = coords.boundsInRoot()
                        }
                        .testTag("drag_order_slot_$idx")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isSolved -> qColors.successInk
                                        placedItem != null -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${idx + 1}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = when {
                                    isSolved -> if (qColors.isDark) Color(0xFF04261C) else Color.White
                                    placedItem != null -> MaterialTheme.colorScheme.onPrimary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }

                        Text(
                            text = placedItem ?: if (isHovered) "Dépose en position ${idx + 1} !" else "Étape / élément n°${idx + 1}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (placedItem != null) FontWeight.Bold else FontWeight.Medium,
                            color = if (placedItem != null) qColors.bodyInk else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )

                        if (isSolved) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Bien placé",
                                tint = qColors.successInk
                            )
                        } else if (placedItem != null && !isLocked) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (idx > 0) {
                                    IconButton(
                                        onClick = { onSwap(idx, idx - 1) },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .testTag("drag_order_up_$idx")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.ArrowUpward,
                                            contentDescription = "Monter",
                                            tint = qColors.headingInk,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                if (idx < slots.lastIndex) {
                                    IconButton(
                                        onClick = { onSwap(idx, idx + 1) },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .testTag("drag_order_down_$idx")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.ArrowDownward,
                                            contentDescription = "Descendre",
                                            tint = qColors.headingInk,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = if (remainingBank.isEmpty()) {
                    "Tout est placé ! Tu peux inverser avec les flèches ou toucher une ligne pour la retirer."
                } else {
                    "Étiquettes mélangées à remettre dans l'ordre :"
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = qColors.headingInk
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                remainingBank.forEachIndexed { idx, item ->
                    val isBeingDragged = draggingItem == item
                    var chipRootOrigin by remember { mutableStateOf(Offset.Zero) }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isBeingDragged) {
                            MaterialTheme.colorScheme.surfaceVariant
                        } else {
                            qColors.navyActionContainer
                        },
                        shadowElevation = 0.dp,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .onGloballyPositioned { coords ->
                                chipRootOrigin = coords.positionInRoot()
                            }
                            .pointerInput(item, isLocked) {
                                if (isLocked) return@pointerInput
                                var totalDrag = Offset.Zero
                                var startOffset = Offset.Zero
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        draggingItem = item
                                        startOffset = offset
                                        totalDrag = Offset.Zero
                                        val rootPt = chipRootOrigin + startOffset
                                        dragFingerInContainer = rootPt - containerRootOrigin
                                        hoveredSlotIndex = findSlotForRootPoint(rootPt)
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        totalDrag += dragAmount
                                        val rootPt = chipRootOrigin + startOffset + totalDrag
                                        dragFingerInContainer = rootPt - containerRootOrigin
                                        hoveredSlotIndex = findSlotForRootPoint(rootPt)
                                    },
                                    onDragEnd = {
                                        val dropIdx = hoveredSlotIndex
                                        if (dropIdx != null) {
                                            onPlaceAt(dropIdx, item)
                                        }
                                        draggingItem = null
                                        hoveredSlotIndex = null
                                    },
                                    onDragCancel = {
                                        draggingItem = null
                                        hoveredSlotIndex = null
                                    }
                                )
                            }
                            .clickable(enabled = !isLocked) {
                                onPlaceNextAvailable(item)
                            }
                            .testTag("drag_order_bank_$idx")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DragIndicator,
                                contentDescription = null,
                                tint = if (isBeingDragged) MaterialTheme.colorScheme.onSurfaceVariant else qColors.navyActionOnContainer.copy(alpha = 0.85f),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = item,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isBeingDragged) MaterialTheme.colorScheme.onSurfaceVariant else qColors.navyActionOnContainer
                            )
                        }
                    }
                }
            }
        }

        val activeGhost = draggingItem
        if (activeGhost != null) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = CoralAccent,
                shadowElevation = 10.dp,
                modifier = Modifier
                    .zIndex(20f)
                    .offset {
                        IntOffset(
                            x = (dragFingerInContainer.x - 80f).roundToInt(),
                            y = (dragFingerInContainer.y - 34f).roundToInt()
                        )
                    }
                    .scale(1.06f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.DragIndicator,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = activeGhost,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
