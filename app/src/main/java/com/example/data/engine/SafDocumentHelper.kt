package com.example.data.engine

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.webkit.MimeTypeMap
import com.example.model.FileCategory
import com.example.model.FileItem
import java.io.InputStream
import java.io.OutputStream

class SafDocumentHelper(private val context: Context) {

    private val resolver: ContentResolver = context.contentResolver

    fun getRootDocumentId(treeUri: Uri): String {
        return DocumentsContract.getTreeDocumentId(treeUri)
    }

    fun buildDocumentUri(treeUri: Uri, documentId: String): Uri {
        return DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
    }

    fun getTreeDisplayName(treeUri: Uri): String {
        val rootDocId = getRootDocumentId(treeUri)
        val docUri = buildDocumentUri(treeUri, rootDocId)
        resolver.query(
            docUri,
            arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val name = cursor.getString(0)
                if (!name.isNullOrBlank()) return name
            }
        }
        return rootDocId.substringAfterLast(':').ifBlank { "Mounted Folder" }
    }

    fun listChildren(treeUri: Uri, parentDocumentId: String): List<FileItem> {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocumentId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )

        val items = mutableListOf<FileItem>()
        val treeUriStr = treeUri.toString()

        resolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            val idxId = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val idxName = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val idxMime = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val idxSize = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
            val idxMod = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

            while (cursor.moveToNext()) {
                val docId = if (idxId >= 0) cursor.getString(idxId) ?: continue else continue
                val name = if (idxName >= 0) cursor.getString(idxName) ?: "Unnamed" else "Unnamed"
                val mime = if (idxMime >= 0) cursor.getString(idxMime) ?: "application/octet-stream" else "application/octet-stream"
                val size = if (idxSize >= 0 && !cursor.isNull(idxSize)) cursor.getLong(idxSize) else 0L
                val lastMod = if (idxMod >= 0 && !cursor.isNull(idxMod)) cursor.getLong(idxMod) else 0L

                val isDir = mime == DocumentsContract.Document.MIME_TYPE_DIR
                val ext = if (isDir) "" else name.substringAfterLast('.', "").lowercase()
                val docUri = buildDocumentUri(treeUri, docId)

                items.add(
                    FileItem(
                        id = "saf::$treeUriStr::$docId",
                        name = name,
                        pathOrUri = docUri.toString(),
                        parentId = "saf::$treeUriStr::$parentDocumentId",
                        isDirectory = isDir,
                        sizeBytes = size,
                        lastModifiedMillis = lastMod,
                        mimeType = mime,
                        extension = ext,
                        category = if (isDir) FileCategory.FOLDER else classifyCategory(mime, ext),
                        isSaf = true,
                        safTreeUri = treeUriStr,
                        safDocumentId = docId
                    )
                )
            }
        }
        return items
    }

    fun createDirectory(treeUri: Uri, parentDocumentId: String, name: String): FileItem? {
        val parentUri = buildDocumentUri(treeUri, parentDocumentId)
        val createdUri = runCatching {
            DocumentsContract.createDocument(
                resolver,
                parentUri,
                DocumentsContract.Document.MIME_TYPE_DIR,
                name
            )
        }.getOrNull() ?: return null

        val docId = DocumentsContract.getDocumentId(createdUri)
        val treeUriStr = treeUri.toString()
        return FileItem(
            id = "saf::$treeUriStr::$docId",
            name = name,
            pathOrUri = createdUri.toString(),
            parentId = "saf::$treeUriStr::$parentDocumentId",
            isDirectory = true,
            sizeBytes = 0L,
            lastModifiedMillis = System.currentTimeMillis(),
            mimeType = DocumentsContract.Document.MIME_TYPE_DIR,
            extension = "",
            category = FileCategory.FOLDER,
            isSaf = true,
            safTreeUri = treeUriStr,
            safDocumentId = docId
        )
    }

    fun createFile(treeUri: Uri, parentDocumentId: String, mimeType: String, name: String): FileItem? {
        val parentUri = buildDocumentUri(treeUri, parentDocumentId)
        val createdUri = runCatching {
            DocumentsContract.createDocument(resolver, parentUri, mimeType, name)
        }.getOrNull() ?: return null

        val docId = DocumentsContract.getDocumentId(createdUri)
        val treeUriStr = treeUri.toString()
        val ext = name.substringAfterLast('.', "").lowercase()
        return FileItem(
            id = "saf::$treeUriStr::$docId",
            name = name,
            pathOrUri = createdUri.toString(),
            parentId = "saf::$treeUriStr::$parentDocumentId",
            isDirectory = false,
            sizeBytes = 0L,
            lastModifiedMillis = System.currentTimeMillis(),
            mimeType = mimeType,
            extension = ext,
            category = classifyCategory(mimeType, ext),
            isSaf = true,
            safTreeUri = treeUriStr,
            safDocumentId = docId
        )
    }

    fun renameDocument(documentUri: Uri, newName: String): Uri? {
        return runCatching {
            DocumentsContract.renameDocument(resolver, documentUri, newName)
        }.getOrNull()
    }

    fun deleteDocument(documentUri: Uri): Boolean {
        return runCatching {
            DocumentsContract.deleteDocument(resolver, documentUri)
        }.getOrDefault(false)
    }

    fun openInputStream(uri: Uri): InputStream? {
        return runCatching { resolver.openInputStream(uri) }.getOrNull()
    }

    fun openOutputStream(uri: Uri): OutputStream? {
        return runCatching { resolver.openOutputStream(uri, "wt") }.getOrNull()
            ?: runCatching { resolver.openOutputStream(uri) }.getOrNull()
    }

    companion object {
        fun classifyCategory(mimeType: String, extension: String): FileCategory {
            val ext = extension.lowercase()
            return when {
                mimeType.startsWith("image/") || ext in setOf("png", "jpg", "jpeg", "webp", "bmp", "gif", "heic", "heif") ->
                    FileCategory.IMAGE
                mimeType.startsWith("video/") || ext in setOf("mp4", "mkv", "mov", "avi", "webm", "3gp") ->
                    FileCategory.VIDEO
                mimeType.startsWith("audio/") || ext in setOf("mp3", "wav", "ogg", "flac", "aac", "m4a") ->
                    FileCategory.AUDIO
                ext in setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz") || mimeType.contains("zip") || mimeType.contains("archive") ->
                    FileCategory.ARCHIVE
                mimeType.startsWith("text/") || mimeType.contains("pdf") || mimeType.contains("document") ||
                    ext in setOf("pdf", "doc", "docx", "txt", "md", "csv", "json", "xml", "xls", "xlsx", "ppt", "pptx") ->
                    FileCategory.DOCUMENT
                else -> FileCategory.OTHER
            }
        }

        fun guessMimeType(fileName: String): String {
            val ext = fileName.substringAfterLast('.', "").lowercase()
            return when (ext) {
                "png" -> "image/png"
                "jpg", "jpeg" -> "image/jpeg"
                "webp" -> "image/webp"
                "bmp" -> "image/bmp"
                "txt" -> "text/plain"
                "json" -> "application/json"
                "pdf" -> "application/pdf"
                "zip" -> "application/zip"
                else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
            }
        }
    }
}
