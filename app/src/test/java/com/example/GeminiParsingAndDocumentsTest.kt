package com.example

import android.content.Context
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import com.example.data.ChatTurnInput
import com.example.data.GeminiTutorService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GeminiParsingAndDocumentsTest {

    private val service = GeminiTutorService()

    @Test
    fun stripJsonFencesCleansMarkdownCodeBlocksAndNoise() {
        val fenced = """
            Voici la réponse :
            ```json
            {"lessonTitle":"Fractions","questions":[]}
            ```
        """.trimIndent()
        assertEquals("""{"lessonTitle":"Fractions","questions":[]}""", GeminiTutorService.stripJsonFences(fenced))
    }

    @Test
    fun parseLessonAndQuizResponseHandlesValidMarkdownWrappedJson() {
        val raw = """
            ```json
            {
              "lessonTitle": "Les fractions simples",
              "summaryIntro": "Une fraction représente une part d'un tout.",
              "keyPoints": ["Numérateur en haut", "Dénominateur en bas"],
              "simpleExplanation": "Si tu coupes une pizza en 4 parts et en manges 1, tu as mangé 1/4.",
              "memoryTip": "Dénominateur = Dessous !",
              "quizTitle": "Quiz Fractions",
              "questions": [
                {
                  "prompt": "Dans 3/4, quel est le dénominateur ?",
                  "options": ["3", "4", "7", "1"],
                  "correctIndex": 1,
                  "explanation": "Le dénominateur est toujours en bas."
                }
              ]
            }
            ```
        """.trimIndent()

        val result = service.parseLessonAndQuizResponse(raw, "Mathématiques")
        assertTrue(result.isSuccess)
        val parsed = result.getOrThrow()
        assertEquals("Les fractions simples", parsed.lessonTitle)
        assertEquals(2, parsed.keyPoints.size)
        assertEquals(1, parsed.questions.size)
        assertEquals(1, parsed.questions.first().correctIndex)
    }

    @Test
    fun parseLessonAndQuizResponseGuardsBlankFieldsForFirestoreRules() {
        val raw = """
            {
              "lessonTitle": "   ",
              "summaryIntro": "",
              "keyPoints": ["   "],
              "simpleExplanation": "",
              "memoryTip": "",
              "quizTitle": " ",
              "questions": [
                {
                  "prompt": "Combien font 2 + 2 ?",
                  "options": ["3", "4", "5", "6"],
                  "correctIndex": 99,
                  "explanation": "2 + 2 = 4"
                }
              ]
            }
        """.trimIndent()

        val result = service.parseLessonAndQuizResponse(raw, "Mathématiques")
        assertTrue(result.isSuccess)
        val parsed = result.getOrThrow()
        // Firestore rules require size() >= 1 for these fields
        assertTrue(parsed.lessonTitle.isNotBlank())
        assertTrue(parsed.summaryIntro.isNotBlank())
        assertTrue(parsed.simpleExplanation.isNotBlank())
        assertTrue(parsed.quizTitle.isNotBlank())
        assertTrue(parsed.keyPoints.isNotEmpty() && parsed.keyPoints.first().isNotBlank())
        // Out-of-range correctIndex must be clamped into 0..lastIndex
        assertEquals(3, parsed.questions.first().correctIndex)
    }

    @Test
    fun parseLessonAndQuizResponseFailsWhenNoValidQuestionsExist() {
        val raw = """{"lessonTitle":"Test","questions":[{"prompt":"","options":["A","B"]}]}"""
        val result = service.parseLessonAndQuizResponse(raw, "Français")
        assertTrue(result.isFailure)
    }

    @Test
    fun parseLessonAndQuizResponseParsesAutoSubjectSkillsAndMixedFillBlankQuestions() {
        val raw = """
            {
              "detectedSubject": "English",
              "lessonTitle": "Le vocabulaire de la maison",
              "summaryIntro": "Révision des meubles du salon en anglais.",
              "keyPoints": ["Les noms des meubles courants", "Savoir les orthographier"],
              "simpleExplanation": "On associe chaque objet de la maison à son mot anglais.",
              "memoryTip": "Imagine ta pièce en disant chaque mot à voix haute !",
              "acquiredSkills": ["Vocabulaire de la cuisine"],
              "weakPoints": ["Orthographe des meubles du salon"],
              "evaluationSummary": "Bon niveau global, quelques mots du salon à renforcer.",
              "quizTitle": "Quiz Vocabulaire Anglais",
              "targetedGaps": ["Orthographe des meubles du salon"],
              "questions": [
                {
                  "questionType": "FILL_BLANK",
                  "prompt": "Comment écrit-on « canapé » en anglais ?",
                  "sentenceTemplate": "In the living room, we sit on the ____.",
                  "acceptedAnswers": ["sofa", "couch"],
                  "options": [],
                  "correctIndex": 0,
                  "guidingHint": "Ce mot anglais ressemble beaucoup au mot français que l'on utilise aussi parfois pour un canapé.",
                  "secondHint": "Il commence par la lettre S et compte 4 lettres.",
                  "skillTag": "Orthographe des meubles du salon",
                  "explanation": "En anglais, on dit sofa ou couch pour désigner un canapé."
                },
                {
                  "questionType": "MCQ",
                  "prompt": "Repère les verbes conjugués dans : « Le chat saute et attrape la balle. »",
                  "options": ["saute", "saute et attrape", "chat", "balle"],
                  "correctIndex": 1,
                  "guidingHint": "Relis bien toute la phrase : il n'y a pas qu'un seul verbe conjugué !",
                  "secondHint": "Cherche toutes les actions que fait le chat dans cette phrase.",
                  "skillTag": "Repérer tous les verbes conjugués",
                  "explanation": "Les deux actions du chat sont saute et attrape."
                }
              ]
            }
        """.trimIndent()

        val result = service.parseLessonAndQuizResponse(raw, com.example.model.SchoolCatalog.AUTO_SUBJECT)
        assertTrue(result.isSuccess)
        val parsed = result.getOrThrow()
        assertEquals("Anglais", parsed.detectedSubject)
        assertEquals(listOf("Vocabulaire de la cuisine"), parsed.acquiredSkills)
        assertEquals(listOf("Orthographe des meubles du salon"), parsed.weakPoints)
        assertEquals("Bon niveau global, quelques mots du salon à renforcer.", parsed.evaluationSummary)
        assertEquals(2, parsed.questions.size)

        val fillQ = parsed.questions[0]
        assertTrue(fillQ.isFillBlank)
        assertEquals(listOf("sofa", "couch"), fillQ.acceptedAnswers)
        assertTrue(fillQ.sentenceTemplate.contains("____"))
        assertTrue(fillQ.guidingHint.isNotBlank())

        val mcqQ = parsed.questions[1]
        assertTrue(!mcqQ.isFillBlank)
        assertEquals(1, mcqQ.correctIndex)
        assertTrue(mcqQ.guidingHint.contains("il n'y a pas qu'un seul verbe"))
    }

    @Test
    fun parseLessonAndQuizResponseParsesInteractiveDragMatchDragOrderAndIllustrations() {
        val raw = """
            {
              "detectedSubject": "Anglais",
              "lessonTitle": "La maison en anglais",
              "summaryIntro": "Découvrons les mots de la maison.",
              "keyPoints": ["Canapé se dit sofa", "Maison se dit house"],
              "simpleExplanation": "Apprenons le vocabulaire du quotidien.",
              "memoryTip": "Imagine ton salon en anglais.",
              "acquiredSkills": ["Vocabulaire de base"],
              "weakPoints": ["Meubles du salon"],
              "evaluationSummary": "Très bon début !",
              "quizTitle": "Mini-jeux d'anglais",
              "targetedGaps": ["Meubles du salon"],
              "questions": [
                {
                  "questionType": "DRAG_MATCH",
                  "prompt": "Associe chaque mot français à sa traduction anglaise",
                  "illustrationKey": "english",
                  "illustrationCaption": "Observe bien les objets du salon !",
                  "matchingPairs": [
                    {"left": "Canapé", "right": "Sofa"},
                    {"left": "Chat", "right": "Cat"},
                    {"left": "Maison", "right": "House"}
                  ],
                  "guidingHint": "Pense aux mots qui ressemblent parfois au français.",
                  "secondHint": "Vérifie chaque paire une par une.",
                  "skillTag": "Associer vocabulaire anglais",
                  "explanation": "Canapé correspond à Sofa, Chat à Cat et Maison à House."
                },
                {
                  "questionType": "DRAG_ORDER",
                  "prompt": "Remets les mots dans l'ordre pour former une phrase correcte",
                  "illustrationKey": "english",
                  "illustrationCaption": "Construis la phrase dans le bon ordre !",
                  "orderedItems": ["The", "cat", "sleeps", "here"],
                  "guidingHint": "Commence par le déterminant puis le sujet.",
                  "secondHint": "Place le verbe juste après le sujet.",
                  "skillTag": "Construire une phrase en anglais",
                  "explanation": "En anglais, l'ordre courant est sujet + verbe + complément."
                }
              ]
            }
        """.trimIndent()

        val parsed = service.parseLessonAndQuizResponse(raw, "Anglais").getOrThrow()
        assertEquals(2, parsed.questions.size)

        val matchQ = parsed.questions[0]
        assertTrue(matchQ.isDragMatch)
        assertEquals(3, matchQ.matchingPairs.size)
        assertEquals("english", matchQ.illustrationKey)
        assertEquals("Observe bien les objets du salon !", matchQ.illustrationCaption)

        val orderQ = parsed.questions[1]
        assertTrue(orderQ.isDragOrder)
        assertEquals(listOf("The", "cat", "sleeps", "here"), orderQ.orderedItems)
        assertEquals("Construis la phrase dans le bon ordre !", orderQ.illustrationCaption)
    }

    @Test
    fun parseLessonAndQuizResponseParsesImageChoiceAndBlocksSubtleGiveawayHints() {
        val raw = """
            {
              "detectedSubject": "Anglais",
              "lessonTitle": "Objets de la maison",
              "summaryIntro": "Associe les images aux mots anglais.",
              "keyPoints": ["Observer les objets du quotidien"],
              "simpleExplanation": "Chaque objet a un nom en anglais.",
              "memoryTip": "Visualise la pièce.",
              "quizTitle": "Quiz Images Maison",
              "questions": [
                {
                  "questionType": "IMAGE_CHOICE",
                  "prompt": "Clique sur l'image qui correspond au mot anglais « Sofa »",
                  "illustrationKey": "english",
                  "illustrationCaption": "Observe bien chaque meuble dessiné !",
                  "imageOptions": [
                    {"label": "Canapé", "visualKey": "sofa"},
                    {"label": "Chaise", "visualKey": "chair"},
                    {"label": "Lit", "visualKey": "bed"},
                    {"label": "Table", "visualKey": "table"}
                  ],
                  "correctIndex": 0,
                  "guidingHint": "Pense à l'endroit du salon où toute la famille peut s'asseoir ensemble.",
                  "secondHint": "Ce mot commence par C et a 6 lettres.",
                  "skillTag": "Reconnaître le mobilier en anglais",
                  "explanation": "Sofa désigne le canapé dans le salon."
                }
              ]
            }
        """.trimIndent()

        val parsed = service.parseLessonAndQuizResponse(raw, "Anglais").getOrThrow()
        assertEquals(1, parsed.questions.size)
        val imgQ = parsed.questions[0]
        assertTrue(imgQ.isImageChoice)
        assertEquals(4, imgQ.imageOptions.size)
        assertEquals("sofa", imgQ.imageOptions[0].visualKey)
        assertEquals("Canapé", imgQ.imageOptions[0].label)
        assertEquals(0, imgQ.correctIndex)
        // Subtle hint 1 is preserved
        assertTrue(imgQ.guidingHint.contains("salon"))
        // Giveaway secondHint ("commence par C et a 6 lettres") MUST be blocked and replaced with a distinct subtle hint!
        assertTrue(!imgQ.secondHint.contains("commence par", ignoreCase = true))
        assertTrue(!imgQ.secondHint.contains("6 lettres", ignoreCase = true))
        assertTrue(!imgQ.secondHint.equals(imgQ.guidingHint, ignoreCase = true))
    }

    @Test
    fun parseQuizResponseParsesTenQuestionSundayReviewQuizWithMixedInteractiveFormats() {
        val raw = """
            {
              "quizTitle": "Quiz Révision du Dimanche",
              "targetedGaps": ["Fractions", "Verbes conjugués", "Vocabulaire anglais"],
              "questions": [
                {
                  "questionType": "IMAGE_CHOICE",
                  "prompt": "Clique sur la figure qui représente un angle droit.",
                  "illustrationKey": "math",
                  "imageOptions": [
                    {"label": "Angle droit", "visualKey": "right_angle"},
                    {"label": "Cercle", "visualKey": "circle"},
                    {"label": "Triangle", "visualKey": "triangle"},
                    {"label": "Carré", "visualKey": "square"}
                  ],
                  "correctIndex": 0,
                  "guidingHint": "Pense au coin d'une feuille de cahier.",
                  "secondHint": "Imagine l'outil géométrique qui sert à vérifier les coins.",
                  "skillTag": "Angles droits",
                  "explanation": "L'angle droit forme un coin parfait."
                },
                {
                  "questionType": "FILL_BLANK",
                  "prompt": "Comment écrit-on « table » en anglais ?",
                  "sentenceTemplate": "Put the book on the ____.",
                  "acceptedAnswers": ["table"],
                  "illustrationKey": "english",
                  "guidingHint": "Ce meuble de la salle à manger porte le même nom qu'en français.",
                  "secondHint": "Réfléchis aux meubles autour desquels on mange.",
                  "skillTag": "Vocabulaire anglais",
                  "explanation": "Le mot table s'écrit pareil en anglais."
                },
                {
                  "questionType": "DRAG_MATCH",
                  "prompt": "Associe chaque fraction à sa lecture.",
                  "illustrationKey": "math",
                  "matchingPairs": [
                    {"left": "1/2", "right": "Un demi"},
                    {"left": "1/4", "right": "Un quart"},
                    {"left": "1/3", "right": "Un tiers"}
                  ],
                  "guidingHint": "Regarde le nombre du bas pour savoir en combien de parts on partage.",
                  "secondHint": "Associe d'abord la moitié, puis le partage en quatre.",
                  "skillTag": "Fractions",
                  "explanation": "Le dénominateur indique le nom de la fraction."
                },
                {
                  "questionType": "DRAG_ORDER",
                  "prompt": "Classe ces fractions de la plus petite à la plus grande.",
                  "illustrationKey": "math",
                  "orderedItems": ["1/4", "1/2", "3/4"],
                  "guidingHint": "Imagine une tarte coupée en quatre parts égales.",
                  "secondHint": "Compare combien de parts tu prends à chaque fois.",
                  "skillTag": "Comparer des fractions",
                  "explanation": "Un quart est plus petit que deux quarts (un demi) et trois quarts."
                },
                {
                  "questionType": "MCQ",
                  "prompt": "Repère les verbes conjugués dans : « Le vent souffle et les feuilles dansent. »",
                  "options": ["souffle", "souffle et dansent", "vent", "feuilles"],
                  "correctIndex": 1,
                  "illustrationKey": "french",
                  "guidingHint": "Relis toute la phrase : il n'y a pas qu'un seul verbe conjugué !",
                  "secondHint": "Cherche toutes les actions décrites dans la phrase.",
                  "skillTag": "Verbes conjugués",
                  "explanation": "Les actions sont souffle et dansent."
                },
                {
                  "questionType": "IMAGE_CHOICE",
                  "prompt": "Clique sur l'image qui correspond au mot anglais « Cat ».",
                  "illustrationKey": "english",
                  "imageOptions": [
                    {"label": "Chat", "visualKey": "cat"},
                    {"label": "Chien", "visualKey": "dog"},
                    {"label": "Livre", "visualKey": "book"},
                    {"label": "Soleil", "visualKey": "sun"}
                  ],
                  "correctIndex": 0,
                  "guidingHint": "C'est un animal de compagnie qui miaule.",
                  "secondHint": "Observe les animaux proposés.",
                  "skillTag": "Animaux en anglais",
                  "explanation": "Cat désigne le chat."
                },
                {
                  "questionType": "FILL_BLANK",
                  "prompt": "Complète la multiplication : 6 × 7 = ?",
                  "sentenceTemplate": "6 × 7 = ____",
                  "acceptedAnswers": ["42"],
                  "illustrationKey": "math",
                  "guidingHint": "Appuie-toi sur 6 × 6 puis ajoute encore 6.",
                  "secondHint": "Vérifie en comptant de 6 en 6 sept fois.",
                  "skillTag": "Tables de multiplication",
                  "explanation": "6 × 7 = 42."
                },
                {
                  "questionType": "MCQ",
                  "prompt": "Quel est le sujet dans « Les élèves préparent leur exposé » ?",
                  "options": ["Les élèves", "préparent", "leur", "exposé"],
                  "correctIndex": 0,
                  "illustrationKey": "french",
                  "guidingHint": "Pose-toi la question : qui fait l'action ?",
                  "secondHint": "Repère d'abord le verbe puis regarde qui agit.",
                  "skillTag": "Identifier le sujet",
                  "explanation": "Ce sont les élèves qui préparent l'exposé."
                },
                {
                  "questionType": "DRAG_ORDER",
                  "prompt": "Remets les étapes du cycle de l'eau dans l'ordre.",
                  "illustrationKey": "science",
                  "orderedItems": ["Évaporation", "Condensation", "Précipitations"],
                  "guidingHint": "L'eau commence par monter grâce à la chaleur du soleil.",
                  "secondHint": "Les nuages se forment avant que la pluie ne tombe.",
                  "skillTag": "Cycle de l'eau",
                  "explanation": "L'eau s'évapore, se condense en nuages puis retombe."
                },
                {
                  "questionType": "MCQ",
                  "prompt": "Dans 5/8, quel est le numérateur ?",
                  "options": ["5", "8", "13", "3"],
                  "correctIndex": 0,
                  "illustrationKey": "math",
                  "guidingHint": "Regarde la position du nombre par rapport à la barre de fraction.",
                  "secondHint": "Le dénominateur est dessous, cherche l'autre nombre.",
                  "skillTag": "Numérateur et dénominateur",
                  "explanation": "Le numérateur est le nombre du haut."
                }
              ]
            }
        """.trimIndent()

        val parsed = service.parseQuizResponse(raw, "Révision hebdomadaire").getOrThrow()
        assertEquals("Quiz Révision du Dimanche", parsed.quizTitle)
        assertEquals(10, parsed.questions.size)
        assertEquals(3, parsed.targetedGaps.size)
        assertTrue(parsed.questions.any { it.isImageChoice })
        assertTrue(parsed.questions.any { it.isDragMatch })
        assertTrue(parsed.questions.any { it.isDragOrder })
        assertTrue(parsed.questions.any { it.isFillBlank })
        assertTrue(parsed.questions.all { it.guidingHint.isNotBlank() && it.secondHint.isNotBlank() })
    }

    @Test
    fun normalizeChatHistoryFixesFailedTurnOrphanUserAndMergesConsecutiveRoles() {
        val brokenHistory = listOf(
            ChatTurnInput(isUser = false, text = "Bonjour orphelin"),
            ChatTurnInput(isUser = true, text = "Question 1"),
            ChatTurnInput(isUser = true, text = "Précision 1"),
            ChatTurnInput(isUser = false, text = "Réponse 1"),
            ChatTurnInput(isUser = true, text = "Question échouée sans réponse")
        )

        val normalized = GeminiTutorService.normalizeChatHistoryForSdk(brokenHistory)
        assertEquals(2, normalized.size)
        assertTrue(normalized[0].isUser)
        assertEquals("Question 1\nPrécision 1", normalized[0].text)
        assertTrue(!normalized[1].isUser)
        assertEquals("Réponse 1", normalized[1].text)
    }

    @Test
    fun extractTextFromDocxPreservesParagraphAndTabBreaksWithXmlNamespaces() {
        val docxXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
              <w:body>
                <w:p><w:r><w:t>Chapitre 1 : Les fractions</w:t></w:r></w:p>
                <w:p><w:r><w:t>Numérateur</w:t><w:tab/><w:t>en haut</w:t></w:r></w:p>
                <w:p><w:r><w:t>Dénominateur</w:t><w:br/><w:t>en bas</w:t></w:r></w:p>
              </w:body>
            </w:document>
        """.trimIndent()

        val zipBytes = createZipBytes("word/document.xml" to docxXml)
        val extracted = GeminiTutorService.extractTextFromOfficeZip(
            bytes = zipBytes,
            ext = "docx",
            isDocx = true,
            isOdt = false,
            isPptx = false
        )

        assertTrue("Expected paragraph break in DOCX: $extracted", extracted.contains("Chapitre 1 : Les fractions\nNumérateur en haut"))
        assertTrue("Expected line break in DOCX: $extracted", extracted.contains("Dénominateur\nen bas"))
    }

    @Test
    fun extractTextFromOdtAndPptxPreservesStructure() {
        val odtXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <office:document-content xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
                xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0">
              <office:body><office:text>
                <text:h>Histoire de France</text:h>
                <text:p>La Révolution de 1789.</text:p>
              </office:text></office:body>
            </office:document-content>
        """.trimIndent()
        val odtExtracted = GeminiTutorService.extractTextFromOfficeZip(
            bytes = createZipBytes("content.xml" to odtXml),
            ext = "odt",
            isDocx = false,
            isOdt = true,
            isPptx = false
        )
        assertEquals("Histoire de France\nLa Révolution de 1789.", odtExtracted)

        val pptxXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <p:sld xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main"
                   xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">
              <p:cSld><p:spTree>
                <a:p><a:r><a:t>Slide 1</a:t></a:r></a:p>
                <a:p><a:r><a:t>Photosynthèse</a:t></a:r></a:p>
              </p:spTree></p:cSld>
            </p:sld>
        """.trimIndent()
        val pptxExtracted = GeminiTutorService.extractTextFromOfficeZip(
            bytes = createZipBytes("ppt/slides/slide1.xml" to pptxXml),
            ext = "pptx",
            isDocx = false,
            isOdt = false,
            isPptx = true
        )
        assertEquals("Slide 1\nPhotosynthèse", pptxExtracted)
    }

    @Test
    fun fileProviderSupportsBothLessonPhotosAndStudioPhotosDirectories() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val authority = "${context.packageName}.fileprovider"

        val lessonDir = File(context.cacheDir, "lesson_photos").apply { mkdirs() }
        val lessonFile = File(lessonDir, "course_test.jpg").apply { writeText("img") }
        val lessonUri = FileProvider.getUriForFile(context, authority, lessonFile)
        assertNotNull(lessonUri)

        val studioDir = File(context.cacheDir, "studio_photos").apply { mkdirs() }
        val studioFile = File(studioDir, "studio_test.jpg").apply { writeText("img") }
        val studioUri = FileProvider.getUriForFile(context, authority, studioFile)
        assertNotNull(studioUri)
    }

    private fun createZipBytes(vararg entries: Pair<String, String>): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            entries.forEach { (name, content) ->
                zos.putNextEntry(ZipEntry(name))
                zos.write(content.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
        }
        return baos.toByteArray()
    }
}
