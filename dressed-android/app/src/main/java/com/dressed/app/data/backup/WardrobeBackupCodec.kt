package com.dressed.app.data.backup

import android.content.Context
import android.util.Base64
import com.dressed.app.data.local.OutfitEntity
import com.dressed.app.data.local.WardrobeItemEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.text.Charsets
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.io.PushbackInputStream
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

private val jsonFormat = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/** Compact JSON for v3 metadata inside zip (no pretty print, smaller). */
private val jsonFormatCompact = Json {
    prettyPrint = false
    ignoreUnknownKeys = true
    encodeDefaults = false
}

private const val ZIP_METADATA = "metadata.json"
private const val PHOTOS_PREFIX = "photos/"
private const val METADATA_MAX_BYTES = 16 * 1024 * 1024

@Serializable
internal data class WardrobeBackupFileDto(
    val version: Int = 2,
    val exportedAtEpochMs: Long,
    val items: List<WardrobeBackupItemDto>,
    val outfits: List<WardrobeBackupOutfitDto>? = null,
)

@Serializable
internal data class WardrobeBackupOutfitDto(
    val id: String,
    val name: String,
    val itemIds: List<String> = emptyList(),
    val wornCount: Int = 0,
    val createdAtEpochMs: Long,
)

@Serializable
internal data class WardrobeBackupItemDto(
    val id: String,
    val name: String,
    val category: String,
    val sizeLabel: String = "",
    val colorHex: String,
    val colorName: String,
    val seasons: List<String> = emptyList(),
    val occasions: List<String> = emptyList(),
    val wornCount: Int = 0,
    val lastWornAtEpochMs: Long? = null,
    val addedAtEpochMs: Long,
    /** v1–v2 JSON backups only. */
    val photoBase64: String? = null,
    /** v3 zip metadata: path inside archive, e.g. `photos/{id}.jpg`. */
    val photoEntry: String? = null,
)

object WardrobeBackupCodec {

    /** Legacy v2 JSON export (still used only for tests / tooling); app UI exports v3 zip via [writeZipArchive]. */
    fun toJson(items: List<WardrobeItemEntity>, outfits: List<OutfitEntity>): String {
        val dtos = items.map { it.toDtoLegacy() }
        val outfitDtos = outfits.map { it.toOutfitDto() }
        val file = WardrobeBackupFileDto(
            version = 2,
            exportedAtEpochMs = System.currentTimeMillis(),
            items = dtos,
            outfits = outfitDtos,
        )
        return jsonFormat.encodeToString(file)
    }

    /**
     * v3 cross-platform backup: metadata.json plus JPEG files under the photos/ directory, streamed into [outputStream].
     */
    fun writeZipArchive(
        context: Context,
        items: List<WardrobeItemEntity>,
        outfits: List<OutfitEntity>,
        outputStream: OutputStream,
    ) {
        val itemDtos = items.map { it.toDtoV3() }
        val outfitDtos = outfits.map { it.toOutfitDto() }
        val file = WardrobeBackupFileDto(
            version = 3,
            exportedAtEpochMs = System.currentTimeMillis(),
            items = itemDtos,
            outfits = outfitDtos,
        )
        val metaBytes = jsonFormatCompact.encodeToString(file).toByteArray(Charsets.UTF_8)

        ZipOutputStream(BufferedOutputStream(outputStream)).use { zos ->
            zos.putNextEntry(ZipEntry(ZIP_METADATA))
            zos.write(metaBytes)
            zos.closeEntry()

            val buffer = ByteArray(32 * 1024)
            for (item in items) {
                val rel = itemDtos.firstOrNull { it.id == item.id }?.photoEntry ?: continue
                val path = item.photoPath?.trim()?.takeIf { it.isNotEmpty() } ?: continue
                val f = File(path)
                if (!f.isFile || !f.canRead()) continue
                zos.putNextEntry(ZipEntry(rel))
                f.inputStream().use { input ->
                    while (true) {
                        val n = input.read(buffer)
                        if (n <= 0) break
                        zos.write(buffer, 0, n)
                    }
                }
                zos.closeEntry()
            }
        }
    }

    /**
     * Detect zip (PK) vs legacy JSON and return decoded entities.
     * Uses buffered stream copy for photo entries (no full-archive read).
     */
    fun readBackup(context: Context, inputStream: InputStream): Result<Pair<List<WardrobeItemEntity>, List<OutfitEntity>>> =
        runCatching {
            val pb = PushbackInputStream(BufferedInputStream(inputStream), 4)
            val header = ByteArray(2)
            val nRead = pb.read(header)
            if (nRead != 2) {
                throw IllegalArgumentException("Backup file is too short")
            }
            pb.unread(header, 0, nRead)
            if (header[0] == 0x50.toByte() && header[1] == 0x4B.toByte()) {
                importZip(context, pb)
            } else {
                val text = pb.readBytes().toString(Charsets.UTF_8)
                fromJson(context, text).getOrThrow()
            }
        }

    fun fromJson(
        context: Context,
        json: String,
    ): Result<Pair<List<WardrobeItemEntity>, List<OutfitEntity>>> =
        runCatching {
            val file = jsonFormat.decodeFromString<WardrobeBackupFileDto>(json)
            require(file.version in 1..2) { "Unsupported JSON backup version ${file.version}. Use zip for v3." }
            val items = file.items.map { it.toEntityLegacy(context) }
            val outfits = (file.outfits ?: emptyList()).map { it.toEntity() }
            items to outfits
        }

    private fun importZip(context: Context, inputStream: InputStream): Pair<List<WardrobeItemEntity>, List<OutfitEntity>> {
        val photosDir = File(context.filesDir, "photos").apply { mkdirs() }
        val extractedPaths = mutableMapOf<String, String>()
        var metadataJson: String? = null

        ZipInputStream(inputStream).use { zis ->
            val buffer = ByteArray(32 * 1024)
            while (true) {
                val entry = zis.nextEntry ?: break
                try {
                    val name = normalizeZipPath(entry.name)
                    when {
                        name.equals(ZIP_METADATA, ignoreCase = true) -> {
                            val bytes = readEntryWithCap(zis, METADATA_MAX_BYTES)
                            metadataJson = bytes.toString(Charsets.UTF_8)
                        }
                        name.startsWith(PHOTOS_PREFIX, ignoreCase = true) &&
                            name.endsWith(".jpg", ignoreCase = true) && !entry.isDirectory -> {
                            val dest = File(photosDir, "${UUID.randomUUID()}.jpg")
                            dest.outputStream().use { out ->
                                while (true) {
                                    val n = zis.read(buffer)
                                    if (n <= 0) break
                                    out.write(buffer, 0, n)
                                }
                            }
                            extractedPaths[name.lowercase(Locale.ROOT)] = dest.absolutePath
                        }
                        else -> {
                            while (zis.read(buffer) > 0) {
                                // drain skipped entry
                            }
                        }
                    }
                } finally {
                    zis.closeEntry()
                }
            }
        }

        val raw = metadataJson ?: error("Invalid backup: missing $ZIP_METADATA")
        val file = jsonFormat.decodeFromString<WardrobeBackupFileDto>(raw)
        require(file.version == 3) { "Unsupported zip metadata version ${file.version}" }
        val items = file.items.map { it.toEntityV3(context, extractedPaths) }
        val outfits = (file.outfits ?: emptyList()).map { it.toEntity() }
        return items to outfits
    }

    private fun readEntryWithCap(zis: ZipInputStream, max: Int): ByteArray {
        val out = ByteArrayOutputStreamWithCap(max)
        val buf = ByteArray(8192)
        while (true) {
            val n = zis.read(buf)
            if (n <= 0) break
            out.write(buf, n)
        }
        return out.toByteArray()
    }

    private fun normalizeZipPath(raw: String): String {
        val t = raw.trim().removePrefix("/").replace('\\', '/')
        if (t.contains("..")) error("Invalid zip entry: $raw")
        return t
    }

    private fun WardrobeItemEntity.toDtoLegacy(): WardrobeBackupItemDto =
        WardrobeBackupItemDto(
            id = id,
            name = name,
            category = category,
            sizeLabel = sizeLabel,
            colorHex = colorHex,
            colorName = colorName,
            seasons = seasons,
            occasions = occasions,
            wornCount = wornCount,
            lastWornAtEpochMs = lastWornAtEpochMs,
            addedAtEpochMs = addedAtEpochMs,
            photoBase64 = encodePhotoBase64(photoPath),
            photoEntry = null,
        )

    private fun WardrobeItemEntity.toDtoV3(): WardrobeBackupItemDto {
        val path = photoPath?.trim()?.takeIf { it.isNotEmpty() }
        val f = path?.let { File(it) }
        val hasPhoto = f != null && f.isFile && f.canRead()
        val entry = if (hasPhoto) "$PHOTOS_PREFIX${safePhotoFileStem(id)}.jpg" else null
        return WardrobeBackupItemDto(
            id = id,
            name = name,
            category = category,
            sizeLabel = sizeLabel,
            colorHex = colorHex,
            colorName = colorName,
            seasons = seasons,
            occasions = occasions,
            wornCount = wornCount,
            lastWornAtEpochMs = lastWornAtEpochMs,
            addedAtEpochMs = addedAtEpochMs,
            photoBase64 = null,
            photoEntry = entry,
        )
    }

    private fun OutfitEntity.toOutfitDto(): WardrobeBackupOutfitDto =
        WardrobeBackupOutfitDto(
            id = id,
            name = name,
            itemIds = itemIds,
            wornCount = wornCount,
            createdAtEpochMs = createdAtEpochMs,
        )

    private fun encodePhotoBase64(path: String?): String? {
        if (path.isNullOrBlank()) return null
        val f = File(path)
        if (!f.exists() || !f.isFile) return null
        return Base64.encodeToString(f.readBytes(), Base64.NO_WRAP)
    }

    private fun WardrobeBackupItemDto.toEntityLegacy(context: Context): WardrobeItemEntity {
        val path = decodePhotoBase64(context, photoBase64)
        return toEntityWithPhoto(path)
    }

    private fun WardrobeBackupItemDto.toEntityV3(
        context: Context,
        extractedPaths: Map<String, String>,
    ): WardrobeItemEntity {
        val path = when {
            !photoEntry.isNullOrBlank() -> extractedPaths[photoEntry.trim().lowercase(Locale.ROOT)]
            !photoBase64.isNullOrBlank() -> decodePhotoBase64(context, photoBase64)
            else -> null
        }
        return toEntityWithPhoto(path)
    }

    private fun WardrobeBackupItemDto.toEntityWithPhoto(photoPath: String?): WardrobeItemEntity =
        WardrobeItemEntity(
            id = id,
            name = name,
            category = category,
            sizeLabel = sizeLabel,
            colorHex = colorHex,
            colorName = colorName,
            seasons = seasons,
            occasions = occasions,
            photoPath = photoPath,
            wornCount = wornCount,
            lastWornAtEpochMs = lastWornAtEpochMs,
            addedAtEpochMs = addedAtEpochMs,
        )

    private fun WardrobeBackupOutfitDto.toEntity(): OutfitEntity =
        OutfitEntity(
            id = id,
            name = name,
            itemIds = itemIds,
            wornCount = wornCount,
            createdAtEpochMs = createdAtEpochMs,
        )

    private fun decodePhotoBase64(context: Context, b64: String?): String? {
        if (b64.isNullOrBlank()) return null
        return runCatching {
            val bytes = Base64.decode(b64, Base64.NO_WRAP)
            val dir = File(context.filesDir, "photos")
            if (!dir.exists()) dir.mkdirs()
            val dest = File(dir, "${UUID.randomUUID()}.jpg")
            dest.writeBytes(bytes)
            dest.absolutePath
        }.getOrNull()
    }

    private fun safePhotoFileStem(id: String): String {
        val s = id.replace(Regex("[^a-zA-Z0-9._-]"), "_").trim('_')
        return s.ifEmpty { "item" }.take(120)
    }
}

/** Minimal growable buffer with hard cap for metadata.json. */
private class ByteArrayOutputStreamWithCap(private val max: Int) {
    private var buf = ByteArray(256)
    private var count = 0

    fun write(b: ByteArray, len: Int) {
        val newCount = count + len
        if (newCount > max) error("metadata.json exceeds $max bytes")
        if (newCount > buf.size) {
            var newSize = buf.size * 2
            while (newSize < newCount) newSize *= 2
            buf = buf.copyOf(newSize)
        }
        System.arraycopy(b, 0, buf, count, len)
        count = newCount
    }

    fun toByteArray(): ByteArray = buf.copyOf(count)
}
