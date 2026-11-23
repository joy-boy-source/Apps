package com.example.ui.screens

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.local.GameScoreEntity
import com.example.data.local.SubjectSqlStats
import com.example.model.ChildProfile
import com.example.model.Lesson
import com.example.model.Quiz
import com.example.model.SchoolCatalog
import com.example.ui.components.ModernCard
import com.example.ui.theme.QuizzyTheme
import org.json.JSONArray
import org.json.JSONObject

const val SUBJECT_GENERAL_3D = "Général"

data class Quiz3DSubjectOption(
    val subject: String,
    val questionCount: Int
)

private data class Game3DQuestionItem(
    val subject: String,
    val prompt: String,
    val options: List<String>,
    val correctIndex: Int
)

@OptIn(ExperimentalLayoutApi::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun Quizzy3DGameScreen(
    child: ChildProfile,
    quizzes: List<Quiz>,
    recentScores: List<GameScoreEntity>,
    subjectSqlStats: List<SubjectSqlStats>,
    onRecordGameFinished: (
        score: Int,
        correctCount: Int,
        totalQuestions: Int,
        maxCombo: Int,
        durationSeconds: Int,
        subject: String
    ) -> Unit,
    onBack: () -> Unit,
    lessons: List<Lesson> = emptyList()
) {
    var showSqlLeaderboard by remember { mutableStateOf(false) }
    var showMissionSetup by remember { mutableStateOf(true) }
    var selectedSubject by remember { mutableStateOf(SUBJECT_GENERAL_3D) }
    var selectedDifficulty by remember { mutableStateOf("Facile") }
    var webPageLoaded by remember { mutableStateOf(false) }

    BackHandler {
        when {
            showSqlLeaderboard -> showSqlLeaderboard = false
            !showMissionSetup -> showMissionSetup = true
            else -> onBack()
        }
    }

    val latestOnFinished by rememberUpdatedState(onRecordGameFinished)
    val qColors = QuizzyTheme.colors

    val subjectOptions = remember(quizzes, lessons) {
        build3DSubjectOptions(quizzes = quizzes, lessons = lessons)
    }

    val effectiveSubject = remember(selectedSubject, subjectOptions) {
        if (subjectOptions.any { it.subject == selectedSubject }) selectedSubject else SUBJECT_GENERAL_3D
    }

    val selectedQuestionCount = remember(effectiveSubject, subjectOptions) {
        subjectOptions.firstOrNull { it.subject == effectiveSubject }?.questionCount ?: 0
    }

    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    fun pushMissionToWebView(autoStart: Boolean, openExplorer: Boolean) {
        val payload = build3DMissionConfigJson(
            quizzes = quizzes,
            lessons = lessons,
            selectedSubject = effectiveSubject,
            difficulty = selectedDifficulty,
            autoStart = autoStart,
            openExplorer = openExplorer
        )
        val escaped = JSONObject.quote(payload)
        webViewRef?.evaluateJavascript(
            "window.configureQuizzy3D && window.configureQuizzy3D($escaped);",
            null
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.apply {
                stopLoading()
                removeJavascriptInterface("QuizzyGameBridge")
                destroy()
            }
            webViewRef = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B162C))
            .testTag("quizzy_3d_game_screen")
    ) {
        // Isolated WebGL Three.js viewport so Compose recompositions never disrupt the 60fps loop
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setLayerType(View.LAYER_TYPE_HARDWARE, null)
                    setBackgroundColor(android.graphics.Color.parseColor("#0B162C"))
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        allowFileAccess = true
                        allowContentAccess = true
                        mediaPlaybackRequiresUserGesture = false
                        cacheMode = WebSettings.LOAD_DEFAULT
                    }
                    webChromeClient = WebChromeClient()
                    addJavascriptInterface(
                        object {
                            @JavascriptInterface
                            fun onGameFinished(
                                score: Int,
                                correctCount: Int,
                                totalQuestions: Int,
                                maxCombo: Int,
                                durationSeconds: Int,
                                subject: String
                            ) {
                                post {
                                    latestOnFinished(
                                        score,
                                        correctCount,
                                        totalQuestions,
                                        maxCombo,
                                        durationSeconds,
                                        subject.ifBlank { SUBJECT_GENERAL_3D }
                                    )
                                }
                            }
                        },
                        "QuizzyGameBridge"
                    )
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            webPageLoaded = true
                            val payload = build3DMissionConfigJson(
                                quizzes = quizzes,
                                lessons = lessons,
                                selectedSubject = effectiveSubject,
                                difficulty = selectedDifficulty,
                                autoStart = false,
                                openExplorer = false
                            )
                            val escaped = JSONObject.quote(payload)
                            view?.evaluateJavascript(
                                "window.configureQuizzy3D && window.configureQuizzy3D($escaped);",
                                null
                            )
                        }
                    }
                    loadUrl("file:///android_asset/quizzy_3d_game.html")
                    webViewRef = this
                }
            }
        )

        // Compact native top bar (Back, Subject/Difficulty config pill, SQL Records pill)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onBack,
                shape = CircleShape,
                color = Color(0xEC0E182E),
                border = BorderStroke(1.dp, Color(0x775CE1D6)),
                modifier = Modifier
                    .size(44.dp)
                    .testTag("game_3d_back_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quitter le jeu 3D",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = { showMissionSetup = true },
                    shape = RoundedCornerShape(50),
                    color = Color(0xEC0E182E),
                    border = BorderStroke(1.dp, Color(0x885CE1D6)),
                    modifier = Modifier.testTag("game_3d_config_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = "Choisir matière et difficulté",
                            tint = Color(0xFF5CE1D6),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "$effectiveSubject • $selectedDifficulty",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Surface(
                    onClick = { showSqlLeaderboard = true },
                    shape = RoundedCornerShape(50),
                    color = Color(0xEC0E182E),
                    border = BorderStroke(1.dp, Color(0x88FFD34E)),
                    modifier = Modifier.testTag("game_3d_sql_scores_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Leaderboard,
                            contentDescription = "Records SQL",
                            tint = Color(0xFFFFD34E),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${recentScores.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFFFD34E),
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        // Native Mission Setup Overlay: Choose Subject (Course-specific or Général) + Difficulty
        if (showMissionSetup) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xD9071022))
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("game_3d_setup_panel"),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Quizzy 3D Arena",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = qColors.headingInk
                                )
                                Text(
                                    text = "Choisis ta matière de cours et ta vitesse de vol",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.testTag("game_3d_setup_close_button")
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Quitter le jeu 3D")
                            }
                        }

                        // 1. Difficulty Level Selector
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "1. Niveau de difficulté & vitesse",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = qColors.headingInk
                            )
                            val difficulties = listOf(
                                Triple("Facile", "Facile ×1", "Vitesse douce • 4 vies"),
                                Triple("Moyen", "Moyen ×1.2", "Équilibré • 3 vies"),
                                Triple("Difficile", "Difficile ×1.5", "Rapide • 3 vies")
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                difficulties.forEach { (key, title, desc) ->
                                    val isSelected = selectedDifficulty == key
                                    Surface(
                                        onClick = { selectedDifficulty = key },
                                        shape = RoundedCornerShape(18.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("game_3d_difficulty_${key.lowercase()}")
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = title,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else qColors.headingInk
                                            )
                                            Text(
                                                text = desc,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isSelected) {
                                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 2. Subject / Course Filter Selector
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "2. Matière du cours",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = qColors.headingInk
                            )
                            Text(
                                text = if (effectiveSubject == SUBJECT_GENERAL_3D) {
                                    "Mode Général : toutes les questions de l'ensemble de tes cours."
                                } else {
                                    "Questions basées uniquement sur tes cours de $effectiveSubject ($selectedQuestionCount questions prêtes)."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                subjectOptions.forEach { option ->
                                    val selected = effectiveSubject == option.subject
                                    val labelText = when {
                                        option.subject == SUBJECT_GENERAL_3D && option.questionCount > 0 ->
                                            "Général • Tous mes cours (${option.questionCount})"
                                        option.subject == SUBJECT_GENERAL_3D ->
                                            "Général (Tous les cours)"
                                        else ->
                                            "${option.subject} (${option.questionCount})"
                                    }
                                    FilterChip(
                                        selected = selected,
                                        onClick = { selectedSubject = option.subject },
                                        label = {
                                            Text(
                                                text = labelText,
                                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        modifier = Modifier.testTag("game_3d_subject_${option.subject}")
                                    )
                                }
                            }
                        }

                        // Launch CTAs
                        Button(
                            onClick = {
                                showMissionSetup = false
                                if (webPageLoaded) {
                                    pushMissionToWebView(autoStart = true, openExplorer = false)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 54.dp)
                                .testTag("game_3d_launch_button"),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Lancer la mission ($effectiveSubject • $selectedDifficulty)",
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                showMissionSetup = false
                                if (webPageLoaded) {
                                    pushMissionToWebView(autoStart = false, openExplorer = true)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("game_3d_labo_button"),
                            shape = RoundedCornerShape(50)
                        ) {
                            Icon(Icons.Filled.Explore, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Explorer le Labo 3D", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Slide-over SQL Scores & Subject Analytics Panel
        if (showSqlLeaderboard) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("game_3d_sql_dialog"),
                    shape = RoundedCornerShape(26.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Storage,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(
                                        text = "Records 3D & Base SQL",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = qColors.headingInk
                                    )
                                    Text(
                                        text = "Stockage local Room SQLite • ${child.name}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(
                                onClick = { showSqlLeaderboard = false },
                                modifier = Modifier.testTag("game_3d_sql_close_button")
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Fermer les records")
                            }
                        }

                        if (subjectSqlStats.isNotEmpty()) {
                            Text(
                                text = "Moyennes SQL par matière",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = qColors.headingInk
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                subjectSqlStats.take(4).forEach { stat ->
                                    Surface(
                                        color = qColors.pastelMint,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${stat.subject} (${stat.sessionsCount} sessions)",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = qColors.bodyInk
                                            )
                                            Text(
                                                text = "Moy. ${stat.avgScore.toInt()}% • Max ${stat.bestScore}%",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Text(
                            text = "Dernières parties 3D",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = qColors.headingInk
                        )

                        if (recentScores.isEmpty()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Termine ta première course 3D pour enregistrer ton score dans la base SQL locale !",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(recentScores.take(10)) { idx, item ->
                                    ModernCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        cornerRadius = 16.dp,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.EmojiEvents,
                                                contentDescription = null,
                                                tint = if (idx == 0) Color(0xFFFFB300) else MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Spacer(Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "#${idx + 1} • ${item.subject}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = qColors.headingInk
                                                )
                                                Text(
                                                    text = "${item.correctCount}/${item.totalQuestions} portes • Combo x${item.maxCombo} • ${item.durationSeconds}s",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Spacer(Modifier.width(8.dp))
                                            Surface(
                                                color = qColors.pastelYellow,
                                                shape = RoundedCornerShape(50)
                                            ) {
                                                Text(
                                                    text = "${item.score} pts",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = qColors.bodyInk,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
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
}

/**
 * Computes the available 3D Arena subject options:
 * Always starts with "Général" (all courses combined), followed by each subject
 * for which the child actually has deposited courses or quizzes.
 */
internal fun build3DSubjectOptions(
    quizzes: List<Quiz>,
    lessons: List<Lesson> = emptyList()
): List<Quiz3DSubjectOption> {
    val allItems = extractAllCourse3DQuestions(quizzes = quizzes, lessons = lessons)
    val grouped = allItems.groupBy { it.subject }
    val result = mutableListOf(
        Quiz3DSubjectOption(subject = SUBJECT_GENERAL_3D, questionCount = allItems.size)
    )
    grouped.entries
        .sortedByDescending { it.value.size }
        .forEach { (subject, items) ->
            if (subject.isNotBlank() && !subject.equals(SUBJECT_GENERAL_3D, ignoreCase = true)) {
                result.add(Quiz3DSubjectOption(subject = subject, questionCount = items.size))
            }
        }
    return result
}

/**
 * Extracts 3-lane answer-gate questions strictly from the child's actual courses & quizzes,
 * converting MCQ, IMAGE_CHOICE, DRAG_MATCH, DRAG_ORDER, FILL_BLANK, and lesson key points.
 */
private fun extractAllCourse3DQuestions(
    quizzes: List<Quiz>,
    lessons: List<Lesson>
): List<Game3DQuestionItem> {
    val items = mutableListOf<Game3DQuestionItem>()
    val lessonById = lessons.associateBy { it.id }

    // Collect a pool of answer words per subject to build smart distractors for FILL_BLANK questions
    val answerPoolBySubject = mutableMapOf<String, MutableList<String>>()
    quizzes.forEach { quiz ->
        val subj = SchoolCatalog.normalizeSubject(
            quiz.subject.ifBlank { lessonById[quiz.lessonId]?.subject.orEmpty() },
            "Autre"
        )
        val pool = answerPoolBySubject.getOrPut(subj) { mutableListOf() }
        quiz.questions.forEach { q ->
            q.options.forEach { if (it.isNotBlank()) pool.add(it.trim()) }
            q.acceptedAnswers.firstOrNull()?.takeIf { it.isNotBlank() }?.let { pool.add(it.trim()) }
            q.matchingPairs.forEach { if (it.right.isNotBlank()) pool.add(it.right.trim()) }
            q.imageOptions.forEach { if (it.label.isNotBlank()) pool.add(it.label.trim()) }
        }
    }

    quizzes.forEach { quiz ->
        val subject = SchoolCatalog.normalizeSubject(
            quiz.subject.ifBlank { lessonById[quiz.lessonId]?.subject.orEmpty() },
            "Autre"
        )
        val pool = answerPoolBySubject[subject].orEmpty().distinct()

        quiz.questions.forEach { q ->
            val rawTriple: Triple<String, List<String>, Int>? = when {
                q.options.size >= 3 -> {
                    val safeCorrect = q.correctIndex.coerceIn(0, q.options.lastIndex)
                    val correctText = q.options[safeCorrect].trim()
                    val distractors = q.options
                        .filterIndexed { i, _ -> i != safeCorrect }
                        .map { it.trim() }
                        .filter { it.isNotBlank() && !it.equals(correctText, ignoreCase = true) }
                        .distinct()
                        .take(2)
                    if (correctText.isNotBlank() && distractors.size == 2) {
                        Triple(q.prompt, listOf(correctText) + distractors, 0)
                    } else null
                }

                q.imageOptions.size >= 3 -> {
                    val safeCorrect = q.correctIndex.coerceIn(0, q.imageOptions.lastIndex)
                    val correctText = q.imageOptions[safeCorrect].label.trim()
                    val distractors = q.imageOptions
                        .filterIndexed { i, _ -> i != safeCorrect }
                        .map { it.label.trim() }
                        .filter { it.isNotBlank() && !it.equals(correctText, ignoreCase = true) }
                        .distinct()
                        .take(2)
                    if (correctText.isNotBlank() && distractors.size == 2) {
                        Triple(q.prompt, listOf(correctText) + distractors, 0)
                    } else null
                }

                q.matchingPairs.size >= 3 -> {
                    val targetPair = q.matchingPairs.first()
                    val correctText = targetPair.right.trim()
                    val distractors = q.matchingPairs.drop(1)
                        .map { it.right.trim() }
                        .filter { it.isNotBlank() && !it.equals(correctText, ignoreCase = true) }
                        .distinct()
                        .take(2)
                    if (targetPair.left.isNotBlank() && correctText.isNotBlank() && distractors.size == 2) {
                        Triple("Associe : « ${targetPair.left.trim()} »", listOf(correctText) + distractors, 0)
                    } else null
                }

                q.orderedItems.size >= 3 -> {
                    val correctText = q.orderedItems.first().trim()
                    val distractors = q.orderedItems.drop(1)
                        .map { it.trim() }
                        .filter { it.isNotBlank() && !it.equals(correctText, ignoreCase = true) }
                        .distinct()
                        .take(2)
                    if (correctText.isNotBlank() && distractors.size == 2) {
                        Triple("Quel élément vient en 1er ? (${q.prompt.take(55)})", listOf(correctText) + distractors, 0)
                    } else null
                }

                q.acceptedAnswers.isNotEmpty() -> {
                    val correctText = q.acceptedAnswers.first().trim()
                    val distractors = pool
                        .filter { !it.equals(correctText, ignoreCase = true) && it.length <= 36 }
                        .take(2)
                    if (correctText.isNotBlank() && distractors.size == 2) {
                        val cleanPrompt = q.sentenceTemplate.ifBlank { q.prompt }
                        Triple(cleanPrompt, listOf(correctText) + distractors, 0)
                    } else null
                }

                else -> null
            }

            rawTriple?.let { (promptText, opts, correctIdx) ->
                val shift = kotlin.math.abs(promptText.hashCode()) % 3
                val rotated = List(3) { i -> opts[(i + shift) % 3] }
                val newCorrect = (correctIdx - shift + 3) % 3
                items.add(
                    Game3DQuestionItem(
                        subject = subject,
                        prompt = promptText.trim(),
                        options = rotated,
                        correctIndex = newCorrect
                    )
                )
            }
        }
    }

    // If a deposited Lesson has no quiz items yet, synthesize a course-based question from its title & key points
    val subjectsWithQuizItems = items.map { it.subject }.toSet()
    val otherLessonTitles = lessons.map { it.title.trim() }.filter { it.isNotBlank() }.distinct()
    lessons.forEach { lesson ->
        val subject = SchoolCatalog.normalizeSubject(lesson.subject, "Autre")
        if (subject !in subjectsWithQuizItems && lesson.keyPoints.isNotEmpty() && lesson.title.isNotBlank()) {
            val keyHint = lesson.keyPoints.first().trim().take(72)
            val distractors = (otherLessonTitles.filter { !it.equals(lesson.title.trim(), ignoreCase = true) } +
                listOf("Autre chapitre", "Révision libre")).distinct().take(2)
            val opts = listOf(lesson.title.trim()) + distractors
            val shift = kotlin.math.abs(keyHint.hashCode()) % 3
            val rotated = List(3) { i -> opts[(i + shift) % 3] }
            val newCorrect = (0 - shift + 3) % 3
            items.add(
                Game3DQuestionItem(
                    subject = subject,
                    prompt = "De quel cours vient cette notion : « $keyHint » ?",
                    options = rotated,
                    correctIndex = newCorrect
                )
            )
        }
    }

    return items
}

/**
 * Backward-compatible helper returning the raw JSON array of 3-lane questions.
 */
internal fun build3DGameQuestionsJson(
    quizzes: List<Quiz>,
    lessons: List<Lesson> = emptyList(),
    selectedSubject: String = SUBJECT_GENERAL_3D
): String {
    val allItems = extractAllCourse3DQuestions(quizzes = quizzes, lessons = lessons)
    val filtered = if (selectedSubject.equals(SUBJECT_GENERAL_3D, ignoreCase = true)) {
        allItems
    } else {
        allItems.filter { it.subject.equals(selectedSubject, ignoreCase = true) }
    }.take(12)

    val array = JSONArray()
    filtered.forEach { item ->
        val obj = JSONObject().apply {
            put("subject", item.subject)
            put("prompt", item.prompt)
            put("options", JSONArray(item.options))
            put("correct", item.correctIndex)
        }
        array.put(obj)
    }
    return array.toString()
}

internal fun build3DMissionConfigJson(
    quizzes: List<Quiz>,
    lessons: List<Lesson>,
    selectedSubject: String,
    difficulty: String,
    autoStart: Boolean,
    openExplorer: Boolean
): String {
    val safeDifficulty = SchoolCatalog.difficultyDisplayLabel(difficulty)
    val questionsArray = JSONArray(
        build3DGameQuestionsJson(
            quizzes = quizzes,
            lessons = lessons,
            selectedSubject = selectedSubject
        )
    )
    return JSONObject().apply {
        put("subject", selectedSubject)
        put("difficulty", safeDifficulty)
        put("autoStart", autoStart)
        put("openExplorer", openExplorer)
        put("questions", questionsArray)
    }.toString()
}
