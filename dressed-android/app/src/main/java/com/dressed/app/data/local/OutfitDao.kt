package com.dressed.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OutfitDao {

    @Query("SELECT * FROM outfits ORDER BY createdAtEpochMs DESC")
    fun observeAll(): Flow<List<OutfitEntity>>

    @Query("SELECT * FROM outfits WHERE id = :id")
    fun observeById(id: String): Flow<OutfitEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(outfit: OutfitEntity)

    @Query("UPDATE outfits SET wornCount = wornCount + 1 WHERE id = :id")
    suspend fun incrementWearCount(id: String)

    @Query("DELETE FROM outfits WHERE id = :id")
    suspend fun deleteById(id: String): Int
}
