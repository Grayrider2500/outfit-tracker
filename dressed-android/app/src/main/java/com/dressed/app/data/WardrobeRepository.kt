package com.dressed.app.data

import com.dressed.app.data.local.ImageStorage
import com.dressed.app.data.local.WardrobeDao
import com.dressed.app.data.local.WardrobeItemEntity
import kotlinx.coroutines.flow.Flow

class WardrobeRepository(
    private val dao: WardrobeDao,
) {

    fun observeAll(): Flow<List<WardrobeItemEntity>> = dao.observeAll()

    fun observeById(id: String): Flow<WardrobeItemEntity?> = dao.observeById(id)

    suspend fun insert(item: WardrobeItemEntity) = dao.insert(item)

    suspend fun incrementWearCount(id: String) = dao.incrementWearCount(id)

    suspend fun deleteById(id: String) = dao.deleteById(id)

    suspend fun deleteItemAndPhoto(id: String) {
        val item = dao.getById(id)
        ImageStorage.deleteIfExists(item?.photoPath)
        dao.deleteById(id)
    }
}
