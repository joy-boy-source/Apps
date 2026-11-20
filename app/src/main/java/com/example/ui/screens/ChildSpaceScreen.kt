package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.data.CourseAttachment
import com.example.data.FrenchErrorMessages
import com.example.data.GeminiTutorService
import com.example.model.ChildProfile
import com.example.model.Family
import com.example.model.GamificationCatalog
import com.example.model.Lesson
import com.example.model.MissedQuestionRecord
import com.example.model.Quiz
import com.example.model.SchoolCatalog
import com.example.model.WeeklyReviewScheduler
import com.example.notifications.QuizNotificationHelper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.ui.components.AttachedDocumentChipRow
import com.example.ui.components.BadgeShowcaseRow
import com.example.ui.components.ChildAvatarBadge
import com.example.ui.components.QuizzyInlineLoader
import com.example.ui.components.DifficultyMultiplierChip
import com.example.ui.components.GradeLevelSelector
import com.example.ui.components.MascotSpeechBubble
import com.example.ui.components.ModernCard
import com.example.ui.components.QuizClipboardIllustration
import com.example.ui.components.QuizzyStarMascot
import com.example.ui.components.SubjectBadge
import com.example.ui.components.SubjectPastelCard
import com.example.ui.components.SuccessRateGauge
import com.example.ui.components.VoiceMicButton
import com.example.ui.components.XpLevelProgressCard
import com.example.ui.components.scoreColor
import com.example.ui.components.subjectColor
import com.example.ui.components.subjectPastelSurface
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.QuizzyTheme
import com.example.ui.viewmodel.OperationStatus
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ChildSpaceScreen(
    family: Family,
    child: ChildProfile,
    lessons: List<Lesson>,
    quizzes: List<Quiz>,
    opStatus: OperationStatus,
    isTranscribing: Boolean,
    transcribeError: String?,
    onClearOpStatus: () -> Unit,
    onSwitchProfile: () -> Unit,
    onUpdateGradeLevel: (age: Int, gradeLevel: String) -> Unit,
    onDepositCourse: (
        subject: String,
        documentType: String,
        difficulty: String,
        bitmap: Bitmap?,
        attachments: List<CourseAttachment>,
        notes: String
    ) -> Unit,
    onAddDocumentsToLesson: (lesson: Lesson, bitmap: Bitmap?, attachments: List<CourseAttachment>, notes: String) -> Unit,
    onGenerateMoreQuiz: (lesson: Lesson, difficulty: String) -> Unit,
    onGenerateGapQuiz: () -> Unit,
    onGenerateSundayReviewQuiz: (difficulty: String) -> Unit = {},
    onSubmitQuizResult: (
        lesson: Lesson?,
        quiz: Quiz,
        score: Int,
        total: Int,
        maxStreak: Int,
        masteredSkills: List<String>,
        weakSkills: List<String>,
        earnedXp: Long,
        missedQuestions: List<MissedQuestionRecord>
    ) -> Unit,
    onTranscribeAudio: (bytes: ByteArray, onResult: (String) -> Unit) -> Unit,
    nowCalendarProvider: () -> Calendar = { Calendar.getInstance() }
) {
    val context = LocalContext.current
    val childLessons = remember(lessons, child.id) { lessons.filter { it.childId == child.id } }
    val childQuizzes = remember(quizzes, child.id) { quizzes.filter { it.childId == child.id } }

    var selectedTab by remember { mutableIntStateOf(0) }
    var activeLessonId by remember { mutableStateOf<String?>(null) }
    var activeQuizId by remember { mutableStateOf<String?>(null) }
    var showDepositDialog by remember { mutableStateOf(false) }
    var showGradeDialog by remember { mutableStateOf(false) }
    var selectedSubjectFilter by remember { mutableStateOf("Toutes") }
    var selectedDocTypeFilter by remember { mutableStateOf("Tout") }

    // Schedule Play-Policy compliant Sunday 14:00 local reminder whenever missed answers are stored
    LaunchedEffect(child.id, child.weeklyMissedQuestions.size) {
        if (child.weeklyMissedQuestions.isNotEmpty()) {
            QuizNotificationHelper.scheduleNextSundayReviewAlarm(
                context = context,
                childName = child.name,
                missedCount = child.weeklyMissedQuestions.size
            )
        }
    }

    // Navigate automatically to newly generated lesson synthesis or gap-targeted / Sunday review quiz
    LaunchedEffect(opStatus, childQuizzes) {
        if (opStatus is OperationStatus.Success) {
            if (!opStatus.createdLessonId.isNullOrBlank()) {
                showDepositDialog = false
                activeLessonId = opStatus.createdLessonId
                onClearOpStatus()
            } else if (!opStatus.createdQuizId.isNullOrBlank() && activeLessonId == null) {
                val created = childQuizzes.firstOrNull { it.id == opStatus.createdQuizId }
                if (created != null) {
                    activeQuizId = created.id
                    onClearOpStatus()
                }
            }
        }
    }

    val activeQuiz = childQuizzes.firstOrNull { it.id == activeQuizId }
    if (activeQuiz != null) {
        val parentLesson = childLessons.firstOrNull { it.id == activeQuiz.lessonId }
        QuizPlayerScreen(
            quiz = activeQuiz,
            lessonTitle = parentLesson?.title ?: activeQuiz.title,
            childPreviousXp = child.xp,
            childExistingBadges = child.badges,
            childQuizzesTaken = child.quizzesTaken,
            onBack = { activeQuizId = null },
            onQuizCompletedFull = { score, total, maxStreak, masteredSkills, weakSkills, earnedXp, missedQuestions ->
                onSubmitQuizResult(
                    parentLesson,
                    activeQuiz,
                    score,
                    total,
                    maxStreak,
                    masteredSkills,
                    weakSkills,
                    earnedXp,
                    missedQuestions
                )
            }
        )
        return
    }

    val activeLesson = childLessons.firstOrNull { it.id == activeLessonId }
    if (activeLesson != null) {
        val lessonQuizzes = childQuizzes.filter { it.lessonId == activeLesson.id }
        LessonDetailScreen(
            lesson = activeLesson,
            quizzes = lessonQuizzes,
            isReadOnlyParent = false,
            opStatus = opStatus,
            onBack = {
                onClearOpStatus()
                activeLessonId = null
            },
            onStartQuiz = { quiz -> activeQuizId = quiz.id },
            onGenerateNewQuiz = { difficulty ->
                onGenerateMoreQuiz(activeLesson, difficulty)
            },
            onAddDocumentsToLesson = { bitmap, attachments, notes ->
                onAddDocumentsToLesson(activeLesson, bitmap, attachments, notes)
            }
        )
        return
    }

    BackHandler(onBack = onSwitchProfile)

    val qColors = QuizzyTheme.colors

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChildAvatarBadge(
                        avatarKey = child.avatarKey,
                        name = child.name,
                        size = 48.dp
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Bonjour ${child.name} !",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = qColors.headingInk
                        )
                        Surface(
                            onClick = { showGradeDialog = true },
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .testTag("child_grade_pill"),
                            color = qColors.pastelMint,
                            shape = RoundedCornerShape(50)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${child.gradeLevel} • ${child.age} ans",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "Modifier mon niveau",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                    val levelInfo = remember(child.xp) { GamificationCatalog.levelProgress(child.xp) }
                    Surface(
                        onClick = { selectedTab = 1 },
                        color = qColors.pastelYellow,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.testTag("child_topbar_xp_pill")
                    ) {
                        Text(
                            text = "N${levelInfo.level} • ${child.xp} XP",
                            style = MaterialTheme.typography.labelMedium,
                            color = qColors.bodyInk,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        IconButton(
                            onClick = onSwitchProfile,
                            modifier = Modifier.testTag("child_switch_profile_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "Changer de profil",
                                tint = qColors.bodyInk
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            val navColors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = qColors.pastelMint,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
            ) {
                NavigationBar(
                    windowInsets = WindowInsets.navigationBars,
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Filled.Book, contentDescription = null) },
                        label = { Text("Cours") },
                        colors = navColors,
                        modifier = Modifier.testTag("nav_child_courses")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Filled.Quiz, contentDescription = null) },
                        label = { Text("Quiz") },
                        colors = navColors,
                        modifier = Modifier.testTag("nav_child_quizzes")
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = {
                        onClearOpStatus()
                        showDepositDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    modifier = Modifier.testTag("deposit_course_fab")
                ) {
                    Icon(Icons.Filled.AddAPhoto, contentDescription = "Ajouter un cours ou exercice")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            when (selectedTab) {
                0 -> ChildCoursesTab(
                    child = child,
                    lessons = childLessons,
                    quizzes = childQuizzes,
                    selectedSubject = selectedSubjectFilter,
                    selectedDocType = selectedDocTypeFilter,
                    onSelectSubject = { selectedSubjectFilter = it },
                    onSelectDocType = { selectedDocTypeFilter = it },
                    onOpenLesson = { activeLessonId = it.id },
                    onStartQuiz = { activeQuizId = it.id },
                    onOpenDeposit = {
                        onClearOpStatus()
                        showDepositDialog = true
                    }
                )
                else -> ChildQuizzesAndProgressTab(
                    familyName = family.familyName,
                    child = child,
                    quizzes = childQuizzes,
                    lessons = childLessons,
                    opStatus = opStatus,
                    onStartQuiz = { activeQuizId = it.id },
                    onGenerateGapQuiz = onGenerateGapQuiz,
                    onGenerateSundayReviewQuiz = onGenerateSundayReviewQuiz,
                    onEditGrade = { showGradeDialog = true },
                    nowCalendarProvider = nowCalendarProvider
                )
            }
        }
    }

    if (showDepositDialog) {
        DepositCourseDialog(
            child = child,
            opStatus = opStatus,
            isTranscribing = isTranscribing,
            transcribeError = transcribeError,
            onTranscribeAudio = onTranscribeAudio,
            onDismiss = {
                if (opStatus !is OperationStatus.Busy) {
                    onClearOpStatus()
                    showDepositDialog = false
                }
            },
            onSubmit = { subject, documentType, difficulty, bitmap, attachments, notes ->
                onDepositCourse(subject, documentType, difficulty, bitmap, attachments, notes)
            }
        )
    }

    if (showGradeDialog) {
        EditGradeLevelDialog(
            currentAge = child.age.toInt(),
            currentGrade = child.gradeLevel,
            onDismiss = { showGradeDialog = false },
            onSave = { newAge, newGrade ->
                onUpdateGradeLevel(newAge, newGrade)
                showGradeDialog = false
            }
        )
    }
    }
}

@Composable
private fun ChildCoursesTab(
    child: ChildProfile,
    lessons: List<Lesson>,
    quizzes: List<Quiz>,
    selectedSubject: String,
    selectedDocType: String,
    onSelectSubject: (String) -> Unit,
    onSelectDocType: (String) -> Unit,
    onOpenLesson: (Lesson) -> Unit,
    onStartQuiz: (Quiz) -> Unit,
    onOpenDeposit: () -> Unit
) {
    val filtered = remember(lessons, selectedSubject, selectedDocType) {
        lessons.filter { lesson ->
            val subjectMatches = selectedSubject == "Toutes" || lesson.subject == selectedSubject
            val docTypeMatches = selectedDocType == "Tout" ||
                SchoolCatalog.normalizeDocumentType(lesson.documentType) == selectedDocType
            subjectMatches && docTypeMatches
        }
    }

    // Highlighted subject folders ordered with primary subjects first
    val displayedSubjects = remember(lessons) {
        val priority = listOf("Français", "Mathématiques", "Anglais", "Sciences", "Histoire-Géo", "SVT", "Physique-Chimie")
        val presentExtras = lessons.map { it.subject }.distinct().filter { it !in priority }
        priority + presentExtras
    }

    val subjectStats = remember(lessons, quizzes) {
        displayedSubjects.associateWith { subj ->
            val subjLessons = lessons.filter { it.subject == subj }
            val scored = subjLessons.mapNotNull { it.bestScorePercent?.toInt() }
            val quizScored = quizzes.filter { it.subject == subj }.mapNotNull { it.lastScorePercent?.toInt() }
            val allScores = (scored + quizScored)
            val pct = when {
                allScores.isNotEmpty() -> allScores.sum() / allScores.size
                subjLessons.isNotEmpty() -> 25 // Started studying this subject
                else -> 0
            }
            Triple(pct, allScores.isNotEmpty() || subjLessons.isNotEmpty(), subjLessons.size)
        }
    }

    val dailyQuiz = remember(quizzes) {
        quizzes.firstOrNull { !it.isAlreadyCompleted } ?: quizzes.firstOrNull()
    }

    val qColors = QuizzyTheme.colors

    Box(modifier = Modifier.fillMaxSize()) {
        // Subtle organic pastel corner glow matching the reference mockup
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = qColors.pastelMint.copy(alpha = 0.65f),
                radius = size.width * 0.34f,
                center = Offset(size.width * 0.96f, size.height * 0.02f)
            )
            drawCircle(
                color = qColors.pastelPeach.copy(alpha = 0.45f),
                radius = size.width * 0.30f,
                center = Offset(size.width * 0.02f, size.height * 0.30f)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 92.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Friendly Star Mascot + Speech Bubble Greeting
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    QuizzyStarMascot(size = 86.dp)
                    MascotSpeechBubble(
                        text = "Prêt à réviser et\nprogresser ?",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 2. "Mes matières" Large Pastel Subject Cards with Circular Progress Rings
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Mes dossiers par matière",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = qColors.headingInk
                        )
                        if (selectedSubject != "Toutes") {
                            Surface(
                                onClick = { onSelectSubject("Toutes") },
                                color = qColors.pastelMint,
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "Tout afficher",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(displayedSubjects, key = { it }) { subject ->
                            val (pct, hasData, count) = subjectStats[subject] ?: Triple(0, false, 0)
                            val isSelected = selectedSubject == subject
                            SubjectPastelCard(
                                subject = subject,
                                percent = pct,
                                hasData = hasData,
                                lessonCount = count,
                                selected = isSelected,
                                onClick = {
                                    onSelectSubject(if (isSelected) "Toutes" else subject)
                                }
                            )
                        }
                    }
                }
            }

            // 3. Prominent Deep-Turquoise Pill CTA: "+ Ajouter un cours"
            item {
                Button(
                    onClick = onOpenDeposit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("child_add_course_pill"),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Ajouter un cours, exercice ou devoir",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // 4. "Quiz du jour" Warm Peach Card with Clipboard Illustration & Coral CTA
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("child_daily_quiz_card"),
                    shape = RoundedCornerShape(26.dp),
                    color = qColors.pastelPeach,
                    border = BorderStroke(1.dp, qColors.coralInk.copy(alpha = 0.24f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuizClipboardIllustration(size = 64.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Quiz du jour",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = qColors.headingInk
                            )
                            Text(
                                text = if (dailyQuiz != null) {
                                    val xpHint = if (dailyQuiz.isAlreadyCompleted) "Entraînement (0 XP)" else "+XP à gagner"
                                    "${dailyQuiz.questions.size} questions • ${dailyQuiz.subject} • $xpHint"
                                } else {
                                    "Ajoute un cours pour lancer ton premier quiz"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = qColors.secondaryInk,
                                maxLines = 2
                            )
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    if (dailyQuiz != null) onStartQuiz(dailyQuiz) else onOpenDeposit()
                                },
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CoralAccent,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .heightIn(min = 40.dp)
                                    .testTag("child_daily_quiz_cta")
                            ) {
                                Text(
                                    text = if (dailyQuiz != null) "C'est parti !" else "Commencer",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }

            // 5. Bibliothèque des cours, exercices & devoirs
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedSubject == "Toutes") "Mes cours & exercices"
                            else "Dossier • $selectedSubject",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = qColors.headingInk
                        )
                        Text(
                            text = "${filtered.size} fiche${if (filtered.size > 1) "s" else ""}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val filters = listOf("Tout", "Cours", "Exercice", "Devoir")
                        items(filters, key = { it }) { filterLabel ->
                            FilterChip(
                                selected = selectedDocType == filterLabel,
                                onClick = { onSelectDocType(filterLabel) },
                                label = {
                                    Text(
                                        when (filterLabel) {
                                            "Exercice" -> "Exercices"
                                            "Devoir" -> "Devoirs"
                                            else -> filterLabel
                                        }
                                    )
                                },
                                modifier = Modifier.testTag("doc_type_filter_$filterLabel")
                            )
                        }
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    ModernCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 26.dp,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(26.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(58.dp)
                                    .clip(CircleShape)
                                    .background(qColors.pastelMint),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CameraAlt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "Aucun cours déposé ici",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Ajoute une photo ou un fichier PDF de ton cahier pour créer une fiche claire et des quiz.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = onOpenDeposit,
                                shape = RoundedCornerShape(50)
                            ) {
                                Icon(Icons.Filled.AddAPhoto, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Ajouter mon premier cours")
                            }
                        }
                    }
                }
            } else {
                items(filtered, key = { it.id }) { lesson ->
                    val subjColor = subjectColor(lesson.subject)
                    val subjPastel = subjectPastelSurface(lesson.subject)
                    val dateLabel = remember(lesson.createdAt) { formatCourseDate(lesson.createdAt) }

                    ModernCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("child_lesson_card_${lesson.id}"),
                        cornerRadius = 24.dp,
                        borderColor = subjColor.copy(alpha = 0.16f),
                        onClick = { onOpenLesson(lesson) }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    SubjectBadge(subject = lesson.subject)
                                    val docTypeLabel = SchoolCatalog.normalizeDocumentType(lesson.documentType)
                                    Surface(
                                        color = when (docTypeLabel) {
                                            "Exercice" -> qColors.pastelYellow
                                            "Devoir" -> qColors.pastelPeach
                                            else -> qColors.pastelMint
                                        },
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text(
                                            text = docTypeLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = qColors.bodyInk,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (dateLabel.isNotBlank()) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.CalendarToday,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = dateLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    lesson.bestScorePercent?.let { best ->
                                        Surface(
                                            color = scoreColor(best.toInt()).copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Text(
                                                text = "$best%",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = scoreColor(best.toInt()),
                                                fontWeight = FontWeight.ExtraBold,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = lesson.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = qColors.headingInk
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = lesson.summaryIntro,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    color = subjPastel,
                                    shape = RoundedCornerShape(50)
                                ) {
                                    val docsSuffix = if (lesson.attachedFiles.isNotEmpty()) {
                                        " • ${lesson.attachedFiles.size} doc"
                                    } else ""
                                    Text(
                                        text = "${lesson.keyPoints.size} points clés • ${lesson.quizCount} quiz$docsSuffix",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = subjColor,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                if (lesson.weakPoints.isNotEmpty()) {
                                    Surface(
                                        color = qColors.pastelPeach,
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text(
                                            text = "${lesson.weakPoints.size} à travailler",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = qColors.coralInk,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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

private fun formatCourseDate(timestamp: Timestamp?): String {
    val date = timestamp?.toDate() ?: return ""
    return SimpleDateFormat("d MMM", Locale.FRANCE).format(date)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChildQuizzesAndProgressTab(
    familyName: String,
    child: ChildProfile,
    quizzes: List<Quiz>,
    lessons: List<Lesson>,
    opStatus: OperationStatus,
    onStartQuiz: (Quiz) -> Unit,
    onGenerateGapQuiz: () -> Unit,
    onGenerateSundayReviewQuiz: (difficulty: String) -> Unit,
    onEditGrade: () -> Unit,
    nowCalendarProvider: () -> Calendar
) {
    val allWeakPoints = remember(child.weakPoints, lessons) {
        (child.weakPoints + lessons.flatMap { it.weakPoints })
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }
    val isBusy = opStatus is OperationStatus.Busy
    val nowCal = nowCalendarProvider()
    val nowMillis = nowCal.timeInMillis
    val currentWeekKey = remember(nowMillis) { WeeklyReviewScheduler.currentWeekKey(nowCal) }
    val isSundayWindowOpen = remember(nowMillis) { WeeklyReviewScheduler.isSundayReviewWindowOpen(nowCal) }
    val sundayCountdown = remember(nowMillis) { WeeklyReviewScheduler.countdownLabel(nowCal) }
    val existingSundayQuiz = remember(quizzes, currentWeekKey) {
        quizzes.firstOrNull { it.weeklyReview && it.weekKey == currentWeekKey }
    }
    val missedQuestions = child.weeklyMissedQuestions
    val missedThemes = remember(missedQuestions) {
        missedQuestions
            .map { it.skillTag.ifBlank { it.subject } }
            .filter { it.isNotBlank() }
            .distinct()
    }
    var sundayDifficulty by remember { mutableStateOf("Moyen") }
    val qColors = QuizzyTheme.colors

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 640.dp),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ModernCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ChildAvatarBadge(avatarKey = child.avatarKey, name = child.name, size = 60.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = child.name, style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = "${child.gradeLevel} • ${child.age} ans",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = familyName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        SuccessRateGauge(
                            percent = child.successRatePercent,
                            hasAttempts = child.totalQuestions > 0,
                            size = 66.dp
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatMiniCard(
                            label = "Cours",
                            value = "${lessons.size}",
                            modifier = Modifier.weight(1f)
                        )
                        StatMiniCard(
                            label = "Quiz faits",
                            value = "${child.quizzesTaken}",
                            modifier = Modifier.weight(1f)
                        )
                        StatMiniCard(
                            label = "Bonnes rép.",
                            value = "${child.totalCorrect}/${child.totalQuestions}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onEditGrade,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("edit_grade_level_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Modifier ma classe ou mon âge")
                    }
                }
            }
        }

        // Sunday 14:00 Weekly 10-Question Review Quiz Card (all first-try wrong answers saved during the week)
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sunday_review_quiz_card"),
                shape = RoundedCornerShape(24.dp),
                color = if (isSundayWindowOpen && (missedQuestions.isNotEmpty() || existingSundayQuiz != null)) {
                    qColors.pastelMint
                } else {
                    qColors.pastelYellow
                },
                border = BorderStroke(
                    width = 1.5.dp,
                    color = if (isSundayWindowOpen) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                    else qColors.headingInk.copy(alpha = 0.25f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = null,
                                tint = qColors.headingInk
                            )
                            Text(
                                text = "Quiz Révision du Dimanche",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = qColors.headingInk
                            )
                        }
                        Surface(
                            color = qColors.chipOnTint,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("sunday_review_schedule_badge")
                        ) {
                            Text(
                                text = "Dim. 14h • 10 Q",
                                style = MaterialTheme.typography.labelSmall,
                                color = qColors.headingInk,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = sundayCountdown,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSundayWindowOpen) MaterialTheme.colorScheme.primary else qColors.coralInk,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.testTag("sunday_review_countdown_text")
                    )

                    if (missedQuestions.isEmpty() && existingSundayQuiz == null) {
                        Text(
                            text = "Aucune erreur enregistrée cette semaine ! Dès qu'une question est ratée au 1er essai, elle est gardée de côté pour le quiz de 10 nouvelles questions du dimanche à 14h.",
                            style = MaterialTheme.typography.bodySmall,
                            color = qColors.bodyInk
                        )
                    } else {
                        Text(
                            text = if (existingSundayQuiz != null) {
                                "Ton quiz de 10 nouvelles questions sur les thématiques en échec de la semaine est prêt !"
                            } else {
                                "${missedQuestions.size} mauvaise${if (missedQuestions.size > 1) "s" else ""} réponse${if (missedQuestions.size > 1) "s" else ""} mémorisée${if (missedQuestions.size > 1) "s" else ""} • Dimanche à 14h, Quizzy te préparera 10 nouvelles questions sur les mêmes thématiques."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = qColors.bodyInk,
                            modifier = Modifier.testTag("sunday_review_missed_summary")
                        )

                        if (missedThemes.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                missedThemes.take(5).forEach { theme ->
                                    Surface(
                                        color = qColors.chipOnTint,
                                        shape = RoundedCornerShape(50),
                                        border = BorderStroke(1.dp, qColors.headingInk.copy(alpha = 0.24f))
                                    ) {
                                        Text(
                                            text = theme,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = qColors.bodyInk,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (existingSundayQuiz == null && isSundayWindowOpen) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                SchoolCatalog.difficulties.forEach { diff ->
                                    FilterChip(
                                        selected = sundayDifficulty == diff,
                                        onClick = { sundayDifficulty = diff },
                                        label = { Text(SchoolCatalog.difficultyMultiplierLabel(diff)) },
                                        modifier = Modifier.testTag("sunday_difficulty_chip_$diff")
                                    )
                                }
                            }
                        }

                        val usePrimarySundayBtn = isSundayWindowOpen || existingSundayQuiz != null
                        val sundayBtnContainer = if (usePrimarySundayBtn) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            qColors.navyActionContainer
                        }
                        val sundayBtnContent = if (usePrimarySundayBtn) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            qColors.navyActionOnContainer
                        }

                        Button(
                            onClick = {
                                if (existingSundayQuiz != null) {
                                    onStartQuiz(existingSundayQuiz)
                                } else {
                                    onGenerateSundayReviewQuiz(sundayDifficulty)
                                }
                            },
                            enabled = !isBusy && (existingSundayQuiz != null || (isSundayWindowOpen && missedQuestions.isNotEmpty())),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("sunday_review_action_button"),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = sundayBtnContainer,
                                contentColor = sundayBtnContent
                            )
                        ) {
                            if (isBusy) {
                                QuizzyInlineLoader(size = 20.dp, tint = sundayBtnContent)
                                Spacer(Modifier.width(10.dp))
                                Text("Préparation des 10 nouvelles questions...")
                            } else {
                                Icon(
                                    imageVector = if (existingSundayQuiz != null) Icons.Filled.PlayArrow else Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = when {
                                        existingSundayQuiz != null -> "Lancer mon quiz du dimanche (10 Q)"
                                        isSundayWindowOpen -> "Générer mon quiz du dimanche (10 Q)"
                                        else -> "Disponible dimanche à 14h"
                                    },
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Gap-Targeted Quiz Card (evaluates weakPoints and generates adaptive classroom-style quiz)
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("child_gap_quiz_card"),
                shape = RoundedCornerShape(24.dp),
                color = qColors.pastelPeach,
                border = BorderStroke(1.dp, qColors.coralInk.copy(alpha = 0.28f))
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
                            tint = qColors.coralInk
                        )
                        Text(
                            text = "Quiz adapté à tes lacunes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = qColors.headingInk
                        )
                    }

                    if (allWeakPoints.isEmpty()) {
                        Text(
                            text = "Ajoute tes exercices ou devoirs dans l'onglet Cours pour que Quizzy repère les notions à renforcer et te crée un quiz sur mesure !",
                            style = MaterialTheme.typography.bodySmall,
                            color = qColors.bodyInk
                        )
                    } else {
                        Text(
                            text = "Notions repérées à travailler :",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = qColors.bodyInk
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            allWeakPoints.take(5).forEach { gap ->
                                Surface(
                                    color = qColors.chipOnTint,
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(1.dp, qColors.coralInk.copy(alpha = 0.28f))
                                ) {
                                    Text(
                                        text = gap,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = qColors.bodyInk,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (opStatus is OperationStatus.Error) {
                        Text(
                            text = opStatus.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = onGenerateGapQuiz,
                        enabled = !isBusy && lessons.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("generate_gap_quiz_button"),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CoralAccent,
                            contentColor = Color.White
                        )
                    ) {
                        if (isBusy) {
                            QuizzyInlineLoader(size = 20.dp, tint = Color.White)
                            Spacer(Modifier.width(10.dp))
                            Text("Préparation de ton quiz sur mesure...")
                        } else {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Lancer un quiz sur mes lacunes",
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }

        item {
            XpLevelProgressCard(child = child)
        }

        item {
            BadgeShowcaseRow(unlockedIds = child.badges)
        }

        item {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Mes Quiz de révision (${quizzes.size})",
                style = MaterialTheme.typography.titleLarge,
                color = qColors.headingInk,
                fontWeight = FontWeight.ExtraBold
            )
        }

        if (quizzes.isEmpty()) {
            item {
                ModernCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(26.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Quiz,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        Text("Pas encore de quiz", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Dépose d'abord un cours ou un exercice dans l'onglet Cours !",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(quizzes, key = { it.id }) { quiz ->
                val lessonTitle = lessons.firstOrNull { it.id == quiz.lessonId }?.title ?: quiz.subject
                ModernCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("child_quiz_card_${quiz.id}"),
                    cornerRadius = 18.dp,
                    onClick = { onStartQuiz(quiz) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                SubjectBadge(subject = quiz.subject)
                                DifficultyMultiplierChip(difficulty = quiz.difficulty)
                                if (quiz.questions.any { it.isFillBlank }) {
                                    Surface(
                                        color = qColors.pastelYellow,
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text(
                                            text = "QCM + À compléter",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = qColors.bodyInk,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = quiz.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = qColors.headingInk,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (quiz.weeklyReview) "Révision du dimanche • 10 nouvelles questions" else "Cours : $lessonTitle",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (quiz.targetedGaps.isNotEmpty()) {
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = "Cible : ${quiz.targetedGaps.take(2).joinToString(" • ")}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = qColors.coralInk,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            val status = if (quiz.isAlreadyCompleted) {
                                quiz.lastScorePercent?.let {
                                    val stars = GamificationCatalog.starsForPercentage(it.toInt())
                                    "Dernier score : $it% (${"★".repeat(stars)}${"☆".repeat(3 - stars)}) • Entraînement (0 XP)"
                                } ?: "Déjà fait • Entraînement (0 XP)"
                            } else {
                                "Nouveau quiz • +XP à gagner"
                            }
                            Text(
                                text = "${quiz.questions.size} questions • $status",
                                style = MaterialTheme.typography.labelMedium,
                                color = quiz.lastScorePercent?.let { scoreColor(it.toInt()) }
                                    ?: MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Jouer au quiz",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatMiniCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    ModernCard(
        modifier = modifier,
        cornerRadius = 16.dp,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DepositCourseDialog(
    child: ChildProfile,
    opStatus: OperationStatus,
    isTranscribing: Boolean,
    transcribeError: String?,
    onTranscribeAudio: (bytes: ByteArray, onResult: (String) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: (
        subject: String,
        documentType: String,
        difficulty: String,
        bitmap: Bitmap?,
        attachments: List<CourseAttachment>,
        notes: String
    ) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedDocType by remember { mutableStateOf(SchoolCatalog.documentTypes.first()) }
    var selectedSubject by remember { mutableStateOf(SchoolCatalog.AUTO_SUBJECT) }
    var selectedDifficulty by remember { mutableStateOf("Moyen") }
    var notes by remember { mutableStateOf("") }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedAttachments by remember { mutableStateOf<List<CourseAttachment>>(emptyList()) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var permissionNotice by remember { mutableStateOf<String?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                selectedBitmap = GeminiTutorService.loadBitmapFromUri(context, uri)
            }
        }
    }

    val documentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                permissionNotice = null
                val loaded = mutableListOf<CourseAttachment>()
                val errors = mutableListOf<String>()
                uris.forEach { uri ->
                    GeminiTutorService.loadCourseAttachmentFromUri(context, uri).fold(
                        onSuccess = { loaded.add(it) },
                        onFailure = { e -> errors.add(FrenchErrorMessages.toFrenchError(e, "Fichier illisible.")) }
                    )
                }
                if (loaded.isNotEmpty()) {
                    selectedAttachments = (selectedAttachments + loaded).take(5)
                }
                if (errors.isNotEmpty()) {
                    permissionNotice = errors.first()
                }
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            scope.launch {
                selectedBitmap = GeminiTutorService.loadBitmapFromUri(context, uri)
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permissionNotice = null
            val uri = createTempPhotoUri(context)
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            permissionNotice = "Autorise l'appareil photo ou utilise 'Galerie' / 'Fichier'."
        }
    }

    val isBusy = opStatus is OperationStatus.Busy
    val scrollState = rememberScrollState()

    BackHandler(enabled = true) {
        if (!isBusy) onDismiss()
    }

    // Automatically scroll down when a photo, document, warning, or error appears so the child immediately sees it
    LaunchedEffect(
        selectedBitmap,
        selectedAttachments.size,
        permissionNotice,
        transcribeError,
        opStatus
    ) {
        if (
            selectedBitmap != null ||
            selectedAttachments.isNotEmpty() ||
            !permissionNotice.isNullOrBlank() ||
            !transcribeError.isNullOrBlank() ||
            opStatus is OperationStatus.Error ||
            opStatus is OperationStatus.Busy
        ) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    // Rendered directly in the Activity window (avoiding Android Dialog's broken edge-to-edge WindowInsets)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.52f))
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(
                    enabled = !isBusy,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            val maxModalHeight = maxHeight
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 580.dp)
                    .heightIn(max = maxModalHeight),
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shadowElevation = 0.dp,
                tonalElevation = 0.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Compact Pinned Header
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 12.dp, top = 14.dp, bottom = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ajouter un cours ou exercice",
                                style = MaterialTheme.typography.headlineSmall
                            )
                            IconButton(
                                onClick = onDismiss,
                                enabled = !isBusy
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Fermer")
                            }
                        }
                        Text(
                            text = "Classement auto & bilan adaptés à ${child.name} (${child.gradeLevel}, ${child.age} ans)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Fully Scrollable Form Content (doc type, subject folder, photo, files, notes, notices/errors)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .testTag("deposit_dialog_scroll_container")
                            .verticalScroll(scrollState)
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        Text("1. Type de document", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SchoolCatalog.documentTypes.forEach { docType ->
                                FilterChip(
                                    selected = selectedDocType == docType,
                                    onClick = { selectedDocType = docType },
                                    label = { Text(docType) },
                                    modifier = Modifier.testTag("doctype_chip_$docType")
                                )
                            }
                        }
                        if (selectedDocType != "Cours") {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Quizzy évalue tes compétences acquises et repère tes lacunes sur ce ${selectedDocType.lowercase()}.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(Modifier.height(14.dp))
                        Text("2. Dossier matière", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Laisse « Classement auto » pour que Quizzy range ton document dans le bon dossier (Maths, Français, Anglais...).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = selectedSubject == SchoolCatalog.AUTO_SUBJECT,
                                onClick = { selectedSubject = SchoolCatalog.AUTO_SUBJECT },
                                label = { Text("Classement auto") },
                                modifier = Modifier.testTag("subject_chip_Auto")
                            )
                            SchoolCatalog.subjects.forEach { subj ->
                                FilterChip(
                                    selected = selectedSubject == subj,
                                    onClick = { selectedSubject = subj },
                                    label = { Text(subj) },
                                    modifier = Modifier.testTag("subject_chip_$subj")
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        Text("3. Niveau de difficulté du quiz", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Choisis le niveau du quiz généré (tu pourras aussi en créer d'autres depuis la synthèse).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SchoolCatalog.difficulties.forEach { diff ->
                                FilterChip(
                                    selected = selectedDifficulty == diff,
                                    onClick = { selectedDifficulty = diff },
                                    label = { Text(SchoolCatalog.difficultyMultiplierLabel(diff)) },
                                    modifier = Modifier.testTag("deposit_difficulty_chip_$diff")
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        Text("4. Photo ou fichier (PDF, Word, Texte...)", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val hasPerm = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.CAMERA
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (hasPerm) {
                                        val uri = createTempPhotoUri(context)
                                        pendingCameraUri = uri
                                        cameraLauncher.launch(uri)
                                    } else {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                },
                                enabled = !isBusy,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("take_photo_button"),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Caméra", maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = {
                                    galleryLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                enabled = !isBusy,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("pick_photo_button"),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Galerie", maxLines = 1)
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { documentLauncher.launch(arrayOf("*/*")) },
                            enabled = !isBusy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("pick_document_button"),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Filled.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Fichier (PDF, Word, Texte, CSV...)")
                        }

                        if (selectedAttachments.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                selectedAttachments.forEachIndexed { idx, att ->
                                    AttachedDocumentChipRow(
                                        fileName = att.fileName,
                                        mimeType = att.mimeType,
                                        sizeBytes = att.sizeBytes,
                                        onRemove = if (!isBusy) {
                                            { selectedAttachments = selectedAttachments.filterIndexed { i, _ -> i != idx } }
                                        } else null
                                    )
                                }
                            }
                        }

                        selectedBitmap?.let { bmp ->
                            Spacer(Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(170.dp)
                                    .clip(RoundedCornerShape(16.dp))
                            ) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Photo du cours sélectionnée",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                IconButton(
                                    onClick = { selectedBitmap = null },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), CircleShape)
                                ) {
                                    Icon(Icons.Filled.Close, contentDescription = "Retirer la photo")
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "5. Notes ou dictée vocale",
                                style = MaterialTheme.typography.titleMedium
                            )
                            VoiceMicButton(
                                isTranscribing = isTranscribing,
                                enabled = !isBusy,
                                onAudioCaptured = { bytes ->
                                    permissionNotice = null
                                    onTranscribeAudio(bytes) { transcript ->
                                        notes = if (notes.isBlank()) transcript else "$notes\n$transcript"
                                    }
                                },
                                onPermissionDenied = { msg -> permissionNotice = msg },
                                modifier = Modifier.testTag("deposit_mic_button")
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            placeholder = { Text("Ex : Chapitre sur les fractions ou dicte ton cours au micro...") },
                            minLines = 3,
                            maxLines = 5,
                            enabled = !isBusy,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("deposit_notes_input")
                        )
                        val notice = permissionNotice ?: transcribeError
                        if (!notice.isNullOrBlank()) {
                            Spacer(Modifier.height(12.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = notice,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        if (opStatus is OperationStatus.Error) {
                            Spacer(Modifier.height(12.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = opStatus.message,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        if (opStatus is OperationStatus.Busy) {
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                QuizzyInlineLoader(
                                    size = 22.dp,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = opStatus.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Compact Pinned Footer: Primary Action Button always visible above system bars & keyboard
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Button(
                            onClick = { onSubmit(selectedSubject, selectedDocType, selectedDifficulty, selectedBitmap, selectedAttachments, notes) },
                            enabled = !isBusy && (selectedBitmap != null || selectedAttachments.isNotEmpty() || notes.isNotBlank()),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .testTag("submit_deposit_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Générer synthèse & quiz (${SchoolCatalog.difficultyMultiplierLabel(selectedDifficulty)})")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditGradeLevelDialog(
    currentAge: Int,
    currentGrade: String,
    onDismiss: () -> Unit,
    onSave: (age: Int, grade: String) -> Unit
) {
    var age by remember { mutableIntStateOf(currentAge) }
    var grade by remember { mutableStateOf(currentGrade) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text("Mon niveau scolaire", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(12.dp))
                Text("Classe actuelle :", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                GradeLevelSelector(
                    selectedGrade = grade,
                    onSelect = {
                        grade = it
                        age = SchoolCatalog.suggestedAgeForGrade(it)
                    }
                )
                Spacer(Modifier.height(14.dp))
                Text("Âge : $age ans", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = age.toFloat(),
                    onValueChange = { age = it.roundToInt().coerceIn(5, 18) },
                    valueRange = 5f..18f,
                    steps = 12
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Annuler")
                    }
                    Button(
                        onClick = { onSave(age, grade) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_grade_button")
                    ) {
                        Text("Enregistrer")
                    }
                }
            }
        }
    }
}

private fun createTempPhotoUri(context: Context): Uri {
    val dir = File(context.cacheDir, "lesson_photos").apply { mkdirs() }
    val file = File(dir, "course_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
