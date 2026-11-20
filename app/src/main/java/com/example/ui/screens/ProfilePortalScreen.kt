package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.model.ChildProfile
import com.example.model.Family
import com.example.model.UserAccount
import com.example.ui.components.ChildAvatarBadge
import com.example.ui.components.MascotSpeechBubble
import com.example.ui.components.ModernCard
import com.example.ui.components.PinPadDialog
import com.example.ui.components.QuizzyStarMascot
import com.example.ui.components.QuizzyWordmarkLogo
import com.example.ui.components.SuccessRateGauge
import com.example.ui.theme.QuizzyTheme

@Composable
fun ProfilePortalScreen(
    userAccount: UserAccount,
    family: Family,
    children: List<ChildProfile>,
    pinError: String?,
    onClearPinError: () -> Unit,
    onVerifyParentPin: (String) -> Boolean,
    onVerifyChildPin: (ChildProfile, String) -> Boolean,
    onSignOut: () -> Unit
) {
    var askingParentPin by remember { mutableStateOf(false) }
    var askingChild by remember { mutableStateOf<ChildProfile?>(null) }

    val visibleChildren = remember(userAccount, children) {
        if (userAccount.role == "CHILD" && !userAccount.linkedChildId.isNullOrBlank()) {
            children.filter { it.id == userAccount.linkedChildId }
        } else {
            children
        }
    }

    val qColors = QuizzyTheme.colors

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 620.dp)
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        QuizzyWordmarkLogo(height = 30.dp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = family.familyName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = qColors.headingInk
                        )
                        Text(
                            text = "Qui se connecte aujourd'hui ?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        IconButton(
                            onClick = onSignOut,
                            modifier = Modifier.testTag("portal_sign_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Déconnexion Google"
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    QuizzyStarMascot(size = 84.dp)
                    MascotSpeechBubble(
                        text = "Prêt pour les révisions ?\nChoisis ton profil !",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Text(
                    text = "Connexions Enfants",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            if (visibleChildren.isEmpty()) {
                item {
                    ModernCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Face,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(38.dp)
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = if (userAccount.role == "CHILD") {
                                    "Ton profil enfant n'est plus disponible"
                                } else {
                                    "Aucun profil enfant pour le moment"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = if (userAccount.role == "CHILD") {
                                    "Demande à ton parent de vérifier ton profil dans l'Espace Parent ou déconnecte-toi."
                                } else {
                                    "Ouvre l'Espace Parent (Admin) ci-dessous pour ajouter tes enfants."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(visibleChildren, key = { it.id }) { child ->
                    ModernCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("portal_child_card_${child.id}"),
                        onClick = {
                            onClearPinError()
                            askingChild = child
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ChildAvatarBadge(
                                avatarKey = child.avatarKey,
                                name = child.name,
                                size = 58.dp
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = child.name,
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.padding(vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "${child.gradeLevel} • ${child.age} ans",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = "${child.lessonsCount} cours • ${child.quizzesTaken} quiz",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            SuccessRateGauge(
                                percent = child.successRatePercent,
                                hasAttempts = child.totalQuestions > 0,
                                size = 54.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = "Connexion par PIN",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (userAccount.role == "PARENT") {
                item {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Administration Parentale",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                item {
                    Surface(
                        onClick = {
                            onClearPinError()
                            askingParentPin = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("portal_parent_admin_card"),
                        shape = RoundedCornerShape(26.dp),
                        color = qColors.navyActionContainer,
                        shadowElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(qColors.navyActionOnContainer.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD34E),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Espace parents",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = qColors.navyActionOnContainer
                                )
                                Text(
                                    text = "Progrès par matière, résultats et profils",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = qColors.navyActionOnContainer.copy(alpha = 0.88f)
                                )
                            }
                            Icon(
                                imageVector = Icons.Filled.ChevronRight,
                                contentDescription = null,
                                tint = qColors.navyActionOnContainer
                            )
                        }
                    }
                }
            }
        }
    }

    if (askingParentPin) {
        PinPadDialog(
            title = "Accès Parent Admin",
            subtitle = "Saisis le code PIN administrateur à 4 chiffres.",
            errorMessage = pinError,
            onDismiss = {
                onClearPinError()
                askingParentPin = false
            },
            onPinComplete = { entered ->
                if (onVerifyParentPin(entered)) {
                    askingParentPin = false
                }
            }
        )
    }

    askingChild?.let { child ->
        PinPadDialog(
            title = "Connexion de ${child.name}",
            subtitle = "Saisis ton code PIN personnel (${child.gradeLevel}).",
            errorMessage = pinError,
            onDismiss = {
                onClearPinError()
                askingChild = null
            },
            onPinComplete = { entered ->
                if (onVerifyChildPin(child, entered)) {
                    askingChild = null
                }
            }
        )
    }
}
