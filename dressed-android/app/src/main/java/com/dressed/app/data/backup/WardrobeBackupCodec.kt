package com.dressed.app.data.backup

import android.content.Context
import android.util.Base64
import com.dressed.app.data.local.WardrobeItemEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

private val jsonFormat = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    encodeDefaults = true
}

@Serializable
internal data class WardrobeBackupFileDto(
    val version: Int = 1,
    val exportedAtEpochMs: Long,
    val items: List<WardrobeBackupItemDto>,
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
    val wornCount: Int = 0,
    val addedAtEpochMs: Long,
    val photoBase64: String? = null,
)

object WardrobeBackupCodec {

    fun toJson(items: List<WardrobeItemEntity>): String {
        val dtos = items.map { it.toDto() }
        val file = WardrobeBackupFileDto(
            version = 1,
            exportedAtEpochMs = System.currentTimeMillis(),
            items = dtos,
        )
        return jsonFormat.encodeToString(file)
    }

    fun fromJson(context: Context, json: String): Result<List<WardrobeItemEntity>> =
        runCatching {
            val file = jsonFormat.decodeFromString<WardrobeBackupFileDto>(json)
            require(file.version == 1) { "Unsupported backup version ${file.version}" }
            file.items.map { it.toEntity(context) }
        }

    private fun WardrobeItemEntity.toDto(): WardrobeBackupItemDto =
        WardrobeBackupItemDto(
            id = id,
            name = name,
            category = category,
            sizeLabel = sizeLabel,
            colorHex = colorHex,
            colorName = colorName,
            seasons = seasons,
            wornCount = wornCount,
            addedAtEpochMs = addedAtEpochMs,
            photoBase64 = encodePhoto(photoPath),
        )

    private fun encodePhoto(path: String?): String? {
        if (path.isNullOrBlank()) return null
        val f = File(path)
        if (!f.exists() || !f.isFile) return null
        return Base64.encodeToString(f.readBytes(), Base64.NO_WRAP)
    }

    private fun WardrobeBackupItemDto.toEntity(context: Context): WardrobeItemEntity {
        val path = decodePhoto(context, photoBase64)
        return WardrobeItemEntity(
            id = id,
            name = name,
            category = category,
            sizeLabel = sizeLabel,
            colorHex = colorHex,
            colorName = colorName,
            seasons = seasons,
            photoPath = path,
            wornCount = wornCount,
            addedAtEpochMs = addedAtEpochMs,
        )
    }

    private fun decodePhoto(context: Context, b64: String?): String? {
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
}
