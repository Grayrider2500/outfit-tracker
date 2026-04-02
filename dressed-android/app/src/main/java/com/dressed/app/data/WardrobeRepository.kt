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
        database.withTransaction {
            val outfits = outfitDao.getAllSnapshot().filter { outfit ->
                outfit.itemIds.any { it == id }
            }
            for (outfit in outfits) {
                val nextIds = outfit.itemIds.filterNot { it == id }
                outfitDao.insert(outfit.copy(itemIds = nextIds))
            }
            dao.deleteById(id)
        }
        ImageStorage.deleteIfExists(item?.photoPath)
    }

    /**
     * Inserts items and outfits whose ids are not already in the database (matches iOS `restoreMerge`).
     * Skips duplicates by id; does not delete existing rows or photos. Runs in a single transaction.
     */
    suspend fun mergeWardrobe(
        items: List<WardrobeItemEntity>,
        outfits: List<OutfitEntity> = emptyList(),
    ): Pair<Int, Int> {
        var newItems = 0
        var newOutfits = 0
        database.withTransaction {
            val existingItemIds = dao.getAllSnapshot().map { it.id }.toSet()
            val existingOutfitIds = outfitDao.getAllSnapshot().map { it.id }.toSet()
            for (item in items) {
                if (item.id !in existingItemIds) {
                    dao.insert(item)
                    newItems++
                }
            }
            for (outfit in outfits) {
                if (outfit.id !in existingOutfitIds) {
                    outfitDao.insert(outfit)
                    newOutfits++
                }
            }
        }
        return newItems to newOutfits
    }

    /**
     * Replaces all wardrobe rows and all outfit rows. DB work runs in a single transaction.
     * Old photo files are removed only after the transaction succeeds, and only for paths
     * not still referenced by the restored items—so a failed restore cannot wipe photos early.
     */
    suspend fun replaceAllWardrobe(
        items: List<WardrobeItemEntity>,
        outfits: List<OutfitEntity> = emptyList(),
    ) {
        val existing = dao.getAllSnapshot()
        val oldPhotoPaths = existing.mapNotNull { it.photoPath?.trim()?.takeIf { p -> p.isNotEmpty() } }.toSet()
        val newPhotoPaths = items.mapNotNull { it.photoPath?.trim()?.takeIf { p -> p.isNotEmpty() } }.toSet()
        database.withTransaction {
            dao.deleteAllItems()
            outfitDao.deleteAll()
            for (item in items) {
                dao.insert(item)
            }
            for (outfit in outfits) {
                outfitDao.insert(outfit)
            }
        }
        for (path in oldPhotoPaths - newPhotoPaths) {
            ImageStorage.deleteIfExists(path)
        }
    }
}
