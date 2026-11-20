package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.CourseAttachment
import com.example.data.FrenchErrorMessages
import com.example.data.GeminiTutorService
import com.example.model.GamificationCatalog
import com.example.model.Lesson
import com.example.model.Quiz
import com.example.model.SchoolCatalog
import com.example.ui.components.QuizzyInlineLoader
import com.example.ui.components.AttachedDocumentChipRow
import com.example.ui.components.AttachedDocumentsCard
import com.example.ui.components.DifficultyMultiplierChip
import com.example.ui.components.LessonHeroIllustrationCard
import com.example.ui.components.ModernCard
import com.example.ui.components.scoreColor
import com.example.ui.components.subjectColor
import com.example.ui.components.subjectPastelSurface
import com.example.ui.theme.QuizzyTheme
import com.example.ui.viewmodel.OperationStatus
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LessonDetailScreen(
    lesson: Lesson,
    quizzes: List<Quiz>,
    isReadOnlyParent: Boolean,
    opStatus: OperationStatus,
    onBack: () -> Unit,
    onStartQuiz: (Quiz) -> Unit,
    onGenerateNewQuiz: (String) -> Unit,
    onAddDocumentsToLesson: ((bitmap: Bitmap?, attachments: List<CourseAttachment>, notes: String) -> Unit)? = null
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedDifficulty by remember { mutableStateOf("Moyen") }
    var pendingAttachments by remember { mutableStateOf<List<CourseAttachment>>(emptyList()) }
    var pendingBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pendingNotes by remember { mutableStateOf("") }
    var fileLoadError by remember { mutableStateOf<String?>(null) }
    var showAddFilesPanel by remember { mutableStateOf(false) }

    val isBusy = opStatus is OperationStatus.Busy
    val qColors = QuizzyTheme.colors
    val accent = subjectColor(lesson.subject)
    val pastelTint = subjectPastelSurface(lesson.subject)
    val importantWords = remember(lesson.title, lesson.keyPoints, lesson.summaryIntro) {
        extractImportantKeywords(lesson)
    }

    LaunchedEffect(opStatus) {
        if (opStatus is OperationStatus.Success) {
            pendingAttachments = emptyList()
            pendingBitmap = null
            pendingNotes = ""
            fileLoadError = null
            showAddFilesPanel = false
        }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                fileLoadError = null
                val loaded = mutableListOf<CourseAttachment>()
                val errors = mutableListOf<String>()
                uris.forEach { uri ->
                    GeminiTutorService.loadCourseAttachmentFromUri(context, uri).fold(
                        onSuccess = { loaded.add(it) },
                        onFailure = { e -> errors.add(FrenchErrorMessages.toFrenchError(e, "Fichier illisible.")) }
                    )
                }
                if (loaded.isNotEmpty()) {
                    pendingAttachments = (pendingAttachments + loaded).take(5)
                    showAddFilesPanel = true
                }
                if (errors.isNotEmpty()) {
                    fileLoadError = errors.first()
                }
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                pendingBitmap = GeminiTutorService.loadBitmapFromUri(context, uri)
                showAddFilesPanel = true
            }
        }
    }

    // Soft pastel lavender/subject-tinted header backdrop fading smoothly into theme background
    val topBackdrop = if (lesson.subject == "Sciences") qColors.lessonBackdrop else pastelTint

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to topBackdrop,
                        0.32f to topBackdrop.copy(alpha = 0.45f),
                        0.55f to MaterialTheme.colorScheme.background,
                        1.0f to MaterialTheme.colorScheme.background
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Centered sober header bar + expressive title
            item {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shadowElevation = 0.dp
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.testTag("lesson_detail_back")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Retour",
                                    tint = qColors.headingInk
                                )
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = lesson.subject,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = qColors.bodyInk,
                                textAlign = TextAlign.Center
                            )
                            val docTypeLabel = SchoolCatalog.normalizeDocumentType(lesson.documentType)
                            Surface(
                                color = when (docTypeLabel) {
                                    "Exercice" -> qColors.pastelYellow
                                    "Devoir" -> qColors.pastelPeach
                                    else -> qColors.pastelMint
                                },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.testTag("lesson_detail_doctype_pill")
                            ) {
                                Text(
                                    text = docTypeLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = qColors.bodyInk,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }
                        }
                        // Balanced spacer to keep subject title optically centered
                        Spacer(Modifier.size(48.dp))
                    }

                    Text(
                        text = lesson.title,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = qColors.headingInk
                    )
                }
            }

            // Friendly pastel illustration banner
            item {
                LessonHeroIllustrationCard(
                    subject = lesson.subject,
                    gradeLabel = "Adapté pour ${lesson.gradeLevel} (${lesson.childAge} ans)"
                )
            }

            if (opStatus is OperationStatus.Error) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = opStatus.message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            if (opStatus is OperationStatus.Success) {
                item {
                    Surface(
                        color = qColors.successInk.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = opStatus.message,
                            color = qColors.successInk,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            // Block 1: L'idée principale
            item {
                ModernCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 26.dp,
                    borderColor = accent.copy(alpha = 0.25f)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(pastelTint),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = "L'idée principale",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = qColors.headingInk
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = lesson.summaryIntro,
                            style = MaterialTheme.typography.bodyLarge,
                            color = qColors.bodyInk
                        )
                    }
                }
            }

            // Block 2: À retenir (Checkmark list matching center reference mockup!)
            item {
                ModernCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 26.dp
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "À retenir",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = qColors.headingInk
                        )
                        lesson.keyPoints.forEach { point ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(accent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = if (qColors.isDark) Color(0xFF0D1526) else Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = point,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = qColors.bodyInk,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Prominent "Faire le quiz" CTA pill
            if (!isReadOnlyParent && quizzes.isNotEmpty()) {
                item {
                    Button(
                        onClick = { onStartQuiz(quizzes.first()) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .testTag("lesson_primary_quiz_cta"),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = qColors.navyActionContainer,
                            contentColor = qColors.navyActionOnContainer
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Faire le quiz",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // Block 3: Les mots importants
            if (importantWords.isNotEmpty()) {
                item {
                    ModernCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 24.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(pastelTint),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Tag,
                                        contentDescription = null,
                                        tint = accent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = "Les mots importants",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = qColors.headingInk
                                )
                            }
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                importantWords.forEach { word ->
                                    Surface(
                                        color = pastelTint,
                                        shape = RoundedCornerShape(50),
                                        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f))
                                    ) {
                                        Text(
                                            text = word,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = qColors.bodyInk,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Block 4: Explication & Un exemple
            item {
                ModernCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 24.dp
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = "Un exemple & explication",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = qColors.headingInk
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = lesson.simpleExplanation,
                            style = MaterialTheme.typography.bodyLarge,
                            color = qColors.bodyInk
                        )
                    }
                }
            }

            // Memory Tip (soft sunny-yellow highlight card)
            if (lesson.memoryTip.isNotBlank()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = qColors.pastelYellow,
                        border = BorderStroke(1.dp, qColors.warningInk.copy(alpha = 0.30f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(qColors.chipOnTint),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Lightbulb,
                                    contentDescription = null,
                                    tint = qColors.warningInk,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Astuce pour retenir",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = qColors.headingInk,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = lesson.memoryTip,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = qColors.bodyInk
                                )
                            }
                        }
                    }
                }
            }

            // Bilan des compétences & lacunes (especially helpful for Exercices & Devoirs)
            if (lesson.evaluationSummary.isNotBlank() || lesson.acquiredSkills.isNotEmpty() || lesson.weakPoints.isNotEmpty()) {
                item {
                    ModernCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("lesson_skills_evaluation_card"),
                        cornerRadius = 24.dp,
                        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(qColors.pastelMint),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = "Bilan des compétences",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = qColors.headingInk
                                )
                            }

                            if (lesson.evaluationSummary.isNotBlank()) {
                                Text(
                                    text = lesson.evaluationSummary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = qColors.bodyInk
                                )
                            }

                            if (lesson.acquiredSkills.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = null,
                                            tint = qColors.successInk,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Notions acquises",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = qColors.successInk
                                        )
                                    }
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        lesson.acquiredSkills.forEach { skill ->
                                            Surface(
                                                color = qColors.pastelMint,
                                                shape = RoundedCornerShape(50),
                                                border = BorderStroke(1.dp, qColors.successInk.copy(alpha = 0.28f))
                                            ) {
                                                Text(
                                                    text = skill,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = qColors.bodyInk,
                                                    fontWeight = FontWeight.SemiBold,
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (lesson.weakPoints.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Lightbulb,
                                            contentDescription = null,
                                            tint = qColors.coralInk,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Points à travailler",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = qColors.coralInk
                                        )
                                    }
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        lesson.weakPoints.forEach { gap ->
                                            Surface(
                                                color = qColors.pastelPeach,
                                                shape = RoundedCornerShape(50),
                                                border = BorderStroke(1.dp, qColors.coralInk.copy(alpha = 0.28f))
                                            ) {
                                                Text(
                                                    text = gap,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = qColors.bodyInk,
                                                    fontWeight = FontWeight.SemiBold,
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
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

            if (lesson.attachedFiles.isNotEmpty()) {
                item {
                    AttachedDocumentsCard(documents = lesson.attachedFiles)
                }
            }

            if (!isReadOnlyParent && onAddDocumentsToLesson != null) {
                item {
                    ModernCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("lesson_add_documents_card"),
                        cornerRadius = 24.dp,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.UploadFile,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Déposer des fichiers dans ce cours",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "PDF, Word (DOCX), Texte, CSV, Images...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { documentPickerLauncher.launch(arrayOf("*/*")) },
                                    enabled = !isBusy,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("lesson_pick_files_button"),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Icon(Icons.Filled.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("PDF / Fichier", maxLines = 1)
                                }
                                OutlinedButton(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    enabled = !isBusy,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("lesson_pick_photo_button"),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Image", maxLines = 1)
                                }
                            }

                            if (!fileLoadError.isNullOrBlank()) {
                                Text(
                                    text = fileLoadError.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }

                            if (showAddFilesPanel || pendingAttachments.isNotEmpty() || pendingBitmap != null || pendingNotes.isNotBlank()) {
                                pendingAttachments.forEachIndexed { idx, att ->
                                    AttachedDocumentChipRow(
                                        fileName = att.fileName,
                                        mimeType = att.mimeType,
                                        sizeBytes = att.sizeBytes,
                                        onRemove = if (!isBusy) {
                                            { pendingAttachments = pendingAttachments.filterIndexed { i, _ -> i != idx } }
                                        } else null
                                    )
                                }

                                pendingBitmap?.let { bmp ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(140.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                    ) {
                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = "Image ajoutée",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        IconButton(
                                            onClick = { pendingBitmap = null },
                                            enabled = !isBusy,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(6.dp)
                                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), CircleShape)
                                        ) {
                                            Icon(Icons.Filled.Close, contentDescription = "Retirer l'image")
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = pendingNotes,
                                    onValueChange = { pendingNotes = it },
                                    placeholder = { Text("Précision optionnelle sur ce fichier...") },
                                    minLines = 2,
                                    maxLines = 4,
                                    enabled = !isBusy,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Button(
                                    onClick = {
                                        onAddDocumentsToLesson(pendingBitmap, pendingAttachments, pendingNotes)
                                    },
                                    enabled = !isBusy && (pendingBitmap != null || pendingAttachments.isNotEmpty() || pendingNotes.isNotBlank()),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("lesson_enrich_submit_button"),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    if (isBusy) {
                                        QuizzyInlineLoader(
                                            size = 20.dp,
                                            tint = MaterialTheme.colorScheme.onPrimary
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text("Analyse et mise à jour...")
                                    } else {
                                        Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Enrichir le cours & créer un quiz")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Quizzes section
            item {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Quiz sur ce cours (${quizzes.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = qColors.headingInk
                )
            }

            items(quizzes, key = { it.id }) { quiz ->
                ModernCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("lesson_quiz_card_${quiz.id}"),
                    cornerRadius = 22.dp,
                    onClick = if (!isReadOnlyParent) { { onStartQuiz(quiz) } } else null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(pastelTint),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Quiz,
                                contentDescription = null,
                                tint = accent
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = quiz.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
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
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                            if (quiz.targetedGaps.isNotEmpty()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Cible : ${quiz.targetedGaps.take(2).joinToString(" • ")}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = qColors.coralInk,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            val scoreLabel = if (quiz.isAlreadyCompleted) {
                                val pctText = quiz.lastScorePercent?.let { "$it%" } ?: "fait"
                                "Dernier score : $pctText • Entraînement (0 XP)"
                            } else {
                                "Nouveau • +XP à gagner"
                            }
                            Text(
                                text = "${quiz.questions.size} questions • $scoreLabel",
                                style = MaterialTheme.typography.bodySmall,
                                color = quiz.lastScorePercent?.let { scoreColor(it.toInt()) }
                                    ?: MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (!isReadOnlyParent) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(qColors.navyActionContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = "Lancer le quiz",
                                    tint = qColors.navyActionOnContainer,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (!isReadOnlyParent) {
                item {
                    ModernCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 24.dp,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "Envie de t'entraîner encore ?",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = qColors.headingInk
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Choisis un niveau et génère un nouveau quiz sur ce cours :",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(10.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                SchoolCatalog.difficulties.forEach { diff ->
                                    val label = SchoolCatalog.difficultyMultiplierLabel(diff)
                                    FilterChip(
                                        selected = selectedDifficulty == diff,
                                        onClick = { selectedDifficulty = diff },
                                        label = { Text(label) },
                                        modifier = Modifier.testTag("lesson_difficulty_chip_$diff")
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = { onGenerateNewQuiz(selectedDifficulty) },
                                enabled = !isBusy,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("generate_more_quiz_button"),
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                if (isBusy) {
                                    QuizzyInlineLoader(
                                        size = 20.dp,
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text("Génération en cours...")
                                } else {
                                    Icon(Icons.Filled.AddCircle, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Générer un nouveau quiz (${SchoolCatalog.difficultyMultiplierLabel(selectedDifficulty)})")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private val frenchStopWords = setOf(
    "les", "des", "une", "dans", "pour", "avec", "sur", "par", "est", "sont", "qui", "que",
    "elle", "elles", "ils", "nous", "vous", "leur", "leurs", "notre", "votre", "cette", "cet",
    "mais", "donc", "comme", "plus", "moins", "très", "tout", "tous", "toute", "toutes", "entre",
    "vers", "chez", "sans", "sous", "après", "avant", "aussi", "ainsi", "alors", "quand", "être",
    "avoir", "faire", "peut", "font", "permet", "chaque", "autres", "autre", "cours", "leçon"
)

private fun extractImportantKeywords(lesson: Lesson): List<String> {
    val combined = (listOf(lesson.title, lesson.summaryIntro) + lesson.keyPoints).joinToString(" ")
    val tokens = Regex("[\\p{L}]{5,}")
        .findAll(combined)
        .map { it.value.lowercase() }
        .filter { it !in frenchStopWords }
        .distinct()
        .take(6)
        .map { token -> token.replaceFirstChar { it.uppercase() } }
        .toList()
    return tokens
}
