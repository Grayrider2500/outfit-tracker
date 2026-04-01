package com.dressed.app.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

object ImageStorage {

    private const val MAX_LONG_EDGE_PX = 1600
    private const val JPEG_QUALITY = 87

    private fun photosDir(context: Context): File {
        val dir = File(context.filesDir, "photos")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Reads the image at [uri], scales so the long edge is at most [MAX_LONG_EDGE_PX] (aspect preserved),
     * and writes a new JPEG (quality [JPEG_QUALITY]). Does not modify the source file / content URI.
     */
    fun copyFromUri(context: Context, uri: Uri): String? {
        var bitmap: Bitmap? = null
        return try {
            val decoded = decodeSampledBitmap(context, uri, MAX_LONG_EDGE_PX) ?: return null
            bitmap = scaleToMaxLongEdge(decoded, MAX_LONG_EDGE_PX)
            if (bitmap !== decoded) {
                decoded.recycle()
            }
            val bmp = bitmap
            val dest = File(photosDir(context), "${UUID.randomUUID()}.jpg")
            val ok = FileOutputStream(dest).use { out ->
                bmp.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }
            if (!ok) {
                dest.takeIf { it.exists() }?.delete()
                null
            } else {
                dest.absolutePath
            }
        } catch (_: Throwable) {
            null
        } finally {
            bitmap?.recycle()
        }
    }

    private fun decodeSampledBitmap(context: Context, uri: Uri, maxLongEdge: Int): Bitmap? {
        val resolver = context.contentResolver
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, boundsOptions)
        } ?: return null
        val w = boundsOptions.outWidth
        val h = boundsOptions.outHeight
        if (w <= 0 || h <= 0) return null
        var inSampleSize = 1
        val longEdge = max(w, h)
        while (longEdge / inSampleSize > maxLongEdge) {
            inSampleSize *= 2
        }
        val decodeOptions = BitmapFactory.Options().apply { this.inSampleSize = inSampleSize }
        return resolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, decodeOptions)
        }
    }

    private fun scaleToMaxLongEdge(bitmap: Bitmap, maxLongEdge: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val longEdge = max(w, h)
        if (longEdge <= maxLongEdge) return bitmap
        val scale = maxLongEdge.toFloat() / longEdge
        val nw = max(1, (w * scale).roundToInt())
        val nh = max(1, (h * scale).roundToInt())
        return Bitmap.createScaledBitmap(bitmap, nw, nh, true)
    }

    fun deleteIfExists(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).takeIf { it.exists() }?.delete() }
    }
}
