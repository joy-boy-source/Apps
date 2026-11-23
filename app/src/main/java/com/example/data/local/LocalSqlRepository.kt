package com.example.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class LocalSqlRepository(
    private val dao: QuizzySqlDao,
    private val appContext: Context
) {
    fun observeGameScoresForChild(childId: String): Flow<List<GameScoreEntity>> =
        dao.observeGameScoresForChild(childId)

    fun observeAllTopGameScores(): Flow<List<GameScoreEntity>> =
        dao.observeAllTopGameScores()

    fun observeStudioImagesForChild(childId: String): Flow<List<StudioImageEntity>> =
        dao.observeStudioImagesForChild(childId)

    fun observeAllStudioImages(): Flow<List<StudioImageEntity>> =
        dao.observeAllStudioImages()

    fun observeSubjectStatsForChild(childId: String): Flow<List<SubjectSqlStats>> =
        dao.observeSubjectStatsForChild(childId)

    fun observeAllSubjectStats(): Flow<List<SubjectSqlStats>> =
        dao.observeAllSubjectStats()

    fun observeChildSqlOverview(childId: String): Flow<ChildSqlOverview> = combine(
        dao.observeGameSessionsCount(childId),
        dao.observeBestGameScore(childId),
        dao.observeStudioImagesCount(childId),
        dao.observeStudyLogsCount(childId)
    ) { gamesCount, bestScore, imagesCount, logsCount ->
        ChildSqlOverview(
            totalGameSessions = gamesCount,
            bestGameScore = bestScore,
            totalStudioImages = imagesCount,
            totalStudyLogs = logsCount
        )
    }

    suspend fun recordGameSession(
        childId: String,
        childName: String,
        subject: String,
        gameMode: String,
        score: Int,
        correctCount: Int,
        totalQuestions: Int,
        maxCombo: Int,
        durationSeconds: Int
    ): Long {
        val id = dao.insertGameScore(
            GameScoreEntity(
                childId = childId,
                childName = childName,
                subject = subject,
                gameMode = gameMode,
                score = score,
                correctCount = correctCount,
                totalQuestions = totalQuestions,
                maxCombo = maxCombo,
                durationSeconds = durationSeconds
            )
        )
        val pct = if (totalQuestions > 0) ((correctCount * 100) / totalQuestions).coerceIn(0, 100) else 100
        dao.insertStudyLog(
            StudySqlLogEntity(
                childId = childId,
                subject = subject,
                activityType = "GAME_3D",
                scorePercent = pct,
                xpDelta = (score / 10).coerceAtLeast(5)
            )
        )
        return id
    }

    suspend fun recordQuizStudyLog(
        childId: String,
        subject: String,
        scorePercent: Int,
        xpDelta: Int
    ) {
        dao.insertStudyLog(
            StudySqlLogEntity(
                childId = childId,
                subject = subject,
                activityType = "QUIZ",
                scorePercent = scorePercent.coerceIn(0, 100),
                xpDelta = xpDelta.coerceAtLeast(0)
            )
        )
    }

    suspend fun saveStudioImage(
        childId: String,
        prompt: String,
        editInstruction: String,
        subject: String,
        styleLabel: String,
        bitmap: Bitmap,
        isEdited: Boolean
    ): StudioImageEntity = withContext(Dispatchers.IO) {
        val dir = File(appContext.filesDir, "studio_images").apply { mkdirs() }
        val file = File(dir, "img_${childId}_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
        }
        val entity = StudioImageEntity(
            childId = childId,
            prompt = prompt.trim(),
            editInstruction = editInstruction.trim(),
            subject = subject.ifBlank { "Général" },
            styleLabel = styleLabel.ifBlank { "Illustration" },
            imagePath = file.absolutePath,
            isEdited = isEdited
        )
        val insertedId = dao.insertStudioImage(entity)
        dao.insertStudyLog(
            StudySqlLogEntity(
                childId = childId,
                subject = entity.subject,
                activityType = "STUDIO_IMAGE",
                scorePercent = -1,
                xpDelta = 10
            )
        )
        entity.copy(id = insertedId)
    }

    suspend fun deleteStudioImage(item: StudioImageEntity) = withContext(Dispatchers.IO) {
        runCatching { File(item.imagePath).delete() }
        dao.deleteStudioImageById(item.id)
    }

    suspend fun loadStudioBitmap(imagePath: String): Bitmap? = withContext(Dispatchers.IO) {
        val file = File(imagePath)
        if (!file.exists()) return@withContext null
        BitmapFactory.decodeFile(file.absolutePath)
    }

    // --- Verbatim School Exercise Sheets ---

    fun observeExerciseSheetsForChild(childId: String): Flow<List<com.example.model.ExerciseSheet>> =
        dao.observeExerciseSheetsForChild(childId).map { entities ->
            entities.map { it.toDomainModel() }
        }

    suspend fun saveExerciseSheet(sheet: com.example.model.ExerciseSheet) = withContext(Dispatchers.IO) {
        dao.upsertExerciseSheet(sheet.toEntity())
    }

    suspend fun updateExerciseItem(
        sheetId: String,
        itemId: String,
        transform: (com.example.model.ExerciseItem) -> com.example.model.ExerciseItem
    ): com.example.model.ExerciseSheet? = withContext(Dispatchers.IO) {
        val current = dao.getExerciseSheetById(sheetId)?.toDomainModel() ?: return@withContext null
        val updatedItems = current.items.map { item ->
            if (item.id == itemId) transform(item) else item
        }
        val updatedSheet = current.copy(
            items = updatedItems,
            updatedAt = System.currentTimeMillis()
        )
        dao.upsertExerciseSheet(updatedSheet.toEntity())
        updatedSheet
    }

    suspend fun updateExerciseSheetDrafts(
        sheetId: String,
        draftAnswersByItemId: Map<String, String>
    ): com.example.model.ExerciseSheet? = withContext(Dispatchers.IO) {
        val current = dao.getExerciseSheetById(sheetId)?.toDomainModel() ?: return@withContext null
        val updatedItems = current.items.map { item ->
            val draft = draftAnswersByItemId[item.id]
            if (draft != null && draft.trim() != item.studentAnswer.trim()) {
                item.copy(studentAnswer = draft.trim())
            } else {
                item
            }
        }
        val updatedSheet = current.copy(
            items = updatedItems,
            finalEvaluationError = null,
            updatedAt = System.currentTimeMillis()
        )
        dao.upsertExerciseSheet(updatedSheet.toEntity())
        updatedSheet
    }

    suspend fun updateExerciseSheetEvaluation(
        sheetId: String,
        evaluation: com.example.model.ExerciseFinalEvaluation?,
        error: String?
    ): com.example.model.ExerciseSheet? = withContext(Dispatchers.IO) {
        val current = dao.getExerciseSheetById(sheetId)?.toDomainModel() ?: return@withContext null
        val updatedSheet = current.copy(
            finalEvaluation = evaluation ?: current.finalEvaluation,
            finalEvaluationError = error,
            updatedAt = System.currentTimeMillis()
        )
        dao.upsertExerciseSheet(updatedSheet.toEntity())
        if (evaluation != null) {
            dao.insertStudyLog(
                StudySqlLogEntity(
                    childId = current.childId,
                    subject = current.subject,
                    activityType = "EXERCISE",
                    scorePercent = evaluation.percentage,
                    xpDelta = (evaluation.scoreOutOf20 * 2).toInt().coerceAtLeast(5)
                )
            )
        }
        updatedSheet
    }

    suspend fun deleteExerciseSheet(sheetId: String) = withContext(Dispatchers.IO) {
        dao.deleteExerciseSheetById(sheetId)
    }

    private fun ExerciseSheetEntity.toDomainModel(): com.example.model.ExerciseSheet =
        com.example.model.ExerciseSheet(
            id = id,
            childId = childId,
            subject = subject,
            title = title,
            generalInstructions = generalInstructions,
            items = com.example.model.ExerciseJsonCodec.decodeItems(itemsJson),
            sourceFileNames = com.example.model.ExerciseJsonCodec.decodeStringList(sourceFileNamesJson),
            finalEvaluation = com.example.model.ExerciseJsonCodec.decodeFinalEvaluation(finalEvaluationJson),
            finalEvaluationError = finalEvaluationError,
            createdAt = createdAt,
            updatedAt = updatedAt
        )

    private fun com.example.model.ExerciseSheet.toEntity(): ExerciseSheetEntity =
        ExerciseSheetEntity(
            id = id,
            childId = childId,
            subject = subject,
            title = title,
            generalInstructions = generalInstructions,
            itemsJson = com.example.model.ExerciseJsonCodec.encodeItems(items),
            sourceFileNamesJson = com.example.model.ExerciseJsonCodec.encodeStringList(sourceFileNames),
            finalEvaluationJson = com.example.model.ExerciseJsonCodec.encodeFinalEvaluation(finalEvaluation),
            finalEvaluationError = finalEvaluationError,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
}
