package com.example.ui.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CourseAttachment
import com.example.data.FamilyRepository
import com.example.data.FrenchErrorMessages
import com.example.data.GeminiTutorService
import com.example.data.local.ChildSqlOverview
import com.example.data.local.GameScoreEntity
import com.example.data.local.LocalSqlRepository
import com.example.data.local.StudioImageEntity
import com.example.data.local.SubjectSqlStats
import com.example.model.ChildProfile
import com.example.model.ExerciseItem
import com.example.model.ExerciseSheet
import com.example.model.Family
import com.example.model.LanguageToneCatalog
import com.example.model.Lesson
import com.example.model.MissedQuestionRecord
import com.example.model.PinHasher
import com.example.model.Quiz
import com.example.model.QuizAttempt
import com.example.model.SchoolCatalog
import com.example.model.UiState
import com.example.model.UserAccount
import com.example.model.WeeklyReviewScheduler
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FamilyBundle(
    val userAccount: UserAccount?,
    val family: Family?,
    val children: List<ChildProfile> = emptyList(),
    val lessons: List<Lesson> = emptyList(),
    val quizzes: List<Quiz> = emptyList(),
    val attempts: List<QuizAttempt> = emptyList()
)

sealed interface ActiveSession {
    data object Portal : ActiveSession
    data object ParentAdmin : ActiveSession
    data class ChildSpace(val childId: String) : ActiveSession
}

sealed interface OperationStatus {
    data object Idle : OperationStatus
    data class Busy(val message: String) : OperationStatus
    data class Success(
        val message: String,
        val createdLessonId: String? = null,
        val createdQuizId: String? = null,
        val createdExerciseId: String? = null
    ) : OperationStatus
    data class Error(val message: String) : OperationStatus
}

sealed interface ImageStudioUiState {
    data object Idle : ImageStudioUiState
    data class Loading(val message: String) : ImageStudioUiState
    data class Success(val bitmap: Bitmap, val prompt: String, val isEdited: Boolean) : ImageStudioUiState
    data class Error(val message: String) : ImageStudioUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class EduFamilleViewModel(
    private val repository: FamilyRepository,
    private val geminiService: GeminiTutorService,
    val currentUserId: String,
    val defaultDisplayName: String,
    private val localSqlRepository: LocalSqlRepository? = null
) : ViewModel() {

    val bundleState: StateFlow<UiState<FamilyBundle>> =
        repository.observeUserAccount(currentUserId)
            .flatMapLatest { account ->
                if (account == null || account.familyId.isBlank()) {
                    flowOf(FamilyBundle(userAccount = null, family = null))
                } else {
                    val fid = account.familyId
                    combine(
                        repository.observeFamily(fid),
                        repository.observeChildren(fid),
                        repository.observeLessons(fid),
                        repository.observeQuizzes(fid),
                        repository.observeQuizAttempts(fid)
                    ) { family, children, lessons, quizzes, attempts ->
                        FamilyBundle(
                            userAccount = account,
                            family = family,
                            children = children,
                            lessons = lessons,
                            quizzes = quizzes,
                            attempts = attempts
                        )
                    }
                }
            }
            .map<FamilyBundle, UiState<FamilyBundle>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing family bundle", rootError)
                emit(
                    UiState.Error(
                        FrenchErrorMessages.toFrenchError(
                            rootError,
                            "Impossible de charger les données du foyer."
                        )
                    )
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    private val _session = MutableStateFlow<ActiveSession>(ActiveSession.Portal)
    val session: StateFlow<ActiveSession> = _session.asStateFlow()

    private val _opStatus = MutableStateFlow<OperationStatus>(OperationStatus.Idle)
    val opStatus: StateFlow<OperationStatus> = _opStatus.asStateFlow()

    private val _exerciseOpStatus = MutableStateFlow<OperationStatus>(OperationStatus.Idle)
    val exerciseOpStatus: StateFlow<OperationStatus> = _exerciseOpStatus.asStateFlow()

    private val _gradingExerciseItemKeys = MutableStateFlow<Set<String>>(emptySet())
    val gradingExerciseItemKeys: StateFlow<Set<String>> = _gradingExerciseItemKeys.asStateFlow()

    private val _finalizingExerciseSheetIds = MutableStateFlow<Set<String>>(emptySet())
    val finalizingExerciseSheetIds: StateFlow<Set<String>> = _finalizingExerciseSheetIds.asStateFlow()

    private val _pinError = MutableStateFlow<String?>(null)
    val pinError: StateFlow<String?> = _pinError.asStateFlow()

    // Audio Transcription state (used for voice dictation when depositing a course/exercise)
    private val _isTranscribing = MutableStateFlow(false)
    val isTranscribing: StateFlow<Boolean> = _isTranscribing.asStateFlow()

    private val _transcribeError = MutableStateFlow<String?>(null)
    val transcribeError: StateFlow<String?> = _transcribeError.asStateFlow()

    private var transcribeJob: Job? = null

    private val _imageStudioState = MutableStateFlow<ImageStudioUiState>(ImageStudioUiState.Idle)
    val imageStudioState: StateFlow<ImageStudioUiState> = _imageStudioState.asStateFlow()
    private var imageStudioJob: Job? = null

    val allTopGameScores: StateFlow<List<GameScoreEntity>> =
        (localSqlRepository?.observeAllTopGameScores() ?: flowOf(emptyList()))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allStudioImages: StateFlow<List<StudioImageEntity>> =
        (localSqlRepository?.observeAllStudioImages() ?: flowOf(emptyList()))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allSubjectSqlStats: StateFlow<List<SubjectSqlStats>> =
        (localSqlRepository?.observeAllSubjectStats() ?: flowOf(emptyList()))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    fun observeChildGameScores(childId: String): Flow<List<GameScoreEntity>> =
        localSqlRepository?.observeGameScoresForChild(childId) ?: flowOf(emptyList())

    fun observeChildStudioImages(childId: String): Flow<List<StudioImageEntity>> =
        localSqlRepository?.observeStudioImagesForChild(childId) ?: flowOf(emptyList())

    fun observeChildSubjectSqlStats(childId: String): Flow<List<SubjectSqlStats>> =
        localSqlRepository?.observeSubjectStatsForChild(childId) ?: flowOf(emptyList())

    fun observeChildSqlOverview(childId: String): Flow<ChildSqlOverview> =
        localSqlRepository?.observeChildSqlOverview(childId) ?: flowOf(ChildSqlOverview())

    fun resetImageStudioState() {
        imageStudioJob?.cancel()
        imageStudioJob = null
        _imageStudioState.value = ImageStudioUiState.Idle
    }

    fun clearOperationStatus() {
        _opStatus.value = OperationStatus.Idle
    }

    fun clearPinError() {
        _pinError.value = null
    }

    fun switchToPortal() {
        _pinError.value = null
        clearTranscribeState()
        _session.value = ActiveSession.Portal
    }

    private fun clearTranscribeState() {
        transcribeJob?.cancel()
        transcribeJob = null
        _isTranscribing.value = false
        _transcribeError.value = null
    }

    fun unlockParentAdmin(family: Family, enteredPin: String): Boolean {
        return if (PinHasher.hashPin(enteredPin) == family.parentPinHash) {
            _pinError.value = null
            _session.value = ActiveSession.ParentAdmin
            true
        } else {
            _pinError.value = "Code PIN parent incorrect."
            false
        }
    }

    fun unlockChildSpace(child: ChildProfile, enteredPin: String): Boolean {
        return if (PinHasher.hashPin(enteredPin) == child.pinHash) {
            _pinError.value = null
            clearTranscribeState()
            _session.value = ActiveSession.ChildSpace(child.id)
            true
        } else {
            _pinError.value = "Code PIN de ${child.name} incorrect."
            false
        }
    }

    fun transcribeAudio(
        audioBytes: ByteArray,
        mimeType: String = "audio/mp4",
        onResult: (String) -> Unit
    ) {
        transcribeJob?.cancel()
        transcribeJob = viewModelScope.launch {
            _isTranscribing.value = true
            _transcribeError.value = null
            try {
                geminiService.transcribeAudio(audioBytes, mimeType).fold(
                    onSuccess = { transcript ->
                        onResult(transcript)
                    },
                    onFailure = { e ->
                        _transcribeError.value = FrenchErrorMessages.toFrenchError(e, "Impossible de transcrire l'audio.")
                    }
                )
            } finally {
                _isTranscribing.value = false
            }
        }
    }

    // --- Family & Course Persistence ---

    fun createFamilyAsParent(
        familyName: String,
        parentPin: String,
        firstChildName: String,
        firstChildAge: Int,
        firstChildGrade: String,
        firstChildAvatar: String,
        firstChildPin: String
    ) {
        viewModelScope.launch {
            _opStatus.value = OperationStatus.Busy("Création de votre espace famille...")
            val result = repository.createFamilyAsParent(
                familyName = familyName,
                displayName = defaultDisplayName,
                parentPin = parentPin
            )
            result.fold(
                onSuccess = { fid ->
                    var childError: String? = null
                    if (firstChildName.isNotBlank() && firstChildPin.length == 4) {
                        val tempFamily = Family(
                            id = fid,
                            ownerId = currentUserId,
                            familyName = familyName,
                            memberUids = listOf(currentUserId)
                        )
                        repository.addChildProfile(
                            family = tempFamily,
                            name = firstChildName,
                            age = firstChildAge,
                            gradeLevel = firstChildGrade,
                            avatarKey = firstChildAvatar,
                            pin = firstChildPin
                        ).onFailure { err ->
                            childError = FrenchErrorMessages.toFrenchError(
                                err,
                                "Foyer créé, mais impossible d'ajouter $firstChildName. Ajoute-le depuis l'Espace Parent."
                            )
                        }
                    }
                    val errMessage = childError
                    _opStatus.value = if (errMessage != null) {
                        OperationStatus.Error(errMessage)
                    } else {
                        OperationStatus.Success("Espace famille créé !")
                    }
                    _session.value = ActiveSession.ParentAdmin
                },
                onFailure = { e ->
                    _opStatus.value = OperationStatus.Error(
                        FrenchErrorMessages.toFrenchError(e, "Erreur lors de la création du foyer.")
                    )
                }
            )
        }
    }

    fun joinFamilyAsChild(
        inviteCode: String,
        childName: String,
        age: Int,
        gradeLevel: String,
        avatarKey: String,
        childPin: String
    ) {
        viewModelScope.launch {
            _opStatus.value = OperationStatus.Busy("Connexion au foyer...")
            val result = repository.joinFamilyAsChild(
                inviteCodeInput = inviteCode,
                childName = childName,
                age = age,
                gradeLevel = gradeLevel,
                avatarKey = avatarKey,
                childPin = childPin
            )
            result.fold(
                onSuccess = { (_, childId) ->
                    _opStatus.value = OperationStatus.Success("Bienvenue $childName !")
                    _session.value = ActiveSession.ChildSpace(childId)
                },
                onFailure = { e ->
                    _opStatus.value = OperationStatus.Error(
                        FrenchErrorMessages.toFrenchError(e, "Code famille invalide ou accès refusé.")
                    )
                }
            )
        }
    }

    fun addChild(
        family: Family,
        name: String,
        age: Int,
        gradeLevel: String,
        avatarKey: String,
        pin: String,
        languageToneLevel: String = LanguageToneCatalog.AUTO
    ) {
        viewModelScope.launch {
            _opStatus.value = OperationStatus.Busy("Ajout du profil...")
            repository.addChildProfile(
                family = family,
                name = name,
                age = age,
                gradeLevel = gradeLevel,
                avatarKey = avatarKey,
                pin = pin,
                languageToneLevel = languageToneLevel
            ).fold(
                onSuccess = {
                    _opStatus.value = OperationStatus.Success("Profil de $name ajouté !")
                },
                onFailure = { e ->
                    _opStatus.value = OperationStatus.Error(
                        FrenchErrorMessages.toFrenchError(e, "Impossible d'ajouter l'enfant.")
                    )
                }
            )
        }
    }

    fun updateChildGradeLevel(familyId: String, childId: String, age: Int, gradeLevel: String) {
        viewModelScope.launch {
            _opStatus.value = OperationStatus.Busy("Mise à jour du niveau...")
            repository.updateChildSchoolLevel(familyId, childId, age, gradeLevel).fold(
                onSuccess = {
                    _opStatus.value = OperationStatus.Success("Niveau scolaire mis à jour ($gradeLevel) !")
                },
                onFailure = { e ->
                    _opStatus.value = OperationStatus.Error(
                        FrenchErrorMessages.toFrenchError(e, "Erreur de mise à jour du niveau.")
                    )
                }
            )
        }
    }

    fun updateChildAdmin(
        familyId: String,
        childId: String,
        name: String,
        age: Int,
        gradeLevel: String,
        avatarKey: String,
        newPin: String?,
        languageToneLevel: String? = null
    ) {
        viewModelScope.launch {
            _opStatus.value = OperationStatus.Busy("Mise à jour...")
            repository.updateChildProfileAdmin(
                familyId = familyId,
                childId = childId,
                name = name,
                age = age,
                gradeLevel = gradeLevel,
                avatarKey = avatarKey,
                newPin = newPin,
                languageToneLevel = languageToneLevel
            ).fold(
                onSuccess = {
                    _opStatus.value = OperationStatus.Success("Profil mis à jour !")
                },
                onFailure = { e ->
                    _opStatus.value = OperationStatus.Error(
                        FrenchErrorMessages.toFrenchError(e, "Erreur lors de la modification du profil.")
                    )
                }
            )
        }
    }

    fun updateChildLanguageTone(
        familyId: String,
        childId: String,
        languageToneLevel: String
    ) {
        val normalized = LanguageToneCatalog.normalize(languageToneLevel)
        val option = LanguageToneCatalog.optionFor(normalized)
        viewModelScope.launch {
            _opStatus.value = OperationStatus.Busy("Mise à jour du niveau de langage...")
            repository.updateChildLanguageTone(familyId, childId, normalized).fold(
                onSuccess = {
                    _opStatus.value = OperationStatus.Success(
                        "Niveau de langage défini sur « ${option.title} » !"
                    )
                },
                onFailure = { e ->
                    _opStatus.value = OperationStatus.Error(
                        FrenchErrorMessages.toFrenchError(e, "Impossible de modifier le niveau de langage.")
                    )
                }
            )
        }
    }

    fun deleteChild(familyId: String, childId: String) {
        viewModelScope.launch {
            _opStatus.value = OperationStatus.Busy("Suppression...")
            repository.deleteChildProfile(familyId, childId).fold(
                onSuccess = {
                    _opStatus.value = OperationStatus.Success("Profil supprimé.")
                },
                onFailure = { e ->
                    _opStatus.value = OperationStatus.Error(
                        FrenchErrorMessages.toFrenchError(e, "Suppression impossible.")
                    )
                }
            )
        }
    }

    fun depositCourseAndGenerate(
        family: Family,
        child: ChildProfile,
        subject: String,
        documentType: String = "Cours",
        difficulty: String = "Moyen",
        bitmap: Bitmap?,
        attachments: List<CourseAttachment> = emptyList(),
        notes: String
    ) {
        if (bitmap == null && attachments.isEmpty() && notes.isBlank()) {
            _opStatus.value = OperationStatus.Error("Ajoute une photo, un fichier (PDF, texte...) ou quelques notes.")
            return
        }
        val safeDocType = SchoolCatalog.normalizeDocumentType(documentType)
        val safeDifficulty = SchoolCatalog.normalizeDifficulty(difficulty)
        val difficultyLabel = SchoolCatalog.difficultyMultiplierLabel(safeDifficulty)
        viewModelScope.launch {
            _opStatus.value = OperationStatus.Busy(
                if (safeDocType == "Cours") {
                    "Analyse de ton cours et préparation du quiz $difficultyLabel (${child.gradeLevel})..."
                } else {
                    "Évaluation de ton $safeDocType et création de ton quiz $difficultyLabel..."
                }
            )
            val aiResult = geminiService.analyzeLessonAndCreateQuiz(
                bitmap = bitmap,
                attachments = attachments,
                notes = notes,
                subject = subject,
                documentType = safeDocType,
                difficulty = safeDifficulty,
                childName = child.name,
                childAge = child.age.toInt(),
                gradeLevel = child.gradeLevel,
                existingWeakPoints = child.weakPoints,
                languageToneLevel = child.languageToneLevel
            )
            aiResult.fold(
                onSuccess = { generated ->
                    _opStatus.value = OperationStatus.Busy(
                        "Classement dans « ${generated.detectedSubject} » et enregistrement..."
                    )
                    repository.saveGeneratedLessonAndQuiz(
                        family = family,
                        child = child,
                        subject = subject,
                        documentType = safeDocType,
                        difficulty = safeDifficulty,
                        sourceNotes = notes,
                        photoAttached = bitmap != null || attachments.any { it.bitmap != null },
                        attachedFiles = attachments.map { it.toAttachedDocument() },
                        generated = generated
                    ).fold(
                        onSuccess = { (lessonId, quizId) ->
                            _opStatus.value = OperationStatus.Success(
                                message = "$safeDocType rangé dans « ${generated.detectedSubject} » et quiz $difficultyLabel prêt !",
                                createdLessonId = lessonId,
                                createdQuizId = quizId
                            )
                        },
                        onFailure = { e ->
                            _opStatus.value = OperationStatus.Error(
                                FrenchErrorMessages.toFrenchError(e, "Erreur d'enregistrement du document.")
                            )
                        }
                    )
                },
                onFailure = { e ->
                    _opStatus.value = OperationStatus.Error(
                        FrenchErrorMessages.toFrenchError(e, "Impossible d'analyser ce document.")
                    )
                }
            )
        }
    }

    fun addFilesToExistingLesson(
        family: Family,
        child: ChildProfile,
        lesson: Lesson,
        bitmap: Bitmap?,
        attachments: List<CourseAttachment>,
        notes: String
    ) {
        if (bitmap == null && attachments.isEmpty() && notes.isBlank()) {
            _opStatus.value = OperationStatus.Error("Sélectionne un fichier (PDF, document, image) ou ajoute des notes.")
            return
        }
        viewModelScope.launch {
            _opStatus.value = OperationStatus.Busy("Analyse du document et mise à jour des compétences...")
            val aiResult = geminiService.enrichExistingLessonWithAttachments(
                existingLesson = lesson,
                bitmap = bitmap,
                attachments = attachments,
                notes = notes,
                childName = child.name,
                childAge = child.age.toInt(),
                gradeLevel = child.gradeLevel,
                existingWeakPoints = child.weakPoints,
                languageToneLevel = child.languageToneLevel
            )
            aiResult.fold(
                onSuccess = { generated ->
                    _opStatus.value = OperationStatus.Busy("Mise à jour de la fiche et création du quiz...")
                    repository.enrichLessonWithDocumentsAndQuiz(
                        family = family,
                        child = child,
                        lesson = lesson,
                        newAttachments = attachments.map { it.toAttachedDocument() },
                        addedNotes = notes,
                        photoAttachedNow = bitmap != null || attachments.any { it.bitmap != null },
                        generated = generated
                    ).fold(
                        onSuccess = { quizId ->
                            _opStatus.value = OperationStatus.Success(
                                message = "Document(s) ajoutés, compétences mises à jour et nouveau quiz prêt !",
                                createdQuizId = quizId
                            )
                        },
                        onFailure = { e ->
                            _opStatus.value = OperationStatus.Error(
                                FrenchErrorMessages.toFrenchError(e, "Erreur lors de la mise à jour.")
                            )
                        }
                    )
                },
                onFailure = { e ->
                    _opStatus.value = OperationStatus.Error(
                        FrenchErrorMessages.toFrenchError(e, "Impossible d'analyser les fichiers ajoutés.")
                    )
                }
            )
        }
    }

    fun generateMoreQuiz(
        family: Family,
        child: ChildProfile,
        lesson: Lesson,
        difficulty: String
    ) {
        viewModelScope.launch {
            _opStatus.value = OperationStatus.Busy("Création d'un nouveau quiz ($difficulty) ciblé sur tes lacunes...")
            val combinedGaps = (lesson.weakPoints + child.weakPoints).distinct()
            val aiResult = geminiService.generateAdditionalQuiz(
                lessonTitle = lesson.title,
                summaryIntro = lesson.summaryIntro,
                keyPoints = lesson.keyPoints,
                simpleExplanation = lesson.simpleExplanation,
                subject = lesson.subject,
                childAge = child.age.toInt(),
                gradeLevel = child.gradeLevel,
                difficulty = difficulty,
                existingQuizCount = lesson.quizCount.toInt(),
                childWeakPoints = combinedGaps,
                languageToneLevel = child.languageToneLevel
            )
            aiResult.fold(
                onSuccess = { generated ->
                    repository.saveAdditionalQuiz(
                        family = family,
                        lesson = lesson,
                        difficulty = difficulty,
                        generated = generated
                    ).fold(
                        onSuccess = { quizId ->
                            _opStatus.value = OperationStatus.Success(
                                message = "Nouveau quiz adaptatif prêt !",
                                createdQuizId = quizId
                            )
                        },
                        onFailure = { e ->
                            _opStatus.value = OperationStatus.Error(
                                FrenchErrorMessages.toFrenchError(e, "Erreur d'enregistrement du quiz.")
                            )
                        }
                    )
                },
                onFailure = { e ->
                    _opStatus.value = OperationStatus.Error(
                        FrenchErrorMessages.toFrenchError(e, "Échec de génération du quiz.")
                    )
                }
            )
        }
    }

    fun generateGapTargetedQuiz(
        family: Family,
        child: ChildProfile,
        childLessons: List<Lesson>
    ) {
        val targetLesson = childLessons.firstOrNull { it.weakPoints.isNotEmpty() }
            ?: childLessons.firstOrNull()
        if (targetLesson == null) {
            _opStatus.value = OperationStatus.Error("Dépose d'abord un cours ou un exercice dans l'onglet Cours.")
            return
        }
        viewModelScope.launch {
            _opStatus.value = OperationStatus.Busy("Préparation d'un quiz sur mesure pour tes points à travailler...")
            val aiResult = geminiService.generateGapTargetedQuiz(
                childName = child.name,
                childAge = child.age.toInt(),
                gradeLevel = child.gradeLevel,
                targetLesson = targetLesson,
                weakPoints = child.weakPoints,
                languageToneLevel = child.languageToneLevel
            )
            aiResult.fold(
                onSuccess = { generated ->
                    repository.saveAdditionalQuiz(
                        family = family,
                        lesson = targetLesson,
                        difficulty = "Moyen",
                        generated = generated
                    ).fold(
                        onSuccess = { quizId ->
                            _opStatus.value = OperationStatus.Success(
                                message = "Ton quiz ciblé sur tes lacunes est prêt !",
                                createdQuizId = quizId
                            )
                        },
                        onFailure = { e ->
                            _opStatus.value = OperationStatus.Error(
                                FrenchErrorMessages.toFrenchError(e, "Erreur d'enregistrement du quiz ciblé.")
                            )
                        }
                    )
                },
                onFailure = { e ->
                    _opStatus.value = OperationStatus.Error(
                        FrenchErrorMessages.toFrenchError(e, "Impossible de créer le quiz ciblé.")
                    )
                }
            )
        }
    }

    fun generateSundayReviewQuiz(
        family: Family,
        child: ChildProfile,
        childLessons: List<Lesson>,
        difficulty: String = "Moyen",
        weekKey: String = WeeklyReviewScheduler.currentWeekKey()
    ) {
        if (child.weeklyMissedQuestions.isEmpty()) {
            _opStatus.value = OperationStatus.Error("Aucune mauvaise réponse enregistrée cette semaine. Bravo !")
            return
        }
        val safeDifficulty = SchoolCatalog.normalizeDifficulty(difficulty)
        viewModelScope.launch {
            _opStatus.value = OperationStatus.Busy("Préparation de ton quiz du dimanche (10 nouvelles questions sur tes erreurs de la semaine)...")
            val aiResult = geminiService.generateSundayReviewQuiz(
                childName = child.name,
                childAge = child.age.toInt(),
                gradeLevel = child.gradeLevel,
                missedQuestions = child.weeklyMissedQuestions,
                lessons = childLessons,
                difficulty = safeDifficulty,
                languageToneLevel = child.languageToneLevel
            )
            aiResult.fold(
                onSuccess = { generated ->
                    repository.saveSundayWeeklyReviewQuiz(
                        family = family,
                        child = child,
                        weekKey = weekKey,
                        difficulty = safeDifficulty,
                        generated = generated
                    ).fold(
                        onSuccess = { quizId ->
                            _opStatus.value = OperationStatus.Success(
                                message = "Ton quiz de révision du dimanche (10 questions) est prêt !",
                                createdQuizId = quizId
                            )
                        },
                        onFailure = { e ->
                            _opStatus.value = OperationStatus.Error(
                                FrenchErrorMessages.toFrenchError(e, "Erreur d'enregistrement du quiz du dimanche.")
                            )
                        }
                    )
                },
                onFailure = { e ->
                    _opStatus.value = OperationStatus.Error(
                        FrenchErrorMessages.toFrenchError(e, "Impossible de créer le quiz du dimanche.")
                    )
                }
            )
        }
    }

    fun submitQuizResult(
        family: Family,
        child: ChildProfile,
        lesson: Lesson?,
        quiz: Quiz,
        score: Int,
        totalQuestions: Int,
        maxStreak: Int,
        masteredSkills: List<String> = emptyList(),
        weakSkills: List<String> = emptyList(),
        earnedXpOverride: Long? = null,
        missedQuestions: List<MissedQuestionRecord> = emptyList()
    ) {
        viewModelScope.launch {
            repository.recordQuizAttempt(
                family = family,
                child = child,
                lesson = lesson,
                quiz = quiz,
                score = score,
                totalQuestions = totalQuestions,
                maxStreak = maxStreak,
                masteredSkills = masteredSkills,
                weakSkills = weakSkills,
                earnedXpOverride = earnedXpOverride,
                missedQuestions = missedQuestions
            ).onSuccess {
                val pct = if (totalQuestions > 0) ((score * 100) / totalQuestions).coerceIn(0, 100) else 0
                runCatching {
                    localSqlRepository?.recordQuizStudyLog(
                        childId = child.id,
                        subject = quiz.subject.ifBlank { "Général" },
                        scorePercent = pct,
                        xpDelta = (earnedXpOverride ?: 0L).toInt()
                    )
                }
            }.onFailure { e ->
                _opStatus.value = OperationStatus.Error(
                    FrenchErrorMessages.toFrenchError(e, "Erreur lors de la sauvegarde du score.")
                )
            }
        }
    }

    // --- 3D Game & Room SQLite Persistence ---

    fun record3DGameScore(
        child: ChildProfile,
        score: Int,
        correctCount: Int,
        totalQuestions: Int,
        maxCombo: Int,
        durationSeconds: Int,
        subject: String
    ) {
        viewModelScope.launch {
            runCatching {
                localSqlRepository?.recordGameSession(
                    childId = child.id,
                    childName = child.name,
                    subject = subject.ifBlank { "Général" },
                    gameMode = "ARENA_3D",
                    score = score,
                    correctCount = correctCount,
                    totalQuestions = totalQuestions,
                    maxCombo = maxCombo,
                    durationSeconds = durationSeconds
                )
            }.onFailure { e ->
                Log.e(TAG, "Failed to save 3D game score in Room SQLite", e)
            }
        }
    }

    // --- Gemini Nano Banana 2.1 Image Studio ---

    fun generateStudioImage(
        child: ChildProfile,
        prompt: String,
        styleLabel: String,
        subject: String
    ) {
        imageStudioJob?.cancel()
        imageStudioJob = viewModelScope.launch {
            _imageStudioState.value = ImageStudioUiState.Loading("Création de ton image avec Nano Banana 2.1...")
            geminiService.generateEducationalImage(
                prompt = prompt,
                gradeLevel = child.gradeLevel,
                childAge = child.age.toInt(),
                onStatusUpdate = { statusMsg ->
                    _imageStudioState.value = ImageStudioUiState.Loading(statusMsg)
                }
            ).fold(
                onSuccess = { bitmap ->
                    runCatching {
                        localSqlRepository?.saveStudioImage(
                            childId = child.id,
                            prompt = prompt,
                            editInstruction = "",
                            subject = subject,
                            styleLabel = styleLabel,
                            bitmap = bitmap,
                            isEdited = false
                        )
                    }
                    _imageStudioState.value = ImageStudioUiState.Success(
                        bitmap = bitmap,
                        prompt = prompt,
                        isEdited = false
                    )
                },
                onFailure = { e ->
                    _imageStudioState.value = ImageStudioUiState.Error(
                        FrenchErrorMessages.toFrenchError(e, "Impossible de créer l'image IA pour le moment.")
                    )
                }
            )
        }
    }

    fun editStudioImage(
        child: ChildProfile,
        sourceBitmap: Bitmap,
        editInstruction: String,
        basePrompt: String,
        subject: String
    ) {
        imageStudioJob?.cancel()
        imageStudioJob = viewModelScope.launch {
            _imageStudioState.value = ImageStudioUiState.Loading("Retouche de l'image avec Nano Banana 2.1...")
            geminiService.editEducationalImage(
                inputBitmap = sourceBitmap,
                editInstruction = editInstruction,
                onStatusUpdate = { statusMsg ->
                    _imageStudioState.value = ImageStudioUiState.Loading(statusMsg)
                }
            ).fold(
                onSuccess = { editedBitmap ->
                    runCatching {
                        localSqlRepository?.saveStudioImage(
                            childId = child.id,
                            prompt = basePrompt,
                            editInstruction = editInstruction,
                            subject = subject,
                            styleLabel = "Retouche IA",
                            bitmap = editedBitmap,
                            isEdited = true
                        )
                    }
                    _imageStudioState.value = ImageStudioUiState.Success(
                        bitmap = editedBitmap,
                        prompt = editInstruction,
                        isEdited = true
                    )
                },
                onFailure = { e ->
                    _imageStudioState.value = ImageStudioUiState.Error(
                        FrenchErrorMessages.toFrenchError(e, "Impossible de retoucher cette image pour le moment.")
                    )
                }
            )
        }
    }

    fun deleteStudioImage(item: StudioImageEntity) {
        viewModelScope.launch {
            runCatching { localSqlRepository?.deleteStudioImage(item) }
        }
    }

    suspend fun loadStudioBitmap(imagePath: String): Bitmap? =
        localSqlRepository?.loadStudioBitmap(imagePath)

    // --- Verbatim School Exercise Tab (Room SQLite + Ultra-Fast Gemini Grading) ---

    fun observeChildExerciseSheets(childId: String): Flow<List<ExerciseSheet>> =
        localSqlRepository?.observeExerciseSheetsForChild(childId) ?: flowOf(emptyList())

    fun clearExerciseOpStatus() {
        _exerciseOpStatus.value = OperationStatus.Idle
    }

    fun createIdenticalExercise(
        child: ChildProfile,
        subject: String,
        bitmap: Bitmap?,
        attachments: List<CourseAttachment>,
        notes: String
    ) {
        if (bitmap == null && attachments.isEmpty() && notes.isBlank()) {
            _exerciseOpStatus.value = OperationStatus.Error(
                "Ajoute une photo, un fichier ou le texte de ton exercice."
            )
            return
        }
        viewModelScope.launch {
            _exerciseOpStatus.value = OperationStatus.Busy(
                "Lecture et reproduction à l'identique de ton exercice..."
            )
            val result = geminiService.extractIdenticalExercise(
                bitmap = bitmap,
                attachments = attachments,
                notes = notes,
                selectedSubject = subject,
                childAge = child.age.toInt(),
                gradeLevel = child.gradeLevel
            )
            result.fold(
                onSuccess = { extracted ->
                    val sheet = ExerciseSheet(
                        childId = child.id,
                        subject = extracted.detectedSubject,
                        title = extracted.title,
                        generalInstructions = extracted.generalInstructions,
                        items = extracted.items,
                        sourceFileNames = attachments.map { it.fileName }
                    )
                    runCatching {
                        localSqlRepository?.saveExerciseSheet(sheet)
                    }.onSuccess {
                        _exerciseOpStatus.value = OperationStatus.Success(
                            message = "Exercice « ${sheet.title} » reproduit à l'identique !",
                            createdExerciseId = sheet.id
                        )
                    }.onFailure { e ->
                        _exerciseOpStatus.value = OperationStatus.Error(
                            FrenchErrorMessages.toFrenchError(e, "Impossible d'enregistrer cet exercice localement.")
                        )
                    }
                },
                onFailure = { e ->
                    _exerciseOpStatus.value = OperationStatus.Error(
                        FrenchErrorMessages.toFrenchError(
                            e,
                            "Impossible de reproduire l'exercice pour le moment. Appuie sur « Réessayer »."
                        )
                    )
                }
            )
        }
    }

    fun gradeExerciseItemAnswer(
        child: ChildProfile,
        sheet: ExerciseSheet,
        item: ExerciseItem,
        studentAnswer: String
    ) {
        val cleaned = studentAnswer.trim()
        if (cleaned.isEmpty()) return
        val key = "${sheet.id}:${item.id}"
        if (key in _gradingExerciseItemKeys.value) return

        viewModelScope.launch {
            _gradingExerciseItemKeys.value = _gradingExerciseItemKeys.value + key
            try {
                // Save student answer immediately and clear any previous error
                localSqlRepository?.updateExerciseItem(sheet.id, item.id) { current ->
                    current.copy(studentAnswer = cleaned, gradingError = null)
                }
                val gradeResult = geminiService.gradeExerciseAnswer(
                    subject = sheet.subject,
                    gradeLevel = child.gradeLevel,
                    childAge = child.age.toInt(),
                    generalInstructions = sheet.generalInstructions,
                    item = item,
                    studentAnswer = cleaned,
                    languageToneLevel = child.languageToneLevel
                )
                gradeResult.fold(
                    onSuccess = { graded ->
                        localSqlRepository?.updateExerciseItem(sheet.id, item.id) { current ->
                            current.copy(
                                studentAnswer = cleaned,
                                verdict = graded.verdict,
                                aiExplanation = graded.explanation,
                                gradingError = null
                            )
                        }
                    },
                    onFailure = { e ->
                        val friendlyErr = FrenchErrorMessages.toFrenchError(
                            e,
                            "La vérification IA a pris trop de temps. Appuie sur « Réessayer la vérification »."
                        )
                        localSqlRepository?.updateExerciseItem(sheet.id, item.id) { current ->
                            current.copy(
                                studentAnswer = cleaned,
                                gradingError = friendlyErr
                            )
                        }
                    }
                )
            } finally {
                _gradingExerciseItemKeys.value = _gradingExerciseItemKeys.value - key
            }
        }
    }

    fun finalizeExerciseSheet(
        child: ChildProfile,
        sheet: ExerciseSheet,
        draftAnswersByItemId: Map<String, String> = emptyMap()
    ) {
        if (sheet.id in _finalizingExerciseSheetIds.value) return
        viewModelScope.launch {
            _finalizingExerciseSheetIds.value = _finalizingExerciseSheetIds.value + sheet.id
            try {
                val mergedItems = sheet.items.map { item ->
                    val draft = draftAnswersByItemId[item.id]?.trim()
                    if (draft != null && draft != item.studentAnswer) {
                        item.copy(studentAnswer = draft)
                    } else {
                        item
                    }
                }
                val updatedSheet = sheet.copy(items = mergedItems, finalEvaluationError = null)
                runCatching {
                    localSqlRepository?.updateExerciseSheetDrafts(sheet.id, draftAnswersByItemId)
                }

                val evalResult = geminiService.evaluateCompleteExercise(
                    childName = child.name,
                    childAge = child.age.toInt(),
                    gradeLevel = child.gradeLevel,
                    sheet = updatedSheet,
                    languageToneLevel = child.languageToneLevel
                )
                evalResult.fold(
                    onSuccess = { evaluation ->
                        runCatching {
                            localSqlRepository?.updateExerciseSheetEvaluation(
                                sheetId = sheet.id,
                                evaluation = evaluation,
                                error = null
                            )
                        }
                    },
                    onFailure = { e ->
                        val friendlyErr = FrenchErrorMessages.toFrenchError(
                            e,
                            "Impossible de calculer la note sur 20 pour le moment. Appuie sur « Réessayer »."
                        )
                        runCatching {
                            localSqlRepository?.updateExerciseSheetEvaluation(
                                sheetId = sheet.id,
                                evaluation = sheet.finalEvaluation,
                                error = friendlyErr
                            )
                        }
                    }
                )
            } finally {
                _finalizingExerciseSheetIds.value = _finalizingExerciseSheetIds.value - sheet.id
            }
        }
    }

    fun deleteExerciseSheet(sheetId: String) {
        viewModelScope.launch {
            runCatching { localSqlRepository?.deleteExerciseSheet(sheetId) }
        }
    }

    companion object {
        private const val TAG = "EduFamilleVM"
    }
}
