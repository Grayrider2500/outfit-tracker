package com.dressed.app.ui.outfits

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.dressed.app.data.local.OutfitEntity
import com.dressed.app.data.local.WardrobeItemEntity
import com.dressed.app.data.model.WardrobeCategories
import com.dressed.app.ui.wardrobe.coilPhotoFileOrNull

@Composable
internal fun OutfitCollageCard(
    outfit: OutfitEntity,
    items: List<WardrobeItemEntity>,
    onClick: () -> Unit,
) {
    val collageItems = items.take(4)

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column {
            // Photo collage — square aspect ratio
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)),
            ) {
                when {
                    collageItems.isEmpty() -> {
                        EmptyCollageCell(modifier = Modifier.fillMaxSize())
                    }
                    collageItems.size == 1 -> {
                        CollageCell(
                            item = collageItems[0],
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    else -> {
                        // 2×2 grid; empty cells show muted background
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                            ) {
                                CollageCell(
                                    item = collageItems[0],
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize(),
                                )
                                CollageCell(
                                    item = collageItems.getOrNull(1),
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize(),
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                            ) {
                                CollageCell(
                                    item = collageItems.getOrNull(2),
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize(),
                                )
                                CollageCell(
                                    item = collageItems.getOrNull(3),
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize(),
                                )
                            }
                        }
                    }
                }
            }

            // Info section
            Column(modifier = Modifier.padding(10.dp, 10.dp)) {
                Text(
                    text = outfit.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val pieceCount = outfit.itemIds.size
                    Text(
                        text = "$pieceCount piece${if (pieceCount != 1) "s" else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (outfit.wornCount > 0) {
                        Text(
                            text = "· worn ${outfit.wornCount}×",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

/** Display order for picker summaries (aligns with iOS collage / silhouette flow). */
internal fun pickerDisplayOrder(category: String): Int = when (category) {
    WardrobeCategories.TOPS, WardrobeCategories.DRESSES -> 0
    WardrobeCategories.BOTTOMS -> 1
    WardrobeCategories.OUTERWEAR -> 2
    WardrobeCategories.SHOES -> 3
    WardrobeCategories.ACCESSORIES -> 4
    else -> 5
}

internal fun pickerItemsSortedForDisplay(items: List<WardrobeItemEntity>): List<WardrobeItemEntity> =
    items.sortedWith(
        compareBy(
            { pickerDisplayOrder(it.category) },
            { it.name.lowercase() },
            { it.id },
        ),
    )

/** Short line like "Navy Chinos + White Sneakers" for suggestion cards ([maxPieces] main items). */
internal fun pickerMainPiecesSummaryLine(items: List<WardrobeItemEntity>, maxPieces: Int = 3): String {
    val parts = pickerItemsSortedForDisplay(items).take(maxPieces).map { item ->
        val c = item.colorName.trim()
        val n = item.name.trim()
        when {
            c.isNotEmpty() && n.isNotEmpty() -> "$c $n"
            n.isNotEmpty() -> n
            c.isNotEmpty() -> c
            else -> WardrobeCategories.label(item.category)
        }
    }
    return parts.joinToString(" + ")
}

/** Picker / suggestion card (no [OutfitEntity] yet). Reuses the same collage layout as [OutfitCollageCard]. */
@Composable
internal fun SuggestionOutfitCollageCard(
    title: String,
    items: List<WardrobeItemEntity>,
    subtitle: String?,
    onClick: () -> Unit,
) {
    val collageItems = items.take(4)
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)),
            ) {
                when {
                    collageItems.isEmpty() -> EmptyCollageCell(modifier = Modifier.fillMaxSize())
                    collageItems.size == 1 -> CollageCell(collageItems[0], Modifier.fillMaxSize())
                    else -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                            ) {
                                CollageCell(
                                    collageItems[0],
                                    Modifier.weight(1f).fillMaxSize(),
                                )
                                CollageCell(
                                    collageItems.getOrNull(1),
                                    Modifier.weight(1f).fillMaxSize(),
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                            ) {
                                CollageCell(
                                    collageItems.getOrNull(2),
                                    Modifier.weight(1f).fillMaxSize(),
                                )
                                CollageCell(
                                    collageItems.getOrNull(3),
                                    Modifier.weight(1f).fillMaxSize(),
                                )
                            }
                        }
                    }
                }
            }
            Column(modifier = Modifier.padding(10.dp, 10.dp)) {
                val summary = pickerMainPiecesSummaryLine(items)
                if (summary.isNotBlank()) {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = if (summary.isNotBlank()) 4.dp else 0.dp),
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CollageCell(
    item: WardrobeItemEntity?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        ),
        contentAlignment = Alignment.Center,
    ) {
        if (item == null) return@Box

        val photoFile = coilPhotoFileOrNull(item.photoPath)
        if (photoFile != null) {
            AsyncImage(
                model = photoFile,
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = WardrobeCategories.emoji(item.category),
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

@Composable
private fun EmptyCollageCell(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "👗",
            style = MaterialTheme.typography.displayMedium,
        )
    }
}
