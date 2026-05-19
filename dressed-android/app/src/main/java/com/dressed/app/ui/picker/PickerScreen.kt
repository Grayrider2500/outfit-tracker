package com.dressed.app.ui.picker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dressed.app.data.local.WardrobeItemEntity
import com.dressed.app.data.model.WardrobeCategories
import com.dressed.app.data.picker.PickerAiBannerState
import com.dressed.app.data.picker.PickerAIProvider
import com.dressed.app.data.picker.WardrobePickerEngine
import com.dressed.app.ui.outfits.SuggestionOutfitCollageCard
import com.dressed.app.ui.outfits.pickerItemsSortedForDisplay
import com.dressed.app.ui.theme.DressedTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PickerScreen(
    viewModel: PickerViewModel,
    onNavigateHome: () -> Unit,
) {
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val aiBanner by viewModel.aiBanner.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showAiSettings by rememberSaveable { mutableStateOf(false) }
    var detailSuggestion by remember { mutableStateOf<WardrobePickerEngine.PickerSuggestion?>(null) }

    var occasionId by rememberSaveable { mutableStateOf(WardrobePickerEngine.OCCASIONS.first().id) }
    var weatherIds by remember { mutableStateOf(emptySet<String>()) }
    var moodIds by remember { mutableStateOf(emptySet<String>()) }

    val occasionLabel =
        WardrobePickerEngine.OCCASIONS.find { it.id == occasionId }?.label ?: "Outfit"

    Scaffold(
        modifier = Modifier.fillMaxSize(),
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
        bottomBar = {
            PickerSurpriseMeBottomBar(
                busy = busy,
                onSurpriseMe = { viewModel.generate(occasionId, weatherIds, moodIds) },
            )
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                PickerScrollableContent(
                    modifier = Modifier.fillMaxWidth(),
                    suggestions = suggestions,
                    busy = busy,
                    aiBanner = aiBanner,
                    occasionId = occasionId,
                    onOccasionIdChange = { occasionId = it },
                    weatherIds = weatherIds,
                    onWeatherIdsChange = { weatherIds = it },
                    moodIds = moodIds,
                    onMoodIdsChange = { moodIds = it },
                    onOpenAiSettings = { showAiSettings = true },
                    onSaveOutfit = { suggestionIndex ->
                        viewModel.saveAsNewOutfit(suggestionIndex, occasionLabel) { err ->
                            scope.launch {
                                if (err == null) snackbarHostState.showSnackbar("Saved as new outfit")
                                else snackbarHostState.showSnackbar(err)
                            }
                        }
                    },
                    onWearToday = { suggestionIndex ->
                        viewModel.wearToday(suggestionIndex) { err ->
                            scope.launch {
                                if (err == null) snackbarHostState.showSnackbar("Marked pieces as worn today")
                                else snackbarHostState.showSnackbar(err)
                            }
                        }
                    },
                    onSuggestionClick = { detailSuggestion = it },
                )
            }
            SuggestionDetailBottomSheet(
                suggestion = detailSuggestion,
                onDismiss = { detailSuggestion = null },
            )
            PickerAiSettingsSheet(
                visible = showAiSettings,
                store = viewModel.aiStore,
                onDismissRequest = {
                    showAiSettings = false
                    viewModel.refreshAiBanner()
                },
                onPrefsChanged = { viewModel.refreshAiBanner() },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun PickerScrollableContent(
    modifier: Modifier = Modifier,
    suggestions: List<WardrobePickerEngine.PickerSuggestion>,
    busy: Boolean,
    aiBanner: PickerAiBannerModel,
    occasionId: String,
    onOccasionIdChange: (String) -> Unit,
    weatherIds: Set<String>,
    onWeatherIdsChange: (Set<String>) -> Unit,
    moodIds: Set<String>,
    onMoodIdsChange: (Set<String>) -> Unit,
    onOpenAiSettings: () -> Unit,
    onSaveOutfit: (Int) -> Unit,
    onWearToday: (Int) -> Unit,
    onSuggestionClick: (WardrobePickerEngine.PickerSuggestion) -> Unit,
) {
    val sectionGap = 4.dp
    val chipSpacing = 6.dp
    val chipFlowTopPadding = 2.dp
    // Insets from the top bar, bottom bar, and system bars are applied by the Scaffold body wrapper.
    // Extra bottom padding so the last suggestion clears the pinned bottom bar (including nav-bar inset).
    val listPadding = PaddingValues(
        start = 12.dp,
        end = 12.dp,
        top = 2.dp,
        bottom = 32.dp,
    )

    LazyColumn(
        modifier = modifier,
        contentPadding = listPadding,
        verticalArrangement = Arrangement.spacedBy(sectionGap),
    ) {
        item {
            AiPickerStatusBanner(
                model = aiBanner,
                onOpenSettings = onOpenAiSettings,
            )
        }

        item {
            Text("Occasion", style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(chipSpacing),
                verticalArrangement = Arrangement.spacedBy(chipSpacing),
                modifier = Modifier.padding(top = chipFlowTopPadding),
            ) {
                for (o in WardrobePickerEngine.OCCASIONS) {
                    FilterChip(
                        selected = occasionId == o.id,
                        onClick = { onOccasionIdChange(o.id) },
                        label = { Text(o.label) },
                    )
                }
            }
        }

        item {
            Text("Weather (optional)", style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(chipSpacing),
                verticalArrangement = Arrangement.spacedBy(chipSpacing),
                modifier = Modifier.padding(top = chipFlowTopPadding),
            ) {
                for ((id, label) in WardrobePickerEngine.WEATHER_TAGS) {
                    val sel = weatherIds.contains(id)
                    FilterChip(
                        selected = sel,
                        onClick = {
                            onWeatherIdsChange(if (sel) weatherIds - id else weatherIds + id)
                        },
                        label = { Text(label) },
                    )
                }
            }
        }

        item {
            Text("Mood (optional)", style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(chipSpacing),
                verticalArrangement = Arrangement.spacedBy(chipSpacing),
                modifier = Modifier.padding(top = chipFlowTopPadding),
            ) {
                for ((id, label) in WardrobePickerEngine.MOOD_TAGS) {
                    val sel = moodIds.contains(id)
                    FilterChip(
                        selected = sel,
                        onClick = {
                            onMoodIdsChange(if (sel) moodIds - id else moodIds + id)
                        },
                        label = { Text(label) },
                    )
                }
            }
        }

        when {
            busy -> {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(36.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp,
                        )
                        Text(
                            "Checking your wardrobe and building suggestions...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }
            }
            suggestions.isEmpty() -> {
                item {
                    Text(
                        "Pick an occasion, then tap Surprise me below. Add tops, bottoms, or dresses first.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                    )
                }
            }
            else -> {
                itemsIndexed(
                    items = suggestions,
                    key = { index, sug -> "${sug.title}:$index:${sug.score}" },
                ) { index, sug ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SuggestionOutfitCollageCard(
                            title = sug.title,
                            items = sug.items,
                            subtitle = "${sug.reason}\n${sug.items.size} pieces · score ${"%.0f".format(sug.score)}",
                            onClick = { onSuggestionClick(sug) },
                        )
                        RowButtons(
                            onSave = { onSaveOutfit(index) },
                            onWear = { onWearToday(index) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerSurpriseMeBottomBar(
    busy: Boolean,
    onSurpriseMe: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            if (busy) {
                Text(
                    text = "Checking your wardrobe and building suggestions...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    textAlign = TextAlign.Center,
                )
            }
            Button(
                onClick = onSurpriseMe,
                enabled = !busy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    disabledContainerColor = MaterialTheme.colorScheme.primary,
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    if (busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                        Text(
                            "Building outfits...",
                            modifier = Modifier.padding(start = 10.dp),
                        )
                    } else {
                        Text("Surprise me")
                    }
                }
            }
        }
    }
}

@Composable
private fun AiPickerStatusBanner(
    model: PickerAiBannerModel,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (state, p) = model
    val (title, subtitle) = aiBannerStrings(state, p)
    val (container, content) = when (state) {
        PickerAiBannerState.FEATURE_DISABLED_IN_BUILD ->
            MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        PickerAiBannerState.NEEDS_KEY ->
            MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        PickerAiBannerState.KEY_SAVED_REASONING_OFF ->
            MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        PickerAiBannerState.READY ->
            MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenSettings),
        shape = MaterialTheme.shapes.medium,
        color = container.copy(alpha = 0.45f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = content)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.labelLarge, color = content)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = content.copy(alpha = 0.85f),
                )
            }
            Text("›", style = MaterialTheme.typography.labelLarge, color = content)
        }
    }
}

private fun aiBannerStrings(state: PickerAiBannerState, p: PickerAIProvider): Pair<String, String> {
    return when (state) {
        PickerAiBannerState.FEATURE_DISABLED_IN_BUILD ->
            "AI cloud explanations" to "Not enabled in this build — use a debug build for BYOK."
        PickerAiBannerState.NEEDS_KEY -> {
            if (p == PickerAIProvider.GROK) {
                "AI explanations (Grok)" to "Grok isn’t available yet — pick Anthropic or OpenAI, or use on-device hints."
            } else {
                "AI outfit explanations" to "Add your ${p.shortBannerLabel} key to enable (optional). Tap to open settings."
            }
        }
        PickerAiBannerState.KEY_SAVED_REASONING_OFF ->
            "AI reasoning is off" to "${p.shortBannerLabel} key saved — tap to turn explanations back on."
        PickerAiBannerState.READY ->
            "AI reasoning on · ${p.shortBannerLabel}" to "${p.shortBannerLabel} adds short reasons to each suggestion."
    }
}

@Composable
private fun RowButtons(onSave: () -> Unit, onWear: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
            Text("Save as new outfit")
        }
        OutlinedButton(onClick = onWear, modifier = Modifier.fillMaxWidth()) {
            Text("Wear today")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Preview(
    name = "Picker — small phone, empty",
    showBackground = true,
    heightDp = 480,
    widthDp = 360,
)
@Composable
private fun PickerPreviewSmallPhoneEmpty() {
    PickerScrollablePreviewShell(suggestions = emptyList(), busy = false)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Preview(
    name = "Picker — small phone, suggestions",
    showBackground = true,
    heightDp = 480,
    widthDp = 360,
)
@Composable
private fun PickerPreviewSmallPhoneWithSuggestions() {
    PickerScrollablePreviewShell(suggestions = pickerPreviewSampleSuggestions(), busy = false)
}

private fun pickerPreviewSampleSuggestions(): List<WardrobePickerEngine.PickerSuggestion> {
    val items = listOf(
        WardrobeItemEntity(
            id = "p1",
            name = "Oxford",
            category = WardrobeCategories.TOPS,
            sizeLabel = "",
            colorHex = "#c4b498",
            colorName = "Tan",
            seasons = emptyList(),
            occasions = emptyList(),
            photoPath = null,
            wornCount = 0,
            lastWornAtEpochMs = null,
            addedAtEpochMs = 0L,
            lendable = false,
        ),
        WardrobeItemEntity(
            id = "p2",
            name = "Chinos",
            category = WardrobeCategories.BOTTOMS,
            sizeLabel = "",
            colorHex = "#2e4057",
            colorName = "Navy",
            seasons = emptyList(),
            occasions = emptyList(),
            photoPath = null,
            wornCount = 0,
            lastWornAtEpochMs = null,
            addedAtEpochMs = 0L,
            lendable = false,
        ),
        WardrobeItemEntity(
            id = "p3",
            name = "Sneakers",
            category = WardrobeCategories.SHOES,
            sizeLabel = "",
            colorHex = "#ffffff",
            colorName = "White",
            seasons = emptyList(),
            occasions = emptyList(),
            photoPath = null,
            wornCount = 0,
            lastWornAtEpochMs = null,
            addedAtEpochMs = 0L,
            lendable = false,
        ),
    )
    return listOf(
        WardrobePickerEngine.PickerSuggestion(
            title = "Casual · Look 1",
            items = items,
            score = 42.0,
            reason = "In-season · tags match",
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun PickerScrollablePreviewShell(
    suggestions: List<WardrobePickerEngine.PickerSuggestion>,
    busy: Boolean,
) {
    DressedTheme {
        val snackbarHostState = remember { SnackbarHostState() }
        var occasionId by remember { mutableStateOf(WardrobePickerEngine.OCCASIONS.first().id) }
        var weatherIds by remember { mutableStateOf(emptySet<String>()) }
        var moodIds by remember { mutableStateOf(emptySet<String>()) }
        Scaffold(
            modifier = Modifier.fillMaxSize(),
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
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                )
            },
            bottomBar = {
                PickerSurpriseMeBottomBar(busy = busy, onSurpriseMe = {})
            },
        ) { innerPadding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                PickerScrollableContent(
                    modifier = Modifier.fillMaxWidth(),
                    suggestions = suggestions,
                    busy = busy,
                    aiBanner = PickerAiBannerModel(
                        PickerAiBannerState.FEATURE_DISABLED_IN_BUILD,
                        PickerAIProvider.OPENAI,
                    ),
                    occasionId = occasionId,
                    onOccasionIdChange = { occasionId = it },
                    weatherIds = weatherIds,
                    onWeatherIdsChange = { weatherIds = it },
                    moodIds = moodIds,
                    onMoodIdsChange = { moodIds = it },
                    onOpenAiSettings = {},
                    onSaveOutfit = {},
                    onWearToday = {},
                    onSuggestionClick = {},
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuggestionDetailBottomSheet(
    suggestion: WardrobePickerEngine.PickerSuggestion?,
    onDismiss: () -> Unit,
) {
    if (suggestion == null) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Text(
                text = suggestion.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = suggestion.reason,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = "${suggestion.items.size} pieces · score ${"%.0f".format(suggestion.score)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp),
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            Text(
                text = "All pieces",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            for (item in pickerItemsSortedForDisplay(suggestion.items)) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    val headline = buildString {
                        val c = item.colorName.trim()
                        val n = item.name.trim()
                        when {
                            c.isNotEmpty() && n.isNotEmpty() -> append("$c $n")
                            n.isNotEmpty() -> append(n)
                            c.isNotEmpty() -> append(c)
                            else -> append(WardrobeCategories.label(item.category))
                        }
                    }
                    Text(
                        text = headline,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = WardrobeCategories.label(item.category) +
                            item.sizeLabel.trim().takeIf { it.isNotEmpty() }?.let { " · $it" }.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
