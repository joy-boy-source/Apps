package com.example

import com.example.model.ChildProfile
import com.example.model.GamificationCatalog
import com.example.model.PinHasher
import com.example.model.SchoolCatalog
import com.example.ui.components.documentFormatLabel
import com.example.ui.components.formatFileSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GamificationAndModelsTest {

    @Test
    fun childSuccessRatePercentHandlesZeroAndBounds() {
        val noAttempts = ChildProfile(totalCorrect = 0, totalQuestions = 0)
        assertEquals(0, noAttempts.successRatePercent)

        val partial = ChildProfile(totalCorrect = 4, totalQuestions = 5)
        assertEquals(80, partial.successRatePercent)

        val perfect = ChildProfile(totalCorrect = 10, totalQuestions = 10)
        assertEquals(100, perfect.successRatePercent)

        val overflowSafe = ChildProfile(totalCorrect = 15, totalQuestions = 10)
        assertEquals(100, overflowSafe.successRatePercent)
    }

    @Test
    fun calculateQuizXpAppliesDifficultyMultiplierStreakAndPerfectBonus() {
        // Facile (x1.0): 3*15 + streak 2*5 + completion 10 = 65
        assertEquals(65L, GamificationCatalog.calculateQuizXp(3, 5, 2, "Facile"))

        // Moyen (x1.2): 5*15*1.2 (90) + streak 5*5 (25) + completion 10 + perfect 30 = 155
        assertEquals(155L, GamificationCatalog.calculateQuizXp(5, 5, 5, "Moyen"))

        // Défi (x1.5): 4*15*1.5 (90) + streak 3*5 (15) + completion 10 = 115
        assertEquals(115L, GamificationCatalog.calculateQuizXp(4, 5, 3, "Défi"))

        // 0 bonnes réponses -> minimum garanti de 10 XP (bonus complétion)
        assertEquals(10L, GamificationCatalog.calculateQuizXp(0, 5, 0, "Facile"))

        // Rejouer un quiz déjà fait rapporte strictement 0 XP quel que soit le score ou la difficulté
        assertEquals(0L, GamificationCatalog.calculateQuizXp(5, 5, 5, "Défi", isReplay = true))
        assertEquals(0L, GamificationCatalog.calculateQuizXp(3, 5, 2, "Facile", isReplay = true))
        assertEquals(0L, GamificationCatalog.calculateQuizXp(0, 5, 0, "Moyen", isReplay = true))

        // Quiz.isAlreadyCompleted
        val freshQuiz = com.example.model.Quiz(id = "q_new", attemptsCount = 0L, lastScorePercent = null)
        val completedByAttempts = com.example.model.Quiz(id = "q_done_1", attemptsCount = 1L, lastScorePercent = null)
        val completedByScore = com.example.model.Quiz(id = "q_done_2", attemptsCount = 0L, lastScorePercent = 80L)
        assertFalse(freshQuiz.isAlreadyCompleted)
        assertTrue(completedByAttempts.isAlreadyCompleted)
        assertTrue(completedByScore.isAlreadyCompleted)
    }

    @Test
    fun questionXpForAttemptHalvesPointsTwiceThenDropsToZeroAndAlwaysZeroOnReplay() {
        // Facile : 20 -> 10 -> 5 -> 0 -> 0
        assertEquals(20, GamificationCatalog.questionXpForAttempt("Facile", 0))
        assertEquals(10, GamificationCatalog.questionXpForAttempt("Facile", 1))
        assertEquals(5, GamificationCatalog.questionXpForAttempt("Facile", 2))
        assertEquals(0, GamificationCatalog.questionXpForAttempt("Facile", 3))
        assertEquals(0, GamificationCatalog.questionXpForAttempt("Facile", 5))

        // Moyen : 24 -> 12 -> 6 -> 0 -> 0
        assertEquals(24, GamificationCatalog.questionXpForAttempt("Moyen", 0))
        assertEquals(12, GamificationCatalog.questionXpForAttempt("Moyen", 1))
        assertEquals(6, GamificationCatalog.questionXpForAttempt("Moyen", 2))
        assertEquals(0, GamificationCatalog.questionXpForAttempt("Moyen", 3))

        // Défi : 32 -> 16 -> 8 -> 0 -> 0
        assertEquals(32, GamificationCatalog.questionXpForAttempt("Défi", 0))
        assertEquals(16, GamificationCatalog.questionXpForAttempt("Défi", 1))
        assertEquals(8, GamificationCatalog.questionXpForAttempt("Défi", 2))
        assertEquals(0, GamificationCatalog.questionXpForAttempt("Défi", 3))

        // Replay toujours strictement 0 XP quel que soit l'essai
        for (wrong in 0..3) {
            assertEquals(0, GamificationCatalog.questionXpForAttempt("Défi", wrong, isReplay = true))
            assertEquals(0, GamificationCatalog.questionXpForAttempt("Moyen", wrong, isReplay = true))
        }
    }

    @Test
    fun starsForPercentageMatchesThresholds() {
        assertEquals(1, GamificationCatalog.starsForPercentage(0))
        assertEquals(1, GamificationCatalog.starsForPercentage(49))
        assertEquals(2, GamificationCatalog.starsForPercentage(50))
        assertEquals(2, GamificationCatalog.starsForPercentage(79))
        assertEquals(3, GamificationCatalog.starsForPercentage(80))
        assertEquals(3, GamificationCatalog.starsForPercentage(100))
    }

    @Test
    fun levelProgressHandlesAllTiersAndNegativeXpSafely() {
        val neg = GamificationCatalog.levelProgress(-50L)
        assertEquals(1, neg.level)
        assertEquals(0L, neg.totalXp)
        assertEquals(0f, neg.progressFraction, 0.001f)

        val l1 = GamificationCatalog.levelProgress(50L)
        assertEquals(1, l1.level)
        assertEquals(0.5f, l1.progressFraction, 0.001f)

        assertEquals(2, GamificationCatalog.levelProgress(100L).level)
        assertEquals(3, GamificationCatalog.levelProgress(250L).level)
        assertEquals(4, GamificationCatalog.levelProgress(500L).level)
        assertEquals(5, GamificationCatalog.levelProgress(900L).level)

        val l6 = GamificationCatalog.levelProgress(1400L)
        assertEquals(6, l6.level)
        assertEquals("Légende Quizzy", l6.title)
        assertEquals(1900L, l6.nextLevelXp)

        val l8 = GamificationCatalog.levelProgress(2500L)
        assertEquals(8, l8.level)
    }

    @Test
    fun computeUnlockedBadgesUnlocksAllMilestonesWithoutDuplicates() {
        val badges = GamificationCatalog.computeUnlockedBadges(
            existingBadges = listOf("first_quiz"),
            newQuizzesTaken = 5L,
            newTotalXp = 320L,
            percentage = 100,
            maxStreak = 5,
            difficulty = "Défi"
        )
        assertTrue(badges.containsAll(
            listOf("first_quiz", "quiz_5", "combo_3", "streak_5", "perfect_100", "challenger", "scholar_300")
        ))
        assertEquals(badges.distinct().size, badges.size)
    }

    @Test
    fun challengerBadgeRequiresAtLeast60PercentInDefiMode() {
        val failedDefi = GamificationCatalog.computeUnlockedBadges(
            existingBadges = emptyList(),
            newQuizzesTaken = 1L,
            newTotalXp = 50L,
            percentage = 40,
            maxStreak = 2,
            difficulty = "Défi"
        )
        assertFalse(failedDefi.contains("challenger"))

        val passedDefi = GamificationCatalog.computeUnlockedBadges(
            existingBadges = emptyList(),
            newQuizzesTaken = 1L,
            newTotalXp = 90L,
            percentage = 60,
            maxStreak = 2,
            difficulty = "Défi"
        )
        assertTrue(passedDefi.contains("challenger"))
    }

    @Test
    fun pinHasherProducesValidSha256AndValidInviteCodes() {
        val hash1 = PinHasher.hashPin("1234")
        val hash2 = PinHasher.hashPin(" 1234 ")
        val hashOther = PinHasher.hashPin("4321")

        assertEquals(64, hash1.length)
        assertTrue(hash1.matches(Regex("^[0-9a-f]{64}$")))
        assertEquals(hash1, hash2)
        assertNotEquals(hash1, hashOther)

        repeat(20) {
            val code = PinHasher.generateInviteCode()
            assertEquals(6, code.length)
            assertTrue("Invite code '$code' must match Firestore rule", code.matches(Regex("^[A-Z0-9]{6}$")))
        }
    }

    @Test
    fun schoolCatalogSuggestedAgesStayWithinFirestoreAllowedRange() {
        SchoolCatalog.gradeLevels.forEach { grade ->
            val age = SchoolCatalog.suggestedAgeForGrade(grade)
            assertTrue("Age $age for $grade must be within 5..18", age in 5..18)
        }
    }

    @Test
    fun fileFormattingHelpersProduceExpectedFrenchLabels() {
        assertEquals("", formatFileSize(0L))
        assertEquals("512 o", formatFileSize(512L))
        assertEquals("39 Ko", formatFileSize(39 * 1024L))
        assertEquals("PDF", documentFormatLabel("Cours_Maths.pdf", "application/pdf"))
        assertEquals("DOCX", documentFormatLabel("Lecon.docx", "application/octet-stream"))
        assertEquals("PDF", documentFormatLabel("sans_extension", "application/pdf"))
    }

    @Test
    fun sanitizeNeutralExplanationStripsBravoAndPraiseOpeners() {
        assertEquals(
            "Le numérateur est le nombre du haut.",
            com.example.model.QuizFeedbackHelper.sanitizeNeutralExplanation("Bravo ! Le numérateur est le nombre du haut.")
        )
        assertEquals(
            "La Révolution française a débuté en 1789.",
            com.example.model.QuizFeedbackHelper.sanitizeNeutralExplanation(
                "🎉 Bravo ! Bien joué ! Tu as trouvé la bonne réponse ! la Révolution française a débuté en 1789."
            )
        )
        assertEquals(
            "2 x 6 est égal à 12.",
            com.example.model.QuizFeedbackHelper.sanitizeNeutralExplanation("Super, c'est exact : 2 x 6 est égal à 12.")
        )
        assertEquals(
            "",
            com.example.model.QuizFeedbackHelper.sanitizeNeutralExplanation("Bravo ! Félicitations !")
        )
        assertEquals(
            "Superficie : on la calcule en multipliant longueur par largeur.",
            com.example.model.QuizFeedbackHelper.sanitizeNeutralExplanation(
                "Superficie : on la calcule en multipliant longueur par largeur."
            )
        )
        assertEquals(
            "Exactitude : ce mot désigne la précision.",
            com.example.model.QuizFeedbackHelper.sanitizeNeutralExplanation(
                "Exactitude : ce mot désigne la précision."
            )
        )
    }

    @Test
    fun quizAnswerEvaluatorMatchesFillBlankWithCaseAccentAndPunctuationTolerance() {
        val acceptedEnglish = listOf("sofa", "couch")
        assertTrue(com.example.model.QuizAnswerEvaluator.matchesFillBlank("sofa", acceptedEnglish))
        assertTrue(com.example.model.QuizAnswerEvaluator.matchesFillBlank("  Sofa! ", acceptedEnglish))
        assertTrue(com.example.model.QuizAnswerEvaluator.matchesFillBlank("COUCH.", acceptedEnglish))
        assertFalse(com.example.model.QuizAnswerEvaluator.matchesFillBlank("chair", acceptedEnglish))
        assertFalse(com.example.model.QuizAnswerEvaluator.matchesFillBlank("   ", acceptedEnglish))

        val acceptedFrench = listOf("étions", "mangé")
        assertTrue(com.example.model.QuizAnswerEvaluator.matchesFillBlank("etions", acceptedFrench))
        assertTrue(com.example.model.QuizAnswerEvaluator.matchesFillBlank("Mange", acceptedFrench))
        assertFalse(com.example.model.QuizAnswerEvaluator.matchesFillBlank("sommes", acceptedFrench))
    }

    @Test
    fun schoolCatalogNormalizesSubjectsAndDocumentTypesAccurately() {
        assertEquals("Mathématiques", SchoolCatalog.normalizeSubject("maths"))
        assertEquals("Français", SchoolCatalog.normalizeSubject("Conjugaison et grammaire"))
        assertEquals("Anglais", SchoolCatalog.normalizeSubject("English vocabulary"))
        assertEquals("Histoire-Géo", SchoolCatalog.normalizeSubject("Histoire"))
        assertEquals("Autre", SchoolCatalog.normalizeSubject("Auto"))

        assertEquals("Cours", SchoolCatalog.normalizeDocumentType("Cours"))
        assertEquals("Exercice", SchoolCatalog.normalizeDocumentType("exercice d'anglais"))
        assertEquals("Devoir", SchoolCatalog.normalizeDocumentType("Devoir maison"))
        assertEquals("Devoir", SchoolCatalog.normalizeDocumentType("Contrôle de maths"))
    }

    @Test
    fun skillTrackerHelperGraduatesMasteredSkillsAndRecordsNewGaps() {
        val (acquired, weak) = com.example.model.SkillTrackerHelper.mergeChildSkills(
            existingAcquired = listOf("Addition de fractions"),
            existingWeak = listOf("Verbes conjugués", "Vocabulaire du salon"),
            newlyAcquired = listOf("Verbes conjugués", "Multiplications"),
            newlyWeak = listOf("Accord du participe passé")
        )
        assertTrue(acquired.contains("Verbes conjugués"))
        assertTrue(acquired.contains("Multiplications"))
        assertTrue(acquired.contains("Addition de fractions"))
        assertFalse(weak.contains("Verbes conjugués"))
        assertTrue(weak.contains("Vocabulaire du salon"))
        assertTrue(weak.contains("Accord du participe passé"))
    }

    @Test
    fun safeGuidingHintBlocksLeakedAnswersAndKeepsSocraticHints() {
        val socratic = "Relis bien toute la phrase : il n'y a pas qu'un seul verbe conjugué !"
        val kept = com.example.model.QuizFeedbackHelper.safeGuidingHint(
            rawHint = socratic,
            forbiddenAnswers = listOf("mange et court"),
            questionIndex = 0
        )
        assertEquals(socratic, kept)

        val leaked = "En anglais, le mot pour canapé est sofa dans le salon."
        val filtered = com.example.model.QuizFeedbackHelper.safeGuidingHint(
            rawHint = leaked,
            forbiddenAnswers = listOf("sofa", "couch"),
            questionIndex = 0
        )
        assertFalse("Hint must never leak the exact answer", filtered.contains("sofa", ignoreCase = true))
        assertTrue(filtered.isNotBlank())

        // Ultra-subtle filter: must also block giveaways like starting letter, letter count, word similarity, rhymes, or hidden count
        val letterGiveaway = com.example.model.QuizFeedbackHelper.safeGuidingHint(
            rawHint = "Ce mot anglais commence par S et a quatre lettres.",
            forbiddenAnswers = listOf("sofa"),
            questionIndex = 0,
            isSecondAttempt = true
        )
        assertFalse(letterGiveaway.contains("commence par", ignoreCase = true))
        assertFalse(letterGiveaway.contains("quatre lettres", ignoreCase = true))

        val similarityGiveaway = com.example.model.QuizFeedbackHelper.safeGuidingHint(
            rawHint = "Pense à un mot court très proche du français.",
            forbiddenAnswers = listOf("sofa"),
            questionIndex = 1
        )
        assertFalse(similarityGiveaway.contains("proche", ignoreCase = true))

        val countGiveaway = com.example.model.QuizFeedbackHelper.safeGuidingHint(
            rawHint = "Attention, il y a 2 verbes conjugués dans cette phrase.",
            forbiddenAnswers = listOf("saute et attrape"),
            questionIndex = 2
        )
        assertFalse(countGiveaway.contains("il y a 2", ignoreCase = true))

        val rhymeGiveaway = com.example.model.QuizFeedbackHelper.safeGuidingHint(
            rawHint = "Ce mot rime avec moka.",
            forbiddenAnswers = listOf("sofa"),
            questionIndex = 3
        )
        assertFalse(rhymeGiveaway.contains("rime", ignoreCase = true))
    }

    @Test
    fun dragMatchAndDragOrderEvaluatorsAndDeterministicShuffleWorkProperly() {
        val pairs = listOf(
            com.example.model.QuizMatchPair("Canapé", "Sofa"),
            com.example.model.QuizMatchPair("Chat", "Cat"),
            com.example.model.QuizMatchPair("Maison", "House")
        )
        assertTrue(
            com.example.model.QuizAnswerEvaluator.matchesDragPairs(
                pairs,
                mapOf(0 to "sofa", 1 to "CAT", 2 to "House")
            )
        )
        assertFalse(
            com.example.model.QuizAnswerEvaluator.matchesDragPairs(
                pairs,
                mapOf(0 to "Cat", 1 to "Sofa", 2 to "House")
            )
        )
        assertFalse(
            com.example.model.QuizAnswerEvaluator.matchesDragPairs(
                pairs,
                mapOf(0 to "Sofa", 1 to "Cat")
            )
        )

        val expectedOrder = listOf("Bouche", "Œsophage", "Estomac", "Intestin")
        assertTrue(
            com.example.model.QuizAnswerEvaluator.matchesDragOrder(
                expectedOrder,
                listOf("bouche", "œsophage", "Estomac", "intestin")
            )
        )
        assertFalse(
            com.example.model.QuizAnswerEvaluator.matchesDragOrder(
                expectedOrder,
                listOf("Estomac", "Bouche", "Œsophage", "Intestin")
            )
        )

        val shuffled = com.example.model.QuizAnswerEvaluator.shuffledDeterministically(expectedOrder, seed = 42)
        assertEquals(expectedOrder.toSet(), shuffled.toSet())
        assertTrue("Drag order bank should not start already solved", shuffled != expectedOrder)

        assertEquals(
            com.example.model.QuizQuestion.ILLUS_MATH,
            com.example.model.QuizAnswerEvaluator.resolveIllustrationKey("", "Mathématiques", "Calcule 3/4")
        )
        assertEquals(
            com.example.model.QuizQuestion.ILLUS_ENGLISH,
            com.example.model.QuizAnswerEvaluator.resolveIllustrationKey("english", "Autre", "Translate")
        )
        assertEquals(
            com.example.model.QuizQuestion.ILLUS_SCIENCE,
            com.example.model.QuizAnswerEvaluator.resolveIllustrationKey("", "Sciences", "La digestion")
        )
        assertEquals(
            com.example.model.QuizQuestion.ILLUS_HISTORY,
            com.example.model.QuizAnswerEvaluator.resolveIllustrationKey("", "Histoire-Géo", "1789")
        )
    }

    @Test
    fun schoolCatalogNormalizesAndFormatsDifficultyLabels() {
        assertEquals("Facile", SchoolCatalog.normalizeDifficulty("facile"))
        assertEquals("Moyen", SchoolCatalog.normalizeDifficulty("MOYEN"))
        assertEquals("Défi", SchoolCatalog.normalizeDifficulty("Difficile"))
        assertEquals("Défi", SchoolCatalog.normalizeDifficulty("difficile ×1,5"))
        assertEquals("Défi", SchoolCatalog.normalizeDifficulty("Défi"))
        assertEquals("Moyen", SchoolCatalog.normalizeDifficulty("inconnu"))

        assertEquals("Facile", SchoolCatalog.difficultyDisplayLabel("Facile"))
        assertEquals("Moyen", SchoolCatalog.difficultyDisplayLabel("Moyen"))
        assertEquals("Difficile", SchoolCatalog.difficultyDisplayLabel("Défi"))

        assertEquals("Facile ×1", SchoolCatalog.difficultyMultiplierLabel("Facile"))
        assertEquals("Moyen ×1.2", SchoolCatalog.difficultyMultiplierLabel("Moyen"))
        assertEquals("Difficile ×1.5", SchoolCatalog.difficultyMultiplierLabel("Défi"))
    }

    @Test
    fun weeklyReviewSchedulerDetectsSunday14hWindowComputesNextSundayAndMergesMissedQuestions() {
        fun makeCal(year: Int, month: Int, day: Int, hour: Int, minute: Int): java.util.Calendar {
            return java.util.Calendar.getInstance().apply {
                set(year, month, day, hour, minute, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
        }

        // 2026-11-21 is Saturday, 2026-11-22 is Sunday
        val saturday = makeCal(2026, java.util.Calendar.NOVEMBER, 21, 15, 0)
        val sundayBefore14 = makeCal(2026, java.util.Calendar.NOVEMBER, 22, 13, 59)
        val sundayAt14 = makeCal(2026, java.util.Calendar.NOVEMBER, 22, 14, 0)
        val sundayEvening = makeCal(2026, java.util.Calendar.NOVEMBER, 22, 18, 30)

        assertFalse(com.example.model.WeeklyReviewScheduler.isSundayReviewWindowOpen(saturday))
        assertFalse(com.example.model.WeeklyReviewScheduler.isSundayReviewWindowOpen(sundayBefore14))
        assertTrue(com.example.model.WeeklyReviewScheduler.isSundayReviewWindowOpen(sundayAt14))
        assertTrue(com.example.model.WeeklyReviewScheduler.isSundayReviewWindowOpen(sundayEvening))

        // Same ISO Monday-to-Sunday week for Saturday and Sunday
        val satWeekKey = com.example.model.WeeklyReviewScheduler.currentWeekKey(saturday)
        val sunWeekKey = com.example.model.WeeklyReviewScheduler.currentWeekKey(sundayAt14)
        assertEquals(satWeekKey, sunWeekKey)
        assertTrue(satWeekKey.matches(Regex("^\\d{4}-W\\d{2}$")))

        // Next Sunday 14:00 is always strictly in the future and lands on Sunday 14:00:00
        listOf(saturday, sundayBefore14, sundayAt14, sundayEvening).forEach { now ->
            val nextMillis = com.example.model.WeeklyReviewScheduler.nextSunday14hMillis(now)
            assertTrue("Next Sunday 14h must be strictly after now", nextMillis > now.timeInMillis)
            val targetCal = java.util.Calendar.getInstance().apply { timeInMillis = nextMillis }
            assertEquals(java.util.Calendar.SUNDAY, targetCal.get(java.util.Calendar.DAY_OF_WEEK))
            assertEquals(14, targetCal.get(java.util.Calendar.HOUR_OF_DAY))
            assertEquals(0, targetCal.get(java.util.Calendar.MINUTE))
        }

        // Countdown label switches cleanly
        assertTrue(com.example.model.WeeklyReviewScheduler.countdownLabel(saturday).contains("dimanche à 14h"))
        assertTrue(com.example.model.WeeklyReviewScheduler.countdownLabel(sundayBefore14).contains("14h"))
        assertTrue(com.example.model.WeeklyReviewScheduler.countdownLabel(sundayAt14).contains("Disponible maintenant"))

        // Merge missed questions deduplicates case/accent-insensitively and enforces 40-item cap
        val existing = (1..38).map { i ->
            com.example.model.MissedQuestionRecord(
                subject = "Mathématiques",
                lessonId = "l$i",
                lessonTitle = "Leçon $i",
                quizId = "q$i",
                skillTag = "Notions $i",
                prompt = "Question numéro $i ?",
                weekKey = satWeekKey
            )
        }
        val incoming = listOf(
            // Duplicate of #1 (different case/accent)
            com.example.model.MissedQuestionRecord(
                subject = "mathematiques",
                lessonId = "l1",
                lessonTitle = "Leçon 1",
                quizId = "q1",
                skillTag = "Notions 1",
                prompt = "question numero 1 ?",
                weekKey = satWeekKey
            ),
            // 4 brand new items -> total 42 unique -> capped to last 40
            com.example.model.MissedQuestionRecord(subject = "Français", prompt = "Nouvelle 1", skillTag = "Verbes", weekKey = satWeekKey),
            com.example.model.MissedQuestionRecord(subject = "Français", prompt = "Nouvelle 2", skillTag = "Accords", weekKey = satWeekKey),
            com.example.model.MissedQuestionRecord(subject = "Anglais", prompt = "Nouvelle 3", skillTag = "Salon", weekKey = satWeekKey),
            com.example.model.MissedQuestionRecord(subject = "Sciences", prompt = "Nouvelle 4", skillTag = "Digestion", weekKey = satWeekKey)
        )
        val merged = com.example.model.WeeklyReviewScheduler.mergeWeeklyMissedQuestions(existing, incoming)
        assertEquals(40, merged.size)
        assertTrue(merged.any { it.prompt == "Nouvelle 4" })
        assertEquals(1, merged.count { it.prompt.equals("question numero 1 ?", ignoreCase = true) || it.prompt == "Question numéro 1 ?" })
    }
}
