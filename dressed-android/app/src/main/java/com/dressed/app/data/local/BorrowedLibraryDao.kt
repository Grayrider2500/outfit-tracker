package com.dressed.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class BorrowedLibraryListRow(
    val id: String,
    val sharerName: String,
    val importedAtEpochMs: Long,
    @ColumnInfo(name = "itemCount") val itemCount: Int,
)

@Dao
interface BorrowedLibraryDao {

    @Query(
        """
        SELECT l.id, l.sharerName, l.importedAtEpochMs,
        (SELECT COUNT(*) FROM borrowed_items WHERE libraryId = l.id) AS itemCount
        FROM borrowed_libraries l
        ORDER BY l.importedAtEpochMs DESC
        """,
    )
    fun observeListRows(): Flow<List<BorrowedLibraryListRow>>

    @Query("SELECT * FROM borrowed_libraries ORDER BY importedAtEpochMs DESC")
    fun observeAll(): Flow<List<BorrowedLibraryEntity>>

    @Query("SELECT * FROM borrowed_libraries WHERE sharerName = :name LIMIT 1")
    suspend fun getBySharerName(name: String): BorrowedLibraryEntity?

    @Query("SELECT * FROM borrowed_libraries WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): BorrowedLibraryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(library: BorrowedLibraryEntity)

    @Delete
    suspend fun delete(library: BorrowedLibraryEntity)

    @Query("DELETE FROM borrowed_libraries WHERE id = :id")
    suspend fun deleteById(id: String)
}
