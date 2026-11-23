package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "game_scores",
    indices = [Index(value = ["childId", "createdAt"])]
)
data class GameScoreEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val childId: String,
    val childName: String,
    val subject: String,
    val gameMode: String = "ARENA_3D",
    val score: Int,
    val correctCount: Int,
    val totalQuestions: Int,
    val maxCombo: Int,
    val durationSeconds: Int,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "studio_images",
    indices = [Index(value = ["childId", "createdAt"])]
)
data class StudioImageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val childId: String,
    val prompt: String,
    val editInstruction: String = "",
    val subject: String = "Général",
    val styleLabel: String = "Illustration",
    val imagePath: String,
    val isEdited: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "study_sql_logs",
    indices = [Index(value = ["childId", "subject", "createdAt"])]
)
data class StudySqlLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val childId: String,
    val subject: String,
    val activityType: String, // "QUIZ", "GAME_3D", "STUDIO_IMAGE"
    val scorePercent: Int,
    val xpDelta: Int,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "exercise_sheets",
    indices = [Index(value = ["childId", "updatedAt"])]
)
data class ExerciseSheetEntity(
    @PrimaryKey val id: String,
    val childId: String,
    val subject: String,
    val title: String,
    val generalInstructions: String = "",
    val itemsJson: String = "[]",
    val sourceFileNamesJson: String = "[]",
    val finalEvaluationJson: String? = null,
    val finalEvaluationError: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class SubjectSqlStats(
    val subject: String,
    val sessionsCount: Int,
    val avgScore: Double,
    val bestScore: Int
)

data class ChildSqlOverview(
    val totalGameSessions: Int = 0,
    val bestGameScore: Int = 0,
    val totalStudioImages: Int = 0,
    val totalStudyLogs: Int = 0
)
