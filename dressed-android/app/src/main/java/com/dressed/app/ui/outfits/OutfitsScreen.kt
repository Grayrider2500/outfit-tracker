package com.dressed.app.ui.outfits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dressed.app.ui.WardrobeViewModel

private const val ROUTE_OUTFITS_LIST = "outfits_list"
private const val ROUTE_CREATE_OUTFIT = "outfits_create"

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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OutfitsListScreen(
    wardrobeViewModel: WardrobeViewModel,
    outfitsViewModel: OutfitsViewModel,
    onNavigateHome: () -> Unit,
    onCreateOutfit: () -> Unit,
) {
    val outfits by outfitsViewModel.outfits.collectAsStateWithLifecycle(initialValue = emptyList())
    val wardrobeItems by wardrobeViewModel.items.collectAsStateWithLifecycle(initialValue = emptyList())
    val itemsById = remember(wardrobeItems) { wardrobeItems.associateBy { it.id } }

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
        if (outfits.isEmpty()) {
            EmptyOutfitsState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp),
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                    bottom = 80.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(outfits, key = { it.id }) { outfit ->
                    val items = remember(outfit.itemIds, itemsById) {
                        outfit.itemIds.mapNotNull { itemsById[it] }
                    }
                    OutfitCollageCard(
                        outfit = outfit,
                        items = items,
                        onClick = { /* outfit detail — future */ },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyOutfitsState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "No outfits yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "Tap + to build your first look from your wardrobe pieces.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
            textAlign = TextAlign.Center,
        )
    }
}
