package com.dressed.app.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dressed.app.ui.WardrobeViewModel
import com.dressed.app.ui.theme.WardrobeOnBarText
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandingScreen(
    viewModel: WardrobeViewModel,
    onMyWardrobe: () -> Unit,
    onSearchFilter: () -> Unit,
    onOutfits: () -> Unit,
) {
    val gradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF4A3370),
            Color(0xFF6B4AAD),
            Color(0xFF8B62D4),
        ),
    )

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var menuOpen by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            viewModel.exportBackup(uri) { err ->
                scope.launch {
                    if (err == null) snackbarHostState.showSnackbar("Backup saved")
                    else snackbarHostState.showSnackbar("Backup failed: $err")
                }
            }
        }
    }

    val openBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) pendingRestoreUri = uri }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(gradient)
                .statusBarsPadding(),
        ) {
            Box(Modifier.align(Alignment.TopEnd).padding(end = 16.dp, top = 8.dp)) {
                IconButton(
                    onClick = { menuOpen = true },
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = WardrobeOnBarText.copy(alpha = 0.92f),
                    ),
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.12f), CircleShape),
                ) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Menu")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Backup to file…") },
                        onClick = {
                            menuOpen = false
                            createBackupLauncher.launch("dressed-backup.json")
                        },
                        leadingIcon = { Icon(Icons.Outlined.Save, contentDescription = null) },
                    )
                    DropdownMenuItem(
                        text = { Text("Restore from file…") },
                        onClick = {
                            menuOpen = false
                            openBackupLauncher.launch(
                                arrayOf("application/json", "application/*", "*/*"),
                            )
                        },
                        leadingIcon = { Icon(Icons.Outlined.FolderOpen, contentDescription = null) },
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(48.dp))
                Surface(
                    shape = RoundedCornerShape(40.dp),
                    color = Color(0x409664FF),
                    border = BorderStroke(1.dp, Color(0x66B48CFF)),
                    modifier = Modifier.padding(top = 24.dp),
                ) {
                    Box(
                        modifier = Modifier.padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("👗", fontSize = 64.sp)
                    }
                }
                Text(
                    "Dressed",
                    style = MaterialTheme.typography.headlineLarge,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Normal,
                    color = WardrobeOnBarText,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(top = 24.dp),
                )
                Text(
                    "YOUR PERSONAL WARDROBE",
                    style = MaterialTheme.typography.labelSmall,
                    color = WardrobeOnBarText.copy(alpha = 0.55f),
                    letterSpacing = 3.sp,
                )
                Spacer(Modifier.height(28.dp))
                Box(
                    modifier = Modifier
                        .height(1.dp)
                        .fillMaxWidth(0.12f)
                        .background(WardrobeOnBarText.copy(alpha = 0.2f)),
                )
                Spacer(Modifier.height(28.dp))

                HubNavButton(
                    mainLabel = "My Wardrobe",
                    subLabel = "Browse & manage pieces",
                    onClick = onMyWardrobe,
                    emphasized = true,
                    icon = Icons.Filled.Checkroom,
                )
                Spacer(Modifier.height(12.dp))
                HubNavButton(
                    mainLabel = "Search & Filter",
                    subLabel = "Find by category, color, season",
                    onClick = onSearchFilter,
                    emphasized = false,
                    icon = Icons.Filled.Search,
                )
                Spacer(Modifier.height(12.dp))
                HubNavButton(
                    mainLabel = "Outfits",
                    subLabel = "Build and save your looks",
                    onClick = onOutfits,
                    emphasized = false,
                    icon = Icons.Filled.AutoAwesome,
                )

                Spacer(modifier = Modifier.weight(1f))
                Text(
                    "DRESSED · PERSONAL",
                    style = MaterialTheme.typography.labelSmall,
                    color = WardrobeOnBarText.copy(alpha = 0.3f),
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(bottom = 40.dp),
                )
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
                                if (err == null) snackbarHostState.showSnackbar("Restore completed")
                                else snackbarHostState.showSnackbar("Restore failed: $err")
                            }
                        }
                    },
                ) {
                    Text("Restore", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreUri = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun HubNavButton(
    mainLabel: String,
    subLabel: String,
    onClick: () -> Unit,
    emphasized: Boolean,
    icon: ImageVector? = null,
) {
    val border = if (emphasized) Color(0x73B48CFF) else Color(0x40B48CFF)
    val bg = if (emphasized) Color(0x598C5AFF) else Color(0x268C5AFF)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = bg,
        border = BorderStroke(1.dp, border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = WardrobeOnBarText.copy(alpha = 0.85f),
                    modifier = Modifier.padding(end = 14.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    mainLabel,
                    color = WardrobeOnBarText,
                    fontSize = 16.sp,
                    letterSpacing = 1.sp,
                )
                Text(
                    subLabel,
                    color = WardrobeOnBarText.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Text(
                "›",
                color = WardrobeOnBarText.copy(alpha = 0.45f),
                fontSize = 22.sp,
            )
        }
    }
}
