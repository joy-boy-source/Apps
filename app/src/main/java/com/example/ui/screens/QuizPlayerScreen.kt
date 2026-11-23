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
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
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
import com.example.model.LanguageToneCatalog
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
import com.example.ui.components.QuizResultFeedbackAnimation
import com.example.ui.components.QuizzyAnimatedMascot
import com.example.ui.components.QuizzyMascotMood
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
    childAge: Int = 10,
    childLanguageToneLevel: String = LanguageToneCatalog.AUTO,
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
    var isRemediationPhase by remember(quiz.id, attemptKey) { mutableStateOf(false) }
    var remediationQueue by remember(quiz.id, attemptKey) { mutableStateOf<List<Int>>(emptyList()) }
    var remediationPosition by remember(quiz.id, attemptKey) { mutableIntStateOf(0) }
    var failedOutOnMainPass by remember(quiz.id, attemptKey) { mutableStateOf(false) }

    var selectedOption by remember(quiz.id, attemptKey) { mutableStateOf<Int?>(null) }
    var selectedOptions by remember(quiz.id, attemptKey) { mutableStateOf<Set<Int>>(emptySet()) }
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
    val remediationQuestionIndices = remember(quiz.id, attemptKey) { mutableStateListOf<Int>() }

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
                        QuizzyAnimatedMascot(
                            mood = if (pct >= 60) QuizzyMascotMood.VICTORY else QuizzyMascotMood.ENCOURAGE,
                            size = 118.dp
                        )

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
            val activeQuestionIndex = if (isRemediationPhase) {
                remediationQueue.getOrElse(remediationPosition) { 0 }.coerceIn(0, questions.lastIndex)
            } else {
                currentIndex.coerceIn(0, questions.lastIndex)
            }
            val q = questions[activeQuestionIndex]
            val questionRoundKey = if (isRemediationPhase) "rem_${remediationPosition}_$activeQuestionIndex" else "main_$activeQuestionIndex"
            val isQuestionResolved = isQuestionSolved || wasSkipped || failedOutOnMainPass
            val skillLabel = q.skillTag.ifBlank { q.prompt.take(70) }
            var dragMatchAssignments by remember(quiz.id, attemptKey, questionRoundKey) {
                mutableStateOf<Map<Int, String>>(emptyMap())
            }
            var dragOrderSlots by remember(quiz.id, attemptKey, questionRoundKey) {
                mutableStateOf<List<String?>>(List(q.orderedItems.size) { null })
            }
            val shuffledMatchTargets = remember(quiz.id, attemptKey, questionRoundKey, q) {
                QuizAnswerEvaluator.shuffledDeterministically(
                    items = q.matchingPairs.map { it.right },
                    seed = activeQuestionIndex + attemptKey + (if (isRemediationPhase) 97 else 17)
                )
            }
            val shuffledOrderBank = remember(quiz.id, attemptKey, questionRoundKey, q) {
                QuizAnswerEvaluator.shuffledDeterministically(
                    items = q.orderedItems,
                    seed = activeQuestionIndex + attemptKey + (if (isRemediationPhase) 131 else 41)
                )
            }
            val hint1 = remember(activeQuestionIndex, q) {
                QuizFeedbackHelper.safeGuidingHint(
                    rawHint = q.guidingHint,
                    forbiddenAnswers = q.forbiddenAnswersForHints,
                    questionIndex = activeQuestionIndex,
                    isSecondAttempt = false
                )
            }
            val hint2 = remember(activeQuestionIndex, q, hint1) {
                val candidate = QuizFeedbackHelper.safeGuidingHint(
                    rawHint = q.secondHint,
                    forbiddenAnswers = q.forbiddenAnswersForHints,
                    questionIndex = activeQuestionIndex,
                    isSecondAttempt = true
                )
                if (candidate.equals(hint1, ignoreCase = true)) {
                    QuizFeedbackHelper.safeGuidingHint(
                        rawHint = "",
                        forbiddenAnswers = q.forbiddenAnswersForHints,
                        questionIndex = activeQuestionIndex + 1,
                        isSecondAttempt = true
                    )
                } else {
                    candidate
                }
            }
            val nextTryPotentialXp = if (isRemediationPhase) {
                0
            } else {
                GamificationCatalog.questionXpForAttempt(
                    difficulty = quiz.difficulty,
                    wrongAttemptsBeforeSolve = wrongAttemptsOnQuestion,
                    isReplay = isReplayAttempt
                )
            }

            fun resetPerQuestionState() {
                selectedOption = null
                selectedOptions = emptySet()
                triedWrongOptions = emptySet()
                textAnswer = ""
                wrongAttemptsOnQuestion = 0
                isQuestionSolved = false
                wasSkipped = false
                failedOutOnMainPass = false
                hadStreakBeforeMiss = false
            }

            fun finishQuizAndReport() {
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

            fun validateCurrentAnswer() {
                if (isQuestionResolved) return
                val isCorrect = when {
                    q.isImageChoice -> {
                        if (q.isMultiSelect) {
                            if (selectedOptions.isEmpty()) return
                            selectedOptions == q.effectiveCorrectIndices
                        } else {
                            val picked = selectedOption ?: return
                            picked == q.correctIndex
                        }
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
                        if (q.isMultiSelect) {
                            if (selectedOptions.isEmpty()) return
                            selectedOptions == q.effectiveCorrectIndices
                        } else {
                            val picked = selectedOption ?: return
                            picked == q.correctIndex
                        }
                    }
                }

                if (isCorrect) {
                    isQuestionSolved = true
                    hadStreakBeforeMiss = false
                    if (isRemediationPhase) {
                        // Remediation round awards strictly 0 XP and never alters 1st-try accuracy score
                        lastQuestionXp = 0
                    } else {
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
                    }
                } else {
                    if (!isRemediationPhase && wrongAttemptsOnQuestion == 0) {
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
                    if (!q.isMultiSelect && (q.isImageChoice || (!q.isFillBlank && !q.isDragMatch && !q.isDragOrder))) {
                        selectedOption?.let { wrongIdx ->
                            triedWrongOptions = triedWrongOptions + wrongIdx
                        }
                        selectedOption = null
                    }
                    // Main pass rule: after 2 consecutive errors on a question, lock retries and move on;
                    // the question will be re-asked at the end of the quiz (0 XP, unlimited tries).
                    if (!isRemediationPhase && wrongAttemptsOnQuestion >= 2) {
                        failedOutOnMainPass = true
                        if (activeQuestionIndex !in remediationQuestionIndices) {
                            remediationQuestionIndices.add(activeQuestionIndex)
                        }
                    }
                }
            }

            fun moveToNextQuestion() {
                if (isRemediationPhase) {
                    if (remediationPosition < remediationQueue.lastIndex) {
                        remediationPosition += 1
                        resetPerQuestionState()
                    } else {
                        finishQuizAndReport()
                    }
                } else {
                    if (currentIndex < questions.lastIndex) {
                        currentIndex += 1
                        resetPerQuestionState()
                    } else if (remediationQuestionIndices.isNotEmpty()) {
                        // Enter end-of-quiz remediation round for all missed/failed questions!
                        isRemediationPhase = true
                        remediationQueue = remediationQuestionIndices.toList()
                        remediationPosition = 0
                        resetPerQuestionState()
                    } else {
                        finishQuizAndReport()
                    }
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
                        color = if (isRemediationPhase) qColors.pastelPeach else qColors.pastelMint,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(
                            1.dp,
                            (if (isRemediationPhase) qColors.coralInk else MaterialTheme.colorScheme.primary).copy(alpha = 0.30f)
                        ),
                        modifier = Modifier.testTag(
                            if (isRemediationPhase) "quiz_remediation_progress_pill" else "quiz_question_progress_pill"
                        )
                    ) {
                        Text(
                            text = if (isRemediationPhase) {
                                "Rattrapage ${remediationPosition + 1} sur ${remediationQueue.size}"
                            } else {
                                "Question ${currentIndex + 1} sur ${questions.size}"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isRemediationPhase) qColors.coralInk else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                LinearProgressIndicator(
                    progress = {
                        if (isRemediationPhase) {
                            (remediationPosition + 1).toFloat() / remediationQueue.size.coerceAtLeast(1).toFloat()
                        } else {
                            (currentIndex + 1).toFloat() / questions.size.toFloat()
                        }
                    },
                    color = if (isRemediationPhase) qColors.coralInk else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(50))
                )

                if (isRemediationPhase) {
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        color = qColors.pastelYellow,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, qColors.warningInk.copy(alpha = 0.38f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quiz_remediation_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = null,
                                tint = qColors.warningInk,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Rattrapage de tes erreurs • 0 point • Essais illimités jusqu'à réussir !",
                                style = MaterialTheme.typography.labelMedium,
                                color = qColors.headingInk,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

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

                    if (isRemediationPhase) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("quiz_remediation_zero_xp_pill")
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
                                    text = "Rattrapage • 0 XP",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else if (isReplayAttempt) {
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
                            onSelect = { idx ->
                                if (q.isMultiSelect) {
                                    selectedOptions = if (idx in selectedOptions) {
                                        selectedOptions - idx
                                    } else {
                                        selectedOptions + idx
                                    }
                                } else {
                                    selectedOption = idx
                                }
                            },
                            isMultiSelect = q.isMultiSelect,
                            selectedIndices = selectedOptions,
                            correctIndices = q.effectiveCorrectIndices
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
                    if (q.isMultiSelect) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = qColors.pastelYellow,
                            border = BorderStroke(1.dp, qColors.warningInk.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .testTag("quiz_multi_select_badge")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckBox,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Plusieurs réponses attendues — coche toutes les bonnes propositions",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = qColors.headingInk
                                )
                            }
                        }
                    }
                    q.options.forEachIndexed { idx, optionText ->
                        val isSelected = if (q.isMultiSelect) idx in selectedOptions else selectedOption == idx
                        val wasTriedWrong = !q.isMultiSelect && idx in triedWrongOptions
                        val showAsCorrect = isQuestionSolved && idx in q.effectiveCorrectIndices

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
                            onClick = {
                                if (q.isMultiSelect) {
                                    selectedOptions = if (idx in selectedOptions) {
                                        selectedOptions - idx
                                    } else {
                                        selectedOptions + idx
                                    }
                                } else {
                                    selectedOption = idx
                                }
                            },
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
                                        .clip(if (q.isMultiSelect) RoundedCornerShape(10.dp) else CircleShape)
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
                                } else if (q.isMultiSelect) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                                        contentDescription = if (isSelected) "Option cochée" else "Option non cochée",
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                    }
                }

                // 2-Consecutive-Errors Auto-Advance Notice (main pass only — NEVER reveals the answer!)
                AnimatedVisibility(visible = failedOutOnMainPass && !isQuestionSolved) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = qColors.pastelPeach,
                        border = BorderStroke(1.2.dp, qColors.coralInk.copy(alpha = 0.42f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp)
                            .testTag("quiz_two_errors_advance_card")
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            QuizzyAnimatedMascot(
                                mood = QuizzyMascotMood.ENCOURAGE,
                                size = 66.dp
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(
                                    text = "2 essais utilisés — on passe à la suite !",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = qColors.coralInk,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "Pas de souci ! Cette question reviendra à la fin du quiz en mode rattrapage (0 point, essais illimités) pour que tu la réussisses à ton rythme.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = qColors.bodyInk
                                )
                            }
                        }
                    }
                }

                // Benevolent Socratic Guidance on Wrong Attempts (shown when still retrying: 1st error in main pass, or any error in unlimited remediation)
                AnimatedVisibility(visible = wrongAttemptsOnQuestion > 0 && !isQuestionSolved && !wasSkipped && !failedOutOnMainPass) {
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
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                QuizzyAnimatedMascot(
                                    mood = QuizzyMascotMood.ENCOURAGE,
                                    size = 64.dp
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = QuizFeedbackHelper.friendlyWrongAnswerMessage(
                                            questionIndex = activeQuestionIndex + wrongAttemptsOnQuestion - 1,
                                            childAge = childAge,
                                            languageToneLevel = childLanguageToneLevel
                                        ),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = qColors.coralInk,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    if (hadStreakBeforeMiss && !isRemediationPhase) {
                                        Text(
                                            text = "Ta série repart à zéro, tu vas te rattraper très vite !",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    val xpRetryNotice = when {
                                        isRemediationPhase ->
                                            "Rattrapage (0 XP) : tu as autant d'essais que tu veux pour trouver !"
                                        isReplayAttempt ->
                                            "Mode entraînement (0 XP) : il te reste 1 essai sur cette question."
                                        else ->
                                            "Dernier essai sur cette question pour gagner la moitié des points (+$nextTryPotentialXp XP)"
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
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        QuizzyAnimatedMascot(
                                            mood = QuizzyMascotMood.THINKING,
                                            size = 52.dp
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
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
                                    }
                                }

                                // Indice 2 (shown together with Indice 1 from 2nd wrong attempt in unlimited remediation)
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
                                text = "Gardée pour ton rattrapage de fin de quiz !",
                                style = MaterialTheme.typography.titleSmall,
                                color = qColors.headingInk,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Tu la retrouveras à la fin du quiz avec des essais illimités. Garde cette piste en tête :",
                                style = MaterialTheme.typography.bodySmall,
                                color = qColors.bodyInk
                            )
                            Text(
                                text = hint1,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Solved Celebration with rotating educational animations
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
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                QuizzyAnimatedMascot(
                                    mood = QuizzyMascotMood.VICTORY,
                                    size = 68.dp
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = when {
                                            isRemediationPhase ->
                                                "0 XP • Bravo, question de rattrapage validée !"
                                            isReplayAttempt && wrongAttemptsOnQuestion > 0 ->
                                                "Super persévérance ! Tu as trouvé la bonne réponse par toi-même !"
                                            isReplayAttempt ->
                                                if (currentStreak >= 2) "Bravo, série x$currentStreak ! (Entraînement)"
                                                else "Bravo, bonne réponse !"
                                            wrongAttemptsOnQuestion == 0 ->
                                                if (currentStreak >= 2) "+$lastQuestionXp XP • Bravo, série x$currentStreak !"
                                                else "+$lastQuestionXp XP • Bravo, bonne réponse !"
                                            else ->
                                                "+$lastQuestionXp XP (½ points) • Super persévérance ! Tu as trouvé la bonne réponse par toi-même !"
                                        },
                                        style = MaterialTheme.typography.titleSmall,
                                        color = qColors.successInk,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    if (!isRemediationPhase && wrongAttemptsOnQuestion > 0) {
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
                        q.isImageChoice -> if (q.isMultiSelect) selectedOptions.isNotEmpty() else selectedOption != null
                        q.isDragMatch -> dragMatchAssignments.size == q.matchingPairs.size
                        q.isDragOrder -> dragOrderSlots.size == q.orderedItems.size && dragOrderSlots.none { it == null }
                        q.isFillBlank -> textAnswer.isNotBlank()
                        else -> if (q.isMultiSelect) selectedOptions.isNotEmpty() else selectedOption != null
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

                    if (!isRemediationPhase && wrongAttemptsOnQuestion >= 1) {
                        Spacer(Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                wasSkipped = true
                                if (activeQuestionIndex !in remediationQuestionIndices) {
                                    remediationQuestionIndices.add(activeQuestionIndex)
                                }
                            },
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
                                text = "Passer cette question pour la fin du quiz",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else {
                    val nextLabel = when {
                        isRemediationPhase && remediationPosition < remediationQueue.lastIndex ->
                            "Question de rattrapage suivante"
                        isRemediationPhase ->
                            "Terminer le quiz"
                        currentIndex < questions.lastIndex ->
                            "Question suivante"
                        remediationQuestionIndices.isNotEmpty() ->
                            "Revoir mes erreurs (${remediationQuestionIndices.size})"
                        else ->
                            "Voir mon résultat"
                    }
                    Button(
                        onClick = { moveToNextQuestion() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("quiz_next_button"),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (failedOutOnMainPass) qColors.coralInk else MaterialTheme.colorScheme.primary,
                            contentColor = if (failedOutOnMainPass && qColors.isDark) Color(0xFF0E1726) else MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = nextLabel,
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
