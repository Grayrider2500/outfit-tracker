package com.dressed.app.ui.picker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dressed.app.BuildConfig
import com.dressed.app.DressedApplication
import com.dressed.app.data.picker.PickerAnthropicReasoner
import com.dressed.app.data.OutfitRepository
import com.dressed.app.data.WardrobeRepository
import com.dressed.app.data.local.OutfitEntity
import com.dressed.app.data.picker.WardrobePickerEngine
import com.dressed.app.data.picker.WardrobePickerEngine.PickerSuggestion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

class PickerViewModel(
    application: Application,
    private val wardrobeRepository: WardrobeRepository,
    private val outfitRepository: OutfitRepository,
) : AndroidViewModel(application) {

    private val _suggestions = MutableStateFlow<List<PickerSuggestion>>(emptyList())
    val suggestions: StateFlow<List<PickerSuggestion>> = _suggestions.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    fun generate(occasionId: String, weatherTagIds: Set<String>, moodTagIds: Set<String>) {
        viewModelScope.launch {
            _busy.value = true
            try {
                val items = wardrobeRepository.getAllSnapshot()
                val seed = System.nanoTime()
                val now = System.currentTimeMillis()
                val occasionLabel =
                    WardrobePickerEngine.OCCASIONS.find { it.id == occasionId }?.label ?: "Outfit"
                val weatherLabels = weatherTagIds.mapNotNull { tid ->
                    WardrobePickerEngine.WEATHER_TAGS.find { it.first == tid }?.second
                }
                val moodLabels = moodTagIds.mapNotNull { mid ->
                    WardrobePickerEngine.MOOD_TAGS.find { it.first == mid }?.second
                }
                val base = WardrobePickerEngine.suggest(
                    allItems = items,
                    occasionId = occasionId,
                    weatherTagIds = weatherTagIds,
                    moodTagIds = moodTagIds,
                    seed = seed,
                    maxOutfits = 3,
                    nowEpochMs = now,
                )
                _suggestions.value =
                    if (BuildConfig.ANTHROPIC_API_KEY.isBlank()) {
                        base
                    } else {
                        PickerAnthropicReasoner.enrichReasons(
                            apiKey = BuildConfig.ANTHROPIC_API_KEY,
                            suggestions = base,
                            occasionLabel = occasionLabel,
                            weatherLabels = weatherLabels,
                            moodLabels = moodLabels,
                            nowEpochMs = now,
                        )
                    }
            } finally {
                _busy.value = false
            }
        }
    }

    fun saveAsNewOutfit(
        index: Int,
        occasionLabel: String,
        onDone: (error: String?) -> Unit,
    ) {
        val sug = _suggestions.value.getOrNull(index)
        if (sug == null) {
            onDone("No suggestion selected")
            return
        }
        viewModelScope.launch {
            runCatching {
                val day = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                val entity = OutfitEntity(
                    id = UUID.randomUUID().toString(),
                    name = "Picker · $occasionLabel · $day",
                    itemIds = sug.itemIds,
                    wornCount = 0,
                    createdAtEpochMs = System.currentTimeMillis(),
                )
                outfitRepository.insert(entity)
            }.fold(
                onSuccess = { onDone(null) },
                onFailure = { e -> onDone(e.message ?: "Save failed") },
            )
        }
    }

    fun wearToday(index: Int, onDone: (error: String?) -> Unit) {
        val sug = _suggestions.value.getOrNull(index)
        if (sug == null) {
            onDone("No suggestion selected")
            return
        }
        viewModelScope.launch {
            runCatching {
                for (id in sug.itemIds) {
                    wardrobeRepository.incrementWearCount(id)
                }
            }.fold(
                onSuccess = { onDone(null) },
                onFailure = { e -> onDone(e.message ?: "Could not update wear counts") },
            )
        }
    }

    companion object {
        fun factory(app: DressedApplication): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass == PickerViewModel::class.java)
                    return PickerViewModel(app, app.wardrobeRepository, app.outfitRepository) as T
                }
            }
    }
}
