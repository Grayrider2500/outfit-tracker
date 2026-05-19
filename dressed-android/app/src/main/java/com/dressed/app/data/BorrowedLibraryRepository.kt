package com.dressed.app.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.dressed.app.data.backup.LibraryShareCodec
import com.dressed.app.data.local.BorrowedItemEntity
import com.dressed.app.data.local.BorrowedLibraryEntity
import com.dressed.app.data.local.BorrowedLibraryListRow
import com.dressed.app.data.local.DressedDatabase
import com.dressed.app.data.local.BorrowedItemDao
import com.dressed.app.data.local.BorrowedLibraryDao
import kotlinx.coroutines.flow.Flow
import java.io.InputStream
import java.util.UUID

data class LibraryImportOutcome(
    val libraryId: String,
    val sharerName: String,
)

class BorrowedLibraryRepository(
    private val database: DressedDatabase,
    private val libraryDao: BorrowedLibraryDao,
    private val itemDao: BorrowedItemDao,
) {

    fun observeLibraries(): Flow<List<BorrowedLibraryEntity>> = libraryDao.observeAll()

    fun observeLibraryListRows(): Flow<List<BorrowedLibraryListRow>> = libraryDao.observeListRows()

    fun observeItems(libraryId: String): Flow<List<BorrowedItemEntity>> =
        itemDao.observeForLibrary(libraryId)

    /** Deletes the library row; Room CASCADE removes [BorrowedItemEntity] rows for this library. */
    suspend fun removeLibrary(libraryId: String) =
        database.withTransaction {
            libraryDao.deleteById(libraryId)
        }

    suspend fun importFromUri(context: Context, uri: Uri): Result<LibraryImportOutcome> =
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                importFromZipStream(context, input).getOrThrow()
            } ?: error("Could not read library file")
        }

    suspend fun importFromZipStream(context: Context, inputStream: InputStream): Result<LibraryImportOutcome> =
        runCatching {
            val parsed = LibraryShareCodec.readLibraryZip(context, inputStream).getOrThrow()
            val sharerName = parsed.sharerName
            lateinit var libId: String
            database.withTransaction {
                val existing = libraryDao.getBySharerName(sharerName)
                if (existing != null) {
                    libraryDao.delete(existing)
                }
                libId = UUID.randomUUID().toString()
                libraryDao.insert(
                    BorrowedLibraryEntity(
                        id = libId,
                        sharerName = sharerName,
                        importedAtEpochMs = System.currentTimeMillis(),
                    ),
                )
                val rows = parsed.items.map { dto ->
                    val photo = LibraryShareCodec.resolvedPhotoPath(
                        context,
                        dto,
                        parsed.extractedPaths,
                    )
                    BorrowedItemEntity(
                        id = "$libId|${dto.id}",
                        libraryId = libId,
                        name = dto.name,
                        category = dto.category,
                        sizeLabel = dto.sizeLabel,
                        colorHex = dto.colorHex,
                        colorName = dto.colorName,
                        seasons = dto.seasons,
                        occasions = dto.occasions,
                        photoPath = photo,
                        wornCount = dto.wornCount,
                        lastWornAtEpochMs = dto.lastWornAtEpochMs,
                        addedAtEpochMs = dto.addedAtEpochMs,
                    )
                }
                if (rows.isNotEmpty()) itemDao.insertAll(rows)
            }
            LibraryImportOutcome(libraryId = libId, sharerName = sharerName)
        }
}
