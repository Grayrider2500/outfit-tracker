package com.dressed.app.ui.wardrobe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dressed.app.data.model.WardrobeCategories
import com.dressed.app.data.model.WardrobeSortOption
import com.dressed.app.data.model.sortedForDisplay
import com.dressed.app.ui.WardrobeViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WardrobeListScreen(
    viewModel: WardrobeViewModel,
    onNavigateHome: () -> Unit,
    onAddClick: () -> Unit,
    onItemClick: (String) -> Unit,
) {
    val items by viewModel.items.collectAsStateWithLifecycle(initialValue = emptyList())
    var filterKey by rememberSaveable { mutableStateOf(WardrobeCategories.ALL) }

    val afterCategory = remember(items, filterKey) {
        if (filterKey == WardrobeCategories.ALL) items
        else items.filter { it.category == filterKey }
    }
    val displayed = remember(afterCategory) {
        afterCategory.sortedForDisplay(WardrobeSortOption.DATE_ADDED_DESC)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "Wardrobe",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Text(
                            itemCountLabel(items.size),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.65f),
                            textAlign = TextAlign.Center,
                        )
                    }
                },
                navigationIcon = {
                    TextButton(onClick = onNavigateHome) {
                        Text(
                            "← Home",
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.92f),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onAddClick) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Add piece",
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                WardrobeCategories.FILTERS.forEach { (key, label) ->
                    FilterChip(
                        selected = filterKey == key,
                        onClick = { filterKey = key },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }

            if (displayed.isEmpty()) {
                EmptyWardrobeState(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    hasItemsInWardrobe = items.isNotEmpty(),
                    filtersExcludeAll = items.isNotEmpty() && afterCategory.isEmpty(),
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(displayed, key = { it.id }) { item ->
                        WardrobeItemCard(item = item, onClick = { onItemClick(item.id) })
                    }
                }
            }
        }
    }
}
