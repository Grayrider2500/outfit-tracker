package com.dressed.app.ui

import android.app.Application
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dressed.app.BuildConfig
import com.dressed.app.DressedApplication
import com.dressed.app.data.OutfitRepository
import com.dressed.app.data.WardrobeRepository
import com.dressed.app.data.backup.LibraryShareCodec
import com.dressed.app.data.backup.WardrobeBackupCodec
import com.dressed.app.data.library.LibraryPreferences
import com.dressed.app.data.dev.TestDataSeeder
import com.dressed.app.data.local.ImageStorage
import com.dressed.app.data.local.WardrobeItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class WardrobeViewModel(
    application: Application,
    private val repository: WardrobeRepository,
    private val outfitRepository: OutfitRepository,
    private val libraryPreferences: LibraryPreferences,
) : AndroidViewModel(application) {

    val items: Flow<List<WardrobeItemEntity>> = repository.observeAll()

    fun observeItem(id: String): Flow<WardrobeItemEntity?> = repository.observeById(id)

    /** Number of saved outfits that include this wardrobe item. */
    fun observeOutfitCountForItem(itemId: String): Flow<Int> =
        outfitRepository.observeAll().map { outfits ->
            outfits.count { outfit -> itemId in outfit.itemIds }
        }

    /** All outfits that include this wardrobe item, by name (for display in item detail). */
    fun observeOutfitsForItem(itemId: String): Flow<List<String>> =
        outfitRepository.observeAll().map { outfits ->
            outfits.filter { itemId in it.itemIds }.map { it.name }
        }

    fun addItem(
        name: String,
        category: String,
        sizeLabel: String,
        colorHex: String,
        colorName: String,
        seasons: List<String>,
        occasions: List<String> = emptyList(),
        photoPath: String?,
        onInserted: () -> Unit = {},
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val entity = WardrobeItemEntity(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                category = category,
                sizeLabel = sizeLabel.trim(),
                colorHex = colorHex,
                colorName = colorName,
                seasons = seasons,
                occasions = occasions,
                photoPath = photoPath,
                wornCount = 0,
                lastWornAtEpochMs = null,
                addedAtEpochMs = System.currentTimeMillis(),
                lendable = false,
            )
            repository.insert(entity)
            withContext(Dispatchers.Main) { onInserted() }
        }
    }

    fun saveEdit(
        itemId: String,
        name: String,
        category: String,
        sizeLabel: String,
        colorHex: String,
        colorName: String,
        seasons: List<String>,
        occasions: List<String>,
        photoPath: String?,
        onSaved: () -> Unit = {},
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = repository.getById(itemId) ?: return@launch
            val oldPath = existing.photoPath
            repository.insert(
                existing.copy(
                    name = name.trim(),
                    category = category,
                    sizeLabel = sizeLabel.trim(),
                    colorHex = colorHex,
                    colorName = colorName,
                    seasons = seasons,
                    occasions = occasions,
                    photoPath = photoPath,
                ),
            )
            if (oldPath != null && oldPath != photoPath) {
                ImageStorage.deleteIfExists(oldPath)
            }
            withContext(Dispatchers.Main) { onSaved() }
        }
    }

    fun markWorn(id: String) {
        viewModelScope.launch { repository.incrementWearCount(id) }
    }

    fun deleteItem(id: String) {
        viewModelScope.launch { repository.deleteItemAndPhoto(id) }
    }

    /** Full row upsert (DAO uses OnConflictStrategy.REPLACE). Use for lendable, occasions, etc. */
    fun updateItem(entity: WardrobeItemEntity) {
        viewModelScope.launch { repository.insert(entity) }
    }

    fun saveSharerDisplayName(name: String) {
        libraryPreferences.sharerDisplayName = name.trim()
    }

    fun sharerDisplayName(): String = libraryPreferences.sharerDisplayName

    fun libraryExplainerSeen(): Boolean = libraryPreferences.libraryExplainerSeen

    fun setLibraryExplainerSeen(seen: Boolean) {
        libraryPreferences.libraryExplainerSeen = seen
    }

    /**
     * Writes a `.dressed-library` zip under [Application.getCacheDir] and returns a [FileProvider] URI
     * for [Intent.ACTION_SEND] (share sheet).
     */
    fun exportBorrowableLibraryForShare(
        sharerName: String,
        onDone: (errorMessage: String?, contentUri: Uri?) -> Unit,
    ) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val ctx = getApplication<Application>()
                    val trimmed = sharerName.trim()
                    if (trimmed.isEmpty()) error("Sharer name required")
                    val all = repository.getAllSnapshot()
                    val lendable = all.filter { it.lendable }
                    if (lendable.isEmpty()) error("No items marked available to lend")
                    val stem = trimmed.replace(Regex("[^a-zA-Z0-9._-]"), "_").ifEmpty { "shared" }
                    val file = File(ctx.cacheDir, "dressed-library-$stem.dressed-library")
                    file.outputStream().use { stream ->
                        LibraryShareCodec.writeLibraryZip(lendable, trimmed, stream)
                    }
                    FileProvider.getUriForFile(
                        ctx,
                        "${ctx.packageName}.fileprovider",
                        file,
                    )
                }
            }
            onDone(result.exceptionOrNull()?.message, result.getOrNull())
        }
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
                    return WardrobeViewModel(
                        app,
                        app.wardrobeRepository,
                        app.outfitRepository,
                        app.libraryPreferences,
                    ) as T
                }
            }
    }
}
