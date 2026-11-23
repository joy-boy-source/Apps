package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.data.attemptAutoSignIn
import com.example.data.onGoogleSignInClicked
import com.example.ui.components.MascotSpeechBubble
import com.example.ui.components.QuizzyInlineLoader
import com.example.ui.components.QuizzyMascotMood
import com.example.ui.components.QuizzyStarMascot
import com.example.ui.components.QuizzyWordmarkLogo
import com.example.ui.theme.QuizzyTheme

@Composable
fun AuthScreen(
    initialRoleHint: String = "PARENT",
    onRoleHintChanged: (String) -> Unit = {},
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var selectedRole by remember(initialRoleHint) { mutableStateOf(initialRoleHint) }
    var isLoading by remember { mutableStateOf(false) }
    var isCheckingAutoSignIn by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                isCheckingAutoSignIn = false
                onAuthSuccess()
            },
            onUnauthenticated = {
                isCheckingAutoSignIn = false
            },
            scope = scope
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val qColors = QuizzyTheme.colors
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = qColors.pastelMint.copy(alpha = 0.72f),
                radius = size.width * 0.42f,
                center = Offset(size.width * 0.92f, size.height * 0.06f)
            )
            drawCircle(
                color = qColors.pastelPeach.copy(alpha = 0.58f),
                radius = size.width * 0.36f,
                center = Offset(size.width * 0.04f, size.height * 0.92f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .widthIn(max = 520.dp)
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(10.dp))

            // Welcoming Brand Identity (official transparent light/dark Quizzy logo)
            QuizzyWordmarkLogo(height = 56.dp)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Révise tes cours et progresse en famille",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(18.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                QuizzyStarMascot(size = 112.dp, mood = QuizzyMascotMood.WAVE)
                Spacer(Modifier.width(4.dp))
                MascotSpeechBubble(
                    text = "Bienvenue !\nQui se connecte ?"
                )
            }

            Spacer(Modifier.height(26.dp))

            // Two Large, Visually Distinct Role Cards: "Je suis un enfant" & "Je suis un parent"
            RoleChoiceCard(
                title = "Je suis un enfant",
                subtitle = "Mes matières, synthèses et quiz du jour",
                icon = Icons.Filled.Face,
                selected = selectedRole == "CHILD",
                pastelSurface = qColors.pastelMint,
                accentColor = MaterialTheme.colorScheme.primary,
                onAccentColor = MaterialTheme.colorScheme.onPrimary,
                onClick = {
                    selectedRole = "CHILD"
                    onRoleHintChanged("CHILD")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_role_child")
            )

            Spacer(Modifier.height(14.dp))

            RoleChoiceCard(
                title = "Je suis un parent",
                subtitle = "Espace parents, progrès par matière et suivi",
                icon = Icons.Filled.Group,
                selected = selectedRole == "PARENT",
                pastelSurface = qColors.parentSkyContainer,
                accentColor = qColors.parentSkyAccent,
                onAccentColor = Color.White,
                onClick = {
                    selectedRole = "PARENT"
                    onRoleHintChanged("PARENT")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_role_parent")
            )

            if (!errorMessage.isNullOrBlank()) {
                Spacer(Modifier.height(16.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(14.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(26.dp))

            val ctaColor by animateColorAsState(
                targetValue = if (selectedRole == "PARENT") qColors.navyActionContainer else MaterialTheme.colorScheme.primary,
                label = "auth_cta_color"
            )
            val ctaContentColor by animateColorAsState(
                targetValue = if (selectedRole == "PARENT") qColors.navyActionOnContainer else MaterialTheme.colorScheme.onPrimary,
                label = "auth_cta_content_color"
            )

            Button(
                onClick = {
                    errorMessage = null
                    isLoading = true
                    onGoogleSignInClicked(
                        context = context,
                        credentialManager = credentialManager,
                        onAuthSuccess = {
                            isLoading = false
                            onAuthSuccess()
                        },
                        onAuthError = { msg ->
                            isLoading = false
                            errorMessage = msg
                        },
                        scope = scope,
                        onAuthCancelled = {
                            isLoading = false
                        }
                    )
                },
                enabled = !isLoading && !isCheckingAutoSignIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .testTag("google_sign_in_button"),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ctaColor,
                    contentColor = ctaContentColor
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                if (isLoading || isCheckingAutoSignIn) {
                    QuizzyInlineLoader(
                        size = 20.dp,
                        tint = ctaContentColor
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = if (isCheckingAutoSignIn) "Reconnexion en cours..." else "Connexion Google...",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Login,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = if (selectedRole == "PARENT") "Continuer en tant que parent"
                        else "Continuer en tant qu'enfant",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Connexion sécurisée avec votre compte Google",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun RoleChoiceCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    pastelSurface: Color,
    accentColor: Color,
    onAccentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val qColors = QuizzyTheme.colors
    val containerColor by animateColorAsState(
        targetValue = if (selected) pastelSurface else MaterialTheme.colorScheme.surface,
        label = "role_card_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) accentColor else MaterialTheme.colorScheme.outlineVariant,
        label = "role_card_border"
    )

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(26.dp),
        color = containerColor,
        border = BorderStroke(if (selected) 2.2.dp else 1.dp, borderColor),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (selected) accentColor else pastelSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) onAccentColor else accentColor,
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = qColors.headingInk
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = qColors.secondaryInk
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Sélectionné",
                    tint = accentColor,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}
