package com.example.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

data class ExerciseItem(
    val id: String = UUID.randomUUID().toString(),
    val numberLabel: String = "1",
    val prompt: String = "",
    val inputType: String = TYPE_TEXT_INPUT, // "TEXT_INPUT" or "MCQ"
    val options: List<String> = emptyList(),
    val allowMultipleAnswers: Boolean = false,
    val expectedAnswerGuide: String = "",
    val studentAnswer: String = "",
    val verdict: String = VERDICT_NONE, // "", "CORRECT", "PARTIAL", "INCORRECT"
    val aiExplanation: String = "",
    val gradingError: String? = null
) {
    val isMcq: Boolean
        get() = inputType.equals(TYPE_MCQ, ignoreCase = true) && options.size >= 2

    val isMultiSelectMcq: Boolean
        get() = isMcq && (
            allowMultipleAnswers ||
                detectsMultipleAnswers(prompt, "", expectedAnswerGuide, options)
            )

    val isGraded: Boolean
        get() = verdict == VERDICT_CORRECT || verdict == VERDICT_PARTIAL || verdict == VERDICT_INCORRECT

    companion object {
        const val TYPE_TEXT_INPUT = "TEXT_INPUT"
        const val TYPE_MCQ = "MCQ"
        const val MULTI_ANSWER_SEPARATOR = " ; "

        const val VERDICT_NONE = ""
        const val VERDICT_CORRECT = "CORRECT"
        const val VERDICT_PARTIAL = "PARTIAL"
        const val VERDICT_INCORRECT = "INCORRECT"

        private val multiChoiceRegex = Regex(
            pattern = """(?:\bplusieurs\s+r[ée]ponses?\b|\bplus\s+d['’]une\s+r[ée]ponse\b|\bles\s+bonnes\s+r[ée]ponses\b|\btoutes\s+les\s+(?:bonnes\s+)?r[ée]ponses\b|\bcoche(?:r|z)?\s+(?:les|toutes)\b|\bentoure(?:r|z)?\s+(?:les|toutes)\b|\bs[ée]lectionne(?:r|z)?\s+(?:les|toutes)\b|\bchoisis(?:sez)?\s+(?:les|toutes|deux|trois)\b|\bquel(?:le)?s?\s+sont\b|\blesquel(?:le)?s\b|\b(?:deux|2|trois|3)\s+(?:bonnes\s+)?r[ée]ponses\b|\bchoix\s+multiples?\s*\(plusieurs\b)""",
            options = setOf(RegexOption.IGNORE_CASE)
        )

        fun detectsMultipleAnswers(
            prompt: String,
            generalInstructions: String = "",
            expectedAnswerGuide: String = "",
            options: List<String> = emptyList()
        ): Boolean {
            if (options.size < 2) return false
            val combinedPrompt = "$prompt $generalInstructions"
            if (multiChoiceRegex.containsMatchIn(combinedPrompt)) return true

            if (expectedAnswerGuide.isNotBlank()) {
                val normGuide = QuizAnswerEvaluator.stripAccents(expectedAnswerGuide.lowercase())
                val matchedOptionsCount = options.count { opt ->
                    val normOpt = QuizAnswerEvaluator.stripAccents(opt.trim().lowercase())
                    normOpt.length >= 2 && normGuide.contains(normOpt)
                }
                if (matchedOptionsCount >= 2) return true
            }
            return false
        }

        fun encodeSelectedMcqOptions(selected: Set<String>, orderedOptions: List<String>): String {
            if (selected.isEmpty()) return ""
            val ordered = orderedOptions.filter { it in selected }
            val extras = selected.filter { it !in orderedOptions }
            return (ordered + extras).joinToString(MULTI_ANSWER_SEPARATOR)
        }

        fun decodeSelectedMcqOptions(studentAnswer: String, options: List<String>): Set<String> {
            val trimmed = studentAnswer.trim()
            if (trimmed.isEmpty()) return emptySet()
            if (trimmed in options) return setOf(trimmed)
            val splitParts = trimmed
                .split(MULTI_ANSWER_SEPARATOR)
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            val matched = splitParts.filter { part -> options.any { it == part } }.toSet()
            if (matched.isNotEmpty()) return matched
            // Fallback: match case-insensitively if edited
            val normParts = splitParts.map { it.lowercase() }.toSet()
            return options.filter { it.lowercase() in normParts }.toSet()
        }

        fun normalizeVerdict(raw: String): String = when (raw.trim().uppercase()) {
            "CORRECT", "CORRECTE", "JUSTE", "VRAI", "EXACT" -> VERDICT_CORRECT
            "PARTIAL", "PARTIALLY_CORRECT", "PARTIELLEMENT_CORRECTE", "PARTIEL" -> VERDICT_PARTIAL
            "INCORRECT", "FAUSSE", "FAUX", "WRONG" -> VERDICT_INCORRECT
            else -> VERDICT_INCORRECT
        }
    }
}

data class ExerciseFinalEvaluation(
    val scoreOutOf20: Double = 0.0,
    val overallAssessment: String = "",
    val strengths: List<String> = emptyList(),
    val improvementAxes: List<String> = emptyList(),
    val practicalAdvice: String = "",
    val encouragement: String = "",
    val evaluatedAt: Long = System.currentTimeMillis()
) {
    val formattedScoreOutOf20: String
        get() {
            val clamped = clampScore(scoreOutOf20)
            return if (clamped % 1.0 == 0.0) {
                "${clamped.toInt()}/20"
            } else {
                String.format(Locale.FRANCE, "%.1f/20", clamped)
            }
        }

    val percentage: Int
        get() = ((clampScore(scoreOutOf20) / 20.0) * 100.0).roundToInt().coerceIn(0, 100)

    companion object {
        fun clampScore(raw: Double): Double {
            if (raw.isNaN() || raw.isInfinite()) return 0.0
            val clamped = raw.coerceIn(0.0, 20.0)
            return (clamped * 2.0).roundToInt() / 2.0
        }
    }
}

data class ExerciseSheet(
    val id: String = UUID.randomUUID().toString(),
    val childId: String = "",
    val subject: String = "Autre",
    val title: String = "",
    val generalInstructions: String = "",
    val items: List<ExerciseItem> = emptyList(),
    val sourceFileNames: List<String> = emptyList(),
    val finalEvaluation: ExerciseFinalEvaluation? = null,
    val finalEvaluationError: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val gradedCount: Int get() = items.count { it.isGraded }
    val answeredCount: Int get() = items.count { it.studentAnswer.isNotBlank() }
    val correctCount: Int get() = items.count { it.verdict == ExerciseItem.VERDICT_CORRECT }
    val partialCount: Int get() = items.count { it.verdict == ExerciseItem.VERDICT_PARTIAL }
    val incorrectCount: Int get() = items.count { it.verdict == ExerciseItem.VERDICT_INCORRECT }
}

data class ExtractedExerciseResult(
    val detectedSubject: String,
    val title: String,
    val generalInstructions: String,
    val items: List<ExerciseItem>
)

data class ExerciseGradingResult(
    val verdict: String, // "CORRECT" | "PARTIAL" | "INCORRECT"
    val explanation: String
)

object ExerciseJsonCodec {

    fun encodeItems(items: List<ExerciseItem>): String {
        val arr = JSONArray()
        items.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("numberLabel", item.numberLabel)
            obj.put("prompt", item.prompt)
            obj.put("inputType", item.inputType)
            obj.put("options", JSONArray(item.options))
            obj.put("allowMultipleAnswers", item.allowMultipleAnswers)
            obj.put("expectedAnswerGuide", item.expectedAnswerGuide)
            obj.put("studentAnswer", item.studentAnswer)
            obj.put("verdict", item.verdict)
            obj.put("aiExplanation", item.aiExplanation)
            if (item.gradingError != null) {
                obj.put("gradingError", item.gradingError)
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    fun decodeItems(json: String): List<ExerciseItem> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val optsArr = obj.optJSONArray("options")
                    val opts = buildList {
                        if (optsArr != null) {
                            for (j in 0 until optsArr.length()) {
                                val v = optsArr.optString(j).trim()
                                if (v.isNotEmpty()) add(v)
                            }
                        }
                    }
                    val rawType = obj.optString("inputType", ExerciseItem.TYPE_TEXT_INPUT)
                    val safeType = if (rawType.equals(ExerciseItem.TYPE_MCQ, ignoreCase = true) && opts.size >= 2) {
                        ExerciseItem.TYPE_MCQ
                    } else {
                        ExerciseItem.TYPE_TEXT_INPUT
                    }
                    val err = if (obj.has("gradingError") && !obj.isNull("gradingError")) {
                        obj.optString("gradingError").takeIf { it.isNotBlank() }
                    } else {
                        null
                    }
                    add(
                        ExerciseItem(
                            id = obj.optString("id").ifBlank { "item_${i + 1}" },
                            numberLabel = obj.optString("numberLabel").ifBlank { "${i + 1}" },
                            prompt = obj.optString("prompt"),
                            inputType = safeType,
                            options = opts,
                            allowMultipleAnswers = safeType == ExerciseItem.TYPE_MCQ &&
                                obj.optBoolean("allowMultipleAnswers", false),
                            expectedAnswerGuide = obj.optString("expectedAnswerGuide"),
                            studentAnswer = obj.optString("studentAnswer"),
                            verdict = obj.optString("verdict"),
                            aiExplanation = obj.optString("aiExplanation"),
                            gradingError = err
                        )
                    )
                }
            }
        }.getOrElse { emptyList() }
    }

    fun encodeFinalEvaluation(eval: ExerciseFinalEvaluation?): String? {
        if (eval == null) return null
        val obj = JSONObject()
        obj.put("scoreOutOf20", ExerciseFinalEvaluation.clampScore(eval.scoreOutOf20))
        obj.put("overallAssessment", eval.overallAssessment)
        obj.put("strengths", JSONArray(eval.strengths))
        obj.put("improvementAxes", JSONArray(eval.improvementAxes))
        obj.put("practicalAdvice", eval.practicalAdvice)
        obj.put("encouragement", eval.encouragement)
        obj.put("evaluatedAt", eval.evaluatedAt)
        return obj.toString()
    }

    fun decodeFinalEvaluation(json: String?): ExerciseFinalEvaluation? {
        if (json.isNullOrBlank()) return null
        return runCatching {
            val obj = JSONObject(json)
            val strengthsArr = obj.optJSONArray("strengths")
            val strengths = buildList {
                if (strengthsArr != null) {
                    for (i in 0 until strengthsArr.length()) {
                        val s = strengthsArr.optString(i).trim()
                        if (s.isNotEmpty()) add(s)
                    }
                }
            }
            val axesArr = obj.optJSONArray("improvementAxes")
            val axes = buildList {
                if (axesArr != null) {
                    for (i in 0 until axesArr.length()) {
                        val s = axesArr.optString(i).trim()
                        if (s.isNotEmpty()) add(s)
                    }
                }
            }
            ExerciseFinalEvaluation(
                scoreOutOf20 = ExerciseFinalEvaluation.clampScore(obj.optDouble("scoreOutOf20", 0.0)),
                overallAssessment = obj.optString("overallAssessment").trim(),
                strengths = strengths,
                improvementAxes = axes,
                practicalAdvice = obj.optString("practicalAdvice").trim(),
                encouragement = obj.optString("encouragement").trim(),
                evaluatedAt = obj.optLong("evaluatedAt", System.currentTimeMillis())
            )
        }.getOrNull()
    }

    fun encodeStringList(values: List<String>): String = JSONArray(values).toString()

    fun decodeStringList(json: String): List<String> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val v = arr.optString(i).trim()
                    if (v.isNotEmpty()) add(v)
                }
            }
        }.getOrElse { emptyList() }
    }
}
