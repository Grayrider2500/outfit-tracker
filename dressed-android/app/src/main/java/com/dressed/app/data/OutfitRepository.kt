package com.dressed.app.data

import com.dressed.app.data.local.OutfitDao
import com.dressed.app.data.local.OutfitEntity
import kotlinx.coroutines.flow.Flow

class OutfitRepository(private val dao: OutfitDao) {

    fun observeAll(): Flow<List<OutfitEntity>> = dao.observeAll()

    fun observeById(id: String): Flow<OutfitEntity?> = dao.observeById(id)

    suspend fun insert(outfit: OutfitEntity) = dao.insert(outfit)

    suspend fun incrementWearCount(id: String) = dao.incrementWearCount(id)

    suspend fun deleteById(id: String) = dao.deleteById(id)
}
