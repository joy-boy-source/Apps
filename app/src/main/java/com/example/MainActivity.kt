package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.FamilyRepository
import com.example.data.GeminiTutorService
import com.example.data.authStateFlow
import com.example.data.local.ChildSqlOverview
import com.example.data.local.LocalSqlRepository
import com.example.data.local.QuizzySqlDatabase
import com.example.data.signOut
import com.example.firebase.configureAppCheck
import com.example.model.UiState
import com.example.notifications.QuizNotificationHelper
import com.example.ui.components.QuizzyBookLoader
import com.example.ui.components.QuizzyWordmarkLogo
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChildSpaceScreen
import com.example.ui.screens.FamilySetupScreen
import com.example.ui.screens.ParentDashboardScreen
import com.example.ui.screens.ProfilePortalScreen
import com.example.ui.theme.EduFamilleTheme
import com.example.ui.viewmodel.ActiveSession
import com.example.ui.viewmodel.EduFamilleViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mandatory: configure App Check before UI, ViewModels, or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            EduFamilleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser
    var roleHint by remember { mutableStateOf("PARENT") }

    if (user == null) {
        AuthScreen(
            initialRoleHint = roleHint,
            onRoleHintChanged = { roleHint = it },
            onAuthSuccess = {}
        )
    } else {
        key(user.uid) {
            AuthenticatedRoot(
                currentUserId = user.uid,
                defaultDisplayName = user.displayName ?: user.email ?: "Parent",
                initialRoleHint = roleHint
            )
        }
    }
}

@Composable
private fun AuthenticatedRoot(
    currentUserId: String,
    defaultDisplayName: String,
    initialRoleHint: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val viewModel: EduFamilleViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val sqlDb = QuizzySqlDatabase.getInstance(app)
                val localSqlRepository = LocalSqlRepository(
                    dao = sqlDb.quizzySqlDao(),
                    appContext = app
                )
                EduFamilleViewModel(
                    repository = FamilyRepository(db),
                    geminiService = GeminiTutorService(),
                    currentUserId = currentUserId,
                    defaultDisplayName = defaultDisplayName,
                    localSqlRepository = localSqlRepository
                )
            }
        }
    )

    val bundleState by viewModel.bundleState.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle()
    val opStatus by viewModel.opStatus.collectAsStateWithLifecycle()
    val pinError by viewModel.pinError.collectAsStateWithLifecycle()
    val isTranscribing by viewModel.isTranscribing.collectAsStateWithLifecycle()
    val transcribeError by viewModel.transcribeError.collectAsStateWithLifecycle()
    val imageStudioState by viewModel.imageStudioState.collectAsStateWithLifecycle()
    val allTopGameScores by viewModel.allTopGameScores.collectAsStateWithLifecycle()
    val allStudioImages by viewModel.allStudioImages.collectAsStateWithLifecycle()
    val allSubjectSqlStats by viewModel.allSubjectSqlStats.collectAsStateWithLifecycle()
    val exerciseOpStatus by viewModel.exerciseOpStatus.collectAsStateWithLifecycle()
    val gradingExerciseItemKeys by viewModel.gradingExerciseItemKeys.collectAsStateWithLifecycle()
    val finalizingExerciseSheetIds by viewModel.finalizingExerciseSheetIds.collectAsStateWithLifecycle()

    var notificationsPermissionGranted by remember {
        mutableStateOf(QuizNotificationHelper.hasNotificationPermission(context))
    }
    var notificationPermissionRequested by remember { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationsPermissionGranted = granted
    }
    val requestNotificationPermission: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            notificationsPermissionGranted = true
        }
    }

    var readAttemptIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    val performSignOut: () -> Unit = {
        signOut(credentialManager = credentialManager, scope = scope)
    }

    when (val state = bundleState) {
        is UiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    QuizzyWordmarkLogo(height = 46.dp)
                    Spacer(Modifier.height(22.dp))
                    QuizzyBookLoader(
                        size = 96.dp,
                        message = "Chargement de l'espace famille..."
                    )
                }
            }
        }
        is UiState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Erreur de connexion",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Button(onClick = performSignOut) {
                        Text("Se déconnecter")
                    }
                }
            }
        }
        is UiState.Success -> {
            val bundle = state.data
            val account = bundle.userAccount
            val family = bundle.family

            if (account == null || family == null) {
                FamilySetupScreen(
                    opStatus = opStatus,
                    initialRole = initialRoleHint,
                    onClearOpStatus = { viewModel.clearOperationStatus() },
                    onCreateParentFamily = { famName, parentPin, childName, childAge, childGrade, childAvatar, childPin ->
                        viewModel.createFamilyAsParent(
                            familyName = famName,
                            parentPin = parentPin,
                            firstChildName = childName,
                            firstChildAge = childAge,
                            firstChildGrade = childGrade,
                            firstChildAvatar = childAvatar,
                            firstChildPin = childPin
                        )
                    },
                    onJoinAsChild = { code, name, age, grade, avatar, pin ->
                        viewModel.joinFamilyAsChild(code, name, age, grade, avatar, pin)
                    },
                    onSignOut = performSignOut
                )
            } else {
                // Request Android notification permission for Parent accounts and dispatch a
                // native notification with the child's name whenever a new quiz is validated.
                LaunchedEffect(family.id, account.role) {
                    if (account.role == "PARENT") {
                        QuizNotificationHelper.ensureChannel(context)
                        readAttemptIds = QuizNotificationHelper.getReadAttemptIds(context, family.id)
                        if (!notificationsPermissionGranted && !notificationPermissionRequested && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionRequested = true
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                LaunchedEffect(family.id, account.role, bundle.attempts, bundle.children) {
                    if (account.role == "PARENT") {
                        val activeChildIds = bundle.children.map { it.id }.toSet()
                        val validAttempts = bundle.attempts.filter { it.id.isNotBlank() && it.childId in activeChildIds }
                        if (!QuizNotificationHelper.isFamilySeeded(context, family.id)) {
                            val initialIds = validAttempts.map { it.id }
                            QuizNotificationHelper.markAttemptsNotified(context, family.id, initialIds)
                            QuizNotificationHelper.markAttemptsRead(context, family.id, initialIds)
                            readAttemptIds = QuizNotificationHelper.getReadAttemptIds(context, family.id)
                        } else {
                            val alreadyNotified = QuizNotificationHelper.getNotifiedAttemptIds(context, family.id)
                            val newlyValidated = validAttempts.filter { it.id !in alreadyNotified }
                            if (newlyValidated.isNotEmpty()) {
                                newlyValidated.forEach { attempt ->
                                    QuizNotificationHelper.showQuizValidatedNotification(context, attempt)
                                }
                                QuizNotificationHelper.markAttemptsNotified(
                                    context,
                                    family.id,
                                    newlyValidated.map { it.id }
                                )
                            }
                        }
                    }
                }

                val activeChildIdsForNotif = remember(bundle.children) { bundle.children.map { it.id }.toSet() }
                val unreadNotificationCount = remember(bundle.attempts, activeChildIdsForNotif, readAttemptIds) {
                    bundle.attempts.count {
                        it.id.isNotBlank() && it.childId in activeChildIdsForNotif && it.id !in readAttemptIds
                    }
                }

                when (val active = session) {
                    is ActiveSession.Portal -> {
                        ProfilePortalScreen(
                            userAccount = account,
                            family = family,
                            children = bundle.children,
                            pinError = pinError,
                            onClearPinError = { viewModel.clearPinError() },
                            onVerifyParentPin = { entered -> viewModel.unlockParentAdmin(family, entered) },
                            onVerifyChildPin = { child, entered -> viewModel.unlockChildSpace(child, entered) },
                            onSignOut = performSignOut
                        )
                    }
                    is ActiveSession.ParentAdmin -> {
                        ParentDashboardScreen(
                            family = family,
                            children = bundle.children,
                            lessons = bundle.lessons,
                            quizzes = bundle.quizzes,
                            attempts = bundle.attempts,
                            opStatus = opStatus,
                            onSwitchProfile = { viewModel.switchToPortal() },
                            onAddChild = { name, age, grade, avatar, pin ->
                                viewModel.addChild(family, name, age, grade, avatar, pin)
                            },
                            onEditChild = { id, name, age, grade, avatar, pin ->
                                viewModel.updateChildAdmin(family.id, id, name, age, grade, avatar, pin)
                            },
                            onDeleteChild = { id ->
                                viewModel.deleteChild(family.id, id)
                            },
                            unreadNotificationCount = unreadNotificationCount,
                            notificationsPermissionGranted = notificationsPermissionGranted,
                            onRequestNotificationPermission = requestNotificationPermission,
                            onMarkNotificationsRead = {
                                val ids = bundle.attempts.map { it.id }.filter { it.isNotBlank() }
                                QuizNotificationHelper.markAttemptsRead(context, family.id, ids)
                                readAttemptIds = QuizNotificationHelper.getReadAttemptIds(context, family.id)
                            },
                            sqlGameScores = allTopGameScores,
                            sqlStudioImagesCount = allStudioImages.size,
                            sqlSubjectStats = allSubjectSqlStats,
                            onUpdateChildLanguageTone = { childId, toneLevel ->
                                viewModel.updateChildLanguageTone(family.id, childId, toneLevel)
                            },
                            onAddChildWithTone = { name, age, grade, avatar, pin, toneLevel ->
                                viewModel.addChild(family, name, age, grade, avatar, pin, toneLevel)
                            },
                            onEditChildWithTone = { id, name, age, grade, avatar, pin, toneLevel ->
                                viewModel.updateChildAdmin(family.id, id, name, age, grade, avatar, pin, toneLevel)
                            }
                        )
                    }
                    is ActiveSession.ChildSpace -> {
                        val activeChild = bundle.children.firstOrNull { it.id == active.childId }
                        if (activeChild == null) {
                            LaunchedEffect(active.childId) {
                                delay(2500L)
                                viewModel.switchToPortal()
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    QuizzyWordmarkLogo(height = 44.dp)
                                    Spacer(Modifier.height(20.dp))
                                    QuizzyBookLoader(
                                        size = 92.dp,
                                        message = "Ouverture de ton espace..."
                                    )
                                }
                            }
                        } else {
                            val childGameScores by remember(activeChild.id) {
                                viewModel.observeChildGameScores(activeChild.id)
                            }.collectAsStateWithLifecycle(initialValue = emptyList())
                            val childStudioImages by remember(activeChild.id) {
                                viewModel.observeChildStudioImages(activeChild.id)
                            }.collectAsStateWithLifecycle(initialValue = emptyList())
                            val childSubjectSqlStats by remember(activeChild.id) {
                                viewModel.observeChildSubjectSqlStats(activeChild.id)
                            }.collectAsStateWithLifecycle(initialValue = emptyList())
                            val childSqlOverview by remember(activeChild.id) {
                                viewModel.observeChildSqlOverview(activeChild.id)
                            }.collectAsStateWithLifecycle(initialValue = ChildSqlOverview())
                            val childExerciseSheets by remember(activeChild.id) {
                                viewModel.observeChildExerciseSheets(activeChild.id)
                            }.collectAsStateWithLifecycle(initialValue = emptyList())

                            ChildSpaceScreen(
                                family = family,
                                child = activeChild,
                                lessons = bundle.lessons,
                                quizzes = bundle.quizzes,
                                opStatus = opStatus,
                                isTranscribing = isTranscribing,
                                transcribeError = transcribeError,
                                onClearOpStatus = { viewModel.clearOperationStatus() },
                                onSwitchProfile = { viewModel.switchToPortal() },
                                onUpdateGradeLevel = { age, grade ->
                                    viewModel.updateChildGradeLevel(family.id, activeChild.id, age, grade)
                                },
                                onDepositCourse = { subject, documentType, difficulty, bitmap, attachments, notes ->
                                    viewModel.depositCourseAndGenerate(
                                        family = family,
                                        child = activeChild,
                                        subject = subject,
                                        documentType = documentType,
                                        difficulty = difficulty,
                                        bitmap = bitmap,
                                        attachments = attachments,
                                        notes = notes
                                    )
                                },
                                onAddDocumentsToLesson = { lesson, bitmap, attachments, notes ->
                                    viewModel.addFilesToExistingLesson(family, activeChild, lesson, bitmap, attachments, notes)
                                },
                                onGenerateMoreQuiz = { lesson, difficulty ->
                                    viewModel.generateMoreQuiz(family, activeChild, lesson, difficulty)
                                },
                                onGenerateGapQuiz = {
                                    val childOnlyLessons = bundle.lessons.filter { it.childId == activeChild.id }
                                    viewModel.generateGapTargetedQuiz(family, activeChild, childOnlyLessons)
                                },
                                onGenerateSundayReviewQuiz = { difficulty ->
                                    val childOnlyLessons = bundle.lessons.filter { it.childId == activeChild.id }
                                    viewModel.generateSundayReviewQuiz(
                                        family = family,
                                        child = activeChild,
                                        childLessons = childOnlyLessons,
                                        difficulty = difficulty
                                    )
                                },
                                onSubmitQuizResult = { lesson, quiz, score, total, maxStreak, masteredSkills, weakSkills, earnedXp, missedQuestions ->
                                    viewModel.submitQuizResult(
                                        family = family,
                                        child = activeChild,
                                        lesson = lesson,
                                        quiz = quiz,
                                        score = score,
                                        totalQuestions = total,
                                        maxStreak = maxStreak,
                                        masteredSkills = masteredSkills,
                                        weakSkills = weakSkills,
                                        earnedXpOverride = earnedXp,
                                        missedQuestions = missedQuestions
                                    )
                                },
                                onTranscribeAudio = { bytes, onResult ->
                                    viewModel.transcribeAudio(bytes, onResult = onResult)
                                },
                                childGameScores = childGameScores,
                                childStudioImages = childStudioImages,
                                childSubjectSqlStats = childSubjectSqlStats,
                                childSqlOverview = childSqlOverview,
                                imageStudioState = imageStudioState,
                                onRecord3DGameFinished = { score, correctCount, totalQuestions, maxCombo, durationSec, subject ->
                                    viewModel.record3DGameScore(
                                        child = activeChild,
                                        score = score,
                                        correctCount = correctCount,
                                        totalQuestions = totalQuestions,
                                        maxCombo = maxCombo,
                                        durationSeconds = durationSec,
                                        subject = subject
                                    )
                                },
                                onGenerateStudioImage = { prompt, styleLabel, subject ->
                                    viewModel.generateStudioImage(activeChild, prompt, styleLabel, subject)
                                },
                                onEditStudioImage = { bitmap, editInstruction, basePrompt, subject ->
                                    viewModel.editStudioImage(activeChild, bitmap, editInstruction, basePrompt, subject)
                                },
                                onLoadStudioBitmap = { path -> viewModel.loadStudioBitmap(path) },
                                onDeleteStudioImage = { item -> viewModel.deleteStudioImage(item) },
                                onResetImageStudioState = { viewModel.resetImageStudioState() },
                                exerciseSheets = childExerciseSheets,
                                exerciseOpStatus = exerciseOpStatus,
                                gradingExerciseItemKeys = gradingExerciseItemKeys,
                                onClearExerciseOpStatus = { viewModel.clearExerciseOpStatus() },
                                onDepositExercise = { subject, bitmap, attachments, notes ->
                                    viewModel.createIdenticalExercise(
                                        child = activeChild,
                                        subject = subject,
                                        bitmap = bitmap,
                                        attachments = attachments,
                                        notes = notes
                                    )
                                },
                                onGradeExerciseItem = { sheet, item, answer ->
                                    viewModel.gradeExerciseItemAnswer(
                                        child = activeChild,
                                        sheet = sheet,
                                        item = item,
                                        studentAnswer = answer
                                    )
                                },
                                onDeleteExerciseSheet = { sheetId ->
                                    viewModel.deleteExerciseSheet(sheetId)
                                },
                                finalizingExerciseSheetIds = finalizingExerciseSheetIds,
                                onFinalizeExercise = { sheet, draftAnswersByItemId ->
                                    viewModel.finalizeExerciseSheet(
                                        child = activeChild,
                                        sheet = sheet,
                                        draftAnswersByItemId = draftAnswersByItemId
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
