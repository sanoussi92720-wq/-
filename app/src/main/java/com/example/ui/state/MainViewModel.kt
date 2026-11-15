package com.example.ui.state

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.FileMasterApp
import com.example.data.engine.BatchRenameEngine
import com.example.data.engine.SafDocumentHelper
import com.example.data.local.BatchUndoGroupEntity
import com.example.data.local.RecentEntryEntity
import com.example.data.local.UserSettings
import com.example.model.BatchOperationType
import com.example.model.BatchProgress
import com.example.model.BatchResultSummary
import com.example.model.BreadcrumbNode
import com.example.model.ConvertConfig
import com.example.model.DEFAULT_RESIZE_PRESETS
import com.example.model.FileCategory
import com.example.model.FileItem
import com.example.model.FolderSizeInfo
import com.example.model.ImageFormat
import com.example.model.ImageFormatCount
import com.example.model.ImagePreviewComparison
import com.example.model.ImageResizeComparison
import com.example.model.OutputFolderChoice
import com.example.model.PipelineConfig
import com.example.model.RenameConfig
import com.example.model.RenameMode
import com.example.model.RenamePreviewItem
import com.example.model.RenameTargetScope
import com.example.model.ResizeConfig
import com.example.model.ResizeMode
import com.example.model.SortDirection
import com.example.model.SortField
import com.example.model.StorageKind
import com.example.model.StorageRoot
import com.example.model.StorageStats
import com.example.model.ViewMode
import com.example.ui.theme.ThemePreference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed interface UiMessage {
    data class Snack(val text: String, val undoGroupId: Long? = null) : UiMessage
}

data class ExplorerState(
    val storageRoots: List<StorageRoot> = emptyList(),
    val activeRoot: StorageRoot? = null,
    val breadcrumbs: List<BreadcrumbNode> = emptyList(),
    val rawFiles: List<FileItem> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<FileItem> = emptyList(),
    val isSearching: Boolean = false,
    val viewMode: ViewMode = ViewMode.LIST,
    val sortField: SortField = SortField.NAME,
    val sortDirection: SortDirection = SortDirection.ASCENDING,
    val selectedIds: Set<String> = emptySet(),
    val categoryFilter: FileCategory? = null
) {
    val currentLocation: BreadcrumbNode? get() = breadcrumbs.lastOrNull()
    val isMultiSelectActive: Boolean get() = selectedIds.isNotEmpty()
    val displayedFiles: List<FileItem>
        get() {
            val base = if (searchQuery.isNotBlank()) searchResults else rawFiles
            val filtered = if (categoryFilter != null) {
                base.filter { it.isDirectory || it.category == categoryFilter }
            } else base
            return sortItems(filtered, sortField, sortDirection)
        }

    companion object {
        fun sortItems(
            items: List<FileItem>,
            field: SortField,
            direction: SortDirection
        ): List<FileItem> {
            val comparator = when (field) {
                SortField.NAME -> compareBy<FileItem> { it.name.lowercase() }
                SortField.SIZE -> compareBy<FileItem> { it.sizeBytes }
                SortField.DATE -> compareBy<FileItem> { it.lastModifiedMillis }
                SortField.TYPE -> compareBy<FileItem> { it.extension.lowercase() }.thenBy { it.name.lowercase() }
            }
            val ordered = if (direction == SortDirection.ASCENDING) {
                items.sortedWith(compareByDescending<FileItem> { it.isDirectory }.then(comparator))
            } else {
                items.sortedWith(compareByDescending<FileItem> { it.isDirectory }.then(comparator.reversed()))
            }
            return ordered
        }
    }
}

data class GalleryState(
    val allImages: List<FileItem> = emptyList(),
    val isLoading: Boolean = false,
    val formatFilter: ImageFormat? = null,
    val folderFilter: String? = null,
    val selectedIds: Set<String> = emptySet()
) {
    val displayedImages: List<FileItem>
        get() = allImages.filter { img ->
            val formatOk = formatFilter == null || ImageFormat.fromExtension(img.extension) == formatFilter
            val folderOk = folderFilter == null || img.parentId == folderFilter
            formatOk && folderOk
        }

    val availableFolders: List<Pair<String, String>>
        get() = allImages.map { item ->
            val label = if (item.isSaf) {
                item.parentId.substringAfterLast("::").substringAfterLast('/')
            } else {
                File(item.parentId).name
            }.ifBlank { "Root" }
            item.parentId to label
        }.distinctBy { it.first }
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as FileMasterApp
    private val repo = app.fileRepository
    private val imageEngine = app.imageEngine
    private val safHelper = app.safHelper
    private val settingsStore = app.settingsDataStore

    val userSettings: StateFlow<UserSettings> = settingsStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserSettings())

    val recentFiles: StateFlow<List<RecentEntryEntity>> = repo.observeRecentFiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentFolders: StateFlow<List<RecentEntryEntity>> = repo.observeRecentFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val batchHistory: StateFlow<List<BatchUndoGroupEntity>> = repo.observeBatchHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _storageStats = MutableStateFlow(StorageStats())
    val storageStats: StateFlow<StorageStats> = _storageStats.asStateFlow()

    private val _explorer = MutableStateFlow(ExplorerState())
    val explorer: StateFlow<ExplorerState> = _explorer.asStateFlow()

    private val _gallery = MutableStateFlow(GalleryState())
    val gallery: StateFlow<GalleryState> = _gallery.asStateFlow()

    val imageFormatCounts: StateFlow<List<ImageFormatCount>> = _gallery.combine(MutableStateFlow(Unit)) { g, _ ->
        ImageFormat.entries.map { fmt ->
            val matching = g.allImages.filter { ImageFormat.fromExtension(it.extension) == fmt }
            ImageFormatCount(fmt.label, matching.size, matching.sumOf { it.sizeBytes })
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _quickFolders = MutableStateFlow<List<FolderSizeInfo>>(emptyList())
    val quickFolders: StateFlow<List<FolderSizeInfo>> = _quickFolders.asStateFlow()

    private val _fileDetailsTarget = MutableStateFlow<FileItem?>(null)
    val fileDetailsTarget: StateFlow<FileItem?> = _fileDetailsTarget.asStateFlow()

    private val _permissionDenied = MutableStateFlow(false)
    val permissionDenied: StateFlow<Boolean> = _permissionDenied.asStateFlow()

    // Full-screen Viewer State
    private val _viewerImages = MutableStateFlow<List<FileItem>>(emptyList())
    val viewerImages: StateFlow<List<FileItem>> = _viewerImages.asStateFlow()

    private val _viewerInitialIndex = MutableStateFlow(0)
    val viewerInitialIndex: StateFlow<Int> = _viewerInitialIndex.asStateFlow()

    private val _viewerDimensionsCache = MutableStateFlow<Map<String, Pair<Int, Int>>>(emptyMap())
    val viewerDimensionsCache: StateFlow<Map<String, Pair<Int, Int>>> = _viewerDimensionsCache.asStateFlow()

    // Batch Rename State
    private val _renameConfig = MutableStateFlow(RenameConfig())
    val renameConfig: StateFlow<RenameConfig> = _renameConfig.asStateFlow()

    private val _renameFolderLabel = MutableStateFlow("Pictures")
    val renameFolderLabel: StateFlow<String> = _renameFolderLabel.asStateFlow()

    private val _renameCandidateFiles = MutableStateFlow<List<FileItem>>(emptyList())
    val renameCandidateFiles: StateFlow<List<FileItem>> = _renameCandidateFiles.asStateFlow()

    private val _renameSelectedFileIds = MutableStateFlow<Set<String>>(emptySet())
    val renameSelectedFileIds: StateFlow<Set<String>> = _renameSelectedFileIds.asStateFlow()

    private val _renameSelectedItems = MutableStateFlow<List<FileItem>>(emptyList())
    private val _renameFolderAllItems = MutableStateFlow<List<FileItem>>(emptyList())
    private val _renameSiblingNames = MutableStateFlow<Map<String, Set<String>>>(emptyMap())

    private val _renamePreviews = MutableStateFlow<List<RenamePreviewItem>>(emptyList())
    val renamePreviews: StateFlow<List<RenamePreviewItem>> = _renamePreviews.asStateFlow()

    // Batch Convert State
    private val _convertConfig = MutableStateFlow(ConvertConfig())
    val convertConfig: StateFlow<ConvertConfig> = _convertConfig.asStateFlow()

    private val _convertSourceItems = MutableStateFlow<List<FileItem>>(emptyList())
    val convertSourceItems: StateFlow<List<FileItem>> = _convertSourceItems.asStateFlow()

    private val _convertPreview = MutableStateFlow<ImagePreviewComparison?>(null)
    val convertPreview: StateFlow<ImagePreviewComparison?> = _convertPreview.asStateFlow()

    // Batch Resize State
    private val _resizeConfig = MutableStateFlow(ResizeConfig())
    val resizeConfig: StateFlow<ResizeConfig> = _resizeConfig.asStateFlow()

    private val _resizeSourceItems = MutableStateFlow<List<FileItem>>(emptyList())
    val resizeSourceItems: StateFlow<List<FileItem>> = _resizeSourceItems.asStateFlow()

    private val _resizeComparisons = MutableStateFlow<List<ImageResizeComparison>>(emptyList())
    val resizeComparisons: StateFlow<List<ImageResizeComparison>> = _resizeComparisons.asStateFlow()

    private val _resizePreview = MutableStateFlow<ImagePreviewComparison?>(null)
    val resizePreview: StateFlow<ImagePreviewComparison?> = _resizePreview.asStateFlow()

    // Batch Pipeline State
    private val _pipelineConfig = MutableStateFlow(PipelineConfig())
    val pipelineConfig: StateFlow<PipelineConfig> = _pipelineConfig.asStateFlow()

    private val _pipelineSourceItems = MutableStateFlow<List<FileItem>>(emptyList())
    val pipelineSourceItems: StateFlow<List<FileItem>> = _pipelineSourceItems.asStateFlow()

    // Active Progress & Results
    private val _batchProgress = MutableStateFlow<BatchProgress?>(null)
    val batchProgress: StateFlow<BatchProgress?> = _batchProgress.asStateFlow()

    private val _batchResult = MutableStateFlow<BatchResultSummary?>(null)
    val batchResult: StateFlow<BatchResultSummary?> = _batchResult.asStateFlow()

    private val _latestBatchResult = MutableStateFlow<BatchResultSummary?>(null)
    val latestBatchResult: StateFlow<BatchResultSummary?> = _latestBatchResult.asStateFlow()

    private val _messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 8)
    val messages: SharedFlow<UiMessage> = _messages.asSharedFlow()

    private var activeBatchJob: Job? = null
    private var searchJob: Job? = null
    private var previewJob: Job? = null

    init {
        viewModelScope.launch {
            val settings = settingsStore.settingsFlow.first()
            _explorer.value = _explorer.value.copy(
                viewMode = settings.defaultViewMode,
                sortField = settings.defaultSortField,
                sortDirection = settings.defaultSortDirection
            )
            _convertConfig.value = _convertConfig.value.copy(
                preserveOriginals = settings.preserveOriginalsByDefault
            )
            _resizeConfig.value = _resizeConfig.value.copy(
                preserveOriginals = settings.preserveOriginalsByDefault
            )
            _pipelineConfig.value = _pipelineConfig.value.copy(
                preserveOriginals = settings.preserveOriginalsByDefault
            )
            refreshAllData()
        }
    }

    fun refreshAllData() {
        viewModelScope.launch {
            val roots = repo.getStorageRoots()
            val currentRoot = _explorer.value.activeRoot?.let { prev ->
                roots.firstOrNull { it.id == prev.id }
            } ?: roots.firstOrNull()

            _explorer.value = _explorer.value.copy(storageRoots = roots, activeRoot = currentRoot)
            if (_explorer.value.breadcrumbs.isEmpty() && currentRoot != null) {
                selectStorageRoot(currentRoot)
            } else {
                refreshCurrentFolder()
            }
            refreshDashboardAndGallery()
        }
    }

    fun refreshDashboardAndGallery() {
        viewModelScope.launch {
            _gallery.value = _gallery.value.copy(isLoading = true)
            val stats = repo.computeStorageStats()
            _storageStats.value = stats
            _quickFolders.value = repo.listQuickFolders()
            val images = repo.listAllImagesAcrossRoots()
            _gallery.value = _gallery.value.copy(allImages = images, isLoading = false)
        }
    }

    fun openFileDetails(item: FileItem) {
        _fileDetailsTarget.value = item
        if (item.isImage) loadDimensionsForItem(item)
    }

    fun refreshFileDetailsIfNeeded(updated: FileItem) {
        if (_fileDetailsTarget.value?.id == updated.id || _fileDetailsTarget.value?.pathOrUri == updated.pathOrUri) {
            _fileDetailsTarget.value = updated
        }
    }

    fun openQuickFolder(folder: FolderSizeInfo) {
        viewModelScope.launch {
            val roots = repo.getStorageRoots()
            val matchingRoot = if (folder.isSaf) {
                roots.firstOrNull { it.treeUri == folder.safTreeUri }
            } else {
                roots.firstOrNull { it.rootPath != null && folder.pathOrUri.startsWith(it.rootPath) }
            }
            val crumb = BreadcrumbNode(
                name = folder.name,
                pathOrDocId = if (folder.isSaf) folder.safDocumentId ?: "" else folder.pathOrUri,
                isSaf = folder.isSaf,
                treeUri = folder.safTreeUri
            )
            _explorer.value = _explorer.value.copy(
                activeRoot = matchingRoot ?: _explorer.value.activeRoot,
                breadcrumbs = listOf(crumb),
                selectedIds = emptySet(),
                searchQuery = ""
            )
            loadDirectory(crumb)
        }
    }

    fun setPermissionDenied(denied: Boolean) {
        _permissionDenied.value = denied
    }

    fun selectStorageRoot(root: StorageRoot) {
        viewModelScope.launch {
            val crumb = if (root.kind == StorageKind.SAF_TREE && root.treeUri != null) {
                val docId = withContext(Dispatchers.IO) {
                    safHelper.getRootDocumentId(Uri.parse(root.treeUri))
                }
                BreadcrumbNode(root.name, docId, isSaf = true, treeUri = root.treeUri)
            } else {
                BreadcrumbNode(root.name, root.rootPath ?: repo.workspaceRootDir.absolutePath, isSaf = false)
            }
            _explorer.value = _explorer.value.copy(
                activeRoot = root,
                breadcrumbs = listOf(crumb),
                selectedIds = emptySet(),
                searchQuery = "",
                searchResults = emptyList(),
                categoryFilter = null
            )
            loadDirectory(crumb)
        }
    }

    fun openFolder(folder: FileItem) {
        if (!folder.isDirectory) return
        viewModelScope.launch {
            repo.recordAccess(folder)
            val crumb = if (folder.isSaf && folder.safTreeUri != null && folder.safDocumentId != null) {
                BreadcrumbNode(folder.name, folder.safDocumentId, isSaf = true, treeUri = folder.safTreeUri)
            } else {
                BreadcrumbNode(folder.name, folder.pathOrUri, isSaf = false)
            }
            val currentCrumbs = _explorer.value.breadcrumbs
            val existingIdx = currentCrumbs.indexOfFirst { it.pathOrDocId == crumb.pathOrDocId && it.isSaf == crumb.isSaf }
            val nextCrumbs = if (existingIdx >= 0) currentCrumbs.take(existingIdx + 1) else currentCrumbs + crumb
            _explorer.value = _explorer.value.copy(
                breadcrumbs = nextCrumbs,
                selectedIds = emptySet(),
                searchQuery = "",
                searchResults = emptyList()
            )
            loadDirectory(crumb)
        }
    }

    fun recentToFileItem(entry: RecentEntryEntity): FileItem {
        val ext = if (entry.isDirectory) "" else entry.name.substringAfterLast('.', "").lowercase()
        return FileItem(
            id = entry.id,
            name = entry.name,
            pathOrUri = entry.pathOrUri,
            parentId = if (entry.isSaf) "saf::${entry.safTreeUri}::" else File(entry.pathOrUri).parent ?: "",
            isDirectory = entry.isDirectory,
            sizeBytes = entry.sizeBytes,
            lastModifiedMillis = entry.lastAccessedMillis,
            mimeType = entry.mimeType,
            extension = ext,
            category = if (entry.isDirectory) FileCategory.FOLDER else SafDocumentHelper.classifyCategory(entry.mimeType, ext),
            isSaf = entry.isSaf,
            safTreeUri = entry.safTreeUri,
            safDocumentId = entry.safDocumentId
        )
    }

    fun openRecentEntry(entry: RecentEntryEntity) {
        viewModelScope.launch {
            if (entry.isDirectory) {
                val roots = repo.getStorageRoots()
                val matchingRoot = if (entry.isSaf) {
                    roots.firstOrNull { it.treeUri == entry.safTreeUri }
                } else {
                    roots.firstOrNull { it.rootPath != null && entry.pathOrUri.startsWith(it.rootPath) }
                }
                val crumb = BreadcrumbNode(
                    name = entry.name,
                    pathOrDocId = if (entry.isSaf) entry.safDocumentId ?: "" else entry.pathOrUri,
                    isSaf = entry.isSaf,
                    treeUri = entry.safTreeUri
                )
                _explorer.value = _explorer.value.copy(
                    activeRoot = matchingRoot ?: _explorer.value.activeRoot,
                    breadcrumbs = listOf(crumb),
                    selectedIds = emptySet(),
                    searchQuery = ""
                )
                loadDirectory(crumb)
            } else {
                val item = recentToFileItem(entry)
                if (item.isImage) {
                    openImageViewer(listOf(item), 0)
                }
            }
        }
    }

    fun navigateToBreadcrumb(index: Int) {
        val crumbs = _explorer.value.breadcrumbs
        if (index !in crumbs.indices) return
        val next = crumbs.take(index + 1)
        _explorer.value = _explorer.value.copy(breadcrumbs = next, selectedIds = emptySet())
        loadDirectory(next.last())
    }

    fun navigateUpFolder(): Boolean {
        val crumbs = _explorer.value.breadcrumbs
        if (crumbs.size <= 1) return false
        navigateToBreadcrumb(crumbs.lastIndex - 1)
        return true
    }

    fun refreshCurrentFolder() {
        val loc = _explorer.value.currentLocation ?: return
        loadDirectory(loc)
    }

    private fun loadDirectory(node: BreadcrumbNode) {
        viewModelScope.launch {
            _explorer.value = _explorer.value.copy(isLoading = true)
            val items = if (node.isSaf && node.treeUri != null) {
                repo.listSafDirectory(node.treeUri, node.pathOrDocId)
            } else {
                repo.listLocalDirectory(node.pathOrDocId)
            }
            _explorer.value = _explorer.value.copy(rawFiles = items, isLoading = false)
        }
    }

    fun setSearchQuery(query: String) {
        _explorer.value = _explorer.value.copy(searchQuery = query)
        searchJob?.cancel()
        if (query.isBlank()) {
            _explorer.value = _explorer.value.copy(searchResults = emptyList(), isSearching = false)
            return
        }
        searchJob = viewModelScope.launch {
            _explorer.value = _explorer.value.copy(isSearching = true)
            val results = repo.searchAcrossRoots(query)
            _explorer.value = _explorer.value.copy(searchResults = results, isSearching = false)
        }
    }

    fun setCategoryFilter(category: FileCategory?) {
        _explorer.value = _explorer.value.copy(categoryFilter = category)
    }

    fun toggleViewMode() {
        val next = if (_explorer.value.viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
        _explorer.value = _explorer.value.copy(viewMode = next)
        viewModelScope.launch { settingsStore.setDefaultViewMode(next) }
    }

    fun setSort(field: SortField, direction: SortDirection) {
        _explorer.value = _explorer.value.copy(sortField = field, sortDirection = direction)
        viewModelScope.launch { settingsStore.setDefaultSort(field, direction) }
    }

    fun toggleFileSelection(item: FileItem) {
        val current = _explorer.value.selectedIds.toMutableSet()
        if (!current.add(item.id)) current.remove(item.id)
        _explorer.value = _explorer.value.copy(selectedIds = current)
    }

    fun selectAllFiles() {
        val allIds = _explorer.value.displayedFiles.map { it.id }.toSet()
        _explorer.value = _explorer.value.copy(selectedIds = allIds)
    }

    fun selectAllImagesInCurrentFolder() {
        val imageIds = _explorer.value.displayedFiles.filter { it.isImage }.map { it.id }.toSet()
        _explorer.value = _explorer.value.copy(selectedIds = imageIds)
    }

    fun deselectAllFiles() {
        _explorer.value = _explorer.value.copy(selectedIds = emptySet())
    }

    fun getSelectedExplorerFiles(): List<FileItem> {
        val ids = _explorer.value.selectedIds
        return _explorer.value.displayedFiles.filter { it.id in ids }
    }

    fun mountSafFolder(treeUri: Uri) {
        viewModelScope.launch {
            val root = repo.mountSafTree(treeUri)
            if (root != null) {
                val roots = repo.getStorageRoots()
                _explorer.value = _explorer.value.copy(storageRoots = roots)
                selectStorageRoot(root)
                refreshDashboardAndGallery()
                _messages.tryEmit(UiMessage.Snack("Mounted folder: ${root.name}"))
            } else {
                _messages.tryEmit(UiMessage.Snack("Could not mount selected folder"))
            }
        }
    }

    fun unmountSafFolder(treeUri: String) {
        viewModelScope.launch {
            repo.unmountSafTree(treeUri)
            val roots = repo.getStorageRoots()
            val fallback = roots.firstOrNull()
            _explorer.value = _explorer.value.copy(storageRoots = roots)
            if (_explorer.value.activeRoot?.treeUri == treeUri && fallback != null) {
                selectStorageRoot(fallback)
            }
            refreshDashboardAndGallery()
            _messages.tryEmit(UiMessage.Snack("Unmounted device folder"))
        }
    }

    fun createFolder(name: String) {
        val loc = _explorer.value.currentLocation ?: return
        viewModelScope.launch {
            val created = repo.createFolder(loc.isSaf, loc.pathOrDocId, loc.treeUri, name)
            if (created != null) {
                refreshCurrentFolder()
                _messages.tryEmit(UiMessage.Snack("Created folder ${created.name}"))
            } else {
                _messages.tryEmit(UiMessage.Snack("Could not create folder"))
            }
        }
    }

    fun createFile(name: String, content: String = "") {
        val loc = _explorer.value.currentLocation ?: return
        viewModelScope.launch {
            val created = repo.createEmptyFile(loc.isSaf, loc.pathOrDocId, loc.treeUri, name, content)
            if (created != null) {
                refreshCurrentFolder()
                refreshDashboardAndGallery()
                _messages.tryEmit(UiMessage.Snack("Created ${created.name}"))
            } else {
                _messages.tryEmit(UiMessage.Snack("Could not create file"))
            }
        }
    }

    fun renameSingleItem(item: FileItem, newName: String) {
        viewModelScope.launch {
            val renamed = repo.renameSingle(item, newName)
            if (renamed != null) {
                if (_fileDetailsTarget.value?.id == item.id) {
                    _fileDetailsTarget.value = renamed
                }
                refreshCurrentFolder()
                refreshDashboardAndGallery()
                _messages.tryEmit(UiMessage.Snack("Renamed to ${renamed.name}"))
            } else {
                _messages.tryEmit(UiMessage.Snack("Rename failed (name may already exist)"))
            }
        }
    }

    fun deleteItems(items: List<FileItem>) {
        if (items.isEmpty()) return
        viewModelScope.launch {
            val (ok, fail) = repo.deleteItems(items)
            val deletedIds = items.map { it.id }.toSet()
            if (_fileDetailsTarget.value?.id in deletedIds) {
                _fileDetailsTarget.value = null
            }
            _explorer.value = _explorer.value.copy(selectedIds = emptySet())
            _gallery.value = _gallery.value.copy(selectedIds = _gallery.value.selectedIds - deletedIds)
            refreshCurrentFolder()
            refreshDashboardAndGallery()
            val msg = if (fail == 0) "Deleted $ok item(s)" else "Deleted $ok, failed $fail"
            _messages.tryEmit(UiMessage.Snack(msg))
        }
    }

    fun copyOrMoveItems(
        items: List<FileItem>,
        destIsSaf: Boolean,
        destPathOrDocId: String,
        destSafTreeUri: String?,
        isMove: Boolean
    ) {
        if (items.isEmpty()) return
        viewModelScope.launch {
            val (ok, fail) = repo.copyOrMoveItems(items, destIsSaf, destPathOrDocId, destSafTreeUri, isMove)
            _explorer.value = _explorer.value.copy(selectedIds = emptySet())
            refreshCurrentFolder()
            refreshDashboardAndGallery()
            val verb = if (isMove) "Moved" else "Copied"
            val msg = if (fail == 0) "$verb $ok item(s)" else "$verb $ok, failed $fail"
            _messages.tryEmit(UiMessage.Snack(msg))
        }
    }

    fun importPickedPhotos(uris: List<Uri>, intoCurrentFolder: Boolean) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val loc = if (intoCurrentFolder) _explorer.value.currentLocation else null
            val destIsSaf = loc?.isSaf ?: false
            val destPath = loc?.pathOrDocId ?: File(repo.workspaceRootDir, "Pictures").apply { mkdirs() }.absolutePath
            val destTree = loc?.treeUri

            val imported = repo.importPickedUris(uris, destIsSaf, destPath, destTree)
            refreshCurrentFolder()
            refreshDashboardAndGallery()
            _messages.tryEmit(UiMessage.Snack("Imported ${imported.size} image(s)"))
        }
    }

    suspend fun getShareUris(items: List<FileItem>): ArrayList<Uri> = repo.getShareUris(items)

    suspend fun listSubfoldersForPicker(
        isSaf: Boolean,
        pathOrDocId: String,
        treeUri: String?
    ): List<FileItem> {
        val items = if (isSaf && treeUri != null) {
            repo.listSafDirectory(treeUri, pathOrDocId)
        } else {
            repo.listLocalDirectory(pathOrDocId)
        }
        return items.filter { it.isDirectory }.sortedBy { it.name.lowercase() }
    }

    // Gallery Controls
    fun setGalleryFormatFilter(format: ImageFormat?) {
        _gallery.value = _gallery.value.copy(formatFilter = format)
    }

    fun setGalleryFolderFilter(folderId: String?) {
        _gallery.value = _gallery.value.copy(folderFilter = folderId)
    }

    fun toggleGallerySelection(item: FileItem) {
        val next = _gallery.value.selectedIds.toMutableSet()
        if (!next.add(item.id)) next.remove(item.id)
        _gallery.value = _gallery.value.copy(selectedIds = next)
    }

    fun selectAllGalleryImages() {
        val ids = _gallery.value.displayedImages.map { it.id }.toSet()
        _gallery.value = _gallery.value.copy(selectedIds = ids)
    }

    fun deselectAllGalleryImages() {
        _gallery.value = _gallery.value.copy(selectedIds = emptySet())
    }

    fun getSelectedGalleryImages(): List<FileItem> {
        val ids = _gallery.value.selectedIds
        return _gallery.value.displayedImages.filter { it.id in ids }
    }

    fun openImageViewer(images: List<FileItem>, initialIndex: Int) {
        if (images.isEmpty()) return
        val safeIdx = initialIndex.coerceIn(0, images.lastIndex)
        _viewerImages.value = images
        _viewerInitialIndex.value = safeIdx
        viewModelScope.launch {
            repo.recordAccess(images[safeIdx])
            loadDimensionsForItem(images[safeIdx])
        }
    }

    fun loadDimensionsForItem(item: FileItem) {
        if (!item.isImage) return
        if (_viewerDimensionsCache.value.containsKey(item.id)) return
        viewModelScope.launch(Dispatchers.IO) {
            val dims = imageEngine.readImageDimensions(item) ?: return@launch
            _viewerDimensionsCache.value = _viewerDimensionsCache.value + (item.id to dims)
        }
    }

    // ==================== BATCH RENAME STUDIO ====================

    fun prepareBatchRename(items: List<FileItem>, folderItems: List<FileItem>? = null) {
        viewModelScope.launch {
            val currentFolderFiles = _explorer.value.rawFiles.filter { !it.isDirectory }
            val fallbackFiles = currentFolderFiles.ifEmpty { _gallery.value.allImages }
            val selected = if (items.isNotEmpty()) items else fallbackFiles
            val folderScope = (folderItems ?: currentFolderFiles).ifEmpty { selected }

            val distinctCandidates = (folderScope + selected).distinctBy { it.id }
            _renameCandidateFiles.value = distinctCandidates
            _renameSelectedItems.value = selected
            _renameSelectedFileIds.value = selected.map { it.id }.toSet()
            _renameFolderAllItems.value = distinctCandidates
            _renameFolderLabel.value = _explorer.value.currentLocation?.name
                ?: distinctCandidates.firstOrNull()?.parentId?.substringAfterLast('/')
                ?: "Pictures"

            _renameSiblingNames.value = repo.getSiblingNamesByParent(distinctCandidates)

            val defaultScope = if (items.isNotEmpty()) RenameTargetScope.SELECTED_ONLY
            else RenameTargetScope.ALL_IN_FOLDER
            _renameConfig.value = _renameConfig.value.copy(scope = defaultScope)
            recomputeRenamePreviews()
        }
    }

    fun setBatchRenameSourceFolder(choice: OutputFolderChoice) {
        viewModelScope.launch {
            val files = if (choice.safTreeUri != null && choice.safDocumentId != null) {
                repo.listSafDirectory(choice.safTreeUri, choice.safDocumentId)
            } else if (choice.localDirPath != null) {
                repo.listLocalDirectory(choice.localDirPath)
            } else {
                emptyList()
            }.filter { !it.isDirectory }

            _renameFolderLabel.value = choice.label
            _renameCandidateFiles.value = files
            _renameFolderAllItems.value = files
            _renameSelectedItems.value = files
            _renameSelectedFileIds.value = files.map { it.id }.toSet()
            _renameSiblingNames.value = repo.getSiblingNamesByParent(files)
            recomputeRenamePreviews()
        }
    }

    fun toggleRenameFileSelection(item: FileItem) {
        val nextIds = _renameSelectedFileIds.value.toMutableSet()
        if (!nextIds.add(item.id)) nextIds.remove(item.id)
        _renameSelectedFileIds.value = nextIds
        _renameSelectedItems.value = _renameCandidateFiles.value.filter { it.id in nextIds }
        recomputeRenamePreviews()
    }

    fun selectAllRenameFiles() {
        val all = _renameCandidateFiles.value
        _renameSelectedFileIds.value = all.map { it.id }.toSet()
        _renameSelectedItems.value = all
        recomputeRenamePreviews()
    }

    fun deselectAllRenameFiles() {
        _renameSelectedFileIds.value = emptySet()
        _renameSelectedItems.value = emptyList()
        recomputeRenamePreviews()
    }

    fun updateRenameConfig(transform: (RenameConfig) -> RenameConfig) {
        _renameConfig.value = transform(_renameConfig.value)
        recomputeRenamePreviews()
    }

    private fun recomputeRenamePreviews() {
        val config = _renameConfig.value
        val activeItems = when (config.scope) {
            RenameTargetScope.SELECTED_ONLY -> _renameSelectedItems.value
            RenameTargetScope.ALL_IN_FOLDER -> _renameFolderAllItems.value
            RenameTargetScope.ALL_IMAGES_IN_FOLDER -> _renameFolderAllItems.value.filter { it.isImage }
        }
        _renamePreviews.value = BatchRenameEngine.generatePreviews(
            items = activeItems,
            config = config,
            existingNamesByParent = _renameSiblingNames.value
        )
    }

    fun autoResolveRenameConflicts() {
        val current = _renameConfig.value
        if (current.mode == RenameMode.MODIFY_ORIGINAL && !current.useSequence) {
            updateRenameConfig { it.copy(useSequence = true, digits = 3, startNumber = 1) }
        } else {
            val stamp = (System.currentTimeMillis() % 1000).toInt()
            updateRenameConfig {
                it.copy(
                    useSequence = true,
                    suffix = if (it.suffix.isEmpty()) "_v$stamp" else "${it.suffix}_$stamp"
                )
            }
        }
    }

    fun executeBatchRename() {
        val previews = _renamePreviews.value
        if (previews.isEmpty() || BatchRenameEngine.hasBlockingConflicts(previews)) return
        activeBatchJob?.cancel()
        activeBatchJob = viewModelScope.launch {
            try {
                val summary = repo.executeBatchRename(previews) { progress ->
                    _batchProgress.value = progress
                }
                _batchProgress.value = null
                _batchResult.value = summary
                _latestBatchResult.value = summary
                _explorer.value = _explorer.value.copy(selectedIds = emptySet())
                _gallery.value = _gallery.value.copy(selectedIds = emptySet())
                refreshCurrentFolder()
                refreshDashboardAndGallery()
                _messages.tryEmit(UiMessage.Snack(summary.message, undoGroupId = summary.undoGroupId))
            } catch (_: CancellationException) {
                _batchProgress.value = null
                _messages.tryEmit(UiMessage.Snack("تم إلغاء إعادة التسمية"))
                refreshCurrentFolder()
            }
        }
    }

    fun undoBatchRename(groupId: Long) {
        viewModelScope.launch {
            val summary = repo.undoBatchRename(groupId)
            refreshCurrentFolder()
            refreshDashboardAndGallery()
            _messages.tryEmit(UiMessage.Snack(summary.message))
        }
    }

    // ==================== BATCH CONVERT STUDIO ====================

    fun prepareBatchConvert(items: List<FileItem>) {
        val images = items.filter { it.isImage }.ifEmpty { _gallery.value.allImages }
        _convertSourceItems.value = images
        val defaultOut = OutputFolderChoice(
            label = "Converted (Workspace)",
            isSameAsSource = false,
            localDirPath = File(repo.workspaceRootDir, "Converted").absolutePath
        )
        _convertConfig.value = _convertConfig.value.copy(outputFolder = defaultOut)
        refreshConvertPreview()
    }

    fun updateConvertConfig(transform: (ConvertConfig) -> ConvertConfig) {
        _convertConfig.value = transform(_convertConfig.value)
        refreshConvertPreview()
    }

    private fun refreshConvertPreview() {
        val sample = _convertSourceItems.value.firstOrNull() ?: run {
            _convertPreview.value = null
            return
        }
        val cfg = _convertConfig.value
        val autoOrient = userSettings.value.autoOrientExif
        previewJob?.cancel()
        previewJob = viewModelScope.launch(Dispatchers.IO) {
            _convertPreview.value = imageEngine.generatePreviewComparison(
                source = sample,
                targetFormat = cfg.targetFormat,
                quality = cfg.quality,
                resizeConfig = null,
                autoOrientExif = autoOrient
            )
        }
    }

    fun executeBatchConvert() {
        val sources = _convertSourceItems.value
        if (sources.isEmpty()) return
        val cfg = _convertConfig.value
        val autoOrient = userSettings.value.autoOrientExif
        activeBatchJob?.cancel()
        activeBatchJob = viewModelScope.launch {
            try {
                val summary = repo.executeBatchConvert(sources, cfg, autoOrient) { progress ->
                    _batchProgress.value = progress
                }
                _batchProgress.value = null
                _batchResult.value = summary
                _latestBatchResult.value = summary
                refreshCurrentFolder()
                refreshDashboardAndGallery()
                _messages.tryEmit(UiMessage.Snack(summary.message))
            } catch (_: CancellationException) {
                _batchProgress.value = null
                _messages.tryEmit(UiMessage.Snack("تم إلغاء التحويل"))
            }
        }
    }

    // ==================== BATCH RESIZE STUDIO ====================

    fun prepareBatchResize(items: List<FileItem>) {
        val images = items.filter { it.isImage }.ifEmpty { _gallery.value.allImages }
        _resizeSourceItems.value = images
        val defaultOut = OutputFolderChoice(
            label = "Resized",
            isSameAsSource = false,
            localDirPath = File(repo.workspaceRootDir, "Resized").absolutePath
        )
        _resizeConfig.value = _resizeConfig.value.copy(outputFolder = defaultOut)
        refreshResizeComparisonsAndPreview()
    }

    fun updateResizeConfig(transform: (ResizeConfig) -> ResizeConfig) {
        _resizeConfig.value = transform(_resizeConfig.value)
        refreshResizeComparisonsAndPreview()
    }

    fun applyResizePreset(presetId: String) {
        val preset = DEFAULT_RESIZE_PRESETS.firstOrNull { it.id == presetId } ?: return
        updateResizeConfig {
            it.copy(
                mode = ResizeMode.PRESET,
                selectedPresetId = preset.id,
                targetWidth = preset.width,
                targetHeight = preset.height
            )
        }
    }

    private fun refreshResizeComparisonsAndPreview() {
        val sources = _resizeSourceItems.value
        val cfg = _resizeConfig.value
        val autoOrient = userSettings.value.autoOrientExif
        previewJob?.cancel()
        previewJob = viewModelScope.launch(Dispatchers.IO) {
            val comps = sources.take(80).mapNotNull { item ->
                val dims = imageEngine.readImageDimensions(item) ?: return@mapNotNull null
                val (tw, th) = imageEngine.computeTargetDimensions(dims.first, dims.second, cfg)
                ImageResizeComparison(
                    sourceName = item.name,
                    originalWidth = dims.first,
                    originalHeight = dims.second,
                    targetWidth = tw,
                    targetHeight = th,
                    originalBytes = item.sizeBytes
                )
            }
            _resizeComparisons.value = comps

            val sample = sources.firstOrNull()
            if (sample != null) {
                val fmt = if (cfg.keepOriginalFormat) {
                    ImageFormat.fromExtension(sample.extension) ?: ImageFormat.JPEG
                } else cfg.outputFormat
                _resizePreview.value = imageEngine.generatePreviewComparison(
                    source = sample,
                    targetFormat = fmt,
                    quality = cfg.quality,
                    resizeConfig = cfg,
                    autoOrientExif = autoOrient
                )
            } else {
                _resizePreview.value = null
            }
        }
    }

    fun executeBatchResize() {
        val sources = _resizeSourceItems.value
        if (sources.isEmpty()) return
        val cfg = _resizeConfig.value
        val autoOrient = userSettings.value.autoOrientExif
        activeBatchJob?.cancel()
        activeBatchJob = viewModelScope.launch {
            try {
                val summary = repo.executeBatchResize(sources, cfg, autoOrient) { progress ->
                    _batchProgress.value = progress
                }
                _batchProgress.value = null
                _batchResult.value = summary
                _latestBatchResult.value = summary
                refreshCurrentFolder()
                refreshDashboardAndGallery()
                _messages.tryEmit(UiMessage.Snack(summary.message))
            } catch (_: CancellationException) {
                _batchProgress.value = null
                _messages.tryEmit(UiMessage.Snack("تم إلغاء تغيير الأحجام"))
            }
        }
    }

    // ==================== BATCH PIPELINE STUDIO ====================

    fun prepareBatchPipeline(items: List<FileItem>) {
        val images = items.filter { it.isImage }.ifEmpty { _gallery.value.allImages }
        _pipelineSourceItems.value = images
        val defaultOut = OutputFolderChoice(
            label = "Exports",
            isSameAsSource = false,
            localDirPath = File(repo.workspaceRootDir, "Exports").absolutePath
        )
        _pipelineConfig.value = _pipelineConfig.value.copy(outputFolder = defaultOut)
    }

    fun updatePipelineConfig(transform: (PipelineConfig) -> PipelineConfig) {
        _pipelineConfig.value = transform(_pipelineConfig.value)
    }

    fun executeBatchPipeline() {
        val sources = _pipelineSourceItems.value
        if (sources.isEmpty()) return
        val cfg = _pipelineConfig.value
        val autoOrient = userSettings.value.autoOrientExif
        activeBatchJob?.cancel()
        activeBatchJob = viewModelScope.launch {
            try {
                val summary = repo.executeBatchPipeline(sources, cfg, autoOrient) { progress ->
                    _batchProgress.value = progress
                }
                _batchProgress.value = null
                _batchResult.value = summary
                _latestBatchResult.value = summary
                refreshCurrentFolder()
                refreshDashboardAndGallery()
                _messages.tryEmit(UiMessage.Snack(summary.message))
            } catch (_: CancellationException) {
                _batchProgress.value = null
                _messages.tryEmit(UiMessage.Snack("تم إلغاء المعالجة"))
            }
        }
    }

    fun cancelActiveBatch() {
        activeBatchJob?.cancel()
        _batchProgress.value = null
    }

    fun dismissBatchResult() {
        _batchResult.value = null
    }

    // ==================== SETTINGS ====================

    fun setThemePreference(pref: ThemePreference) {
        viewModelScope.launch { settingsStore.setThemePreference(pref) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setDynamicColor(enabled) }
    }

    fun setPreserveOriginals(preserve: Boolean) {
        viewModelScope.launch { settingsStore.setPreserveOriginals(preserve) }
    }

    fun setConfirmBeforeOverwrite(confirm: Boolean) {
        viewModelScope.launch { settingsStore.setConfirmBeforeOverwrite(confirm) }
    }

    fun setAutoOrientExif(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setAutoOrientExif(enabled) }
    }

    fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            val freed = repo.clearThumbnailCache()
            _messages.tryEmit(UiMessage.Snack("Cleared cache (${com.example.util.FormatUtils.formatBytes(freed)})"))
        }
    }

    fun emitMessage(text: String) {
        _messages.tryEmit(UiMessage.Snack(text))
    }
}
