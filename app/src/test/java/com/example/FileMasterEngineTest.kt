package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.example.data.engine.BatchRenameEngine
import com.example.data.engine.BmpEncoder
import com.example.data.engine.ImageProcessorEngine
import com.example.model.FileCategory
import com.example.model.FileItem
import com.example.model.ImageFormat
import com.example.model.RenameConfig
import com.example.model.RenameConflictStatus
import com.example.model.RenameMode
import com.example.model.ResizeConfig
import com.example.model.ResizeMode
import com.example.model.SequencePosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FileMasterEngineTest {

    private fun sampleItem(name: String, parent: String = "/tmp/photos"): FileItem {
        val ext = name.substringAfterLast('.', "")
        return FileItem(
            id = "local::$parent/$name",
            name = name,
            pathOrUri = "$parent/$name",
            parentId = parent,
            isDirectory = false,
            sizeBytes = 1024L,
            lastModifiedMillis = 1000L,
            mimeType = "image/$ext",
            extension = ext,
            category = FileCategory.IMAGE,
            isSaf = false
        )
    }

    @Test
    fun `batch rename pattern replace produces expected MyTheme_001 sequence`() {
        val items = listOf(
            sampleItem("IMG_1024.png"),
            sampleItem("IMG_1025.png"),
            sampleItem("IMG_1026.png")
        )
        val config = RenameConfig(
            mode = RenameMode.REPLACE_NAME,
            customBaseName = "MyTheme_",
            useSequence = true,
            startNumber = 1,
            digits = 3,
            preserveExtension = true
        )
        val previews = BatchRenameEngine.generatePreviews(
            items = items,
            config = config,
            existingNamesByParent = mapOf("/tmp/photos" to items.map { it.name }.toSet())
        )

        assertFalse(BatchRenameEngine.hasBlockingConflicts(previews))
        assertEquals("MyTheme_001.png", previews[0].newName)
        assertEquals("MyTheme_002.png", previews[1].newName)
        assertEquals("MyTheme_003.png", previews[2].newName)
    }

    @Test
    fun `batch rename find replace with prefix and suffix detects duplicate conflict`() {
        val items = listOf(
            sampleItem("photo_a.jpg"),
            sampleItem("photo_b.jpg")
        )
        val config = RenameConfig(
            mode = RenameMode.MODIFY_ORIGINAL,
            findText = "a",
            replaceText = "b",
            useSequence = false,
            prefix = "pre_",
            suffix = "_suf"
        )
        val previews = BatchRenameEngine.generatePreviews(
            items = items,
            config = config,
            existingNamesByParent = mapOf("/tmp/photos" to items.map { it.name }.toSet())
        )

        assertTrue(BatchRenameEngine.hasBlockingConflicts(previews))
        assertEquals(RenameConflictStatus.DUPLICATE_IN_BATCH, previews[0].conflictStatus)
        assertEquals(RenameConflictStatus.DUPLICATE_IN_BATCH, previews[1].conflictStatus)
    }

    @Test
    fun `batch rename detects collision with unselected file in folder`() {
        val items = listOf(sampleItem("IMG_1.png"))
        val config = RenameConfig(
            mode = RenameMode.REPLACE_NAME,
            customBaseName = "Cover",
            useSequence = false
        )
        val previews = BatchRenameEngine.generatePreviews(
            items = items,
            config = config,
            existingNamesByParent = mapOf("/tmp/photos" to setOf("IMG_1.png", "Cover.png"))
        )

        assertTrue(BatchRenameEngine.hasBlockingConflicts(previews))
        assertEquals(RenameConflictStatus.EXISTS_IN_FOLDER, previews[0].conflictStatus)
    }

    @Test
    fun `two phase rename plan creates temp steps for cyclic swaps`() {
        val items = listOf(
            sampleItem("001.png"),
            sampleItem("002.png")
        )
        val config = RenameConfig(
            mode = RenameMode.REPLACE_NAME,
            customBaseName = "",
            useSequence = true,
            startNumber = 2,
            digits = 3,
            sequencePosition = SequencePosition.SUFFIX
        )
        val previews = BatchRenameEngine.generatePreviews(
            items = items,
            config = config,
            existingNamesByParent = mapOf("/tmp/photos" to setOf("001.png", "002.png"))
        )
        assertFalse(BatchRenameEngine.hasBlockingConflicts(previews))
        val plan = BatchRenameEngine.buildTwoPhasePlan(previews, "tok")
        assertTrue(plan.phaseOneTemps.isNotEmpty())
        assertEquals(2, plan.phaseTwoFinals.size)
        assertEquals("002.png", plan.phaseTwoFinals[0].third)
        assertEquals("003.png", plan.phaseTwoFinals[1].third)
    }

    @Test
    fun `bmp encoder writes valid BMP header and image processor resizes`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val bmp = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.BLUE)

        val baos = ByteArrayOutputStream()
        BmpEncoder.encode(bmp, baos)
        val bytes = baos.toByteArray()
        assertEquals('B'.code.toByte(), bytes[0])
        assertEquals('M'.code.toByte(), bytes[1])

        val decodedBmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        assertNotNull(decodedBmp)
        assertEquals(200, decodedBmp.width)
        assertEquals(100, decodedBmp.height)

        val tempSrc = File(context.cacheDir, "test_src.png")
        FileOutputStream(tempSrc).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val srcItem = sampleItem("test_src.png", context.cacheDir.absolutePath)

        val engine = ImageProcessorEngine(context)
        val outStream = ByteArrayOutputStream()
        val resizeCfg = ResizeConfig(
            mode = ResizeMode.CUSTOM_DIMENSIONS,
            targetWidth = 100,
            targetHeight = 100,
            maintainAspectRatio = true
        )
        val res = engine.processImageToStream(
            source = srcItem,
            outputStream = outStream,
            targetFormat = ImageFormat.WEBP,
            quality = 85,
            resizeConfig = resizeCfg,
            autoOrientExif = false
        )
        assertTrue(res.isSuccess)
        val (w, h) = res.getOrThrow()
        assertEquals(100, w)
        assertEquals(50, h)
        tempSrc.delete()
    }
}
