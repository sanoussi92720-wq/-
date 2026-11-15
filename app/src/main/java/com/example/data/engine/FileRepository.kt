package com.example.data.engine

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.StatFs
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.example.data.local.BatchHistoryDao
import com.example.data.local.BatchUndoGroupEntity
import com.example.data.local.BatchUndoItemEntity
import com.example.data.local.RecentEntryDao
import com.example.data.local.RecentEntryEntity
import com.example.data.local.SettingsDataStore
import com.example.model.BatchItemFailure
import com.example.model.BatchOperationType
import com.example.model.BatchProgress
import com.example.model.BatchResultSummary
import com.example.model.ConvertConfig
import com.example.model.FileCategory
import com.example.model.FileItem
import com.example.model.FolderSizeInfo
import com.example.model.ImageFormat
import com.example.model.OutputFolderChoice
import com.example.model.PipelineConfig
import com.example.model.RenameConfig
import com.example.model.RenamePreviewItem
import com.example.model.ResizeConfig
import com.example.model.StorageKind
import com.example.model.StorageRoot
import com.example.model.StorageStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID

class FileRepository(
    private val context: Context,
    private val safHelper: SafDocumentHelper,
    private val imageEngine: ImageProcessorEngine,
    private val recentEntryDao: RecentEntryDao,
    private val batchHistoryDao: BatchHistoryDao,
    private val settingsDataStore: SettingsDataStore
) {

    val workspaceRootDir: File by lazy {
        File(context.filesDir, "FileMaster").apply {
            if (!exists()) {
                mkdirs()
                File(this, "Pictures").mkdirs()
                File(this, "Documents").mkdirs()
                File(this, "Converted").mkdirs()
                File(this, "Resized").mkdirs()
                File(this, "Exports").mkdirs()
            }
        }
    }

    val externalAppRootDir: File? by lazy {
        context.getExternalFilesDir(null)?.let { dir ->
            File(dir, "Workspace").apply { if (!exists()) mkdirs() }
        }
    }

    fun observeRecentFiles(): Flow<List<RecentEntryEntity>> = recentEntryDao.observeRecentFiles()
    fun observeRecentFolders(): Flow<List<RecentEntryEntity>> = recentEntryDao.observeRecentFolders()
    fun observeBatchHistory(): Flow<List<BatchUndoGroupEntity>> = batchHistoryDao.observeGroups()

    suspend fun getStorageRoots(): List<StorageRoot> = withContext(Dispatchers.IO) {
        val roots = mutableListOf(
            StorageRoot(
                id = "workspace",
                name = "App Workspace",
                subtitle = "Internal private workspace",
                kind = StorageKind.APP_WORKSPACE,
                rootPath = workspaceRootDir.absolutePath
            )
        )
        externalAppRootDir?.let { extDir ->
            roots.add(
                StorageRoot(
                    id = "external_workspace",
                    name = "Shared App Folder",
                    subtitle = extDir.name,
                    kind = StorageKind.EXTERNAL_APP_STORAGE,
                    rootPath = extDir.absolutePath
                )
            )
        }
        val safUris = settingsDataStore.settingsFlow.first().safTreeUris
        safUris.forEach { uriStr ->
            val uri = runCatching { Uri.parse(uriStr) }.getOrNull() ?: return@forEach
            val hasPerm = context.contentResolver.persistedUriPermissions.any {
                it.uri == uri && it.isReadPermission
            }
            if (hasPerm) {
                val title = safHelper.getTreeDisplayName(uri)
                roots.add(
                    StorageRoot(
                        id = "saf::$uriStr",
                        name = title,
                        subtitle = "Device Folder (SAF)",
                        kind = StorageKind.SAF_TREE,
                        treeUri = uriStr
                    )
                )
            }
        }
        roots
    }

    suspend fun mountSafTree(treeUri: Uri): StorageRoot? = withContext(Dispatchers.IO) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching {
            context.contentResolver.takePersistableUriPermission(treeUri, flags)
        }.onFailure {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }
        val uriStr = treeUri.toString()
        settingsDataStore.addSafTreeUri(uriStr)
        val name = safHelper.getTreeDisplayName(treeUri)
        StorageRoot(
            id = "saf::$uriStr",
            name = name,
            subtitle = "Device Folder (SAF)",
            kind = StorageKind.SAF_TREE,
            treeUri = uriStr
        )
    }

    suspend fun unmountSafTree(uriString: String) = withContext(Dispatchers.IO) {
        runCatching {
            val uri = Uri.parse(uriString)
            context.contentResolver.releasePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
        settingsDataStore.removeSafTreeUri(uriString)
    }

    suspend fun listLocalDirectory(dirPath: String): List<FileItem> = withContext(Dispatchers.IO) {
        val dir = File(dirPath)
        if (!dir.exists() || !dir.isDirectory) return@withContext emptyList()
        val files = dir.listFiles() ?: return@withContext emptyList()
        files.filter { !it.name.startsWith(".__fm_tmp_") }
            .map { fileToItem(it) }
    }

    suspend fun listSafDirectory(treeUriStr: String, documentId: String): List<FileItem> =
        withContext(Dispatchers.IO) {
            val treeUri = Uri.parse(treeUriStr)
            safHelper.listChildren(treeUri, documentId)
                .filter { !it.name.startsWith(".__fm_tmp_") }
        }

    suspend fun listAllImagesAcrossRoots(limitPerTree: Int = 1500): List<FileItem> =
        withContext(Dispatchers.IO) {
            val collected = mutableListOf<FileItem>()
            val seenIds = HashSet<String>()

            fun collectLocal(dir: File, depth: Int) {
                if (depth > 6 || collected.size >= limitPerTree) return
                val children = dir.listFiles() ?: return
                for (child in children) {
                    if (child.name.startsWith(".__fm_tmp_")) continue
                    if (child.isDirectory) {
                        collectLocal(child, depth + 1)
                    } else {
                        val item = fileToItem(child)
                        if (item.isImage && seenIds.add(item.id)) {
                            collected.add(item)
                        }
                    }
                    if (collected.size >= limitPerTree) break
                }
            }

            collectLocal(workspaceRootDir, 0)
            externalAppRootDir?.let { collectLocal(it, 0) }

            val roots = getStorageRoots().filter { it.kind == StorageKind.SAF_TREE && it.treeUri != null }
            for (root in roots) {
                val treeUri = Uri.parse(root.treeUri!!)
                val rootDocId = safHelper.getRootDocumentId(treeUri)
                val queue = ArrayDeque<Pair<String, Int>>()
                queue.add(rootDocId to 0)
                while (queue.isNotEmpty() && collected.size < limitPerTree) {
                    val (docId, depth) = queue.removeFirst()
                    val children = safHelper.listChildren(treeUri, docId)
                    for (child in children) {
                        if (child.name.startsWith(".__fm_tmp_")) continue
                        if (child.isDirectory && depth < 4 && child.safDocumentId != null) {
                            queue.add(child.safDocumentId to depth + 1)
                        } else if (child.isImage && seenIds.add(child.id)) {
                            collected.add(child)
                        }
                        if (collected.size >= limitPerTree) break
                    }
                }
            }

            collected.sortedByDescending { it.lastModifiedMillis }
        }

    suspend fun searchAcrossRoots(query: String): List<FileItem> = withContext(Dispatchers.IO) {
        val q = query.trim().lowercase()
        if (q.isBlank()) return@withContext emptyList()
        val results = mutableListOf<FileItem>()

        fun walkLocal(dir: File, depth: Int) {
            if (depth > 6 || results.size >= 400) return
            val children = dir.listFiles() ?: return
            for (child in children) {
                if (child.name.startsWith(".__fm_tmp_")) continue
                if (child.name.lowercase().contains(q)) {
                    results.add(fileToItem(child))
                }
                if (child.isDirectory) walkLocal(child, depth + 1)
                if (results.size >= 400) break
            }
        }
        walkLocal(workspaceRootDir, 0)
        externalAppRootDir?.let { walkLocal(it, 0) }

        val roots = getStorageRoots().filter { it.kind == StorageKind.SAF_TREE && it.treeUri != null }
        for (root in roots) {
            val treeUri = Uri.parse(root.treeUri!!)
            val rootDocId = safHelper.getRootDocumentId(treeUri)
            val queue = ArrayDeque<Pair<String, Int>>()
            queue.add(rootDocId to 0)
            while (queue.isNotEmpty() && results.size < 400) {
                val (docId, depth) = queue.removeFirst()
                val children = safHelper.listChildren(treeUri, docId)
                for (child in children) {
                    if (child.name.startsWith(".__fm_tmp_")) continue
                    if (child.name.lowercase().contains(q)) {
                        results.add(child)
                    }
                    if (child.isDirectory && depth < 3 && child.safDocumentId != null) {
                        queue.add(child.safDocumentId to depth + 1)
                    }
                    if (results.size >= 400) break
                }
            }
        }
        results
    }

    suspend fun computeStorageStats(): StorageStats = withContext(Dispatchers.IO) {
        val statFs = StatFs(context.filesDir.absolutePath)
        val totalBytes = statFs.totalBytes
        val freeBytes = statFs.availableBytes

        var workspaceBytes = 0L
        var imageCount = 0
        var imageBytes = 0L
        var videoBytes = 0L
        var documentBytes = 0L
        var otherBytes = 0L
        val folderTotals = mutableListOf<FolderSizeInfo>()

        fun scan(dir: File): Pair<Long, Int> {
            var dirBytes = 0L
            var dirItems = 0
            val children = dir.listFiles() ?: return 0L to 0
            for (child in children) {
                if (child.name.startsWith(".__fm_tmp_")) continue
                dirItems++
                if (child.isDirectory) {
                    val (subBytes, _) = scan(child)
                    dirBytes += subBytes
                } else {
                    val len = child.length()
                    dirBytes += len
                    workspaceBytes += len
                    val ext = child.extension.lowercase()
                    val mime = SafDocumentHelper.guessMimeType(child.name)
                    when (SafDocumentHelper.classifyCategory(mime, ext)) {
                        FileCategory.IMAGE -> {
                            imageCount++
                            imageBytes += len
                        }
                        FileCategory.VIDEO -> videoBytes += len
                        FileCategory.DOCUMENT -> documentBytes += len
                        else -> otherBytes += len
                    }
                }
            }
            return dirBytes to dirItems
        }

        workspaceRootDir.listFiles()?.filter { it.isDirectory }?.forEach { sub ->
            val (sz, cnt) = scan(sub)
            folderTotals.add(
                FolderSizeInfo(
                    id = "local::${sub.absolutePath}",
                    name = sub.name,
                    pathOrUri = sub.absolutePath,
                    sizeBytes = sz,
                    itemCount = cnt,
                    lastModifiedMillis = sub.lastModified(),
                    isSaf = false
                )
            )
        }
        workspaceRootDir.listFiles()?.filter { !it.isDirectory && !it.name.startsWith(".__fm_tmp_") }?.forEach { file ->
            val len = file.length()
            workspaceBytes += len
            val ext = file.extension.lowercase()
            val mime = SafDocumentHelper.guessMimeType(file.name)
            when (SafDocumentHelper.classifyCategory(mime, ext)) {
                FileCategory.IMAGE -> { imageCount++; imageBytes += len }
                FileCategory.VIDEO -> videoBytes += len
                FileCategory.DOCUMENT -> documentBytes += len
                else -> otherBytes += len
            }
        }
        externalAppRootDir?.let { extDir ->
            val (sz, cnt) = scan(extDir)
            if (cnt > 0) {
                folderTotals.add(
                    FolderSizeInfo(
                        id = "local::${extDir.absolutePath}",
                        name = extDir.name,
                        pathOrUri = extDir.absolutePath,
                        sizeBytes = sz,
                        itemCount = cnt,
                        lastModifiedMillis = extDir.lastModified(),
                        isSaf = false
                    )
                )
            }
        }

        val safRoots = getStorageRoots().filter { it.kind == StorageKind.SAF_TREE && it.treeUri != null }
        for (root in safRoots) {
            val treeUri = Uri.parse(root.treeUri!!)
            val rootDocId = safHelper.getRootDocumentId(treeUri)
            val children = safHelper.listChildren(treeUri, rootDocId)
            var safBytes = 0L
            var safCount = 0
            children.forEach { child ->
                if (!child.isDirectory) {
                    safBytes += child.sizeBytes
                    safCount++
                    when (child.category) {
                        FileCategory.IMAGE -> { imageCount++; imageBytes += child.sizeBytes }
                        FileCategory.VIDEO -> videoBytes += child.sizeBytes
                        FileCategory.DOCUMENT -> documentBytes += child.sizeBytes
                        else -> otherBytes += child.sizeBytes
                    }
                } else {
                    safCount++
                }
            }
            folderTotals.add(
                FolderSizeInfo(
                    id = root.id,
                    name = root.name,
                    pathOrUri = root.treeUri,
                    sizeBytes = safBytes,
                    itemCount = safCount,
                    lastModifiedMillis = System.currentTimeMillis(),
                    isSaf = true,
                    safTreeUri = root.treeUri,
                    safDocumentId = rootDocId
                )
            )
        }

        StorageStats(
            totalBytes = totalBytes,
            freeBytes = freeBytes,
            workspaceUsedBytes = workspaceBytes,
            imageCount = imageCount,
            imageBytes = imageBytes,
            videoBytes = videoBytes,
            documentBytes = documentBytes,
            otherBytes = otherBytes,
            largestFolders = folderTotals.sortedByDescending { it.sizeBytes }.take(8)
        )
    }

    suspend fun listQuickFolders(): List<FolderSizeInfo> = withContext(Dispatchers.IO) {
        val list = mutableListOf<FolderSizeInfo>()
        workspaceRootDir.listFiles()?.filter { it.isDirectory }?.forEach { dir ->
            val children = dir.listFiles()?.filter { !it.name.startsWith(".__fm_tmp_") }.orEmpty()
            val size = children.sumOf { if (it.isFile) it.length() else 0L }
            list.add(
                FolderSizeInfo(
                    id = "local::${dir.absolutePath}",
                    name = dir.name,
                    pathOrUri = dir.absolutePath,
                    sizeBytes = size,
                    itemCount = children.size,
                    lastModifiedMillis = dir.lastModified(),
                    isSaf = false
                )
            )
        }
        getStorageRoots().filter { it.kind == StorageKind.SAF_TREE && it.treeUri != null }.forEach { root ->
            val treeUri = Uri.parse(root.treeUri!!)
            val rootDocId = safHelper.getRootDocumentId(treeUri)
            val children = safHelper.listChildren(treeUri, rootDocId)
            list.add(
                FolderSizeInfo(
                    id = root.id,
                    name = root.name,
                    pathOrUri = root.treeUri,
                    sizeBytes = children.sumOf { it.sizeBytes },
                    itemCount = children.size,
                    lastModifiedMillis = System.currentTimeMillis(),
                    isSaf = true,
                    safTreeUri = root.treeUri,
                    safDocumentId = rootDocId
                )
            )
        }
        list.sortedByDescending { it.itemCount }
    }

    suspend fun recordAccess(item: FileItem) = withContext(Dispatchers.IO) {
        recentEntryDao.upsert(
            RecentEntryEntity(
                id = item.id,
                name = item.name,
                pathOrUri = item.pathOrUri,
                isDirectory = item.isDirectory,
                isSaf = item.isSaf,
                safTreeUri = item.safTreeUri,
                safDocumentId = item.safDocumentId,
                mimeType = item.mimeType,
                sizeBytes = item.sizeBytes,
                lastAccessedMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun createFolder(
        isSaf: Boolean,
        parentPathOrDocId: String,
        safTreeUri: String?,
        folderName: String
    ): FileItem? = withContext(Dispatchers.IO) {
        val cleanName = folderName.trim()
        if (cleanName.isBlank()) return@withContext null
        if (isSaf && safTreeUri != null) {
            safHelper.createDirectory(Uri.parse(safTreeUri), parentPathOrDocId, cleanName)
        } else {
            val target = File(parentPathOrDocId, cleanName)
            if (target.exists() || target.mkdirs()) {
                fileToItem(target)
            } else null
        }
    }

    suspend fun createEmptyFile(
        isSaf: Boolean,
        parentPathOrDocId: String,
        safTreeUri: String?,
        fileName: String,
        initialContent: String = ""
    ): FileItem? = withContext(Dispatchers.IO) {
        val cleanName = fileName.trim()
        if (cleanName.isBlank()) return@withContext null
        if (isSaf && safTreeUri != null) {
            val mime = SafDocumentHelper.guessMimeType(cleanName)
            val created = safHelper.createFile(Uri.parse(safTreeUri), parentPathOrDocId, mime, cleanName)
            if (created != null && initialContent.isNotEmpty()) {
                safHelper.openOutputStream(created.uri)?.use {
                    it.write(initialContent.toByteArray())
                }
            }
            created
        } else {
            val target = File(parentPathOrDocId, cleanName)
            if (!target.exists()) {
                target.writeText(initialContent)
                fileToItem(target)
            } else null
        }
    }

    suspend fun renameSingle(item: FileItem, newName: String): FileItem? = withContext(Dispatchers.IO) {
        val clean = newName.trim()
        if (clean.isBlank() || clean == item.name) return@withContext item
        if (item.isSaf && item.safTreeUri != null) {
            val newUri = safHelper.renameDocument(Uri.parse(item.pathOrUri), clean) ?: return@withContext null
            val newDocId = DocumentsContract.getDocumentId(newUri)
            val ext = if (item.isDirectory) "" else clean.substringAfterLast('.', "").lowercase()
            val mime = if (item.isDirectory) item.mimeType else SafDocumentHelper.guessMimeType(clean)
            item.copy(
                id = "saf::${item.safTreeUri}::$newDocId",
                name = clean,
                pathOrUri = newUri.toString(),
                extension = ext,
                mimeType = mime,
                category = if (item.isDirectory) FileCategory.FOLDER else SafDocumentHelper.classifyCategory(mime, ext),
                safDocumentId = newDocId
            )
        } else {
            val srcFile = File(item.pathOrUri)
            val destFile = File(srcFile.parentFile, clean)
            if (destFile.exists()) return@withContext null
            if (srcFile.renameTo(destFile)) {
                fileToItem(destFile)
            } else null
        }
    }

    suspend fun deleteItems(items: List<FileItem>): Pair<Int, Int> = withContext(Dispatchers.IO) {
        var success = 0
        var failed = 0
        for (item in items) {
            val ok = if (item.isSaf) {
                safHelper.deleteDocument(Uri.parse(item.pathOrUri))
            } else {
                File(item.pathOrUri).deleteRecursively()
            }
            if (ok) {
                success++
                recentEntryDao.deleteById(item.id)
            } else {
                failed++
            }
        }
        success to failed
    }

    suspend fun copyOrMoveItems(
        items: List<FileItem>,
        destIsSaf: Boolean,
        destPathOrDocId: String,
        destSafTreeUri: String?,
        isMove: Boolean
    ): Pair<Int, Int> = withContext(Dispatchers.IO) {
        var success = 0
        var failed = 0
        for (item in items) {
            val copied = copySingleRecursive(item, destIsSaf, destPathOrDocId, destSafTreeUri)
            if (copied != null) {
                if (isMove) {
                    val deleted = if (item.isSaf) {
                        safHelper.deleteDocument(Uri.parse(item.pathOrUri))
                    } else {
                        File(item.pathOrUri).deleteRecursively()
                    }
                    if (deleted) success++ else failed++
                } else {
                    success++
                }
            } else {
                failed++
            }
        }
        success to failed
    }

    private fun copySingleRecursive(
        item: FileItem,
        destIsSaf: Boolean,
        destPathOrDocId: String,
        destSafTreeUri: String?
    ): FileItem? {
        return if (item.isDirectory) {
            val newDir = if (destIsSaf && destSafTreeUri != null) {
                val uniqueName = resolveUniqueSafName(Uri.parse(destSafTreeUri), destPathOrDocId, item.name)
                safHelper.createDirectory(Uri.parse(destSafTreeUri), destPathOrDocId, uniqueName)
            } else {
                val destDir = resolveUniqueLocalFile(File(destPathOrDocId), item.name)
                if (destDir.mkdirs()) fileToItem(destDir) else null
            } ?: return null

            val children = if (item.isSaf && item.safTreeUri != null && item.safDocumentId != null) {
                safHelper.listChildren(Uri.parse(item.safTreeUri), item.safDocumentId)
            } else {
                File(item.pathOrUri).listFiles()?.map { fileToItem(it) } ?: emptyList()
            }
            val nextParent = if (destIsSaf) newDir.safDocumentId!! else newDir.pathOrUri
            children.forEach { child ->
                copySingleRecursive(child, destIsSaf, nextParent, destSafTreeUri)
            }
            newDir
        } else {
            val createdDest = if (destIsSaf && destSafTreeUri != null) {
                val uniqueName = resolveUniqueSafName(Uri.parse(destSafTreeUri), destPathOrDocId, item.name)
                safHelper.createFile(Uri.parse(destSafTreeUri), destPathOrDocId, item.mimeType, uniqueName)
            } else {
                val destFile = resolveUniqueLocalFile(File(destPathOrDocId), item.name)
                fileToItem(destFile)
            } ?: return null

            val input = openInputStream(item) ?: return null
            val output = openOutputStream(createdDest) ?: run {
                input.close()
                return null
            }
            val ok = runCatching {
                input.use { ins ->
                    output.use { outs ->
                        ins.copyTo(outs, bufferSize = 64 * 1024)
                    }
                }
                true
            }.getOrDefault(false)
            if (!ok) return null
            if (!destIsSaf) fileToItem(File(createdDest.pathOrUri)) else createdDest
        }
    }

    suspend fun importPickedUris(
        uris: List<Uri>,
        destIsSaf: Boolean,
        destPathOrDocId: String,
        destSafTreeUri: String?
    ): List<FileItem> = withContext(Dispatchers.IO) {
        val imported = mutableListOf<FileItem>()
        for (uri in uris) {
            val rawName = queryDisplayName(uri) ?: "Image_${System.currentTimeMillis()}.jpg"
            val mime = context.contentResolver.getType(uri) ?: SafDocumentHelper.guessMimeType(rawName)
            val created = if (destIsSaf && destSafTreeUri != null) {
                val uniqueName = resolveUniqueSafName(Uri.parse(destSafTreeUri), destPathOrDocId, rawName)
                safHelper.createFile(Uri.parse(destSafTreeUri), destPathOrDocId, mime, uniqueName)
            } else {
                val destFile = resolveUniqueLocalFile(File(destPathOrDocId), rawName)
                fileToItem(destFile)
            } ?: continue

            val ok = runCatching {
                context.contentResolver.openInputStream(uri)?.use { ins ->
                    openOutputStream(created)?.use { outs ->
                        ins.copyTo(outs, 64 * 1024)
                    }
                }
                true
            }.getOrDefault(false)

            if (ok) {
                val finalItem = if (!destIsSaf) fileToItem(File(created.pathOrUri)) else created
                imported.add(finalItem)
                recordAccess(finalItem)
            }
        }
        imported
    }

    suspend fun getShareUris(items: List<FileItem>): ArrayList<Uri> = withContext(Dispatchers.IO) {
        val result = ArrayList<Uri>()
        val authority = "${context.packageName}.fileprovider"
        for (item in items) {
            if (item.isDirectory) continue
            if (item.isSaf) {
                result.add(Uri.parse(item.pathOrUri))
            } else {
                val file = File(item.pathOrUri)
                if (file.exists()) {
                    runCatching {
                        FileProvider.getUriForFile(context, authority, file)
                    }.getOrNull()?.let { result.add(it) }
                }
            }
        }
        result
    }

    suspend fun getSiblingNamesByParent(items: List<FileItem>): Map<String, Set<String>> =
        withContext(Dispatchers.IO) {
            val map = mutableMapOf<String, Set<String>>()
            items.groupBy { it.parentId }.forEach { (parentId, group) ->
                val sample = group.first()
                val names = if (sample.isSaf && sample.safTreeUri != null) {
                    val parentDocId = sample.parentId.substringAfterLast("::")
                    safHelper.listChildren(Uri.parse(sample.safTreeUri), parentDocId)
                        .map { it.name }
                        .toSet()
                } else {
                    File(sample.parentId).listFiles()?.map { it.name }?.toSet() ?: emptySet()
                }
                map[parentId] = names
            }
            map
        }

    suspend fun executeBatchRename(
        previews: List<RenamePreviewItem>,
        onProgress: (BatchProgress) -> Unit
    ): BatchResultSummary = withContext(Dispatchers.IO) {
        val token = UUID.randomUUID().toString().take(8)
        val plan = BatchRenameEngine.buildTwoPhasePlan(previews, token)
        val total = plan.phaseTwoFinals.size
        if (total == 0) {
            return@withContext BatchResultSummary(
                operationType = BatchOperationType.RENAME,
                totalCount = 0,
                successCount = 0,
                failureCount = 0,
                message = "No files required renaming."
            )
        }

        val tempRenamedMap = mutableMapOf<String, FileItem>()
        val failures = mutableListOf<BatchItemFailure>()
        val undoItems = mutableListOf<BatchUndoItemEntity>()
        val outputItems = mutableListOf<FileItem>()

        // Phase 1: Rename any cyclic/colliding items to temporary names
        for ((src, tempName) in plan.phaseOneTemps) {
            currentCoroutineContext().ensureActive()
            val tmpItem = renameSingle(src, tempName)
            if (tmpItem != null) {
                tempRenamedMap[src.id] = tmpItem
            } else {
                failures.add(BatchItemFailure(src.name, "Could not prepare temporary rename"))
            }
        }

        var completed = 0
        var success = 0

        // Phase 2: Rename to final target names
        for ((originalSrc, _, targetName) in plan.phaseTwoFinals) {
            currentCoroutineContext().ensureActive()
            val currentItem = tempRenamedMap[originalSrc.id] ?: originalSrc
            onProgress(
                BatchProgress(
                    operationType = BatchOperationType.RENAME,
                    totalCount = total,
                    completedCount = completed,
                    successCount = success,
                    failureCount = failures.size,
                    currentFileName = originalSrc.name,
                    isRunning = true
                )
            )
            val renamed = renameSingle(currentItem, targetName)
            completed++
            if (renamed != null) {
                success++
                outputItems.add(renamed)
                val parentPathOrDocId = if (originalSrc.isSaf) {
                    originalSrc.parentId.substringAfterLast("::")
                } else {
                    originalSrc.parentId
                }
                undoItems.add(
                    BatchUndoItemEntity(
                        groupId = 0,
                        originalName = originalSrc.name,
                        newName = renamed.name,
                        originalPathOrUri = originalSrc.pathOrUri,
                        newPathOrUri = renamed.pathOrUri,
                        parentPathOrUri = parentPathOrDocId,
                        isSaf = originalSrc.isSaf,
                        safTreeUri = originalSrc.safTreeUri
                    )
                )
            } else {
                failures.add(BatchItemFailure(originalSrc.name, "Rename rejected by storage provider"))
            }
        }

        onProgress(
            BatchProgress(
                operationType = BatchOperationType.RENAME,
                totalCount = total,
                completedCount = total,
                successCount = success,
                failureCount = failures.size,
                currentFileName = "",
                isRunning = false
            )
        )

        val groupId = if (undoItems.isNotEmpty()) {
            batchHistoryDao.saveUndoBatch(
                group = BatchUndoGroupEntity(
                    operationType = BatchOperationType.RENAME.label,
                    summaryText = "Renamed $success of $total files",
                    timestampMillis = System.currentTimeMillis(),
                    isReversible = true
                ),
                items = undoItems
            )
        } else null

        BatchResultSummary(
            operationType = BatchOperationType.RENAME,
            totalCount = total,
            successCount = success,
            failureCount = failures.size,
            failures = failures,
            outputFiles = outputItems,
            undoGroupId = groupId,
            message = "Renamed $success of $total files"
        )
    }

    suspend fun undoBatchRename(groupId: Long): BatchResultSummary = withContext(Dispatchers.IO) {
        val group = batchHistoryDao.getGroup(groupId)
        if (group == null || !group.isReversible || group.isUndone) {
            return@withContext BatchResultSummary(
                operationType = BatchOperationType.RENAME,
                totalCount = 0,
                successCount = 0,
                failureCount = 0,
                message = "Undo is no longer available for this batch."
            )
        }
        val items = batchHistoryDao.getItemsForGroup(groupId)
        val token = UUID.randomUUID().toString().take(8)
        var success = 0
        val failures = mutableListOf<BatchItemFailure>()

        // Two-phase undo so cyclic swaps reverse cleanly as well!
        val tempItems = mutableListOf<Pair<BatchUndoItemEntity, FileItem>>()
        for ((idx, entry) in items.withIndex()) {
            val currentFileItem = buildFileItemFromUndoEntry(entry)
            if (currentFileItem == null) {
                failures.add(BatchItemFailure(entry.newName, "File no longer exists"))
                continue
            }
            val ext = entry.originalName.substringAfter('.', "")
            val tmpName = ".__fm_tmp_undo_${token}_$idx" + if (ext.isNotBlank()) ".$ext" else ""
            val tmpItem = renameSingle(currentFileItem, tmpName)
            if (tmpItem != null) {
                tempItems.add(entry to tmpItem)
            } else {
                failures.add(BatchItemFailure(entry.newName, "Could not prepare undo"))
            }
        }

        for ((entry, tmpItem) in tempItems) {
            val restored = renameSingle(tmpItem, entry.originalName)
            if (restored != null) {
                success++
            } else {
                failures.add(BatchItemFailure(entry.newName, "Could not restore ${entry.originalName}"))
            }
        }

        batchHistoryDao.markUndone(groupId)
        BatchResultSummary(
            operationType = BatchOperationType.RENAME,
            totalCount = items.size,
            successCount = success,
            failureCount = failures.size,
            failures = failures,
            message = "Restored $success of ${items.size} original filenames"
        )
    }

    suspend fun executeBatchConvert(
        items: List<FileItem>,
        config: ConvertConfig,
        autoOrientExif: Boolean,
        onProgress: (BatchProgress) -> Unit
    ): BatchResultSummary = withContext(Dispatchers.IO) {
        executeImageBatch(
            operationType = BatchOperationType.CONVERT,
            items = items,
            renameConfig = null,
            targetFormat = config.targetFormat,
            quality = config.quality,
            resizeConfig = null,
            preserveOriginals = config.preserveOriginals,
            outputFolder = config.outputFolder,
            autoOrientExif = autoOrientExif,
            onProgress = onProgress
        )
    }

    suspend fun executeBatchResize(
        items: List<FileItem>,
        config: ResizeConfig,
        autoOrientExif: Boolean,
        onProgress: (BatchProgress) -> Unit
    ): BatchResultSummary = withContext(Dispatchers.IO) {
        executeImageBatch(
            operationType = BatchOperationType.RESIZE,
            items = items,
            renameConfig = null,
            targetFormat = if (config.keepOriginalFormat) null else config.outputFormat,
            quality = config.quality,
            resizeConfig = config,
            preserveOriginals = config.preserveOriginals,
            outputFolder = config.outputFolder,
            autoOrientExif = autoOrientExif,
            onProgress = onProgress
        )
    }

    suspend fun executeBatchPipeline(
        items: List<FileItem>,
        config: PipelineConfig,
        autoOrientExif: Boolean,
        onProgress: (BatchProgress) -> Unit
    ): BatchResultSummary = withContext(Dispatchers.IO) {
        executeImageBatch(
            operationType = BatchOperationType.PIPELINE,
            items = items,
            renameConfig = if (config.enableRename) config.renameConfig else null,
            targetFormat = if (config.enableConvert) config.targetFormat else null,
            quality = config.quality,
            resizeConfig = if (config.enableResize) config.resizeConfig else null,
            preserveOriginals = config.preserveOriginals,
            outputFolder = config.outputFolder,
            autoOrientExif = autoOrientExif,
            onProgress = onProgress
        )
    }

    private suspend fun executeImageBatch(
        operationType: BatchOperationType,
        items: List<FileItem>,
        renameConfig: RenameConfig?,
        targetFormat: ImageFormat?,
        quality: Int,
        resizeConfig: ResizeConfig?,
        preserveOriginals: Boolean,
        outputFolder: OutputFolderChoice,
        autoOrientExif: Boolean,
        onProgress: (BatchProgress) -> Unit
    ): BatchResultSummary {
        val total = items.size
        if (total == 0) {
            return BatchResultSummary(operationType, 0, 0, 0, message = "No images selected.")
        }

        val failures = mutableListOf<BatchItemFailure>()
        val outputs = mutableListOf<FileItem>()
        var completed = 0
        var success = 0

        items.forEachIndexed { index, source ->
            currentCoroutineContext().ensureActive()
            onProgress(
                BatchProgress(
                    operationType = operationType,
                    totalCount = total,
                    completedCount = completed,
                    successCount = success,
                    failureCount = failures.size,
                    currentFileName = source.name,
                    isRunning = true
                )
            )

            val resolvedFormat = targetFormat
                ?: ImageFormat.fromExtension(source.extension)
                ?: ImageFormat.JPEG

            val proposedName = if (renameConfig != null) {
                BatchRenameEngine.computeNewFileName(
                    item = source,
                    index = index,
                    config = renameConfig,
                    overrideExtension = resolvedFormat.extension
                )
            } else {
                "${source.nameWithoutExtension}.${resolvedFormat.extension}"
            }

            val destInfo = resolveDestinationFolder(source, outputFolder, operationType)
            val result = processSingleImageToDestination(
                source = source,
                destIsSaf = destInfo.first,
                destPathOrDocId = destInfo.second,
                destSafTreeUri = destInfo.third,
                targetFileName = proposedName,
                targetFormat = resolvedFormat,
                quality = quality,
                resizeConfig = resizeConfig,
                autoOrientExif = autoOrientExif,
                allowOverwriteOriginal = !preserveOriginals
            )

            completed++
            result.fold(
                onSuccess = { outItem ->
                    success++
                    outputs.add(outItem)
                    if (!preserveOriginals && outItem.pathOrUri != source.pathOrUri) {
                        if (source.isSaf) safHelper.deleteDocument(Uri.parse(source.pathOrUri))
                        else File(source.pathOrUri).delete()
                    }
                },
                onFailure = { err ->
                    failures.add(BatchItemFailure(source.name, err.message ?: "Processing failed"))
                }
            )
        }

        onProgress(
            BatchProgress(
                operationType = operationType,
                totalCount = total,
                completedCount = total,
                successCount = success,
                failureCount = failures.size,
                currentFileName = "",
                isRunning = false
            )
        )

        batchHistoryDao.insertGroup(
            BatchUndoGroupEntity(
                operationType = operationType.label,
                summaryText = "Processed $success of $total images",
                timestampMillis = System.currentTimeMillis(),
                isReversible = false
            )
        )

        return BatchResultSummary(
            operationType = operationType,
            totalCount = total,
            successCount = success,
            failureCount = failures.size,
            failures = failures,
            outputFiles = outputs,
            message = "${operationType.label}: $success succeeded, ${failures.size} failed"
        )
    }

    private fun resolveDestinationFolder(
        source: FileItem,
        choice: OutputFolderChoice,
        operationType: BatchOperationType
    ): Triple<Boolean, String, String?> {
        if (choice.isSameAsSource) {
            return if (source.isSaf && source.safTreeUri != null) {
                Triple(true, source.parentId.substringAfterLast("::"), source.safTreeUri)
            } else {
                Triple(false, source.parentId, null)
            }
        }
        if (choice.safTreeUri != null && choice.safDocumentId != null) {
            return Triple(true, choice.safDocumentId, choice.safTreeUri)
        }
        if (choice.localDirPath != null) {
            val dir = File(choice.localDirPath).apply { if (!exists()) mkdirs() }
            return Triple(false, dir.absolutePath, null)
        }
        val defaultSubfolder = when (operationType) {
            BatchOperationType.CONVERT -> "Converted"
            BatchOperationType.RESIZE -> "Resized"
            else -> "Exports"
        }
        val dir = File(workspaceRootDir, defaultSubfolder).apply { if (!exists()) mkdirs() }
        return Triple(false, dir.absolutePath, null)
    }

    private fun processSingleImageToDestination(
        source: FileItem,
        destIsSaf: Boolean,
        destPathOrDocId: String,
        destSafTreeUri: String?,
        targetFileName: String,
        targetFormat: ImageFormat,
        quality: Int,
        resizeConfig: ResizeConfig?,
        autoOrientExif: Boolean,
        allowOverwriteOriginal: Boolean
    ): Result<FileItem> = runCatching {
        if (destIsSaf && destSafTreeUri != null) {
            val treeUri = Uri.parse(destSafTreeUri)
            val safeName = if (allowOverwriteOriginal && targetFileName == source.name &&
                destPathOrDocId == source.parentId.substringAfterLast("::")
            ) {
                targetFileName
            } else {
                resolveUniqueSafName(treeUri, destPathOrDocId, targetFileName)
            }

            // Write to temp cache first, then stream into SAF document so corrupt failures don't leave 0-byte SAF files
            val tempFile = File(context.cacheDir, "proc_${UUID.randomUUID()}.${targetFormat.extension}")
            try {
                FileOutputStream(tempFile).use { fos ->
                    imageEngine.processImageToStream(
                        source = source,
                        outputStream = fos,
                        targetFormat = targetFormat,
                        quality = quality,
                        resizeConfig = resizeConfig,
                        autoOrientExif = autoOrientExif
                    ).getOrThrow()
                }
                val created = safHelper.createFile(treeUri, destPathOrDocId, targetFormat.mimeType, safeName)
                    ?: error("Could not create output file in SAF folder")
                FileInputStream(tempFile).use { fis ->
                    safHelper.openOutputStream(created.uri)?.use { outs ->
                        fis.copyTo(outs, 64 * 1024)
                    } ?: error("Could not write to SAF document")
                }
                created.copy(sizeBytes = tempFile.length())
            } finally {
                tempFile.delete()
            }
        } else {
            val destDir = File(destPathOrDocId).apply { if (!exists()) mkdirs() }
            val destFile = if (allowOverwriteOriginal &&
                File(destDir, targetFileName).absolutePath == source.pathOrUri
            ) {
                File(destDir, targetFileName)
            } else {
                resolveUniqueLocalFile(destDir, targetFileName)
            }

            val tempFile = File(context.cacheDir, "proc_${UUID.randomUUID()}.${targetFormat.extension}")
            try {
                FileOutputStream(tempFile).use { fos ->
                    imageEngine.processImageToStream(
                        source = source,
                        outputStream = fos,
                        targetFormat = targetFormat,
                        quality = quality,
                        resizeConfig = resizeConfig,
                        autoOrientExif = autoOrientExif
                    ).getOrThrow()
                }
                FileInputStream(tempFile).use { fis ->
                    FileOutputStream(destFile).use { fos ->
                        fis.copyTo(fos, 64 * 1024)
                    }
                }
                fileToItem(destFile)
            } finally {
                tempFile.delete()
            }
        }
    }

    private fun buildFileItemFromUndoEntry(entry: BatchUndoItemEntity): FileItem? {
        return if (entry.isSaf && entry.safTreeUri != null) {
            val uri = Uri.parse(entry.newPathOrUri)
            val docId = runCatching { DocumentsContract.getDocumentId(uri) }.getOrNull() ?: return null
            val children = safHelper.listChildren(Uri.parse(entry.safTreeUri), entry.parentPathOrUri)
            children.firstOrNull { it.safDocumentId == docId || it.name == entry.newName }
        } else {
            val file = File(entry.newPathOrUri)
            if (file.exists()) fileToItem(file) else null
        }
    }

    private fun resolveUniqueLocalFile(dir: File, desiredName: String): File {
        var candidate = File(dir, desiredName)
        if (!candidate.exists()) return candidate
        val dot = desiredName.lastIndexOf('.')
        val stem = if (dot > 0) desiredName.substring(0, dot) else desiredName
        val ext = if (dot > 0) desiredName.substring(dot) else ""
        var counter = 1
        while (candidate.exists()) {
            candidate = File(dir, "${stem}_($counter)$ext")
            counter++
        }
        return candidate
    }

    private fun resolveUniqueSafName(treeUri: Uri, parentDocId: String, desiredName: String): String {
        val existing = safHelper.listChildren(treeUri, parentDocId).map { it.name.lowercase() }.toSet()
        if (desiredName.lowercase() !in existing) return desiredName
        val dot = desiredName.lastIndexOf('.')
        val stem = if (dot > 0) desiredName.substring(0, dot) else desiredName
        val ext = if (dot > 0) desiredName.substring(dot) else ""
        var counter = 1
        while ("${stem}_($counter)$ext".lowercase() in existing) {
            counter++
        }
        return "${stem}_($counter)$ext"
    }

    private fun openInputStream(item: FileItem) = if (item.isSaf) {
        safHelper.openInputStream(Uri.parse(item.pathOrUri))
    } else {
        val file = File(item.pathOrUri)
        if (file.exists() && file.canRead()) FileInputStream(file) else null
    }

    private fun openOutputStream(item: FileItem) = if (item.isSaf) {
        safHelper.openOutputStream(Uri.parse(item.pathOrUri))
    } else {
        FileOutputStream(File(item.pathOrUri))
    }

    private fun queryDisplayName(uri: Uri): String? {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) return c.getString(0)
        }
        return uri.lastPathSegment?.substringAfterLast('/')
    }

    fun fileToItem(file: File): FileItem {
        val isDir = file.isDirectory
        val name = file.name
        val ext = if (isDir) "" else file.extension.lowercase()
        val mime = if (isDir) DocumentsContract.Document.MIME_TYPE_DIR else SafDocumentHelper.guessMimeType(name)
        var dirChildren = 0
        var dirBytes = 0L
        if (isDir) {
            val kids = file.listFiles()?.filter { !it.name.startsWith(".__fm_tmp_") }.orEmpty()
            dirChildren = kids.size
            dirBytes = kids.sumOf { if (it.isFile) it.length() else 0L }
        }
        return FileItem(
            id = "local::${file.absolutePath}",
            name = name,
            pathOrUri = file.absolutePath,
            parentId = file.parent ?: workspaceRootDir.absolutePath,
            isDirectory = isDir,
            sizeBytes = if (isDir) dirBytes else file.length(),
            lastModifiedMillis = file.lastModified(),
            mimeType = mime,
            extension = ext,
            category = if (isDir) FileCategory.FOLDER else SafDocumentHelper.classifyCategory(mime, ext),
            isSaf = false,
            childCount = dirChildren
        )
    }

    fun clearThumbnailCache(): Long {
        val cache = context.cacheDir
        var freed = 0L
        cache.listFiles()?.forEach { file ->
            freed += file.walkTopDown().sumOf { if (it.isFile) it.length() else 0L }
            file.deleteRecursively()
        }
        return freed
    }
}
