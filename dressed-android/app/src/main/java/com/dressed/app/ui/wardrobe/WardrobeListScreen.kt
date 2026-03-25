package com.dressed.app.ui.wardrobe

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.dressed.app.data.local.WardrobeItemEntity
import com.dressed.app.data.model.WardrobeCategories
import com.dressed.app.data.model.WardrobeSortOption
import com.dressed.app.data.model.sortedForDisplay
import com.dressed.app.ui.WardrobeViewModel
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WardrobeListScreen(
    viewModel: WardrobeViewModel,
    onAddClick: () -> Unit,
    onItemClick: (String) -> Unit,
) {
    val items by viewModel.items.collectAsStateWithLifecycle(initialValue = emptyList())
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
    ) {
        var list = afterCategory
        if (colorFilterName.isNotBlank()) {
            list = list.filter { it.colorName == colorFilterName }
        }
        if (sizeFilterLabel.isNotBlank()) {
            list = list.filter { it.sizeLabel == sizeFilterLabel }
        }
        list.sortedForDisplay(sortOption)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var overflowOpen by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            viewModel.exportBackup(uri) { err ->
                scope.launch {
                    if (err == null) {
                        snackbarHostState.showSnackbar("Backup saved")
                    } else {
                        snackbarHostState.showSnackbar("Backup failed: $err")
                    }
                }
            }
        }
    }

    val openBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) pendingRestoreUri = uri
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Dressed",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Text(
                            "MY PERSONAL WARDROBE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f),
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { overflowOpen = true }) {
                            Icon(
                                Icons.Filled.MoreVert,
                                contentDescription = "Backup and restore",
                                tint = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                        DropdownMenu(
                            expanded = overflowOpen,
                            onDismissRequest = { overflowOpen = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Backup to file…") },
                                onClick = {
                                    overflowOpen = false
                                    createBackupLauncher.launch("dressed-backup.json")
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Save, contentDescription = null)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Restore from file…") },
                                onClick = {
                                    overflowOpen = false
                                    openBackupLauncher.launch(
                                        arrayOf("application/json", "application/*", "*/*"),
                                    )
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.FolderOpen, contentDescription = null)
                                },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Wardrobe",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = itemCountLabel(items.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                "Category",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                WardrobeCategories.FILTERS.forEach { (key, label) ->
                    FilterChip(
                        selected = filterKey == key,
                        onClick = { filterKey = key },
                        label = { Text(label) },
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "Sort",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "Color",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FilterChip(
                        selected = colorFilterName.isEmpty(),
                        onClick = { colorFilterName = "" },
                        label = { Text("All") },
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
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "Size",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FilterChip(
                        selected = sizeFilterLabel.isEmpty(),
                        onClick = { sizeFilterLabel = "" },
                        label = { Text("All") },
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
                    Text("Clear color & size filters")
                }
            }

            Spacer(Modifier.height(8.dp))

            if (displayed.isEmpty()) {
                EmptyWardrobeState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    hasItemsInWardrobe = items.isNotEmpty(),
                    filtersExcludeAll = items.isNotEmpty() && afterCategory.isNotEmpty() && displayed.isEmpty(),
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
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

    pendingRestoreUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingRestoreUri = null },
            title = { Text("Replace wardrobe?") },
            text = {
                Text(
                    "Restoring replaces every piece in Dressed with the backup. " +
                        "Your current list and photos will be removed. This cannot be undone.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingRestoreUri = null
                        viewModel.importBackup(uri) { err ->
                            scope.launch {
                                if (err == null) {
                                    snackbarHostState.showSnackbar("Restore completed")
                                } else {
                                    snackbarHostState.showSnackbar("Restore failed: $err")
                                }
                            }
                        }
                    },
                ) {
                    Text("Restore", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreUri = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun EmptyWardrobeState(
    modifier: Modifier = Modifier,
    hasItemsInWardrobe: Boolean,
    filtersExcludeAll: Boolean,
) {
    val title = when {
        !hasItemsInWardrobe -> "Your wardrobe awaits"
        filtersExcludeAll -> "No pieces match these filters"
        else -> "Nothing in this category"
    }
    val subtitle = when {
        !hasItemsInWardrobe -> "Tap + to add your first piece."
        filtersExcludeAll -> "Try clearing color/size filters or choose another category or sort."
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

private fun itemCountLabel(count: Int): String =
    "$count item" + if (count != 1) "s" else ""

@Composable
private fun WardrobeItemCard(item: WardrobeItemEntity, onClick: () -> Unit) {
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
private fun ColorDot(hex: String) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(RoundedCornerShape(50))
            .background(parseColorSafe(hex)),
    )
}

private fun parseColorSafe(hex: String): Color =
    runCatching {
        Color(android.graphics.Color.parseColor(hex))
    }.getOrElse { Color.Gray }
