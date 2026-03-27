package com.dressed.app.ui.outfits

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dressed.app.DressedApplication
import com.dressed.app.data.OutfitRepository
import com.dressed.app.data.local.OutfitEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.UUID

class OutfitsViewModel(
    application: Application,
    private val repository: OutfitRepository,
) : AndroidViewModel(application) {

    val outfits: Flow<List<OutfitEntity>> = repository.observeAll()

    fun addOutfit(
        name: String,
        itemIds: List<String>,
        onInserted: () -> Unit = {},
    ) {
        viewModelScope.launch {
            val entity = OutfitEntity(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                itemIds = itemIds,
                wornCount = 0,
                createdAtEpochMs = System.currentTimeMillis(),
            )
            repository.insert(entity)
            onInserted()
        }
    }

    fun markWorn(id: String) {
        viewModelScope.launch { repository.incrementWearCount(id) }
    }

    fun deleteOutfit(id: String) {
        viewModelScope.launch { repository.deleteById(id) }
    }

    companion object {
        fun factory(app: DressedApplication): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass == OutfitsViewModel::class.java)
                    return OutfitsViewModel(app, app.outfitRepository) as T
                }
            }
    }
}
