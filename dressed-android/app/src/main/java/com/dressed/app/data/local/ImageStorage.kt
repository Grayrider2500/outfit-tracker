package com.dressed.app.data.local

import android.util.Log
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
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
            val oriented = applyExifOrientation(context, uri, decoded)
            if (oriented !== decoded) {
                decoded.recycle()
            }
            bitmap = scaleToMaxLongEdge(oriented, MAX_LONG_EDGE_PX)
            if (bitmap !== oriented) {
                oriented.recycle()
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
        } catch (e: Throwable) {
            Log.e("ImageStorage", "copyFromUri failed", e)
            null
        } finally {
            bitmap?.recycle()
        }
    }

    /**
     * Same processing as [copyFromUri] but reads from a local [File] (e.g. camera output), avoiding ContentResolver.
     */
    fun copyFromFile(context: Context, sourceFile: File): String? {
        var bitmap: Bitmap? = null
        return try {
            val decoded = BitmapFactory.decodeFile(sourceFile.absolutePath) ?: return null
            val oriented = applyExifOrientationFromFile(sourceFile, decoded)
            if (oriented !== decoded) decoded.recycle()
            bitmap = scaleToMaxLongEdge(oriented, MAX_LONG_EDGE_PX)
            if (bitmap !== oriented) oriented.recycle()
            val toWrite = bitmap ?: return null
            val dest = File(photosDir(context), "${UUID.randomUUID()}.jpg")
            val ok = FileOutputStream(dest).use { out ->
                toWrite.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }
            if (!ok) {
                dest.takeIf { it.exists() }?.delete()
                null
            } else {
                dest.absolutePath
            }
        } catch (e: Throwable) {
            Log.e("ImageStorage", "copyFromFile failed", e)
            null
        } finally {
            bitmap?.recycle()
        }
    }

    /**
     * Decodes [bytes] (e.g. gallery pick read on the main thread), applies EXIF orientation, scales, writes JPEG.
     * Does not use ContentResolver.
     */
    fun copyFromBytes(context: Context, bytes: ByteArray): String? {
        var bitmap: Bitmap? = null
        return try {
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOptions)
            val w = boundsOptions.outWidth
            val h = boundsOptions.outHeight
            if (w <= 0 || h <= 0) {
                Log.e("ImageStorage", "copyFromBytes: invalid image dimensions $w x $h")
                return null
            }
            var inSampleSize = 1
            val longEdge = max(w, h)
            while (longEdge / inSampleSize > MAX_LONG_EDGE_PX) inSampleSize *= 2

            val decodeOptions = BitmapFactory.Options().apply { this.inSampleSize = inSampleSize }
            val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
                ?: run {
                    Log.e("ImageStorage", "copyFromBytes: decode returned null")
                    return null
                }

            val oriented = applyExifOrientationFromStream(ByteArrayInputStream(bytes), decoded)
            if (oriented !== decoded) decoded.recycle()
            bitmap = scaleToMaxLongEdge(oriented, MAX_LONG_EDGE_PX)
            if (bitmap !== oriented) oriented.recycle()

            val dest = File(photosDir(context), "${UUID.randomUUID()}.jpg")
            val toWrite = bitmap ?: return null
            val ok = FileOutputStream(dest).use { out ->
                toWrite.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }
            if (!ok) {
                dest.takeIf { it.exists() }?.delete()
                null
            } else {
                dest.absolutePath
            }
        } catch (e: Throwable) {
            Log.e("ImageStorage", "copyFromBytes failed", e)
            null
        } finally {
            bitmap?.recycle()
        }
    }

    private fun applyExifOrientationFromStream(stream: InputStream, bitmap: Bitmap): Bitmap {
        val orientation = try {
            ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED,
            )
        } catch (_: Throwable) {
            ExifInterface.ORIENTATION_UNDEFINED
        }
        if (orientation == ExifInterface.ORIENTATION_UNDEFINED ||
            orientation == ExifInterface.ORIENTATION_NORMAL) return bitmap
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f, bitmap.width / 2f, bitmap.height / 2f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f, bitmap.height / 2f, bitmap.width / 2f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f, bitmap.height / 2f, bitmap.width / 2f)
            }
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun applyExifOrientationFromFile(file: File, bitmap: Bitmap): Bitmap {
        val orientation = try {
            ExifInterface(file.absolutePath).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED,
            )
        } catch (_: Throwable) {
            ExifInterface.ORIENTATION_UNDEFINED
        }
        if (orientation == ExifInterface.ORIENTATION_UNDEFINED ||
            orientation == ExifInterface.ORIENTATION_NORMAL) {
            return bitmap
        }
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(
                -1f,
                1f,
                bitmap.width / 2f,
                bitmap.height / 2f,
            )
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(
                1f,
                -1f,
                bitmap.width / 2f,
                bitmap.height / 2f,
            )
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(
                    -1f,
                    1f,
                    bitmap.height / 2f,
                    bitmap.width / 2f,
                )
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(
                    -1f,
                    1f,
                    bitmap.height / 2f,
                    bitmap.width / 2f,
                )
            }
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Applies JPEG EXIF orientation so pixel data matches intended display (similar to iOS normalizedUpOrientation).
     * Returns [bitmap] when no transform is needed; otherwise a new bitmap (caller recycles [bitmap] if distinct).
     */
    private fun applyExifOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation =
            context.contentResolver.openInputStream(uri)?.use { input ->
                ExifInterface(input).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED,
                )
            } ?: ExifInterface.ORIENTATION_UNDEFINED

        if (orientation == ExifInterface.ORIENTATION_UNDEFINED ||
            orientation == ExifInterface.ORIENTATION_NORMAL) {
            return bitmap
        }

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(
                -1f,
                1f,
                bitmap.width / 2f,
                bitmap.height / 2f,
            )
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(
                1f,
                -1f,
                bitmap.width / 2f,
                bitmap.height / 2f,
            )
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(
                    -1f,
                    1f,
                    bitmap.height / 2f,
                    bitmap.width / 2f,
                )
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(
                    -1f,
                    1f,
                    bitmap.height / 2f,
                    bitmap.width / 2f,
                )
            }
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
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
