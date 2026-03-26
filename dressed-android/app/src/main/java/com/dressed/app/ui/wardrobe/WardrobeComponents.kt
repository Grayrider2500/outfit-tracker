package com.dressed.app.ui.wardrobe

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.dressed.app.data.local.WardrobeItemEntity
import com.dressed.app.data.model.WardrobeCategories
import java.io.File

internal fun itemCountLabel(count: Int): String =
    "$count item" + if (count != 1) "s" else ""

@Composable
internal fun EmptyWardrobeState(
    modifier: Modifier = Modifier,
    hasItemsInWardrobe: Boolean,
    filtersExcludeAll: Boolean,
    emptyTitle: String? = null,
    emptySubtitle: String? = null,
) {
    val title = emptyTitle ?: when {
        !hasItemsInWardrobe -> "Your wardrobe awaits"
        filtersExcludeAll -> "No pieces match these filters"
        else -> "Nothing in this category"
    }
    val subtitle = emptySubtitle ?: when {
        !hasItemsInWardrobe -> "Tap + to add your first piece."
        filtersExcludeAll -> "Try clearing filters or widening your search."
        else -> "Try another category or add something new."
    }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
internal fun WardrobeItemCard(item: WardrobeItemEntity, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .clip(RoundedCornerShape(bottomStart = 0.dp, bottomEnd = 0.dp)),
            ) {
                if (item.photoPath != null) {
                    AsyncImage(
                        model = File(item.photoPath),
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = WardrobeCategories.emoji(item.category),
                            style = MaterialTheme.typography.displayMedium,
                        )
                    }
                }
            }
            Column(Modifier.padding(10.dp, 12.dp)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = WardrobeCategories.label(item.category),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    ColorDot(hex = item.colorHex)
                }
                if (item.sizeLabel.isNotBlank()) {
                    Text(
                        text = "Size ${item.sizeLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun ColorDot(hex: String) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(RoundedCornerShape(50))
            .background(parseColorSafe(hex)),
    )
}

internal fun parseColorSafe(hex: String): Color =
    runCatching {
        Color(android.graphics.Color.parseColor(hex))
    }.getOrElse { Color.Gray }
