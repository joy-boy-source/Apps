package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.model.SchoolCatalog
import com.example.ui.components.ChildAvatarBadge
import com.example.ui.components.GradeLevelSelector
import com.example.ui.components.ModernCard
import com.example.ui.components.QuizzyInlineLoader
import com.example.ui.components.QuizzyWordmarkLogo
import com.example.ui.viewmodel.OperationStatus
import kotlin.math.roundToInt

@Composable
fun FamilySetupScreen(
    opStatus: OperationStatus,
    initialRole: String = "PARENT",
    onClearOpStatus: () -> Unit = {},
    onCreateParentFamily: (
        familyName: String,
        parentPin: String,
        firstChildName: String,
        firstChildAge: Int,
        firstChildGrade: String,
        firstChildAvatar: String,
        firstChildPin: String
    ) -> Unit,
    onJoinAsChild: (
        inviteCode: String,
        childName: String,
        age: Int,
        gradeLevel: String,
        avatarKey: String,
        childPin: String
    ) -> Unit,
    onSignOut: () -> Unit
) {
    var selectedRole by remember(initialRole) { mutableStateOf(initialRole) }

    // Parent setup fields
    var familyName by remember { mutableStateOf("Notre Famille") }
    var parentPin by remember { mutableStateOf("") }
    var firstChildName by remember { mutableStateOf("") }
    var firstChildGrade by remember { mutableStateOf("CM1") }
    var firstChildAge by remember { mutableIntStateOf(9) }
    var firstChildAvatar by remember { mutableStateOf("owl") }
    var firstChildPin by remember { mutableStateOf("") }

    // Child join fields
    var inviteCode by remember { mutableStateOf("") }
    var joinChildName by remember { mutableStateOf("") }
    var joinChildGrade by remember { mutableStateOf("5ème") }
    var joinChildAge by remember { mutableIntStateOf(12) }
    var joinChildAvatar by remember { mutableStateOf("astronaut") }
    var joinChildPin by remember { mutableStateOf("") }

    val isBusy = opStatus is OperationStatus.Busy

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
                .widthIn(max = 600.dp)
                .align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Bienvenue sur",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        QuizzyWordmarkLogo(height = 30.dp)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Configure ton accès en 30 secondes",
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
                        modifier = Modifier.testTag("setup_sign_out_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Déconnexion")
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RoleSelectorCard(
                    selected = selectedRole == "PARENT",
                    title = "Je suis Parent",
                    subtitle = "Créer le foyer (Admin)",
                    icon = Icons.Filled.AdminPanelSettings,
                    onClick = {
                        if (selectedRole != "PARENT") {
                            selectedRole = "PARENT"
                            onClearOpStatus()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("role_parent_card")
                )
                RoleSelectorCard(
                    selected = selectedRole == "CHILD",
                    title = "Je suis Enfant",
                    subtitle = "Rejoindre un foyer",
                    icon = Icons.Filled.Face,
                    onClick = {
                        if (selectedRole != "CHILD") {
                            selectedRole = "CHILD"
                            onClearOpStatus()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("role_child_card")
                )
            }

            Spacer(Modifier.height(20.dp))

            if (opStatus is OperationStatus.Error) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = opStatus.message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            if (selectedRole == "PARENT") {
                ModernCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Espace Administrateur Parent",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(Modifier.height(14.dp))

                        OutlinedTextField(
                            value = familyName,
                            onValueChange = { familyName = it },
                            label = { Text("Nom de la famille") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("family_name_input")
                        )

                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = parentPin,
                            onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) parentPin = it },
                            label = { Text("Code PIN Parent Admin (4 chiffres)") },
                            supportingText = { Text("Protège le tableau de bord parental.") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("parent_pin_input")
                        )

                        Spacer(Modifier.height(18.dp))
                        Text(
                            text = "Premier profil enfant (optionnel)",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(8.dp))

                        OutlinedTextField(
                            value = firstChildName,
                            onValueChange = { firstChildName = it },
                            label = { Text("Prénom de l'enfant") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("first_child_name_input")
                        )

                        if (firstChildName.isNotBlank()) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "Niveau scolaire :",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Spacer(Modifier.height(6.dp))
                            GradeLevelSelector(
                                selectedGrade = firstChildGrade,
                                onSelect = {
                                    firstChildGrade = it
                                    firstChildAge = SchoolCatalog.suggestedAgeForGrade(it)
                                }
                            )

                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "Âge : $firstChildAge ans",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Slider(
                                value = firstChildAge.toFloat(),
                                onValueChange = { firstChildAge = it.roundToInt().coerceIn(5, 18) },
                                valueRange = 5f..18f,
                                steps = 12
                            )

                            Spacer(Modifier.height(8.dp))
                            Text("Avatar :", style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.height(6.dp))
                            AvatarPickerRow(
                                selectedAvatar = firstChildAvatar,
                                onSelect = { firstChildAvatar = it }
                            )

                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = firstChildPin,
                                onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) firstChildPin = it },
                                label = { Text("Code PIN de l'enfant (4 chiffres)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("first_child_pin_input")
                            )
                        }

                        Spacer(Modifier.height(22.dp))
                        val canSubmitParent = familyName.isNotBlank() &&
                            parentPin.length == 4 &&
                            (firstChildName.isBlank() || firstChildPin.length == 4)

                        Button(
                            onClick = {
                                onCreateParentFamily(
                                    familyName,
                                    parentPin,
                                    firstChildName,
                                    firstChildAge,
                                    firstChildGrade,
                                    firstChildAvatar,
                                    firstChildPin
                                )
                            },
                            enabled = canSubmitParent && !isBusy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("create_family_button"),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            if (isBusy) {
                                QuizzyInlineLoader(
                                    size = 20.dp,
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("Créer l'espace famille")
                            }
                        }
                    }
                }
            } else {
                ModernCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Rejoindre ma famille",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Demande le code à 6 caractères visible dans l'Espace Parent.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(14.dp))

                        OutlinedTextField(
                            value = inviteCode,
                            onValueChange = { if (it.length <= 6) inviteCode = it.uppercase() },
                            label = { Text("Code Famille (6 caractères)") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("invite_code_input")
                        )

                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = joinChildName,
                            onValueChange = { joinChildName = it },
                            label = { Text("Ton prénom") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("join_child_name_input")
                        )

                        Spacer(Modifier.height(12.dp))
                        Text("Ton niveau scolaire :", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(6.dp))
                        GradeLevelSelector(
                            selectedGrade = joinChildGrade,
                            onSelect = {
                                joinChildGrade = it
                                joinChildAge = SchoolCatalog.suggestedAgeForGrade(it)
                            }
                        )

                        Spacer(Modifier.height(10.dp))
                        Text("Ton âge : $joinChildAge ans", style = MaterialTheme.typography.labelLarge)
                        Slider(
                            value = joinChildAge.toFloat(),
                            onValueChange = { joinChildAge = it.roundToInt().coerceIn(5, 18) },
                            valueRange = 5f..18f,
                            steps = 12
                        )

                        Spacer(Modifier.height(8.dp))
                        Text("Ton avatar :", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(6.dp))
                        AvatarPickerRow(
                            selectedAvatar = joinChildAvatar,
                            onSelect = { joinChildAvatar = it }
                        )

                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = joinChildPin,
                            onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) joinChildPin = it },
                            label = { Text("Ton code PIN secret (4 chiffres)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("join_child_pin_input")
                        )

                        Spacer(Modifier.height(22.dp))
                        val canJoin = inviteCode.trim().length == 6 &&
                            joinChildName.isNotBlank() &&
                            joinChildPin.length == 4

                        Button(
                            onClick = {
                                onJoinAsChild(
                                    inviteCode,
                                    joinChildName,
                                    joinChildAge,
                                    joinChildGrade,
                                    joinChildAvatar,
                                    joinChildPin
                                )
                            },
                            enabled = canJoin && !isBusy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("join_family_button"),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            if (isBusy) {
                                QuizzyInlineLoader(
                                    size = 20.dp,
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("Rejoindre mon espace")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoleSelectorCard(
    selected: Boolean,
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val titleColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val subtitleColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        onClick = onClick,
        modifier = modifier,
        color = containerColor,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(width = if (selected) 2.dp else 1.dp, color = borderColor),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = titleColor
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = subtitleColor
            )
        }
    }
}

@Composable
fun AvatarPickerRow(
    selectedAvatar: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SchoolCatalog.avatars.forEach { (key, label) ->
            val selected = key == selectedAvatar
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .border(
                        width = if (selected) 3.dp else 0.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = CircleShape
                    )
                    .padding(3.dp)
                    .clickable { onSelect(key) }
                    .testTag("avatar_option_$key")
            ) {
                ChildAvatarBadge(avatarKey = key, name = label, size = 44.dp)
            }
        }
    }
}
