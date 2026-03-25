package com.dressed.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dressed.app.DressedApplication
import com.dressed.app.data.WardrobeRepository
import com.dressed.app.data.local.ImageStorage
import com.dressed.app.data.local.WardrobeItemEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.UUID

class WardrobeViewModel(
    application: Application,
    private val repository: WardrobeRepository,
) : AndroidViewModel(application) {

    val items: Flow<List<WardrobeItemEntity>> = repository.observeAll()

    fun observeItem(id: String): Flow<WardrobeItemEntity?> = repository.observeById(id)

    fun addItem(
        name: String,
        category: String,
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
