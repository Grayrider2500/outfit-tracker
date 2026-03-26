package com.dressed.app.ui.wardrobe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dressed.app.data.model.WardrobeCategories
import com.dressed.app.data.model.WardrobeSortOption
import com.dressed.app.data.model.sortedForDisplay
import com.dressed.app.ui.WardrobeViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WardrobeSearchScreen(
    viewModel: WardrobeViewModel,
    onAddClick: () -> Unit,
    onItemClick: (String) -> Unit,
) {
    val items by viewModel.items.collectAsStateWithLifecycle(initialValue = emptyList())
    var nameQuery by rememberSaveable { mutableStateOf("") }
    var filterKey by rememberSaveable { mutableStateOf(WardrobeCategories.ALL) }
    var sortOptionName by rememberSaveable { mutableStateOf(WardrobeSortOption.DATE_ADDED_DESC.name) }
    val sortOption = runCatching { WardrobeSortOption.valueOf(sortOptionName) }
        .getOrElse { WardrobeSortOption.DATE_ADDED_DESC }
    var colorFilterName by rememberSaveable { mutableStateOf("") }
    var sizeFilterLabel by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(filterKey) {
        colorFilterName = ""
        sizeFilterLabel = ""
    }

    val afterCategory = remember(items, filterKey) {
        if (filterKey == WardrobeCategories.ALL) items
        else items.filter { it.category == filterKey }
    }
    val distinctColors = remember(afterCategory) {
        afterCategory.map { it.colorName }.distinct().sorted()
    }
    val distinctSizes = remember(afterCategory) {
        afterCategory.map { it.sizeLabel }.filter { it.isNotBlank() }.distinct()
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
    }

    val displayed = remember(
        afterCategory,
        colorFilterName,
        sizeFilterLabel,
        sortOption,
        nameQuery,
    ) {
        var list = afterCategory
        val q = nameQuery.trim()
        if (q.isNotEmpty()) {
            list = list.filter { it.name.contains(q, ignoreCase = true) }
        }
        if (colorFilterName.isNotBlank()) {
            list = list.filter { it.colorName == colorFilterName }
        }
        if (sizeFilterLabel.isNotBlank()) {
            list = list.filter { it.sizeLabel == sizeFilterLabel }
        }
        list.sortedForDisplay(sortOption)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Find pieces") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add piece")
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            OutlinedTextField(
                value = nameQuery,
                onValueChange = { nameQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                label = { Text("Search by name") },
                singleLine = true,
            )

            Text(
                "Category",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(6.dp))
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                WardrobeCategories.FILTERS.forEach { (key, label) ->
                    FilterChip(
                        selected = filterKey == key,
                        onClick = { filterKey = key },
                        label = { Text(label) },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "Sort",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(6.dp))
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                WardrobeSortOption.entries.forEach { opt ->
                    FilterChip(
                        selected = sortOption == opt,
                        onClick = { sortOptionName = opt.name },
                        label = { Text(opt.shortLabel) },
                    )
                }
            }

            if (distinctColors.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Color",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    FilterChip(
                        selected = colorFilterName.isEmpty(),
                        onClick = { colorFilterName = "" },
                        label = { Text("All colors") },
                    )
                    distinctColors.forEach { c ->
                        FilterChip(
                            selected = colorFilterName == c,
                            onClick = {
                                colorFilterName = if (colorFilterName == c) "" else c
                            },
                            label = { Text(c, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        )
                    }
                }
            }

            if (distinctSizes.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Size",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    FilterChip(
                        selected = sizeFilterLabel.isEmpty(),
                        onClick = { sizeFilterLabel = "" },
                        label = { Text("All sizes") },
                    )
                    distinctSizes.forEach { s ->
                        FilterChip(
                            selected = sizeFilterLabel == s,
                            onClick = {
                                sizeFilterLabel = if (sizeFilterLabel == s) "" else s
                            },
                            label = { Text(s) },
                        )
                    }
                }
            }

            if (colorFilterName.isNotEmpty() || sizeFilterLabel.isNotEmpty()) {
                TextButton(
                    onClick = {
                        colorFilterName = ""
                        sizeFilterLabel = ""
                    },
                    modifier = Modifier.padding(horizontal = 12.dp),
                ) {
                    Text("Clear color & size")
                }
            }

            Spacer(Modifier.height(8.dp))

            if (displayed.isEmpty()) {
                EmptyWardrobeState(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    hasItemsInWardrobe = items.isNotEmpty(),
                    filtersExcludeAll = items.isNotEmpty() && afterCategory.isNotEmpty() && displayed.isEmpty(),
                    emptyTitle = when {
                        items.isEmpty() -> null
                        nameQuery.isNotBlank() && displayed.isEmpty() -> "No name matches"
                        else -> null
                    },
                    emptySubtitle = when {
                        items.isEmpty() -> null
                        nameQuery.isNotBlank() && displayed.isEmpty() ->
                            "Try a different spelling or clear the search box."
                        else -> null
                    },
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(displayed, key = { it.id }) { item ->
                        WardrobeItemCard(item = item, onClick = { onItemClick(item.id) })
                    }
                }
            }
        }
    }
}
