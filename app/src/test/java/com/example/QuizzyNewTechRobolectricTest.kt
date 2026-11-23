package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.GeminiTutorService
import com.example.data.local.ChildSqlOverview
import com.example.data.local.GameScoreEntity
import com.example.data.local.LocalSqlRepository
import com.example.data.local.QuizzySqlDatabase
import com.example.data.local.SubjectSqlStats
import com.example.model.ChildProfile
import com.example.model.Family
import com.example.model.Lesson
import com.example.model.PinHasher
import com.example.model.Quiz
import com.example.model.QuizImageOption
import com.example.model.QuizMatchPair
import com.example.model.QuizQuestion
import com.example.ui.screens.ChildSpaceScreen
import com.example.ui.screens.ImageStudioScreen
import com.example.ui.screens.ParentDashboardScreen
import com.example.ui.screens.Quizzy3DGameScreen
import com.example.ui.screens.SUBJECT_GENERAL_3D
import com.example.ui.screens.build3DGameQuestionsJson
import com.example.ui.screens.build3DMissionConfigJson
import com.example.ui.screens.build3DSubjectOptions
import com.example.ui.theme.EduFamilleTheme
import com.example.ui.viewmodel.ImageStudioUiState
import com.example.ui.viewmodel.OperationStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp-xxhdpi")
class QuizzyNewTechRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var database: QuizzySqlDatabase
    private lateinit var sqlRepo: LocalSqlRepository

    private val sampleFamily = Family(
        id = "fam_sql",
        ownerId = "uid_parent",
        familyName = "Famille Martin",
        inviteCode = "SQL42X",
        parentPinHash = PinHasher.hashPin("1234"),
        memberUids = listOf("uid_parent")
    )

    private val sampleChild = ChildProfile(
        id = "child_sql_1",
        familyId = "fam_sql",
        ownerId = "uid_parent",
        memberUids = listOf("uid_parent"),
        name = "Chloé",
        age = 10L,
        gradeLevel = "CM2",
        avatarKey = "fox",
        pinHash = PinHasher.hashPin("1111"),
        xp = 320L
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, QuizzySqlDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        sqlRepo = LocalSqlRepository(database.quizzySqlDao(), context)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun roomSqlRepository_recordsGameScores_studioImages_andAggregatesSqlStats() = runBlocking {
        // Insert 2 3D game sessions
        sqlRepo.recordGameSession(
            childId = sampleChild.id,
            childName = sampleChild.name,
            subject = "Mathématiques",
            gameMode = "ARENA_3D",
            score = 420,
            correctCount = 4,
            totalQuestions = 5,
            maxCombo = 3,
            durationSeconds = 48
        )
        sqlRepo.recordGameSession(
            childId = sampleChild.id,
            childName = sampleChild.name,
            subject = "Mathématiques",
            gameMode = "ARENA_3D",
            score = 650,
            correctCount = 5,
            totalQuestions = 5,
            maxCombo = 5,
            durationSeconds = 41
        )
        // Insert a standard quiz SQL study log
        sqlRepo.recordQuizStudyLog(
            childId = sampleChild.id,
            subject = "Français",
            scorePercent = 70,
            xpDelta = 45
        )
        // Save a Nano Banana Studio image
        val bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.CYAN)
        }
        val savedImg = sqlRepo.saveStudioImage(
            childId = sampleChild.id,
            prompt = "Le système solaire coloré",
            editInstruction = "",
            subject = "Sciences",
            styleLabel = "Illustration cahier",
            bitmap = bitmap,
            isEdited = false
        )

        val scores = sqlRepo.observeGameScoresForChild(sampleChild.id).first()
        assertEquals(2, scores.size)

        val overview = sqlRepo.observeChildSqlOverview(sampleChild.id).first()
        assertEquals(2, overview.totalGameSessions)
        assertEquals(650, overview.bestGameScore)
        assertEquals(1, overview.totalStudioImages)
        assertEquals(4, overview.totalStudyLogs)

        val subjectStats = sqlRepo.observeSubjectStatsForChild(sampleChild.id).first()
        val mathStats = subjectStats.firstOrNull { it.subject == "Mathématiques" }
        assertNotNull(mathStats)
        assertEquals(2, mathStats!!.sessionsCount)
        assertEquals(90.0, mathStats.avgScore, 0.1) // (80 + 100) / 2
        assertEquals(100, mathStats.bestScore)

        val frenchStats = subjectStats.firstOrNull { it.subject == "Français" }
        assertNotNull(frenchStats)
        assertEquals(1, frenchStats!!.sessionsCount)
        assertEquals(70.0, frenchStats.avgScore, 0.1)

        // Verify saved file exists, loads back, and deletes cleanly
        assertTrue(File(savedImg.imagePath).exists())
        assertNotNull(sqlRepo.loadStudioBitmap(savedImg.imagePath))

        sqlRepo.deleteStudioImage(savedImg)
        assertFalse(File(savedImg.imagePath).exists())
        assertNull(sqlRepo.loadStudioBitmap(savedImg.imagePath))
        assertEquals(0, sqlRepo.observeStudioImagesForChild(sampleChild.id).first().size)
    }

    @Test
    fun build3DGameQuestionsJson_transformsMcqAndImageChoiceTo3LanePayloads() {
        val quiz = Quiz(
            id = "q_3d",
            subject = "Sciences",
            title = "Système Solaire",
            questions = listOf(
                QuizQuestion(
                    questionType = QuizQuestion.TYPE_MCQ,
                    prompt = "Quelle est la planète rouge ?",
                    options = listOf("Vénus", "Mars", "Mercure", "Saturne"),
                    correctIndex = 1
                ),
                QuizQuestion(
                    questionType = QuizQuestion.TYPE_IMAGE_CHOICE,
                    prompt = "Clique sur l'astre au centre du système solaire :",
                    imageOptions = listOf(
                        QuizImageOption(label = "Le Soleil", visualKey = "sun"),
                        QuizImageOption(label = "La Lune", visualKey = "moon"),
                        QuizImageOption(label = "Une comète", visualKey = "star")
                    ),
                    correctIndex = 0
                ),
                // Fill-in-the-blank now also converts to a 3-lane gate using course distractors
                QuizQuestion(
                    questionType = QuizQuestion.TYPE_FILL_BLANK,
                    prompt = "Complète la phrase",
                    acceptedAnswers = listOf("Terre")
                )
            )
        )

        val jsonStr = build3DGameQuestionsJson(listOf(quiz))
        val array = JSONArray(jsonStr)
        assertEquals(3, array.length())

        val q0 = array.getJSONObject(0)
        assertEquals("Sciences", q0.getString("subject"))
        assertEquals(3, q0.getJSONArray("options").length())
        val correct0 = q0.getInt("correct")
        assertTrue(correct0 in 0..2)
        assertEquals("Mars", q0.getJSONArray("options").getString(correct0))

        val q1 = array.getJSONObject(1)
        val correct1 = q1.getInt("correct")
        assertTrue(correct1 in 0..2)
        assertEquals("Le Soleil", q1.getJSONArray("options").getString(correct1))

        val q2 = array.getJSONObject(2)
        val correct2 = q2.getInt("correct")
        assertTrue(correct2 in 0..2)
        assertEquals("Terre", q2.getJSONArray("options").getString(correct2))
    }

    @Test
    fun imageStudioScreen_createAndEditModes_workInDarkAndLightTheme() {
        var requestedPrompt: String? = null
        var requestedStyle: String? = null

        composeRule.setContent {
            EduFamilleTheme(darkTheme = true) {
                ImageStudioScreen(
                    child = sampleChild,
                    studioState = ImageStudioUiState.Idle,
                    savedImages = emptyList(),
                    onGenerateImage = { prompt, style, _ ->
                        requestedPrompt = prompt
                        requestedStyle = style
                    },
                    onEditImage = { _, _, _, _ -> },
                    onLoadSavedBitmap = { null },
                    onDeleteSavedImage = {},
                    onResetState = {},
                    onBack = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("image_studio_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("studio_generate_button").assertIsNotEnabled()

        composeRule.onNodeWithTag("studio_create_prompt_input")
            .performTextInput("Un schéma coloré du cycle de l'eau")
        composeRule.waitForIdle()

        composeRule.onNode(hasScrollAction())
            .performScrollToNode(hasTestTag("studio_generate_button"))
        composeRule.onNodeWithTag("studio_generate_button")
            .assertIsEnabled()
            .performClick()
        composeRule.waitForIdle()

        assertTrue(requestedPrompt!!.contains("Un schéma coloré du cycle de l'eau"))
        assertEquals("Illustration cahier", requestedStyle)

        // Switch to Edit mode and verify photo picker + edit button appear
        composeRule.onNode(hasScrollAction())
            .performScrollToNode(hasTestTag("studio_mode_edit"))
        composeRule.onNodeWithTag("studio_mode_edit").performClick()
        composeRule.waitForIdle()

        composeRule.onNode(hasScrollAction())
            .performScrollToNode(hasTestTag("studio_pick_photo_button"))
        composeRule.onNodeWithTag("studio_pick_photo_button").assertIsDisplayed()
        composeRule.onNode(hasScrollAction())
            .performScrollToNode(hasTestTag("studio_apply_edit_button"))
        composeRule.onNodeWithTag("studio_apply_edit_button").assertIsNotEnabled()
    }

    @Test
    fun imageStudioScreen_displaysRealErrorAndSuccessCards() {
        val sampleBmp = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.MAGENTA)
        }

        composeRule.setContent {
            EduFamilleTheme(darkTheme = false) {
                ImageStudioScreen(
                    child = sampleChild,
                    studioState = ImageStudioUiState.Success(
                        bitmap = sampleBmp,
                        prompt = "Volcan en éruption",
                        isEdited = false
                    ),
                    savedImages = emptyList(),
                    onGenerateImage = { _, _, _ -> },
                    onEditImage = { _, _, _, _ -> },
                    onLoadSavedBitmap = { null },
                    onDeleteSavedImage = {},
                    onResetState = {},
                    onBack = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNode(hasScrollAction())
            .performScrollToNode(hasTestTag("studio_result_card"))
        composeRule.onNodeWithTag("studio_result_card").assertIsDisplayed()
        composeRule.onNode(hasScrollAction())
            .performScrollToNode(hasText("base SQL locale", substring = true))
        composeRule.onNodeWithText("base SQL locale", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun imageStudioScreen_errorStateShowsExplicitFailureCard() {
        composeRule.setContent {
            EduFamilleTheme(darkTheme = true) {
                ImageStudioScreen(
                    child = sampleChild,
                    studioState = ImageStudioUiState.Error("Connexion interrompue lors de la création."),
                    savedImages = emptyList(),
                    onGenerateImage = { _, _, _ -> },
                    onEditImage = { _, _, _, _ -> },
                    onLoadSavedBitmap = { null },
                    onDeleteSavedImage = {},
                    onResetState = {},
                    onBack = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNode(hasScrollAction())
            .performScrollToNode(hasTestTag("studio_error_card"))
        composeRule.onNodeWithTag("studio_error_card").assertIsDisplayed()
        composeRule.onNodeWithText("Connexion interrompue", substring = true).assertIsDisplayed()
    }

    @Test
    fun childSpaceScreen_exposes3DGameAndImageStudioLaunchers() {
        composeRule.setContent {
            EduFamilleTheme(darkTheme = false) {
                ChildSpaceScreen(
                    family = sampleFamily,
                    child = sampleChild,
                    lessons = emptyList(),
                    quizzes = emptyList(),
                    opStatus = OperationStatus.Idle,
                    isTranscribing = false,
                    transcribeError = null,
                    onClearOpStatus = {},
                    onSwitchProfile = {},
                    onUpdateGradeLevel = { _, _ -> },
                    onDepositCourse = { _, _, _, _, _, _ -> },
                    onAddDocumentsToLesson = { _, _, _, _ -> },
                    onGenerateMoreQuiz = { _, _ -> },
                    onGenerateGapQuiz = {},
                    onSubmitQuizResult = { _, _, _, _, _, _, _, _, _ -> },
                    onTranscribeAudio = { _, _ -> },
                    childSqlOverview = ChildSqlOverview(
                        totalGameSessions = 3,
                        bestGameScore = 780,
                        totalStudioImages = 2,
                        totalStudyLogs = 8
                    )
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onAllNodes(hasScrollAction())[0]
            .performScrollToNode(hasTestTag("open_3d_game_card"))
        composeRule.onNodeWithTag("open_3d_game_card").assertIsDisplayed()
        composeRule.onNodeWithTag("open_image_studio_card").assertIsDisplayed()

        // Tap Image Studio card -> opens ImageStudioScreen cleanly
        composeRule.onNodeWithTag("open_image_studio_card").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("image_studio_screen").assertIsDisplayed()
    }

    @Test
    fun parentDashboardScreen_showsRoomSqlTelemetryCard() {
        val sampleScores = listOf(
            GameScoreEntity(
                id = 1L,
                childId = sampleChild.id,
                childName = sampleChild.name,
                subject = "Mathématiques",
                score = 840,
                correctCount = 5,
                totalQuestions = 5,
                maxCombo = 5,
                durationSeconds = 39
            )
        )
        val sampleStats = listOf(
            SubjectSqlStats(subject = "Mathématiques", sessionsCount = 3, avgScore = 88.0, bestScore = 100)
        )

        composeRule.setContent {
            EduFamilleTheme(darkTheme = true) {
                ParentDashboardScreen(
                    family = sampleFamily,
                    children = listOf(sampleChild),
                    lessons = emptyList(),
                    quizzes = emptyList(),
                    attempts = emptyList(),
                    opStatus = OperationStatus.Idle,
                    onSwitchProfile = {},
                    onAddChild = { _, _, _, _, _ -> },
                    onEditChild = { _, _, _, _, _, _ -> },
                    onDeleteChild = {},
                    sqlGameScores = sampleScores,
                    sqlStudioImagesCount = 4,
                    sqlSubjectStats = sampleStats
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNode(hasScrollAction())
            .performScrollToNode(hasTestTag("parent_sql_telemetry_card"))
        composeRule.onNodeWithTag("parent_sql_telemetry_card").assertIsDisplayed()
        composeRule.onNodeWithText("840").assertIsDisplayed()
        composeRule.onNodeWithText("4").assertIsDisplayed()
        composeRule.onNodeWithText("Moyennes SQL : Mathématiques 88%").assertIsDisplayed()
    }

    @Test
    fun imageGeneration_persistentRetryRotatesModelsAndSucceedsAfterQuotaBusyErrors() = runBlocking {
        val service = GeminiTutorService()
        val expectedBitmap = Bitmap.createBitmap(12, 12, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.GREEN)
        }
        val statusLog = mutableListOf<String>()
        var primaryCalls = 0
        var liteCalls = 0
        var proCalls = 0

        val result = service.executeImageGenerationWithPersistentRetry(
            maxRounds = 3,
            roundDelaysMs = listOf(10L, 10L, 10L),
            modelSwitchDelayMs = 5L,
            perAttemptTimeoutMs = 5_000L,
            modelInvokers = listOf(
                "Nano Banana 2.1" to {
                    primaryCalls++
                    throw IllegalStateException("429 RESOURCE_EXHAUSTED high demand")
                },
                "Nano Banana Lite" to {
                    liteCalls++
                    if (liteCalls < 2) {
                        throw IllegalStateException("503 Service Unavailable overloaded")
                    }
                    expectedBitmap
                },
                "Nano Banana Pro" to {
                    proCalls++
                    throw IllegalStateException("429 rate limit exceeded")
                }
            ),
            onStatusUpdate = { statusLog.add(it) }
        )

        assertTrue(result.isSuccess)
        assertEquals(expectedBitmap, result.getOrNull())
        assertEquals(2, primaryCalls)
        assertEquals(2, liteCalls)
        assertEquals(1, proCalls)
        assertTrue(statusLog.any { it.contains("Nano Banana Lite") })
        assertTrue(statusLog.any { it.contains("File d'attente IA") })
    }

    @Test
    fun build3DGameQuestions_filtersStrictlyByCourseSubject_andIncludesAllInGeneralMode() {
        val englishQuiz = Quiz(
            id = "q_en",
            subject = "Anglais",
            title = "Maison en anglais",
            questions = listOf(
                QuizQuestion(
                    questionType = QuizQuestion.TYPE_MCQ,
                    prompt = "Comment dit-on canapé en anglais ?",
                    options = listOf("sofa", "chair", "table", "bed"),
                    correctIndex = 0
                ),
                QuizQuestion(
                    questionType = QuizQuestion.TYPE_DRAG_MATCH,
                    prompt = "Associe les mots anglais",
                    matchingPairs = listOf(
                        QuizMatchPair("lit", "bed"),
                        QuizMatchPair("chaise", "chair"),
                        QuizMatchPair("maison", "house")
                    )
                )
            )
        )
        val mathQuiz = Quiz(
            id = "q_math",
            subject = "Mathématiques",
            title = "Tables",
            questions = listOf(
                QuizQuestion(
                    questionType = QuizQuestion.TYPE_MCQ,
                    prompt = "Combien font 6 × 7 ?",
                    options = listOf("36", "42", "48"),
                    correctIndex = 1
                )
            )
        )
        val historyLessonOnly = Lesson(
            id = "les_hist",
            subject = "Histoire-Géo",
            title = "La Révolution française",
            keyPoints = listOf("14 juillet 1789 : prise de la Bastille")
        )

        val options = build3DSubjectOptions(
            quizzes = listOf(englishQuiz, mathQuiz),
            lessons = listOf(historyLessonOnly)
        )
        assertEquals(SUBJECT_GENERAL_3D, options.first().subject)
        assertEquals(4, options.first().questionCount)
        assertTrue(options.any { it.subject == "Anglais" && it.questionCount == 2 })
        assertTrue(options.any { it.subject == "Mathématiques" && it.questionCount == 1 })
        assertTrue(options.any { it.subject == "Histoire-Géo" && it.questionCount == 1 })

        // Filter strictly by Anglais -> ONLY Anglais course questions
        val englishOnlyJson = JSONArray(
            build3DGameQuestionsJson(
                quizzes = listOf(englishQuiz, mathQuiz),
                lessons = listOf(historyLessonOnly),
                selectedSubject = "Anglais"
            )
        )
        assertEquals(2, englishOnlyJson.length())
        for (i in 0 until englishOnlyJson.length()) {
            assertEquals("Anglais", englishOnlyJson.getJSONObject(i).getString("subject"))
        }

        // General mode -> combines ALL courses (Anglais + Mathématiques + Histoire-Géo)
        val missionCfg = JSONObject(
            build3DMissionConfigJson(
                quizzes = listOf(englishQuiz, mathQuiz),
                lessons = listOf(historyLessonOnly),
                selectedSubject = SUBJECT_GENERAL_3D,
                difficulty = "Difficile",
                autoStart = true,
                openExplorer = false
            )
        )
        assertEquals(SUBJECT_GENERAL_3D, missionCfg.getString("subject"))
        assertEquals("Difficile", missionCfg.getString("difficulty"))
        assertEquals(4, missionCfg.getJSONArray("questions").length())
    }

    @Test
    fun quizzy3DGameScreen_setupPanelAllowsSelectingDifficultyAndCourseSubject() {
        val englishQuiz = Quiz(
            id = "q_en_ui",
            subject = "Anglais",
            title = "Vocabulaire",
            questions = listOf(
                QuizQuestion(
                    questionType = QuizQuestion.TYPE_MCQ,
                    prompt = "Comment dit-on chien en anglais ?",
                    options = listOf("dog", "cat", "bird"),
                    correctIndex = 0
                )
            )
        )
        val mathQuiz = Quiz(
            id = "q_math_ui",
            subject = "Mathématiques",
            title = "Fractions",
            questions = listOf(
                QuizQuestion(
                    questionType = QuizQuestion.TYPE_MCQ,
                    prompt = "La moitié de 10 ?",
                    options = listOf("4", "5", "6"),
                    correctIndex = 1
                )
            )
        )

        composeRule.setContent {
            EduFamilleTheme(darkTheme = false) {
                Quizzy3DGameScreen(
                    child = sampleChild,
                    quizzes = listOf(englishQuiz, mathQuiz),
                    recentScores = emptyList(),
                    subjectSqlStats = emptyList(),
                    onRecordGameFinished = { _, _, _, _, _, _ -> },
                    onBack = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("game_3d_setup_panel").assertIsDisplayed()
        composeRule.onNodeWithTag("game_3d_difficulty_facile").assertIsDisplayed()
        composeRule.onNodeWithTag("game_3d_difficulty_moyen").performClick()
        composeRule.onNodeWithTag("game_3d_subject_Anglais").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Lancer la mission (Anglais • Moyen)").assertIsDisplayed()
    }

    @Test
    fun imageStudioScreen_loadingShowsLiveRetryMessageAndCancelButton() {
        var cancelled = false
        composeRule.setContent {
            EduFamilleTheme(darkTheme = false) {
                ImageStudioScreen(
                    child = sampleChild,
                    studioState = ImageStudioUiState.Loading("File d'attente IA en cours — nouvelle tentative dans 6s..."),
                    savedImages = emptyList(),
                    onGenerateImage = { _, _, _ -> },
                    onEditImage = { _, _, _, _ -> },
                    onLoadSavedBitmap = { null },
                    onDeleteSavedImage = {},
                    onResetState = { cancelled = true },
                    onBack = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNode(hasScrollAction())
            .performScrollToNode(hasTestTag("studio_cancel_button"))
        composeRule.onNodeWithTag("studio_cancel_button").performClick()
        composeRule.waitForIdle()

        assertTrue(cancelled)
    }
}
