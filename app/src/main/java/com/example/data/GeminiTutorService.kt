package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.util.Xml
import com.example.model.AttachedDocument
import com.example.model.Lesson
import com.example.model.MissedQuestionRecord
import com.example.model.QuizImageOption
import com.example.model.QuizMatchPair
import com.example.model.QuizQuestion
import com.example.model.SchoolCatalog
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.type.GenerateContentResponse
import com.google.firebase.ai.type.RequestOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.json.JSONObject

enum class TutorMode(
    val label: String,
    val shortLabel: String,
    val subtitle: String,
    val modelName: String
) {
    GENERAL(
        label = "Tuteur Polyvalent",
        shortLabel = "Polyvalent",
        subtitle = "Aide claire & recherche web",
        modelName = "gemini-4-hydrogen"
    ),
    REASONING(
        label = "Raisonnement Maths & Sciences",
        shortLabel = "Raisonnement",
        subtitle = "Analyse étape par étape",
        modelName = "gemini-4-carbon"
    ),
    INTERACTIVE(
        label = "Coach Interactif",
        shortLabel = "Coach",
        subtitle = "Dialogue guidé & exercices",
        modelName = "gemini-4-neon"
    ),
    FAST(
        label = "Réponse Éclair",
        shortLabel = "Éclair",
        subtitle = "Définition & mémo rapide",
        modelName = "gemini-3.1-flash-lite"
    )
}

data class GroundingSource(
    val title: String,
    val uri: String,
    val domain: String
)

data class ChatTurnInput(
    val isUser: Boolean,
    val text: String
)

sealed interface ChatStreamEvent {
    data class Chunk(val accumulatedText: String) : ChatStreamEvent
    data class Completed(
        val fullText: String,
        val sources: List<GroundingSource>,
        val searchQueries: List<String>
    ) : ChatStreamEvent
}

data class GeneratedLessonAndQuiz(
    val detectedSubject: String,
    val lessonTitle: String,
    val summaryIntro: String,
    val keyPoints: List<String>,
    val simpleExplanation: String,
    val memoryTip: String,
    val acquiredSkills: List<String> = emptyList(),
    val weakPoints: List<String> = emptyList(),
    val evaluationSummary: String = "",
    val quizTitle: String,
    val targetedGaps: List<String> = emptyList(),
    val questions: List<QuizQuestion>
)

data class GeneratedQuiz(
    val quizTitle: String,
    val targetedGaps: List<String> = emptyList(),
    val questions: List<QuizQuestion>
)

data class CourseAttachment(
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val bytes: ByteArray? = null,
    val extractedText: String? = null,
    val bitmap: Bitmap? = null
) {
    fun toAttachedDocument(): AttachedDocument = AttachedDocument(
        fileName = fileName,
        mimeType = mimeType,
        sizeBytes = sizeBytes
    )
}

class GeminiTutorService {

    private val matchPairSchema = Schema.obj(
        properties = mapOf(
            "left" to Schema.string(description = "Élément fixe à gauche (ex: mot français, calcul, organe, date)"),
            "right" to Schema.string(description = "Étiquette correspondante à glisser-déposer (ex: mot anglais, résultat, rôle, événement)")
        )
    )

    private val imageOptionSchema = Schema.obj(
        properties = mapOf(
            "label" to Schema.string(description = "Courte étiquette sous l'image (1 à 3 mots, ex: 'Sofa', '3/4', 'Triangle', 'Soleil')"),
            "visualKey" to Schema.string(
                description = "Clé d'illustration choisie parmi : sofa, chair, bed, table, house, cat, dog, bus, apple, sun, rain, book, pencil, fraction_half, fraction_quarter, fraction_three_quarters, fraction_third, fraction_two_thirds, triangle, square, rectangle, circle, right_angle, clock_3, clock_6, clock_9, plant, water_drop, ice_cube, cloud_steam, planet_saturn, heart, volcano, magnet, castle, pyramid, compass, globe, mountain, crown, hourglass"
            )
        )
    )

    private val questionItemSchema = Schema.obj(
        properties = mapOf(
            "questionType" to Schema.string(
                description = "Type d'activité interactive : 'IMAGE_CHOICE' (cliquer sur la bonne illustration parmi 4 images dessinées), 'DRAG_MATCH' (mini-jeu glisser-déposer pour associer 3 ou 4 paires), 'DRAG_ORDER' (mini-jeu glisser-déposer pour remettre 3 à 5 éléments dans le bon ordre), 'FILL_BLANK' (mot à saisir / phrase à compléter avec '____'), ou 'MCQ' (choix multiple texte, 1 maximum par quiz)"
            ),
            "prompt" to Schema.string(description = "Consigne ludique et claire de l'activité en français"),
            "illustrationKey" to Schema.string(
                description = "Illustration visuelle adaptée parmi exactement : 'math', 'french', 'science', 'english', 'history'"
            ),
            "illustrationCaption" to Schema.string(
                description = "Courte légende visuelle motivante (max 65 caractères) liée au thème de la question SANS JAMAIS donner la réponse"
            ),
            "sentenceTemplate" to Schema.string(
                description = "Pour FILL_BLANK : phrase ou énoncé avec '____' à compléter (chaîne vide sinon)"
            ),
            "options" to Schema.array(
                items = Schema.string(),
                description = "Pour MCQ uniquement : exactement 4 propositions texte. Liste vide [] pour IMAGE_CHOICE, DRAG_MATCH, DRAG_ORDER et FILL_BLANK"
            ),
            "imageOptions" to Schema.array(
                items = imageOptionSchema,
                description = "Pour IMAGE_CHOICE uniquement : exactement 4 cartes illustrées {label, visualKey} sur lesquelles l'enfant clique pour choisir la bonne image. Liste vide [] sinon"
            ),
            "correctIndex" to Schema.integer(description = "Pour MCQ ou IMAGE_CHOICE : index de la bonne réponse entre 0 et 3. Sinon : 0"),
            "acceptedAnswers" to Schema.array(
                items = Schema.string(),
                description = "Pour FILL_BLANK : 1 à 4 réponses exactes acceptées (ex: ['sofa', 'couch']). Liste vide [] sinon"
            ),
            "matchingPairs" to Schema.array(
                items = matchPairSchema,
                description = "Pour DRAG_MATCH uniquement : 3 à 4 paires {left, right} courtes et distinctes à associer par glisser-déposer. Liste vide [] sinon"
            ),
            "orderedItems" to Schema.array(
                items = Schema.string(),
                description = "Pour DRAG_ORDER uniquement : 3 à 5 étiquettes courtes dans le BON ORDRE exact (ex: mots d'une phrase, étapes scientifiques, ordre chronologique ou croissant). Liste vide [] sinon"
            ),
            "guidingHint" to Schema.string(
                description = "1er indice TRÈS SUBTIL (1 phrase courte de méthode ou de contexte) qui fait réfléchir l'élève SANS JAMAIS donner aucun semblant de réponse : INTERDIT de donner la 1ère lettre, le nombre de lettres, une rime, un mot proche/ressemblant, une orthographe partielle ou un nombre exact caché"
            ),
            "secondHint" to Schema.string(
                description = "2e indice complémentaire (différent du 1er, affiché en plus du 1er si l'élève se trompe encore) tout aussi subtil et méthodologique, SANS JAMAIS révéler la réponse ni aucun semblant de réponse"
            ),
            "explanation" to Schema.string(
                description = "Explication pédagogique neutre et claire affichée uniquement quand l'élève a trouvé la bonne réponse, sans jamais commencer par 'Bravo', 'Bien joué' ou 'Super'"
            ),
            "skillTag" to Schema.string(
                description = "Nom court de la compétence scolaire évaluée (ex: 'Repérer les verbes conjugués', 'Vocabulaire anglais : la maison', 'Tables de multiplication')"
            )
        )
    )

    private val lessonAndQuizSchema = Schema.obj(
        properties = mapOf(
            "detectedSubject" to Schema.string(
                description = "Matière scolaire détectée parmi exactement : Mathématiques, Français, Histoire-Géo, Sciences, Anglais, Physique-Chimie, SVT, Autre"
            ),
            "lessonTitle" to Schema.string(description = "Titre clair et concis du document en français"),
            "summaryIntro" to Schema.string(description = "Introduction courte et motivante de 2 à 3 phrases adaptée à l'âge de l'élève"),
            "keyPoints" to Schema.array(
                items = Schema.string(),
                description = "Liste de 4 à 7 points clés explicites à retenir absolument"
            ),
            "simpleExplanation" to Schema.string(description = "Explication détaillée, structurée et très claire avec des exemples concrets adaptés à l'âge"),
            "memoryTip" to Schema.string(description = "Une astuce mnémotechnique ou conseil pratique facile à retenir"),
            "acquiredSkills" to Schema.array(
                items = Schema.string(),
                description = "2 à 5 compétences acquises ou bien comprises repérées dans ce document/exercice/devoir"
            ),
            "weakPoints" to Schema.array(
                items = Schema.string(),
                description = "1 à 5 lacunes, difficultés ou notions à retravailler identifiées dans ce document/exercice/devoir"
            ),
            "evaluationSummary" to Schema.string(
                description = "Court bilan bienveillant (2 à 3 phrases) pour les parents et l'élève sur ce qui est acquis et ce qu'il faut travailler"
            ),
            "quizTitle" to Schema.string(description = "Titre engageant pour le quiz interactif"),
            "targetedGaps" to Schema.array(
                items = Schema.string(),
                description = "Liste des 1 à 3 notions/lacunes prioritairement ciblées par ce quiz"
            ),
            "questions" to Schema.array(
                items = questionItemSchema,
                description = "5 activités très interactives mêlant obligatoirement IMAGE_CHOICE (cliquer sur la bonne image illustrée), DRAG_MATCH (glisser-déposer paires), DRAG_ORDER (glisser-déposer ordre), FILL_BLANK (mot à saisir) et au maximum 1 MCQ"
            )
        )
    )

    private val additionalQuizSchema = Schema.obj(
        properties = mapOf(
            "quizTitle" to Schema.string(description = "Titre distinct du nouveau quiz interactif en français"),
            "targetedGaps" to Schema.array(
                items = Schema.string(),
                description = "Liste des 1 à 4 lacunes ciblées par ce quiz"
            ),
            "questions" to Schema.array(
                items = questionItemSchema,
                description = "5 nouvelles activités très interactives mêlant IMAGE_CHOICE, DRAG_MATCH, DRAG_ORDER, FILL_BLANK et au maximum 1 MCQ"
            )
        )
    )

    private val sundayReviewQuizSchema = Schema.obj(
        properties = mapOf(
            "quizTitle" to Schema.string(description = "Titre motivant pour le grand quiz de révision du dimanche en français"),
            "targetedGaps" to Schema.array(
                items = Schema.string(),
                description = "Liste des 1 à 6 thématiques ou notions en échec révisées dans ce quiz"
            ),
            "questions" to Schema.array(
                items = questionItemSchema,
                description = "EXACTEMENT 10 nouvelles activités très interactives portant sur les mêmes thématiques que les erreurs de la semaine (sans jamais recopier les anciennes questions mot pour mot), mêlant IMAGE_CHOICE, DRAG_MATCH, DRAG_ORDER, FILL_BLANK et au maximum 2 MCQ"
            )
        )
    )

    private val longRequestOptions: RequestOptions by lazy {
        RequestOptions(timeoutInMillis = 10 * 60 * 1000L)
    }

    private val lessonModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = lessonAndQuizSchema
            },
            requestOptions = longRequestOptions
        )
    }

    private val lessonFallbackModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = lessonAndQuizSchema
            },
            requestOptions = longRequestOptions
        )
    }

    private val quizModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = additionalQuizSchema
            },
            requestOptions = longRequestOptions
        )
    }

    private val quizFallbackModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = additionalQuizSchema
            },
            requestOptions = longRequestOptions
        )
    }

    private val sundayReviewModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = sundayReviewQuizSchema
            },
            requestOptions = longRequestOptions
        )
    }

    private val sundayReviewFallbackModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = sundayReviewQuizSchema
            },
            requestOptions = longRequestOptions
        )
    }

    private val transcribeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-transcribe",
            requestOptions = longRequestOptions
        )
    }

    private val imageModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-nano-banana-2.1",
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
            },
            requestOptions = longRequestOptions
        )
    }

    suspend fun analyzeLessonAndCreateQuiz(
        bitmap: Bitmap?,
        attachments: List<CourseAttachment> = emptyList(),
        notes: String,
        subject: String,
        documentType: String = "Cours",
        difficulty: String = "Moyen",
        childName: String,
        childAge: Int,
        gradeLevel: String,
        existingWeakPoints: List<String> = emptyList()
    ): Result<GeneratedLessonAndQuiz> {
        return try {
            val toneInstruction = ageToneInstruction(childAge)
            val safeDocType = SchoolCatalog.normalizeDocumentType(documentType)
            val safeDifficulty = SchoolCatalog.normalizeDifficulty(difficulty)
            val difficultyLabel = SchoolCatalog.difficultyMultiplierLabel(safeDifficulty)
            val isAutoSubject = subject.equals(SchoolCatalog.AUTO_SUBJECT, ignoreCase = true)

            val promptText = buildString {
                appendLine("Tu es un professeur particulier bienveillant et pédagogue.")
                appendLine("L'élève s'appelle $childName, a $childAge ans et est en classe de $gradeLevel.")
                appendLine("Type de document déposé : $safeDocType.")
                appendLine("Niveau de difficulté demandé pour le quiz : $difficultyLabel.")
                if (isAutoSubject) {
                    appendLine("Matière : À DÉTECTER AUTOMATIQUEMENT ! Range ce document dans la matière exacte la plus pertinente parmi : Mathématiques, Français, Histoire-Géo, Sciences, Anglais, Physique-Chimie, SVT, Autre (champ 'detectedSubject').")
                } else {
                    appendLine("Matière choisie : $subject (confirme-la dans 'detectedSubject' parmi : Mathématiques, Français, Histoire-Géo, Sciences, Anglais, Physique-Chimie, SVT, Autre).")
                }
                appendLine("Consigne de niveau : $toneInstruction")
                appendLine(difficultyGuidanceInstruction(safeDifficulty))
                if (existingWeakPoints.isNotEmpty()) {
                    appendLine("Lacunes déjà repérées chez $childName : ${existingWeakPoints.take(8).joinToString(" ; ")}.")
                }
                if (notes.isNotBlank()) {
                    appendLine("Notes ou précisions de l'élève : $notes")
                }
                if (bitmap != null || attachments.any { it.bitmap != null }) {
                    appendLine("Analyse attentivement la ou les photos fournies ci-jointes.")
                }
                if (attachments.any { it.bytes != null }) {
                    val pdfNames = attachments.filter { it.bytes != null }.joinToString(", ") { it.fileName }
                    appendLine("Analyse attentivement le(s) document(s) PDF ci-joint(s) : $pdfNames.")
                }
                attachments.filter { !it.extractedText.isNullOrBlank() }.forEach { doc ->
                    appendLine()
                    appendLine("--- Contenu du fichier déposé (${doc.fileName}) ---")
                    appendLine(doc.extractedText?.take(20_000))
                    appendLine("--- Fin du fichier ${doc.fileName} ---")
                }
                appendLine()
                appendLine("Mission en français :")
                if (safeDocType == "Exercice" || safeDocType == "Devoir") {
                    appendLine("1. ÉVALUE cet $safeDocType : repère attentivement ce que $childName maîtrise déjà ('acquiredSkills') et ses erreurs, oublis ou notions fragiles ('weakPoints'), puis rédige un court bilan clair ('evaluationSummary') utile pour les parents.")
                    appendLine("2. Rédige une fiche de révision claire (summaryIntro, keyPoints, simpleExplanation, memoryTip) pour consolider les règles liées à cet $safeDocType.")
                } else {
                    appendLine("1. Rédige une synthèse explicite, claire et structurée du cours parfaitement adaptée à un élève de $childAge ans ($gradeLevel).")
                    appendLine("2. Identifie les compétences clés travaillées ('acquiredSkills' si maîtrisées ou notions de base du cours) et les points délicats / pièges à surveiller ('weakPoints'), avec un court 'evaluationSummary'.")
                }
                appendLine("3. Crée un quiz TRÈS INTERACTIF et LUDIQUE de 5 activités de niveau $difficultyLabel ciblées sur les lacunes (PAS uniquement des QCM A/B/C/D !) :")
                appendLine("   - Inclus OBLIGATOIREMENT un mélange varié : au moins 1 question 'IMAGE_CHOICE' (4 cartes illustrées cliquables dans 'imageOptions' avec {label, visualKey} pris dans le catalogue visuel : sofa, chair, bed, table, house, cat, dog, bus, apple, sun, rain, book, pencil, fraction_half, fraction_quarter, fraction_three_quarters, fraction_third, fraction_two_thirds, triangle, square, rectangle, circle, right_angle, clock_3, clock_6, clock_9, plant, water_drop, ice_cube, cloud_steam, planet_saturn, heart, volcano, magnet, castle, pyramid, compass, globe, mountain, crown, hourglass), 1 mini-jeu 'DRAG_MATCH' (glisser-déposer 3-4 paires), 1 mini-jeu 'DRAG_ORDER' (remettre 3-5 éléments dans l'ordre), 1 à 2 exercices 'FILL_BLANK' (mot à saisir / phrase à trous '____') et AU MAXIMUM 1 question 'MCQ'.")
                appendLine("   - Pour CHAQUE question, renseigne 'illustrationKey' ('math', 'french', 'science', 'english' ou 'history') et une courte 'illustrationCaption' motivante sans spoiler.")
                appendLine("   - RÈGLE D'OR SUR LES INDICES ULTRA-SUBTILS : 'guidingHint' (1er indice) et 'secondHint' (2e indice complémentaire, différent du 1er) doivent être TRÈS SUBTILS et purement méthodologiques ou contextuels ! Il est STRICTEMENT INTERDIT de donner même un semblant de réponse : jamais la 1ère ou dernière lettre, jamais le nombre de lettres/syllabes, jamais une rime, jamais 'un mot proche du français' ou 'ressemble à', jamais le nombre exact de réponses cachées. Exemple parfait : 'Relis bien toute la phrase : il n'y a pas qu'un seul verbe conjugué !' puis 'Pose-toi la question : que fait le sujet à chaque étape de la phrase ?'.")
                appendLine("   - Renseigne un 'skillTag' court et précis pour chaque question.")
                appendLine("   - Pour 'explanation' (affichée uniquement une fois la bonne réponse trouvée), reste neutre et pédagogique sans commencer par 'Bravo', 'Bien joué' ou 'Super'.")
            }

            val bitmapsToSend = buildList {
                if (bitmap != null) add(scaleBitmapForAi(bitmap))
                attachments.mapNotNull { it.bitmap }.forEach { add(scaleBitmapForAi(it)) }
            }
            val binaryParts = attachments.filter { it.bytes != null }

            val response = generateWithLessonFallback(
                promptText = promptText,
                bitmapsToSend = bitmapsToSend,
                binaryParts = binaryParts
            )

            parseLessonAndQuizResponse(response.text, subject)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini lesson analysis timed out", e)
            Result.failure(
                Exception("La requête vers l'assistant a dépassé le délai maximal. Vérifie ta connexion et réessaie.", e)
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini lesson analysis failed", e)
            Result.failure(
                Exception(
                    FrenchErrorMessages.toFrenchError(
                        e,
                        "Impossible d'analyser ce document pour le moment. Réessaie dans quelques secondes."
                    ),
                    e
                )
            )
        }
    }

    suspend fun enrichExistingLessonWithAttachments(
        existingLesson: Lesson,
        bitmap: Bitmap?,
        attachments: List<CourseAttachment>,
        notes: String,
        childName: String,
        childAge: Int,
        gradeLevel: String,
        existingWeakPoints: List<String> = emptyList()
    ): Result<GeneratedLessonAndQuiz> {
        return try {
            val toneInstruction = ageToneInstruction(childAge)

            val promptText = buildString {
                appendLine("Tu es un professeur particulier bienveillant et pédagogue.")
                appendLine("L'élève s'appelle $childName, a $childAge ans et est en classe de $gradeLevel.")
                appendLine("Matière : ${existingLesson.subject} — Document existant (${existingLesson.documentType}) : ${existingLesson.title}")
                appendLine("Consigne de niveau : $toneInstruction")
                if (existingWeakPoints.isNotEmpty()) {
                    appendLine("Lacunes actuelles à travailler : ${existingWeakPoints.take(8).joinToString(" ; ")}.")
                }
                appendLine()
                appendLine("=== SYNTHÈSE EXISTANTE À CONSERVER ET ENRICHIR ===")
                appendLine("Résumé : ${existingLesson.summaryIntro}")
                if (existingLesson.keyPoints.isNotEmpty()) {
                    appendLine("Points clés existants : ${existingLesson.keyPoints.joinToString(" | ")}")
                }
                appendLine("Explication existante : ${existingLesson.simpleExplanation.take(2500)}")
                if (existingLesson.memoryTip.isNotBlank()) {
                    appendLine("Astuce existante : ${existingLesson.memoryTip}")
                }
                appendLine()
                appendLine("=== NOUVEAUX DOCUMENTS / COMPLÉMENTS DÉPOSÉS ===")
                if (notes.isNotBlank()) {
                    appendLine("Notes ajoutées par l'élève : $notes")
                }
                if (bitmap != null || attachments.any { it.bitmap != null }) {
                    appendLine("Analyse la ou les nouvelles images jointes.")
                }
                if (attachments.any { it.bytes != null }) {
                    val pdfNames = attachments.filter { it.bytes != null }.joinToString(", ") { it.fileName }
                    appendLine("Analyse attentivement le(s) nouveau(x) PDF joint(s) : $pdfNames.")
                }
                attachments.filter { !it.extractedText.isNullOrBlank() }.forEach { doc ->
                    appendLine()
                    appendLine("--- Contenu du fichier ajouté (${doc.fileName}) ---")
                    appendLine(doc.extractedText?.take(20_000))
                    appendLine("--- Fin du fichier ${doc.fileName} ---")
                }
                appendLine()
                appendLine("Mission en français :")
                appendLine("1. Fusionne et enrichis la synthèse (summaryIntro, keyPoints, simpleExplanation, memoryTip) et mets à jour les compétences acquises ('acquiredSkills'), les lacunes ('weakPoints') et 'evaluationSummary'.")
                appendLine("2. Crée un nouveau quiz TRÈS INTERACTIF de 5 activités ciblant les lacunes : inclus au moins 1 question 'IMAGE_CHOICE' (4 images cliquables dans 'imageOptions' avec {label, visualKey}), 1 mini-jeu 'DRAG_MATCH' (associer 3-4 paires par glisser-déposer), 1 mini-jeu 'DRAG_ORDER' (remettre 3-5 éléments dans l'ordre), 1 à 2 exercices 'FILL_BLANK' (mot à saisir / phrase à trous) et au maximum 1 'MCQ'. Renseigne 'illustrationKey' et 'illustrationCaption' pour chaque question.")
                appendLine("3. RÈGLE ABSOLUE SUR LES INDICES ULTRA-SUBTILS : 'guidingHint' et 'secondHint' (distincts et complémentaires) doivent uniquement orienter la démarche SANS JAMAIS donner un semblant de réponse (aucune lettre initiale, aucun nombre de lettres, aucune rime, aucun mot ressemblant, aucun nombre exact caché).")
                appendLine("4. Confirme '${existingLesson.subject}' dans 'detectedSubject'.")
            }

            val bitmapsToSend = buildList {
                if (bitmap != null) add(scaleBitmapForAi(bitmap))
                attachments.mapNotNull { it.bitmap }.forEach { add(scaleBitmapForAi(it)) }
            }
            val binaryParts = attachments.filter { it.bytes != null }

            val response = generateWithLessonFallback(
                promptText = promptText,
                bitmapsToSend = bitmapsToSend,
                binaryParts = binaryParts
            )

            parseLessonAndQuizResponse(response.text, existingLesson.subject)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini lesson enrichment timed out", e)
            Result.failure(
                Exception("La requête vers l'assistant a dépassé le délai maximal. Vérifie ta connexion et réessaie.", e)
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini lesson enrichment failed", e)
            Result.failure(
                Exception(
                    FrenchErrorMessages.toFrenchError(
                        e,
                        "Impossible d'analyser les fichiers ajoutés. Réessaie dans quelques secondes."
                    ),
                    e
                )
            )
        }
    }

    private suspend fun generateWithLessonFallback(
        promptText: String,
        bitmapsToSend: List<Bitmap>,
        binaryParts: List<CourseAttachment>
    ): GenerateContentResponse {
        suspend fun callModel(model: GenerativeModel): GenerateContentResponse {
            return if (bitmapsToSend.isNotEmpty() || binaryParts.isNotEmpty()) {
                model.generateContent(
                    content {
                        bitmapsToSend.forEach { image(it) }
                        binaryParts.forEach { part ->
                            inlineData(part.bytes!!, part.mimeType)
                        }
                        text(promptText)
                    }
                )
            } else {
                model.generateContent(promptText)
            }
        }

        return try {
            callModel(lessonModel)
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Primary lesson model timed out, retrying with gemini-3.5-flash-lite", e)
            delay(600L)
            callModel(lessonFallbackModel)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (!isTransientOrRetryableAiError(e)) throw e
            Log.w(TAG, "Primary lesson model busy/unavailable, retrying with gemini-3.5-flash-lite", e)
            delay(800L)
            callModel(lessonFallbackModel)
        }
    }

    private fun isTransientOrRetryableAiError(error: Throwable): Boolean {
        if (error is QuotaExceededException || error is TimeoutCancellationException) return true
        val chainText = generateSequence(error) { it.cause }
            .take(8)
            .mapNotNull { it.message }
            .joinToString(" ")
            .lowercase()
        return chainText.contains("high demand") ||
            chainText.contains("spikes in demand") ||
            chainText.contains("temporarily") ||
            chainText.contains("overloaded") ||
            chainText.contains("503") ||
            chainText.contains("service unavailable") ||
            chainText.contains("unavailable") ||
            chainText.contains("resource_exhausted") ||
            chainText.contains("resource exhausted") ||
            chainText.contains("try again later") ||
            chainText.contains("timeout") ||
            chainText.contains("timed out") ||
            chainText.contains("deadline")
    }

    internal fun parseLessonAndQuizResponse(
        jsonString: String?,
        subject: String
    ): Result<GeneratedLessonAndQuiz> {
        if (jsonString.isNullOrBlank()) {
            return Result.failure(IllegalStateException("L'IA n'a renvoyé aucun contenu."))
        }
        val json = JSONObject(stripJsonFences(jsonString))
        val keyPoints = parseStringList(json, "keyPoints", maxItems = 12)
            .ifEmpty { listOf("Relire attentivement les notions essentielles de ce cours.") }

        val questions = parseQuestions(json)
        if (questions.isEmpty()) {
            return Result.failure(IllegalStateException("Aucune question de quiz n'a pu être générée."))
        }

        val fallbackSubject = if (subject.equals(com.example.model.SchoolCatalog.AUTO_SUBJECT, ignoreCase = true)) {
            "Autre"
        } else {
            com.example.model.SchoolCatalog.normalizeSubject(subject, "Autre")
        }
        val detectedSubject = if (subject.equals(com.example.model.SchoolCatalog.AUTO_SUBJECT, ignoreCase = true)) {
            com.example.model.SchoolCatalog.normalizeSubject(json.optString("detectedSubject", ""), fallbackSubject)
        } else {
            fallbackSubject
        }

        val lessonTitle = json.optString("lessonTitle", "").trim()
            .ifBlank { "Fiche de révision : $detectedSubject" }
            .take(120)
        val summaryIntro = json.optString("summaryIntro", "").trim()
            .ifBlank { keyPoints.first() }
            .take(1900)
        val simpleExplanation = json.optString("simpleExplanation", "").trim()
            .ifBlank { summaryIntro }
            .take(5800)
        val memoryTip = json.optString("memoryTip", "").trim().take(950)
        val acquiredSkills = parseStringList(json, "acquiredSkills", maxItems = 15)
        val weakPoints = parseStringList(json, "weakPoints", maxItems = 15)
        val evaluationSummary = json.optString("evaluationSummary", "").trim().take(1400)
        val targetedGaps = parseStringList(json, "targetedGaps", maxItems = 15)
            .ifEmpty { weakPoints.take(3) }
        val quizTitle = json.optString("quizTitle", "").trim()
            .ifBlank { "Quiz : $detectedSubject" }
            .take(120)

        return Result.success(
            GeneratedLessonAndQuiz(
                detectedSubject = detectedSubject,
                lessonTitle = lessonTitle,
                summaryIntro = summaryIntro,
                keyPoints = keyPoints,
                simpleExplanation = simpleExplanation,
                memoryTip = memoryTip,
                acquiredSkills = acquiredSkills,
                weakPoints = weakPoints,
                evaluationSummary = evaluationSummary,
                quizTitle = quizTitle,
                targetedGaps = targetedGaps,
                questions = questions
            )
        )
    }

    private fun parseStringList(json: JSONObject, key: String, maxItems: Int): List<String> {
        val arr = json.optJSONArray(key) ?: return emptyList()
        return (0 until arr.length())
            .map { arr.optString(it, "").trim().take(120) }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(maxItems)
    }

    suspend fun generateAdditionalQuiz(
        lessonTitle: String,
        summaryIntro: String,
        keyPoints: List<String>,
        simpleExplanation: String,
        subject: String,
        childAge: Int,
        gradeLevel: String,
        difficulty: String,
        existingQuizCount: Int,
        childWeakPoints: List<String> = emptyList()
    ): Result<GeneratedQuiz> {
        return try {
            val promptText = buildString {
                appendLine("Tu es un professeur particulier bienveillant pour un élève de $childAge ans en classe de $gradeLevel.")
                appendLine("Matière : $subject — Cours : $lessonTitle")
                appendLine("Difficulté demandée : $difficulty.")
                appendLine("Synthèse du cours :")
                appendLine(summaryIntro)
                appendLine("Points clés : ${keyPoints.joinToString("; ")}")
                appendLine("Explication : ${simpleExplanation.take(2000)}")
                if (childWeakPoints.isNotEmpty()) {
                    appendLine("LACUNES PRIORITAIRES À CIBLER : ${childWeakPoints.take(8).joinToString(" ; ")}.")
                }
                appendLine("Crée le quiz n°${existingQuizCount + 1} composé de 5 nouvelles activités TRÈS INTERACTIVES et ludiques en français (ne fais PAS uniquement du QCM A/B/C/D) :")
                appendLine("- Inclus un mélange varié : au moins 1 question 'IMAGE_CHOICE' (4 images illustrées cliquables dans 'imageOptions' avec {label, visualKey}), 1 mini-jeu 'DRAG_MATCH' (glisser-déposer pour associer 3 ou 4 paires), 1 mini-jeu 'DRAG_ORDER' (glisser-déposer pour remettre 3 à 5 éléments dans l'ordre), 1 à 2 exercices 'FILL_BLANK' (mot à écrire / phrase à trous '____') et AU MAXIMUM 1 'MCQ'.")
                appendLine("- Renseigne 'illustrationKey' ('math', 'french', 'science', 'english', 'history') et 'illustrationCaption' pour chaque question.")
                appendLine("- RÈGLE ABSOLUE SUR LES INDICES ULTRA-SUBTILS : Pour chaque question, rédige 'guidingHint' et 'secondHint' (2 indices distincts, purement méthodologiques) SANS JAMAIS donner aucun semblant de réponse (pas de 1ère lettre, pas de nombre de lettres, pas de rime, pas de mot proche, pas de nombre exact caché).")
                appendLine("- Renseigne 'skillTag' (nom court de la compétence) et 'targetedGaps' (les notions/lacunes travaillées).")
                appendLine("- Pour 'explanation', reste neutre et pédagogique sans commencer par 'Bravo', 'Bien joué' ou 'Super'.")
            }

            generateQuizFromPrompt(promptText, fallbackTitle = "Quiz $difficulty : $lessonTitle", fallbackGaps = childWeakPoints.take(3))
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini quiz generation timed out", e)
            Result.failure(Exception("La génération du quiz a expiré. Réessaie.", e))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini quiz generation failed", e)
            Result.failure(
                Exception(
                    FrenchErrorMessages.toFrenchError(
                        e,
                        "Impossible de générer le quiz pour le moment. Réessaie dans quelques secondes."
                    ),
                    e
                )
            )
        }
    }

    suspend fun generateGapTargetedQuiz(
        childName: String,
        childAge: Int,
        gradeLevel: String,
        targetLesson: Lesson,
        weakPoints: List<String>,
        difficulty: String = "Moyen"
    ): Result<GeneratedQuiz> {
        return try {
            val focusGaps = (weakPoints + targetLesson.weakPoints).distinct().take(8)
            val promptText = buildString {
                appendLine("Tu es un professeur particulier bienveillant qui accompagne $childName ($childAge ans, $gradeLevel).")
                appendLine("Matière ciblée : ${targetLesson.subject} — Référence : ${targetLesson.title} (${targetLesson.documentType})")
                appendLine("Difficulté : $difficulty.")
                if (focusGaps.isNotEmpty()) {
                    appendLine("LACUNES À TRAVAILLER EN PRIORITÉ :")
                    focusGaps.forEach { appendLine("- $it") }
                }
                appendLine("Contexte du cours/exercice : ${targetLesson.summaryIntro}")
                if (targetLesson.keyPoints.isNotEmpty()) {
                    appendLine("Notions clés : ${targetLesson.keyPoints.joinToString(" ; ")}")
                }
                appendLine()
                appendLine("Crée un quiz sur mesure TRÈS INTERACTIF de 5 activités pour aider $childName à combler ses lacunes (PAS uniquement des QCM A/B/C/D) :")
                appendLine("- Inclus un mélange ludique : au moins 1 question 'IMAGE_CHOICE' (4 images illustrées cliquables dans 'imageOptions' avec {label, visualKey}), 1 mini-jeu 'DRAG_MATCH' (glisser-déposer pour associer 3 ou 4 paires), 1 mini-jeu 'DRAG_ORDER' (remettre 3 à 5 éléments dans l'ordre par glisser-déposer), 1 à 2 exercices 'FILL_BLANK' (mot à écrire / phrase avec '____') et AU MAXIMUM 1 'MCQ'.")
                appendLine("- Renseigne 'illustrationKey' ('math', 'french', 'science', 'english', 'history') et 'illustrationCaption' pour chaque question.")
                appendLine("- RÈGLE ABSOLUE SUR LES INDICES ULTRA-SUBTILS : L'application ne doit JAMAIS donner la réponse ni même un semblant de réponse ! Rédige 'guidingHint' et 'secondHint' (2 indices complémentaires de réflexion/méthode) sans jamais mentionner de lettre initiale, de nombre de lettres, de rime, de mot ressemblant ou de nombre exact caché.")
                appendLine("- Renseigne 'skillTag' pour chaque question et 'targetedGaps' avec les lacunes ciblées.")
            }

            generateQuizFromPrompt(
                promptText = promptText,
                fallbackTitle = "Quiz ciblé : ${targetLesson.subject}",
                fallbackGaps = focusGaps.take(4)
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini gap quiz generation timed out", e)
            Result.failure(Exception("La génération du quiz ciblé a expiré. Réessaie.", e))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini gap quiz generation failed", e)
            Result.failure(
                Exception(
                    FrenchErrorMessages.toFrenchError(
                        e,
                        "Impossible de créer ton quiz ciblé pour le moment. Réessaie dans quelques secondes."
                    ),
                    e
                )
            )
        }
    }

    suspend fun generateSundayReviewQuiz(
        childName: String,
        childAge: Int,
        gradeLevel: String,
        missedQuestions: List<MissedQuestionRecord>,
        lessons: List<Lesson>,
        difficulty: String = "Moyen"
    ): Result<GeneratedQuiz> {
        return try {
            val safeDifficulty = SchoolCatalog.normalizeDifficulty(difficulty)
            val difficultyLabel = SchoolCatalog.difficultyMultiplierLabel(safeDifficulty)
            val uniqueThemes = missedQuestions
                .map { it.skillTag.ifBlank { "${it.subject} : ${it.lessonTitle}".trim(' ', ':') } }
                .filter { it.isNotBlank() }
                .distinct()
                .take(10)

            val relatedLessons = lessons.filter { lesson ->
                missedQuestions.any { it.lessonId == lesson.id || it.subject.equals(lesson.subject, ignoreCase = true) }
            }.take(5)

            val promptText = buildString {
                appendLine("Tu es un professeur particulier bienveillant qui prépare le GRAND QUIZ DE RÉVISION DU DIMANCHE (14h) pour $childName ($childAge ans, classe de $gradeLevel).")
                appendLine("Niveau de difficulté : $difficultyLabel.")
                appendLine(difficultyGuidanceInstruction(safeDifficulty))
                appendLine("Consigne de ton : ${ageToneInstruction(childAge)}")
                appendLine()
                appendLine("=== ERREURS ENREGISTRÉES CETTE SEMAINE (AU 1ER ESSAI) ===")
                missedQuestions.take(20).forEachIndexed { idx, rec ->
                    val themePart = rec.skillTag.ifBlank { rec.lessonTitle }.ifBlank { rec.subject }
                    appendLine("${idx + 1}. [${rec.subject}] Thématique : $themePart | Ancienne question ratée : « ${rec.prompt} »")
                }
                if (relatedLessons.isNotEmpty()) {
                    appendLine()
                    appendLine("=== RAPPEL DES COURS ASSOCIÉS ===")
                    relatedLessons.forEach { l ->
                        appendLine("- ${l.subject} / ${l.title} : ${l.summaryIntro.take(280)}")
                    }
                }
                appendLine()
                appendLine("MISSION IMPÉRATIVE :")
                appendLine("1. Crée EXACTEMENT 10 NOUVELLES QUESTIONS interactives en français qui reviennent sur TOUTES les thématiques en échec ci-dessus.")
                appendLine("2. NE RECOPIE JAMAIS une ancienne question mot pour mot ! Invente d'AUTRES questions, exemples, phrases, calculs ou situations différentes portant sur les MÊMES thématiques / notions.")
                appendLine("3. Répartis les 10 questions de façon ludique et variée : au moins 2 'IMAGE_CHOICE' (4 images cliquables dans 'imageOptions' avec {label, visualKey}), 2 'DRAG_MATCH' (glisser-déposer 3-4 paires), 2 'DRAG_ORDER' (remettre 3-5 éléments dans l'ordre), 2 à 3 'FILL_BLANK' (mot à saisir / phrase à trous '____') et AU MAXIMUM 2 'MCQ'.")
                appendLine("4. Renseigne 'illustrationKey' ('math', 'french', 'science', 'english', 'history'), 'illustrationCaption' et 'skillTag' pour chacune des 10 questions.")
                appendLine("5. RÈGLE ABSOLUE SUR LES INDICES ULTRA-SUBTILS : Pour chaque question, rédige 'guidingHint' et 'secondHint' (2 indices distincts, complémentaires et purement méthodologiques) SANS JAMAIS donner la réponse ni un semblant de réponse (jamais de 1ère lettre, nombre de lettres, rime, mot ressemblant ou nombre exact caché).")
                appendLine("6. Pour 'explanation', reste neutre et pédagogique sans commencer par 'Bravo', 'Bien joué' ou 'Super'.")
            }

            val response = try {
                sundayReviewModel.generateContent(promptText)
            } catch (e: TimeoutCancellationException) {
                Log.w(TAG, "Primary Sunday review model timed out, retrying with gemini-3.5-flash-lite", e)
                delay(600L)
                sundayReviewFallbackModel.generateContent(promptText)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (!isTransientOrRetryableAiError(e)) throw e
                Log.w(TAG, "Primary Sunday review model busy/unavailable, retrying with gemini-3.5-flash-lite", e)
                delay(800L)
                sundayReviewFallbackModel.generateContent(promptText)
            }

            parseQuizResponse(
                jsonString = response.text,
                fallbackTitle = "Révision du Dimanche (10 questions)",
                fallbackGaps = uniqueThemes.take(6)
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini Sunday review quiz timed out", e)
            Result.failure(Exception("La génération du quiz du dimanche a expiré. Réessaie.", e))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini Sunday review quiz failed", e)
            Result.failure(
                Exception(
                    FrenchErrorMessages.toFrenchError(
                        e,
                        "Impossible de préparer le quiz du dimanche pour le moment. Réessaie dans quelques secondes."
                    ),
                    e
                )
            )
        }
    }

    private suspend fun generateQuizFromPrompt(
        promptText: String,
        fallbackTitle: String,
        fallbackGaps: List<String>
    ): Result<GeneratedQuiz> {
        val response = try {
            quizModel.generateContent(promptText)
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Primary quiz model timed out, retrying with gemini-3.5-flash-lite", e)
            delay(600L)
            quizFallbackModel.generateContent(promptText)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (!isTransientOrRetryableAiError(e)) throw e
            Log.w(TAG, "Primary quiz model busy/unavailable, retrying with gemini-3.5-flash-lite", e)
            delay(800L)
            quizFallbackModel.generateContent(promptText)
        }

        return parseQuizResponse(response.text, fallbackTitle, fallbackGaps)
    }

    internal fun parseQuizResponse(
        jsonString: String?,
        fallbackTitle: String,
        fallbackGaps: List<String> = emptyList()
    ): Result<GeneratedQuiz> {
        if (jsonString.isNullOrBlank()) {
            return Result.failure(IllegalStateException("L'IA n'a renvoyé aucun quiz."))
        }
        val json = JSONObject(stripJsonFences(jsonString))
        val questions = parseQuestions(json)
        if (questions.isEmpty()) {
            return Result.failure(IllegalStateException("Aucune question générée."))
        }

        val safeQuizTitle = json.optString("quizTitle", "").trim()
            .ifBlank { fallbackTitle }
            .take(120)
        val targetedGaps = parseStringList(json, "targetedGaps", maxItems = 15)
            .ifEmpty { fallbackGaps }

        return Result.success(
            GeneratedQuiz(
                quizTitle = safeQuizTitle,
                targetedGaps = targetedGaps,
                questions = questions
            )
        )
    }

    private fun difficultyGuidanceInstruction(normalizedDifficulty: String): String = when (normalizedDifficulty) {
        "Facile" -> "Niveau Facile (×1) : privilégie des questions directes, rassurantes et progressives sur les notions fondamentales du cours."
        "Défi" -> "Niveau Difficile (×1.5) : propose des questions plus exigeantes demandant de la réflexion, de l'application concrète et de déjouer les pièges classiques."
        else -> "Niveau Moyen (×1.2) : propose un bon équilibre entre compréhension du cours et mise en application."
    }

    fun streamTutorChat(
        userMessage: String,
        attachedImage: Bitmap?,
        history: List<ChatTurnInput>,
        mode: TutorMode,
        useWebSearch: Boolean,
        childName: String,
        childAge: Int,
        gradeLevel: String,
        lessonContext: Lesson?
    ): Flow<ChatStreamEvent> = flow {
        val systemText = buildString {
            appendLine("Tu es Quizzy IA, un tuteur scolaire bienveillant, patient et encourageant qui répond toujours en français.")
            appendLine("Élève : $childName, $childAge ans, classe de $gradeLevel.")
            appendLine("Style pédagogique : ${ageToneInstruction(childAge)}")
            when {
                useWebSearch -> {
                    appendLine("Rôle actif : Assistant documenté avec Recherche Google. Donne des informations vérifiées, à jour, sourcées et pédagogiques.")
                }
                mode == TutorMode.REASONING -> {
                    appendLine("Rôle actif : Professeur de mathématiques et sciences. Décompose chaque raisonnement étape par étape, vérifie les calculs et explique le 'pourquoi'.")
                }
                mode == TutorMode.INTERACTIVE -> {
                    appendLine("Rôle actif : Coach interactif socratique. Guide l'élève par des indices progressifs, pose une petite question de relance et félicite ses progrès au lieu de tout faire à sa place.")
                }
                mode == TutorMode.FAST -> {
                    appendLine("Rôle actif : Fiche mémo express. Réponds de manière concise, structurée en puces courtes et faciles à mémoriser.")
                }
                else -> {
                    appendLine("Rôle actif : Tuteur généraliste polyvalent. Explique clairement avec des exemples concrets.")
                }
            }
            if (lessonContext != null) {
                appendLine()
                appendLine("Cours actuellement étudié par l'élève :")
                appendLine("- Matière : ${lessonContext.subject}")
                appendLine("- Titre : ${lessonContext.title}")
                appendLine("- Résumé : ${lessonContext.summaryIntro}")
                if (lessonContext.keyPoints.isNotEmpty()) {
                    appendLine("- Points clés : ${lessonContext.keyPoints.joinToString(" | ")}")
                }
            }
        }

        val effectiveModelName = if (useWebSearch) "gemini-4-hydrogen" else mode.modelName
        val toolsList = if (useWebSearch) listOf(Tool.googleSearch()) else null

        val chatModel = Firebase.ai.generativeModel(
            modelName = effectiveModelName,
            tools = toolsList,
            systemInstruction = content { text(systemText) },
            requestOptions = longRequestOptions
        )

        val sdkHistory: List<Content> = normalizeChatHistoryForSdk(history)
            .map { turn ->
                content(role = if (turn.isUser) "user" else "model") {
                    text(turn.text)
                }
            }

        val scaledBitmap = attachedImage?.let { scaleBitmapForAi(it) }
        val messageContent = content("user") {
            if (scaledBitmap != null) {
                image(scaledBitmap)
            }
            text(userMessage.ifBlank { "Explique-moi cette image étape par étape." })
        }

        val accumulated = StringBuilder()
        val collectedSources = linkedMapOf<String, GroundingSource>()
        val collectedQueries = linkedSetOf<String>()

        suspend fun streamFromModel(model: GenerativeModel) {
            val chat = model.startChat(history = sdkHistory)
            chat.sendMessageStream(messageContent).collect { chunk ->
                val partText = chunk.text.orEmpty()
                if (partText.isNotEmpty()) {
                    accumulated.append(partText)
                    emit(ChatStreamEvent.Chunk(accumulated.toString()))
                }
                chunk.candidates.firstOrNull()?.groundingMetadata?.let { meta ->
                    meta.webSearchQueries.forEach { q ->
                        if (q.isNotBlank()) collectedQueries.add(q)
                    }
                    meta.groundingChunks.forEach { gChunk ->
                        val web = gChunk.web
                        val uri = web?.uri.orEmpty()
                        if (uri.isNotBlank()) {
                            val domain = web?.domain.orEmpty()
                            val title = web?.title.orEmpty().ifBlank { domain.ifBlank { "Source web" } }
                            collectedSources.putIfAbsent(
                                uri,
                                GroundingSource(
                                    title = title,
                                    uri = uri,
                                    domain = domain
                                )
                            )
                        }
                    }
                }
            }
        }

        try {
            streamFromModel(chatModel)
        } catch (e: TimeoutCancellationException) {
            if (accumulated.isNotEmpty()) throw e
            Log.w(TAG, "Chat model $effectiveModelName timed out, falling back to gemini-3.5-flash-lite", e)
            delay(600L)
            val fallbackChatModel = Firebase.ai.generativeModel(
                modelName = "gemini-3.5-flash-lite",
                systemInstruction = content { text(systemText) },
                requestOptions = longRequestOptions
            )
            streamFromModel(fallbackChatModel)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (accumulated.isNotEmpty() || !isTransientOrRetryableAiError(e)) throw e
            Log.w(TAG, "Chat model $effectiveModelName busy/unavailable, falling back to gemini-3.5-flash-lite", e)
            delay(700L)
            val fallbackChatModel = Firebase.ai.generativeModel(
                modelName = "gemini-3.5-flash-lite",
                systemInstruction = content { text(systemText) },
                requestOptions = longRequestOptions
            )
            streamFromModel(fallbackChatModel)
        }

        val finalText = accumulated.toString().trim()
        if (finalText.isBlank()) {
            throw IllegalStateException("L'assistant n'a renvoyé aucune réponse.")
        }
        emit(
            ChatStreamEvent.Completed(
                fullText = finalText,
                sources = collectedSources.values.toList(),
                searchQueries = collectedQueries.toList()
            )
        )
    }

    suspend fun transcribeAudio(
        audioBytes: ByteArray,
        mimeType: String = "audio/mp4"
    ): Result<String> {
        if (audioBytes.isEmpty()) {
            return Result.failure(IllegalArgumentException("L'enregistrement audio est vide."))
        }
        if (audioBytes.size > 18 * 1024 * 1024) {
            return Result.failure(IllegalArgumentException("L'audio dépasse la taille maximale autorisée."))
        }
        return try {
            val response = transcribeModel.generateContent(
                content {
                    inlineData(audioBytes, mimeType)
                    text("Transcris fidèlement cet enregistrement audio en français, avec une ponctuation propre et sans aucun commentaire.")
                }
            )
            val transcript = response.text?.trim().orEmpty()
            if (transcript.isBlank()) {
                Result.failure(IllegalStateException("Aucune parole n'a pu être transcrite."))
            } else {
                Result.success(transcript)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Audio transcription timed out", e)
            Result.failure(Exception("La transcription audio a expiré. Réessaie avec un message plus court.", e))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Audio transcription failed", e)
            Result.failure(
                Exception(
                    FrenchErrorMessages.toFrenchError(e, "Impossible de transcrire l'audio pour le moment."),
                    e
                )
            )
        }
    }

    suspend fun generateEducationalImage(
        prompt: String,
        gradeLevel: String,
        childAge: Int
    ): Result<Bitmap> {
        if (prompt.isBlank()) {
            return Result.failure(IllegalArgumentException("Décris d'abord l'image ou le schéma à créer."))
        }
        return try {
            val enrichedPrompt = buildString {
                append("Crée une illustration pédagogique claire, colorée, soignée et adaptée à un élève de $childAge ans (niveau $gradeLevel) : ")
                append(prompt.trim())
            }
            val response = imageModel.generateContent(enrichedPrompt)
            val bitmap = response.candidates.firstOrNull()
                ?.content?.parts?.filterIsInstance<ImagePart>()
                ?.firstOrNull()?.image

            if (bitmap != null) {
                Result.success(bitmap)
            } else {
                Result.failure(IllegalStateException("Aucune image n'a été générée par le modèle."))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Image generation timed out", e)
            Result.failure(Exception("La génération d'image a expiré. Réessaie.", e))
        } catch (e: QuotaExceededException) {
            Log.e(TAG, "Image generation quota exceeded", e)
            Result.failure(Exception("Quota d'images atteint temporairement. Réessaie dans quelques instants.", e))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Image generation failed", e)
            Result.failure(
                Exception(
                    FrenchErrorMessages.toFrenchError(e, "Échec de la création d'image. Réessaie dans un instant."),
                    e
                )
            )
        }
    }

    suspend fun editEducationalImage(
        inputBitmap: Bitmap,
        editInstruction: String
    ): Result<Bitmap> {
        if (editInstruction.isBlank()) {
            return Result.failure(IllegalArgumentException("Indique la modification souhaitée sur l'image."))
        }
        return try {
            val scaled = scaleBitmapForAi(inputBitmap, maxDim = 1024)
            val response = imageModel.generateContent(
                content {
                    image(scaled)
                    text("Modifie cette image pédagogique en français selon l'instruction suivante : ${editInstruction.trim()}")
                }
            )
            val bitmap = response.candidates.firstOrNull()
                ?.content?.parts?.filterIsInstance<ImagePart>()
                ?.firstOrNull()?.image

            if (bitmap != null) {
                Result.success(bitmap)
            } else {
                Result.failure(IllegalStateException("Le modèle n'a pas renvoyé d'image modifiée."))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Image edit timed out", e)
            Result.failure(Exception("La retouche d'image a expiré. Réessaie dans un instant.", e))
        } catch (e: QuotaExceededException) {
            Log.e(TAG, "Image edit quota exceeded", e)
            Result.failure(Exception("Quota d'images atteint temporairement. Réessaie plus tard.", e))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Image edit failed", e)
            Result.failure(
                Exception(
                    FrenchErrorMessages.toFrenchError(e, "Échec de la retouche d'image. Réessaie dans un instant."),
                    e
                )
            )
        }
    }

    private fun ageToneInstruction(childAge: Int): String = when {
        childAge <= 8 -> "Utilise des mots simples, des phrases courtes, des images concrètes du quotidien et un ton joyeux et encourageant."
        childAge <= 11 -> "Utilise un vocabulaire accessible de primaire/CM, des exemples concrets étape par étape et une structure aérée."
        childAge <= 14 -> "Utilise un ton clair de collège, structure bien les notions, définitions et méthodes avec un exemple parlant."
        else -> "Utilise un style rigoureux et limpide de lycée, mettant en évidence concepts clés, définitions exactes et raisonnement."
    }

    internal fun parseQuestions(json: JSONObject): List<QuizQuestion> {
        val arr = json.optJSONArray("questions") ?: return emptyList()
        val list = mutableListOf<QuizQuestion>()
        for (i in 0 until arr.length().coerceAtMost(10)) {
            val q = arr.optJSONObject(i) ?: continue
            val prompt = q.optString("prompt", "").trim()
            if (prompt.isEmpty()) continue

            val rawType = q.optString("questionType", QuizQuestion.TYPE_MCQ).trim().uppercase()
            val optsArr = q.optJSONArray("options")
            val opts = if (optsArr != null) {
                (0 until optsArr.length())
                    .map { optsArr.optString(it, "").trim() }
                    .filter { it.isNotEmpty() }
            } else {
                emptyList()
            }
            val acceptedArr = q.optJSONArray("acceptedAnswers")
            val accepted = if (acceptedArr != null) {
                (0 until acceptedArr.length())
                    .map { acceptedArr.optString(it, "").trim() }
                    .filter { it.isNotEmpty() }
                    .distinct()
                    .take(6)
            } else {
                emptyList()
            }
            val sentenceTemplate = q.optString("sentenceTemplate", "").trim().take(500)
            val skillTag = q.optString("skillTag", "").trim().take(80)
            val illustrationKey = q.optString("illustrationKey", "").trim().lowercase().take(24)
            val illustrationCaption = q.optString("illustrationCaption", "").trim().take(90)
            val cleanExplanation = com.example.model.QuizFeedbackHelper.sanitizeNeutralExplanation(
                q.optString("explanation", "")
            )

            val pairsArr = q.optJSONArray("matchingPairs")
            val matchingPairs = if (pairsArr != null) {
                (0 until pairsArr.length().coerceAtMost(5)).mapNotNull { idx ->
                    val pairObj = pairsArr.optJSONObject(idx) ?: return@mapNotNull null
                    val left = pairObj.optString("left", "").trim().take(80)
                    val right = pairObj.optString("right", "").trim().take(80)
                    if (left.isNotEmpty() && right.isNotEmpty()) QuizMatchPair(left, right) else null
                }.distinctBy { it.left.lowercase() }
            } else {
                emptyList()
            }

            val orderArr = q.optJSONArray("orderedItems")
            val orderedItems = if (orderArr != null) {
                (0 until orderArr.length().coerceAtMost(6))
                    .map { orderArr.optString(it, "").trim().take(80) }
                    .filter { it.isNotEmpty() }
            } else {
                emptyList()
            }

            val imageOptsArr = q.optJSONArray("imageOptions")
            val imageOptions = if (imageOptsArr != null) {
                (0 until imageOptsArr.length().coerceAtMost(4)).mapNotNull { idx ->
                    val imgObj = imageOptsArr.optJSONObject(idx) ?: return@mapNotNull null
                    val label = imgObj.optString("label", "").trim().take(50)
                    val visualKey = imgObj.optString("visualKey", "").trim().lowercase().take(40)
                    if (label.isNotEmpty() || visualKey.isNotEmpty()) {
                        QuizImageOption(
                            label = label.ifBlank { visualKey },
                            visualKey = visualKey.ifBlank { label.lowercase() }
                        )
                    } else null
                }
            } else {
                emptyList()
            }

            val isImageChoice = (rawType == QuizQuestion.TYPE_IMAGE_CHOICE && imageOptions.size >= 2) ||
                (imageOptions.size >= 2 && opts.size < 2 && accepted.isEmpty() && matchingPairs.size < 2 && orderedItems.size < 2)
            val isDragMatch = !isImageChoice &&
                ((rawType == QuizQuestion.TYPE_DRAG_MATCH && matchingPairs.size >= 2) ||
                    (matchingPairs.size >= 2 && opts.size < 2 && accepted.isEmpty() && orderedItems.size < 2))
            val isDragOrder = !isImageChoice && !isDragMatch &&
                ((rawType == QuizQuestion.TYPE_DRAG_ORDER && orderedItems.size >= 2) ||
                    (orderedItems.size >= 2 && opts.size < 2 && accepted.isEmpty()))
            val isFillBlank = !isImageChoice && !isDragMatch && !isDragOrder &&
                ((rawType == QuizQuestion.TYPE_FILL_BLANK && accepted.isNotEmpty()) ||
                    (opts.size < 2 && accepted.isNotEmpty()))

            fun buildDistinctSafeHints(forbidden: List<String>, qIndex: Int): Pair<String, String> {
                val h1 = com.example.model.QuizFeedbackHelper.safeGuidingHint(
                    rawHint = q.optString("guidingHint", ""),
                    forbiddenAnswers = forbidden,
                    questionIndex = qIndex,
                    isSecondAttempt = false
                )
                var h2 = com.example.model.QuizFeedbackHelper.safeGuidingHint(
                    rawHint = q.optString("secondHint", ""),
                    forbiddenAnswers = forbidden,
                    questionIndex = qIndex,
                    isSecondAttempt = true
                )
                if (h2.equals(h1, ignoreCase = true)) {
                    h2 = com.example.model.QuizFeedbackHelper.safeGuidingHint(
                        rawHint = "",
                        forbiddenAnswers = forbidden,
                        questionIndex = qIndex + 1,
                        isSecondAttempt = true
                    )
                }
                return h1 to h2
            }

            when {
                isImageChoice -> {
                    val safeCorrect = q.optInt("correctIndex", 0).coerceIn(0, imageOptions.lastIndex)
                    val correctImg = imageOptions.getOrNull(safeCorrect)
                    val forbidden = listOfNotNull(correctImg?.label, correctImg?.visualKey).filter { it.isNotBlank() }
                    val (safeHint1, safeHint2) = buildDistinctSafeHints(forbidden, i)
                    list.add(
                        QuizQuestion(
                            prompt = prompt,
                            options = emptyList(),
                            correctIndex = safeCorrect,
                            explanation = cleanExplanation,
                            questionType = QuizQuestion.TYPE_IMAGE_CHOICE,
                            sentenceTemplate = "",
                            acceptedAnswers = emptyList(),
                            matchingPairs = emptyList(),
                            orderedItems = emptyList(),
                            imageOptions = imageOptions,
                            illustrationKey = illustrationKey,
                            illustrationCaption = illustrationCaption,
                            guidingHint = safeHint1,
                            secondHint = safeHint2,
                            skillTag = skillTag
                        )
                    )
                }
                isDragMatch -> {
                    val forbidden = matchingPairs.map { it.right }
                    val (safeHint1, safeHint2) = buildDistinctSafeHints(forbidden, i)
                    list.add(
                        QuizQuestion(
                            prompt = prompt,
                            options = emptyList(),
                            correctIndex = 0,
                            explanation = cleanExplanation,
                            questionType = QuizQuestion.TYPE_DRAG_MATCH,
                            sentenceTemplate = "",
                            acceptedAnswers = emptyList(),
                            matchingPairs = matchingPairs,
                            orderedItems = emptyList(),
                            illustrationKey = illustrationKey,
                            illustrationCaption = illustrationCaption,
                            guidingHint = safeHint1,
                            secondHint = safeHint2,
                            skillTag = skillTag
                        )
                    )
                }
                isDragOrder -> {
                    val forbidden = listOf(orderedItems.joinToString(" "))
                    val (safeHint1, safeHint2) = buildDistinctSafeHints(forbidden, i)
                    list.add(
                        QuizQuestion(
                            prompt = prompt,
                            options = emptyList(),
                            correctIndex = 0,
                            explanation = cleanExplanation,
                            questionType = QuizQuestion.TYPE_DRAG_ORDER,
                            sentenceTemplate = "",
                            acceptedAnswers = emptyList(),
                            matchingPairs = emptyList(),
                            orderedItems = orderedItems,
                            illustrationKey = illustrationKey,
                            illustrationCaption = illustrationCaption,
                            guidingHint = safeHint1,
                            secondHint = safeHint2,
                            skillTag = skillTag
                        )
                    )
                }
                isFillBlank -> {
                    val (safeHint1, safeHint2) = buildDistinctSafeHints(accepted, i)
                    list.add(
                        QuizQuestion(
                            prompt = prompt,
                            options = emptyList(),
                            correctIndex = 0,
                            explanation = cleanExplanation,
                            questionType = QuizQuestion.TYPE_FILL_BLANK,
                            sentenceTemplate = sentenceTemplate,
                            acceptedAnswers = accepted,
                            matchingPairs = emptyList(),
                            orderedItems = emptyList(),
                            illustrationKey = illustrationKey,
                            illustrationCaption = illustrationCaption,
                            guidingHint = safeHint1,
                            secondHint = safeHint2,
                            skillTag = skillTag
                        )
                    )
                }
                opts.size >= 2 -> {
                    val safeCorrect = q.optInt("correctIndex", 0).coerceIn(0, opts.lastIndex)
                    val forbidden = listOfNotNull(opts.getOrNull(safeCorrect))
                    val (safeHint1, safeHint2) = buildDistinctSafeHints(forbidden, i)
                    list.add(
                        QuizQuestion(
                            prompt = prompt,
                            options = opts,
                            correctIndex = safeCorrect,
                            explanation = cleanExplanation,
                            questionType = QuizQuestion.TYPE_MCQ,
                            sentenceTemplate = "",
                            acceptedAnswers = emptyList(),
                            matchingPairs = emptyList(),
                            orderedItems = emptyList(),
                            illustrationKey = illustrationKey,
                            illustrationCaption = illustrationCaption,
                            guidingHint = safeHint1,
                            secondHint = safeHint2,
                            skillTag = skillTag
                        )
                    )
                }
            }
        }
        return list
    }

    private fun scaleBitmapForAi(bitmap: Bitmap, maxDim: Int = 1400): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDim && height <= maxDim) return bitmap
        val ratio = width.toFloat() / height.toFloat()
        val (newW, newH) = if (width >= height) {
            maxDim to (maxDim / ratio).toInt().coerceAtLeast(1)
        } else {
            (maxDim * ratio).toInt().coerceAtLeast(1) to maxDim
        }
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }

    companion object {
        private const val TAG = "GeminiTutorService"
        private const val MAX_FILE_BYTES = 15 * 1024 * 1024

        suspend fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decode image uri", e)
                null
            }
        }

        suspend fun loadCourseAttachmentFromUri(context: Context, uri: Uri): Result<CourseAttachment> =
            withContext(Dispatchers.IO) {
                try {
                    val resolver = context.contentResolver
                    var fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "document"
                    var reportedSize = -1L

                    resolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (cursor.moveToFirst()) {
                            if (nameIdx >= 0 && !cursor.isNull(nameIdx)) {
                                fileName = cursor.getString(nameIdx)
                            }
                            if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                                reportedSize = cursor.getLong(sizeIdx)
                            }
                        }
                    }

                    if (reportedSize > MAX_FILE_BYTES) {
                        return@withContext Result.failure(
                            IllegalArgumentException("Le fichier « $fileName » dépasse 15 Mo.")
                        )
                    }

                    val rawMime = resolver.getType(uri).orEmpty().lowercase()
                    val ext = fileName.substringAfterLast('.', "").lowercase()

                    val isImage = rawMime.startsWith("image/") ||
                        ext in setOf("jpg", "jpeg", "png", "webp", "bmp", "gif", "heic")
                    if (isImage) {
                        val bmp = loadBitmapFromUri(context, uri)
                            ?: return@withContext Result.failure(
                                IllegalArgumentException("Impossible de lire l'image « $fileName ».")
                            )
                        val safeMime = if (rawMime.startsWith("image/")) rawMime else "image/jpeg"
                        val size = if (reportedSize > 0L) reportedSize else bmp.byteCount.toLong()
                        return@withContext Result.success(
                            CourseAttachment(
                                fileName = fileName,
                                mimeType = safeMime,
                                sizeBytes = size,
                                bitmap = bmp
                            )
                        )
                    }

                    val bytes = resolver.openInputStream(uri)?.use { stream ->
                        val buffer = ByteArray(16 * 1024)
                        val out = java.io.ByteArrayOutputStream()
                        var total = 0
                        while (true) {
                            val read = stream.read(buffer)
                            if (read == -1) break
                            total += read
                            if (total > MAX_FILE_BYTES) {
                                return@withContext Result.failure(
                                    IllegalArgumentException("Le fichier « $fileName » dépasse 15 Mo.")
                                )
                            }
                            out.write(buffer, 0, read)
                        }
                        out.toByteArray()
                    } ?: return@withContext Result.failure(
                        IllegalArgumentException("Impossible d'ouvrir « $fileName ».")
                    )

                    if (bytes.isEmpty()) {
                        return@withContext Result.failure(
                            IllegalArgumentException("Le fichier « $fileName » est vide.")
                        )
                    }

                    val actualSize = bytes.size.toLong()

                    // PDF
                    if (rawMime == "application/pdf" || ext == "pdf") {
                        return@withContext Result.success(
                            CourseAttachment(
                                fileName = fileName,
                                mimeType = "application/pdf",
                                sizeBytes = actualSize,
                                bytes = bytes
                            )
                        )
                    }

                    // Office OpenXML / OpenDocument (DOCX, ODT, PPTX)
                    val isDocx = ext == "docx" || rawMime.contains("wordprocessingml")
                    val isOdt = ext == "odt" || rawMime.contains("opendocument.text")
                    val isPptx = ext == "pptx" || rawMime.contains("presentationml")
                    if (isDocx || isOdt || isPptx) {
                        val extracted = extractTextFromOfficeZip(bytes, ext, isDocx, isOdt, isPptx)
                        if (extracted.isBlank()) {
                            return@withContext Result.failure(
                                IllegalArgumentException("Aucun texte lisible trouvé dans « $fileName ».")
                            )
                        }
                        val officeMime = rawMime.ifBlank { "application/octet-stream" }
                        return@withContext Result.success(
                            CourseAttachment(
                                fileName = fileName,
                                mimeType = officeMime,
                                sizeBytes = actualSize,
                                extractedText = extracted.take(60_000)
                            )
                        )
                    }

                    // Plain text / Markdown / CSV / HTML / JSON / XML / RTF
                    val isTextFormat = rawMime.startsWith("text/") ||
                        rawMime in setOf("application/json", "application/xml", "application/rtf") ||
                        ext in setOf("txt", "md", "csv", "tsv", "json", "xml", "html", "htm", "rtf", "log", "tex", "py", "java", "kt")

                    if (isTextFormat || isLikelyPlainText(bytes)) {
                        val textContent = bytes.toString(Charsets.UTF_8).trim()
                        if (textContent.isBlank()) {
                            return@withContext Result.failure(
                                IllegalArgumentException("Le fichier « $fileName » ne contient pas de texte.")
                            )
                        }
                        return@withContext Result.success(
                            CourseAttachment(
                                fileName = fileName,
                                mimeType = rawMime.ifBlank { "text/plain" },
                                sizeBytes = actualSize,
                                extractedText = textContent.take(60_000)
                            )
                        )
                    }

                    Result.failure(
                        IllegalArgumentException(
                            "Format non supporté pour « $fileName ». Formats acceptés : PDF, Images, TXT, MD, CSV, DOCX, ODT, PPTX, HTML."
                        )
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to read course attachment", e)
                    Result.failure(
                        IllegalArgumentException(
                            FrenchErrorMessages.toFrenchError(e, "Erreur lors de la lecture du fichier.")
                        )
                    )
                }
            }

        private fun isLikelyPlainText(bytes: ByteArray): Boolean {
            val sampleLen = minOf(bytes.size, 2048)
            var weirdCount = 0
            for (i in 0 until sampleLen) {
                val b = bytes[i].toInt() and 0xFF
                if (b == 0) return false
                if (b < 0x20 && b != '\n'.code && b != '\r'.code && b != '\t'.code) {
                    weirdCount++
                }
            }
            return weirdCount * 20 <= sampleLen
        }

        internal fun stripJsonFences(raw: String): String {
            var trimmed = raw.trim()
            if (trimmed.startsWith("```")) {
                trimmed = trimmed.substringAfter('\n', trimmed)
                if (trimmed.endsWith("```")) {
                    trimmed = trimmed.removeSuffix("```").trim()
                }
            }
            val startIdx = trimmed.indexOf('{')
            val endIdx = trimmed.lastIndexOf('}')
            return if (startIdx in 0..endIdx) {
                trimmed.substring(startIdx, endIdx + 1)
            } else {
                trimmed
            }
        }

        internal fun normalizeChatHistoryForSdk(history: List<ChatTurnInput>): List<ChatTurnInput> {
            val cleaned = history
                .mapNotNull { turn ->
                    val t = turn.text.trim()
                    if (t.isBlank()) null else turn.copy(text = t)
                }
                .takeLast(20)

            val alternating = mutableListOf<ChatTurnInput>()
            for (turn in cleaned) {
                if (alternating.isEmpty()) {
                    if (turn.isUser) alternating.add(turn)
                } else {
                    val last = alternating.last()
                    if (last.isUser == turn.isUser) {
                        alternating[alternating.lastIndex] = last.copy(text = "${last.text}\n${turn.text}")
                    } else {
                        alternating.add(turn)
                    }
                }
            }
            // Before sending a new user turn, history must end with a model turn (or be empty)
            while (alternating.isNotEmpty() && alternating.last().isUser) {
                alternating.removeAt(alternating.lastIndex)
            }
            return alternating
        }

        internal fun extractTextFromOfficeZip(
            bytes: ByteArray,
            ext: String,
            isDocx: Boolean,
            isOdt: Boolean,
            isPptx: Boolean
        ): String {
            val sb = StringBuilder()
            ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val name = entry.name
                    val matchesTarget = when {
                        isDocx || ext == "docx" -> name == "word/document.xml"
                        isOdt || ext == "odt" -> name == "content.xml"
                        isPptx || ext == "pptx" -> name.startsWith("ppt/slides/slide") && name.endsWith(".xml")
                        else -> false
                    }
                    if (matchesTarget) {
                        val xmlBytes = zis.readBytes()
                        val parser = Xml.newPullParser()
                        parser.setInput(ByteArrayInputStream(xmlBytes), "UTF-8")
                        var eventType = parser.eventType
                        while (eventType != XmlPullParser.END_DOCUMENT) {
                            when (eventType) {
                                XmlPullParser.TEXT -> {
                                    val t = parser.text
                                    if (!t.isNullOrEmpty() && !(t.isBlank() && (t.contains('\n') || t.contains('\r')))) {
                                        sb.append(t)
                                    }
                                }
                                XmlPullParser.END_TAG -> {
                                    val localTag = parser.name?.substringAfter(':').orEmpty()
                                    if (localTag == "p" || localTag == "br" || localTag == "tr" || localTag == "h") {
                                        sb.append('\n')
                                    } else if (localTag == "tab" || localTag == "s") {
                                        sb.append(' ')
                                    }
                                }
                            }
                            eventType = parser.next()
                        }
                        sb.append('\n')
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            return sb.toString()
                .lines()
                .joinToString("\n") { it.trim() }
                .replace(Regex("\n{3,}"), "\n\n")
                .trim()
        }
    }
}
