package com.dressed.app.ui.wardrobe

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import android.net.Uri
import coil.compose.AsyncImage
import com.dressed.app.data.model.WardrobeCategories
import com.dressed.app.data.model.WardrobeColors
import com.dressed.app.data.model.WardrobeSeasons
import com.dressed.app.data.model.WardrobeSizes
import com.dressed.app.ui.WardrobeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: WardrobeViewModel,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("") }
    var sizeText by rememberSaveable { mutableStateOf("") }

    val palette = remember { WardrobeColors.PALETTE }
    var selectedSwatch by remember { mutableStateOf(palette.first()) }

    val seasons = remember { mutableStateListOf<String>() }

    var photoUri by remember { mutableStateOf<Uri?>(null) }
    val pickPhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> photoUri = uri }

    var errorHint by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Add a New Piece") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Cancel") }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
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
                            "Tap to choose a photo",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                palette.forEach { swatch ->
                    val selected = swatch == selectedSwatch
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(swatchComposeColor(swatch.hex))
                            .then(
                                if (selected) {
                                    Modifier.border(
                                        3.dp,
                                        MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(10.dp),
                                    )
                                } else {
                                    Modifier.border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        RoundedCornerShape(10.dp),
                                    )
                                },
                            )
                            .clickable {
                                selectedSwatch = swatch
                            },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text("Season", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                            viewModel.addItem(
                                name = n,
                                category = category,
                                sizeLabel = sizeText,
                                colorHex = selectedSwatch.hex,
                                colorName = selectedSwatch.name,
                                seasons = seasons.toList(),
                                photoUri = photoUri,
                                onInserted = onSaved,
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save to Wardrobe")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun swatchComposeColor(hex: String): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrElse { Color.Gray }
