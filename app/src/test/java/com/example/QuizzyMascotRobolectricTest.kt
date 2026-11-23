package com.example

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.performScrollTo
import com.example.model.Quiz
import com.example.model.QuizQuestion
import com.example.ui.components.QuizzyAnimatedMascot
import com.example.ui.components.QuizzyMascotMood
import com.example.ui.components.QuizzyStarMascot
import com.example.ui.screens.QuizPlayerScreen
import com.example.ui.theme.EduFamilleTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class QuizzyMascotRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun animatedMascot_rendersAllFiveMoods_inLightAndDarkThemes_andSupportsTap() {
        var currentMood by mutableStateOf(QuizzyMascotMood.IDLE)
        var darkMode by mutableStateOf(false)

        composeRule.setContent {
            EduFamilleTheme(darkTheme = darkMode) {
                QuizzyAnimatedMascot(
                    mood = currentMood,
                    size = 110.dp,
                    interactive = true
                )
            }
        }

        // Verify all 5 distinct moods render cleanly in Light Mode
        QuizzyMascotMood.entries.forEach { mood ->
            composeRule.runOnIdle { currentMood = mood }
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("quizzy_animated_mascot").assertIsDisplayed()
            composeRule.onNodeWithTag(
                "quizzy_mascot_pose_${mood.name.lowercase()}",
                useUnmergedTree = true
            ).assertIsDisplayed()
        }

        // Tap-to-cheer interaction works cleanly without crashing
        composeRule.onNodeWithTag("quizzy_animated_mascot").performClick()
        composeRule.waitForIdle()

        // Verify Dark Mode renders cleanly across moods
        composeRule.runOnIdle {
            darkMode = true
            currentMood = QuizzyMascotMood.VICTORY
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("quizzy_mascot_pose_victory", useUnmergedTree = true).assertIsDisplayed()
        assertEquals(5, QuizzyMascotMood.entries.size)
    }

    @Test
    fun starMascotWrapper_delegatesToAnimatedMascot() {
        composeRule.setContent {
            EduFamilleTheme(darkTheme = false) {
                Column {
                    QuizzyStarMascot(size = 96.dp, mood = QuizzyMascotMood.WAVE)
                }
            }
        }

        composeRule.onNodeWithTag("quizzy_animated_mascot").assertIsDisplayed()
        composeRule.onNodeWithTag("quizzy_mascot_pose_wave", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun quizPlayer_displaysContextualMascotMoods_onWrongAndCorrectAnswers() {
        val sampleQuiz = Quiz(
            id = "quiz_mascot_1",
            lessonId = "lesson_mascot_1",
            subject = "Sciences",
            title = "Le cycle de l'eau",
            questions = listOf(
                QuizQuestion(
                    prompt = "Quel phénomène transforme l'eau liquide en vapeur ?",
                    options = listOf("Évaporation", "Condensation", "Solidification"),
                    correctIndex = 0,
                    explanation = "Sous l'effet de la chaleur du Soleil, l'eau liquide s'évapore.",
                    guidingHint = "Pense à ce que fait la chaleur du soleil sur une flaque d'eau.",
                    secondHint = "L'eau liquide monte dans l'air sous forme de gaz invisible."
                )
            )
        )

        composeRule.setContent {
            EduFamilleTheme(darkTheme = false) {
                QuizPlayerScreen(
                    quiz = sampleQuiz,
                    childAge = 10,
                    onBack = {},
                    onQuizCompleted = { _, _, _, _, _ -> }
                )
            }
        }

        // 1. Choose wrong option -> ENCOURAGE + THINKING mascot poses appear!
        composeRule.onNodeWithTag("quiz_option_1").performScrollTo().performClick()
        composeRule.onNodeWithTag("quiz_validate_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("quiz_feedback_banner").performScrollTo().assertIsDisplayed()
        composeRule.onAllNodesWithTag("quizzy_mascot_pose_encourage", useUnmergedTree = true)
            .onFirst().assertIsDisplayed()
        composeRule.onNodeWithTag("quiz_guiding_hint_card").performScrollTo().assertIsDisplayed()
        composeRule.onAllNodesWithTag("quizzy_mascot_pose_thinking", useUnmergedTree = true)
            .onFirst().assertIsDisplayed()

        // 2. Retry with correct option -> VICTORY mascot pose appears!
        composeRule.onNodeWithTag("quiz_option_0").performScrollTo().performClick()
        composeRule.onNodeWithTag("quiz_validate_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("quiz_feedback_banner").performScrollTo().assertIsDisplayed()
        composeRule.onAllNodesWithTag("quizzy_mascot_pose_victory", useUnmergedTree = true)
            .onFirst().assertIsDisplayed()
    }
}
