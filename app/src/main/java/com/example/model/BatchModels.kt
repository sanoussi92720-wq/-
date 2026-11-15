package com.example.model

import android.graphics.Bitmap

enum class RenameMode(val label: String, val description: String) {
    REPLACE_NAME(
        label = "Pattern Replace",
        description = "Replace original names with a clean pattern + sequence"
    ),
    MODIFY_ORIGINAL(
        label = "Edit Existing",
        description = "Keep original names with find/replace, prefix & suffix"
    )
}

enum class SequencePosition(val label: String) {
    SUFFIX("After Name"),
    PREFIX("Before Name")
}

enum class ExtensionCaseMode(val label: String) {
    PRESERVE("Preserve Original"),
    LOWERCASE("Force lowercase"),
    UPPERCASE("Force UPPERCASE")
}

enum class RenameTargetScope(val label: String) {
    SELECTED_ONLY("Selected Only"),
    ALL_IN_FOLDER("All Files in Folder"),
    ALL_IMAGES_IN_FOLDER("All Images in Folder")
}

data class RenameConfig(
    val mode: RenameMode = RenameMode.REPLACE_NAME,
    val customBaseName: String = "MyTheme_",
    val useSequence: Boolean = true,
    val startNumber: Int = 1,
    val stepNumber: Int = 1,
    val digits: Int = 3,
    val sequencePosition: SequencePosition = SequencePosition.SUFFIX,
    val prefix: String = "",
    val suffix: String = "",
    val findText: String = "",
    val replaceText: String = "",
    val caseSensitiveFind: Boolean = false,
    val preserveExtension: Boolean = true,
    val extensionCaseMode: ExtensionCaseMode = ExtensionCaseMode.PRESERVE,
    val customExtension: String = "",
    val scope: RenameTargetScope = RenameTargetScope.SELECTED_ONLY
)

enum class RenameConflictStatus {
    READY,
    UNCHANGED,
    DUPLICATE_IN_BATCH,
    EXISTS_IN_FOLDER,
    INVALID_NAME
}

data class RenamePreviewItem(
    val source: FileItem,
    val originalName: String,
    val newName: String,
    val conflictStatus: RenameConflictStatus,
    val conflictReason: String? = null
)

enum class ImageFormat(
    val label: String,
    val extension: String,
    val mimeType: String,
    val supportsQuality: Boolean
) {
    PNG("PNG", "png", "image/png", false),
    JPEG("JPG / JPEG", "jpg", "image/jpeg", true),
    WEBP("WEBP", "webp", "image/webp", true),
    BMP("BMP", "bmp", "image/bmp", false);

    companion object {
        fun fromExtension(ext: String): ImageFormat? {
            return when (ext.lowercase()) {
                "png" -> PNG
                "jpg", "jpeg" -> JPEG
                "webp" -> WEBP
                "bmp" -> BMP
                else -> null
            }
        }
    }
}

data class OutputFolderChoice(
    val label: String,
    val isSameAsSource: Boolean = true,
    val localDirPath: String? = null,
    val safTreeUri: String? = null,
    val safDocumentId: String? = null
)

data class ConvertConfig(
    val targetFormat: ImageFormat = ImageFormat.WEBP,
    val quality: Int = 88,
    val preserveOriginals: Boolean = true,
    val outputFolder: OutputFolderChoice = OutputFolderChoice("Converted (Workspace)", isSameAsSource = false)
)

enum class ResizeMode(val label: String) {
    PRESET("Preset"),
    CUSTOM_DIMENSIONS("Custom W×H"),
    PERCENTAGE("Percentage"),
    MAX_DIMENSION("Fit Max Edge")
}

data class ResizePreset(
    val id: String,
    val label: String,
    val width: Int,
    val height: Int
)

val DEFAULT_RESIZE_PRESETS = listOf(
    ResizePreset("square_1080", "1080 × 1080 (Square)", 1080, 1080),
    ResizePreset("fhd_1920", "1920 × 1080 (Full HD)", 1920, 1080),
    ResizePreset("hd_1280", "1280 × 720 (HD)", 1280, 720),
    ResizePreset("insta_portrait", "1080 × 1350 (Portrait)", 1080, 1350),
    ResizePreset("web_800", "800 × 800 (Web)", 800, 800),
    ResizePreset("icon_512", "512 × 512 (Thumb)", 512, 512)
)

data class ResizeConfig(
    val mode: ResizeMode = ResizeMode.PRESET,
    val targetWidth: Int = 1080,
    val targetHeight: Int = 1080,
    val maintainAspectRatio: Boolean = true,
    val percentage: Int = 50,
    val maxEdgePx: Int = 1080,
    val selectedPresetId: String = "square_1080",
    val keepOriginalFormat: Boolean = true,
    val outputFormat: ImageFormat = ImageFormat.WEBP,
    val quality: Int = 90,
    val preserveOriginals: Boolean = true,
    val outputFolder: OutputFolderChoice = OutputFolderChoice("Resized (Workspace)", isSameAsSource = false)
)

data class PipelineConfig(
    val enableRename: Boolean = true,
    val renameConfig: RenameConfig = RenameConfig(
        mode = RenameMode.REPLACE_NAME,
        customBaseName = "Export_",
        useSequence = true,
        startNumber = 1,
        digits = 3
    ),
    val enableConvert: Boolean = true,
    val targetFormat: ImageFormat = ImageFormat.WEBP,
    val quality: Int = 88,
    val enableResize: Boolean = true,
    val resizeConfig: ResizeConfig = ResizeConfig(
        mode = ResizeMode.PRESET,
        targetWidth = 1080,
        targetHeight = 1080,
        maintainAspectRatio = true
    ),
    val preserveOriginals: Boolean = true,
    val outputFolder: OutputFolderChoice = OutputFolderChoice("Pipeline_Exports (Workspace)", isSameAsSource = false)
)

enum class BatchOperationType(val label: String) {
    RENAME("Batch Rename"),
    CONVERT("Batch Convert"),
    RESIZE("Batch Resize"),
    PIPELINE("Batch Pipeline")
}

data class BatchProgress(
    val operationType: BatchOperationType,
    val totalCount: Int,
    val completedCount: Int,
    val successCount: Int,
    val failureCount: Int,
    val currentFileName: String,
    val isRunning: Boolean,
    val isCancelled: Boolean = false
) {
    val fraction: Float
        get() = if (totalCount > 0) (completedCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f) else 0f
}

data class BatchItemFailure(
    val fileName: String,
    val reason: String
)

data class BatchResultSummary(
    val operationType: BatchOperationType,
    val totalCount: Int,
    val successCount: Int,
    val failureCount: Int,
    val failures: List<BatchItemFailure> = emptyList(),
    val outputFiles: List<FileItem> = emptyList(),
    val undoGroupId: Long? = null,
    val message: String = ""
)

data class ImageResizeComparison(
    val sourceName: String,
    val originalWidth: Int,
    val originalHeight: Int,
    val targetWidth: Int,
    val targetHeight: Int,
    val originalBytes: Long
)

data class ImagePreviewComparison(
    val originalBitmap: Bitmap?,
    val processedBitmap: Bitmap?,
    val originalWidth: Int,
    val originalHeight: Int,
    val newWidth: Int,
    val newHeight: Int,
    val originalSizeBytes: Long,
    val estimatedNewSizeBytes: Long,
    val outputFormat: ImageFormat
)
