package com.dressed.app.ui.wardrobe

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.dressed.app.data.local.WardrobeItemEntity
import com.dressed.app.data.model.WardrobeCategories
import com.dressed.app.ui.WardrobeViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(
    itemId: String,
    viewModel: WardrobeViewModel,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
) {
    val item by viewModel.observeItem(itemId).collectAsStateWithLifecycle(initialValue = null)

    var sawItem by remember { mutableStateOf(false) }
    LaunchedEffect(item) {
        if (item != null) sawItem = true
    }
    LaunchedEffect(sawItem, item) {
        if (sawItem && item == null) onDeleted()
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    val current = item
    if (current == null && !sawItem) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Piece") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            },
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text("Loading…", style = MaterialTheme.typography.bodyLarge)
            }
        }
        return
    }

    if (current == null) return

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        WardrobeCategories.label(current.category).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
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
                .verticalScroll(rememberScrollState()),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .aspectRatio(3f / 4f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            ) {
                if (current.photoPath != null) {
                    AsyncImage(
                        model = File(current.photoPath),
                        contentDescription = current.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            WardrobeCategories.emoji(current.category),
                            style = MaterialTheme.typography.displayLarge,
                        )
                    }
                }
            }

            Column(Modifier.padding(20.dp)) {
                Text(
                    current.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (current.sizeLabel.isBlank()) "Size —" else "Size ${current.sizeLabel}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(RoundedCornerShape(50))
                            .background(parseColorSafe(current.colorHex)),
                    )
                    Text(
                        current.colorName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (current.seasons.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        current.seasons.joinToString(" · ") { it.replaceFirstChar { c -> c.uppercase() } },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Spacer(Modifier.height(20.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    StatBlock(value = "${current.wornCount}", label = "Times Worn")
                    StatBlock(value = "0", label = "Outfits")
                }

                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { viewModel.markWorn(current.id) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("+ Mark as Worn Today")
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Remove from Wardrobe")
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Remove this piece?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteItem(current.id)
                    },
                ) { Text("Remove", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun StatBlock(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
