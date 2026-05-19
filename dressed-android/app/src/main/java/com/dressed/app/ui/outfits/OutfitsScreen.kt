package com.dressed.app.ui.outfits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dressed.app.ui.WardrobeViewModel

// ── Sort + filter constants ──────────────────────────────────────────────────

private val OUTFIT_SORT_OPTIONS = listOf(
    "newest" to "Newest",
    "worn"   to "Most Worn",
    "name"   to "A–Z",
)

private val OUTFIT_SEASON_FILTERS = listOf(
    "all"    to "All seasons",
    "spring" to "Spring",
    "summer" to "Summer",
    "fall"   to "Autumn",
    "winter" to "Winter",
)

private val OUTFIT_SIZE_FILTERS = listOf(
    "any" to "Any size",
    "1"   to "Solo",
    "2-3" to "2–3 pcs",
    "4+"  to "4 + pcs",
)

private const val ROUTE_OUTFITS_LIST = "outfits_list"
private const val ROUTE_CREATE_OUTFIT = "outfits_create"
private const val ROUTE_OUTFIT_DETAIL = "outfits_detail/{id}"
private const val ROUTE_EDIT_OUTFIT = "outfits_edit/{id}"

@Composable
fun OutfitsNav(
    wardrobeViewModel: WardrobeViewModel,
    outfitsViewModel: OutfitsViewModel,
    onNavigateHome: () -> Unit,
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = ROUTE_OUTFITS_LIST,
    ) {
        composable(ROUTE_OUTFITS_LIST) {
            OutfitsListScreen(
                wardrobeViewModel = wardrobeViewModel,
                outfitsViewModel = outfitsViewModel,
                onNavigateHome = onNavigateHome,
                onCreateOutfit = { navController.navigate(ROUTE_CREATE_OUTFIT) },
                onOutfitClick = { id -> navController.navigate("outfits_detail/$id") },
            )
        }
        composable(ROUTE_CREATE_OUTFIT) {
            CreateOutfitScreen(
                wardrobeViewModel = wardrobeViewModel,
                outfitsViewModel = outfitsViewModel,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }
        composable(
            route = ROUTE_OUTFIT_DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            OutfitDetailScreen(
                outfitId = id,
                wardrobeViewModel = wardrobeViewModel,
                outfitsViewModel = outfitsViewModel,
                onBack = { navController.popBackStack() },
                onEdit = { editId -> navController.navigate("outfits_edit/$editId") },
            )
        }
        composable(
            route = ROUTE_EDIT_OUTFIT,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            EditOutfitScreen(
                outfitId = id,
                wardrobeViewModel = wardrobeViewModel,
                outfitsViewModel = outfitsViewModel,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OutfitsListScreen(
    wardrobeViewModel: WardrobeViewModel,
    outfitsViewModel: OutfitsViewModel,
    onNavigateHome: () -> Unit,
    onCreateOutfit: () -> Unit,
    onOutfitClick: (String) -> Unit,
) {
    val outfits by outfitsViewModel.outfits.collectAsStateWithLifecycle(initialValue = emptyList())
    val wardrobeItems by wardrobeViewModel.items.collectAsStateWithLifecycle(initialValue = emptyList())
    val itemsById = remember(wardrobeItems) { wardrobeItems.associateBy { it.id } }

    var sortMode    by rememberSaveable { mutableStateOf("newest") }
    var seasonFilter by rememberSaveable { mutableStateOf("all") }
    var sizeFilter  by rememberSaveable { mutableStateOf("any") }

    val displayedOutfits = remember(outfits, itemsById, sortMode, seasonFilter, sizeFilter) {
        var list = outfits

        // Season filter — inferred from constituent wardrobe pieces
        if (seasonFilter != "all") {
            list = list.filter { outfit ->
                outfit.itemIds.any { id -> itemsById[id]?.seasons?.contains(seasonFilter) == true }
            }
        }

        // Piece-count filter
        list = when (sizeFilter) {
            "1"   -> list.filter { it.itemIds.size == 1 }
            "2-3" -> list.filter { it.itemIds.size in 2..3 }
            "4+"  -> list.filter { it.itemIds.size >= 4 }
            else  -> list
        }

        // Sort
        when (sortMode) {
            "worn" -> list.sortedByDescending { it.wornCount }
            "name" -> list.sortedBy { it.name.lowercase() }
            else   -> list.sortedByDescending { it.createdAtEpochMs }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Outfits",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateOutfit,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Create outfit")
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // ── Sort chips ──────────────────────────────────────────────
            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(OUTFIT_SORT_OPTIONS) { (key, label) ->
                    FilterChip(
                        selected = sortMode == key,
                        onClick = { sortMode = key },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }

            // ── Season chips ────────────────────────────────────────────
            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(OUTFIT_SEASON_FILTERS) { (key, label) ->
                    FilterChip(
                        selected = seasonFilter == key,
                        onClick = { seasonFilter = key },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }

            // ── Piece-count chips ───────────────────────────────────────
            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(OUTFIT_SIZE_FILTERS) { (key, label) ->
                    FilterChip(
                        selected = sizeFilter == key,
                        onClick = { sizeFilter = key },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // ── Grid / empty state ──────────────────────────────────────
            if (displayedOutfits.isEmpty()) {
                EmptyOutfitsState(
                    noOutfitsAtAll = outfits.isEmpty(),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 80.dp,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    gridItems(displayedOutfits, key = { it.id }) { outfit ->
                        val items = remember(outfit.itemIds, itemsById) {
                            outfit.itemIds.mapNotNull { itemsById[it] }
                        }
                        OutfitCollageCard(
                            outfit = outfit,
                            items = items,
                            onClick = { onOutfitClick(outfit.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyOutfitsState(
    noOutfitsAtAll: Boolean,
    modifier: Modifier = Modifier,
) {
    val title = if (noOutfitsAtAll) "No outfits yet" else "No outfits match"
    val body  = if (noOutfitsAtAll) {
        "Tap + to build your first look from your wardrobe pieces."
    } else {
        "Try adjusting the season or size filters above."
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
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
            textAlign = TextAlign.Center,
        )
    }
}
