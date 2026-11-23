package com.example.data

import android.content.Context
import com.example.R
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.example.model.AttachedDocument
import com.example.model.ChildProfile
import com.example.model.Family
import com.example.model.FamilyMessage
import com.example.model.GamificationCatalog
import com.example.model.Lesson
import com.example.model.MissedQuestionRecord
import com.example.model.PinHasher
import com.example.model.Quiz
import com.example.model.QuizAttempt
import com.example.model.QuizQuestion
import com.example.model.SchoolCatalog
import com.example.model.SkillTrackerHelper
import com.example.model.UserAccount
import com.example.model.WeeklyReviewScheduler
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FamilyRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("L'utilisateur doit être connecté avec Google.")
    }

    fun observeUserAccount(userId: String): Flow<UserAccount?> = flow {
        val uid = requireUserId()
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(UserAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    handleFlowError(error, OperationType.GET, path, uid)
                }
        )
    }

    fun observeFamily(familyId: String): Flow<Family?> = flow {
        val uid = requireUserId()
        val path = "families/$familyId"
        emitAll(
            db.collection("families").document(familyId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(Family::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    handleFlowError(error, OperationType.GET, path, uid)
                }
        )
    }

    fun observeChildren(familyId: String): Flow<List<ChildProfile>> = flow {
        val uid = requireUserId()
        val path = "families/$familyId/children"
        emitAll(
            db.collection("families").document(familyId).collection("children")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(ChildProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.name.lowercase() }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                }
        )
    }

    fun observeLessons(familyId: String): Flow<List<Lesson>> = flow {
        val uid = requireUserId()
        val path = "families/$familyId/lessons"
        emitAll(
            db.collection("families").document(familyId).collection("lessons")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Lesson::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                }
        )
    }

    fun observeQuizzes(familyId: String): Flow<List<Quiz>> = flow {
        val uid = requireUserId()
        val path = "families/$familyId/quizzes"
        emitAll(
            db.collection("families").document(familyId).collection("quizzes")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Quiz::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                }
        )
    }

    fun observeQuizAttempts(familyId: String): Flow<List<QuizAttempt>> = flow {
        val uid = requireUserId()
        val path = "families/$familyId/quiz_attempts"
        emitAll(
            db.collection("families").document(familyId).collection("quiz_attempts")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(QuizAttempt::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                }
        )
    }

    fun observeFamilyMessages(familyId: String): Flow<List<FamilyMessage>> = flow {
        val uid = requireUserId()
        val path = "families/$familyId/family_messages"
        emitAll(
            db.collection("families").document(familyId).collection("family_messages")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(FamilyMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(
                            compareBy<FamilyMessage>(
                                { it.createdAt?.seconds ?: Long.MAX_VALUE },
                                { it.createdAt?.nanoseconds ?: Int.MAX_VALUE }
                            )
                        )
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                }
        )
    }

    suspend fun sendFamilyMessage(
        family: Family,
        channelId: String,
        senderRole: String,
        senderName: String,
        text: String
    ): Result<String> {
        val docRef = db.collection("families").document(family.id).collection("family_messages").document()
        return try {
            val uid = requireUserId()
            val cleanText = text.trim().take(1000)
            require(cleanText.isNotEmpty()) { "Le message ne peut pas être vide." }
            val safeChannel = channelId.trim().ifEmpty { FamilyMessage.CHANNEL_FAMILY_GROUP }
            val safeRole = if (senderRole == "CHILD") "CHILD" else "PARENT"
            val safeSenderName = senderName.trim().ifEmpty {
                if (safeRole == "PARENT") "Parent" else "Enfant"
            }.take(60)
            val members = (family.memberUids + family.ownerId + uid).distinct()

            val payload = mapOf(
                "familyId" to family.id,
                "ownerId" to family.ownerId,
                "memberUids" to members,
                "channelId" to safeChannel,
                "senderRole" to safeRole,
                "senderName" to safeSenderName,
                "senderUid" to uid,
                "text" to cleanText,
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteFamilyMessage(familyId: String, messageId: String): Result<Unit> {
        val docRef = db.collection("families").document(familyId).collection("family_messages").document(messageId)
        return try {
            requireUserId()
            require(messageId.isNotBlank()) { "Identifiant de message invalide." }
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    private fun handleFlowError(error: Throwable, op: OperationType, path: String, uid: String): Nothing {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, op, path)
        }
        throw error
    }

    suspend fun createFamilyAsParent(
        familyName: String,
        displayName: String,
        parentPin: String
    ): Result<String> {
        var docPath = "families"
        return try {
            val uid = requireUserId()
            val cleanName = familyName.trim().take(60)
            require(cleanName.isNotEmpty()) { "Le nom de la famille est requis." }
            require(parentPin.length == 4 && parentPin.all { it.isDigit() }) { "Le code PIN doit comporter 4 chiffres." }

            val inviteCode = PinHasher.generateInviteCode()
            val pinHash = PinHasher.hashPin(parentPin)
            val safeDisplay = displayName.trim().ifEmpty { "Parent" }.take(80)

            val familyRef = db.collection("families").document(uid)
            val inviteRef = db.collection("invite_codes").document(inviteCode)
            val userRef = db.collection("users").document(uid)

            docPath = familyRef.path
            val batch = db.batch()
            batch.set(
                familyRef,
                mapOf(
                    "ownerId" to uid,
                    "familyName" to cleanName,
                    "inviteCode" to inviteCode,
                    "parentPinHash" to pinHash,
                    "memberUids" to listOf(uid),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                inviteRef,
                mapOf(
                    "familyId" to uid,
                    "ownerId" to uid,
                    "familyName" to cleanName,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                userRef,
                mapOf(
                    "userId" to uid,
                    "displayName" to safeDisplay,
                    "role" to "PARENT",
                    "familyId" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(uid)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun joinFamilyAsChild(
        inviteCodeInput: String,
        childName: String,
        age: Int,
        gradeLevel: String,
        avatarKey: String,
        childPin: String
    ): Result<Pair<String, String>> {
        var docPath = "invite_codes"
        var currentOp = OperationType.GET
        return try {
            val uid = requireUserId()
            val code = inviteCodeInput.trim().uppercase()
            require(code.length == 6 && code.all { it.isLetterOrDigit() }) {
                "Le code famille doit contenir 6 caractères."
            }
            val cleanChildName = childName.trim().take(40)
            require(cleanChildName.isNotEmpty()) { "Le prénom de l'enfant est requis." }
            require(age in 5..18) { "L'âge doit être compris entre 5 et 18 ans." }
            require(childPin.length == 4 && childPin.all { it.isDigit() }) { "Le code PIN doit comporter 4 chiffres." }

            docPath = "invite_codes/$code"
            currentOp = OperationType.GET
            val inviteDoc = db.collection("invite_codes").document(code).get().await()
            if (!inviteDoc.exists()) {
                return Result.failure(IllegalArgumentException("Code famille introuvable : $code"))
            }
            val familyId = inviteDoc.getString("familyId") ?: ""
            val ownerId = inviteDoc.getString("ownerId") ?: ""
            require(familyId.isNotBlank() && ownerId.isNotBlank()) { "Code famille invalide." }

            val familyRef = db.collection("families").document(familyId)
            docPath = familyRef.path
            currentOp = OperationType.UPDATE
            // Add the child's UID to memberUids using FieldValue.arrayUnion BEFORE reading the family doc
            familyRef.update(
                mapOf(
                    "memberUids" to FieldValue.arrayUnion(uid),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            currentOp = OperationType.GET
            val familySnap = familyRef.get().await()
            val updatedMembers = ((familySnap.get("memberUids") as? List<*>)
                ?.filterIsInstance<String>()
                ?: listOf(ownerId, uid)).let { (it + ownerId + uid).distinct() }

            val childRef = familyRef.collection("children").document()
            val userRef = db.collection("users").document(uid)
            val existingUser = runCatching { userRef.get().await() }.getOrNull()
            docPath = childRef.path
            currentOp = OperationType.CREATE
            val batch = db.batch()
            batch.set(
                childRef,
                mapOf(
                    "familyId" to familyId,
                    "ownerId" to ownerId,
                    "memberUids" to updatedMembers,
                    "linkedUid" to uid,
                    "name" to cleanChildName,
                    "age" to age.toLong(),
                    "gradeLevel" to gradeLevel.trim().take(30),
                    "avatarKey" to avatarKey,
                    "pinHash" to PinHasher.hashPin(childPin),
                    "lessonsCount" to 0L,
                    "quizzesTaken" to 0L,
                    "totalCorrect" to 0L,
                    "totalQuestions" to 0L,
                    "xp" to 0L,
                    "bestStreak" to 0L,
                    "badges" to emptyList<String>(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (existingUser != null && existingUser.exists()) {
                batch.update(
                    userRef,
                    mapOf(
                        "displayName" to cleanChildName,
                        "role" to "CHILD",
                        "familyId" to familyId,
                        "linkedChildId" to childRef.id,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.set(
                    userRef,
                    mapOf(
                        "userId" to uid,
                        "displayName" to cleanChildName,
                        "role" to "CHILD",
                        "familyId" to familyId,
                        "linkedChildId" to childRef.id,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(familyId to childRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, currentOp, docPath)
            Result.failure(e)
        }
    }

    suspend fun addChildProfile(
        family: Family,
        name: String,
        age: Int,
        gradeLevel: String,
        avatarKey: String,
        pin: String,
        languageToneLevel: String = com.example.model.LanguageToneCatalog.AUTO
    ): Result<String> {
        val docRef = db.collection("families").document(family.id).collection("children").document()
        return try {
            val uid = requireUserId()
            val cleanName = name.trim().take(40)
            require(cleanName.isNotEmpty()) { "Le prénom est requis." }
            require(age in 5..18) { "Âge invalide (5 à 18 ans)." }
            require(pin.length == 4 && pin.all { it.isDigit() }) { "Le code PIN doit comporter 4 chiffres." }

            val members = (family.memberUids + family.ownerId + uid).distinct()
            val payload = mapOf(
                "familyId" to family.id,
                "ownerId" to family.ownerId,
                "memberUids" to members,
                "name" to cleanName,
                "age" to age.toLong(),
                "gradeLevel" to gradeLevel.trim().take(30),
                "avatarKey" to avatarKey,
                "pinHash" to PinHasher.hashPin(pin),
                "languageToneLevel" to com.example.model.LanguageToneCatalog.normalize(languageToneLevel),
                "lessonsCount" to 0L,
                "quizzesTaken" to 0L,
                "totalCorrect" to 0L,
                "totalQuestions" to 0L,
                "xp" to 0L,
                "bestStreak" to 0L,
                "badges" to emptyList<String>(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateChildSchoolLevel(
        familyId: String,
        childId: String,
        age: Int,
        gradeLevel: String
    ): Result<Unit> {
        val docRef = db.collection("families").document(familyId).collection("children").document(childId)
        return try {
            requireUserId()
            require(age in 5..18) { "Âge invalide (5 à 18 ans)." }
            docRef.update(
                mapOf(
                    "age" to age.toLong(),
                    "gradeLevel" to gradeLevel.trim().take(30),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateChildProfileAdmin(
        familyId: String,
        childId: String,
        name: String,
        age: Int,
        gradeLevel: String,
        avatarKey: String,
        newPin: String?,
        languageToneLevel: String? = null
    ): Result<Unit> {
        val docRef = db.collection("families").document(familyId).collection("children").document(childId)
        return try {
            requireUserId()
            val cleanName = name.trim().take(40)
            require(cleanName.isNotEmpty()) { "Le prénom est requis." }
            require(age in 5..18) { "Âge invalide." }
            val updates = mutableMapOf<String, Any>(
                "name" to cleanName,
                "age" to age.toLong(),
                "gradeLevel" to gradeLevel.trim().take(30),
                "avatarKey" to avatarKey,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!languageToneLevel.isNullOrBlank()) {
                updates["languageToneLevel"] = com.example.model.LanguageToneCatalog.normalize(languageToneLevel)
            }
            if (!newPin.isNullOrBlank()) {
                require(newPin.length == 4 && newPin.all { it.isDigit() }) { "PIN à 4 chiffres requis." }
                updates["pinHash"] = PinHasher.hashPin(newPin)
            }
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateChildLanguageTone(
        familyId: String,
        childId: String,
        languageToneLevel: String
    ): Result<Unit> {
        val docRef = db.collection("families").document(familyId).collection("children").document(childId)
        return try {
            requireUserId()
            docRef.update(
                mapOf(
                    "languageToneLevel" to com.example.model.LanguageToneCatalog.normalize(languageToneLevel),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteChildProfile(familyId: String, childId: String): Result<Unit> {
        val familyRef = db.collection("families").document(familyId)
        val docRef = familyRef.collection("children").document(childId)
        return try {
            val uid = requireUserId()
            // Cascade cleanup of lessons, quizzes, and attempts owned by this child
            val refsToDelete = mutableListOf(docRef)
            for (subcollection in listOf("lessons", "quizzes", "quiz_attempts")) {
                val snap = runCatching {
                    familyRef.collection(subcollection)
                        .whereArrayContains("memberUids", uid)
                        .whereEqualTo("childId", childId)
                        .get()
                        .await()
                }.getOrNull()
                snap?.documents?.forEach { refsToDelete.add(it.reference) }
            }
            val privateMsgsSnap = runCatching {
                familyRef.collection("family_messages")
                    .whereArrayContains("memberUids", uid)
                    .whereEqualTo("channelId", childId)
                    .get()
                    .await()
            }.getOrNull()
            privateMsgsSnap?.documents?.forEach { refsToDelete.add(it.reference) }
            refsToDelete.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { batch.delete(it) }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun saveGeneratedLessonAndQuiz(
        family: Family,
        child: ChildProfile,
        subject: String,
        documentType: String = "Cours",
        difficulty: String = "Moyen",
        sourceNotes: String,
        photoAttached: Boolean,
        attachedFiles: List<AttachedDocument> = emptyList(),
        generated: GeneratedLessonAndQuiz
    ): Result<Pair<String, String>> {
        val familyRef = db.collection("families").document(family.id)
        val lessonRef = familyRef.collection("lessons").document()
        val quizRef = familyRef.collection("quizzes").document()
        val childRef = familyRef.collection("children").document(child.id)

        return try {
            val uid = requireUserId()
            val members = (family.memberUids + family.ownerId + uid).distinct()
            val finalSubject = if (subject.equals(SchoolCatalog.AUTO_SUBJECT, ignoreCase = true)) {
                SchoolCatalog.normalizeSubject(generated.detectedSubject, "Autre")
            } else {
                SchoolCatalog.normalizeSubject(subject, "Autre")
            }
            val safeDocType = SchoolCatalog.normalizeDocumentType(documentType)
            val safeDifficulty = SchoolCatalog.normalizeDifficulty(difficulty)

            val filesPayload = attachedFiles.take(20).map { doc ->
                mapOf(
                    "fileName" to doc.fileName.take(120),
                    "mimeType" to doc.mimeType.take(100),
                    "sizeBytes" to doc.sizeBytes.coerceAtLeast(0L)
                )
            }

            val acquiredClean = generated.acquiredSkills.map { it.trim().take(100) }.filter { it.isNotEmpty() }.take(15)
            val weakClean = generated.weakPoints.map { it.trim().take(100) }.filter { it.isNotEmpty() }.take(15)
            val gapsClean = generated.targetedGaps.map { it.trim().take(100) }.filter { it.isNotEmpty() }.take(15)

            val lessonPayload = mapOf(
                "familyId" to family.id,
                "ownerId" to family.ownerId,
                "memberUids" to members,
                "childId" to child.id,
                "childName" to child.name.take(40),
                "gradeLevel" to child.gradeLevel.take(30),
                "childAge" to child.age,
                "subject" to finalSubject.take(40),
                "documentType" to safeDocType,
                "title" to generated.lessonTitle.take(120),
                "sourceNotes" to sourceNotes.take(4000),
                "summaryIntro" to generated.summaryIntro.take(2000),
                "keyPoints" to generated.keyPoints.take(12),
                "simpleExplanation" to generated.simpleExplanation.take(6000),
                "memoryTip" to generated.memoryTip.take(1000),
                "acquiredSkills" to acquiredClean,
                "weakPoints" to weakClean,
                "evaluationSummary" to generated.evaluationSummary.take(1500),
                "photoAttached" to photoAttached,
                "attachedFiles" to filesPayload,
                "quizCount" to 1L,
                "attemptsCount" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val quizPayload = mapOf(
                "familyId" to family.id,
                "ownerId" to family.ownerId,
                "memberUids" to members,
                "lessonId" to lessonRef.id,
                "childId" to child.id,
                "subject" to finalSubject.take(40),
                "title" to generated.quizTitle.take(120),
                "difficulty" to safeDifficulty,
                "questions" to generated.questions.toFirestoreMaps(),
                "targetedGaps" to gapsClean,
                "attemptsCount" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val (mergedAcquired, mergedWeak) = SkillTrackerHelper.mergeChildSkills(
                existingAcquired = child.acquiredSkills,
                existingWeak = child.weakPoints,
                newlyAcquired = acquiredClean,
                newlyWeak = weakClean
            )

            val batch = db.batch()
            batch.set(lessonRef, lessonPayload)
            batch.set(quizRef, quizPayload)
            batch.update(
                childRef,
                mapOf(
                    "lessonsCount" to FieldValue.increment(1L),
                    "acquiredSkills" to mergedAcquired,
                    "weakPoints" to mergedWeak,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(lessonRef.id to quizRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, lessonRef.path)
            Result.failure(e)
        }
    }

    suspend fun enrichLessonWithDocumentsAndQuiz(
        family: Family,
        child: ChildProfile,
        lesson: Lesson,
        newAttachments: List<AttachedDocument>,
        addedNotes: String,
        photoAttachedNow: Boolean,
        generated: GeneratedLessonAndQuiz
    ): Result<String> {
        val familyRef = db.collection("families").document(family.id)
        val lessonRef = familyRef.collection("lessons").document(lesson.id)
        val quizRef = familyRef.collection("quizzes").document()
        val childRef = familyRef.collection("children").document(child.id)

        return try {
            val uid = requireUserId()
            val members = (family.memberUids + family.ownerId + uid).distinct()

            val mergedAttachments = (lesson.attachedFiles + newAttachments)
                .take(20)
                .map { doc ->
                    mapOf(
                        "fileName" to doc.fileName.take(120),
                        "mimeType" to doc.mimeType.take(100),
                        "sizeBytes" to doc.sizeBytes.coerceAtLeast(0L)
                    )
                }

            val additionsLabel = buildList {
                if (newAttachments.isNotEmpty()) {
                    add("Fichiers ajoutés : ${newAttachments.joinToString(", ") { it.fileName }}")
                }
                if (addedNotes.isNotBlank()) {
                    add(addedNotes.trim())
                }
            }.joinToString(" — ")

            val mergedNotes = listOf(lesson.sourceNotes.trim(), additionsLabel)
                .filter { it.isNotBlank() }
                .joinToString("\n")
                .take(4000)

            val acquiredClean = (lesson.acquiredSkills + generated.acquiredSkills)
                .map { it.trim().take(100) }.filter { it.isNotEmpty() }.distinct().take(15)
            val weakClean = (lesson.weakPoints + generated.weakPoints)
                .map { it.trim().take(100) }.filter { it.isNotEmpty() }.distinct().take(15)
            val gapsClean = generated.targetedGaps.map { it.trim().take(100) }.filter { it.isNotEmpty() }.take(15)

            val quizPayload = mapOf(
                "familyId" to family.id,
                "ownerId" to family.ownerId,
                "memberUids" to members,
                "lessonId" to lesson.id,
                "childId" to lesson.childId,
                "subject" to lesson.subject.take(40),
                "title" to generated.quizTitle.take(120),
                "difficulty" to "Moyen",
                "questions" to generated.questions.toFirestoreMaps(),
                "targetedGaps" to gapsClean,
                "attemptsCount" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val (mergedChildAcquired, mergedChildWeak) = SkillTrackerHelper.mergeChildSkills(
                existingAcquired = child.acquiredSkills,
                existingWeak = child.weakPoints,
                newlyAcquired = generated.acquiredSkills,
                newlyWeak = generated.weakPoints
            )

            val batch = db.batch()
            batch.update(
                lessonRef,
                mapOf(
                    "sourceNotes" to mergedNotes,
                    "summaryIntro" to generated.summaryIntro.take(2000),
                    "keyPoints" to generated.keyPoints.take(12),
                    "simpleExplanation" to generated.simpleExplanation.take(6000),
                    "memoryTip" to generated.memoryTip.take(1000),
                    "acquiredSkills" to acquiredClean,
                    "weakPoints" to weakClean,
                    "evaluationSummary" to generated.evaluationSummary.ifBlank { lesson.evaluationSummary }.take(1500),
                    "photoAttached" to (lesson.photoAttached || photoAttachedNow),
                    "attachedFiles" to mergedAttachments,
                    "quizCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(quizRef, quizPayload)
            batch.update(
                childRef,
                mapOf(
                    "acquiredSkills" to mergedChildAcquired,
                    "weakPoints" to mergedChildWeak,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(quizRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, lessonRef.path)
            Result.failure(e)
        }
    }

    suspend fun saveAdditionalQuiz(
        family: Family,
        lesson: Lesson,
        difficulty: String,
        generated: GeneratedQuiz
    ): Result<String> {
        val familyRef = db.collection("families").document(family.id)
        val quizRef = familyRef.collection("quizzes").document()
        val lessonRef = familyRef.collection("lessons").document(lesson.id)

        return try {
            val uid = requireUserId()
            val members = (family.memberUids + family.ownerId + uid).distinct()
            val gapsClean = generated.targetedGaps.map { it.trim().take(100) }.filter { it.isNotEmpty() }.take(15)

            val quizPayload = mapOf(
                "familyId" to family.id,
                "ownerId" to family.ownerId,
                "memberUids" to members,
                "lessonId" to lesson.id,
                "childId" to lesson.childId,
                "subject" to lesson.subject.take(40),
                "title" to generated.quizTitle.take(120),
                "difficulty" to difficulty,
                "questions" to generated.questions.toFirestoreMaps(),
                "targetedGaps" to gapsClean,
                "attemptsCount" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val batch = db.batch()
            batch.set(quizRef, quizPayload)
            batch.update(
                lessonRef,
                mapOf(
                    "quizCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(quizRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, quizRef.path)
            Result.failure(e)
        }
    }

    suspend fun saveSundayWeeklyReviewQuiz(
        family: Family,
        child: ChildProfile,
        weekKey: String,
        difficulty: String,
        generated: GeneratedQuiz
    ): Result<String> {
        val familyRef = db.collection("families").document(family.id)
        val quizRef = familyRef.collection("quizzes").document()
        val childRef = familyRef.collection("children").document(child.id)

        return try {
            val uid = requireUserId()
            val members = (family.memberUids + family.ownerId + uid).distinct()
            val safeDifficulty = SchoolCatalog.normalizeDifficulty(difficulty)
            val safeWeekKey = weekKey.trim().take(20)
            val gapsClean = generated.targetedGaps.map { it.trim().take(100) }.filter { it.isNotEmpty() }.take(15)

            val quizPayload = mapOf(
                "familyId" to family.id,
                "ownerId" to family.ownerId,
                "memberUids" to members,
                "lessonId" to WeeklyReviewScheduler.WEEKLY_REVIEW_LESSON_ID,
                "childId" to child.id,
                "subject" to "Révision Hebdo",
                "title" to generated.quizTitle.take(120),
                "difficulty" to safeDifficulty,
                "questions" to generated.questions.toFirestoreMaps(),
                "targetedGaps" to gapsClean,
                "weeklyReview" to true,
                "weekKey" to safeWeekKey,
                "attemptsCount" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val batch = db.batch()
            batch.set(quizRef, quizPayload)
            batch.update(
                childRef,
                mapOf(
                    "lastSundayQuizWeekKey" to safeWeekKey,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(quizRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, quizRef.path)
            Result.failure(e)
        }
    }

    suspend fun recordQuizAttempt(
        family: Family,
        child: ChildProfile,
        lesson: Lesson?,
        quiz: Quiz,
        score: Int,
        totalQuestions: Int,
        maxStreak: Int = 0,
        masteredSkills: List<String> = emptyList(),
        weakSkills: List<String> = emptyList(),
        earnedXpOverride: Long? = null,
        missedQuestions: List<MissedQuestionRecord> = emptyList()
    ): Result<Int> {
        val familyRef = db.collection("families").document(family.id)
        val attemptRef = familyRef.collection("quiz_attempts").document()
        val quizRef = familyRef.collection("quizzes").document(quiz.id)
        val lessonRef = lesson?.let { familyRef.collection("lessons").document(it.id) }
        val childRef = familyRef.collection("children").document(child.id)

        return try {
            val uid = requireUserId()
            require(totalQuestions >= 1 && score in 0..totalQuestions) { "Score invalide." }
            val percentage = ((score * 100) / totalQuestions).coerceIn(0, 100)
            val safeMaxStreak = maxStreak.coerceIn(0, totalQuestions)
            val members = (family.memberUids + family.ownerId + uid).distinct()
            val masteredClean = masteredSkills.map { it.trim().take(100) }.filter { it.isNotEmpty() }.distinct().take(20)
            val weakClean = weakSkills.map { it.trim().take(100) }.filter { it.isNotEmpty() }.distinct().take(20)
            val effectiveLessonId = lesson?.id?.takeIf { it.isNotBlank() }
                ?: quiz.lessonId.takeIf { it.isNotBlank() }
                ?: WeeklyReviewScheduler.WEEKLY_REVIEW_LESSON_ID
            val effectiveLessonTitle = (lesson?.title ?: quiz.title).take(120)

            db.runTransaction { transaction ->
                // Read fresh documents inside transaction to prevent XP farming on rapid/duplicate replays
                val quizSnap = transaction.get(quizRef)
                val lessonSnap = lessonRef?.let { transaction.get(it) }
                val childSnap = transaction.get(childRef)

                val existingQuizAttempts = quizSnap.getLong("attemptsCount") ?: quiz.attemptsCount
                val existingQuizLastScore = quizSnap.getLong("lastScorePercent") ?: quiz.lastScorePercent
                val isReplay = existingQuizAttempts > 0L || existingQuizLastScore != null

                val xpEarned = when {
                    isReplay -> 0L
                    earnedXpOverride != null -> earnedXpOverride.coerceIn(0L, 5000L)
                    else -> GamificationCatalog.calculateQuizXp(
                        score = score,
                        totalQuestions = totalQuestions,
                        maxStreak = safeMaxStreak,
                        difficulty = quiz.difficulty,
                        isReplay = false
                    )
                }

                val existingLessonBest = lessonSnap?.getLong("bestScorePercent") ?: lesson?.bestScorePercent ?: 0L
                val newBestScore = maxOf(existingLessonBest, percentage.toLong())

                val existingAcquired = (childSnap.get("acquiredSkills") as? List<*>)
                    ?.filterIsInstance<String>()
                    ?: child.acquiredSkills
                val existingWeak = (childSnap.get("weakPoints") as? List<*>)
                    ?.filterIsInstance<String>()
                    ?: child.weakPoints
                val (updatedAcquired, updatedWeak) = SkillTrackerHelper.mergeChildSkills(
                    existingAcquired = existingAcquired,
                    existingWeak = existingWeak,
                    newlyAcquired = masteredClean,
                    newlyWeak = weakClean
                )

                val existingMissed = parseMissedQuestionMaps(childSnap.get("weeklyMissedQuestions"))
                    .ifEmpty { child.weeklyMissedQuestions }
                val mergedMissed = WeeklyReviewScheduler.mergeWeeklyMissedQuestions(
                    existing = existingMissed,
                    newlyMissed = missedQuestions,
                    maxItems = 40
                )
                val missedPayload = mergedMissed.toMissedFirestoreMaps()

                val attemptPayload = mapOf(
                    "familyId" to family.id,
                    "ownerId" to family.ownerId,
                    "memberUids" to members,
                    "childId" to child.id,
                    "childName" to child.name.take(40),
                    "lessonId" to effectiveLessonId,
                    "lessonTitle" to effectiveLessonTitle,
                    "quizId" to quiz.id,
                    "subject" to quiz.subject.take(40),
                    "score" to score.toLong(),
                    "totalQuestions" to totalQuestions.toLong(),
                    "percentage" to percentage.toLong(),
                    "xpEarned" to xpEarned,
                    "maxStreak" to safeMaxStreak.toLong(),
                    "masteredSkills" to masteredClean,
                    "weakSkills" to weakClean,
                    "createdAt" to FieldValue.serverTimestamp()
                )

                transaction.set(attemptRef, attemptPayload)
                transaction.update(
                    quizRef,
                    mapOf(
                        "lastScorePercent" to percentage.toLong(),
                        "attemptsCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                if (lessonRef != null && lessonSnap != null && lessonSnap.exists()) {
                    transaction.update(
                        lessonRef,
                        mapOf(
                            "bestScorePercent" to newBestScore,
                            "attemptsCount" to FieldValue.increment(1L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                // Only grant XP, quiz completion count, and XP-backed progression on the FIRST completion of a quiz.
                if (!isReplay) {
                    val currentXp = childSnap.getLong("xp") ?: child.xp
                    val currentQuizzesTaken = childSnap.getLong("quizzesTaken") ?: child.quizzesTaken
                    val currentBestStreak = childSnap.getLong("bestStreak") ?: child.bestStreak
                    val currentBadges = (childSnap.get("badges") as? List<*>)
                        ?.filterIsInstance<String>()
                        ?: child.badges

                    val newBestStreak = maxOf(currentBestStreak, safeMaxStreak.toLong())
                    val updatedBadges = GamificationCatalog.computeUnlockedBadges(
                        existingBadges = currentBadges,
                        newQuizzesTaken = currentQuizzesTaken + 1L,
                        newTotalXp = currentXp + xpEarned,
                        percentage = percentage,
                        maxStreak = safeMaxStreak,
                        difficulty = quiz.difficulty
                    )

                    val childUpdates = mutableMapOf<String, Any>(
                        "quizzesTaken" to FieldValue.increment(1L),
                        "totalCorrect" to FieldValue.increment(score.toLong()),
                        "totalQuestions" to FieldValue.increment(totalQuestions.toLong()),
                        "xp" to FieldValue.increment(xpEarned),
                        "bestStreak" to newBestStreak,
                        "badges" to updatedBadges,
                        "acquiredSkills" to updatedAcquired,
                        "weakPoints" to updatedWeak,
                        "weeklyMissedQuestions" to missedPayload,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    if (quiz.weeklyReview && quiz.weekKey.isNotBlank()) {
                        childUpdates["lastSundayQuizWeekKey"] = quiz.weekKey.take(20)
                    }
                    transaction.update(childRef, childUpdates)
                } else if (masteredClean.isNotEmpty() || weakClean.isNotEmpty() || missedQuestions.isNotEmpty()) {
                    // On replay: 0 XP, but still update competency mastery / gaps and store first-try wrong answers!
                    val replayUpdates = mutableMapOf<String, Any>(
                        "acquiredSkills" to updatedAcquired,
                        "weakPoints" to updatedWeak,
                        "weeklyMissedQuestions" to missedPayload,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    transaction.update(childRef, replayUpdates)
                }
            }.await()

            Result.success(percentage)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, attemptRef.path)
            Result.failure(e)
        }
    }

    private fun parseMissedQuestionMaps(raw: Any?): List<MissedQuestionRecord> {
        val list = raw as? List<*> ?: return emptyList()
        return list.mapNotNull { entry ->
            val map = entry as? Map<*, *> ?: return@mapNotNull null
            MissedQuestionRecord(
                subject = (map["subject"] as? String).orEmpty(),
                lessonId = (map["lessonId"] as? String).orEmpty(),
                lessonTitle = (map["lessonTitle"] as? String).orEmpty(),
                quizId = (map["quizId"] as? String).orEmpty(),
                skillTag = (map["skillTag"] as? String).orEmpty(),
                prompt = (map["prompt"] as? String).orEmpty(),
                weekKey = (map["weekKey"] as? String).orEmpty()
            )
        }
    }

    private fun List<MissedQuestionRecord>.toMissedFirestoreMaps(): List<Map<String, Any>> {
        return takeLast(40).map { rec ->
            mapOf(
                "subject" to rec.subject.take(40),
                "lessonId" to rec.lessonId.take(128),
                "lessonTitle" to rec.lessonTitle.take(120),
                "quizId" to rec.quizId.take(128),
                "skillTag" to rec.skillTag.take(100),
                "prompt" to rec.prompt.take(240),
                "weekKey" to rec.weekKey.take(20)
            )
        }
    }

    private fun List<QuizQuestion>.toFirestoreMaps(): List<Map<String, Any>> {
        return take(10).map { q ->
            mapOf(
                "prompt" to q.prompt,
                "options" to q.options,
                "correctIndex" to q.correctIndex.toLong(),
                "correctIndices" to q.correctIndices.map { it.toLong() },
                "allowMultipleSelection" to q.allowMultipleSelection,
                "explanation" to q.explanation,
                "questionType" to q.questionType,
                "sentenceTemplate" to q.sentenceTemplate,
                "acceptedAnswers" to q.acceptedAnswers,
                "matchingPairs" to q.matchingPairs.map { pair ->
                    mapOf("left" to pair.left, "right" to pair.right)
                },
                "orderedItems" to q.orderedItems,
                "imageOptions" to q.imageOptions.map { opt ->
                    mapOf("label" to opt.label, "visualKey" to opt.visualKey)
                },
                "illustrationKey" to q.illustrationKey,
                "illustrationCaption" to q.illustrationCaption,
                "guidingHint" to q.guidingHint,
                "secondHint" to q.secondHint,
                "skillTag" to q.skillTag
            )
        }
    }
}
