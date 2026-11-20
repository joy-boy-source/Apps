package com.example

import com.example.data.FrenchErrorMessages
import com.google.firebase.firestore.FirebaseFirestoreException
import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.SocketTimeoutException
import java.net.UnknownHostException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FrenchErrorMessagesTest {

    @Test
    fun highDemandEnglishErrorIsTranslatedToFrench() {
        val raw = Exception(
            "This model is currently experiencing high demand. Spikes in demand are usually temporary. Please try again later."
        )
        val result = FrenchErrorMessages.toFrenchError(raw, "Erreur par défaut.")
        assertTrue(result.contains("très sollicité") || result.contains("Réessaie"))
        assertFalse(result.lowercase().contains("high demand"))
        assertFalse(result.lowercase().contains("please try again"))
    }

    @Test
    fun unknownHostExceptionIsTranslatedToFrench() {
        val result = FrenchErrorMessages.toFrenchError(
            UnknownHostException("Unable to resolve host"),
            "Erreur."
        )
        assertTrue(result.contains("connexion Internet"))
    }

    @Test
    fun socketTimeoutExceptionIsTranslatedToFrench() {
        val result = FrenchErrorMessages.toFrenchError(
            SocketTimeoutException("Read timed out"),
            "Erreur."
        )
        assertTrue(result.contains("trop de temps"))
    }

    @Test
    fun firestoreExceptionsAreTranslatedToFrench() {
        val permDenied = FirebaseFirestoreException(
            "PERMISSION_DENIED: Missing or insufficient permissions.",
            FirebaseFirestoreException.Code.PERMISSION_DENIED
        )
        val unavail = FirebaseFirestoreException(
            "Service unavailable",
            FirebaseFirestoreException.Code.UNAVAILABLE
        )
        val unauth = FirebaseFirestoreException(
            "Unauthenticated",
            FirebaseFirestoreException.Code.UNAUTHENTICATED
        )

        assertTrue(FrenchErrorMessages.toFrenchError(permDenied, "Fallback").contains("Accès refusé"))
        assertTrue(FrenchErrorMessages.toFrenchError(unavail, "Fallback").contains("indisponible"))
        assertTrue(FrenchErrorMessages.toFrenchError(unauth, "Fallback").contains("session a expiré"))
    }

    @Test
    fun jsonAndPayloadAndAppCheckErrorsAreTranslatedToFrench() {
        val jsonErr = FrenchErrorMessages.toFrenchError(JSONException("End of input at character 0"), "Fallback")
        assertTrue(jsonErr.contains("incomplète"))

        val payloadErr = FrenchErrorMessages.toFrenchError(Exception("HTTP 413 Payload Too Large"), "Fallback")
        assertTrue(payloadErr.contains("trop volumineux"))

        val appCheckErr = FrenchErrorMessages.toFrenchError(Exception("403 App Check token rejected"), "Fallback")
        assertTrue(appCheckErr.contains("sécurité"))

        val unsupportedErr = FrenchErrorMessages.toFrenchError(Exception("INVALID_ARGUMENT: unable to process input"), "Fallback")
        assertTrue(unsupportedErr.contains("n'a pas pu être lu"))
    }

    @Test
    fun unknownEnglishSdkMessageFallsBackToFrench() {
        val result = FrenchErrorMessages.toFrenchError(
            RuntimeException("Unexpected internal error occurred on server"),
            "Impossible d'analyser ce cours."
        )
        assertEquals("Impossible d'analyser ce cours.", result)
    }

    @Test
    fun frenchMessageIsPreserved() {
        val msg = "Le fichier « maths.pdf » dépasse 15 Mo."
        val result = FrenchErrorMessages.toFrenchError(IllegalArgumentException(msg), "Erreur.")
        assertEquals(msg, result)
    }
}
