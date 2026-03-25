package com.dressed.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dressed.app.DressedApplication
import com.dressed.app.data.WardrobeRepository
import com.dressed.app.data.backup.WardrobeBackupCodec
import com.dressed.app.data.local.ImageStorage
import com.dressed.app.data.local.WardrobeItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.text.Charsets

class WardrobeViewModel(
    application: Application,
    private val repository: WardrobeRepository,
) : AndroidViewModel(application) {

    val items: Flow<List<WardrobeItemEntity>> = repository.observeAll()

    fun observeItem(id: String): Flow<WardrobeItemEntity?> = repository.observeById(id)

    fun addItem(
        name: String,
        category: String,
        sizeLabel: String,
        colorHex: String,
        colorName: String,
        seasons: List<String>,
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
                photoPath = path,
                wornCount = 0,
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
                    val json = WardrobeBackupCodec.toJson(items)
                    val out = getApplication<Application>().contentResolver.openOutputStream(uri)
                        ?: error("Could not open file for writing")
                    out.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                }.exceptionOrNull()?.message
            }
            onDone(err)
        }
    }

    fun importBackup(uri: Uri, onDone: (errorMessage: String?) -> Unit) {
        viewModelScope.launch {
            val err = withContext(Dispatchers.IO) {
                runCatching {
                    val text = getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
                        input.readBytes().toString(Charsets.UTF_8)
                    } ?: error("Could not read file")
                    val items = WardrobeBackupCodec.fromJson(getApplication(), text).getOrThrow()
                    repository.replaceAllWardrobe(items)
                }.exceptionOrNull()?.message
            }
            onDone(err)
        }
    }

    companion object {
        fun factory(app: DressedApplication): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass == WardrobeViewModel::class.java)
                    return WardrobeViewModel(app, app.wardrobeRepository) as T
                }
            }
    }
}
