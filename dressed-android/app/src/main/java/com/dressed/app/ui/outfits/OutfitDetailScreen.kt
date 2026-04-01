package com.dressed.app.ui.outfits

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.dressed.app.data.local.WardrobeItemEntity
import com.dressed.app.data.model.WardrobeCategories
import com.dressed.app.ui.WardrobeViewModel
import com.dressed.app.ui.wardrobe.coilPhotoFileOrNull

/** Read-only outfit detail (parity with iOS). Uses [coilPhotoFileOrNull] for resized JPEG paths. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutfitDetailScreen(
    outfitId: String,
    wardrobeViewModel: WardrobeViewModel,
    outfitsViewModel: OutfitsViewModel,
    onBack: () -> Unit,
) {
    val outfit by outfitsViewModel.observeOutfit(outfitId).collectAsStateWithLifecycle(initialValue = null)
    val wardrobeItems by wardrobeViewModel.items.collectAsStateWithLifecycle(initialValue = emptyList())
    val itemsById = remember(wardrobeItems) { wardrobeItems.associateBy { it.id } }

    var sawOutfit by remember { mutableStateOf(false) }
    LaunchedEffect(outfit) {
        if (outfit != null) sawOutfit = true
    }
    LaunchedEffect(sawOutfit, outfit) {
        if (sawOutfit && outfit == null) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Outfit",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(
                            "← Back",
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.92f),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
    ) { padding ->
        when {
            outfitId.isBlank() -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Invalid outfit.", style = MaterialTheme.typography.bodyLarge)
                }
            }
            outfit == null && !sawOutfit -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Loading…", style = MaterialTheme.typography.bodyLarge)
                }
            }
            outfit != null -> {
                val o = outfit!!
                val orderedIds = o.itemIds.map { it.trim() }.filter { it.isNotEmpty() }
                val resolved = orderedIds.mapNotNull { itemsById[it] }
                val displayName = o.name.trim().ifEmpty { "Untitled outfit" }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
                    ) {
                        OutfitDetailHero(resolved = resolved, orderedIds = orderedIds)
                    }

                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "${orderedIds.size} piece" + if (orderedIds.size != 1) "s" else "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (o.wornCount > 0) {
                            Text(
                                text = "· Worn ${o.wornCount}×",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    Text(
                        text = "Pieces",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )

                    if (orderedIds.isEmpty()) {
                        Text(
                            text = "No pieces are linked to this outfit.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }

                    orderedIds.forEach { pid ->
                        OutfitDetailPieceRow(
                            pieceId = pid,
                            item = itemsById[pid],
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
            else -> Unit
        }
    }
}

@Composable
private fun OutfitDetailHero(
    resolved: List<WardrobeItemEntity>,
    orderedIds: List<String>,
) {
    val collageItems = resolved.take(4)
    val dressOnly = resolved.isEmpty() && orderedIds.isEmpty()
    when {
        dressOnly -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("👗", style = MaterialTheme.typography.displayLarge)
            }
        }
        collageItems.isEmpty() -> FourEmptyHeroCells()
        collageItems.size == 1 -> {
            HeroImageCell(item = collageItems[0], modifier = Modifier.fillMaxSize())
        }
        else -> {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.weight(1f)) {
                    HeroImageCell(
                        item = collageItems[0],
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                    HeroImageCell(
                        item = collageItems.getOrNull(1),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
                Row(Modifier.weight(1f)) {
                    HeroImageCell(
                        item = collageItems.getOrNull(2),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                    HeroImageCell(
                        item = collageItems.getOrNull(3),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
            }
        }
    }
}

@Composable
private fun FourEmptyHeroCells() {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.weight(1f)) {
            EmptyHeroCell(Modifier.weight(1f))
            EmptyHeroCell(Modifier.weight(1f))
        }
        Row(Modifier.weight(1f)) {
            EmptyHeroCell(Modifier.weight(1f))
            EmptyHeroCell(Modifier.weight(1f))
        }
    }
}

@Composable
private fun EmptyHeroCell(modifier: Modifier) {
    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
    )
}

@Composable
private fun HeroImageCell(item: WardrobeItemEntity?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center,
    ) {
        if (item == null) return@Box
        val photo = coilPhotoFileOrNull(item.photoPath)
        if (photo != null) {
            AsyncImage(
                model = photo,
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = WardrobeCategories.emoji(item.category),
                style = MaterialTheme.typography.headlineLarge,
            )
        }
    }
}

@Composable
private fun OutfitDetailPieceRow(
    pieceId: String,
    item: WardrobeItemEntity?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (item != null) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center,
            ) {
                val photo = coilPhotoFileOrNull(item.photoPath)
                if (photo != null) {
                    AsyncImage(
                        model = photo,
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Text(
                        text = WardrobeCategories.emoji(item.category),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = WardrobeCategories.label(item.category),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center,
            ) {
                Text("?", style = MaterialTheme.typography.titleMedium)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Missing piece",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Not in wardrobe · $pieceId",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
