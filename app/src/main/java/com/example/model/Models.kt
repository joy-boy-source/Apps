package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.security.MessageDigest
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class UserAccount(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val role: String = "PARENT",
    val familyId: String = "",
    val linkedChildId: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Family(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val familyName: String = "",
    val inviteCode: String = "",
    val parentPinHash: String = "",
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MissedQuestionRecord(
    val subject: String = "",
    val lessonId: String = "",
    val lessonTitle: String = "",
    val quizId: String = "",
    val skillTag: String = "",
    val prompt: String = "",
    val weekKey: String = ""
)

@Keep
@IgnoreExtraProperties
data class ChildProfile(
    @DocumentId val id: String = "",
    val familyId: String = "",
    val ownerId: String = "",
    val memberUids: List<String> = emptyList(),
    val linkedUid: String? = null,
    val name: String = "",
    val age: Long = 8L,
    val gradeLevel: String = "CE2",
    val avatarKey: String = "owl",
    val pinHash: String = "",
    val languageToneLevel: String = LanguageToneCatalog.AUTO,
    val lessonsCount: Long = 0L,
    val quizzesTaken: Long = 0L,
    val totalCorrect: Long = 0L,
    val totalQuestions: Long = 0L,
    val xp: Long = 0L,
    val bestStreak: Long = 0L,
    val badges: List<String> = emptyList(),
    val acquiredSkills: List<String> = emptyList(),
    val weakPoints: List<String> = emptyList(),
    val weeklyMissedQuestions: List<MissedQuestionRecord> = emptyList(),
    val lastSundayQuizWeekKey: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val successRatePercent: Int
        get() = if (totalQuestions > 0L) {
            ((totalCorrect * 100L) / totalQuestions).toInt().coerceIn(0, 100)
        } else {
            0
        }
}

@Keep
@IgnoreExtraProperties
data class AttachedDocument(
    val fileName: String = "",
    val mimeType: String = "",
    val sizeBytes: Long = 0L
)

@Keep
@IgnoreExtraProperties
data class Lesson(
    @DocumentId val id: String = "",
    val familyId: String = "",
    val ownerId: String = "",
    val memberUids: List<String> = emptyList(),
    val childId: String = "",
    val childName: String = "",
    val gradeLevel: String = "",
    val childAge: Long = 8L,
    val subject: String = "",
    val documentType: String = "Cours", // "Cours" | "Exercice" | "Devoir"
    val title: String = "",
    val sourceNotes: String = "",
    val summaryIntro: String = "",
    val keyPoints: List<String> = emptyList(),
    val simpleExplanation: String = "",
    val memoryTip: String = "",
    val acquiredSkills: List<String> = emptyList(),
    val weakPoints: List<String> = emptyList(),
    val evaluationSummary: String = "",
    val photoAttached: Boolean = false,
    val attachedFiles: List<AttachedDocument> = emptyList(),
    val quizCount: Long = 0L,
    val bestScorePercent: Long? = null,
    val attemptsCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class QuizMatchPair(
    val left: String = "",
    val right: String = ""
)

@Keep
@IgnoreExtraProperties
data class QuizImageOption(
    val label: String = "",
    val visualKey: String = ""
)

@Keep
@IgnoreExtraProperties
data class QuizQuestion(
    val prompt: String = "",
    val options: List<String> = emptyList(),
    val correctIndex: Int = 0,
    val correctIndices: List<Int> = emptyList(),
    val allowMultipleSelection: Boolean = false,
    val explanation: String = "",
    val questionType: String = TYPE_MCQ, // "MCQ" | "FILL_BLANK" | "DRAG_MATCH" | "DRAG_ORDER" | "IMAGE_CHOICE"
    val sentenceTemplate: String = "",
    val acceptedAnswers: List<String> = emptyList(),
    val matchingPairs: List<QuizMatchPair> = emptyList(),
    val orderedItems: List<String> = emptyList(),
    val imageOptions: List<QuizImageOption> = emptyList(),
    val illustrationKey: String = "", // "math" | "french" | "science" | "english" | "history"
    val illustrationCaption: String = "",
    val guidingHint: String = "",
    val secondHint: String = "",
    val skillTag: String = ""
) {
    @get:Exclude
    val isDragMatch: Boolean
        get() = questionType.equals(TYPE_DRAG_MATCH, ignoreCase = true) && matchingPairs.size >= 2

    @get:Exclude
    val isDragOrder: Boolean
        get() = questionType.equals(TYPE_DRAG_ORDER, ignoreCase = true) && orderedItems.size >= 2

    @get:Exclude
    val isImageChoice: Boolean
        get() = questionType.equals(TYPE_IMAGE_CHOICE, ignoreCase = true) && imageOptions.size >= 2

    @get:Exclude
    val isFillBlank: Boolean
        get() = !isDragMatch && !isDragOrder && !isImageChoice &&
            (questionType.equals(TYPE_FILL_BLANK, ignoreCase = true) ||
                (options.size < 2 && acceptedAnswers.isNotEmpty()))

    @get:Exclude
    val effectiveCorrectIndices: Set<Int>
        get() {
            val maxIndex = when {
                isImageChoice -> imageOptions.lastIndex
                else -> options.lastIndex
            }
            if (maxIndex < 0) return emptySet()
            val cleaned = correctIndices.filter { it in 0..maxIndex }.toSet()
            return if (cleaned.isNotEmpty()) cleaned else setOf(correctIndex.coerceIn(0, maxIndex))
        }

    @get:Exclude
    val isMultiSelect: Boolean
        get() {
            if (isDragMatch || isDragOrder || isFillBlank) return false
            val choiceCount = if (isImageChoice) imageOptions.size else options.size
            if (choiceCount < 2) return false
            return allowMultipleSelection || effectiveCorrectIndices.size > 1
        }

    @get:Exclude
    val forbiddenAnswersForHints: List<String>
        get() = when {
            isDragMatch -> matchingPairs.map { it.right }.filter { it.isNotBlank() }
            isDragOrder -> listOf(orderedItems.joinToString(" ")).filter { it.isNotBlank() }
            isImageChoice -> effectiveCorrectIndices.flatMap { idx ->
                listOfNotNull(
                    imageOptions.getOrNull(idx)?.label?.takeIf { it.isNotBlank() },
                    imageOptions.getOrNull(idx)?.visualKey?.takeIf { it.isNotBlank() }
                )
            }
            isFillBlank -> acceptedAnswers.filter { it.isNotBlank() }
            else -> effectiveCorrectIndices.mapNotNull { idx ->
                options.getOrNull(idx)?.takeIf { it.isNotBlank() }
            }
        }

    companion object {
        const val TYPE_MCQ = "MCQ"
        const val TYPE_FILL_BLANK = "FILL_BLANK"
        const val TYPE_DRAG_MATCH = "DRAG_MATCH"
        const val TYPE_DRAG_ORDER = "DRAG_ORDER"
        const val TYPE_IMAGE_CHOICE = "IMAGE_CHOICE"

        const val ILLUS_MATH = "math"
        const val ILLUS_FRENCH = "french"
        const val ILLUS_SCIENCE = "science"
        const val ILLUS_ENGLISH = "english"
        const val ILLUS_HISTORY = "history"
    }
}

@Keep
@IgnoreExtraProperties
data class Quiz(
    @DocumentId val id: String = "",
    val familyId: String = "",
    val ownerId: String = "",
    val memberUids: List<String> = emptyList(),
    val lessonId: String = "",
    val childId: String = "",
    val subject: String = "",
    val title: String = "",
    val difficulty: String = "Moyen",
    val questions: List<QuizQuestion> = emptyList(),
    val targetedGaps: List<String> = emptyList(),
    val weeklyReview: Boolean = false,
    val weekKey: String = "",
    val lastScorePercent: Long? = null,
    val attemptsCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    @get:Exclude
    val isAlreadyCompleted: Boolean
        get() = attemptsCount > 0L || lastScorePercent != null
}

@Keep
@IgnoreExtraProperties
data class QuizAttempt(
    @DocumentId val id: String = "",
    val familyId: String = "",
    val ownerId: String = "",
    val memberUids: List<String> = emptyList(),
    val childId: String = "",
    val childName: String = "",
    val lessonId: String = "",
    val lessonTitle: String = "",
    val quizId: String = "",
    val subject: String = "",
    val score: Long = 0L,
    val totalQuestions: Long = 0L,
    val percentage: Long = 0L,
    val xpEarned: Long = 0L,
    val maxStreak: Long = 0L,
    val masteredSkills: List<String> = emptyList(),
    val weakSkills: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FamilyMessage(
    @DocumentId val id: String = "",
    val familyId: String = "",
    val ownerId: String = "",
    val memberUids: List<String> = emptyList(),
    val channelId: String = CHANNEL_FAMILY_GROUP,
    val senderRole: String = "PARENT", // "PARENT" | "CHILD"
    val senderName: String = "",
    val senderUid: String = "",
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    companion object {
        const val CHANNEL_FAMILY_GROUP = "FAMILY"
    }
}

data class QuizBadge(
    val id: String,
    val title: String,
    val description: String
)

data class LevelProgress(
    val level: Int,
    val title: String,
    val totalXp: Long,
    val levelStartXp: Long,
    val nextLevelXp: Long
) {
    val progressFraction: Float
        get() {
            val span = (nextLevelXp - levelStartXp).coerceAtLeast(1L)
            return ((totalXp - levelStartXp).toFloat() / span.toFloat()).coerceIn(0f, 1f)
        }
}

object GamificationCatalog {
    val allBadges = listOf(
        QuizBadge("first_quiz", "Premier Pas", "Terminer son premier quiz"),
        QuizBadge("combo_3", "En Feu !", "Enchaîner 3 bonnes réponses d'affilée"),
        QuizBadge("perfect_100", "Sans Faute", "Obtenir 100% sur un quiz"),
        QuizBadge("challenger", "Courageux", "Réussir un quiz Défi (≥ 60%)"),
        QuizBadge("quiz_5", "Habitué", "Compléter 5 quiz"),
        QuizBadge("streak_5", "Série Imbattable", "Enchaîner 5 bonnes réponses d'affilée"),
        QuizBadge("scholar_300", "Savant en Herbe", "Atteindre 300 XP")
    )

    fun badgeById(id: String): QuizBadge? = allBadges.firstOrNull { it.id == id }

    fun difficultyMultiplier(difficulty: String): Float = when (difficulty) {
        "Défi" -> 1.5f
        "Moyen" -> 1.2f
        else -> 1.0f
    }

    /**
     * Exact base XP per question (divisible by 4 so progressive halving stays exact:
     * 1st try = 100%, 2nd try after 1 hint = 50%, 3rd try after 2 hints = 25%, 4th+ try = 0 XP).
     */
    fun baseQuestionXp(difficulty: String): Int = when (difficulty) {
        "Défi" -> 32
        "Moyen" -> 24
        else -> 20
    }

    /**
     * Computes XP earned for solving a single question depending on how many wrong attempts
     * occurred before finding the right answer:
     * - 0 wrong attempts (1st try): 100% of question XP
     * - 1 wrong attempt (2nd try, 1 hint shown): 50% of question XP
     * - 2 wrong attempts (3rd try, 2 hints shown): 25% of question XP (half of 2nd try)
     * - 3+ wrong attempts (4th+ try): 0 XP (still allowed to keep trying and learn!)
     * - Replay of an already completed quiz: always 0 XP.
     */
    fun questionXpForAttempt(
        difficulty: String,
        wrongAttemptsBeforeSolve: Int,
        isReplay: Boolean = false
    ): Int {
        if (isReplay) return 0
        val base = baseQuestionXp(difficulty)
        return when (wrongAttemptsBeforeSolve.coerceAtLeast(0)) {
            0 -> base
            1 -> base / 2
            2 -> (base / 2) / 2
            else -> 0
        }
    }

    fun calculateQuizXp(
        score: Int,
        totalQuestions: Int,
        maxStreak: Int,
        difficulty: String,
        isReplay: Boolean = false
    ): Long {
        if (isReplay) return 0L
        val base = score * 15.0 * difficultyMultiplier(difficulty)
        val streakBonus = maxStreak * 5L
        val completionBonus = 10L
        val perfectBonus = if (totalQuestions > 0 && score == totalQuestions) 30L else 0L
        return (base.roundToInt().toLong() + streakBonus + completionBonus + perfectBonus).coerceAtLeast(10L)
    }

    fun starsForPercentage(percentage: Int): Int = when {
        percentage >= 80 -> 3
        percentage >= 50 -> 2
        else -> 1
    }

    fun levelProgress(xp: Long): LevelProgress {
        val safeXp = xp.coerceAtLeast(0L)
        return when {
            safeXp < 100L -> LevelProgress(1, "Apprenti Curieux", safeXp, 0L, 100L)
            safeXp < 250L -> LevelProgress(2, "Explorateur Malin", safeXp, 100L, 250L)
            safeXp < 500L -> LevelProgress(3, "As des Révisions", safeXp, 250L, 500L)
            safeXp < 900L -> LevelProgress(4, "Maître du Savoir", safeXp, 500L, 900L)
            safeXp < 1400L -> LevelProgress(5, "Champion Quizzy", safeXp, 900L, 1400L)
            else -> {
                val extraLevels = ((safeXp - 1400L) / 500L).toInt()
                val level = 6 + extraLevels
                val start = 1400L + extraLevels * 500L
                LevelProgress(level, "Légende Quizzy", safeXp, start, start + 500L)
            }
        }
    }

    fun computeUnlockedBadges(
        existingBadges: List<String>,
        newQuizzesTaken: Long,
        newTotalXp: Long,
        percentage: Int,
        maxStreak: Int,
        difficulty: String
    ): List<String> {
        val unlocked = existingBadges.toMutableSet()
        if (newQuizzesTaken >= 1L) unlocked.add("first_quiz")
        if (newQuizzesTaken >= 5L) unlocked.add("quiz_5")
        if (maxStreak >= 3) unlocked.add("combo_3")
        if (maxStreak >= 5) unlocked.add("streak_5")
        if (percentage == 100) unlocked.add("perfect_100")
        if (difficulty == "Défi" && percentage >= 60) unlocked.add("challenger")
        if (newTotalXp >= 300L) unlocked.add("scholar_300")
        return unlocked.toList()
    }
}

object SchoolCatalog {
    const val AUTO_SUBJECT = "Auto"

    val documentTypes = listOf("Cours", "Exercice", "Devoir")

    val gradeLevels = listOf(
        "CP", "CE1", "CE2", "CM1", "CM2",
        "6ème", "5ème", "4ème", "3ème",
        "2nde", "1ère", "Terminale"
    )

    val subjects = listOf(
        "Mathématiques",
        "Français",
        "Histoire-Géo",
        "Sciences",
        "Anglais",
        "Physique-Chimie",
        "SVT",
        "Autre"
    )

    val avatars = listOf(
        "owl" to "Hibou",
        "astronaut" to "Astronaute",
        "fox" to "Renard",
        "rocket" to "Fusée",
        "palette" to "Artiste"
    )

    val difficulties = listOf("Facile", "Moyen", "Défi")

    fun normalizeDifficulty(raw: String?): String {
        val clean = QuizAnswerEvaluator.stripAccents(raw.orEmpty().trim().lowercase())
        return when {
            clean.contains("facile") -> "Facile"
            clean.contains("difficile") || clean.contains("defi") -> "Défi"
            else -> "Moyen"
        }
    }

    fun difficultyDisplayLabel(difficulty: String): String = when (normalizeDifficulty(difficulty)) {
        "Facile" -> "Facile"
        "Défi" -> "Difficile"
        else -> "Moyen"
    }

    fun difficultyMultiplierLabel(difficulty: String): String = when (normalizeDifficulty(difficulty)) {
        "Facile" -> "Facile ×1"
        "Défi" -> "Difficile ×1.5"
        else -> "Moyen ×1.2"
    }

    fun normalizeDocumentType(raw: String?): String {
        val clean = raw?.trim()?.lowercase().orEmpty()
        return when {
            clean.contains("exercice") || clean.contains("exo") -> "Exercice"
            clean.contains("devoir") || clean.contains("contrôle") || clean.contains("controle") || clean.contains("eval") -> "Devoir"
            else -> "Cours"
        }
    }

    fun normalizeSubject(raw: String?, fallback: String = "Autre"): String {
        val clean = QuizAnswerEvaluator.stripAccents(raw.orEmpty().trim().lowercase())
        if (clean.isBlank() || clean == "auto") {
            return if (fallback in subjects) fallback else "Autre"
        }
        subjects.firstOrNull {
            QuizAnswerEvaluator.stripAccents(it.lowercase()) == clean
        }?.let { return it }

        return when {
            clean.contains("math") || clean.contains("geometrie") || clean.contains("calcul") || clean.contains("algebre") -> "Mathématiques"
            clean.contains("franc") || clean.contains("grammaire") || clean.contains("conjug") || clean.contains("orthograph") || clean.contains("lecture") || clean.contains("dictee") || clean.contains("vocabulaire") -> "Français"
            clean.contains("anglais") || clean.contains("english") -> "Anglais"
            clean.contains("histoire") || clean.contains("geo") || clean.contains("emc") -> "Histoire-Géo"
            clean.contains("physique") || clean.contains("chimie") -> "Physique-Chimie"
            clean.contains("svt") || clean.contains("biologie") -> "SVT"
            clean.contains("science") || clean.contains("techno") -> "Sciences"
            else -> if (fallback in subjects) fallback else "Autre"
        }
    }

    fun suggestedAgeForGrade(grade: String): Int = when (grade) {
        "CP" -> 6
        "CE1" -> 7
        "CE2" -> 8
        "CM1" -> 9
        "CM2" -> 10
        "6ème" -> 11
        "5ème" -> 12
        "4ème" -> 13
        "3ème" -> 14
        "2nde" -> 15
        "1ère" -> 16
        "Terminale" -> 17
        else -> 10
    }
}

data class LanguageToneOption(
    val id: String,
    val shortLabel: String,
    val title: String,
    val description: String
)

object LanguageToneCatalog {
    const val AUTO = "AUTO"
    const val SIMPLIFIED = "SIMPLIFIED"
    const val STANDARD = "STANDARD"
    const val MATURE = "MATURE"
    const val ADVANCED = "ADVANCED"

    val options = listOf(
        LanguageToneOption(
            id = AUTO,
            shortLabel = "Âge (auto)",
            title = "Adapté à l'âge (par défaut)",
            description = "Calibre automatiquement le vocabulaire, les indices, les synthèses et les conseils selon l'âge et la classe."
        ),
        LanguageToneOption(
            id = SIMPLIFIED,
            shortLabel = "Très simple",
            title = "Très simple & rassurant",
            description = "Phrases courtes, mots du quotidien, exemples très concrets et ton doux."
        ),
        LanguageToneOption(
            id = STANDARD,
            shortLabel = "Clair",
            title = "Clair & pédagogique",
            description = "Vocabulaire accessible et structuré étape par étape (niveau primaire supérieur / début collège)."
        ),
        LanguageToneOption(
            id = MATURE,
            shortLabel = "Mature",
            title = "Mature & précis",
            description = "Ton posé sans formulations enfantines, vocabulaire scolaire précis et raisonnement structuré."
        ),
        LanguageToneOption(
            id = ADVANCED,
            shortLabel = "Soutenu",
            title = "Soutenu & exigeant",
            description = "Style rigoureux et analytique (niveau lycée / avancé) avec terminologie disciplinaire précise."
        )
    )

    val validIds: Set<String> = options.map { it.id }.toSet()

    fun normalize(raw: String?): String {
        val clean = raw?.trim()?.uppercase().orEmpty()
        return if (clean in validIds) clean else AUTO
    }

    fun optionFor(raw: String?): LanguageToneOption {
        val norm = normalize(raw)
        return options.firstOrNull { it.id == norm } ?: options.first()
    }

    fun isMatureRegister(childAge: Int, toneLevel: String?): Boolean = when (normalize(toneLevel)) {
        MATURE, ADVANCED -> true
        SIMPLIFIED, STANDARD -> false
        else -> childAge >= 13
    }

    fun buildAiToneInstruction(
        childAge: Int,
        gradeLevel: String,
        toneLevel: String? = AUTO
    ): String {
        return when (normalize(toneLevel)) {
            SIMPLIFIED ->
                "NIVEAU DE LANGAGE CHOISI PAR LE PARENT (TRÈS SIMPLE) : Emploie un vocabulaire très simple, des phrases courtes, des images concrètes du quotidien et un ton chaleureux et rassurant pour TOUS les textes (synthèses, explications, indices, conseils et bilans)."
            STANDARD ->
                "NIVEAU DE LANGAGE CHOISI PAR LE PARENT (CLAIR & PÉDAGOGIQUE) : Emploie un vocabulaire clair et accessible, explique chaque notion étape par étape avec des exemples concrets et un ton bienveillant pour TOUS les textes (synthèses, explications, indices, conseils et bilans)."
            MATURE ->
                "NIVEAU DE LANGAGE CHOISI PAR LE PARENT (MATURE & PRÉCIS) : Évite toute formulation enfantine ou bébé. Emploie un ton mature, direct, posé et respectueux, un vocabulaire scolaire précis et des explications structurées pour TOUS les textes (synthèses, explications, indices, conseils et bilans), tout en restant bienveillant et encourageant."
            ADVANCED ->
                "NIVEAU DE LANGAGE CHOISI PAR LE PARENT (SOUTENU & EXIGEANT) : Emploie un registre soutenu, rigoureux et analytique de niveau lycée/avancé, avec une terminologie disciplinaire exacte, une méthodologie exigeante et aucune formulation puérile dans TOUS les textes (synthèses, explications, indices, conseils et bilans), tout en restant constructif et encourageant."
            else -> when {
                childAge <= 7 ->
                    "ADAPTATION À L'ÂGE ($childAge ans, $gradeLevel) : Emploie des mots très simples de CP/CE1, des phrases très courtes, des exemples concrets du quotidien et un ton joyeux, doux et rassurant dans CHAQUE conseil, détail, indice et synthèse."
                childAge <= 9 ->
                    "ADAPTATION À L'ÂGE ($childAge ans, $gradeLevel) : Emploie des mots simples de primaire (CE2/CM1), des phrases claires, des exemples concrets et un ton bienveillant et motivant dans CHAQUE conseil, détail, indice et synthèse."
                childAge <= 11 ->
                    "ADAPTATION À L'ÂGE ($childAge ans, $gradeLevel) : Emploie un vocabulaire accessible de fin de primaire/6ème, des explications structurées étape par étape et un ton encourageant sans être bébé dans CHAQUE conseil, détail, indice et synthèse."
                childAge <= 14 ->
                    "ADAPTATION À L'ÂGE ($childAge ans, $gradeLevel) : Emploie un ton clair et mature de collège, structure précisément les notions, définitions et méthodes sans langage enfantin dans CHAQUE conseil, détail, indice et synthèse."
                else ->
                    "ADAPTATION À L'ÂGE ($childAge ans, $gradeLevel) : Emploie un style mature, rigoureux et limpide de lycée, avec définitions exactes, raisonnement structuré et vocabulaire académique adapté dans CHAQUE conseil, détail, indice et synthèse."
            }
        }
    }
}

object PinHasher {
    fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(pin.trim().toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun generateInviteCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars.random() }.joinToString("")
    }
}

object QuizAnswerEvaluator {
    private val whitespaceRegex = Regex("\\s+")
    private val edgePunctuationRegex = Regex("^[\\s.,;:!?\"«»()\\[\\]']+|[\\s.,;:!?\"«»()\\[\\]']+$")
    private val diacriticsRegex = Regex("\\p{InCombiningDiacriticalMarks}+")

    fun stripAccents(input: String): String {
        val normalized = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD)
        return diacriticsRegex.replace(normalized, "")
    }

    fun normalizeAnswer(input: String): String {
        return input
            .trim()
            .replace('’', '\'')
            .replace('‘', '\'')
            .replace('–', '-')
            .replace('—', '-')
            .replace(whitespaceRegex, " ")
            .replace(edgePunctuationRegex, "")
            .lowercase()
    }

    fun matchesFillBlank(userInput: String, acceptedAnswers: List<String>): Boolean {
        val cleanInput = normalizeAnswer(userInput)
        if (cleanInput.isEmpty() || acceptedAnswers.isEmpty()) return false

        val normalizedAccepted = acceptedAnswers
            .map { normalizeAnswer(it) }
            .filter { it.isNotEmpty() }
        if (cleanInput in normalizedAccepted) return true

        val accentlessInput = stripAccents(cleanInput)
        return normalizedAccepted.any { stripAccents(it) == accentlessInput }
    }

    fun matchesDragPairs(pairs: List<QuizMatchPair>, userAssignments: Map<Int, String>): Boolean {
        if (pairs.isEmpty() || userAssignments.size < pairs.size) return false
        return pairs.indices.all { idx ->
            val assigned = userAssignments[idx] ?: return@all false
            normalizeAnswer(assigned) == normalizeAnswer(pairs[idx].right)
        }
    }

    fun matchesDragOrder(expectedOrder: List<String>, userOrder: List<String?>): Boolean {
        if (expectedOrder.isEmpty() || userOrder.size != expectedOrder.size) return false
        return expectedOrder.indices.all { idx ->
            val candidate = userOrder[idx] ?: return@all false
            normalizeAnswer(candidate) == normalizeAnswer(expectedOrder[idx])
        }
    }

    /**
     * Produces a deterministic shuffle that is guaranteed not to match the original order
     * whenever there are at least 2 distinct items, so drag mini-games never start solved.
     */
    fun shuffledDeterministically(items: List<String>, seed: Int): List<String> {
        if (items.size < 2 || items.distinct().size < 2) return items
        val rng = java.util.Random(seed.toLong() * 31L + 7919L)
        val result = items.toMutableList()
        result.shuffle(rng)
        if (result == items) {
            val first = result.removeAt(0)
            result.add(first)
        }
        return result
    }

    fun resolveIllustrationKey(explicitKey: String, subject: String, prompt: String = ""): String {
        val cleanKey = explicitKey.trim().lowercase()
        val validKeys = setOf(
            QuizQuestion.ILLUS_MATH,
            QuizQuestion.ILLUS_FRENCH,
            QuizQuestion.ILLUS_SCIENCE,
            QuizQuestion.ILLUS_ENGLISH,
            QuizQuestion.ILLUS_HISTORY
        )
        if (cleanKey in validKeys) return cleanKey

        val combined = stripAccents("$subject $prompt".lowercase())
        return when {
            combined.contains("math") || combined.contains("fraction") || combined.contains("geometr") ||
                combined.contains("calcul") || combined.contains("multipli") || combined.contains("addition") ||
                combined.contains("nombre") -> QuizQuestion.ILLUS_MATH
            combined.contains("anglais") || combined.contains("english") || combined.contains("tradu") ->
                QuizQuestion.ILLUS_ENGLISH
            combined.contains("histoire") || combined.contains("geo") || combined.contains("carte") ||
                combined.contains("roi") || combined.contains("siecle") || combined.contains("chronolog") ->
                QuizQuestion.ILLUS_HISTORY
            combined.contains("science") || combined.contains("svt") || combined.contains("physique") ||
                combined.contains("chimie") || combined.contains("planete") || combined.contains("plante") ||
                combined.contains("digestion") || combined.contains("corps") || combined.contains("eau") ->
                QuizQuestion.ILLUS_SCIENCE
            else -> QuizQuestion.ILLUS_FRENCH
        }
    }
}

object SkillTrackerHelper {
    fun mergeChildSkills(
        existingAcquired: List<String>,
        existingWeak: List<String>,
        newlyAcquired: List<String>,
        newlyWeak: List<String>,
        maxPerList: Int = 40
    ): Pair<List<String>, List<String>> {
        val acquiredMap = linkedMapOf<String, String>()
        val weakMap = linkedMapOf<String, String>()

        fun keyOf(s: String): String = QuizAnswerEvaluator.stripAccents(s.trim().lowercase())

        existingAcquired.forEach { raw ->
            val clean = raw.trim().take(100)
            if (clean.isNotEmpty()) acquiredMap.putIfAbsent(keyOf(clean), clean)
        }
        existingWeak.forEach { raw ->
            val clean = raw.trim().take(100)
            if (clean.isNotEmpty()) weakMap.putIfAbsent(keyOf(clean), clean)
        }

        // Newly acquired skills graduate out of weakPoints
        newlyAcquired.forEach { raw ->
            val clean = raw.trim().take(100)
            if (clean.isNotEmpty()) {
                val key = keyOf(clean)
                weakMap.remove(key)
                acquiredMap[key] = clean
            }
        }

        // Newly detected gaps are added to weakPoints (and removed from acquired if regressed)
        newlyWeak.forEach { raw ->
            val clean = raw.trim().take(100)
            if (clean.isNotEmpty()) {
                val key = keyOf(clean)
                acquiredMap.remove(key)
                weakMap[key] = clean
            }
        }

        return acquiredMap.values.toList().takeLast(maxPerList) to
            weakMap.values.toList().takeLast(maxPerList)
    }
}

object QuizFeedbackHelper {
    private val congratulatoryPrefixRegex = Regex(
        pattern = """^(?:[\p{So}\p{Sk}\s]*(?:bravo|bien\s+jou[ée]|tr[èe]s\s+bien|super|g[ée]nial|parfait|excellent|exactement|exact|f[ée]licitations?|chapeau|magnifique|formidable|bien\s+vu|bonne\s+r[ée]ponse|c['’]est\s+(?:juste|exact|correct|gagn[ée]|bien\s+[çc]a)|tu\s+as\s+(?:bien\s+)?(?:trouv[ée]|r[ée]pondu|r[ée]ussi|raison)(?:\s+la\s+bonne\s+r[ée]ponse)?|tu\s+es\s+(?:un|une|tr[èe]s\s+)?(?:champion(?:ne)?|fort(?:e)?|au\s+top)|bien\s+r[ée]pondu|tout\s+[àa]\s+fait|en\s+effet)(?![\p{L}\p{N}_])[\p{So}\p{Sk}\s,;:!.\-—–]*)""",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    private val wrongAnswerMessages = listOf(
        "Ce n'est pas encore la bonne réponse, mais pas d'inquiétude ! Utilise cet indice pour chercher autrement.",
        "Presque ! Tu as eu faux cette fois-ci, mais tu peux trouver par toi-même avec cette petite piste.",
        "Oups, petite erreur ! C'est en cherchant qu'on apprend le mieux. Lis bien cet indice et réessaie !",
        "Pas tout à fait ! Prends le temps d'observer chaque détail avec cette aide bienveillante."
    )

    private val fallbackFirstHints = listOf(
        "Relis attentivement la consigne et repense à la règle principale vue dans ta leçon.",
        "Prends un court instant pour observer chaque élément de la question avant de te lancer.",
        "Demande-toi quelle méthode tu utilises d'habitude en classe pour ce type d'exercice."
    )

    private val fallbackSecondHints = listOf(
        "Avance étape par étape : vérifie chaque détail calmement et écarte d'abord ce qui ne colle pas avec le cours.",
        "Reprends le raisonnement depuis le début et compare soigneusement chaque piste avec ce que tu sais déjà."
    )

    /**
     * Blocks overly obvious "semblants de réponse" such as starting letters, exact letter/syllable counts,
     * word-similarity spoilers, rhymes, or revealing exact hidden counts.
     */
    private val giveawayPatternRegex = Regex(
        pattern = """(?:\b(?:commence|debute|se\s+termine|finit)\s+par\b|\b(?:premiere|derniere)\s+lettre\b|\b\d+\s+lettres?\b|\b(?:deux|trois|quatre|cinq|six|sept|huit|neuf|dix)\s+lettres?\b|\b\d+\s+syllabes?\b|\brime\s+avec\b|\bressemble\s+(?:au\s+mot|beaucoup\s+au|a\s+un\s+mot\s+francais|au\s+francais)\b|\bproche\s+(?:d['’]un\s+mot|du\s+mot|du\s+francais)\b|\bs['’]ecrit\s+avec\s+un\b|\bcontient\s+la\s+lettre\b|\bil\s+y\s+(?:en\s+)?a\s+(?:\d+|deux|trois|quatre|cinq)\b|\bla\s+reponse\s+est\b|\bc['’]est\s+le\s+mot\b)""",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    private val matureWrongAnswerMessages = listOf(
        "Ce n'est pas encore la réponse attendue. Appuie-toi sur cet indice méthodologique pour revoir ton raisonnement.",
        "Réponse inexacte pour cet essai : reprends la question calmement à l'aide de cette piste.",
        "Pas encore juste. Utilise cet indice pour analyser la consigne sous un autre angle et réessayer.",
        "Cette proposition ne convient pas encore. Prends un instant pour vérifier ta démarche avec cette indication."
    )

    fun friendlyWrongAnswerMessage(
        questionIndex: Int,
        childAge: Int = 10,
        languageToneLevel: String = LanguageToneCatalog.AUTO
    ): String {
        val pool = if (LanguageToneCatalog.isMatureRegister(childAge, languageToneLevel)) {
            matureWrongAnswerMessages
        } else {
            wrongAnswerMessages
        }
        val safeIdx = questionIndex.coerceAtLeast(0) % pool.size
        return pool[safeIdx]
    }

    /**
     * Strips leading congratulatory phrases ("Bravo !", "Bien joué !", "Super !", "Tu as trouvé !", etc.)
     * so explanations remain neutral and accurate when displayed after a wrong answer.
     */
    fun sanitizeNeutralExplanation(raw: String): String {
        var text = raw.trim()
        var guard = 0
        while (text.isNotEmpty() && guard < 5) {
            val match = congratulatoryPrefixRegex.find(text) ?: break
            if (match.range.first != 0 || match.value.isEmpty()) break
            text = text.substring(match.range.last + 1).trimStart()
            guard++
        }
        if (text.isEmpty()) return ""
        return text.replaceFirstChar { ch ->
            if (ch.isLowerCase()) ch.titlecase() else ch.toString()
        }
    }

    /**
     * Ensures a Socratic guiding hint is subtle and NEVER reveals the exact answer
     * or an obvious giveaway (starting letter, letter count, hidden count, rhyme, etc.).
     */
    fun safeGuidingHint(
        rawHint: String,
        forbiddenAnswers: List<String>,
        questionIndex: Int = 0,
        isSecondAttempt: Boolean = false
    ): String {
        val cleaned = sanitizeNeutralExplanation(rawHint)
        val fallbackList = if (isSecondAttempt) fallbackSecondHints else fallbackFirstHints
        val fallback = fallbackList[questionIndex.coerceAtLeast(0) % fallbackList.size]
        if (cleaned.isBlank()) return fallback

        val normalizedHint = QuizAnswerEvaluator.stripAccents(cleaned.lowercase())
        if (giveawayPatternRegex.containsMatchIn(normalizedHint)) {
            return fallback
        }
        val leaksAnswer = forbiddenAnswers.any { ans ->
            val normAns = QuizAnswerEvaluator.stripAccents(QuizAnswerEvaluator.normalizeAnswer(ans))
            if (normAns.length < 3) return@any false
            val boundaryRegex = Regex("(?<![\\p{L}\\p{N}])${Regex.escape(normAns)}(?![\\p{L}\\p{N}])")
            boundaryRegex.containsMatchIn(normalizedHint)
        }
        return if (leaksAnswer) fallback else cleaned
    }
}

object WeeklyReviewScheduler {
    const val WEEKLY_REVIEW_LESSON_ID = "weekly_sunday_review"
    const val SUNDAY_REVIEW_HOUR = 14
    const val SUNDAY_REVIEW_QUESTION_COUNT = 10

    /**
     * Returns an ISO-like Monday-to-Sunday week identifier (e.g. "2026-W47")
     * so mistakes collected Monday through Sunday belong to the same Sunday 14:00 review window.
     */
    fun currentWeekKey(now: Calendar = Calendar.getInstance()): String {
        val cal = (now.clone() as Calendar).apply {
            firstDayOfWeek = Calendar.MONDAY
            minimalDaysInFirstWeek = 4
        }
        val year = cal.get(Calendar.YEAR)
        val week = cal.get(Calendar.WEEK_OF_YEAR)
        return String.format(Locale.US, "%04d-W%02d", year, week)
    }

    /**
     * True every Sunday starting at 14:00 (2:00 PM) local time.
     */
    fun isSundayReviewWindowOpen(now: Calendar = Calendar.getInstance()): Boolean {
        return now.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY &&
            now.get(Calendar.HOUR_OF_DAY) >= SUNDAY_REVIEW_HOUR
    }

    /**
     * Computes epoch millis for the next upcoming Sunday at 14:00:00.
     * If `now` is already Sunday >= 14:00, schedules the following Sunday at 14:00.
     */
    fun nextSunday14hMillis(now: Calendar = Calendar.getInstance()): Long {
        val target = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, SUNDAY_REVIEW_HOUR)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dayOfWeek = target.get(Calendar.DAY_OF_WEEK)
        val daysUntilSunday = (Calendar.SUNDAY - dayOfWeek + 7) % 7
        target.add(Calendar.DAY_OF_YEAR, daysUntilSunday)
        if (target.timeInMillis <= now.timeInMillis) {
            target.add(Calendar.DAY_OF_YEAR, 7)
        }
        return target.timeInMillis
    }

    fun countdownLabel(now: Calendar = Calendar.getInstance()): String {
        if (isSundayReviewWindowOpen(now)) {
            return "Disponible maintenant ! (Dimanche 14h)"
        }
        val diffMillis = (nextSunday14hMillis(now) - now.timeInMillis).coerceAtLeast(0L)
        val totalMinutes = diffMillis / 60_000L
        val days = totalMinutes / (60L * 24L)
        val hours = (totalMinutes % (60L * 24L)) / 60L
        val minutes = totalMinutes % 60L
        val isSunday = now.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
        return when {
            days >= 1L -> "Rendez-vous dimanche à 14h (dans ${days}j ${hours}h)"
            !isSunday -> "Demain dimanche à 14h (dans ${hours}h ${minutes}min)"
            hours >= 1L -> "Aujourd'hui à 14h (dans ${hours}h ${minutes}min)"
            else -> "Bientôt disponible à 14h (dans ${minutes.coerceAtLeast(1L)} min)"
        }
    }

    fun mergeWeeklyMissedQuestions(
        existing: List<MissedQuestionRecord>,
        newlyMissed: List<MissedQuestionRecord>,
        maxItems: Int = 40
    ): List<MissedQuestionRecord> {
        val merged = linkedMapOf<String, MissedQuestionRecord>()
        fun keyOf(rec: MissedQuestionRecord): String {
            val subjectKey = QuizAnswerEvaluator.stripAccents(rec.subject.trim().lowercase())
            val promptKey = QuizAnswerEvaluator.stripAccents(rec.prompt.trim().lowercase()).take(80)
            return "$subjectKey|$promptKey"
        }
        existing.forEach { rec ->
            if (rec.prompt.isNotBlank() || rec.skillTag.isNotBlank()) {
                merged[keyOf(rec)] = rec
            }
        }
        newlyMissed.forEach { rec ->
            if (rec.prompt.isNotBlank() || rec.skillTag.isNotBlank()) {
                val sanitized = rec.copy(
                    subject = rec.subject.trim().take(40),
                    lessonId = rec.lessonId.trim().take(128),
                    lessonTitle = rec.lessonTitle.trim().take(120),
                    quizId = rec.quizId.trim().take(128),
                    skillTag = rec.skillTag.trim().take(100),
                    prompt = rec.prompt.trim().take(240),
                    weekKey = rec.weekKey.trim().take(20)
                )
                val key = keyOf(sanitized)
                merged.remove(key)
                merged[key] = sanitized
            }
        }
        return merged.values.toList().takeLast(maxItems)
    }
}

