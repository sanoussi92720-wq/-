package com.example.data.engine

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import com.example.model.FileItem
import com.example.model.ImageFormat
import com.example.model.ImagePreviewComparison
import com.example.model.ResizeConfig
import com.example.model.ResizeMode
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class ImageProcessorEngine(private val context: Context) {

    private val resolver: ContentResolver = context.contentResolver

    fun readImageDimensions(item: FileItem): Pair<Int, Int>? {
        return openStream(item)?.use { stream ->
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(stream, null, opts)
            if (opts.outWidth > 0 && opts.outHeight > 0) {
                val rotation = if (item.extension.lowercase() in setOf("jpg", "jpeg")) {
                    readExifRotation(item)
                } else 0
                if (rotation == 90 || rotation == 270) {
                    opts.outHeight to opts.outWidth
                } else {
                    opts.outWidth to opts.outHeight
                }
            } else null
        }
    }

    fun computeTargetDimensions(
        srcWidth: Int,
        srcHeight: Int,
        config: ResizeConfig
    ): Pair<Int, Int> {
        if (srcWidth <= 0 || srcHeight <= 0) return 1 to 1
        return when (config.mode) {
            ResizeMode.PERCENTAGE -> {
                val factor = config.percentage.coerceIn(5, 400) / 100f
                val w = (srcWidth * factor).roundToInt().coerceAtLeast(1)
                val h = (srcHeight * factor).roundToInt().coerceAtLeast(1)
                w to h
            }
            ResizeMode.MAX_DIMENSION -> {
                val maxEdge = config.maxEdgePx.coerceAtLeast(16)
                val longest = max(srcWidth, srcHeight)
                if (longest <= maxEdge) {
                    srcWidth to srcHeight
                } else {
                    val scale = maxEdge.toFloat() / longest.toFloat()
                    val w = (srcWidth * scale).roundToInt().coerceAtLeast(1)
                    val h = (srcHeight * scale).roundToInt().coerceAtLeast(1)
                    w to h
                }
            }
            ResizeMode.PRESET, ResizeMode.CUSTOM_DIMENSIONS -> {
                val reqW = config.targetWidth.coerceAtLeast(1)
                val reqH = config.targetHeight.coerceAtLeast(1)
                if (!config.maintainAspectRatio) {
                    reqW to reqH
                } else {
                    val scale = min(reqW.toFloat() / srcWidth.toFloat(), reqH.toFloat() / srcHeight.toFloat())
                    val w = (srcWidth * scale).roundToInt().coerceAtLeast(1)
                    val h = (srcHeight * scale).roundToInt().coerceAtLeast(1)
                    w to h
                }
            }
        }
    }

    fun processImageToStream(
        source: FileItem,
        outputStream: OutputStream,
        targetFormat: ImageFormat,
        quality: Int,
        resizeConfig: ResizeConfig?,
        autoOrientExif: Boolean
    ): Result<Pair<Int, Int>> = runCatching {
        val rawDims = readRawBounds(source)
            ?: error("Unsupported or unreadable image format: ${source.name}")
        val exifOrientation = if (autoOrientExif) readExifOrientationTag(source) else ExifInterface.ORIENTATION_NORMAL
        val isRotated90Or270 = exifOrientation == ExifInterface.ORIENTATION_ROTATE_90 ||
            exifOrientation == ExifInterface.ORIENTATION_ROTATE_270 ||
            exifOrientation == ExifInterface.ORIENTATION_TRANSPOSE ||
            exifOrientation == ExifInterface.ORIENTATION_TRANSVERSE

        val orientedSrcW = if (isRotated90Or270) rawDims.second else rawDims.first
        val orientedSrcH = if (isRotated90Or270) rawDims.first else rawDims.second

        val (targetW, targetH) = if (resizeConfig != null) {
            computeTargetDimensions(orientedSrcW, orientedSrcH, resizeConfig)
        } else {
            orientedSrcW to orientedSrcH
        }

        // Compute power-of-2 inSampleSize to avoid allocating massive bitmaps when downscaling
        val sampleSize = calculateInSampleSize(
            rawWidth = rawDims.first,
            rawHeight = rawDims.second,
            reqWidth = if (isRotated90Or270) targetH else targetW,
            reqHeight = if (isRotated90Or270) targetW else targetH
        )

        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        var workingBitmap = openStream(source)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOpts)
        } ?: error("Failed to decode bitmap for ${source.name}")

        try {
            if (autoOrientExif && exifOrientation != ExifInterface.ORIENTATION_NORMAL &&
                exifOrientation != ExifInterface.ORIENTATION_UNDEFINED
            ) {
                val oriented = applyExifTransform(workingBitmap, exifOrientation)
                if (oriented !== workingBitmap) {
                    workingBitmap.recycle()
                    workingBitmap = oriented
                }
            }

            if (resizeConfig != null && (workingBitmap.width != targetW || workingBitmap.height != targetH)) {
                val scaled = scaleBitmapHighQuality(workingBitmap, targetW, targetH)
                if (scaled !== workingBitmap) {
                    workingBitmap.recycle()
                    workingBitmap = scaled
                }
            }

            encodeBitmap(workingBitmap, targetFormat, quality, outputStream)
            workingBitmap.width to workingBitmap.height
        } finally {
            if (!workingBitmap.isRecycled) {
                workingBitmap.recycle()
            }
        }
    }

    fun generatePreviewComparison(
        source: FileItem,
        targetFormat: ImageFormat,
        quality: Int,
        resizeConfig: ResizeConfig?,
        autoOrientExif: Boolean
    ): ImagePreviewComparison? {
        val dims = readImageDimensions(source) ?: return null
        val (origW, origH) = dims
        val (newW, newH) = if (resizeConfig != null) {
            computeTargetDimensions(origW, origH, resizeConfig)
        } else {
            origW to origH
        }

        // Encode full target into memory stream for accurate byte-size estimation (bounded for safety)
        val byteCounter = ByteArrayOutputStream()
        val encodedOk = processImageToStream(
            source = source,
            outputStream = byteCounter,
            targetFormat = targetFormat,
            quality = quality,
            resizeConfig = resizeConfig,
            autoOrientExif = autoOrientExif
        ).isSuccess

        val estimatedBytes = if (encodedOk) byteCounter.size().toLong() else source.sizeBytes

        // Decode small display thumbnails (max 720px) for side-by-side preview UI
        val rawDims = readRawBounds(source) ?: dims
        val thumbSample = calculateInSampleSize(rawDims.first, rawDims.second, 720, 720)
        val thumbOpts = BitmapFactory.Options().apply { inSampleSize = thumbSample }
        var origThumb = openStream(source)?.use { BitmapFactory.decodeStream(it, null, thumbOpts) }
        if (origThumb != null && autoOrientExif) {
            val tag = readExifOrientationTag(source)
            val rotated = applyExifTransform(origThumb, tag)
            if (rotated !== origThumb) {
                origThumb.recycle()
                origThumb = rotated
            }
        }

        val processedThumb = if (encodedOk) {
            val encodedBytes = byteCounter.toByteArray()
            val pOpts = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(newW, newH, 720, 720)
            }
            BitmapFactory.decodeByteArray(encodedBytes, 0, encodedBytes.size, pOpts)
        } else null

        return ImagePreviewComparison(
            originalBitmap = origThumb,
            processedBitmap = processedThumb,
            originalWidth = origW,
            originalHeight = origH,
            newWidth = newW,
            newHeight = newH,
            originalSizeBytes = source.sizeBytes,
            estimatedNewSizeBytes = estimatedBytes,
            outputFormat = targetFormat
        )
    }

    private fun scaleBitmapHighQuality(bitmap: Bitmap, targetW: Int, targetH: Int): Bitmap {
        var current = bitmap
        // Step-down halving for sharper downscaling when shrinking more than 2x
        while (current.width / 2 >= targetW && current.height / 2 >= targetH) {
            val halfW = (current.width / 2).coerceAtLeast(targetW)
            val halfH = (current.height / 2).coerceAtLeast(targetH)
            val next = Bitmap.createScaledBitmap(current, halfW, halfH, true)
            if (current !== bitmap) current.recycle()
            current = next
        }
        if (current.width == targetW && current.height == targetH) {
            return current
        }
        val finalBmp = Bitmap.createScaledBitmap(current, targetW, targetH, true)
        if (current !== bitmap && current !== finalBmp) {
            current.recycle()
        }
        return finalBmp
    }

    private fun encodeBitmap(
        bitmap: Bitmap,
        format: ImageFormat,
        quality: Int,
        outputStream: OutputStream
    ) {
        val clampedQuality = quality.coerceIn(1, 100)
        when (format) {
            ImageFormat.PNG -> {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
            ImageFormat.JPEG -> {
                bitmap.compress(Bitmap.CompressFormat.JPEG, clampedQuality, outputStream)
            }
            ImageFormat.WEBP -> {
                @Suppress("DEPRECATION")
                val compressFormat = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (clampedQuality == 100) Bitmap.CompressFormat.WEBP_LOSSLESS
                    else Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    Bitmap.CompressFormat.WEBP
                }
                bitmap.compress(compressFormat, clampedQuality, outputStream)
            }
            ImageFormat.BMP -> {
                BmpEncoder.encode(bitmap, outputStream)
            }
        }
        outputStream.flush()
    }

    private fun readRawBounds(item: FileItem): Pair<Int, Int>? {
        return openStream(item)?.use { stream ->
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(stream, null, opts)
            if (opts.outWidth > 0 && opts.outHeight > 0) {
                opts.outWidth to opts.outHeight
            } else null
        }
    }

    private fun readExifOrientationTag(item: FileItem): Int {
        return runCatching {
            openStream(item)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
    }

    private fun readExifRotation(item: FileItem): Int {
        return when (readExifOrientationTag(item)) {
            ExifInterface.ORIENTATION_ROTATE_90, ExifInterface.ORIENTATION_TRANSPOSE -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270, ExifInterface.ORIENTATION_TRANSVERSE -> 270
            else -> 0
        }
    }

    private fun applyExifTransform(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun calculateInSampleSize(
        rawWidth: Int,
        rawHeight: Int,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        var inSampleSize = 1
        if (rawHeight > reqHeight * 2 || rawWidth > reqWidth * 2) {
            val halfHeight = rawHeight / 2
            val halfWidth = rawWidth / 2
            while ((halfHeight / inSampleSize) >= reqHeight * 2 &&
                (halfWidth / inSampleSize) >= reqWidth * 2
            ) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }

    private fun openStream(item: FileItem): InputStream? {
        return if (item.isSaf) {
            runCatching { resolver.openInputStream(Uri.parse(item.pathOrUri)) }.getOrNull()
        } else {
            val file = File(item.pathOrUri)
            if (file.exists() && file.canRead()) FileInputStream(file) else null
        }
    }
}
