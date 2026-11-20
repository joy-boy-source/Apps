package com.example.data

import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.TimeoutCancellationException
import org.json.JSONException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Centralise la traduction systématique de toute erreur technique ou SDK
 * en message clair, rassurant et 100 % en français pour l'interface utilisateur.
 */
object FrenchErrorMessages {

    private val englishMarkers = listOf(
        "this model",
        "currently experiencing",
        "high demand",
        "spikes in demand",
        "usually temporary",
        "please try again",
        "try again later",
        "service unavailable",
        "unavailable",
        "overloaded",
        "resource exhausted",
        "rate limit",
        "quota",
        "permission_denied",
        "permission denied",
        "missing or insufficient permissions",
        "unauthenticated",
        "unable to resolve host",
        "failed to connect",
        "network error",
        "timeout",
        "timed out",
        "deadline exceeded",
        "invalid argument",
        "bad request",
        "internal error",
        "unknown error",
        "exception",
        "credential",
        "canceled",
        "cancelled",
        "forbidden",
        "not found",
        "socket"
    )

    fun toFrenchError(error: Throwable?, fallback: String): String {
        if (error == null) return normalizeMessage(null, fallback)

        val chain = generateSequence(error) { it.cause }.take(10).toList()

        if (chain.any { it is TimeoutCancellationException || it is SocketTimeoutException || it is InterruptedIOException }) {
            return "L'opération a pris trop de temps. Réessaie dans un instant."
        }

        if (chain.any { it is UnknownHostException || it is ConnectException || it is SocketException || it is SSLException }) {
            return "Problème de connexion Internet. Vérifie ton réseau et réessaie."
        }

        if (chain.any { it is QuotaExceededException }) {
            return "Le service d'IA est très sollicité en ce moment. Patiente quelques secondes puis réessaie."
        }

        val firestoreEx = chain.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
        if (firestoreEx != null) {
            return when (firestoreEx.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    "Accès refusé par le serveur. Vérifie que tu es bien connecté au bon foyer."
                FirebaseFirestoreException.Code.UNAVAILABLE,
                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                    "Serveur temporairement indisponible. Vérifie ta connexion Internet et réessaie."
                FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                    "Ta session a expiré. Reconnecte-toi avec Google."
                FirebaseFirestoreException.Code.NOT_FOUND ->
                    "Élément introuvable ou supprimé."
                FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED ->
                    "Trop de requêtes envoyées. Patiente quelques instants avant de réessayer."
                else -> "Erreur de synchronisation avec le foyer. Réessaie dans un instant."
            }
        }

        if (chain.any { it is JSONException }) {
            return "La réponse reçue était incomplète. Réessaie dans un instant."
        }

        val combinedText = chain
            .mapNotNull { it.message }
            .joinToString(" | ")

        return normalizeMessage(combinedText, fallback)
    }

    fun normalizeMessage(rawMessage: String?, fallback: String): String {
        val trimmed = rawMessage?.trim().orEmpty()
        if (trimmed.isBlank()) return fallback

        val lower = trimmed.lowercase()

        return when {
            lower.contains("high demand") ||
                lower.contains("spikes in demand") ||
                lower.contains("temporarily") ||
                lower.contains("overloaded") ||
                lower.contains("503") ||
                lower.contains("service unavailable") ||
                lower.contains("try again later") ->
                "Le service d'IA est très sollicité en ce moment. Réessaie dans quelques secondes."

            lower.contains("quota") ||
                lower.contains("rate limit") ||
                lower.contains("resource_exhausted") ||
                lower.contains("resource exhausted") ||
                lower.contains("429") ->
                "Limite temporaire atteinte. Patiente quelques instants avant de réessayer."

            lower.contains("unable to resolve host") ||
                lower.contains("failed to connect") ||
                lower.contains("network") ||
                lower.contains("offline") ||
                lower.contains("connection reset") ||
                lower.contains("software caused connection abort") ->
                "Problème de connexion Internet. Vérifie ton réseau et réessaie."

            lower.contains("timed out") ||
                lower.contains("timeout") ||
                lower.contains("deadline exceeded") ->
                "L'opération a pris trop de temps. Réessaie dans un instant."

            lower.contains("app check") ||
                lower.contains("unauthenticated") ||
                lower.contains("401") ||
                lower.contains("403") ->
                "Vérification de sécurité expirée ou refusée. Redémarre l'application et réessaie."

            lower.contains("permission_denied") ||
                lower.contains("missing or insufficient permissions") ||
                lower.contains("permission denied") ->
                "Accès refusé par le serveur. Vérifie que tu es bien connecté au bon foyer."

            lower.contains("413") ||
                lower.contains("payload too large") ||
                lower.contains("request entity too large") ->
                "Le fichier envoyé est trop volumineux. Essaie avec un document plus léger."

            lower.contains("invalid_argument") ||
                lower.contains("invalid argument") ||
                lower.contains("unsupported") ||
                lower.contains("unable to process") ||
                lower.contains("cannot decode") ->
                "Ce fichier ou ce format n'a pas pu être lu par l'assistant. Essaie un autre document ou une photo nette."

            lower.contains("credential") ||
                lower.contains("google id") ||
                lower.contains("developer_error") ||
                lower.contains("apiexception") ->
                "Connexion Google impossible pour le moment. Vérifie ton compte Google et réessaie."

            looksFrench(trimmed) && englishMarkers.none { lower.contains(it) } ->
                trimmed

            else -> fallback
        }
    }

    private fun looksFrench(text: String): Boolean {
        val lower = text.lowercase()
        val hasFrenchAccents = text.any { it in "éèêëàâäîïôöùûüçœ«»" }
        val frenchHints = listOf(
            "le ", "la ", "les ", "l'", "un ", "une ", "des ", "du ",
            "pas ", "pour ", "dans ", "avec ", "erreur", "impossible",
            "réessaie", "fichier", "cours", "quiz", "foyer", "code",
            "utilisateur", "connexion", "transcription", "image", "audio",
            "trop ", "veuillez", "aucun", "invalide", "expiré"
        )
        return hasFrenchAccents || frenchHints.any { lower.contains(it) }
    }
}
