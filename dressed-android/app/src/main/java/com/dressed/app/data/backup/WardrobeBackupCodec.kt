package com.dressed.app.data.backup

import android.content.Context
import android.util.Base64
import com.dressed.app.data.local.OutfitEntity
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
    val wornCount: Int = 0,
    val addedAtEpochMs: Long,
    val photoBase64: String? = null,
)

object WardrobeBackupCodec {

    fun toJson(items: List<WardrobeItemEntity>, outfits: List<OutfitEntity>): String {
        val dtos = items.map { it.toDto() }
        val outfitDtos = outfits.map {
            WardrobeBackupOutfitDto(
                id = it.id,
                name = it.name,
                itemIds = it.itemIds,
                wornCount = it.wornCount,
                createdAtEpochMs = it.createdAtEpochMs,
            )
        }
        val file = WardrobeBackupFileDto(
            version = 2,
            exportedAtEpochMs = System.currentTimeMillis(),
            items = dtos,
            outfits = outfitDtos,
        )
        return jsonFormat.encodeToString(file)
    }

    fun fromJson(
        context: Context,
        json: String,
    ): Result<Pair<List<WardrobeItemEntity>, List<OutfitEntity>>> =
        runCatching {
            val file = jsonFormat.decodeFromString<WardrobeBackupFileDto>(json)
            require(file.version in 1..2) { "Unsupported backup version ${file.version}" }
            val items = file.items.map { it.toEntity(context) }
            val outfits = (file.outfits ?: emptyList()).map {
                OutfitEntity(
                    id = it.id,
                    name = it.name,
                    itemIds = it.itemIds,
                    wornCount = it.wornCount,
                    createdAtEpochMs = it.createdAtEpochMs,
                )
            }
            items to outfits
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
