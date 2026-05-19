package com.dressed.app.data.backup

import android.content.Context
import com.dressed.app.data.local.WardrobeItemEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.text.Charsets

private val jsonCompact = Json {
    prettyPrint = false
    ignoreUnknownKeys = true
    encodeDefaults = false
}

private const val ZIP_METADATA = "metadata.json"
private const val PHOTOS_PREFIX = "photos/"
private const val METADATA_MAX_BYTES = 16 * 1024 * 1024

@Serializable
internal data class LibraryManifestDto(
    val type: String,
    val version: Int,
    val sharerName: String,
    val exportedAtEpochMs: Long,
    val items: List<WardrobeBackupItemDto>,
)

internal object LibraryShareCodec {

    fun writeLibraryZip(
        items: List<WardrobeItemEntity>,
        sharerName: String,
        outputStream: OutputStream,
    ) {
        val itemDtos = items.map { it.toLibraryItemDtoV3() }
        val manifest = LibraryManifestDto(
            type = "library",
            version = 1,
            sharerName = sharerName.trim(),
            exportedAtEpochMs = System.currentTimeMillis(),
            items = itemDtos,
        )
        val metaBytes = jsonCompact.encodeToString(manifest).toByteArray(Charsets.UTF_8)

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

    internal data class ParsedLibraryZip(
        val sharerName: String,
        val items: List<WardrobeBackupItemDto>,
        val extractedPaths: Map<String, String>,
    )

    /** Reads a `.dressed-library` zip (metadata + streamed photo files). */
    internal fun readLibraryZip(
        context: Context,
        inputStream: InputStream,
    ): Result<ParsedLibraryZip> =
        runCatching {
            val photosDir = File(context.filesDir, "photos").apply { mkdirs() }
            val extractedPaths = mutableMapOf<String, String>()
            var metadataJson: String? = null

            ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
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
                                while (zis.read(buffer) > 0) { }
                            }
                        }
                    } finally {
                        zis.closeEntry()
                    }
                }
            }

            val raw = metadataJson ?: error("Invalid library: missing $ZIP_METADATA")
            val file = jsonCompact.decodeFromString<LibraryManifestDto>(raw)
            require(file.type == "library") { "Not a library file (type=${file.type})" }
            require(file.version == 1) { "Unsupported library version ${file.version}" }
            val sharer = file.sharerName.trim().ifEmpty { error("library missing sharerName") }
            ParsedLibraryZip(
                sharerName = sharer,
                items = file.items,
                extractedPaths = extractedPaths.toMap(),
            )
        }

    internal fun resolvedPhotoPath(
        context: Context,
        dto: WardrobeBackupItemDto,
        extractedPaths: Map<String, String>,
    ): String? =
        when {
            !dto.photoEntry.isNullOrBlank() ->
                extractedPaths[dto.photoEntry.trim().lowercase(Locale.ROOT)]
            !dto.photoBase64.isNullOrBlank() -> decodeLibraryPhotoB64(context, dto.photoBase64)
            else -> null
        }

    private fun decodeLibraryPhotoB64(context: Context, b64: String?): String? {
        if (b64.isNullOrBlank()) return null
        return runCatching {
            val bytes = android.util.Base64.decode(b64, android.util.Base64.NO_WRAP)
            val dir = File(context.filesDir, "photos")
            if (!dir.exists()) dir.mkdirs()
            val dest = File(dir, "${UUID.randomUUID()}.jpg")
            dest.writeBytes(bytes)
            dest.absolutePath
        }.getOrNull()
    }

    private fun normalizeZipPath(raw: String): String {
        val t = raw.trim().removePrefix("/").replace('\\', '/')
        if (t.contains("..")) error("Invalid zip entry: $raw")
        return t
    }

    private fun readEntryWithCap(zis: ZipInputStream, max: Int): ByteArray {
        val out = LibraryMetadataCapBuffer(max)
        val buf = ByteArray(8192)
        while (true) {
            val n = zis.read(buf)
            if (n <= 0) break
            out.write(buf, n)
        }
        return out.toByteArray()
    }

    private fun WardrobeItemEntity.toLibraryItemDtoV3(): WardrobeBackupItemDto {
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

    private fun safePhotoFileStem(id: String): String {
        val s = id.replace(Regex("[^a-zA-Z0-9._-]"), "_").trim('_')
        return s.ifEmpty { "item" }.take(120)
    }
}

private class LibraryMetadataCapBuffer(private val max: Int) {
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
