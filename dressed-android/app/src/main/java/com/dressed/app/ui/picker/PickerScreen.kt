package com.dressed.app.ui.picker

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dressed.app.data.picker.WardrobePickerEngine
import com.dressed.app.ui.outfits.SuggestionOutfitCollageCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun PickerScreen(
    viewModel: PickerViewModel,
    onNavigateHome: () -> Unit,
) {
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var occasionId by rememberSaveable { mutableStateOf(WardrobePickerEngine.OCCASIONS.first().id) }
    var weatherIds by remember { mutableStateOf(emptySet<String>()) }
    var moodIds by remember { mutableStateOf(emptySet<String>()) }

    val occasionLabel =
        WardrobePickerEngine.OCCASIONS.find { it.id == occasionId }?.label ?: "Outfit"

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Suggest outfits",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Text(
                            "Rule-based picks from your wardrobe",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                        )
                    }
                },
                navigationIcon = {
                    androidx.compose.material3.TextButton(onClick = onNavigateHome) {
                        Text("← Home", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.92f))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Text("Occasion", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                for (o in WardrobePickerEngine.OCCASIONS) {
                    FilterChip(
                        selected = occasionId == o.id,
                        onClick = { occasionId = o.id },
                        label = { Text(o.label) },
                    )
                }
            }

            Text("Weather (optional)", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 16.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                for ((id, label) in WardrobePickerEngine.WEATHER_TAGS) {
                    val sel = weatherIds.contains(id)
                    FilterChip(
                        selected = sel,
                        onClick = {
                            weatherIds = if (sel) weatherIds - id else weatherIds + id
                        },
                        label = { Text(label) },
                    )
                }
            }

            Text("Mood (optional)", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 16.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                for ((id, label) in WardrobePickerEngine.MOOD_TAGS) {
                    val sel = moodIds.contains(id)
                    FilterChip(
                        selected = sel,
                        onClick = {
                            moodIds = if (sel) moodIds - id else moodIds + id
                        },
                        label = { Text(label) },
                    )
                }
            }

            Button(
                onClick = {
                    viewModel.generate(occasionId, weatherIds, moodIds)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
                    .height(52.dp),
                enabled = !busy,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Surprise me")
                }
            }

            Spacer(Modifier.height(20.dp))

            if (suggestions.isEmpty() && !busy) {
                Text(
                    "Pick an occasion and tap Surprise me. Add tops, bottoms, or dresses first.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                )
            } else if (busy) {
                Text("Building suggestions…", modifier = Modifier.padding(16.dp))
            } else {
                val pagerState = rememberPagerState(pageCount = { suggestions.size })
                HorizontalPager(
                    state = pagerState,
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    pageSpacing = 12.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp),
                ) { page ->
                    val sug = suggestions[page]
                    SuggestionOutfitCollageCard(
                        title = sug.title,
                        items = sug.items,
                        subtitle = "${sug.reason}\n${sug.items.size} pieces · score ${"%.0f".format(sug.score)}",
                    )
                }

                val page = pagerState.currentPage.coerceIn(0, suggestions.lastIndex.coerceAtLeast(0))
                RowButtons(
                    onSave = {
                        viewModel.saveAsNewOutfit(page, occasionLabel) { err ->
                            scope.launch {
                                if (err == null) snackbarHostState.showSnackbar("Saved as new outfit")
                                else snackbarHostState.showSnackbar(err)
                            }
                        }
                    },
                    onWear = {
                        viewModel.wearToday(page) { err ->
                            scope.launch {
                                if (err == null) snackbarHostState.showSnackbar("Marked pieces as worn today")
                                else snackbarHostState.showSnackbar(err)
                            }
                        }
                    },
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun RowButtons(onSave: () -> Unit, onWear: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
            Text("Save as new outfit")
        }
        OutlinedButton(onClick = onWear, modifier = Modifier.fillMaxWidth()) {
            Text("Wear today")
        }
    }
}
