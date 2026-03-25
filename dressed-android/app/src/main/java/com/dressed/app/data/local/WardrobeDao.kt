package com.dressed.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WardrobeDao {

    @Query("SELECT * FROM wardrobe_items ORDER BY addedAtEpochMs DESC")
    fun observeAll(): Flow<List<WardrobeItemEntity>>

    @Query("SELECT * FROM wardrobe_items WHERE id = :id")
    fun observeById(id: String): Flow<WardrobeItemEntity?>

    @Query("SELECT * FROM wardrobe_items WHERE id = :id")
    suspend fun getById(id: String): WardrobeItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WardrobeItemEntity)

    @Query("UPDATE wardrobe_items SET wornCount = wornCount + 1 WHERE id = :id")
    suspend fun incrementWearCount(id: String)

    @Query("DELETE FROM wardrobe_items WHERE id = :id")
    suspend fun deleteById(id: String)
}
