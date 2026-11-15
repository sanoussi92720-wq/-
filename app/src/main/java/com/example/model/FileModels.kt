package com.example.model

import android.net.Uri

enum class FileCategory(val label: String) {
    FOLDER("Folders"),
    IMAGE("Images"),
    VIDEO("Videos"),
    AUDIO("Audio"),
    DOCUMENT("Documents"),
    ARCHIVE("Archives"),
    OTHER("Other")
}

enum class StorageKind {
    APP_WORKSPACE,
    EXTERNAL_APP_STORAGE,
    SAF_TREE
}

data class StorageRoot(
    val id: String,
    val name: String,
    val subtitle: String,
    val kind: StorageKind,
    val rootPath: String? = null,
    val treeUri: String? = null
)

data class BreadcrumbNode(
    val name: String,
    val pathOrDocId: String,
    val isSaf: Boolean,
    val treeUri: String? = null
)

data class FileItem(
    val id: String,
    val name: String,
    val pathOrUri: String,
    val parentId: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModifiedMillis: Long,
    val mimeType: String,
    val extension: String,
    val category: FileCategory,
    val isSaf: Boolean,
    val safTreeUri: String? = null,
    val safDocumentId: String? = null,
    val imageWidth: Int? = null,
    val imageHeight: Int? = null,
    val childCount: Int = 0
) {
    val uri: Uri
        get() = if (isSaf) Uri.parse(pathOrUri) else Uri.fromFile(java.io.File(pathOrUri))

    val isImage: Boolean
        get() = category == FileCategory.IMAGE

    val nameWithoutExtension: String
        get() {
            if (isDirectory) return name
            val dot = name.lastIndexOf('.')
            return if (dot > 0) name.substring(0, dot) else name
        }
}

enum class SortField(val label: String) {
    NAME("Name"),
    SIZE("Size"),
    DATE("Date"),
    TYPE("Type")
}

enum class SortDirection {
    ASCENDING,
    DESCENDING
}

enum class ViewMode {
    LIST,
    GRID
}

data class FolderSizeInfo(
    val id: String,
    val name: String,
    val pathOrUri: String,
    val sizeBytes: Long,
    val itemCount: Int,
    val lastModifiedMillis: Long = 0L,
    val isSaf: Boolean = false,
    val safTreeUri: String? = null,
    val safDocumentId: String? = null
)

data class StorageStats(
    val totalBytes: Long = 0L,
    val freeBytes: Long = 0L,
    val workspaceUsedBytes: Long = 0L,
    val imageCount: Int = 0,
    val imageBytes: Long = 0L,
    val videoBytes: Long = 0L,
    val documentBytes: Long = 0L,
    val otherBytes: Long = 0L,
    val largestFolders: List<FolderSizeInfo> = emptyList()
) {
    val usedBytes: Long get() = (totalBytes - freeBytes).coerceAtLeast(0L)
    val usedFraction: Float
        get() = if (totalBytes > 0L) (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
}

data class ImageFormatCount(
    val formatLabel: String,
    val count: Int,
    val totalBytes: Long
)
