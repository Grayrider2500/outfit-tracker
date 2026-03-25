package com.dressed.app.data.local

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID

object ImageStorage {

    private fun photosDir(context: Context): File {
        val dir = File(context.filesDir, "photos")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun copyFromUri(context: Context, uri: Uri): String? {
        return runCatching {
            val dest = File(photosDir(context), "${UUID.randomUUID()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            dest.absolutePath
        }.getOrNull()
    }

    fun deleteIfExists(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).takeIf { it.exists() }?.delete() }
    }
}
