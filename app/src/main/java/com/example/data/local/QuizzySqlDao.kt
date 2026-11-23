package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface QuizzySqlDao {

    // --- 3D Game Scores ---

    @Query("SELECT * FROM game_scores WHERE childId = :childId ORDER BY createdAt DESC LIMIT 30")
    fun observeGameScoresForChild(childId: String): Flow<List<GameScoreEntity>>

    @Query("SELECT * FROM game_scores ORDER BY score DESC, createdAt DESC LIMIT 20")
    fun observeAllTopGameScores(): Flow<List<GameScoreEntity>>

    @Query("SELECT COALESCE(MAX(score), 0) FROM game_scores WHERE childId = :childId")
    fun observeBestGameScore(childId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM game_scores WHERE childId = :childId")
    fun observeGameSessionsCount(childId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGameScore(score: GameScoreEntity): Long

    // --- Studio AI Images (Nano Banana 2.1) ---

    @Query("SELECT * FROM studio_images WHERE childId = :childId ORDER BY createdAt DESC LIMIT 40")
    fun observeStudioImagesForChild(childId: String): Flow<List<StudioImageEntity>>

    @Query("SELECT * FROM studio_images ORDER BY createdAt DESC LIMIT 40")
    fun observeAllStudioImages(): Flow<List<StudioImageEntity>>

    @Query("SELECT COUNT(*) FROM studio_images WHERE childId = :childId")
    fun observeStudioImagesCount(childId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudioImage(image: StudioImageEntity): Long

    @Query("DELETE FROM studio_images WHERE id = :id")
    suspend fun deleteStudioImageById(id: Long)

    // --- Study Telemetry & SQL Analytics ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudyLog(log: StudySqlLogEntity): Long

    @Query("SELECT COUNT(*) FROM study_sql_logs WHERE childId = :childId")
    fun observeStudyLogsCount(childId: String): Flow<Int>

    @Query(
        """
        SELECT subject AS subject,
               COUNT(*) AS sessionsCount,
               AVG(scorePercent) AS avgScore,
               MAX(scorePercent) AS bestScore
        FROM study_sql_logs
        WHERE childId = :childId AND scorePercent >= 0
        GROUP BY subject
        ORDER BY sessionsCount DESC, avgScore DESC
        """
    )
    fun observeSubjectStatsForChild(childId: String): Flow<List<SubjectSqlStats>>

    @Query(
        """
        SELECT subject AS subject,
               COUNT(*) AS sessionsCount,
               AVG(scorePercent) AS avgScore,
               MAX(scorePercent) AS bestScore
        FROM study_sql_logs
        WHERE scorePercent >= 0
        GROUP BY subject
        ORDER BY sessionsCount DESC, avgScore DESC
        """
    )
    fun observeAllSubjectStats(): Flow<List<SubjectSqlStats>>

    // --- Verbatim School Exercise Sheets ---

    @Query("SELECT * FROM exercise_sheets WHERE childId = :childId ORDER BY updatedAt DESC")
    fun observeExerciseSheetsForChild(childId: String): Flow<List<ExerciseSheetEntity>>

    @Query("SELECT * FROM exercise_sheets WHERE id = :id LIMIT 1")
    suspend fun getExerciseSheetById(id: String): ExerciseSheetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExerciseSheet(sheet: ExerciseSheetEntity)

    @Query("DELETE FROM exercise_sheets WHERE id = :id")
    suspend fun deleteExerciseSheetById(id: String)
}
