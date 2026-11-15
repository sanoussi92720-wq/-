package com.example.data.engine

import android.graphics.Bitmap
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Writes a standard uncompressed 24-bit Windows BMP (BITMAPFILEHEADER + BITMAPINFOHEADER)
 * so Android BitmapFactory and desktop image viewers can open converted BMP files reliably.
 */
object BmpEncoder {

    fun encode(bitmap: Bitmap, outputStream: OutputStream) {
        val width = bitmap.width
        val height = bitmap.height
        val rowBytesUnpadded = width * 3
        val rowPadding = (4 - (rowBytesUnpadded % 4)) % 4
        val stride = rowBytesUnpadded + rowPadding
        val pixelDataSize = stride * height
        val fileSize = 54 + pixelDataSize

        val header = ByteBuffer.allocate(54).order(ByteOrder.LITTLE_ENDIAN).apply {
            // BITMAPFILEHEADER (14 bytes)
            put('B'.code.toByte())
            put('M'.code.toByte())
            putInt(fileSize)
            putShort(0)
            putShort(0)
            putInt(54)

            // BITMAPINFOHEADER (40 bytes)
            putInt(40)
            putInt(width)
            putInt(height) // positive height = bottom-up rows
            putShort(1) // planes
            putShort(24) // bits per pixel
            putInt(0) // BI_RGB uncompressed
            putInt(pixelDataSize)
            putInt(2835) // ~72 DPI X
            putInt(2835) // ~72 DPI Y
            putInt(0)
            putInt(0)
        }

        outputStream.write(header.array())

        val rowPixels = IntArray(width)
        val rowBuffer = ByteArray(stride)

        // Write bottom-up rows
        for (y in height - 1 downTo 0) {
            bitmap.getPixels(rowPixels, 0, width, 0, y, width, 1)
            var offset = 0
            for (x in 0 until width) {
                val argb = rowPixels[x]
                // Blend alpha over white for 24-bit BMP
                val a = (argb ushr 24) and 0xFF
                var r = (argb ushr 16) and 0xFF
                var g = (argb ushr 8) and 0xFF
                var b = argb and 0xFF
                if (a < 255) {
                    val invA = 255 - a
                    r = (r * a + 255 * invA) / 255
                    g = (g * a + 255 * invA) / 255
                    b = (b * a + 255 * invA) / 255
                }
                rowBuffer[offset++] = b.toByte()
                rowBuffer[offset++] = g.toByte()
                rowBuffer[offset++] = r.toByte()
            }
            for (p in 0 until rowPadding) {
                rowBuffer[offset++] = 0
            }
            outputStream.write(rowBuffer)
        }
        outputStream.flush()
    }
}
