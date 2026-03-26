package com.dressed.app.ui.wardrobe

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dressed.app.data.model.WardrobeSortOption
import com.dressed.app.data.model.sortedForDisplay
import com.dressed.app.ui.WardrobeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WardrobeListScreen(
    viewModel: WardrobeViewModel,
    onAddClick: () -> Unit,
    onItemClick: (String) -> Unit,
) {
    val items by viewModel.items.collectAsStateWithLifecycle(initialValue = emptyList())
    val displayed = remember(items) {
        items.sortedForDisplay(WardrobeSortOption.DATE_ADDED_DESC)
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
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = itemCountLabel(items.size),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Search tab: filters & sort",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (displayed.isEmpty()) {
                EmptyWardrobeState(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    hasItemsInWardrobe = false,
                    filtersExcludeAll = false,
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

