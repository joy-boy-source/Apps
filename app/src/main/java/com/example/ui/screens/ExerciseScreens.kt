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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.data.CourseAttachment
import com.example.data.FrenchErrorMessages
import com.example.data.GeminiTutorService
import com.example.model.ChildProfile
import com.example.model.ExerciseFinalEvaluation
import com.example.model.ExerciseItem
import com.example.model.ExerciseSheet
import com.example.model.SchoolCatalog
import com.example.ui.components.AttachedDocumentChipRow
import com.example.ui.components.ModernCard
import com.example.ui.components.QuizResultFeedbackAnimation
import com.example.ui.components.QuizzyAnimatedMascot
import com.example.ui.components.QuizzyInlineLoader
import com.example.ui.components.QuizzyMascotMood
import com.example.ui.components.SubjectBadge
import com.example.ui.components.VoiceMicButton
import com.example.ui.components.scoreColor
import com.example.ui.theme.QuizzyTheme
import com.example.ui.viewmodel.OperationStatus
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChildExercisesTab(
    child: ChildProfile,
    exerciseSheets: List<ExerciseSheet>,
    selectedSubject: String,
    onSelectSubject: (String) -> Unit,
    onOpenExercise: (ExerciseSheet) -> Unit,
    onOpenDepositExercise: () -> Unit,
    onDeleteExercise: (ExerciseSheet) -> Unit
) {
    val qColors = QuizzyTheme.colors
    val subjectsInSheets = remember(exerciseSheets) {
        listOf("Toutes") + exerciseSheets.map { it.subject }.distinct().sorted()
    }
    val filteredSheets = remember(exerciseSheets, selectedSubject) {
        if (selectedSubject == "Toutes") exerciseSheets
        else exerciseSheets.filter { it.subject.equals(selectedSubject, ignoreCase = true) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 600.dp)
            .testTag("child_exercises_tab_list"),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero banner explaining verbatim exercise recreation + instant AI per-answer correction
        item {
            ModernCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("exercise_hero_card"),
                cornerRadius = 26.dp,
                containerColor = qColors.pastelMint
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuizzyAnimatedMascot(
                            mood = QuizzyMascotMood.THINKING,
                            size = 64.dp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Mes exercices à l'identique",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = qColors.headingInk
                            )
                            Text(
                                text = "Photographie ou dépose un exercice non fait : Quizzy le reproduit exactement tel quel et corrige chaque réponse instantanément !",
                                style = MaterialTheme.typography.bodySmall,
                                color = qColors.bodyInk
                            )
                        }
                    }

                    Button(
                        onClick = onOpenDepositExercise,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 50.dp)
                            .testTag("exercise_open_deposit_button"),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = qColors.navyActionContainer,
                            contentColor = qColors.navyActionOnContainer
                        )
                    ) {
                        Icon(Icons.Filled.AddAPhoto, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Déposer un exercice non fait",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        if (subjectsInSheets.size > 1) {
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(subjectsInSheets, key = { it }) { subject ->
                        FilterChip(
                            selected = selectedSubject == subject,
                            onClick = { onSelectSubject(subject) },
                            label = { Text(subject) },
                            modifier = Modifier.testTag("exercise_filter_$subject")
                        )
                    }
                }
            }
        }

        if (filteredSheets.isEmpty()) {
            item {
                ModernCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("exercise_empty_state_card"),
                    cornerRadius = 24.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuizzyAnimatedMascot(
                            mood = QuizzyMascotMood.WAVE,
                            size = 92.dp
                        )
                        Text(
                            text = "Aucun exercice déposé pour le moment",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = qColors.headingInk,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Ajoute une photo ou un document d'un exercice de ${child.gradeLevel} : chaque question texte ou QCM sera reproduite à l'identique avec correction IA détaillée.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filteredSheets, key = { it.id }) { sheet ->
                ModernCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("exercise_sheet_card_${sheet.id}"),
                    cornerRadius = 22.dp,
                    onClick = { onOpenExercise(sheet) }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            SubjectBadge(subject = sheet.subject)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                sheet.finalEvaluation?.let { eval ->
                                    val badgeColor = scoreColor(eval.percentage)
                                    Surface(
                                        color = badgeColor.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(50),
                                        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.42f)),
                                        modifier = Modifier.testTag("exercise_card_score_${sheet.id}")
                                    ) {
                                        Text(
                                            text = eval.formattedScoreOutOf20,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = badgeColor,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                Surface(
                                    color = qColors.pastelYellow,
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = "${sheet.gradedCount}/${sheet.items.size} validées",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = qColors.bodyInk,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteExercise(sheet) },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("exercise_delete_${sheet.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = "Supprimer cet exercice",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = sheet.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = qColors.headingInk,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (sheet.generalInstructions.isNotBlank()) {
                            Text(
                                text = sheet.generalInstructions,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (sheet.correctCount > 0) {
                                Surface(color = qColors.pastelMint, shape = RoundedCornerShape(50)) {
                                    Text(
                                        text = "${sheet.correctCount} correcte${if (sheet.correctCount > 1) "s" else ""}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = qColors.successInk,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            if (sheet.partialCount > 0) {
                                Surface(color = qColors.pastelYellow, shape = RoundedCornerShape(50)) {
                                    Text(
                                        text = "${sheet.partialCount} partielle${if (sheet.partialCount > 1) "s" else ""}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = qColors.warningInk,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            if (sheet.incorrectCount > 0) {
                                Surface(color = qColors.pastelPeach, shape = RoundedCornerShape(50)) {
                                    Text(
                                        text = "${sheet.incorrectCount} à revoir",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = qColors.coralInk,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(50)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (sheet.gradedCount > 0) "Continuer l'exercice" else "Faire l'exercice",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onPrimary
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
fun DepositExerciseDialog(
    child: ChildProfile,
    opStatus: OperationStatus,
    isTranscribing: Boolean,
    transcribeError: String?,
    onTranscribeAudio: (bytes: ByteArray, onResult: (String) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: (
        subject: String,
        bitmap: Bitmap?,
        attachments: List<CourseAttachment>,
        notes: String
    ) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedSubject by remember { mutableStateOf(SchoolCatalog.AUTO_SUBJECT) }
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
            val uri = createTempExercisePhotoUri(context)
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
                                text = "Déposer un exercice non fait",
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
                            text = "Reproduction exacte à l'identique + correction IA rapide pour ${child.name} (${child.gradeLevel})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .testTag("deposit_exercise_scroll_container")
                            .verticalScroll(scrollState)
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        Text("1. Matière de l'exercice", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Sélectionne la matière de l'exercice ou laisse « Classement auto ».",
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
                                modifier = Modifier.testTag("exercise_subject_chip_Auto")
                            )
                            SchoolCatalog.subjects.forEach { subj ->
                                FilterChip(
                                    selected = selectedSubject == subj,
                                    onClick = { selectedSubject = subj },
                                    label = { Text(subj) },
                                    modifier = Modifier.testTag("exercise_subject_chip_$subj")
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        Text("2. Photo ou document de l'exercice", style = MaterialTheme.typography.titleMedium)
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
                                        val uri = createTempExercisePhotoUri(context)
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
                                    .testTag("exercise_take_photo_button"),
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
                                    .testTag("exercise_pick_photo_button"),
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
                                .testTag("exercise_pick_document_button"),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Filled.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Fichier (PDF, Word, Texte, Image...)")
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
                                    contentDescription = "Photo de l'exercice sélectionnée",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                IconButton(
                                    onClick = { selectedBitmap = null },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), CircleShape)
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
                                text = "3. Énoncé texte ou précision (optionnel)",
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
                                modifier = Modifier.testTag("exercise_mic_button")
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            placeholder = { Text("Ex : Colle ici l'énoncé de l'exercice ou ajoute une consigne...") },
                            minLines = 3,
                            maxLines = 6,
                            enabled = !isBusy,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("exercise_notes_input")
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
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("exercise_deposit_error_card")
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = opStatus.message,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            onSubmit(selectedSubject, selectedBitmap, selectedAttachments, notes)
                                        },
                                        modifier = Modifier.testTag("exercise_deposit_retry_button"),
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Réessayer la génération")
                                    }
                                }
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
                                    size = 24.dp,
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

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Button(
                            onClick = { onSubmit(selectedSubject, selectedBitmap, selectedAttachments, notes) },
                            enabled = !isBusy && (selectedBitmap != null || selectedAttachments.isNotEmpty() || notes.isNotBlank()),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .testTag("submit_exercise_deposit_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            if (isBusy) {
                                QuizzyInlineLoader(
                                    size = 24.dp,
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.width(10.dp))
                                Text("Reproduction à l'identique...")
                            } else {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Générer l'exercice à l'identique")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExercisePlayerScreen(
    sheet: ExerciseSheet,
    gradingItemKeys: Set<String>,
    onBack: () -> Unit,
    onValidateItemAnswer: (sheet: ExerciseSheet, item: ExerciseItem, answer: String) -> Unit,
    isFinalizing: Boolean = false,
    onFinalizeExercise: (sheet: ExerciseSheet, draftAnswersByItemId: Map<String, String>) -> Unit = { _, _ -> }
) {
    BackHandler(onBack = onBack)
    val qColors = QuizzyTheme.colors

    val draftAnswersByItemId = remember(sheet.id) {
        mutableStateMapOf<String, String>().apply {
            sheet.items.forEach { put(it.id, it.studentAnswer) }
        }
    }
    LaunchedEffect(sheet.items) {
        sheet.items.forEach { item ->
            if (item.id !in draftAnswersByItemId || (draftAnswersByItemId[item.id].isNullOrBlank() && item.studentAnswer.isNotBlank())) {
                draftAnswersByItemId[item.id] = item.studentAnswer
            }
        }
    }

    val liveAnsweredCount = sheet.items.count { item ->
        val draft = draftAnswersByItemId[item.id] ?: item.studentAnswer
        draft.isNotBlank()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 620.dp)
                .align(Alignment.TopCenter)
                .testTag("exercise_player_list"),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("exercise_player_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Retour aux exercices",
                                tint = qColors.headingInk
                            )
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        SubjectBadge(subject = sheet.subject)
                    }
                    sheet.finalEvaluation?.let { eval ->
                        val evalColor = scoreColor(eval.percentage)
                        Surface(
                            color = evalColor.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, evalColor.copy(alpha = 0.42f))
                        ) {
                            Text(
                                text = eval.formattedScoreOutOf20,
                                style = MaterialTheme.typography.labelLarge,
                                color = evalColor,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                    Surface(
                        color = qColors.pastelMint,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                    ) {
                        Text(
                            text = "${sheet.gradedCount}/${sheet.items.size} validées",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Title & Verbatim General Instructions
            item {
                ModernCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 24.dp
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = sheet.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = qColors.headingInk
                        )
                        if (sheet.generalInstructions.isNotBlank()) {
                            Surface(
                                color = qColors.pastelYellow,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, qColors.warningInk.copy(alpha = 0.25f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Consigne de l'exercice",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = qColors.headingInk,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = sheet.generalInstructions,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = qColors.bodyInk
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Individual Verbatim Exercise Items
            items(sheet.items, key = { it.id }) { item ->
                val itemKey = "${sheet.id}:${item.id}"
                val isGrading = itemKey in gradingItemKeys
                val itemIndex = sheet.items.indexOf(item).coerceAtLeast(0)

                ExerciseItemInteractiveCard(
                    item = item,
                    itemIndex = itemIndex,
                    isGrading = isGrading || isFinalizing,
                    onDraftChanged = { updated ->
                        draftAnswersByItemId[item.id] = updated
                    },
                    onSubmitAnswer = { answer ->
                        draftAnswersByItemId[item.id] = answer
                        onValidateItemAnswer(sheet, item, answer)
                    }
                )
            }

            // Final Exercise Validation & Pragmatic /20 AI Grading Section
            item(key = "exercise_final_validation_section") {
                ModernCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("exercise_finalize_section"),
                    cornerRadius = 24.dp,
                    containerColor = qColors.pastelMint
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
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.EmojiEvents,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Valider l'exercice complet (Note /20)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = qColors.headingInk
                                )
                                Text(
                                    text = "$liveAnsweredCount/${sheet.items.size} question${if (sheet.items.size > 1) "s" else ""} renseignée${if (liveAnsweredCount > 1) "s" else ""} • Bilan objectif, conseils et note sur 20 par l'IA.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = qColors.bodyInk
                                )
                            }
                        }

                        Button(
                            onClick = {
                                onFinalizeExercise(sheet, draftAnswersByItemId.toMap())
                            },
                            enabled = !isFinalizing && liveAnsweredCount > 0,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 54.dp)
                                .testTag("exercise_finalize_button"),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = qColors.navyActionContainer,
                                contentColor = qColors.navyActionOnContainer
                            )
                        ) {
                            if (isFinalizing) {
                                QuizzyInlineLoader(
                                    size = 24.dp,
                                    tint = qColors.navyActionOnContainer
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "Notation IA sur 20 en cours...",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.EmojiEvents,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (sheet.finalEvaluation != null) {
                                        "Recalculer ma note /20"
                                    } else {
                                        "Valider mon exercice et voir ma note /20"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        if (!sheet.finalEvaluationError.isNullOrBlank() && !isFinalizing) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("exercise_finalize_error_card")
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = sheet.finalEvaluationError,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    OutlinedButton(
                                        onClick = { onFinalizeExercise(sheet, draftAnswersByItemId.toMap()) },
                                        modifier = Modifier.testTag("exercise_finalize_retry_button"),
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Réessayer la notation")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Final /20 Evaluation Report Card (visible once graded)
            sheet.finalEvaluation?.let { evaluation ->
                item(key = "exercise_final_evaluation_report") {
                    ExerciseFinalEvaluationCard(evaluation = evaluation)
                }
            }

            item {
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ExerciseFinalEvaluationCard(
    evaluation: ExerciseFinalEvaluation
) {
    val qColors = QuizzyTheme.colors
    val accentColor = scoreColor(evaluation.percentage)
    val isSuccessScore = evaluation.scoreOutOf20 >= 12.0
    val isPartialScore = evaluation.scoreOutOf20 in 8.0..11.99
    val appreciation = when {
        evaluation.scoreOutOf20 >= 18.0 -> "Excellent travail"
        evaluation.scoreOutOf20 >= 15.0 -> "Très bon résultat"
        evaluation.scoreOutOf20 >= 12.0 -> "Bon travail"
        evaluation.scoreOutOf20 >= 9.0 -> "Ensemble encourageant"
        else -> "Des bases à consolider"
    }

    ModernCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("exercise_final_evaluation_card"),
        cornerRadius = 26.dp,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuizzyAnimatedMascot(
                    mood = when {
                        isSuccessScore -> QuizzyMascotMood.VICTORY
                        isPartialScore -> QuizzyMascotMood.THINKING
                        else -> QuizzyMascotMood.ENCOURAGE
                    },
                    size = 74.dp
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Bilan de ton exercice",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = qColors.headingInk
                    )
                    Text(
                        text = appreciation,
                        style = MaterialTheme.typography.bodySmall,
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    color = accentColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.55f)),
                    modifier = Modifier.testTag("exercise_final_score_badge")
                ) {
                    Text(
                        text = evaluation.formattedScoreOutOf20,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }

            // Overall pragmatic assessment
            Surface(
                color = qColors.pastelMint,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = evaluation.overallAssessment,
                    style = MaterialTheme.typography.bodyMedium,
                    color = qColors.bodyInk,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(14.dp)
                )
            }

            // Strengths
            if (evaluation.strengths.isNotEmpty()) {
                Surface(
                    color = qColors.pastelMint,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, qColors.successInk.copy(alpha = 0.28f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("exercise_strengths_section")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                text = "Points forts",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = qColors.headingInk
                            )
                        }
                        evaluation.strengths.forEach { strength ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("•", color = qColors.successInk, fontWeight = FontWeight.Bold)
                                Text(
                                    text = strength,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = qColors.bodyInk
                                )
                            }
                        }
                    }
                }
            }

            // Improvement axes — rendered ONLY when necessary (non-empty)
            if (evaluation.improvementAxes.isNotEmpty()) {
                Surface(
                    color = qColors.pastelPeach,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, qColors.coralInk.copy(alpha = 0.30f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("exercise_improvement_axes_section")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.TrendingUp,
                                contentDescription = null,
                                tint = qColors.coralInk,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Axes d'amélioration",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = qColors.headingInk
                            )
                        }
                        evaluation.improvementAxes.forEach { axis ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("•", color = qColors.coralInk, fontWeight = FontWeight.Bold)
                                Text(
                                    text = axis,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = qColors.bodyInk
                                )
                            }
                        }
                    }
                }
            }

            // Practical advice
            if (evaluation.practicalAdvice.isNotBlank()) {
                Surface(
                    color = qColors.pastelYellow,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, qColors.warningInk.copy(alpha = 0.32f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("exercise_practical_advice_section")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                text = "Conseil pratique",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = qColors.headingInk
                            )
                        }
                        Text(
                            text = evaluation.practicalAdvice,
                            style = MaterialTheme.typography.bodyMedium,
                            color = qColors.bodyInk
                        )
                    }
                }
            }

            // Warm encouragement footer
            if (evaluation.encouragement.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("exercise_encouragement_banner")
                ) {
                    Text(
                        text = evaluation.encouragement,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = qColors.headingInk,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ExerciseItemInteractiveCard(
    item: ExerciseItem,
    itemIndex: Int,
    isGrading: Boolean,
    onDraftChanged: (String) -> Unit = {},
    onSubmitAnswer: (String) -> Unit
) {
    val qColors = QuizzyTheme.colors
    var draftAnswer by remember(item.id, item.studentAnswer) {
        mutableStateOf(item.studentAnswer)
    }
    val initialDecoded = remember(item.id, item.studentAnswer, item.options) {
        ExerciseItem.decodeSelectedMcqOptions(item.studentAnswer, item.options)
    }
    var multiSelectMode by remember(item.id, item.isMultiSelectMcq) {
        mutableStateOf(item.isMultiSelectMcq || initialDecoded.size > 1)
    }

    ModernCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("exercise_item_card_${item.id}"),
        cornerRadius = 22.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    color = qColors.pastelMint,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "Question ${item.numberLabel}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (item.isMcq) {
                        FilterChip(
                            selected = multiSelectMode,
                            onClick = {
                                if (!isGrading) {
                                    multiSelectMode = !multiSelectMode
                                    if (!multiSelectMode) {
                                        val first = ExerciseItem.decodeSelectedMcqOptions(draftAnswer, item.options).firstOrNull().orEmpty()
                                        draftAnswer = first
                                        onDraftChanged(first)
                                    }
                                }
                            },
                            enabled = !isGrading,
                            label = {
                                Text(
                                    text = "Plusieurs choix",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            modifier = Modifier.testTag("exercise_mcq_multi_toggle_${item.id}")
                        )
                    } else {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "Réponse texte",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Text(
                text = item.prompt,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = qColors.headingInk
            )

            if (item.isMcq) {
                val selectedOptions = remember(draftAnswer, item.options) {
                    ExerciseItem.decodeSelectedMcqOptions(draftAnswer, item.options)
                }
                if (multiSelectMode) {
                    Surface(
                        color = qColors.pastelYellow,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, qColors.warningInk.copy(alpha = 0.30f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("exercise_mcq_multi_hint_${item.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckBox,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Plusieurs réponses possibles — coche toutes celles qui conviennent",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = qColors.headingInk
                            )
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item.options.forEachIndexed { optIdx, optionText ->
                        val isSelected = if (multiSelectMode) {
                            optionText in selectedOptions
                        } else {
                            draftAnswer == optionText
                        }
                        Surface(
                            onClick = {
                                if (!isGrading) {
                                    val nextAnswer = if (multiSelectMode) {
                                        val nextSet = if (optionText in selectedOptions) {
                                            selectedOptions - optionText
                                        } else {
                                            selectedOptions + optionText
                                        }
                                        ExerciseItem.encodeSelectedMcqOptions(nextSet, item.options)
                                    } else {
                                        optionText
                                    }
                                    draftAnswer = nextAnswer
                                    onDraftChanged(nextAnswer)
                                }
                            },
                            enabled = !isGrading,
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) qColors.pastelMint else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .testTag("exercise_mcq_${item.id}_$optIdx")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(if (multiSelectMode) RoundedCornerShape(8.dp) else CircleShape)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = ('A' + optIdx).toString(),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = optionText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = qColors.bodyInk,
                                    modifier = Modifier.weight(1f)
                                )
                                if (multiSelectMode) {
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
            } else {
                OutlinedTextField(
                    value = draftAnswer,
                    onValueChange = {
                        draftAnswer = it
                        onDraftChanged(it)
                    },
                    placeholder = { Text("Écris ta réponse ici...") },
                    enabled = !isGrading,
                    minLines = 2,
                    maxLines = 5,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (!isGrading && draftAnswer.isNotBlank()) {
                                onSubmitAnswer(draftAnswer)
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedTextColor = qColors.bodyInk,
                        unfocusedTextColor = qColors.bodyInk
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("exercise_text_input_${item.id}")
                )
            }

            // Validate button with upgraded page-turning book loader
            Button(
                onClick = { onSubmitAnswer(draftAnswer) },
                enabled = !isGrading && draftAnswer.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .testTag("exercise_validate_${item.id}"),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = qColors.navyActionContainer,
                    contentColor = qColors.navyActionOnContainer
                )
            ) {
                if (isGrading) {
                    QuizzyInlineLoader(
                        size = 24.dp,
                        tint = qColors.navyActionOnContainer
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Vérification IA rapide...",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                } else {
                    Text(
                        text = if (item.isGraded) "Revérifier ma réponse" else "Valider ma réponse",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // Error banner with instant retry button
            AnimatedVisibility(visible = !item.gradingError.isNullOrBlank() && !isGrading) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("exercise_error_card_${item.id}")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = item.gradingError.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                        OutlinedButton(
                            onClick = { onSubmitAnswer(draftAnswer) },
                            modifier = Modifier.testTag("exercise_retry_grade_${item.id}"),
                            shape = RoundedCornerShape(50)
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Réessayer la vérification")
                        }
                    }
                }
            }

            // Instant AI Verdict + Detailed Short Explanation
            AnimatedVisibility(visible = item.isGraded && !isGrading) {
                val (bgColor, accentColor, labelText) = when (item.verdict) {
                    ExerciseItem.VERDICT_CORRECT -> Triple(
                        qColors.pastelMint,
                        qColors.successInk,
                        "Réponse correcte !"
                    )
                    ExerciseItem.VERDICT_PARTIAL -> Triple(
                        qColors.pastelYellow,
                        qColors.warningInk,
                        "Partiellement correcte"
                    )
                    else -> Triple(
                        qColors.pastelPeach,
                        qColors.coralInk,
                        "Réponse fausse"
                    )
                }

                Surface(
                    color = bgColor,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.2.dp, accentColor.copy(alpha = 0.42f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("exercise_verdict_card_${item.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuizzyAnimatedMascot(
                            mood = when (item.verdict) {
                                ExerciseItem.VERDICT_CORRECT -> QuizzyMascotMood.VICTORY
                                ExerciseItem.VERDICT_PARTIAL -> QuizzyMascotMood.THINKING
                                else -> QuizzyMascotMood.ENCOURAGE
                            },
                            size = 62.dp
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = when (item.verdict) {
                                        ExerciseItem.VERDICT_CORRECT -> Icons.Filled.CheckCircle
                                        ExerciseItem.VERDICT_PARTIAL -> Icons.Filled.WarningAmber
                                        else -> Icons.Filled.Cancel
                                    },
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = labelText,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = accentColor,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            if (item.aiExplanation.isNotBlank()) {
                                Text(
                                    text = item.aiExplanation,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = qColors.bodyInk
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun createTempExercisePhotoUri(context: Context): Uri {
    val dir = File(context.cacheDir, "exercise_photos").apply { mkdirs() }
    val file = File(dir, "exercise_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
