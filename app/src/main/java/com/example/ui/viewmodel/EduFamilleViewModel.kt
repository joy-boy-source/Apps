package com.example.ui.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CourseAttachment
import com.example.data.FamilyRepository
import com.example.data.FrenchErrorMessages
import com.example.data.GeminiTutorService
import com.example.model.ChildProfile
import com.example.model.Family
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
    data class Success(val message: String, val createdLessonId: String? = null, val createdQuizId: String? = null) : OperationStatus
    data class Error(val message: String) : OperationStatus
}

@OptIn(ExperimentalCoroutinesApi::class)
class EduFamilleViewModel(
    private val repository: FamilyRepository,
    private val geminiService: GeminiTutorService,
    val currentUserId: String,
    val defaultDisplayName: String
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

    private val _pinError = MutableStateFlow<String?>(null)
    val pinError: StateFlow<String?> = _pinError.asStateFlow()

    // Audio Transcription state (used for voice dictation when depositing a course/exercise)
    private val _isTranscribing = MutableStateFlow(false)
    val isTranscribing: StateFlow<Boolean> = _isTranscribing.asStateFlow()

    private val _transcribeError = MutableStateFlow<String?>(null)
    val transcribeError: StateFlow<String?> = _transcribeError.asStateFlow()

    private var transcribeJob: Job? = null

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
        pin: String
    ) {
        viewModelScope.launch {
            _opStatus.value = OperationStatus.Busy("Ajout du profil...")
            repository.addChildProfile(family, name, age, gradeLevel, avatarKey, pin).fold(
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
        newPin: String?
    ) {
        viewModelScope.launch {
            _opStatus.value = OperationStatus.Busy("Mise à jour...")
            repository.updateChildProfileAdmin(familyId, childId, name, age, gradeLevel, avatarKey, newPin).fold(
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
                existingWeakPoints = child.weakPoints
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
                existingWeakPoints = child.weakPoints
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
                childWeakPoints = combinedGaps
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
                weakPoints = child.weakPoints
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
                difficulty = safeDifficulty
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
            ).onFailure { e ->
                _opStatus.value = OperationStatus.Error(
                    FrenchErrorMessages.toFrenchError(e, "Erreur lors de la sauvegarde du score.")
                )
            }
        }
    }

    companion object {
        private const val TAG = "EduFamilleVM"
    }
}
