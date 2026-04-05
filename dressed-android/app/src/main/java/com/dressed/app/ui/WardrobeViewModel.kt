package com.dressed.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dressed.app.BuildConfig
import com.dressed.app.DressedApplication
import com.dressed.app.data.OutfitRepository
import com.dressed.app.data.WardrobeRepository
import com.dressed.app.data.backup.WardrobeBackupCodec
import com.dressed.app.data.dev.TestDataSeeder
import com.dressed.app.data.local.ImageStorage
import com.dressed.app.data.local.WardrobeItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class WardrobeViewModel(
    application: Application,
    private val repository: WardrobeRepository,
    private val outfitRepository: OutfitRepository,
) : AndroidViewModel(application) {

    val items: Flow<List<WardrobeItemEntity>> = repository.observeAll()

    fun observeItem(id: String): Flow<WardrobeItemEntity?> = repository.observeById(id)

    /** Number of saved outfits that include this wardrobe item. */
    fun observeOutfitCountForItem(itemId: String): Flow<Int> =
        outfitRepository.observeAll().map { outfits ->
            outfits.count { outfit -> itemId in outfit.itemIds }
        }

    fun addItem(
        name: String,
        category: String,
        sizeLabel: String,
        colorHex: String,
        colorName: String,
        seasons: List<String>,
        occasions: List<String> = emptyList(),
        photoUri: Uri?,
        onInserted: () -> Unit = {},
    ) {
        viewModelScope.launch {
            val path = photoUri?.let { ImageStorage.copyFromUri(getApplication(), it) }
            val entity = WardrobeItemEntity(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                category = category,
                sizeLabel = sizeLabel.trim(),
                colorHex = colorHex,
                colorName = colorName,
                seasons = seasons,
                occasions = occasions,
                photoPath = path,
                wornCount = 0,
                lastWornAtEpochMs = null,
                addedAtEpochMs = System.currentTimeMillis(),
            )
            repository.insert(entity)
            onInserted()
        }
    }

    fun markWorn(id: String) {
        viewModelScope.launch { repository.incrementWearCount(id) }
    }

    fun deleteItem(id: String) {
        viewModelScope.launch { repository.deleteItemAndPhoto(id) }
    }

    fun exportBackup(uri: Uri, onDone: (errorMessage: String?) -> Unit) {
        viewModelScope.launch {
            val err = withContext(Dispatchers.IO) {
                runCatching {
                    val items = repository.getAllSnapshot()
                    val outfits = outfitRepository.getAllSnapshot()
                    val out = getApplication<Application>().contentResolver.openOutputStream(uri)
                        ?: error("Could not open file for writing")
                    out.use { stream ->
                        WardrobeBackupCodec.writeZipArchive(getApplication(), items, outfits, stream)
                    }
                }.exceptionOrNull()?.message
            }
            onDone(err)
        }
    }

    /** Replace all wardrobe + outfit data with backup contents (matches iOS Replace All). */
    fun importBackupReplace(uri: Uri, onDone: (errorMessage: String?) -> Unit) {
        viewModelScope.launch {
            val err = withContext(Dispatchers.IO) {
                runCatching {
                    val pair = getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
                        WardrobeBackupCodec.readBackup(getApplication(), input).getOrThrow()
                    } ?: error("Could not read file")
                    repository.replaceAllWardrobe(pair.first, pair.second)
                }.exceptionOrNull()?.message
            }
            onDone(err)
        }
    }

    /** Merge backup: add items/outfits not already present by id (matches iOS Merge). */
    fun importBackupMerge(
        uri: Uri,
        onDone: (errorMessage: String?, snackbarMessage: String?) -> Unit,
    ) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val pair = getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
                        WardrobeBackupCodec.readBackup(getApplication(), input).getOrThrow()
                    } ?: error("Could not read file")
                    repository.mergeWardrobe(pair.first, pair.second)
                }
            }
            result.fold(
                onSuccess = { (newItems, newOutfits) ->
                    val parts = buildList {
                        if (newItems > 0) add("$newItems item" + if (newItems == 1) "" else "s")
                        if (newOutfits > 0) add("$newOutfits outfit" + if (newOutfits == 1) "" else "s")
                    }
                    val snack = if (parts.isEmpty()) "No new data to merge" else "Merged ${parts.joinToString(", ")}"
                    onDone(null, snack)
                },
                onFailure = { e ->
                    onDone(e.message ?: "Merge failed", null)
                },
            )
        }
    }

    /**
     * Debug only: inserts a fixed professional-closet dataset and 9 outfits when missing.
     */
    fun seedDebugTestData(onDone: (message: String) -> Unit) {
        if (!BuildConfig.DEBUG) {
            onDone("Not available in release builds.")
            return
        }
        viewModelScope.launch {
            val msg = runCatching {
                val s = withContext(Dispatchers.IO) {
                    TestDataSeeder.run(getApplication<DressedApplication>().database)
                }
                "Added ${s.itemsAdded} items, ${s.outfitsAdded} outfits. " +
                    "Skipped ${s.itemsSkipped} items, ${s.outfitsSkipped} outfits (already in database)."
            }.getOrElse { e ->
                "Seed failed: ${e.message ?: "unknown error"}"
            }
            onDone(msg)
        }
    }

    companion object {
        fun factory(app: DressedApplication): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass == WardrobeViewModel::class.java)
                    return WardrobeViewModel(app, app.wardrobeRepository, app.outfitRepository) as T
                }
            }
    }
}
