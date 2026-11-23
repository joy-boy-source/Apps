package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.GeminiTutorService
import com.example.data.local.StudioImageEntity
import com.example.model.ChildProfile
import com.example.ui.components.ModernCard
import com.example.ui.components.QuizzyBookLoader
import com.example.ui.components.QuizzyInlineLoader
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.QuizzyTheme
import com.example.ui.viewmodel.ImageStudioUiState
import kotlinx.coroutines.launch

private val QUICK_IMAGE_IDEAS = listOf(
    "Schéma du cycle de l'eau avec nuages et rivière",
    "Le système solaire coloré avec les planètes",
    "Les fractions expliquées avec une pizza partagée",
    "Un château fort du Moyen Âge illustré",
    "Un volcan en éruption avec schéma simple"
)

private val QUICK_EDIT_IDEAS = listOf(
    "Ajoute des flèches explicatives colorées",
    "Transforme l'arrière-plan en ciel étoilé",
    "Rends le style comme une bande dessinée joyeuse",
    "Ajoute une petite étoile mascotte souriante"
)

private val VISUAL_STYLES = listOf(
    "Illustration cahier",
    "Schéma scientifique",
    "Bande dessinée",
    "Aquarelle douce"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImageStudioScreen(
    child: ChildProfile,
    studioState: ImageStudioUiState,
    savedImages: List<StudioImageEntity>,
    onGenerateImage: (prompt: String, styleLabel: String, subject: String) -> Unit,
    onEditImage: (sourceBitmap: Bitmap, editInstruction: String, basePrompt: String, subject: String) -> Unit,
    onLoadSavedBitmap: suspend (String) -> Bitmap?,
    onDeleteSavedImage: (StudioImageEntity) -> Unit,
    onResetState: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val qColors = QuizzyTheme.colors

    // 0 = Créer une image, 1 = Retoucher une image
    var modeTab by remember { mutableIntStateOf(0) }
    var promptText by remember { mutableStateOf("") }
    var selectedStyle by remember { mutableStateOf(VISUAL_STYLES.first()) }
    var editInstruction by remember { mutableStateOf("") }
    var sourceEditBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var sourceBasePrompt by remember { mutableStateOf("Image personnalisée") }

    val activeResultBitmap = (studioState as? ImageStudioUiState.Success)?.bitmap

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val bmp = GeminiTutorService.loadBitmapFromUri(context, uri)
                if (bmp != null) {
                    sourceEditBitmap = bmp
                    sourceBasePrompt = "Photo importée"
                    modeTab = 1
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("image_studio_screen")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        onClick = onBack,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("image_studio_back_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Retour",
                                tint = qColors.headingInk
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Studio Images IA",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = qColors.headingInk
                        )
                        Text(
                            text = "Crée et retouche tes schémas & illustrations (Nano Banana 2.1)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Mode toggle pills: Créer / Retoucher
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        onClick = { modeTab = 0 },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("studio_mode_create"),
                        shape = RoundedCornerShape(16.dp),
                        color = if (modeTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            if (modeTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = if (modeTab == 0) MaterialTheme.colorScheme.onPrimary else qColors.headingInk,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Créer",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (modeTab == 0) MaterialTheme.colorScheme.onPrimary else qColors.headingInk
                            )
                        }
                    }

                    Surface(
                        onClick = { modeTab = 1 },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("studio_mode_edit"),
                        shape = RoundedCornerShape(16.dp),
                        color = if (modeTab == 1) CoralAccent else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            if (modeTab == 1) CoralAccent else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Brush,
                                contentDescription = null,
                                tint = if (modeTab == 1) Color.White else qColors.headingInk,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Retoucher",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (modeTab == 1) Color.White else qColors.headingInk
                            )
                        }
                    }
                }
            }

            // Creation or Editing Form Card
            item {
                ModernCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 24.dp,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val isLoading = studioState is ImageStudioUiState.Loading

                        if (modeTab == 0) {
                            Text(
                                text = "Décris l'image ou le schéma à créer",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = qColors.headingInk
                            )
                            OutlinedTextField(
                                value = promptText,
                                onValueChange = { promptText = it },
                                placeholder = { Text("Ex : Un schéma coloré du système solaire...") },
                                minLines = 2,
                                maxLines = 4,
                                enabled = !isLoading,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("studio_create_prompt_input")
                            )

                            Text(
                                text = "Style visuel :",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = qColors.bodyInk
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                VISUAL_STYLES.forEach { style ->
                                    FilterChip(
                                        selected = selectedStyle == style,
                                        onClick = { selectedStyle = style },
                                        label = { Text(style) },
                                        modifier = Modifier.testTag("studio_style_$style")
                                    )
                                }
                            }

                            Text(
                                text = "Idées rapides :",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = qColors.bodyInk
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                QUICK_IMAGE_IDEAS.forEach { idea ->
                                    Surface(
                                        onClick = { promptText = idea },
                                        color = qColors.pastelMint,
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text(
                                            text = idea,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = qColors.bodyInk,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    onGenerateImage(
                                        "$promptText (style : $selectedStyle)",
                                        selectedStyle,
                                        "Création IA"
                                    )
                                },
                                enabled = !isLoading && promptText.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 52.dp)
                                    .testTag("studio_generate_button"),
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                if (isLoading) {
                                    QuizzyInlineLoader(
                                        size = 24.dp,
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text("Création en cours...", fontWeight = FontWeight.ExtraBold)
                                } else {
                                    Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Générer mon image IA", fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        } else {
                            Text(
                                text = "Retoucher ou transformer une image",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = qColors.headingInk
                            )

                            val targetBitmap = sourceEditBitmap ?: activeResultBitmap

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    enabled = !isLoading,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("studio_pick_photo_button"),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Choisir une photo")
                                }
                            }

                            if (targetBitmap != null) {
                                Image(
                                    bitmap = targetBitmap.asImageBitmap(),
                                    contentDescription = "Image source à retoucher",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                )
                            } else {
                                Surface(
                                    color = qColors.pastelPeach,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Sélectionne une photo depuis ta galerie ou crée d'abord une image dans l'onglet « Créer ».",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = qColors.bodyInk,
                                        modifier = Modifier.padding(14.dp)
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = editInstruction,
                                onValueChange = { editInstruction = it },
                                placeholder = { Text("Ex : Ajoute des étiquettes en français et un fond étoilé...") },
                                minLines = 2,
                                maxLines = 4,
                                enabled = !isLoading,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("studio_edit_prompt_input")
                            )

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                QUICK_EDIT_IDEAS.forEach { editIdea ->
                                    Surface(
                                        onClick = { editInstruction = editIdea },
                                        color = qColors.pastelYellow,
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text(
                                            text = editIdea,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = qColors.bodyInk,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    if (targetBitmap != null) {
                                        onEditImage(targetBitmap, editInstruction, sourceBasePrompt, "Retouche IA")
                                    }
                                },
                                enabled = !isLoading && targetBitmap != null && editInstruction.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 52.dp)
                                    .testTag("studio_apply_edit_button"),
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CoralAccent,
                                    contentColor = Color.White
                                )
                            ) {
                                if (isLoading) {
                                    QuizzyInlineLoader(
                                        size = 24.dp,
                                        tint = Color.White
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text("Retouche en cours...", fontWeight = FontWeight.ExtraBold)
                                } else {
                                    Icon(Icons.Filled.Brush, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Appliquer la retouche IA", fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                }
            }

            // Active Generation Result / Loading / Real Error Banner
            when (studioState) {
                is ImageStudioUiState.Loading -> {
                    item {
                        ModernCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("studio_loading_card"),
                            cornerRadius = 24.dp,
                            containerColor = qColors.pastelLavender
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                QuizzyBookLoader(
                                    size = 86.dp,
                                    message = studioState.message
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "Quizzy réessaie automatiquement jusqu'à obtenir ton image, même si le serveur est très sollicité. Patiente tranquillement !",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = qColors.secondaryInk,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = onResetState,
                                    modifier = Modifier.testTag("studio_cancel_button")
                                ) {
                                    Text("Annuler")
                                }
                            }
                        }
                    }
                }
                is ImageStudioUiState.Error -> {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("studio_error_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = studioState.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (modeTab == 0 && promptText.isNotBlank()) {
                                        Button(
                                            onClick = {
                                                onGenerateImage(
                                                    "$promptText (style : $selectedStyle)",
                                                    selectedStyle,
                                                    "Création IA"
                                                )
                                            },
                                            modifier = Modifier.testTag("studio_retry_button")
                                        ) {
                                            Text("Réessayer")
                                        }
                                    } else if (modeTab == 1 && (sourceEditBitmap ?: activeResultBitmap) != null && editInstruction.isNotBlank()) {
                                        val bmp = sourceEditBitmap ?: activeResultBitmap
                                        Button(
                                            onClick = {
                                                if (bmp != null) {
                                                    onEditImage(bmp, editInstruction, sourceBasePrompt, "Retouche IA")
                                                }
                                            },
                                            modifier = Modifier.testTag("studio_retry_button")
                                        ) {
                                            Text("Réessayer")
                                        }
                                    }
                                    OutlinedButton(onClick = onResetState) {
                                        Text("Fermer")
                                    }
                                }
                            }
                        }
                    }
                }
                is ImageStudioUiState.Success -> {
                    item {
                        ModernCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("studio_result_card"),
                            cornerRadius = 24.dp,
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (studioState.isEdited) "Image retouchée !" else "Nouvelle création IA !",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = qColors.headingInk
                                    )
                                    Surface(
                                        color = qColors.pastelMint,
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text(
                                            text = "Sauvegardée en SQL",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                Image(
                                    bitmap = studioState.bitmap.asImageBitmap(),
                                    contentDescription = studioState.prompt,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 200.dp, max = 340.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                )
                                OutlinedButton(
                                    onClick = {
                                        sourceEditBitmap = studioState.bitmap
                                        sourceBasePrompt = studioState.prompt
                                        modeTab = 1
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Retoucher cette image")
                                }
                            }
                        }
                    }
                }
                is ImageStudioUiState.Idle -> Unit
            }

            // Room SQLite Saved Creations Gallery
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Storage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Galerie de ${child.name} — Base SQL (${savedImages.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = qColors.headingInk
                    )
                }
            }

            if (savedImages.isEmpty()) {
                item {
                    ModernCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 20.dp,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            text = "Tes créations et retouches d'images seront sauvegardées ici automatiquement dans ta base SQL locale.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        )
                    }
                }
            } else {
                items(savedImages, key = { it.id }) { item ->
                    SavedStudioImageCard(
                        item = item,
                        onLoadBitmap = onLoadSavedBitmap,
                        onEditThis = { bmp ->
                            sourceEditBitmap = bmp
                            sourceBasePrompt = item.prompt
                            modeTab = 1
                        },
                        onDelete = { onDeleteSavedImage(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedStudioImageCard(
    item: StudioImageEntity,
    onLoadBitmap: suspend (String) -> Bitmap?,
    onEditThis: (Bitmap) -> Unit,
    onDelete: () -> Unit
) {
    val qColors = QuizzyTheme.colors
    var bitmap by remember(item.imagePath) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(item.imagePath) {
        bitmap = onLoadBitmap(item.imagePath)
    }

    ModernCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("studio_gallery_item_${item.id}"),
        cornerRadius = 20.dp,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(qColors.pastelMint),
                contentAlignment = Alignment.Center
            ) {
                val bmp = bitmap
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = item.prompt,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (item.isEdited && item.editInstruction.isNotBlank()) item.editInstruction else item.prompt,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = qColors.headingInk,
                    maxLines = 2
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (item.isEdited) "Retouche IA • ${item.styleLabel}" else "Création IA • ${item.styleLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            bitmap?.let { loaded ->
                IconButton(onClick = { onEditThis(loaded) }) {
                    Icon(
                        imageVector = Icons.Filled.Brush,
                        contentDescription = "Retoucher cette image",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Supprimer cette image",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
