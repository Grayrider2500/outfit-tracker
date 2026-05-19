package com.dressed.app.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Date
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dressed.app.data.local.BorrowedLibraryListRow
import com.dressed.app.ui.LibrariesViewModel
import com.dressed.app.ui.wardrobe.itemCountLabel as wardrobeItemCountLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibrariesListScreen(
    viewModel: LibrariesViewModel,
    onNavigateHome: () -> Unit,
    onLibraryClick: (libraryId: String, title: String) -> Unit,
) {
    val rows by viewModel.libraryRows.collectAsStateWithLifecycle(initialValue = emptyList())
    var pendingDelete by remember { mutableStateOf<BorrowedLibraryListRow?>(null) }

    pendingDelete?.let { row ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Remove library?") },
            text = {
                Text(
                    "Remove ${row.sharerName}'s library? Your own wardrobe is not affected.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeLibrary(row.id)
                        pendingDelete = null
                    },
                ) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Borrowed libraries", color = MaterialTheme.colorScheme.onPrimary) },
                navigationIcon = {
                    TextButton(onClick = onNavigateHome) {
                        Text("← Home", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.92f))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
    ) { padding ->
        if (rows.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    "No borrowed libraries yet",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "Ask a friend to share theirs.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(rows, key = { it.id }) { row ->
                    LibraryRowCard(
                        row = row,
                        onClick = { onLibraryClick(row.id, row.sharerName) },
                        onRemoveClick = { pendingDelete = row },
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryRowCard(
    row: BorrowedLibraryListRow,
    onClick: () -> Unit,
    onRemoveClick: () -> Unit,
) {
    val importedLabel = remember(row.importedAtEpochMs) {
        DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(row.importedAtEpochMs))
    }
    var menuOpen by remember { mutableStateOf(false) }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${row.sharerName}'s Library",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    wardrobeItemCountLabel(row.itemCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    "Imported $importedLabel",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Remove library") },
                        onClick = {
                            menuOpen = false
                            onRemoveClick()
                        },
                    )
                }
            }
        }
    }
}
