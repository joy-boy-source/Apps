package com.example

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.model.ChildProfile
import com.example.model.Family
import com.example.model.Lesson
import com.example.model.MissedQuestionRecord
import com.example.model.PinHasher
import com.example.model.Quiz
import com.example.model.QuizAttempt
import com.example.model.QuizQuestion
import com.example.model.SchoolCatalog
import com.example.model.UserAccount
import com.example.notifications.QuizNotificationHelper
import com.example.ui.components.QuizzyBookLoader
import com.example.ui.components.QuizzyInlineLoader
import com.example.ui.components.QuizzyWordmarkLogo
import com.example.ui.screens.ChildSpaceScreen
import com.example.ui.screens.FamilySetupScreen
import com.example.ui.screens.LessonDetailScreen
import com.example.ui.screens.ParentDashboardScreen
import com.example.ui.screens.ProfilePortalScreen
import com.example.ui.screens.QuizPlayerScreen
import androidx.compose.ui.graphics.luminance
import com.example.ui.theme.EduFamilleTheme
import com.example.ui.theme.QuizzyExtendedColors
import com.example.ui.theme.QuizzyTheme
import com.example.ui.viewmodel.OperationStatus
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class QuizzyUiFlowsRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val sampleFamily = Family(
        id = "fam1",
        ownerId = "uid_parent",
        familyName = "Famille Dupont",
        inviteCode = "AB12CD",
        parentPinHash = PinHasher.hashPin("1234"),
        memberUids = listOf("uid_parent")
    )

    private val sampleChild = ChildProfile(
        id = "child1",
        familyId = "fam1",
        ownerId = "uid_parent",
        memberUids = listOf("uid_parent"),
        name = "Léo",
        age = 11L,
        gradeLevel = "6ème",
        avatarKey = "owl",
        pinHash = PinHasher.hashPin("0000"),
        lessonsCount = 1L,
        quizzesTaken = 1L,
        totalCorrect = 4L,
        totalQuestions = 5L,
        xp = 80L,
        bestStreak = 3L,
        badges = listOf("first_quiz"),
        acquiredSkills = listOf("Additions simples"),
        weakPoints = listOf("Repérer tous les verbes conjugués")
    )

    private val sampleQuiz = Quiz(
        id = "quiz1",
        familyId = "fam1",
        ownerId = "uid_parent",
        lessonId = "lesson1",
        childId = "child1",
        subject = "Mathématiques",
        title = "Quiz Fractions",
        difficulty = "Moyen",
        questions = listOf(
            QuizQuestion(
                prompt = "Quel est le numérateur de 3/4 ?",
                options = listOf("3", "4", "7", "1"),
                correctIndex = 0,
                explanation = "Le numérateur est en haut.",
                guidingHint = "Regarde le nombre placé au-dessus de la barre de fraction.",
                secondHint = "Le dénominateur est dessous, cherche celui du dessus.",
                skillTag = "Identifier le numérateur"
            ),
            QuizQuestion(
                prompt = "Combien font 2 x 6 ?",
                options = listOf("10", "12", "14", "8"),
                correctIndex = 1,
                explanation = "2 multiplié par 6 égale 12.",
                guidingHint = "Additionne 6 deux fois pour vérifier ton calcul.",
                skillTag = "Table de 6"
            )
        )
    )

    @Test
    fun quizPlayerFullFlowAndReplayRefreshesXpAndBadgesBaseline() {
        var childXp by mutableLongStateOf(0L)
        var childBadges by mutableStateOf<List<String>>(emptyList())
        var childQuizzesTaken by mutableLongStateOf(0L)
        val completedCalls = mutableListOf<Triple<Int, Int, Int>>()

        composeRule.setContent {
            EduFamilleTheme {
                QuizPlayerScreen(
                    quiz = sampleQuiz,
                    childPreviousXp = childXp,
                    childExistingBadges = childBadges,
                    childQuizzesTaken = childQuizzesTaken,
                    onBack = {},
                    onQuizCompleted = { score, total, streak, _, _ ->
                        completedCalls.add(Triple(score, total, streak))
                        childXp += 86L
                        childBadges = listOf("first_quiz", "perfect_100")
                        childQuizzesTaken += 1L
                    }
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("quiz_validate_button").performScrollTo().assertIsNotEnabled()

        // Answer Question 1 correctly (option 0)
        composeRule.onNodeWithTag("quiz_option_0").performScrollTo().performClick()
        composeRule.onNodeWithTag("quiz_validate_button").performScrollTo().assertIsEnabled().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("quiz_streak_pill").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("quiz_next_button").performScrollTo().performClick()

        // Answer Question 2 correctly (option 1)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("quiz_option_1").performScrollTo().performClick()
        composeRule.onNodeWithTag("quiz_validate_button").performScrollTo().performClick()
        composeRule.onNodeWithTag("quiz_next_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals(1, completedCalls.size)
        assertEquals(Triple(2, 2, 2), completedCalls.first())
        composeRule.onNodeWithTag("quiz_xp_summary_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("quiz_earned_xp_label").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Nouveaux badges débloqués !").performScrollTo().assertIsDisplayed()

        // Replay the same quiz: enters 0 XP training mode and awards strictly 0 XP
        composeRule.onNodeWithTag("quiz_replay_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("quiz_replay_mode_pill").assertIsDisplayed()
        composeRule.onNodeWithTag("quiz_live_xp_pill").assertDoesNotExist()

        composeRule.onNodeWithTag("quiz_option_0").performScrollTo().performClick()
        composeRule.onNodeWithTag("quiz_validate_button").performScrollTo().performClick()
        composeRule.onNodeWithTag("quiz_next_button").performScrollTo().performClick()
        composeRule.onNodeWithTag("quiz_option_1").performScrollTo().performClick()
        composeRule.onNodeWithTag("quiz_validate_button").performScrollTo().performClick()
        composeRule.onNodeWithTag("quiz_next_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals(2, completedCalls.size)
        composeRule.onNodeWithTag("quiz_replay_no_xp_label").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("quiz_replay_no_xp_message").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("quiz_earned_xp_label").assertDoesNotExist()
        composeRule.onNodeWithText("Nouveaux badges débloqués !").assertDoesNotExist()
    }

    @Test
    fun quizPlayerAlreadyCompletedQuizStartsInZeroXpTrainingMode() {
        val completedQuiz = sampleQuiz.copy(attemptsCount = 1L, lastScorePercent = 80L)

        composeRule.setContent {
            EduFamilleTheme {
                QuizPlayerScreen(
                    quiz = completedQuiz,
                    childPreviousXp = 120L,
                    childExistingBadges = listOf("first_quiz"),
                    childQuizzesTaken = 1L,
                    onBack = {},
                    onQuizCompleted = { _, _, _, _, _ -> }
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("quiz_replay_mode_pill").assertIsDisplayed()
        composeRule.onNodeWithTag("quiz_live_xp_pill").assertDoesNotExist()

        composeRule.onNodeWithTag("quiz_option_0").performScrollTo().performClick()
        composeRule.onNodeWithTag("quiz_validate_button").performScrollTo().performClick()
        composeRule.onNodeWithTag("quiz_next_button").performScrollTo().performClick()
        composeRule.onNodeWithTag("quiz_option_1").performScrollTo().performClick()
        composeRule.onNodeWithTag("quiz_validate_button").performScrollTo().performClick()
        composeRule.onNodeWithTag("quiz_next_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("quiz_replay_no_xp_label").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("120 / 250 XP").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("quiz_earned_xp_label").assertDoesNotExist()
    }

    @Test
    fun quizPlayerEmptyQuizShowsSafeFallback() {
        val emptyQuiz = sampleQuiz.copy(questions = emptyList())
        var backCalled = false

        composeRule.setContent {
            EduFamilleTheme {
                QuizPlayerScreen(
                    quiz = emptyQuiz,
                    onBack = { backCalled = true },
                    onQuizCompleted = { _, _, _, _, _ -> }
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("Ce quiz ne contient aucune question.").assertIsDisplayed()
        composeRule.onNodeWithText("Retour").performClick()
        assertTrue(backCalled)
    }

    @Test
    fun quizPlayerWrongAnswerNeverRevealsAnswerGuidesChildAndReportsWeakSkills() {
        val recordedWeakSkills = mutableListOf<String>()
        val quizConjugaison = sampleQuiz.copy(
            subject = "Français",
            questions = listOf(
                QuizQuestion(
                    prompt = "Trouve les verbes conjugués dans : « Le chat saute et attrape la balle. »",
                    options = listOf("saute", "saute et attrape", "chat", "balle"),
                    correctIndex = 1,
                    guidingHint = "Relis bien toute la phrase : il n'y a pas qu'un seul verbe conjugué !",
                    secondHint = "Repère toutes les actions faites par le chat dans la phrase.",
                    skillTag = "Repérer tous les verbes conjugués",
                    explanation = "Bravo ! Les verbes conjugués expriment les actions du sujet."
                )
            )
        )

        composeRule.setContent {
            EduFamilleTheme {
                QuizPlayerScreen(
                    quiz = quizConjugaison,
                    onBack = {},
                    onQuizCompleted = { _, _, _, _, weak ->
                        recordedWeakSkills.addAll(weak)
                    }
                )
            }
        }

        composeRule.waitForIdle()
        // Child picks ONLY one verb (wrong option 0: "saute")
        composeRule.onNodeWithTag("quiz_option_0").performScrollTo().performClick()
        composeRule.onNodeWithTag("quiz_validate_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        // Warm benevolent banner + Socratic guiding hint appear
        composeRule.onNodeWithTag("quiz_feedback_banner").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("quiz_guiding_hint_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Relis bien toute la phrase : il n'y a pas qu'un seul verbe conjugué !")
            .performScrollTo().assertIsDisplayed()

        // NEVER says Bravo and NEVER spoils the answer!
        composeRule.onNodeWithText("Bravo", substring = true, ignoreCase = true).assertDoesNotExist()
        composeRule.onNodeWithText("La bonne réponse est", substring = true, ignoreCase = true).assertDoesNotExist()

        // Child retries and finds option 1 themselves! (2nd try -> half of 24 = +12 XP)
        composeRule.onNodeWithTag("quiz_option_1").performScrollTo().performClick()
        composeRule.onNodeWithTag("quiz_validate_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Super persévérance ! Tu as trouvé la bonne réponse par toi-même !", substring = true)
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("+12 XP (½ points)", substring = true)
            .performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("quiz_next_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        // Result screen reports the weak skill & shows guiding tip (NEVER "Réponse à retenir")
        assertTrue(recordedWeakSkills.contains("Repérer tous les verbes conjugués"))
        composeRule.onNodeWithTag("quiz_result_review_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Réponse à retenir", substring = true).assertDoesNotExist()
    }

    @Test
    fun quizPlayerFillBlankClassroomExerciseGuidesWithoutSpoilingThenAcceptsAnswer() {
        val fillBlankQuiz = sampleQuiz.copy(
            subject = "Anglais",
            title = "Vocabulaire du salon",
            questions = listOf(
                QuizQuestion(
                    questionType = QuizQuestion.TYPE_FILL_BLANK,
                    prompt = "Comment écrit-on « canapé » en anglais ?",
                    sentenceTemplate = "In the living room, we sit on the ____.",
                    acceptedAnswers = listOf("sofa", "couch"),
                    guidingHint = "Imagine le grand meuble confortable du salon où l'on s'assoit en famille.",
                    secondHint = "Repense au vocabulaire des pièces de la maison étudié en classe.",
                    skillTag = "Vocabulaire du salon en anglais",
                    explanation = "On utilise sofa ou couch pour dire canapé en anglais."
                )
            )
        )

        composeRule.setContent {
            EduFamilleTheme {
                QuizPlayerScreen(
                    quiz = fillBlankQuiz,
                    onBack = {},
                    onQuizCompleted = { _, _, _, _, _ -> }
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("quiz_illustration_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("quiz_sentence_template_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("quiz_fill_blank_input").performScrollTo().performTextInput("chair")
        composeRule.onNodeWithTag("quiz_validate_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        // Wrong answer shows subtle Socratic hint and NEVER reveals "sofa" or "couch"
        composeRule.onNodeWithTag("quiz_guiding_hint_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("quiz_guiding_hint_1").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Imagine le grand meuble confortable du salon où l'on s'assoit en famille.")
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("sofa", substring = true, ignoreCase = true).assertDoesNotExist()
        composeRule.onNodeWithText("couch", substring = true, ignoreCase = true).assertDoesNotExist()

        // Child corrects their text and submits "Sofa" (2nd try -> half points = +12 XP)
        composeRule.onNodeWithTag("quiz_fill_blank_input").performScrollTo().performTextClearance()
        composeRule.onNodeWithTag("quiz_fill_blank_input").performScrollTo().performTextInput("Sofa")
        composeRule.onNodeWithTag("quiz_validate_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Super persévérance ! Tu as trouvé la bonne réponse par toi-même !", substring = true)
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("+12 XP (½ points)", substring = true)
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun profilePortalShowsChildSpecificMessageWhenChildProfileMissing() {
        val childAccount = UserAccount(
            id = "uid_child",
            userId = "uid_child",
            displayName = "Léo",
            role = "CHILD",
            familyId = "fam1",
            linkedChildId = "deleted_child_id"
        )

        composeRule.setContent {
            EduFamilleTheme {
                ProfilePortalScreen(
                    userAccount = childAccount,
                    family = sampleFamily,
                    children = emptyList(),
                    pinError = null,
                    onClearPinError = {},
                    onVerifyParentPin = { false },
                    onVerifyChildPin = { _, _ -> false },
                    onSignOut = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("Ton profil enfant n'est plus disponible").assertIsDisplayed()
        composeRule.onNodeWithTag("portal_parent_admin_card").assertDoesNotExist()
    }

    @Test
    fun profilePortalParentPinFlowValidatesCorrectly() {
        val parentAccount = UserAccount(
            id = "uid_parent",
            userId = "uid_parent",
            displayName = "Parent",
            role = "PARENT",
            familyId = "fam1"
        )
        var verifiedPin by mutableStateOf<String?>(null)

        composeRule.setContent {
            EduFamilleTheme {
                ProfilePortalScreen(
                    userAccount = parentAccount,
                    family = sampleFamily,
                    children = listOf(sampleChild),
                    pinError = null,
                    onClearPinError = {},
                    onVerifyParentPin = { pin ->
                        verifiedPin = pin
                        pin == "1234"
                    },
                    onVerifyChildPin = { _, _ -> false },
                    onSignOut = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("portal_parent_admin_card"))
        composeRule.onNodeWithTag("portal_parent_admin_card").performClick()
        composeRule.waitForIdle()
        listOf("1", "2", "3", "4").forEach { digit ->
            composeRule.onNodeWithTag("pin_key_$digit").performClick()
        }
        composeRule.waitForIdle()
        assertEquals("1234", verifiedPin)
    }

    @Test
    fun familySetupClearsErrorOnRoleSwitchAndValidatesInviteForm() {
        var clearCount = 0

        composeRule.setContent {
            EduFamilleTheme {
                FamilySetupScreen(
                    opStatus = OperationStatus.Error("Code invalide"),
                    initialRole = "PARENT",
                    onClearOpStatus = { clearCount += 1 },
                    onCreateParentFamily = { _, _, _, _, _, _, _ -> },
                    onJoinAsChild = { _, _, _, _, _, _ -> },
                    onSignOut = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("role_child_card").performClick()
        composeRule.waitForIdle()
        assertEquals(1, clearCount)

        composeRule.onNodeWithTag("join_family_button").performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithTag("invite_code_input").performScrollTo().performTextInput("AB12CD")
        composeRule.onNodeWithTag("join_child_name_input").performScrollTo().performTextInput("Lina")
        composeRule.onNodeWithTag("join_child_pin_input").performScrollTo().performTextInput("2468")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("join_family_button").performScrollTo().assertIsEnabled()
    }

    @Test
    fun childSpaceHasOnlyCoursAndQuizTabsAndSupportsDocTypeDifficultyAndAutoFilingDeposit() {
        var opStatus by mutableStateOf<OperationStatus>(OperationStatus.Idle)
        var submittedSubject: String? = null
        var submittedDocType: String? = null
        var submittedDifficulty: String? = null
        var gapQuizTriggered = false

        val sampleLesson = Lesson(
            id = "lesson1",
            childId = "child1",
            subject = "Français",
            documentType = "Exercice",
            title = "Exercice de conjugaison",
            weakPoints = listOf("Repérer tous les verbes conjugués")
        )

        composeRule.setContent {
            EduFamilleTheme {
                ChildSpaceScreen(
                    family = sampleFamily,
                    child = sampleChild,
                    lessons = listOf(sampleLesson),
                    quizzes = listOf(sampleQuiz),
                    opStatus = opStatus,
                    isTranscribing = false,
                    transcribeError = null,
                    onClearOpStatus = { opStatus = OperationStatus.Idle },
                    onSwitchProfile = {},
                    onUpdateGradeLevel = { _, _ -> },
                    onDepositCourse = { subject, docType, difficulty, _, _, _ ->
                        submittedSubject = subject
                        submittedDocType = docType
                        submittedDifficulty = difficulty
                        opStatus = OperationStatus.Error("Le service d'IA est très sollicité en ce moment.")
                    },
                    onAddDocumentsToLesson = { _, _, _, _ -> },
                    onGenerateMoreQuiz = { _, _ -> },
                    onGenerateGapQuiz = { gapQuizTriggered = true },
                    onGenerateSundayReviewQuiz = {},
                    onSubmitQuizResult = { _, _, _, _, _, _, _, _, _ -> },
                    onTranscribeAudio = { _, _ -> }
                )
            }
        }

        composeRule.waitForIdle()
        // ONLY Cours and Quiz tabs exist (Atelier, Tuteur, Famille are completely removed!)
        composeRule.onNodeWithTag("nav_child_courses").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_child_quizzes").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_child_tutor").assertDoesNotExist()
        composeRule.onNodeWithTag("nav_child_studio").assertDoesNotExist()
        composeRule.onNodeWithTag("nav_child_family_chat").assertDoesNotExist()

        // Open deposit popup, choose "Exercice", select "Difficile ×1.5", keep default "Classement auto", scroll & submit
        composeRule.onNodeWithTag("deposit_course_fab").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("deposit_dialog_scroll_container").assertIsDisplayed()
        composeRule.onNodeWithTag("doctype_chip_Exercice").performScrollTo().performClick()
        composeRule.onNodeWithTag("subject_chip_Auto").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("deposit_difficulty_chip_Facile").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("deposit_difficulty_chip_Moyen").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("deposit_difficulty_chip_Défi").performScrollTo().performClick()
        composeRule.onNodeWithTag("deposit_notes_input").performScrollTo().performTextInput("Exercice de conjugaison")
        composeRule.onNodeWithTag("submit_deposit_button").assertIsDisplayed().assertIsEnabled().performClick()
        composeRule.waitForIdle()

        assertEquals(SchoolCatalog.AUTO_SUBJECT, submittedSubject)
        assertEquals("Exercice", submittedDocType)
        assertEquals("Défi", submittedDifficulty)
        composeRule.onNodeWithText("Le service d'IA est très sollicité en ce moment.").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("submit_deposit_button").assertIsDisplayed().assertIsEnabled()

        // Close dialog checks
        composeRule.onNodeWithTag("deposit_dialog_scroll_container").assertIsDisplayed()
        composeRule.onNodeWithText("Ajouter un cours ou exercice").assertIsDisplayed()
        opStatus = OperationStatus.Idle
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("subject_chip_Mathématiques").performScrollTo().assertIsDisplayed()

        assertTrue(!gapQuizTriggered)
    }

    @Test
    fun childSpaceQuizTabShowsWeakPointsAndTriggersGapQuiz() {
        var gapQuizTriggered = false
        val sampleLesson = Lesson(
            id = "lesson1",
            childId = "child1",
            subject = "Anglais",
            documentType = "Devoir",
            title = "Devoir d'anglais",
            weakPoints = listOf("Orthographe des meubles du salon")
        )

        composeRule.setContent {
            EduFamilleTheme {
                ChildSpaceScreen(
                    family = sampleFamily,
                    child = sampleChild,
                    lessons = listOf(sampleLesson),
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
                    onGenerateGapQuiz = { gapQuizTriggered = true },
                    onGenerateSundayReviewQuiz = {},
                    onSubmitQuizResult = { _, _, _, _, _, _, _, _, _ -> },
                    onTranscribeAudio = { _, _ -> }
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("nav_child_quizzes").performClick()
        composeRule.waitForIdle()

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("child_gap_quiz_card"))
        composeRule.onNodeWithTag("child_gap_quiz_card").assertIsDisplayed()
        composeRule.onNodeWithText("Orthographe des meubles du salon").assertIsDisplayed()
        composeRule.onNodeWithTag("generate_gap_quiz_button").performClick()
        assertTrue(gapQuizTriggered)
    }

    @Test
    fun parentDashboardShowsNotesAcquiredSkillsAndLacunesWithNotificationsAndNoChat() {
        var markedRead = false
        val activeLesson = Lesson(
            id = "l1",
            childId = "child1",
            subject = "Français",
            documentType = "Exercice",
            title = "Les verbes conjugués",
            acquiredSkills = listOf("Trouver le sujet"),
            weakPoints = listOf("Repérer tous les verbes conjugués")
        )
        val orphanLesson = Lesson(id = "l_orphan", childId = "deleted_child", subject = "Français", title = "Orphelin")
        val activeAttempt = QuizAttempt(
            id = "a1",
            childId = "child1",
            childName = "Léo",
            lessonId = "l1",
            lessonTitle = "Les verbes conjugués",
            subject = "Français",
            score = 4,
            totalQuestions = 5,
            percentage = 80,
            xpEarned = 110,
            masteredSkills = listOf("Trouver le sujet"),
            weakSkills = listOf("Repérer tous les verbes conjugués")
        )
        val orphanAttempt = QuizAttempt(
            id = "a_orphan",
            childId = "deleted_child",
            childName = "Fantôme",
            lessonId = "l_orphan",
            lessonTitle = "Orphelin",
            subject = "Français",
            score = 1,
            totalQuestions = 5,
            percentage = 20
        )

        composeRule.setContent {
            EduFamilleTheme {
                ParentDashboardScreen(
                    family = sampleFamily,
                    children = listOf(sampleChild),
                    lessons = listOf(activeLesson, orphanLesson),
                    quizzes = emptyList(),
                    attempts = listOf(activeAttempt, orphanAttempt),
                    opStatus = OperationStatus.Idle,
                    onSwitchProfile = {},
                    onAddChild = { _, _, _, _, _ -> },
                    onEditChild = { _, _, _, _, _, _ -> },
                    onDeleteChild = {},
                    unreadNotificationCount = 1,
                    onMarkNotificationsRead = { markedRead = true }
                )
            }
        }

        composeRule.waitForIdle()
        // Chat tab is completely gone
        composeRule.onNodeWithTag("nav_parent_chat").assertDoesNotExist()

        // Notification dialog shows child name
        composeRule.onNodeWithTag("parent_notifications_button").performClick()
        composeRule.waitForIdle()
        assertTrue(markedRead)
        composeRule.onNodeWithTag("parent_notifications_dialog").assertIsDisplayed()
        composeRule.onNodeWithText("Quiz validé par Léo !").assertIsDisplayed()
        composeRule.onNodeWithText("Fermer").performClick()
        composeRule.waitForIdle()

        // Pedagogical recap card shows both weak points ("Ce que Léo doit travailler") & acquired skills
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("parent_skills_recap_card"))
        composeRule.onNodeWithTag("parent_skills_recap_card").assertIsDisplayed()
        composeRule.onNodeWithTag("parent_weak_points_section").assertIsDisplayed()
        composeRule.onNodeWithTag("parent_acquired_skills_section").assertIsDisplayed()
        composeRule.onNodeWithText("Repérer tous les verbes conjugués").assertIsDisplayed()
        composeRule.onNodeWithText("Trouver le sujet").assertIsDisplayed()

        // Orphan deleted child attempts are excluded
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Léo • Français"))
        composeRule.onNodeWithText("Léo • Français").assertIsDisplayed()
        composeRule.onNodeWithText("Fantôme • Français").assertDoesNotExist()
    }

    @Test
    fun quizNotificationHelperPostsAndroidNotificationWithChildName() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        val attempt = QuizAttempt(
            id = "att_42",
            childId = "child1",
            childName = "Emma",
            lessonId = "l2",
            lessonTitle = "La Révolution française",
            subject = "Histoire-Géo",
            score = 4,
            totalQuestions = 5,
            percentage = 80,
            xpEarned = 110
        )

        val posted = QuizNotificationHelper.showQuizValidatedNotification(app, attempt)
        assertTrue(posted)

        val nm = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notifications = Shadows.shadowOf(nm).allNotifications
        assertTrue(notifications.isNotEmpty())
        val latest = notifications.last()
        val title = latest.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = latest.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        assertTrue("Notification title must contain child name: $title", title.contains("Emma"))
        assertTrue("Notification body must contain child name and score: $text", text.contains("Emma") && text.contains("4/5"))
    }

    @Test
    fun quizzyWordmarkLogoAndAnimatedBookLoaderRenderInLightAndDarkModes() {
        var darkMode by mutableStateOf(false)
        composeRule.mainClock.autoAdvance = false

        composeRule.setContent {
            EduFamilleTheme(darkTheme = darkMode) {
                androidx.compose.foundation.layout.Column {
                    QuizzyWordmarkLogo()
                    QuizzyBookLoader(message = "Chargement des cours...")
                    QuizzyInlineLoader()
                }
            }
        }

        composeRule.mainClock.advanceTimeBy(300)
        composeRule.onNodeWithTag("quizzy_wordmark_logo").assertIsDisplayed()
        composeRule.onNodeWithTag("quizzy_book_loader").assertIsDisplayed()
        composeRule.onNodeWithText("Chargement des cours...").assertIsDisplayed()

        darkMode = true
        composeRule.mainClock.advanceTimeBy(400)
        composeRule.onNodeWithTag("quizzy_wordmark_logo").assertIsDisplayed()
        composeRule.onNodeWithTag("quizzy_book_loader").assertIsDisplayed()
    }

    @Test
    fun quizPlayerInteractiveDragMatchAndDragOrderShowIllustrationsAndSocraticGuidance() {
        val pairs = listOf(
            com.example.model.QuizMatchPair("Canapé", "Sofa"),
            com.example.model.QuizMatchPair("Chat", "Cat")
        )
        val orderItems = listOf("Bouche", "Estomac", "Intestin")
        val interactiveQuiz = Quiz(
            id = "quiz_interactive",
            subject = "Anglais",
            title = "Quiz Mini-Jeux",
            difficulty = "Moyen",
            questions = listOf(
                QuizQuestion(
                    prompt = "Associe chaque mot français à son mot anglais",
                    questionType = QuizQuestion.TYPE_DRAG_MATCH,
                    matchingPairs = pairs,
                    illustrationKey = QuizQuestion.ILLUS_ENGLISH,
                    illustrationCaption = "Observe bien le salon illustré !",
                    guidingHint = "Associe d'abord l'objet du salon que tu reconnais tout de suite.",
                    secondHint = "Compare chaque étiquette avec l'animal et le meuble.",
                    skillTag = "Vocabulaire anglais",
                    explanation = "Canapé se dit Sofa et Chat se dit Cat."
                ),
                QuizQuestion(
                    prompt = "Remets les étapes de la digestion dans l'ordre",
                    questionType = QuizQuestion.TYPE_DRAG_ORDER,
                    orderedItems = orderItems,
                    illustrationKey = QuizQuestion.ILLUS_SCIENCE,
                    illustrationCaption = "Suis le trajet des aliments !",
                    guidingHint = "Par où entrent les aliments en tout premier ?",
                    skillTag = "Étapes de la digestion",
                    explanation = "Les aliments passent par la bouche, puis l'estomac, puis l'intestin."
                )
            )
        )

        var completedScore = -1
        var completedTotal = -1
        composeRule.setContent {
            EduFamilleTheme {
                QuizPlayerScreen(
                    quiz = interactiveQuiz,
                    onBack = {},
                    onQuizCompleted = { score, total, _, _, _ ->
                        completedScore = score
                        completedTotal = total
                    }
                )
            }
        }

        // Question 1: DRAG_MATCH + Illustration visible
        composeRule.onNodeWithTag("quiz_illustration_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Observe bien le salon illustré !").assertIsDisplayed()
        composeRule.onNodeWithTag("drag_match_container").performScrollTo().assertIsDisplayed()

        // Intentionally make a wrong assignment via tap-to-place: Canapé -> Cat, Chat -> Sofa
        composeRule.onNodeWithText("Cat").performScrollTo().performClick()
        composeRule.onNodeWithTag("drag_match_slot_0").performScrollTo().performClick()
        composeRule.onNodeWithText("Sofa").performScrollTo().performClick()
        composeRule.onNodeWithTag("drag_match_slot_1").performScrollTo().performClick()

        composeRule.onNodeWithTag("quiz_validate_button").performScrollTo().performClick()

        // Socratic hint shown without leaking answer
        composeRule.onNodeWithTag("quiz_guiding_hint_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Associe d'abord l'objet du salon que tu reconnais tout de suite.", substring = true).assertIsDisplayed()

        // Fix assignments: remove both slots and place correctly (Canapé -> Sofa, Chat -> Cat)
        composeRule.onNodeWithTag("drag_match_slot_0").performScrollTo().performClick()
        composeRule.onNodeWithTag("drag_match_slot_1").performScrollTo().performClick()
        composeRule.onNodeWithText("Sofa").performScrollTo().performClick()
        composeRule.onNodeWithTag("drag_match_slot_0").performScrollTo().performClick()
        composeRule.onNodeWithText("Cat").performScrollTo().performClick()
        composeRule.onNodeWithTag("drag_match_slot_1").performScrollTo().performClick()

        composeRule.onNodeWithTag("quiz_validate_button").performScrollTo().performClick()
        composeRule.onNodeWithTag("quiz_next_button").performScrollTo().performClick()

        // Question 2: DRAG_ORDER + Illustration visible
        composeRule.onNodeWithTag("quiz_illustration_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Suis le trajet des aliments !").assertIsDisplayed()
        composeRule.onNodeWithTag("drag_order_container").performScrollTo().assertIsDisplayed()

        // Place items in the exact order: Bouche -> Estomac -> Intestin
        composeRule.onNodeWithText("Bouche").performScrollTo().performClick()
        composeRule.onNodeWithText("Estomac").performScrollTo().performClick()
        composeRule.onNodeWithText("Intestin").performScrollTo().performClick()

        composeRule.onNodeWithTag("quiz_validate_button").performScrollTo().performClick()
        composeRule.onNodeWithTag("quiz_correct_explanation_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("quiz_next_button").performScrollTo().performClick()

        assertEquals(1, completedScore) // Q1 took 2 attempts (0 first-try), Q2 solved 1st try (1)
        assertEquals(2, completedTotal)
    }

    @Test
    fun quizPlayerProgressiveHalvingXpCumulativeTwoHintsAndImageChoiceWorkProperly() {
        val imageAndRetryQuiz = Quiz(
            id = "quiz_image_and_retry",
            subject = "Mathématiques",
            title = "Quiz Images & Essais Progressifs",
            difficulty = "Moyen", // 24 XP -> 12 XP -> 6 XP -> 0 XP
            questions = listOf(
                QuizQuestion(
                    prompt = "Clique sur l'image qui représente un angle droit",
                    questionType = QuizQuestion.TYPE_IMAGE_CHOICE,
                    imageOptions = listOf(
                        com.example.model.QuizImageOption("Angle droit", "right_angle"),
                        com.example.model.QuizImageOption("Cercle", "circle"),
                        com.example.model.QuizImageOption("Triangle", "triangle"),
                        com.example.model.QuizImageOption("Carré", "square")
                    ),
                    correctIndex = 0,
                    illustrationKey = QuizQuestion.ILLUS_MATH,
                    illustrationCaption = "Compare bien les formes géométriques !",
                    guidingHint = "Pense à l'outil d'écolier qu'on utilise pour vérifier les coins d'une feuille.",
                    secondHint = "Observe comment deux segments se croisent comme le coin d'un cahier.",
                    skillTag = "Reconnaître un angle droit",
                    explanation = "Un angle droit se reconnaît grâce à l'équerre."
                ),
                QuizQuestion(
                    prompt = "Écris le résultat de 7 × 8",
                    questionType = QuizQuestion.TYPE_FILL_BLANK,
                    sentenceTemplate = "7 × 8 = ____",
                    acceptedAnswers = listOf("56"),
                    illustrationKey = QuizQuestion.ILLUS_MATH,
                    guidingHint = "Tu peux décomposer le calcul en t'appuyant sur un produit voisin.",
                    secondHint = "Vérifie en ajoutant une fois de plus le même nombre au résultat précédent.",
                    skillTag = "Table de 7 et 8",
                    explanation = "7 × 8 = 56."
                )
            )
        )

        var reportedEarnedXp = -1L
        composeRule.setContent {
            EduFamilleTheme {
                QuizPlayerScreen(
                    quiz = imageAndRetryQuiz,
                    onBack = {},
                    onQuizCompletedWithXp = { _, _, _, _, _, earnedXp ->
                        reportedEarnedXp = earnedXp
                    }
                )
            }
        }

        // Q1: Clickable IMAGE_CHOICE 2x2 grid
        composeRule.onNodeWithTag("image_choice_grid").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("quiz_image_option_0").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("quiz_image_option_1").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("quiz_image_option_2").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("quiz_image_option_3").performScrollTo().assertIsDisplayed()

        // 1st wrong image (option 1): Indice 1 shown ONLY, next attempt offers half points (+12 XP)
        composeRule.onNodeWithTag("quiz_image_option_1").performScrollTo().performClick()
        composeRule.onNodeWithTag("quiz_validate_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("quiz_guiding_hint_1").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("quiz_guiding_hint_2").assertDoesNotExist()
        composeRule.onNodeWithText("+12 XP", substring = true).performScrollTo().assertIsDisplayed()

        // 2nd wrong image (option 2): BOTH Indice 1 AND Indice 2 are visible simultaneously, next attempt offers quarter points (+6 XP)
        composeRule.onNodeWithTag("quiz_image_option_2").performScrollTo().performClick()
        composeRule.onNodeWithTag("quiz_validate_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("quiz_guiding_hint_1").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("quiz_guiding_hint_2").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Pense à l'outil d'écolier qu'on utilise pour vérifier les coins d'une feuille.")
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Observe comment deux segments se croisent comme le coin d'un cahier.")
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("+6 XP", substring = true).performScrollTo().assertIsDisplayed()

        // 3rd try right (option 0): earns +6 XP (quarter points)
        composeRule.onNodeWithTag("quiz_image_option_0").performScrollTo().performClick()
        composeRule.onNodeWithTag("quiz_validate_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("+6 XP (¼ points)", substring = true).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("quiz_next_button").performScrollTo().performClick()

        // Q2: 3 wrong attempts -> still allowed to retry, BOTH hints stay visible, 0 XP awarded when finally right!
        listOf("50", "52", "54").forEach { wrong ->
            composeRule.onNodeWithTag("quiz_fill_blank_input").performScrollTo().performTextClearance()
            composeRule.onNodeWithTag("quiz_fill_blank_input").performScrollTo().performTextInput(wrong)
            composeRule.onNodeWithTag("quiz_validate_button").performScrollTo().performClick()
            composeRule.waitForIdle()
        }
        composeRule.onNodeWithTag("quiz_guiding_hint_1").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("quiz_guiding_hint_2").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Aucun point (0 XP)", substring = true).performScrollTo().assertIsDisplayed()

        // 4th try right ("56"): allowed to finish, earns strictly 0 XP on this question
        composeRule.onNodeWithTag("quiz_fill_blank_input").performScrollTo().performTextClearance()
        composeRule.onNodeWithTag("quiz_fill_blank_input").performScrollTo().performTextInput("56")
        composeRule.onNodeWithTag("quiz_validate_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("0 XP à ce niveau", substring = true).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("quiz_next_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        // Total XP earned across the quiz: 6 (Q1 on 3rd try) + 0 (Q2 on 4th try) = 6 XP
        assertEquals(6L, reportedEarnedXp)
        composeRule.onNodeWithText("+6 XP gagnés").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun quizPlayerAlwaysCountsFirstTryWrongAsMissedEvenWhenRetriedCorrectly() {
        val singleQuestionQuiz = sampleQuiz.copy(
            id = "q_first_try_miss",
            subject = "Mathématiques",
            difficulty = "Moyen",
            questions = listOf(
                QuizQuestion(
                    prompt = "Quel est le numérateur de 3/4 ?",
                    options = listOf("3", "4", "7", "1"),
                    correctIndex = 0,
                    guidingHint = "Regarde la position du nombre au-dessus de la barre.",
                    secondHint = "Le dénominateur se situe en dessous.",
                    skillTag = "Identifier le numérateur",
                    explanation = "Le numérateur est le nombre du haut."
                )
            )
        )

        var recordedScore = -1
        var recordedTotal = -1
        var recordedXp = -1L
        val recordedMissed = mutableListOf<MissedQuestionRecord>()

        composeRule.setContent {
            EduFamilleTheme {
                QuizPlayerScreen(
                    quiz = singleQuestionQuiz,
                    lessonTitle = "Les fractions simples",
                    onBack = {},
                    onQuizCompletedFull = { score, total, _, _, _, earnedXp, missed ->
                        recordedScore = score
                        recordedTotal = total
                        recordedXp = earnedXp
                        recordedMissed.clear()
                        recordedMissed.addAll(missed)
                    }
                )
            }
        }

        composeRule.waitForIdle()
        // 1st try WRONG (option 1 = "4")
        composeRule.onNodeWithTag("quiz_option_1").performScrollTo().performClick()
        composeRule.onNodeWithTag("quiz_validate_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        // 2nd try RIGHT (option 0 = "3") -> earns half XP (+12 XP), STILL recorded as wrong answer!
        composeRule.onNodeWithTag("quiz_option_0").performScrollTo().performClick()
        composeRule.onNodeWithTag("quiz_validate_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("quiz_first_try_miss_recorded_note").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("quiz_next_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals(0, recordedScore)
        assertEquals(1, recordedTotal)
        assertEquals(12L, recordedXp)
        assertEquals(1, recordedMissed.size)
        assertEquals("Identifier le numérateur", recordedMissed.first().skillTag)
        assertEquals("Les fractions simples", recordedMissed.first().lessonTitle)
        composeRule.onNodeWithTag("quiz_result_missed_count_notice").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun lessonDetailSynthesisAllowsGeneratingQuizAtAnyDifficultyLevel() {
        var requestedDifficulty: String? = null
        val lesson = Lesson(
            id = "lesson_synth",
            childId = "child1",
            subject = "Mathématiques",
            documentType = "Cours",
            title = "Les fractions",
            summaryIntro = "Une fraction représente une part d'un tout.",
            keyPoints = listOf("Numérateur en haut", "Dénominateur en bas"),
            simpleExplanation = "Si tu coupes un gâteau en 4 parts, 1 part vaut 1/4.",
            memoryTip = "Dénominateur = Dessous."
        )

        composeRule.setContent {
            EduFamilleTheme {
                LessonDetailScreen(
                    lesson = lesson,
                    quizzes = listOf(sampleQuiz),
                    opStatus = OperationStatus.Idle,
                    isReadOnlyParent = false,
                    onBack = {},
                    onStartQuiz = {},
                    onGenerateNewQuiz = { requestedDifficulty = it }
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("generate_more_quiz_button"))
        composeRule.onNodeWithTag("lesson_difficulty_chip_Facile").assertIsDisplayed()
        composeRule.onNodeWithTag("lesson_difficulty_chip_Moyen").assertIsDisplayed()
        composeRule.onNodeWithTag("lesson_difficulty_chip_Défi").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("generate_more_quiz_button").performClick()
        assertEquals("Défi", requestedDifficulty)
    }

    @Test
    fun childSpaceSundayReviewCardLocksBeforeSunday14hAndTriggersGenerationAtSunday14h() {
        val missedList = listOf(
            MissedQuestionRecord(
                subject = "Mathématiques",
                lessonId = "lesson1",
                lessonTitle = "Fractions",
                quizId = "quiz1",
                skillTag = "Identifier le numérateur",
                prompt = "Quel est le numérateur de 3/4 ?",
                weekKey = "2026-W47"
            ),
            MissedQuestionRecord(
                subject = "Français",
                lessonId = "lesson2",
                lessonTitle = "Conjugaison",
                quizId = "quiz2",
                skillTag = "Verbes conjugués",
                prompt = "Trouve tous les verbes conjugués.",
                weekKey = "2026-W47"
            )
        )
        val childWithMistakes = sampleChild.copy(weeklyMissedQuestions = missedList)

        var simulatedCalendar by mutableStateOf(
            Calendar.getInstance().apply {
                set(2026, Calendar.NOVEMBER, 20, 10, 0, 0) // Friday 10:00
                set(Calendar.MILLISECOND, 0)
            }
        )
        var sundayDifficultyRequested: String? = null

        composeRule.setContent {
            EduFamilleTheme {
                ChildSpaceScreen(
                    family = sampleFamily,
                    child = childWithMistakes,
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
                    onGenerateSundayReviewQuiz = { sundayDifficultyRequested = it },
                    onSubmitQuizResult = { _, _, _, _, _, _, _, _, _ -> },
                    onTranscribeAudio = { _, _ -> },
                    nowCalendarProvider = { simulatedCalendar }
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("nav_child_quizzes").performClick()
        composeRule.waitForIdle()

        // Friday 10:00 -> card shows 2 memorized wrong answers, countdown, and button is disabled
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("sunday_review_quiz_card"))
        composeRule.onNodeWithTag("sunday_review_quiz_card").assertIsDisplayed()
        composeRule.onNodeWithTag("sunday_review_schedule_badge").assertIsDisplayed()
        composeRule.onNodeWithTag("sunday_review_missed_summary").assertIsDisplayed()
        composeRule.onNodeWithText("Disponible dimanche à 14h").assertIsDisplayed()
        composeRule.onNodeWithTag("sunday_review_action_button").assertIsNotEnabled()

        // Switch clock to Sunday 14:05 -> unlocks difficulty selector & 10-question generation CTA
        simulatedCalendar = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 22, 14, 5, 0) // Sunday 14:05
            set(Calendar.MILLISECOND, 0)
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("sunday_difficulty_chip_Défi").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Générer mon quiz du dimanche (10 Q)").assertIsDisplayed()
        composeRule.onNodeWithTag("sunday_review_action_button").assertIsEnabled().performClick()
        assertEquals("Défi", sundayDifficultyRequested)
    }

    @Test
    fun darkModeProvidesHighContrastTokensAndRendersAllPrimaryScreensCleanly() {
        var capturedDarkColors: QuizzyExtendedColors? = null
        var capturedLightColors: QuizzyExtendedColors? = null
        var screenIndex by mutableStateOf(0)

        val darkSampleLesson = Lesson(
            id = "lesson_dark",
            childId = "child1",
            subject = "Anglais",
            documentType = "Cours",
            title = "Les articles a / an",
            summaryIntro = "On utilise 'an' devant un son voyelle.",
            keyPoints = listOf("a book", "an eraser"),
            simpleExplanation = "Écoute le premier son du mot.",
            memoryTip = "Son voyelle -> an !",
            weakPoints = listOf("Emploi des articles a / an")
        )
        val darkFillQuiz = sampleQuiz.copy(
            id = "quiz_dark_fill",
            subject = "Anglais",
            title = "Articles en anglais",
            questions = listOf(
                QuizQuestion(
                    questionType = QuizQuestion.TYPE_FILL_BLANK,
                    prompt = "Complète la phrase : 'This is ____ eraser.'",
                    sentenceTemplate = "This is ____ eraser.",
                    acceptedAnswers = listOf("an"),
                    guidingHint = "Écoute le premier son du mot qui suit le blanc.",
                    secondHint = "Ce mot commence par un son voyelle.",
                    skillTag = "Emploi des articles a / an",
                    explanation = "On écrit 'an eraser'."
                )
            )
        )

        composeRule.setContent {
            EduFamilleTheme(darkTheme = false) {
                capturedLightColors = QuizzyTheme.colors
            }
            EduFamilleTheme(darkTheme = true) {
                capturedDarkColors = QuizzyTheme.colors
                when (screenIndex) {
                    0 -> QuizPlayerScreen(
                        quiz = darkFillQuiz,
                        lessonTitle = darkSampleLesson.title,
                        onBack = {},
                        onQuizCompletedFull = { _, _, _, _, _, _, _ -> }
                    )
                    1 -> LessonDetailScreen(
                        lesson = darkSampleLesson,
                        quizzes = listOf(darkFillQuiz),
                        opStatus = OperationStatus.Idle,
                        isReadOnlyParent = false,
                        onBack = {},
                        onStartQuiz = {},
                        onGenerateNewQuiz = {}
                    )
                    2 -> ChildSpaceScreen(
                        family = sampleFamily,
                        child = sampleChild,
                        lessons = listOf(darkSampleLesson),
                        quizzes = listOf(darkFillQuiz),
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
                        onGenerateSundayReviewQuiz = {},
                        onSubmitQuizResult = { _, _, _, _, _, _, _, _, _ -> },
                        onTranscribeAudio = { _, _ -> }
                    )
                    else -> ParentDashboardScreen(
                        family = sampleFamily,
                        children = listOf(sampleChild),
                        lessons = listOf(darkSampleLesson),
                        quizzes = listOf(darkFillQuiz),
                        attempts = emptyList(),
                        opStatus = OperationStatus.Idle,
                        onSwitchProfile = {},
                        onAddChild = { _, _, _, _, _ -> },
                        onEditChild = { _, _, _, _, _, _ -> },
                        onDeleteChild = {}
                    )
                }
            }
        }

        composeRule.waitForIdle()

        // 1. Verify high-contrast dark palette tokens vs light palette tokens
        val dark = requireNotNull(capturedDarkColors)
        val light = requireNotNull(capturedLightColors)
        assertTrue("Dark theme flag must be true", dark.isDark)
        assertTrue("Light theme flag must be false", !light.isDark)
        assertTrue("Dark headingInk must be bright for readability", dark.headingInk.luminance() > 0.65f)
        assertTrue("Dark bodyInk must be bright for readability", dark.bodyInk.luminance() > 0.70f)
        assertTrue("Dark secondaryInk must be readable", dark.secondaryInk.luminance() > 0.40f)
        assertTrue("Dark pastelMint card must be dark", dark.pastelMint.luminance() < 0.15f)
        assertTrue("Dark pastelPeach card must be dark", dark.pastelPeach.luminance() < 0.15f)
        assertTrue("Dark pastelYellow card must be dark", dark.pastelYellow.luminance() < 0.15f)
        assertTrue("Dark parentCanvas must be dark", dark.parentCanvas.luminance() < 0.10f)
        assertTrue("Dark chipOnTint must be dark", dark.chipOnTint.luminance() < 0.15f)

        // 2. QuizPlayerScreen fill-in-the-blank + wrong attempt hint in Dark Mode
        composeRule.onNodeWithTag("quiz_fill_blank_input").performScrollTo().performTextInput("a")
        composeRule.onNodeWithTag("quiz_validate_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("quiz_guiding_hint_card").performScrollTo().assertIsDisplayed()

        // 3. LessonDetailScreen in Dark Mode
        screenIndex = 1
        composeRule.waitForIdle()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("generate_more_quiz_button"))
        composeRule.onNodeWithTag("generate_more_quiz_button").assertIsDisplayed()

        // 4. ChildSpaceScreen in Dark Mode
        screenIndex = 2
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("nav_child_courses").assertIsDisplayed()
        composeRule.onNodeWithTag("deposit_course_fab").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_child_quizzes").performClick()
        composeRule.waitForIdle()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("sunday_review_quiz_card"))
        composeRule.onNodeWithTag("sunday_review_quiz_card").assertIsDisplayed()

        // 5. ParentDashboardScreen in Dark Mode
        screenIndex = 3
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Espace parents").assertIsDisplayed()
    }
}
