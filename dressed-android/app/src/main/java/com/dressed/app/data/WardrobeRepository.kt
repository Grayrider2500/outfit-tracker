package com.dressed.app.data

import androidx.room.withTransaction
import com.dressed.app.data.local.DressedDatabase
import com.dressed.app.data.local.ImageStorage
import com.dressed.app.data.local.OutfitDao
import com.dressed.app.data.local.OutfitEntity
import com.dressed.app.data.local.WardrobeDao
import com.dressed.app.data.local.WardrobeItemEntity
import kotlinx.coroutines.flow.Flow

class WardrobeRepository(
    private val database: DressedDatabase,
    private val dao: WardrobeDao,
    private val outfitDao: OutfitDao,
) {

    fun observeAll(): Flow<List<WardrobeItemEntity>> = dao.observeAll()

    fun observeById(id: String): Flow<WardrobeItemEntity?> = dao.observeById(id)

    suspend fun getAllSnapshot(): List<WardrobeItemEntity> = dao.getAllSnapshot()

    suspend fun insert(item: WardrobeItemEntity) = dao.insert(item)

    suspend fun incrementWearCount(id: String) = dao.incrementWearCount(id)

    suspend fun deleteById(id: String) = dao.deleteById(id)

    suspend fun deleteItemAndPhoto(id: String) {
        val item = dao.getById(id)
        ImageStorage.deleteIfExists(item?.photoPath)
        dao.deleteById(id)
    }

    /**
     * Replaces all wardrobe rows, all outfit rows, and associated image files.
     * Runs in a single transaction. v1 backups supply an empty outfit list.
     */
    suspend fun replaceAllWardrobe(
        items: List<WardrobeItemEntity>,
        outfits: List<OutfitEntity> = emptyList(),
    ) {
        database.withTransaction {
            val existing = dao.getAllSnapshot()
            existing.forEach { ImageStorage.deleteIfExists(it.photoPath) }
            dao.deleteAllItems()
            outfitDao.deleteAll()
            for (item in items) {
                dao.insert(item)
            }
            for (outfit in outfits) {
                outfitDao.insert(outfit)
            }
        }
    }
}
