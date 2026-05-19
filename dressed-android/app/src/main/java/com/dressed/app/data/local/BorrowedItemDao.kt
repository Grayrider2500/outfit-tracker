package com.dressed.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BorrowedItemDao {

    @Query("SELECT * FROM borrowed_items WHERE libraryId = :libraryId ORDER BY addedAtEpochMs DESC")
    fun observeForLibrary(libraryId: String): Flow<List<BorrowedItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: BorrowedItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<BorrowedItemEntity>)
}
