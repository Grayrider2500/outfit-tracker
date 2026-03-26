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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dressed.app.data.model.WardrobeCategories
import com.dressed.app.data.model.WardrobeSeasons
import com.dressed.app.data.model.WardrobeSortOption
import com.dressed.app.data.model.sortedForDisplay
import com.dressed.app.ui.WardrobeViewModel

private val SearchSeasonFilters: List<Pair<String, String>> =
    listOf(WardrobeCategories.ALL to "All") + WardrobeSeasons.ALL.map { (key, label) ->
        if (key == "fall") key to "Autumn" else key to label
    }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WardrobeSearchScreen(
    viewModel: WardrobeViewModel,
    onNavigateHome: () -> Unit,
    onItemClick: (String) -> Unit,
) {
    val items by viewModel.items.collectAsStateWithLifecycle(initialValue = emptyList())
    var nameQuery by rememberSaveable { mutableStateOf("") }
    var filterKey by rememberSaveable { mutableStateOf(WardrobeCategories.ALL) }
    var seasonKey by rememberSaveable { mutableStateOf(WardrobeCategories.ALL) }
    var sortMode by rememberSaveable { mutableStateOf("recent") }

    val sortOption = when (sortMode) {
        "worn" -> WardrobeSortOption.WORN_DESC
        "name" -> WardrobeSortOption.NAME_ASC
        else -> WardrobeSortOption.DATE_ADDED_DESC
    }

    val afterCategory = remember(items, filterKey) {
        if (filterKey == WardrobeCategories.ALL) items
        else items.filter { it.category == filterKey }
    }

    val displayed = remember(afterCategory, nameQuery, seasonKey, sortOption) {
        var list = afterCategory
        val q = nameQuery.trim()
        if (q.isNotEmpty()) {
            list = list.filter { it.name.contains(q, ignoreCase = true) }
        }
        if (seasonKey != WardrobeCategories.ALL) {
            list = list.filter { seasonKey in it.seasons }
        }
        list.sortedForDisplay(sortOption)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Search",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Text(
                            "Filter & sort",
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
                actions = { Spacer(Modifier.padding(horizontal = 64.dp)) },
                colors = TopAppBarDefaults.topAppBarColors(
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
                .padding(padding),
        ) {
            OutlinedTextField(
                value = nameQuery,
                onValueChange = { nameQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                placeholder = { Text("Search pieces…") },
                singleLine = true,
            )

            Text(
                "CATEGORY",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.4.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(10.dp))
            FlowRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
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

            Spacer(Modifier.height(16.dp))
            Text(
                "SEASON",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.4.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(10.dp))
            FlowRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SearchSeasonFilters.forEach { (key, label) ->
                    FilterChip(
                        selected = seasonKey == key,
                        onClick = { seasonKey = key },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = sortMode == "recent",
                    onClick = { sortMode = "recent" },
                    label = { Text("Recently added") },
                )
                FilterChip(
                    selected = sortMode == "worn",
                    onClick = { sortMode = "worn" },
                    label = { Text("Most worn") },
                )
                FilterChip(
                    selected = sortMode == "name",
                    onClick = { sortMode = "name" },
                    label = { Text("A → Z") },
                )
            }

            if (displayed.isEmpty()) {
                EmptyWardrobeState(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    hasItemsInWardrobe = items.isNotEmpty(),
                    filtersExcludeAll = items.isNotEmpty() && afterCategory.isNotEmpty() && displayed.isEmpty(),
                    emptyTitle = when {
                        items.isNotEmpty() && nameQuery.isNotBlank() -> "No name matches"
                        else -> null
                    },
                    emptySubtitle = when {
                        items.isNotEmpty() && nameQuery.isNotBlank() ->
                            "Try a different spelling or clear the search box."
                        else -> null
                    },
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(displayed, key = { it.id }) { item ->
                        WardrobeSearchResultRow(
                            item = item,
                            onClick = { onItemClick(item.id) },
                        )
                    }
                }
            }
        }
    }
}
