package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.model.ChildProfile
import com.example.model.Family
import com.example.model.GamificationCatalog
import com.example.model.LanguageToneCatalog
import com.example.model.Lesson
import com.example.model.Quiz
import com.example.model.QuizAttempt
import com.example.model.SchoolCatalog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import com.example.ui.components.BadgeShowcaseRow
import com.example.ui.components.ChildAvatarBadge
import com.example.ui.components.GradeLevelSelector
import com.example.ui.components.ModernCard
import com.example.ui.components.SubjectBadge
import com.example.ui.components.SuccessRateGauge
import com.example.ui.components.WeeklyProgressBarChart
import com.example.ui.components.XpLevelProgressCard
import com.example.ui.components.scoreColor
import com.example.ui.components.subjectColor
import com.example.ui.components.subjectIcon
import com.example.ui.components.subjectShortLabel
import com.example.data.local.GameScoreEntity
import com.example.data.local.SubjectSqlStats
import com.example.ui.theme.ParentMintBar
import com.example.ui.theme.QuizzyTheme
import com.example.ui.viewmodel.OperationStatus
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun ParentDashboardScreen(
    family: Family,
    children: List<ChildProfile>,
    lessons: List<Lesson>,
    quizzes: List<Quiz>,
    attempts: List<QuizAttempt>,
    opStatus: OperationStatus,
    onSwitchProfile: () -> Unit,
    onAddChild: (name: String, age: Int, grade: String, avatar: String, pin: String) -> Unit,
    onEditChild: (childId: String, name: String, age: Int, grade: String, avatar: String, newPin: String?) -> Unit,
    onDeleteChild: (childId: String) -> Unit,
    unreadNotificationCount: Int = 0,
    notificationsPermissionGranted: Boolean = true,
    onRequestNotificationPermission: () -> Unit = {},
    onMarkNotificationsRead: () -> Unit = {},
    sqlGameScores: List<GameScoreEntity> = emptyList(),
    sqlStudioImagesCount: Int = 0,
    sqlSubjectStats: List<SubjectSqlStats> = emptyList(),
    onUpdateChildLanguageTone: (childId: String, toneLevel: String) -> Unit = { _, _ -> },
    onAddChildWithTone: ((name: String, age: Int, grade: String, avatar: String, pin: String, languageToneLevel: String) -> Unit)? = null,
    onEditChildWithTone: ((childId: String, name: String, age: Int, grade: String, avatar: String, newPin: String?, languageToneLevel: String) -> Unit)? = null
) {
    val clipboard = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var selectedChildId by remember { mutableStateOf<String?>(null) }
    var inspectedLessonId by remember { mutableStateOf<String?>(null) }
    var showAddChildDialog by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var editingChild by remember { mutableStateOf<ChildProfile?>(null) }
    var deletingChild by remember { mutableStateOf<ChildProfile?>(null) }

    val inspectedLesson = lessons.firstOrNull { it.id == inspectedLessonId }
    if (inspectedLesson != null) {
        LessonDetailScreen(
            lesson = inspectedLesson,
            quizzes = quizzes.filter { it.lessonId == inspectedLesson.id },
            isReadOnlyParent = true,
            opStatus = opStatus,
            onBack = { inspectedLessonId = null },
            onStartQuiz = {},
            onGenerateNewQuiz = {}
        )
        return
    }

    val selectedChild = children.firstOrNull { it.id == selectedChildId }
    if (selectedChild != null) {
        ParentChildDetailView(
            child = selectedChild,
            lessons = lessons.filter { it.childId == selectedChild.id },
            attempts = attempts.filter { it.childId == selectedChild.id },
            onBack = { selectedChildId = null },
            onOpenLesson = { inspectedLessonId = it.id },
            onEdit = { editingChild = selectedChild },
            onDelete = { deletingChild = selectedChild },
            onUpdateLanguageTone = { tone -> onUpdateChildLanguageTone(selectedChild.id, tone) }
        )
    } else {
        BackHandler(onBack = onSwitchProfile)

        var focusedChildId by remember(children) { mutableStateOf<String?>(children.firstOrNull()?.id) }
        var showChildDropdown by remember { mutableStateOf(false) }

        val activeChildIds = remember(children) { children.map { it.id }.toSet() }
        val activeLessons = remember(lessons, activeChildIds) { lessons.filter { it.childId in activeChildIds } }
        val activeAttempts = remember(attempts, activeChildIds) { attempts.filter { it.childId in activeChildIds } }

        val focusedChild = children.firstOrNull { it.id == focusedChildId } ?: children.firstOrNull()
        val focusedLessons = remember(activeLessons, focusedChild?.id) {
            if (focusedChild != null) activeLessons.filter { it.childId == focusedChild.id }
            else activeLessons
        }
        val focusedAttempts = remember(activeAttempts, focusedChild?.id) {
            if (focusedChild != null) activeAttempts.filter { it.childId == focusedChild.id }
            else activeAttempts
        }
        val subjectProgressList = remember(focusedAttempts) {
            val baseSubjects = listOf("Français", "Mathématiques", "Anglais", "Sciences")
            val attemptSubjects = focusedAttempts.map { it.subject }.distinct()
            val allSubjects = (baseSubjects + attemptSubjects).distinct().take(6)
            allSubjects.map { subject ->
                val list = focusedAttempts.filter { it.subject == subject }
                val correct = list.sumOf { it.score }
                val total = list.sumOf { it.totalQuestions }
                val pct = if (total > 0L) ((correct * 100L) / total).toInt().coerceIn(0, 100) else 0
                Triple(subject, pct, total > 0L)
            }
        }

        val totalCorrect = children.sumOf { it.totalCorrect }
        val totalQuestions = children.sumOf { it.totalQuestions }
        val globalSuccess = if (totalQuestions > 0L) ((totalCorrect * 100L) / totalQuestions).toInt() else 0
        val qColors = QuizzyTheme.colors

        Scaffold(
            containerColor = qColors.parentCanvas,
            contentWindowInsets = WindowInsets.navigationBars,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Surface(color = qColors.parentCanvas, tonalElevation = 0.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(qColors.parentSkyContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Group,
                                contentDescription = null,
                                tint = qColors.parentSkyAccent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Espace parents",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = qColors.headingInk
                            )
                            Text(
                                text = family.familyName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shadowElevation = 0.dp
                        ) {
                            IconButton(
                                onClick = {
                                    showNotificationsDialog = true
                                    onMarkNotificationsRead()
                                },
                                modifier = Modifier.testTag("parent_notifications_button")
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (unreadNotificationCount > 0) {
                                            Badge {
                                                Text(
                                                    text = if (unreadNotificationCount > 9) "9+" else "$unreadNotificationCount"
                                                )
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (unreadNotificationCount > 0) Icons.Filled.NotificationsActive else Icons.Filled.Notifications,
                                        contentDescription = "Notifications des quiz validés",
                                        tint = qColors.headingInk
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shadowElevation = 0.dp
                        ) {
                            IconButton(
                                onClick = onSwitchProfile,
                                modifier = Modifier.testTag("parent_switch_profile_button")
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ExitToApp,
                                    contentDescription = "Changer de profil",
                                    tint = qColors.bodyInk
                                )
                            }
                        }
                    }
                }
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { showAddChildDialog = true },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Ajouter un enfant") },
                    containerColor = qColors.navyActionContainer,
                    contentColor = qColors.navyActionOnContainer,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.testTag("add_child_fab")
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .widthIn(max = 640.dp),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 92.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Child Selector Pill
                if (focusedChild != null) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                onClick = {
                                    if (children.size > 1) showChildDropdown = true
                                    else selectedChildId = focusedChild.id
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("parent_child_selector_pill"),
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                shadowElevation = 0.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ChildAvatarBadge(
                                        avatarKey = focusedChild.avatarKey,
                                        name = focusedChild.name,
                                        size = 44.dp
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = focusedChild.name,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = qColors.headingInk
                                        )
                                        Text(
                                            text = "${focusedChild.gradeLevel} • ${focusedChild.age} ans",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Filled.ExpandMore,
                                        contentDescription = "Changer d'enfant",
                                        tint = qColors.headingInk,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = showChildDropdown,
                                onDismissRequest = { showChildDropdown = false }
                            ) {
                                children.forEach { c ->
                                    DropdownMenuItem(
                                        text = { Text("${c.name} (${c.gradeLevel})") },
                                        onClick = {
                                            focusedChildId = c.id
                                            showChildDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 2. "Progression des matières" Clean Card with Thick Rounded Bars
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Progression des matières",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = qColors.headingInk
                            )
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("parent_subject_progress_card"),
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 0.dp,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    subjectProgressList.forEach { (subject, pct, hasData) ->
                                        val barColor = when (subject) {
                                            "Français" -> qColors.parentSkyAccent
                                            "Mathématiques", "Sciences" -> ParentMintBar
                                            else -> subjectColor(subject)
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = subjectIcon(subject),
                                                contentDescription = subject,
                                                tint = if (subject == "Français") qColors.parentSkyAccent else MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(26.dp)
                                            )
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = subjectShortLabel(subject),
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = qColors.bodyInk
                                                    )
                                                    Text(
                                                        text = if (hasData) "$pct %" else "--",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = qColors.bodyInk
                                                    )
                                                }
                                                LinearProgressIndicator(
                                                    progress = { if (hasData) pct / 100f else 0f },
                                                    color = barColor,
                                                    trackColor = qColors.parentSkyAccent.copy(alpha = if (qColors.isDark) 0.18f else 0.12f),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(10.dp)
                                                        .clip(RoundedCornerShape(50))
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Bilan pédagogique : Compétences acquises & Lacunes à travailler (pour les parents)
                    item {
                        ParentSkillsRecapCard(
                            child = focusedChild,
                            childLessons = focusedLessons,
                            childAttempts = focusedAttempts
                        )
                    }

                    // 3b. Niveau de langage et maturité des textes IA (configurable par l'adulte)
                    item {
                        ParentLanguageToneCard(
                            child = focusedChild,
                            onSelectTone = { tone ->
                                onUpdateChildLanguageTone(focusedChild.id, tone)
                            }
                        )
                    }

                    // 4. "Réussites de la semaine" Clean Labeled Bar Chart + "Voir les progrès" CTA
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            WeeklyProgressBarChart(attempts = focusedAttempts)
                            Button(
                                onClick = { selectedChildId = focusedChild.id },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .testTag("parent_view_progress_cta"),
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = qColors.navyActionContainer,
                                    contentColor = qColors.navyActionOnContainer
                                )
                            ) {
                                Icon(Icons.Filled.BarChart, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Voir le bilan complet de ${focusedChild.name}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }

                // Global stats row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatMiniCard(
                            label = "Cours déposés",
                            value = "${activeLessons.size}",
                            modifier = Modifier.weight(1f)
                        )
                        StatMiniCard(
                            label = "Quiz faits",
                            value = "${activeAttempts.size}",
                            modifier = Modifier.weight(1f)
                        )
                        StatMiniCard(
                            label = "Réussite moy.",
                            value = if (totalQuestions > 0) "$globalSuccess%" else "--",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Local Room SQLite 3D & AI Studio Telemetry Recap
                item {
                    val best3D = sqlGameScores.maxOfOrNull { it.score } ?: 0
                    ModernCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("parent_sql_telemetry_card"),
                        cornerRadius = 22.dp,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Jeu 3D, Studio IA & Base SQL locale",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = qColors.headingInk
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                StatMiniCard(
                                    label = "Parties 3D",
                                    value = "${sqlGameScores.size}",
                                    modifier = Modifier.weight(1f)
                                )
                                StatMiniCard(
                                    label = "Record 3D",
                                    value = if (best3D > 0) "$best3D" else "--",
                                    modifier = Modifier.weight(1f)
                                )
                                StatMiniCard(
                                    label = "Images IA",
                                    value = "$sqlStudioImagesCount",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (sqlSubjectStats.isNotEmpty()) {
                                Text(
                                    text = "Moyennes SQL : " + sqlSubjectStats.take(3).joinToString(" • ") {
                                        "${it.subject} ${it.avgScore.toInt()}%"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Sober Family Invite Code card
                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = qColors.navyActionContainer,
                        modifier = Modifier.fillMaxWidth(),
                        shadowElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Code Famille : ${family.inviteCode}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = qColors.navyActionOnContainer,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "Pour connecter l'appareil d'un enfant",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = qColors.navyActionOnContainer.copy(alpha = 0.85f)
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.16f)
                            ) {
                                IconButton(
                                    onClick = {
                                        clipboard.setText(AnnotatedString(family.inviteCode))
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Code ${family.inviteCode} copié !")
                                        }
                                    },
                                    modifier = Modifier.testTag("copy_invite_code_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ContentCopy,
                                        contentDescription = "Copier le code famille",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Suivi des enfants (${children.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = qColors.headingInk
                    )
                }

                if (children.isEmpty()) {
                    item {
                        ModernCard(
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Aucun enfant inscrit", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "Appuie sur 'Ajouter un enfant' pour créer son profil et son code PIN.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(children, key = { it.id }) { child ->
                        ModernCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("parent_child_card_${child.id}"),
                            onClick = { selectedChildId = child.id }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ChildAvatarBadge(
                                    avatarKey = child.avatarKey,
                                    name = child.name,
                                    size = 56.dp
                                )
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    val lvl = remember(child.xp) { GamificationCatalog.levelProgress(child.xp) }
                                    Text(text = child.name, style = MaterialTheme.typography.titleLarge)
                                    Text(
                                        text = "${child.gradeLevel} • ${child.age} ans • Niv.${lvl.level} (${child.xp} XP)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${child.lessonsCount} cours • ${child.quizzesTaken} quiz • ${child.weakPoints.size} point${if (child.weakPoints.size > 1) "s" else ""} à travailler",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                SuccessRateGauge(
                                    percent = child.successRatePercent,
                                    hasAttempts = child.totalQuestions > 0,
                                    size = 58.dp
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Filled.ChevronRight, contentDescription = "Voir le détail")
                            }
                        }
                    }
                }

                if (activeAttempts.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Derniers résultats de la famille",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    items(activeAttempts.take(8), key = { it.id }) { attempt ->
                        ModernCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 18.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${attempt.childName} • ${attempt.subject}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = attempt.lessonTitle,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Surface(
                                            color = scoreColor(attempt.percentage.toInt()).copy(alpha = 0.14f),
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Text(
                                                text = "${attempt.score}/${attempt.totalQuestions} (${attempt.percentage}%)",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = scoreColor(attempt.percentage.toInt()),
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                            )
                                        }
                                        if (attempt.xpEarned > 0L) {
                                            Text(
                                                text = "+${attempt.xpEarned} XP",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                if (attempt.weakSkills.isNotEmpty()) {
                                    Text(
                                        text = "À travailler : ${attempt.weakSkills.take(2).joinToString(" • ")}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = qColors.coralInk,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNotificationsDialog) {
        val activeIds = remember(children) { children.map { it.id }.toSet() }
        val activeNotifAttempts = remember(attempts, activeIds) { attempts.filter { it.childId in activeIds } }
        ParentNotificationsDialog(
            attempts = activeNotifAttempts,
            notificationsPermissionGranted = notificationsPermissionGranted,
            onRequestPermission = onRequestNotificationPermission,
            onDismiss = { showNotificationsDialog = false }
        )
    }

    if (showAddChildDialog) {
        ChildProfileFormDialog(
            title = "Nouveau profil enfant",
            existingChild = null,
            onDismiss = { showAddChildDialog = false },
            onSave = { name, age, grade, avatar, pin, toneLevel ->
                if (onAddChildWithTone != null) {
                    onAddChildWithTone(name, age, grade, avatar, pin ?: "1234", toneLevel)
                } else {
                    onAddChild(name, age, grade, avatar, pin ?: "1234")
                }
                showAddChildDialog = false
            }
        )
    }

    editingChild?.let { childToEdit ->
        ChildProfileFormDialog(
            title = "Modifier ${childToEdit.name}",
            existingChild = childToEdit,
            onDismiss = { editingChild = null },
            onSave = { name, age, grade, avatar, pin, toneLevel ->
                if (onEditChildWithTone != null) {
                    onEditChildWithTone(childToEdit.id, name, age, grade, avatar, pin, toneLevel)
                } else {
                    onEditChild(childToEdit.id, name, age, grade, avatar, pin)
                    if (toneLevel != childToEdit.languageToneLevel) {
                        onUpdateChildLanguageTone(childToEdit.id, toneLevel)
                    }
                }
                editingChild = null
            }
        )
    }

    deletingChild?.let { childToDelete ->
        AlertDialog(
            onDismissRequest = { deletingChild = null },
            title = { Text("Supprimer ${childToDelete.name} ?") },
            text = { Text("Cette action supprimera le profil de ${childToDelete.name} de la liste des connexions.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteChild(childToDelete.id)
                        deletingChild = null
                        selectedChildId = null
                    }
                ) {
                    Text("Supprimer", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingChild = null }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ParentSkillsRecapCard(
    child: ChildProfile,
    childLessons: List<Lesson>,
    childAttempts: List<QuizAttempt>
) {
    val weakPoints = remember(child.weakPoints, childLessons, childAttempts) {
        (child.weakPoints + childLessons.flatMap { it.weakPoints } + childAttempts.flatMap { it.weakSkills })
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
    }
    val weakKeys = remember(weakPoints) { weakPoints.map { it.lowercase() }.toSet() }
    val acquiredSkills = remember(child.acquiredSkills, childLessons, childAttempts, weakKeys) {
        (child.acquiredSkills + childLessons.flatMap { it.acquiredSkills } + childAttempts.flatMap { it.masteredSkills })
            .map { it.trim() }
            .filter { it.isNotBlank() && it.lowercase() !in weakKeys }
            .distinctBy { it.lowercase() }
    }

    val qColors = QuizzyTheme.colors

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("parent_skills_recap_card"),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Bilan pédagogique de ${child.name}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = qColors.headingInk
                )
                Text(
                    text = "Récapitulatif des acquis et des lacunes à travailler à la maison",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Lacunes / Ce qu'il faut travailler
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("parent_weak_points_section"),
                shape = RoundedCornerShape(18.dp),
                color = qColors.pastelPeach,
                border = BorderStroke(1.dp, qColors.coralInk.copy(alpha = 0.28f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lightbulb,
                            contentDescription = null,
                            tint = qColors.coralInk,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Ce que ${child.name} doit travailler (${weakPoints.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = qColors.headingInk
                        )
                    }
                    if (weakPoints.isEmpty()) {
                        Text(
                            text = "Aucune lacune détectée pour l'instant. Les exercices, devoirs et quiz permettront d'identifier précisément les notions à renforcer.",
                            style = MaterialTheme.typography.bodySmall,
                            color = qColors.bodyInk
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            weakPoints.take(8).forEach { gap ->
                                Surface(
                                    color = qColors.chipOnTint,
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(1.dp, qColors.coralInk.copy(alpha = 0.25f))
                                ) {
                                    Text(
                                        text = gap,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = qColors.bodyInk,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Compétences acquises
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("parent_acquired_skills_section"),
                shape = RoundedCornerShape(18.dp),
                color = qColors.pastelMint,
                border = BorderStroke(1.dp, qColors.successInk.copy(alpha = 0.25f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = qColors.successInk,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Compétences acquises (${acquiredSkills.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = qColors.headingInk
                        )
                    }
                    if (acquiredSkills.isEmpty()) {
                        Text(
                            text = "Les notions maîtrisées dans les devoirs et les quiz apparaîtront ici.",
                            style = MaterialTheme.typography.bodySmall,
                            color = qColors.bodyInk
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            acquiredSkills.take(8).forEach { skill ->
                                Surface(
                                    color = qColors.chipOnTint,
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(1.dp, qColors.successInk.copy(alpha = 0.25f))
                                ) {
                                    Text(
                                        text = skill,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = qColors.bodyInk,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ParentLanguageToneCard(
    child: ChildProfile,
    onSelectTone: (String) -> Unit
) {
    val qColors = QuizzyTheme.colors
    val activeTone = LanguageToneCatalog.normalize(child.languageToneLevel)
    val activeOption = remember(activeTone) { LanguageToneCatalog.optionFor(activeTone) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("parent_language_tone_card"),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(qColors.parentSkyContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = null,
                        tint = qColors.parentSkyAccent,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Niveau de langage IA (${child.name})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = qColors.headingInk
                    )
                    Text(
                        text = "Par défaut adapté à l'âge (${child.age} ans • ${child.gradeLevel}). Ajuste la maturité des synthèses, conseils et indices :",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LanguageToneCatalog.options.forEach { option ->
                    FilterChip(
                        selected = activeTone == option.id,
                        onClick = { onSelectTone(option.id) },
                        label = {
                            Text(
                                text = option.shortLabel,
                                fontWeight = if (activeTone == option.id) FontWeight.ExtraBold else FontWeight.Medium
                            )
                        },
                        modifier = Modifier.testTag("parent_tone_chip_${option.id}")
                    )
                }
            }

            Surface(
                color = qColors.pastelMint,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("parent_tone_active_description")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Mode actif : ${activeOption.title}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = qColors.headingInk
                    )
                    Text(
                        text = activeOption.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = qColors.bodyInk
                    )
                }
            }
        }
    }
}

@Composable
private fun ParentChildDetailView(
    child: ChildProfile,
    lessons: List<Lesson>,
    attempts: List<QuizAttempt>,
    onBack: () -> Unit,
    onOpenLesson: (Lesson) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onUpdateLanguageTone: (String) -> Unit = {}
) {
    BackHandler(onBack = onBack)

    val bySubject = remember(attempts) {
        attempts.groupBy { it.subject }.mapValues { (_, list) ->
            val correct = list.sumOf { it.score }
            val total = list.sumOf { it.totalQuestions }
            if (total > 0L) ((correct * 100L) / total).toInt() else 0
        }
    }
    val qColors = QuizzyTheme.colors

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(qColors.parentCanvas)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("parent_child_detail_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                    Spacer(Modifier.width(6.dp))
                    ChildAvatarBadge(avatarKey = child.avatarKey, name = child.name, size = 46.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(child.name, style = MaterialTheme.typography.titleLarge)
                        Text(
                            "${child.gradeLevel} • ${child.age} ans",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.testTag("parent_edit_child_button")) {
                        Icon(Icons.Filled.Edit, contentDescription = "Modifier")
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.testTag("parent_delete_child_button")) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Supprimer",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            item {
                ModernCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SuccessRateGauge(
                            percent = child.successRatePercent,
                            hasAttempts = child.totalQuestions > 0,
                            size = 80.dp
                        )
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("Taux de réussite global", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "${child.totalCorrect} bonnes réponses sur ${child.totalQuestions} questions",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${lessons.size} cours/exercices • ${attempts.size} quiz complétés",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            item {
                ParentSkillsRecapCard(
                    child = child,
                    childLessons = lessons,
                    childAttempts = attempts
                )
            }

            item {
                ParentLanguageToneCard(
                    child = child,
                    onSelectTone = onUpdateLanguageTone
                )
            }

            item {
                XpLevelProgressCard(child = child)
            }

            item {
                BadgeShowcaseRow(unlockedIds = child.badges)
            }

            if (bySubject.isNotEmpty()) {
                item {
                    ModernCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Notes & réussite par matière", style = MaterialTheme.typography.titleLarge)
                            bySubject.forEach { (subject, pct) ->
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(subject, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            "$pct%",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = scoreColor(pct)
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { pct / 100f },
                                        color = scoreColor(pct),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text("Cours, exercices & devoirs (${lessons.size})", style = MaterialTheme.typography.titleLarge)
            }

            if (lessons.isEmpty()) {
                item {
                    Text(
                        text = "${child.name} n'a pas encore déposé de cours ou d'exercice.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(lessons, key = { it.id }) { lesson ->
                    ModernCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 18.dp,
                        onClick = { onOpenLesson(lesson) }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    SubjectBadge(subject = lesson.subject)
                                    val docType = SchoolCatalog.normalizeDocumentType(lesson.documentType)
                                    Surface(
                                        color = when (docType) {
                                            "Exercice" -> qColors.pastelYellow
                                            "Devoir" -> qColors.pastelPeach
                                            else -> qColors.pastelMint
                                        },
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text(
                                            text = docType,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = qColors.bodyInk,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(lesson.title, style = MaterialTheme.typography.titleMedium)
                                val bestText = lesson.bestScorePercent?.let { "Meilleur score : $it%" } ?: "Aucun quiz passé"
                                val filesText = if (lesson.attachedFiles.isNotEmpty()) {
                                    " • ${lesson.attachedFiles.size} fichier${if (lesson.attachedFiles.size > 1) "s" else ""}"
                                } else ""
                                Text(
                                    text = "${lesson.quizCount} quiz$filesText • $bestText",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(Icons.Filled.ChevronRight, contentDescription = "Lire la synthèse")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChildProfileFormDialog(
    title: String,
    existingChild: ChildProfile?,
    onDismiss: () -> Unit,
    onSave: (name: String, age: Int, grade: String, avatar: String, pin: String?, languageToneLevel: String) -> Unit
) {
    var name by remember { mutableStateOf(existingChild?.name ?: "") }
    var grade by remember { mutableStateOf(existingChild?.gradeLevel ?: "CM1") }
    var age by remember { mutableIntStateOf(existingChild?.age?.toInt() ?: 9) }
    var avatar by remember { mutableStateOf(existingChild?.avatarKey ?: "owl") }
    var pin by remember { mutableStateOf("") }
    var languageToneLevel by remember {
        mutableStateOf(LanguageToneCatalog.normalize(existingChild?.languageToneLevel))
    }

    val pinValid = if (existingChild == null) pin.length == 4 else (pin.isEmpty() || pin.length == 4)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text(title, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Prénom de l'enfant") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_child_name_input")
                )

                Spacer(Modifier.height(12.dp))
                Text("Niveau scolaire :", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                GradeLevelSelector(
                    selectedGrade = grade,
                    onSelect = {
                        grade = it
                        if (existingChild == null) {
                            age = SchoolCatalog.suggestedAgeForGrade(it)
                        }
                    }
                )

                Spacer(Modifier.height(10.dp))
                Text("Âge : $age ans", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = age.toFloat(),
                    onValueChange = { age = it.roundToInt().coerceIn(5, 18) },
                    valueRange = 5f..18f,
                    steps = 12
                )

                Spacer(Modifier.height(8.dp))
                Text("Avatar :", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                AvatarPickerRow(selectedAvatar = avatar, onSelect = { avatar = it })

                Spacer(Modifier.height(12.dp))
                Text("Niveau de langage & maturité IA :", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    LanguageToneCatalog.options.forEach { option ->
                        FilterChip(
                            selected = languageToneLevel == option.id,
                            onClick = { languageToneLevel = option.id },
                            label = { Text(option.shortLabel) },
                            modifier = Modifier.testTag("dialog_child_tone_${option.id}")
                        )
                    }
                }
                Text(
                    text = LanguageToneCatalog.optionFor(languageToneLevel).description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) pin = it },
                    label = {
                        Text(
                            if (existingChild == null) "Code PIN enfant (4 chiffres)"
                            else "Nouveau code PIN (laisser vide si inchangé)"
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_child_pin_input")
                )

                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Annuler")
                    }
                    Button(
                        onClick = { onSave(name, age, grade, avatar, pin.ifBlank { null }, languageToneLevel) },
                        enabled = name.isNotBlank() && pinValid,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dialog_save_child_button")
                    ) {
                        Text("Valider")
                    }
                }
            }
        }
    }
}

@Composable
private fun ParentNotificationsDialog(
    attempts: List<QuizAttempt>,
    notificationsPermissionGranted: Boolean,
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("parent_notifications_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Notifications des quiz",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!notificationsPermissionGranted) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Active les notifications Android pour être alerté automatiquement à chaque quiz validé par un enfant.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Button(
                                onClick = onRequestPermission,
                                modifier = Modifier.testTag("enable_notifications_button")
                            ) {
                                Text("Activer les notifications")
                            }
                        }
                    }
                }

                if (attempts.isEmpty()) {
                    Text(
                        text = "Aucun quiz validé pour le moment. Tu recevras une notification dès qu'un enfant terminera un quiz.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    attempts.take(15).forEach { attempt ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Quiz validé par ${attempt.childName} !",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${attempt.subject} • ${attempt.lessonTitle}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Score : ${attempt.score}/${attempt.totalQuestions} (${attempt.percentage} %)" +
                                        if (attempt.xpEarned > 0L) " • +${attempt.xpEarned} XP" else "",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = scoreColor(attempt.percentage.toInt()),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = onDismiss) {
                        Text("Fermer")
                    }
                }
            }
        }
    }
}

