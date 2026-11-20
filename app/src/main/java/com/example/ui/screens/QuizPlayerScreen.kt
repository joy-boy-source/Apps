package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.model.GamificationCatalog
import com.example.model.MissedQuestionRecord
import com.example.model.Quiz
import com.example.model.QuizAnswerEvaluator
import com.example.model.QuizFeedbackHelper
import com.example.model.WeeklyReviewScheduler
import com.example.ui.components.BadgeMiniCard
import com.example.ui.components.DifficultyMultiplierChip
import com.example.ui.components.DragMatchMiniGame
import com.example.ui.components.DragOrderMiniGame
import com.example.ui.components.ImageChoiceMiniGame
import com.example.ui.components.InteractiveFillBlankArea
import com.example.ui.components.ModernCard
import com.example.ui.components.QuizQuestionIllustrationCard
import com.example.ui.components.SubjectBadge
import com.example.ui.components.SuccessRateGauge
import com.example.ui.components.TrophySuccessIllustration
import com.example.ui.components.quizQuestionActivityMeta
import com.example.ui.theme.QuizzyTheme
import kotlinx.coroutines.delay

private data class ReviewPointItem(
    val skillLabel: String,
    val prompt: String,
    val guidingHint: String
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuizPlayerScreen(
    quiz: Quiz,
    lessonTitle: String = "",
    childPreviousXp: Long = 0L,
    childExistingBadges: List<String> = emptyList(),
    childQuizzesTaken: Long = 0L,
    onBack: () -> Unit,
    onQuizCompleted: (
        score: Int,
        total: Int,
        maxStreak: Int,
        masteredSkills: List<String>,
        weakSkills: List<String>
    ) -> Unit = { _, _, _, _, _ -> },
    onQuizCompletedWithXp: ((
        score: Int,
        total: Int,
        maxStreak: Int,
        masteredSkills: List<String>,
        weakSkills: List<String>,
        earnedXp: Long
    ) -> Unit)? = null,
    onQuizCompletedFull: ((
        score: Int,
        total: Int,
        maxStreak: Int,
        masteredSkills: List<String>,
        weakSkills: List<String>,
        earnedXp: Long,
        missedQuestions: List<MissedQuestionRecord>
    ) -> Unit)? = null
) {
    BackHandler(onBack = onBack)

    val questions = quiz.questions
    var attemptKey by remember(quiz.id) { mutableIntStateOf(0) }
    var completedInSession by remember(quiz.id) { mutableStateOf(false) }
    val isReplayAttempt = remember(quiz.id, attemptKey) {
        quiz.isAlreadyCompleted || completedInSession
    }
    val startXp = remember(quiz.id, attemptKey) { childPreviousXp }
    val startBadges = remember(quiz.id, attemptKey) { childExistingBadges }
    val startQuizzesTaken = remember(quiz.id, attemptKey) { childQuizzesTaken }

    var currentIndex by remember(quiz.id, attemptKey) { mutableIntStateOf(0) }
    var selectedOption by remember(quiz.id, attemptKey) { mutableStateOf<Int?>(null) }
    var triedWrongOptions by remember(quiz.id, attemptKey) { mutableStateOf<Set<Int>>(emptySet()) }
    var textAnswer by remember(quiz.id, attemptKey) { mutableStateOf("") }
    var wrongAttemptsOnQuestion by remember(quiz.id, attemptKey) { mutableIntStateOf(0) }
    var isQuestionSolved by remember(quiz.id, attemptKey) { mutableStateOf(false) }
    var wasSkipped by remember(quiz.id, attemptKey) { mutableStateOf(false) }

    var correctCount by remember(quiz.id, attemptKey) { mutableIntStateOf(0) }
    var currentStreak by remember(quiz.id, attemptKey) { mutableIntStateOf(0) }
    var maxStreak by remember(quiz.id, attemptKey) { mutableIntStateOf(0) }
    var liveXp by remember(quiz.id, attemptKey) { mutableIntStateOf(0) }
    var lastQuestionXp by remember(quiz.id, attemptKey) { mutableIntStateOf(0) }
    var hadStreakBeforeMiss by remember(quiz.id, attemptKey) { mutableStateOf(false) }
    var isFinished by remember(quiz.id, attemptKey) { mutableStateOf(false) }

    val masteredPrompts = remember(quiz.id, attemptKey) { mutableStateListOf<String>() }
    val masteredSkills = remember(quiz.id, attemptKey) { mutableStateListOf<String>() }
    val weakSkills = remember(quiz.id, attemptKey) { mutableStateListOf<String>() }
    val reviewItems = remember(quiz.id, attemptKey) { mutableStateListOf<ReviewPointItem>() }
    val missedQuestionRecords = remember(quiz.id, attemptKey) { mutableStateListOf<MissedQuestionRecord>() }

    val streakScale by animateFloatAsState(
        targetValue = if (currentStreak > 0) 1f else 0.85f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "streak_bounce"
    )

    val qColors = QuizzyTheme.colors

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        if (questions.isEmpty()) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Ce quiz ne contient aucune question.", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))
                Button(onClick = onBack, shape = RoundedCornerShape(50)) { Text("Retour") }
            }
            return@Box
        }

        if (isFinished) {
            val total = questions.size
            val pct = ((correctCount * 100) / total).coerceIn(0, 100)
            val earnedStars = GamificationCatalog.starsForPercentage(pct)
            val earnedXp = if (isReplayAttempt) 0L else liveXp.toLong().coerceAtLeast(0L)
            val updatedTotalXp = startXp + earnedXp
            val levelAfter = GamificationCatalog.levelProgress(updatedTotalXp)
            val updatedBadges = if (isReplayAttempt) {
                startBadges
            } else {
                GamificationCatalog.computeUnlockedBadges(
                    existingBadges = startBadges,
                    newQuizzesTaken = startQuizzesTaken + 1L,
                    newTotalXp = updatedTotalXp,
                    percentage = pct,
                    maxStreak = maxStreak,
                    difficulty = quiz.difficulty
                )
            }
            val newlyUnlocked = remember(quiz.id, attemptKey, updatedBadges, startBadges) {
                updatedBadges.filterNot { it in startBadges }
                    .mapNotNull { GamificationCatalog.badgeById(it) }
            }

            var revealedStars by remember { mutableIntStateOf(0) }
            LaunchedEffect(isFinished) {
                revealedStars = 0
                for (i in 1..earnedStars) {
                    delay(180L)
                    revealedStars = i
                }
            }

            val animatedXpFraction by animateFloatAsState(
                targetValue = levelAfter.progressFraction,
                animationSpec = tween(durationMillis = 800),
                label = "finish_xp_bar"
            )

            val cheerMessage = when {
                pct == 100 -> "Score parfait du premier coup ! Tu maîtrises chaque notion !"
                pct >= 80 -> "Super travail ! Tu connais très bien ce cours."
                pct >= 60 -> "Joli progrès ! Encore un petit tour sur les notions à revoir et ce sera parfait."
                pct > 0 -> "Bon entraînement ! Chercher par toi-même t'aide à mieux retenir."
                else -> "Pas d'inquiétude, on apprend en cherchant ! Relis tranquillement la synthèse."
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
                    .widthIn(max = 560.dp)
                    .align(Alignment.TopCenter),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ModernCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 28.dp) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        TrophySuccessIllustration(size = 86.dp)

                        Spacer(Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.testTag("quiz_finish_stars")
                        ) {
                            for (starIdx in 1..3) {
                                val lit = starIdx <= revealedStars
                                val starScale by animateFloatAsState(
                                    targetValue = if (lit) 1.15f else 0.9f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    ),
                                    label = "star_$starIdx"
                                )
                                Icon(
                                    imageVector = if (lit) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                    contentDescription = if (lit) "Étoile gagnée" else "Étoile à gagner",
                                    tint = if (lit) qColors.warningInk else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .scale(starScale)
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Quiz terminé !",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = qColors.headingInk
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = quiz.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                        SuccessRateGauge(percent = pct, hasAttempts = true, size = 104.dp)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "$correctCount / $total réussies du 1er coup",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = qColors.bodyInk,
                            textAlign = TextAlign.Center
                        )
                        if (missedQuestionRecords.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Surface(
                                color = qColors.pastelPeach,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, qColors.coralInk.copy(alpha = 0.30f)),
                                modifier = Modifier.testTag("quiz_result_missed_count_notice")
                            ) {
                                Text(
                                    text = "${missedQuestionRecords.size} mauvaise${if (missedQuestionRecords.size > 1) "s" else ""} réponse${if (missedQuestionRecords.size > 1) "s" else ""} comptabilisée${if (missedQuestionRecords.size > 1) "s" else ""} (1er essai) • Ajoutée${if (missedQuestionRecords.size > 1) "s" else ""} à ta révision du dimanche 14h",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = qColors.coralInk,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = cheerMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(18.dp))

                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = qColors.pastelMint,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("quiz_xp_summary_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isReplayAttempt) Icons.Filled.Refresh else Icons.Filled.Bolt,
                                            contentDescription = null,
                                            tint = if (isReplayAttempt) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = when {
                                                isReplayAttempt -> "0 XP • Quiz déjà fait"
                                                earnedXp > 0L -> "+$earnedXp XP gagnés"
                                                else -> "0 XP gagnés • Bel entraînement !"
                                            },
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (isReplayAttempt || earnedXp == 0L) qColors.bodyInk else MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.testTag(
                                                if (isReplayAttempt) "quiz_replay_no_xp_label" else "quiz_earned_xp_label"
                                            )
                                        )
                                    }
                                    Surface(
                                        color = qColors.pastelPeach,
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.LocalFireDepartment,
                                                contentDescription = null,
                                                tint = qColors.coralInk,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Text(
                                                text = "Série max : $maxStreak",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = qColors.coralInk,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                if (isReplayAttempt) {
                                    Text(
                                        text = "Mode entraînement : tu ne gagnes plus d'XP en refaisant le même quiz. Fais un nouveau quiz pour gagner des points !",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.testTag("quiz_replay_no_xp_message")
                                    )
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Niveau ${levelAfter.level} • ${levelAfter.title}",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = qColors.bodyInk
                                        )
                                        Text(
                                            text = "${levelAfter.totalXp} / ${levelAfter.nextLevelXp} XP",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { animatedXpFraction },
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = qColors.chipOnTint,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(10.dp)
                                            .clip(RoundedCornerShape(50))
                                    )
                                }
                            }
                        }

                        if (newlyUnlocked.isNotEmpty()) {
                            Spacer(Modifier.height(16.dp))
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = if (newlyUnlocked.size == 1) "Nouveau badge débloqué !"
                                    else "Nouveaux badges débloqués !",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = qColors.warningInk,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(newlyUnlocked, key = { it.id }) { badge ->
                                        BadgeMiniCard(badge = badge, isUnlocked = true)
                                    }
                                }
                            }
                        }
                    }
                }

                // Distinct Card 1: Points maîtrisés
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quiz_result_mastered_card"),
                    shape = RoundedCornerShape(24.dp),
                    color = qColors.pastelMint,
                    border = BorderStroke(1.dp, qColors.successInk.copy(alpha = 0.28f))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = qColors.successInk
                            )
                            Text(
                                text = "Points maîtrisés (${masteredPrompts.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = qColors.bodyInk
                            )
                        }
                        if (masteredPrompts.isEmpty()) {
                            Text(
                                text = "Tu es prêt(e) à découvrir ces notions dans la synthèse !",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            val displayList = if (masteredSkills.isNotEmpty()) masteredSkills.distinct() else masteredPrompts
                            displayList.forEach { itemText ->
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("•", color = qColors.successInk, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = itemText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = qColors.bodyInk
                                    )
                                }
                            }
                        }
                    }
                }

                // Distinct Card 2: Points à revoir (NEVER reveals the answer, only guiding tips!)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quiz_result_review_card"),
                    shape = RoundedCornerShape(24.dp),
                    color = qColors.pastelYellow,
                    border = BorderStroke(1.dp, qColors.warningInk.copy(alpha = 0.30f))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lightbulb,
                                contentDescription = null,
                                tint = qColors.warningInk
                            )
                            Text(
                                text = "À revoir tranquillement (${reviewItems.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = qColors.bodyInk
                            )
                        }
                        if (reviewItems.isEmpty()) {
                            Text(
                                text = "Aucune hésitation sur ce quiz, tout est bien compris !",
                                style = MaterialTheme.typography.bodyMedium,
                                color = qColors.bodyInk
                            )
                        } else {
                            reviewItems.forEach { item ->
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(
                                        text = item.skillLabel,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = qColors.headingInk
                                    )
                                    Text(
                                        text = item.prompt,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = qColors.bodyInk
                                    )
                                    Text(
                                        text = "Piste pour réviser : ${item.guidingHint}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("quiz_finish_back_button"),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = qColors.navyActionContainer,
                        contentColor = qColors.navyActionOnContainer
                    )
                ) {
                    Text(
                        text = "Revoir la synthèse",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                OutlinedButton(
                    onClick = { attemptKey += 1 },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("quiz_replay_button"),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Recommencer (sans XP)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(Modifier.height(10.dp))
            }
        } else {
            val q = questions[currentIndex]
            val isQuestionResolved = isQuestionSolved || wasSkipped
            val skillLabel = q.skillTag.ifBlank { q.prompt.take(70) }
            var dragMatchAssignments by remember(quiz.id, attemptKey, currentIndex) {
                mutableStateOf<Map<Int, String>>(emptyMap())
            }
            var dragOrderSlots by remember(quiz.id, attemptKey, currentIndex) {
                mutableStateOf<List<String?>>(List(q.orderedItems.size) { null })
            }
            val shuffledMatchTargets = remember(quiz.id, attemptKey, currentIndex, q) {
                QuizAnswerEvaluator.shuffledDeterministically(
                    items = q.matchingPairs.map { it.right },
                    seed = currentIndex + attemptKey + 17
                )
            }
            val shuffledOrderBank = remember(quiz.id, attemptKey, currentIndex, q) {
                QuizAnswerEvaluator.shuffledDeterministically(
                    items = q.orderedItems,
                    seed = currentIndex + attemptKey + 41
                )
            }
            val hint1 = remember(currentIndex, q) {
                QuizFeedbackHelper.safeGuidingHint(
                    rawHint = q.guidingHint,
                    forbiddenAnswers = q.forbiddenAnswersForHints,
                    questionIndex = currentIndex,
                    isSecondAttempt = false
                )
            }
            val hint2 = remember(currentIndex, q, hint1) {
                val candidate = QuizFeedbackHelper.safeGuidingHint(
                    rawHint = q.secondHint,
                    forbiddenAnswers = q.forbiddenAnswersForHints,
                    questionIndex = currentIndex,
                    isSecondAttempt = true
                )
                if (candidate.equals(hint1, ignoreCase = true)) {
                    QuizFeedbackHelper.safeGuidingHint(
                        rawHint = "",
                        forbiddenAnswers = q.forbiddenAnswersForHints,
                        questionIndex = currentIndex + 1,
                        isSecondAttempt = true
                    )
                } else {
                    candidate
                }
            }
            val nextTryPotentialXp = GamificationCatalog.questionXpForAttempt(
                difficulty = quiz.difficulty,
                wrongAttemptsBeforeSolve = wrongAttemptsOnQuestion,
                isReplay = isReplayAttempt
            )

            fun validateCurrentAnswer() {
                if (isQuestionResolved) return
                val isCorrect = when {
                    q.isImageChoice -> {
                        val picked = selectedOption ?: return
                        picked == q.correctIndex
                    }
                    q.isDragMatch -> {
                        if (dragMatchAssignments.size < q.matchingPairs.size) return
                        QuizAnswerEvaluator.matchesDragPairs(q.matchingPairs, dragMatchAssignments)
                    }
                    q.isDragOrder -> {
                        if (dragOrderSlots.size != q.orderedItems.size || dragOrderSlots.any { it == null }) return
                        QuizAnswerEvaluator.matchesDragOrder(q.orderedItems, dragOrderSlots)
                    }
                    q.isFillBlank -> {
                        if (textAnswer.isBlank()) return
                        QuizAnswerEvaluator.matchesFillBlank(textAnswer, q.acceptedAnswers)
                    }
                    else -> {
                        val picked = selectedOption ?: return
                        picked == q.correctIndex
                    }
                }

                if (isCorrect) {
                    isQuestionSolved = true
                    hadStreakBeforeMiss = false
                    val gained = GamificationCatalog.questionXpForAttempt(
                        difficulty = quiz.difficulty,
                        wrongAttemptsBeforeSolve = wrongAttemptsOnQuestion,
                        isReplay = isReplayAttempt
                    )
                    liveXp += gained
                    lastQuestionXp = gained
                    if (wrongAttemptsOnQuestion == 0) {
                        correctCount += 1
                        currentStreak += 1
                        if (currentStreak > maxStreak) {
                            maxStreak = currentStreak
                        }
                        masteredPrompts.add(q.prompt)
                        if (skillLabel.isNotBlank() && skillLabel !in masteredSkills) {
                            masteredSkills.add(skillLabel)
                        }
                    }
                } else {
                    if (wrongAttemptsOnQuestion == 0) {
                        hadStreakBeforeMiss = currentStreak > 0
                        currentStreak = 0
                        lastQuestionXp = 0
                        if (skillLabel.isNotBlank() && skillLabel !in weakSkills) {
                            weakSkills.add(skillLabel)
                        }
                        reviewItems.add(
                            ReviewPointItem(
                                skillLabel = skillLabel,
                                prompt = q.prompt,
                                guidingHint = hint1
                            )
                        )
                        // ALWAYS record a 1st-try wrong answer as missed for the Sunday 14h review quiz,
                        // even if the child later finds the right answer on a retry!
                        missedQuestionRecords.add(
                            MissedQuestionRecord(
                                subject = quiz.subject,
                                lessonId = quiz.lessonId,
                                lessonTitle = lessonTitle.ifBlank { quiz.title },
                                quizId = quiz.id,
                                skillTag = skillLabel,
                                prompt = q.prompt,
                                weekKey = WeeklyReviewScheduler.currentWeekKey()
                            )
                        )
                    }
                    wrongAttemptsOnQuestion += 1
                    if (q.isImageChoice || (!q.isFillBlank && !q.isDragMatch && !q.isDragOrder)) {
                        selectedOption?.let { wrongIdx ->
                            triedWrongOptions = triedWrongOptions + wrongIdx
                        }
                        selectedOption = null
                    }
                }
            }

            fun moveToNextQuestion() {
                if (currentIndex < questions.lastIndex) {
                    currentIndex += 1
                    selectedOption = null
                    triedWrongOptions = emptySet()
                    textAnswer = ""
                    wrongAttemptsOnQuestion = 0
                    isQuestionSolved = false
                    wasSkipped = false
                    hadStreakBeforeMiss = false
                } else {
                    val finalEarnedXp = if (isReplayAttempt) 0L else liveXp.toLong().coerceAtLeast(0L)
                    completedInSession = true
                    isFinished = true
                    val distinctMastered = masteredSkills.distinct()
                    val distinctWeak = weakSkills.distinct()
                    val missedList = missedQuestionRecords.toList()
                    onQuizCompleted(
                        correctCount,
                        questions.size,
                        maxStreak,
                        distinctMastered,
                        distinctWeak
                    )
                    onQuizCompletedWithXp?.invoke(
                        correctCount,
                        questions.size,
                        maxStreak,
                        distinctMastered,
                        distinctWeak,
                        finalEarnedXp
                    )
                    onQuizCompletedFull?.invoke(
                        correctCount,
                        questions.size,
                        maxStreak,
                        distinctMastered,
                        distinctWeak,
                        finalEarnedXp,
                        missedList
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
                    .widthIn(max = 600.dp)
                    .align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shadowElevation = 0.dp
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("quiz_back_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Quitter le quiz",
                                tint = qColors.headingInk
                            )
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        SubjectBadge(subject = quiz.subject)
                    }

                    Surface(
                        color = qColors.pastelMint,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                    ) {
                        Text(
                            text = "Question ${currentIndex + 1} sur ${questions.size}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                LinearProgressIndicator(
                    progress = { (currentIndex + 1).toFloat() / questions.size.toFloat() },
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(50))
                )

                Spacer(Modifier.height(12.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DifficultyMultiplierChip(difficulty = quiz.difficulty)

                    val (activityLabel, activityIcon) = quizQuestionActivityMeta(q)
                    Surface(
                        color = when {
                            q.isImageChoice || q.isDragMatch || q.isDragOrder -> qColors.pastelPeach
                            q.isFillBlank -> qColors.pastelYellow
                            else -> qColors.pastelMint
                        },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.testTag("quiz_question_type_chip")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = activityIcon,
                                contentDescription = null,
                                tint = if (q.isImageChoice || q.isDragMatch || q.isDragOrder) qColors.coralInk else qColors.headingInk,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = activityLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = qColors.headingInk,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (isReplayAttempt) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("quiz_replay_mode_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Mode entraînement • 0 XP",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Surface(
                            color = qColors.pastelYellow,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("quiz_live_xp_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Bolt,
                                    contentDescription = null,
                                    tint = qColors.warningInk,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "+$liveXp XP",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = qColors.bodyInk,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = currentStreak > 0,
                        enter = scaleIn() + fadeIn()
                    ) {
                        Surface(
                            color = qColors.pastelPeach,
                            border = BorderStroke(1.dp, qColors.coralInk.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .scale(streakScale)
                                .testTag("quiz_streak_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LocalFireDepartment,
                                    contentDescription = "Série en cours",
                                    tint = qColors.coralInk,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "x$currentStreak",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = qColors.coralInk,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Central Illustrated Question Card
                ModernCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 26.dp
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuizQuestionIllustrationCard(
                            question = q,
                            subject = quiz.subject
                        )

                        if (q.skillTag.isNotBlank()) {
                            Surface(
                                color = qColors.pastelMint,
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "Notion : ${q.skillTag}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Text(
                            text = q.prompt,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = qColors.headingInk
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Interactive Answer Area: IMAGE_CHOICE, DRAG_MATCH, DRAG_ORDER, FILL_BLANK, or MCQ
                when {
                    q.isImageChoice -> {
                        ImageChoiceMiniGame(
                            imageOptions = q.imageOptions,
                            selectedIndex = selectedOption,
                            triedWrongIndices = triedWrongOptions,
                            correctIndex = q.correctIndex,
                            isQuestionResolved = isQuestionResolved,
                            isQuestionSolved = isQuestionSolved,
                            onSelect = { idx -> selectedOption = idx }
                        )
                    }
                    q.isDragMatch -> {
                        DragMatchMiniGame(
                            pairs = q.matchingPairs,
                            shuffledTargets = shuffledMatchTargets,
                            assignments = dragMatchAssignments,
                            isLocked = isQuestionResolved,
                            isSolved = isQuestionSolved,
                            hasCheckedWrong = wrongAttemptsOnQuestion > 0 && !isQuestionSolved,
                            onAssign = { slotIdx, value ->
                                val mutable = dragMatchAssignments.toMutableMap()
                                // Remove value from any other slot if already placed there
                                mutable.entries.firstOrNull { it.value == value }?.let {
                                    mutable.remove(it.key)
                                }
                                mutable[slotIdx] = value
                                dragMatchAssignments = mutable
                            },
                            onRemove = { slotIdx ->
                                val mutable = dragMatchAssignments.toMutableMap()
                                mutable.remove(slotIdx)
                                dragMatchAssignments = mutable
                            }
                        )
                    }
                    q.isDragOrder -> {
                        DragOrderMiniGame(
                            shuffledBank = shuffledOrderBank,
                            slots = dragOrderSlots,
                            isLocked = isQuestionResolved,
                            isSolved = isQuestionSolved,
                            hasCheckedWrong = wrongAttemptsOnQuestion > 0 && !isQuestionSolved,
                            onPlaceAt = { slotIdx, item ->
                                if (slotIdx in dragOrderSlots.indices) {
                                    val mutable = dragOrderSlots.toMutableList()
                                    val existingIdx = mutable.indexOf(item)
                                    if (existingIdx >= 0) mutable[existingIdx] = null
                                    mutable[slotIdx] = item
                                    dragOrderSlots = mutable
                                }
                            },
                            onPlaceNextAvailable = { item ->
                                val emptyIdx = dragOrderSlots.indexOfFirst { it == null }
                                if (emptyIdx >= 0) {
                                    val mutable = dragOrderSlots.toMutableList()
                                    mutable[emptyIdx] = item
                                    dragOrderSlots = mutable
                                }
                            },
                            onRemoveAt = { slotIdx ->
                                if (slotIdx in dragOrderSlots.indices) {
                                    val mutable = dragOrderSlots.toMutableList()
                                    mutable[slotIdx] = null
                                    dragOrderSlots = mutable
                                }
                            },
                            onSwap = { fromIdx, toIdx ->
                                if (fromIdx in dragOrderSlots.indices && toIdx in dragOrderSlots.indices) {
                                    val mutable = dragOrderSlots.toMutableList()
                                    val tmp = mutable[fromIdx]
                                    mutable[fromIdx] = mutable[toIdx]
                                    mutable[toIdx] = tmp
                                    dragOrderSlots = mutable
                                }
                            }
                        )
                    }
                    q.isFillBlank -> {
                        InteractiveFillBlankArea(
                            sentenceTemplate = q.sentenceTemplate,
                            textAnswer = textAnswer,
                            onTextChange = { textAnswer = it },
                            isQuestionResolved = isQuestionResolved,
                            isQuestionSolved = isQuestionSolved,
                            onSubmitDone = {
                                if (textAnswer.isNotBlank() && !isQuestionResolved) {
                                    validateCurrentAnswer()
                                }
                            }
                        )
                    }
                    else -> {
                    q.options.forEachIndexed { idx, optionText ->
                        val isSelected = selectedOption == idx
                        val wasTriedWrong = idx in triedWrongOptions
                        val showAsCorrect = isQuestionSolved && idx == q.correctIndex

                        val bgColor = when {
                            showAsCorrect -> qColors.pastelMint
                            wasTriedWrong -> qColors.pastelPeach
                            isSelected -> qColors.pastelMint
                            else -> MaterialTheme.colorScheme.surface
                        }
                        val borderColor = when {
                            showAsCorrect -> qColors.successInk
                            wasTriedWrong -> qColors.coralInk
                            isSelected -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.outlineVariant
                        }

                        Surface(
                            onClick = { selectedOption = idx },
                            enabled = !isQuestionResolved && !wasTriedWrong,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .heightIn(min = 60.dp)
                                .testTag("quiz_option_$idx"),
                            shape = RoundedCornerShape(20.dp),
                            color = bgColor,
                            border = BorderStroke(
                                width = if (isSelected || showAsCorrect || wasTriedWrong) 2.dp else 1.dp,
                                color = borderColor
                            ),
                            shadowElevation = 0.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
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
                                        text = ('A' + idx).toString(),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = when {
                                            showAsCorrect || wasTriedWrong -> if (qColors.isDark) Color(0xFF0D1526) else Color.White
                                            isSelected -> MaterialTheme.colorScheme.onPrimary
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                                Spacer(Modifier.width(14.dp))
                                Text(
                                    text = optionText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected || showAsCorrect) FontWeight.Bold else FontWeight.Medium,
                                    color = if (wasTriedWrong) MaterialTheme.colorScheme.onSurfaceVariant else qColors.bodyInk,
                                    modifier = Modifier.weight(1f)
                                )
                                if (showAsCorrect) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Bonne réponse",
                                        tint = qColors.successInk
                                    )
                                } else if (wasTriedWrong) {
                                    Icon(
                                        imageVector = Icons.Filled.Cancel,
                                        contentDescription = "Déjà essayée",
                                        tint = qColors.coralInk
                                    )
                                }
                            }
                        }
                    }
                    }
                }

                // Benevolent Socratic Guidance on Wrong Attempts (cumulative visible hints, NEVER gives away the answer!)
                AnimatedVisibility(visible = wrongAttemptsOnQuestion > 0 && !isQuestionSolved && !wasSkipped) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = qColors.pastelPeach,
                            border = BorderStroke(1.dp, qColors.coralInk.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("quiz_feedback_banner")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Lightbulb,
                                    contentDescription = null,
                                    tint = qColors.coralInk,
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .size(22.dp)
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = QuizFeedbackHelper.friendlyWrongAnswerMessage(
                                            currentIndex + wrongAttemptsOnQuestion - 1
                                        ),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = qColors.coralInk,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    if (hadStreakBeforeMiss) {
                                        Text(
                                            text = "Ta série repart à zéro, tu vas te rattraper très vite !",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    val xpRetryNotice = when {
                                        isReplayAttempt ->
                                            "Mode entraînement (0 XP) : continue à chercher tranquillement."
                                        wrongAttemptsOnQuestion == 1 ->
                                            "Tu peux réessayer pour gagner la moitié des points (+$nextTryPotentialXp XP)"
                                        wrongAttemptsOnQuestion == 2 ->
                                            "Nouvel indice ! Si tu trouves à cet essai, tu gagnes la moitié des points précédents (+$nextTryPotentialXp XP)"
                                        else ->
                                            "Tu peux continuer à essayer pour apprendre ! Aucun point (0 XP) ne sera attribué à ce niveau."
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (nextTryPotentialXp > 0) qColors.pastelYellow else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.testTag("quiz_next_attempt_xp_pill")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Bolt,
                                                contentDescription = null,
                                                tint = if (nextTryPotentialXp > 0) qColors.warningInk else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Text(
                                                text = xpRetryNotice,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = qColors.bodyInk,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = qColors.pastelYellow,
                            border = BorderStroke(1.dp, qColors.warningInk.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("quiz_guiding_hint_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Indice 1 (always visible from 1st wrong attempt onward)
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.testTag("quiz_guiding_hint_1")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Lightbulb,
                                            contentDescription = null,
                                            tint = qColors.warningInk,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Indice 1 • Piste de réflexion",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = qColors.headingInk,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                    Text(
                                        text = hint1,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = qColors.bodyInk,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                // Indice 2 (visible TOGETHER with Indice 1 from 2nd wrong attempt onward!)
                                if (wrongAttemptsOnQuestion >= 2) {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = qColors.chipOnTint,
                                        border = BorderStroke(1.dp, qColors.warningInk.copy(alpha = 0.35f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("quiz_guiding_hint_2")
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Lightbulb,
                                                    contentDescription = null,
                                                    tint = qColors.coralInk,
                                                    modifier = Modifier.size(17.dp)
                                                )
                                                Text(
                                                    text = "Indice 2 • Autre piste complémentaire",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = qColors.coralInk,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            }
                                            Text(
                                                text = hint2,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = qColors.bodyInk,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = "Cherche encore un petit peu et réessaie : tu vas y arriver !",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Skipped Notice (still NEVER reveals the answer)
                AnimatedVisibility(visible = wasSkipped) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = qColors.pastelYellow,
                        border = BorderStroke(1.dp, qColors.warningInk.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp)
                            .testTag("quiz_skipped_notice_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Noté pour tes prochaines révisions !",
                                style = MaterialTheme.typography.titleSmall,
                                color = qColors.headingInk,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Je ne te donne pas la réponse pour que tu puisses la découvrir en relisant ton cours. Garde cette piste en tête :",
                                style = MaterialTheme.typography.bodySmall,
                                color = qColors.bodyInk
                            )
                            Text(
                                text = if (wrongAttemptsOnQuestion >= 2) "$hint1\n$hint2" else hint1,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Solved Celebration (only shown AFTER the child finds the right answer themselves)
                AnimatedVisibility(visible = isQuestionSolved) {
                    val cleanExplanation = QuizFeedbackHelper.sanitizeNeutralExplanation(q.explanation)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = qColors.pastelMint,
                            border = BorderStroke(1.dp, qColors.successInk.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("quiz_feedback_banner")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (currentStreak >= 2) Icons.Filled.LocalFireDepartment
                                    else Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = qColors.successInk,
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .size(22.dp)
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = when {
                                            isReplayAttempt && wrongAttemptsOnQuestion > 0 ->
                                                "Super persévérance ! Tu as trouvé la bonne réponse par toi-même !"
                                            isReplayAttempt ->
                                                if (currentStreak >= 2) "Bravo, série x$currentStreak ! (Entraînement)"
                                                else "Bravo, bonne réponse !"
                                            wrongAttemptsOnQuestion == 0 ->
                                                if (currentStreak >= 2) "+$lastQuestionXp XP • Bravo, série x$currentStreak !"
                                                else "+$lastQuestionXp XP • Bravo, bonne réponse !"
                                            wrongAttemptsOnQuestion == 1 ->
                                                "+$lastQuestionXp XP (½ points) • Super persévérance ! Tu as trouvé la bonne réponse par toi-même !"
                                            wrongAttemptsOnQuestion == 2 ->
                                                "+$lastQuestionXp XP (¼ points) • Super persévérance ! Tu as trouvé avec les 2 indices !"
                                            else ->
                                                "0 XP à ce niveau • Belle persévérance ! Tu as trouvé la bonne réponse par toi-même !"
                                        },
                                        style = MaterialTheme.typography.titleSmall,
                                        color = qColors.successInk,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    if (wrongAttemptsOnQuestion > 0) {
                                        Text(
                                            text = "Enregistrée comme mauvaise réponse (1er essai) pour ta révision du dimanche 14h.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.testTag("quiz_first_try_miss_recorded_note")
                                        )
                                    }
                                }
                            }
                        }

                        if (cleanExplanation.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = qColors.pastelMint,
                                border = BorderStroke(1.dp, qColors.successInk.copy(alpha = 0.25f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("quiz_correct_explanation_card")
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "À retenir :",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = qColors.headingInk,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = cleanExplanation,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = qColors.bodyInk
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))

                if (!isQuestionResolved) {
                    val canSubmit = when {
                        q.isImageChoice -> selectedOption != null
                        q.isDragMatch -> dragMatchAssignments.size == q.matchingPairs.size
                        q.isDragOrder -> dragOrderSlots.size == q.orderedItems.size && dragOrderSlots.none { it == null }
                        q.isFillBlank -> textAnswer.isNotBlank()
                        else -> selectedOption != null
                    }
                    Button(
                        onClick = { validateCurrentAnswer() },
                        enabled = canSubmit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("quiz_validate_button"),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = qColors.navyActionContainer,
                            contentColor = qColors.navyActionOnContainer
                        )
                    ) {
                        Text(
                            text = if (wrongAttemptsOnQuestion > 0) "Essayer cette réponse" else "Valider ma réponse",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    if (wrongAttemptsOnQuestion >= 1) {
                        Spacer(Modifier.height(8.dp))
                        TextButton(
                            onClick = { wasSkipped = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("quiz_skip_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SkipNext,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Passer cette question pour plus tard",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = { moveToNextQuestion() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("quiz_next_button"),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = if (currentIndex < questions.lastIndex) "Question suivante"
                            else "Voir mon résultat",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
