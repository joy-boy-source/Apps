package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.GeminiTutorService
import com.example.data.local.LocalSqlRepository
import com.example.data.local.QuizzySqlDatabase
import com.example.model.ChildProfile
import com.example.model.ExerciseFinalEvaluation
import com.example.model.ExerciseItem
import com.example.model.ExerciseJsonCodec
import com.example.model.ExerciseSheet
import com.example.model.Family
import com.example.model.LanguageToneCatalog
import com.example.model.Quiz
import com.example.model.QuizFeedbackHelper
import com.example.model.QuizQuestion
import com.example.ui.screens.ChildSpaceScreen
import com.example.ui.screens.DepositExerciseDialog
import com.example.ui.screens.ExercisePlayerScreen
import com.example.ui.screens.ParentDashboardScreen
import com.example.ui.screens.QuizPlayerScreen
import com.example.ui.theme.EduFamilleTheme
import com.example.ui.viewmodel.OperationStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class QuizzyExerciseRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val sampleFamily = Family(
        id = "fam_ex",
        familyName = "Famille Martin"
    )

    private val sampleChild = ChildProfile(
        id = "child_ex",
        familyId = "fam_ex",
        name = "Léo",
        age = 11,
        gradeLevel = "6ème",
        avatarKey = "rocket",
        xp = 240
    )

    private fun sampleSheet(): ExerciseSheet = ExerciseSheet(
        id = "sheet_1",
        childId = sampleChild.id,
        subject = "Mathématiques",
        title = "Fiche n°4 : Fractions et décimaux",
        generalInstructions = "Calcule chaque expression sans calculatrice.",
        sourceFileNames = listOf("devoir_fractions.jpg"),
        items = listOf(
            ExerciseItem(
                id = "ex_1",
                numberLabel = "1.",
                prompt = "Écris 3/4 sous forme décimale.",
                inputType = ExerciseItem.TYPE_TEXT_INPUT,
                expectedAnswerGuide = "0,75"
            ),
            ExerciseItem(
                id = "ex_2",
                numberLabel = "2.",
                prompt = "Choisis la fraction équivalente à 1/2.",
                inputType = ExerciseItem.TYPE_MCQ,
                options = listOf("2/4", "2/3", "3/5"),
                expectedAnswerGuide = "2/4",
                studentAnswer = "2/4",
                verdict = ExerciseItem.VERDICT_CORRECT,
                aiExplanation = "Bravo ! 2/4 se simplifie bien en 1/2."
            ),
            ExerciseItem(
                id = "ex_3",
                numberLabel = "3.",
                prompt = "Calcule 1/5 + 2/5.",
                inputType = ExerciseItem.TYPE_TEXT_INPUT,
                expectedAnswerGuide = "3/5",
                studentAnswer = "3/10",
                verdict = ExerciseItem.VERDICT_INCORRECT,
                aiExplanation = "Attention : on additionne uniquement les numérateurs quand le dénominateur est identique."
            )
        )
    )

    @Test
    fun exerciseJsonCodecRoundTripPreservesAllFields() {
        val original = sampleSheet().items + ExerciseItem(
            id = "ex_4",
            numberLabel = "4.",
            prompt = "Donne un exemple de fraction égale à 1.",
            inputType = ExerciseItem.TYPE_TEXT_INPUT,
            studentAnswer = "4/4",
            verdict = ExerciseItem.VERDICT_PARTIAL,
            aiExplanation = "Presque complet !",
            gradingError = "Délai dépassé"
        )
        val encoded = ExerciseJsonCodec.encodeItems(original)
        val decoded = ExerciseJsonCodec.decodeItems(encoded)

        assertEquals(original.size, decoded.size)
        assertEquals(original[0].prompt, decoded[0].prompt)
        assertEquals(ExerciseItem.TYPE_MCQ, decoded[1].inputType)
        assertEquals(listOf("2/4", "2/3", "3/5"), decoded[1].options)
        assertEquals(ExerciseItem.VERDICT_CORRECT, decoded[1].verdict)
        assertEquals("Délai dépassé", decoded[3].gradingError)

        val encodedNames = ExerciseJsonCodec.encodeStringList(listOf("photo1.jpg", "fiche.pdf"))
        assertEquals(listOf("photo1.jpg", "fiche.pdf"), ExerciseJsonCodec.decodeStringList(encodedNames))
    }

    @Test
    fun geminiExerciseParsersHandleExtractionAndGradingVerdicts() {
        val extractionJson = """
            ```json
            {
              "detectedSubject": "Français",
              "title": "Exercice 3 : Accord du participe passé",
              "generalInstructions": "Complète les phrases suivantes.",
              "items": [
                {
                  "numberLabel": "a)",
                  "prompt": "Les fleurs que j'ai (cueillir) ___ sont magnifiques.",
                  "inputType": "TEXT_INPUT",
                  "options": [],
                  "expectedAnswerGuide": "cueillies"
                },
                {
                  "numberLabel": "b)",
                  "prompt": "Choisis la bonne orthographe.",
                  "inputType": "MCQ",
                  "options": ["Elles sont parties", "Elles sont parti"],
                  "expectedAnswerGuide": "Elles sont parties"
                }
              ]
            }
            ```
        """.trimIndent()

        val extracted = GeminiTutorService.parseExtractedExerciseJson(
            rawJson = extractionJson,
            requestedSubject = "Automatique"
        )
        assertEquals("Français", extracted.detectedSubject)
        assertEquals("Exercice 3 : Accord du participe passé", extracted.title)
        assertEquals(2, extracted.items.size)
        assertEquals("a)", extracted.items[0].numberLabel)
        assertEquals(ExerciseItem.TYPE_TEXT_INPUT, extracted.items[0].inputType)
        assertEquals(ExerciseItem.TYPE_MCQ, extracted.items[1].inputType)

        val gradeCorrect = GeminiTutorService.parseExerciseGradeJson(
            """{"verdict": "CORRECT", "explanation": "Parfait, le COD est placé avant."}"""
        )
        assertEquals(ExerciseItem.VERDICT_CORRECT, gradeCorrect.verdict)

        val gradePartial = GeminiTutorService.parseExerciseGradeJson(
            """{"verdict": "PARTIELLEMENT_CORRECTE", "explanation": "Il manque juste le s du pluriel."}"""
        )
        assertEquals(ExerciseItem.VERDICT_PARTIAL, gradePartial.verdict)

        val gradeIncorrect = GeminiTutorService.parseExerciseGradeJson(
            """{"verdict": "FAUSSE", "explanation": "Relis la règle de l'auxiliaire être."}"""
        )
        assertEquals(ExerciseItem.VERDICT_INCORRECT, gradeIncorrect.verdict)
    }

    @Test
    fun roomSqlRepositoryPersistsUpdatesAndDeletesExerciseSheets() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, QuizzySqlDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = LocalSqlRepository(db.quizzySqlDao(), context)
            val sheet = sampleSheet()

            repo.saveExerciseSheet(sheet)
            val observed = repo.observeExerciseSheetsForChild(sampleChild.id).first()
            assertEquals(1, observed.size)
            assertEquals("Fiche n°4 : Fractions et décimaux", observed[0].title)
            assertEquals(3, observed[0].items.size)

            val updated = repo.updateExerciseItem(sheet.id, "ex_1") { current ->
                current.copy(
                    studentAnswer = "0,75",
                    verdict = ExerciseItem.VERDICT_CORRECT,
                    aiExplanation = "Exactement !"
                )
            }
            assertEquals(ExerciseItem.VERDICT_CORRECT, updated?.items?.first { it.id == "ex_1" }?.verdict)

            val afterUpdate = repo.observeExerciseSheetsForChild(sampleChild.id).first()
            assertEquals(2, afterUpdate[0].correctCount)

            repo.deleteExerciseSheet(sheet.id)
            val afterDelete = repo.observeExerciseSheetsForChild(sampleChild.id).first()
            assertTrue(afterDelete.isEmpty())
            assertNull(repo.updateExerciseItem(sheet.id, "ex_1") { it })
        } finally {
            db.close()
        }
    }

    @Test
    fun childSpaceExercicesTabListsSheetsOpensPlayerAndValidatesAnswers() {
        val sheet = sampleSheet()
        var validatedPair: Pair<String, String>? = null
        var deletedId: String? = null

        composeRule.setContent {
            EduFamilleTheme {
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
                    exerciseSheets = listOf(sheet),
                    onGradeExerciseItem = { _, item, answer ->
                        validatedPair = item.id to answer
                    },
                    onDeleteExerciseSheet = { deletedId = it }
                )
            }
        }

        // Switch to the 3rd tab "Exercices"
        composeRule.onNodeWithTag("nav_child_exercises").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("exercise_hero_card").assertIsDisplayed()
        composeRule.onNodeWithTag("deposit_exercise_fab").assertIsDisplayed()
        composeRule.onNodeWithTag("exercise_sheet_card_sheet_1").performScrollTo().assertIsDisplayed()

        // Open exercise sheet
        composeRule.onNodeWithTag("exercise_sheet_card_sheet_1").performClick()
        composeRule.waitForIdle()

        // Verify ExercisePlayerScreen renders items and verdict cards
        composeRule.onNodeWithTag("exercise_player_list").assertIsDisplayed()
        composeRule.onNodeWithTag("exercise_text_input_ex_1").performScrollTo().performTextInput("0,75")
        composeRule.onNodeWithTag("exercise_validate_ex_1").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals("ex_1" to "0,75", validatedPair)

        // Return to list and verify delete callback
        composeRule.onNodeWithTag("exercise_player_list").performScrollToIndex(0)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("exercise_player_back_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("child_exercises_tab_list")
            .performScrollToNode(hasTestTag("exercise_sheet_card_sheet_1"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("exercise_delete_sheet_1").performClick()
        composeRule.waitForIdle()
        assertEquals("sheet_1", deletedId)
    }

    @Test
    fun depositExerciseDialogSubmitsNotesAndHandlesRetryOnError() {
        var submittedNotes = ""

        composeRule.setContent {
            EduFamilleTheme {
                DepositExerciseDialog(
                    child = sampleChild,
                    opStatus = OperationStatus.Error("Réseau indisponible"),
                    isTranscribing = false,
                    transcribeError = null,
                    onTranscribeAudio = { _, _ -> },
                    onDismiss = {},
                    onSubmit = { _, _, _, notes ->
                        submittedNotes = notes
                    }
                )
            }
        }

        composeRule.onNodeWithTag("exercise_deposit_error_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("exercise_notes_input").performScrollTo()
            .performTextInput("1. Calculer 12 x 4\n2. Calculer 15 x 3")
        composeRule.onNodeWithTag("exercise_deposit_retry_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertTrue(submittedNotes.contains("12 x 4"))
    }

    @Test
    fun exercisePlayerRendersMcqAndRetryButtonOnGradingError() {
        val errorSheet = ExerciseSheet(
            id = "sheet_err",
            childId = sampleChild.id,
            subject = "Anglais",
            title = "Grammar Ex. 2",
            items = listOf(
                ExerciseItem(
                    id = "err_1",
                    numberLabel = "1)",
                    prompt = "Choose the right verb: She ___ to school.",
                    inputType = ExerciseItem.TYPE_MCQ,
                    options = listOf("go", "goes"),
                    studentAnswer = "goes",
                    gradingError = "La vérification IA a expiré."
                )
            )
        )
        var retriedAnswer: String? = null

        composeRule.setContent {
            EduFamilleTheme(darkTheme = true) {
                ExercisePlayerScreen(
                    sheet = errorSheet,
                    gradingItemKeys = emptySet(),
                    onBack = {},
                    onValidateItemAnswer = { _, _, ans -> retriedAnswer = ans }
                )
            }
        }

        composeRule.onNodeWithTag("exercise_mcq_err_1_1").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("exercise_error_card_err_1").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("exercise_retry_grade_err_1").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals("goes", retriedAnswer)
    }

    @Test
    fun finalEvaluationModelCodecAndGeminiParserWorkWithAndWithoutImprovementAxes() {
        val evalWithAxes = GeminiTutorService.parseExerciseFinalEvaluationJson(
            """
            {
              "scoreOutOf20": 13.7,
              "overallAssessment": "Travail sérieux sur les fractions, avec une erreur d'addition de dénominateurs à corriger.",
              "strengths": ["Simplification de 2/4 maîtrisée"],
              "improvementAxes": ["Additionner uniquement les numérateurs quand le dénominateur est commun"],
              "practicalAdvice": "Vérifie toujours le dénominateur avant d'additionner.",
              "encouragement": "Continue ainsi, tu progresses bien !"
            }
            """.trimIndent()
        )
        // Clamped to nearest 0.5 -> 13.5
        assertEquals(13.5, evalWithAxes.scoreOutOf20, 0.001)
        assertEquals("13,5/20", evalWithAxes.formattedScoreOutOf20)
        assertEquals(68, evalWithAxes.percentage)
        assertEquals(1, evalWithAxes.improvementAxes.size)

        // Round-trip through ExerciseJsonCodec
        val encoded = ExerciseJsonCodec.encodeFinalEvaluation(evalWithAxes)
        val decoded = ExerciseJsonCodec.decodeFinalEvaluation(encoded)
        assertNotNull(decoded)
        assertEquals(13.5, decoded!!.scoreOutOf20, 0.001)
        assertEquals(evalWithAxes.improvementAxes, decoded.improvementAxes)
        assertEquals(evalWithAxes.practicalAdvice, decoded.practicalAdvice)

        // Perfect score: improvementAxes empty (only included when necessary)
        val evalPerfect = GeminiTutorService.parseExerciseFinalEvaluationJson(
            """
            {
              "scoreOutOf20": 20.0,
              "overallAssessment": "Toutes les réponses sont exactes et bien rédigées.",
              "strengths": ["Raisonnement rigoureux", "Calcul exact"],
              "improvementAxes": [],
              "practicalAdvice": "Garde cette méthode de vérification systématique.",
              "encouragement": "Bravo pour ce 20/20 !"
            }
            """.trimIndent()
        )
        assertEquals("20/20", evalPerfect.formattedScoreOutOf20)
        assertTrue(evalPerfect.improvementAxes.isEmpty())
    }

    @Test
    fun roomSqlRepositoryPersistsExerciseDraftsAndFinalEvaluation() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, QuizzySqlDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = LocalSqlRepository(db.quizzySqlDao(), context)
            val sheet = sampleSheet()
            repo.saveExerciseSheet(sheet)

            repo.updateExerciseSheetDrafts(sheet.id, mapOf("ex_1" to "0,75"))
            val eval = ExerciseFinalEvaluation(
                scoreOutOf20 = 16.5,
                overallAssessment = "Très bon ensemble.",
                strengths = listOf("Conversions décimales justes"),
                improvementAxes = listOf("Revoir la somme de fractions"),
                practicalAdvice = "Relis le dénominateur commun.",
                encouragement = "Bravo Léo !"
            )
            val updated = repo.updateExerciseSheetEvaluation(sheet.id, eval, null)
            assertNotNull(updated?.finalEvaluation)
            assertEquals("16,5/20", updated?.finalEvaluation?.formattedScoreOutOf20)
            assertEquals("0,75", updated?.items?.first { it.id == "ex_1" }?.studentAnswer)

            val stats = repo.observeSubjectStatsForChild(sampleChild.id).first()
            assertTrue(stats.any { it.subject == "Mathématiques" })
        } finally {
            db.close()
        }
    }

    @Test
    fun exercisePlayerFinalizeButtonSubmitsDraftsAndHidesImprovementAxesWhenPerfect() {
        val perfectSheet = sampleSheet().copy(
            finalEvaluation = ExerciseFinalEvaluation(
                scoreOutOf20 = 20.0,
                overallAssessment = "Parfait sur l'ensemble de l'exercice.",
                strengths = listOf("Toutes les réponses sont exactes"),
                improvementAxes = emptyList(),
                practicalAdvice = "Conserve cette rigueur pour les prochains exercices.",
                encouragement = "Excellent travail Léo !"
            )
        )
        var capturedDrafts: Map<String, String>? = null

        composeRule.setContent {
            EduFamilleTheme {
                ExercisePlayerScreen(
                    sheet = perfectSheet,
                    gradingItemKeys = emptySet(),
                    onBack = {},
                    onValidateItemAnswer = { _, _, _ -> },
                    onFinalizeExercise = { _, drafts -> capturedDrafts = drafts }
                )
            }
        }

        composeRule.onNodeWithTag("exercise_text_input_ex_1").performScrollTo().performTextInput("0,75")
        composeRule.onNodeWithTag("exercise_player_list")
            .performScrollToNode(hasTestTag("exercise_finalize_button"))
        composeRule.onNodeWithTag("exercise_finalize_button").assertIsEnabled().performClick()
        composeRule.waitForIdle()

        assertEquals("0,75", capturedDrafts?.get("ex_1"))

        composeRule.onNodeWithTag("exercise_player_list")
            .performScrollToNode(hasTestTag("exercise_final_evaluation_card"))
        composeRule.onNodeWithTag("exercise_final_score_badge").assertIsDisplayed()
        composeRule.onNodeWithText("20/20").assertIsDisplayed()
        composeRule.onNodeWithTag("exercise_strengths_section").assertIsDisplayed()
        composeRule.onNodeWithTag("exercise_practical_advice_section").assertIsDisplayed()
        composeRule.onNodeWithTag("exercise_encouragement_banner").assertIsDisplayed()
        // Improvement axes section must NOT be rendered when empty
        assertTrue(
            composeRule.onAllNodesWithTag("exercise_improvement_axes_section")
                .fetchSemanticsNodes().isEmpty()
        )
    }

    @Test
    fun languageToneCatalogAdaptsToAgeByDefaultAndRespectsParentOverride() {
        val youngAuto = LanguageToneCatalog.buildAiToneInstruction(6, "CP", LanguageToneCatalog.AUTO)
        assertTrue(youngAuto.contains("CP/CE1"))

        val teenAuto = LanguageToneCatalog.buildAiToneInstruction(16, "1ère", LanguageToneCatalog.AUTO)
        assertTrue(teenAuto.contains("lycée"))

        val matureOverride = LanguageToneCatalog.buildAiToneInstruction(9, "CM1", LanguageToneCatalog.MATURE)
        assertTrue(matureOverride.contains("MATURE"))
        assertTrue(LanguageToneCatalog.isMatureRegister(9, LanguageToneCatalog.MATURE))
        assertFalse(LanguageToneCatalog.isMatureRegister(15, LanguageToneCatalog.SIMPLIFIED))

        val childMsg = QuizFeedbackHelper.friendlyWrongAnswerMessage(0, childAge = 8, languageToneLevel = LanguageToneCatalog.AUTO)
        val matureMsg = QuizFeedbackHelper.friendlyWrongAnswerMessage(0, childAge = 8, languageToneLevel = LanguageToneCatalog.MATURE)
        assertTrue(childMsg != matureMsg)
    }

    @Test
    fun parentDashboardAllowsChangingChildLanguageToneLevel() {
        var updatedTone: Pair<String, String>? = null

        composeRule.setContent {
            EduFamilleTheme {
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
                    onUpdateChildLanguageTone = { cid, tone -> updatedTone = cid to tone }
                )
            }
        }

        composeRule.onNode(hasScrollAction())
            .performScrollToNode(hasTestTag("parent_language_tone_card"))
        composeRule.onNodeWithTag("parent_tone_chip_MATURE").performClick()
        composeRule.waitForIdle()

        assertEquals(sampleChild.id to LanguageToneCatalog.MATURE, updatedTone)
    }

    @Test
    fun quizPlayerSupportsMultiSelectMcqCheckboxesAndValidation() {
        val multiQuiz = Quiz(
            id = "quiz_multi_1",
            subject = "Mathématiques",
            title = "Nombres pairs",
            difficulty = "Moyen",
            questions = listOf(
                QuizQuestion(
                    prompt = "Quels nombres sont pairs ? (plusieurs réponses)",
                    questionType = QuizQuestion.TYPE_MCQ,
                    options = listOf("4", "7", "12", "9"),
                    correctIndex = 0,
                    correctIndices = listOf(0, 2),
                    allowMultipleSelection = true,
                    guidingHint = "Un nombre pair est divisible par 2.",
                    explanation = "4 et 12 sont divisibles par 2."
                )
            )
        )
        var finalScore = -1

        composeRule.setContent {
            EduFamilleTheme {
                QuizPlayerScreen(
                    quiz = multiQuiz,
                    childAge = 11,
                    childLanguageToneLevel = LanguageToneCatalog.MATURE,
                    onBack = {},
                    onQuizCompleted = { score, _, _, _, _ -> finalScore = score }
                )
            }
        }

        composeRule.onNodeWithTag("quiz_multi_select_badge").assertIsDisplayed()
        // First attempt: pick only option 0 (incomplete set) -> wrong attempt, options stay toggleable
        composeRule.onNodeWithTag("quiz_option_0").performScrollTo().performClick()
        composeRule.onNodeWithTag("quiz_validate_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("quiz_guiding_hint_card").performScrollTo().assertIsDisplayed()

        // Second attempt: add option 2 so {0, 2} is selected -> solved!
        composeRule.onNodeWithTag("quiz_option_2").performScrollTo().performClick()
        composeRule.onNodeWithTag("quiz_validate_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("quiz_correct_explanation_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("quiz_next_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        // 1st attempt missed, so 1st-try score is 0
        assertEquals(0, finalScore)
    }

    @Test
    fun exercisePlayerSupportsMultiSelectMcqAutoDetectionAndManualToggle() {
        val multiSheet = ExerciseSheet(
            id = "sheet_multi",
            childId = sampleChild.id,
            subject = "Français",
            title = "Les verbes du 1er groupe",
            items = listOf(
                ExerciseItem(
                    id = "m_1",
                    numberLabel = "1.",
                    prompt = "Coche toutes les bonnes réponses : quels verbes sont du 1er groupe ?",
                    inputType = ExerciseItem.TYPE_MCQ,
                    options = listOf("Chanter", "Finir", "Manger"),
                    allowMultipleAnswers = true
                ),
                ExerciseItem(
                    id = "m_2",
                    numberLabel = "2.",
                    prompt = "Choisis le ou les mots féminins.",
                    inputType = ExerciseItem.TYPE_MCQ,
                    options = listOf("Table", "Cahier", "Chaise"),
                    allowMultipleAnswers = false
                )
            )
        )
        val submitted = mutableMapOf<String, String>()

        composeRule.setContent {
            EduFamilleTheme {
                ExercisePlayerScreen(
                    sheet = multiSheet,
                    gradingItemKeys = emptySet(),
                    onBack = {},
                    onValidateItemAnswer = { _, item, answer -> submitted[item.id] = answer }
                )
            }
        }

        // Item 1: already in multi-select mode -> select options 0 & 2
        composeRule.onNodeWithTag("exercise_mcq_multi_hint_m_1").assertIsDisplayed()
        composeRule.onNodeWithTag("exercise_mcq_m_1_0").performScrollTo().performClick()
        composeRule.onNodeWithTag("exercise_mcq_m_1_2").performScrollTo().performClick()
        composeRule.onNodeWithTag("exercise_validate_m_1").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals("Chanter ; Manger", submitted["m_1"])

        // Item 2: manually enable multi-select chip and pick 2 options
        composeRule.onNodeWithTag("exercise_player_list")
            .performScrollToNode(hasTestTag("exercise_item_card_m_2"))
        composeRule.onNodeWithTag("exercise_mcq_multi_toggle_m_2").performClick()
        composeRule.onNodeWithTag("exercise_mcq_m_2_0").performClick()
        composeRule.onNodeWithTag("exercise_mcq_m_2_2").performClick()
        composeRule.onNodeWithTag("exercise_validate_m_2").performClick()
        composeRule.waitForIdle()
        assertEquals("Table ; Chaise", submitted["m_2"])
    }
}
