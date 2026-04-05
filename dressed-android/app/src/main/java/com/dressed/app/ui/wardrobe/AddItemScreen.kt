package com.dressed.app.ui.wardrobe

import android.content.Context
import android.graphics.Color as AndroidColor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.net.Uri
import coil.compose.AsyncImage
import com.dressed.app.data.model.WardrobeCategories
import com.dressed.app.data.model.WardrobeOccasions
import com.dressed.app.data.model.WardrobeSeasons
import com.dressed.app.data.local.ImageStorage
import com.dressed.app.data.model.WardrobeSizes
import com.dressed.app.ui.WardrobeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddItemScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: WardrobeViewModel,
    editingItemId: String? = null,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("") }
    var sizeText by rememberSaveable { mutableStateOf("") }

    var hue by rememberSaveable { mutableStateOf(285f) }
    var saturation by rememberSaveable { mutableStateOf(0.42f) }
    var brightness by rememberSaveable { mutableStateOf(0.96f) }
    var colorName by rememberSaveable { mutableStateOf("") }
    var colorNameExpanded by remember { mutableStateOf(false) }

    val seasons = remember { mutableStateListOf<String>() }
    val occasions = remember { mutableStateListOf<String>() }

    var editLoaded by remember(editingItemId) { mutableStateOf(false) }

    // Auto-fill the colour name whenever the wheel changes (skip until edit screen has hydrated).
    LaunchedEffect(hue, saturation, brightness) {
        if (editingItemId != null && !editLoaded) return@LaunchedEffect
        colorName = labelForPickedColor(hsvToHex(hue, saturation, brightness))
    }

    val context = LocalContext.current
    val photoScope = rememberCoroutineScope()

    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var savedPhotoPath by remember { mutableStateOf<String?>(null) }
    var isCopyingPhoto by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }

    LaunchedEffect(editingItemId) {
        if (editingItemId == null || editLoaded) return@LaunchedEffect
        viewModel.observeItem(editingItemId).collect { entity ->
            if (entity == null || editLoaded) return@collect
            name = entity.name
            category = entity.category
            sizeText = entity.sizeLabel
            colorName = entity.colorName
            val raw = entity.colorHex.let { if (it.startsWith("#")) it else "#$it" }
            runCatching {
                val hsv = FloatArray(3)
                AndroidColor.colorToHSV(AndroidColor.parseColor(raw), hsv)
                hue = hsv[0]
                saturation = hsv[1]
                brightness = hsv[2]
            }
            seasons.clear()
            seasons.addAll(entity.seasons)
            occasions.clear()
            occasions.addAll(entity.occasions)
            savedPhotoPath = entity.photoPath
            if (entity.photoPath != null) {
                photoUri = Uri.fromFile(File(entity.photoPath))
            }
            editLoaded = true
        }
    }

    val pickPhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            photoUri = uri
            savedPhotoPath = null
            isCopyingPhoto = true
            photoScope.launch(Dispatchers.IO) {
                val path = ImageStorage.copyFromUri(context, uri)
                withContext(Dispatchers.Main) {
                    savedPhotoPath = path
                    isCopyingPhoto = false
                }
            }
        }
    }

    val takePicture = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        if (success) {
            val file = pendingCameraFile
            val uri = pendingCameraUri
            if (file != null && uri != null) {
                photoUri = uri
                savedPhotoPath = null
                isCopyingPhoto = true
                photoScope.launch(Dispatchers.IO) {
                    val path = ImageStorage.copyFromFile(context, file)
                    withContext(Dispatchers.Main) {
                        savedPhotoPath = path
                        isCopyingPhoto = false
                    }
                }
            }
        }
        pendingCameraUri = null
        pendingCameraFile = null
    }

    var errorHint by remember { mutableStateOf<String?>(null) }

    fun startCameraCapture() {
        runCatching {
            val (uri, file) = createCameraCaptureUri(context)
            pendingCameraUri = uri
            pendingCameraFile = file
            takePicture.launch(uri)
        }.onFailure {
            errorHint = "Could not open camera. Check that the app has camera permission."
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (editingItemId != null) "Edit Piece" else "Add a New Piece",
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.92f))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Text("Photo", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp),
                    )
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .clickable {
                        pickPhoto.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (photoUri != null) {
                    AsyncImage(
                        model = photoUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📷", style = MaterialTheme.typography.displaySmall)
                        Text(
                            "Tap to browse for an image file",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = { startCameraCapture() },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Take photo")
                }
                OutlinedButton(
                    onClick = {
                        pickPhoto.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Gallery")
                }
            }
            Spacer(Modifier.height(20.dp))

            Text("Name", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. White linen blouse") },
                singleLine = true,
            )

            Spacer(Modifier.height(16.dp))

            Text("Category", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                WardrobeCategories.ADD_PICKER.forEach { (key, label) ->
                    FilterChip(
                        selected = category == key,
                        onClick = { category = key },
                        label = { Text(label) },
                    )
                }
            }

            val sizeSuggestions = remember(category) { WardrobeSizes.suggestionsFor(category) }

            Spacer(Modifier.height(16.dp))
            Text("Size (optional)", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            if (category.isNotBlank() && sizeSuggestions.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    sizeSuggestions.forEach { label ->
                        FilterChip(
                            selected = sizeText == label,
                            onClick = { sizeText = label },
                            label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            } else if (category.isBlank()) {
                Text(
                    "Pick a category for common size shortcuts, or type your own below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
            }
            OutlinedTextField(
                value = sizeText,
                onValueChange = { sizeText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. M, 10, 9.5, or custom") },
                singleLine = true,
            )

            Spacer(Modifier.height(16.dp))

            Text("Color", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            ColorWheelPicker(
                hue = hue,
                onHueChange = { hue = it },
                saturation = saturation,
                onSaturationChange = { saturation = it },
                brightness = brightness,
                onBrightnessChange = { brightness = it },
            )

            Spacer(Modifier.height(16.dp))

            Text("Color Name", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))

            // Always show the full preset list. If the user is typing something custom,
            // float matching names to the top so presets remain visible throughout.
            val colorSuggestions = remember(colorName) {
                val q = colorName.trim().lowercase()
                if (q.isEmpty()) {
                    COLOR_NAME_SUGGESTIONS
                } else {
                    val top = COLOR_NAME_SUGGESTIONS.filter {
                        it.lowercase().startsWith(q) || it.lowercase().contains(q)
                    }
                    top + COLOR_NAME_SUGGESTIONS.filterNot { it in top }
                }
            }

            ExposedDropdownMenuBox(
                expanded = colorNameExpanded && colorSuggestions.isNotEmpty(),
                onExpandedChange = { colorNameExpanded = it },
            ) {
                OutlinedTextField(
                    value = colorName,
                    onValueChange = { colorName = it; colorNameExpanded = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryEditable, enabled = true),
                    placeholder = { Text("e.g. Lavender, Navy, Cream…") },
                    singleLine = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = colorNameExpanded && colorSuggestions.isNotEmpty())
                    },
                )
                ExposedDropdownMenu(
                    expanded = colorNameExpanded && colorSuggestions.isNotEmpty(),
                    onDismissRequest = { colorNameExpanded = false },
                ) {
                    colorSuggestions.forEach { suggestion ->
                        DropdownMenuItem(
                            text = { Text(suggestion) },
                            onClick = { colorName = suggestion; colorNameExpanded = false },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Text("Season", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                WardrobeSeasons.ALL.forEach { (key, label) ->
                    val sel = seasons.contains(key)
                    FilterChip(
                        selected = sel,
                        onClick = {
                            if (sel) seasons.remove(key) else seasons.add(key)
                        },
                        label = { Text(label) },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text("Occasions (optional)", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                WardrobeOccasions.ALL.forEach { (key, label) ->
                    val sel = occasions.contains(key)
                    FilterChip(
                        selected = sel,
                        onClick = {
                            if (sel) occasions.remove(key) else occasions.add(key)
                        },
                        label = { Text(label) },
                    )
                }
            }

            errorHint?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    val n = name.trim()
                    when {
                        n.isBlank() -> errorHint = "Please enter a name"
                        category.isBlank() -> errorHint = "Please select a category"
                        else -> {
                            errorHint = null
                            val hex = hsvToHex(hue, saturation, brightness)
                            if (editingItemId != null) {
                                viewModel.saveEdit(
                                    itemId = editingItemId,
                                    name = n,
                                    category = category,
                                    sizeLabel = sizeText,
                                    colorHex = hex,
                                    colorName = colorName.trim().ifBlank { labelForPickedColor(hex) },
                                    seasons = seasons.toList(),
                                    occasions = occasions.toList(),
                                    photoPath = savedPhotoPath,
                                    onSaved = onSaved,
                                )
                            } else {
                                viewModel.addItem(
                                    name = n,
                                    category = category,
                                    sizeLabel = sizeText,
                                    colorHex = hex,
                                    colorName = colorName.trim().ifBlank { labelForPickedColor(hex) },
                                    seasons = seasons.toList(),
                                    occasions = occasions.toList(),
                                    photoPath = savedPhotoPath,
                                    onInserted = onSaved,
                                )
                            }
                        }
                    }
                },
                enabled = !isCopyingPhoto,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isCopyingPhoto) {
                    Text("Saving photo…")
                } else {
                    Text("Save to Wardrobe")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun createCameraCaptureUri(context: Context): Pair<Uri, File> {
    val dir = File(context.cacheDir, "camera").apply { mkdirs() }
    val file = File.createTempFile("dressed_${UUID.randomUUID()}", ".jpg", dir)
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )
    return Pair(uri, file)
}
