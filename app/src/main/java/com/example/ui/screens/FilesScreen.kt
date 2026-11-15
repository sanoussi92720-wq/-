package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.OpenWith
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.FileCategory
import com.example.model.FileItem
import com.example.model.SortDirection
import com.example.model.SortField
import com.example.model.StorageRoot
import com.example.model.ViewMode
import com.example.ui.components.FileThumbnail
import com.example.ui.components.FmCard
import com.example.ui.components.FmRoundedCheckbox
import com.example.ui.components.FmTopAppBar
import com.example.ui.components.arabicLabel
import com.example.ui.state.ExplorerState
import com.example.ui.theme.extraColors
import com.example.ui.theme.ltr
import com.example.ui.theme.monoLtr
import com.example.util.FormatUtils

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FilesScreen(
    state: ExplorerState,
    initialSearchOpen: Boolean,
    onConsumeInitialSearch: () -> Unit,
    onOpenDrawer: () -> Unit,
    onSelectRoot: (StorageRoot) -> Unit,
    onMountSafFolder: () -> Unit,
    onOpenFolder: (FileItem) -> Unit,
    onNavigateBreadcrumb: (Int) -> Unit,
    onNavigateUp: () -> Boolean,
    onSearchQueryChange: (String) -> Unit,
    onToggleViewMode: () -> Unit,
    onChangeSort: (SortField, SortDirection) -> Unit,
    onCategoryFilterChange: (FileCategory?) -> Unit,
    onToggleSelection: (FileItem) -> Unit,
    onSelectAll: () -> Unit,
    onSelectAllImages: () -> Unit,
    onDeselectAll: () -> Unit,
    onOpenImageViewer: (List<FileItem>, Int) -> Unit,
    onCreateFolderRequest: () -> Unit,
    onCreateFileRequest: () -> Unit,
    onImportPhotos: () -> Unit,
    onRenameSingleRequest: (FileItem) -> Unit,
    onDetailsRequest: (FileItem) -> Unit,
    onCopyRequest: (List<FileItem>) -> Unit,
    onMoveRequest: (List<FileItem>) -> Unit,
    onShareRequest: (List<FileItem>) -> Unit,
    onDeleteRequest: (List<FileItem>) -> Unit,
    onBatchRenameRequest: (List<FileItem>) -> Unit,
    onBatchConvertRequest: (List<FileItem>) -> Unit,
    onBatchResizeRequest: (List<FileItem>) -> Unit,
    onBatchPipelineRequest: (List<FileItem>) -> Unit,
    onOpenStorage: () -> Unit
) {
    var showSearch by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var activeItemMenu by remember { mutableStateOf<FileItem?>(null) }

    LaunchedEffect(initialSearchOpen) {
        if (initialSearchOpen) {
            showSearch = true
            onConsumeInitialSearch()
        }
    }

    BackHandler(enabled = state.isMultiSelectActive || showSearch || state.breadcrumbs.size > 1) {
        when {
            state.isMultiSelectActive -> onDeselectAll()
            showSearch -> {
                showSearch = false
                onSearchQueryChange("")
            }
            else -> onNavigateUp()
        }
    }

    val displayed = state.displayedFiles
    val selectedItems = remember(displayed, state.selectedIds) {
        displayed.filter { it.id in state.selectedIds }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar (Switches cleanly to Selection Bar matching 19_multi_selection.png)
            if (state.isMultiSelectActive) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDeselectAll, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "إلغاء التحديد")
                    }
                    Text(
                        text = "${state.selectedIds.size} محددة",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                    )
                    IconButton(
                        onClick = {
                            if (state.selectedIds.size >= displayed.size && displayed.isNotEmpty()) {
                                onDeselectAll()
                            } else {
                                onSelectAll()
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("files_select_all_toggle")
                    ) {
                        Icon(Icons.Outlined.CheckBox, contentDescription = "تحديد الكل")
                    }
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "خيارات التحديد")
                        }
                        DropdownMenu(expanded = showOverflowMenu, onDismissRequest = { showOverflowMenu = false }) {
                            val selectedImages = selectedItems.filter { it.isImage }
                            if (selectedImages.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("تحويل الصيغ (${selectedImages.size})") },
                                    onClick = { showOverflowMenu = false; onBatchConvertRequest(selectedImages) }
                                )
                                DropdownMenuItem(
                                    text = { Text("تغيير الأحجام (${selectedImages.size})") },
                                    onClick = { showOverflowMenu = false; onBatchResizeRequest(selectedImages) }
                                )
                                DropdownMenuItem(
                                    text = { Text("معالجة متسلسلة (${selectedImages.size})") },
                                    onClick = { showOverflowMenu = false; onBatchPipelineRequest(selectedImages) }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("مشاركة (${selectedItems.size})") },
                                onClick = { showOverflowMenu = false; onShareRequest(selectedItems) }
                            )
                            DropdownMenuItem(
                                text = { Text("تحديد الصور فقط") },
                                onClick = { showOverflowMenu = false; onSelectAllImages() }
                            )
                        }
                    }
                }
            } else {
                FmTopAppBar(
                    title = if (state.viewMode == ViewMode.GRID) "المجلدات" else "الملفات",
                    onNavigationClick = if (state.breadcrumbs.size > 1) {
                        {
                            onNavigateUp()
                            Unit
                        }
                    } else onOpenDrawer,
                    isMenuIcon = state.breadcrumbs.size <= 1,
                    actions = {
                        IconButton(
                            onClick = {
                                showSearch = !showSearch
                                if (!showSearch) onSearchQueryChange("")
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("files_search_toggle")
                        ) {
                            Icon(Icons.Outlined.Search, contentDescription = "بحث")
                        }
                        Box {
                            IconButton(
                                onClick = { showOverflowMenu = true },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("files_overflow_menu")
                            ) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "المزيد")
                            }
                            DropdownMenu(expanded = showOverflowMenu, onDismissRequest = { showOverflowMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("استيراد صور من الجهاز") },
                                    onClick = { showOverflowMenu = false; onImportPhotos() }
                                )
                                DropdownMenuItem(
                                    text = { Text("اختيار مجلد من الجهاز (SAF)") },
                                    onClick = { showOverflowMenu = false; onMountSafFolder() }
                                )
                                DropdownMenuItem(
                                    text = { Text("ملف نصي جديد") },
                                    onClick = { showOverflowMenu = false; onCreateFileRequest() }
                                )
                                DropdownMenuItem(
                                    text = { Text("إعادة التسمية الجماعية للمجلد") },
                                    onClick = {
                                        showOverflowMenu = false
                                        onBatchRenameRequest(displayed.filter { !it.isDirectory })
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("تحديد الكل") },
                                    onClick = { showOverflowMenu = false; onSelectAll() }
                                )
                                DropdownMenuItem(
                                    text = { Text("تحديد الصور فقط") },
                                    onClick = { showOverflowMenu = false; onSelectAllImages() }
                                )
                                DropdownMenuItem(
                                    text = { Text("تحليل التخزين") },
                                    onClick = { showOverflowMenu = false; onOpenStorage() }
                                )
                            }
                        }
                    }
                )
            }

            // Optional Root Selector Strip when SAF/External roots exist
            if (state.storageRoots.size > 1 && !state.isMultiSelectActive) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.storageRoots, key = { it.id }) { root ->
                        val active = root.id == state.activeRoot?.id
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier.clickable { onSelectRoot(root) }
                        ) {
                            Text(
                                text = root.name,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Breadcrumb Path matching 02_files.png ("Internal / Pictures / S18_Theme")
            if (state.breadcrumbs.isNotEmpty() && !state.isMultiSelectActive) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    state.breadcrumbs.forEachIndexed { idx, crumb ->
                        if (idx > 0) {
                            Text(
                                text = " / ",
                                style = MaterialTheme.typography.labelMedium.monoLtr(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val isLast = idx == state.breadcrumbs.lastIndex
                        Text(
                            text = crumb.name,
                            style = MaterialTheme.typography.labelMedium.monoLtr(),
                            fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                            color = if (isLast) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onNavigateBreadcrumb(idx) }
                                .padding(horizontal = 2.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (showSearch) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("ابحث عن ملف أو مجلد...") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.ltr(),
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Filled.Close, contentDescription = "مسح")
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("files_search_input")
                )
            }

            // Sort + Category filter + View Toggle Row matching 02_files.png & 03_folders.png
            if (!state.isMultiSelectActive) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Sort Pill
                    Box {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showSortMenu = true }
                                .testTag("files_sort_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.SwapVert,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = state.sortField.arabicSortLabel(),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                        DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                            SortField.entries.forEach { field ->
                                val selected = state.sortField == field
                                DropdownMenuItem(
                                    text = {
                                        val dirArrow = if (selected) {
                                            if (state.sortDirection == SortDirection.ASCENDING) " ↑" else " ↓"
                                        } else ""
                                        Text(
                                            text = "${field.arabicSortLabel()}$dirArrow",
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        showSortMenu = false
                                        val nextDir = if (selected && state.sortDirection == SortDirection.ASCENDING) {
                                            SortDirection.DESCENDING
                                        } else SortDirection.ASCENDING
                                        onChangeSort(field, nextDir)
                                    }
                                )
                            }
                        }
                    }

                    // Filter Chips
                    val filterOptions: List<Pair<FileCategory?, String>> = listOf(
                        null to "الكل",
                        FileCategory.IMAGE to "الصور",
                        FileCategory.VIDEO to "الفيديو",
                        FileCategory.DOCUMENT to "المستندات",
                        FileCategory.ARCHIVE to "الأرشيف"
                    )
                    LazyRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filterOptions) { (cat, label) ->
                            val active = state.categoryFilter == cat
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                    else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onCategoryFilterChange(cat) }
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    // View Mode Toggle
                    IconButton(
                        onClick = onToggleViewMode,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("files_view_toggle")
                    ) {
                        Icon(
                            imageVector = if (state.viewMode == ViewMode.LIST) Icons.Outlined.GridView
                            else Icons.AutoMirrored.Filled.FormatListBulleted,
                            contentDescription = "تبديل العرض",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (state.isLoading || state.isSearching) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                )
            }

            // Content Area
            if (displayed.isEmpty() && !state.isLoading) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = if (state.searchQuery.isNotBlank()) "لا توجد نتائج مطابقة" else "هذا المجلد فارغ",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "أنشئ مجلداً جديداً أو استورد صوراً من جهازك",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (state.viewMode == ViewMode.LIST) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(
                        start = if (state.isMultiSelectActive) 16.dp else 0.dp,
                        end = if (state.isMultiSelectActive) 16.dp else 0.dp,
                        top = 4.dp,
                        bottom = 88.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(if (state.isMultiSelectActive) 8.dp else 0.dp)
                ) {
                    items(displayed, key = { it.id }) { item ->
                        val isSelected = item.id in state.selectedIds
                        val onTap = {
                            if (state.isMultiSelectActive) {
                                onToggleSelection(item)
                            } else if (item.isDirectory) {
                                onOpenFolder(item)
                            } else if (item.isImage) {
                                val images = displayed.filter { it.isImage }
                                val idx = images.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
                                onOpenImageViewer(images, idx)
                            } else {
                                onDetailsRequest(item)
                            }
                        }

                        if (state.isMultiSelectActive) {
                            // 19_multi_selection.png row style
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) MaterialTheme.extraColors.selectedRowBg else Color.Transparent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .combinedClickable(
                                        onClick = onTap,
                                        onLongClick = { onToggleSelection(item) }
                                    )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    FileThumbnail(item = item, size = 44.dp)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.titleSmall.ltr(),
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val meta = if (item.isDirectory) {
                                            "${item.childCount} ملف · ${FormatUtils.formatBytes(item.sizeBytes)}"
                                        } else {
                                            "${item.extension.uppercase().ifBlank { "FILE" }} · ${FormatUtils.formatBytes(item.sizeBytes)}"
                                        }
                                        Text(
                                            text = meta,
                                            style = MaterialTheme.typography.bodySmall.ltr(),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    FmRoundedCheckbox(
                                        checked = isSelected,
                                        onCheckedChange = { onToggleSelection(item) }
                                    )
                                }
                            }
                        } else {
                            // 02_files.png flat hairline-divided row style
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = onTap,
                                            onLongClick = { onToggleSelection(item) }
                                        )
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    FileThumbnail(item = item, size = 44.dp)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.titleSmall.ltr(),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val meta = if (item.isDirectory) {
                                            "${item.childCount} ملف · ${FormatUtils.formatBytes(item.sizeBytes)}"
                                        } else {
                                            val ext = item.extension.uppercase().ifBlank { item.category.arabicLabel() }
                                            "$ext · ${FormatUtils.formatBytes(item.sizeBytes)}"
                                        }
                                        Text(
                                            text = meta,
                                            style = MaterialTheme.typography.bodySmall.ltr(),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    if (item.isDirectory) {
                                        IconButton(
                                            onClick = { activeItemMenu = item },
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                contentDescription = "فتح أو خيارات",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    } else {
                                        Box {
                                            IconButton(
                                                onClick = { activeItemMenu = item },
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.MoreVert,
                                                    contentDescription = "خيارات الملف",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            FileItemDropdown(
                                                expanded = activeItemMenu?.id == item.id,
                                                item = item,
                                                onDismiss = { activeItemMenu = null },
                                                onDetails = { onDetailsRequest(item) },
                                                onRename = { onRenameSingleRequest(item) },
                                                onCopy = { onCopyRequest(listOf(item)) },
                                                onMove = { onMoveRequest(listOf(item)) },
                                                onShare = { onShareRequest(listOf(item)) },
                                                onConvert = { onBatchConvertRequest(listOf(item)) },
                                                onResize = { onBatchResizeRequest(listOf(item)) },
                                                onDelete = { onDeleteRequest(listOf(item)) }
                                            )
                                        }
                                    }
                                    if (item.isDirectory && activeItemMenu?.id == item.id) {
                                        Box {
                                            FileItemDropdown(
                                                expanded = true,
                                                item = item,
                                                onDismiss = { activeItemMenu = null },
                                                onDetails = { onDetailsRequest(item) },
                                                onRename = { onRenameSingleRequest(item) },
                                                onCopy = { onCopyRequest(listOf(item)) },
                                                onMove = { onMoveRequest(listOf(item)) },
                                                onShare = { onShareRequest(listOf(item)) },
                                                onConvert = {},
                                                onResize = {},
                                                onDelete = { onDeleteRequest(listOf(item)) }
                                            )
                                        }
                                    }
                                }
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                // Grid View matching 03_folders.png
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(displayed, key = { it.id }) { item ->
                        val isSelected = item.id in state.selectedIds
                        FmCard(
                            containerColor = if (isSelected) MaterialTheme.extraColors.heroCardBg else MaterialTheme.colorScheme.surface,
                            borderColor = if (isSelected) MaterialTheme.extraColors.heroCardBorder else MaterialTheme.colorScheme.outlineVariant,
                            cornerRadius = 16.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        if (state.isMultiSelectActive) {
                                            onToggleSelection(item)
                                        } else if (item.isDirectory) {
                                            onOpenFolder(item)
                                        } else if (item.isImage) {
                                            val imgs = displayed.filter { it.isImage }
                                            val idx = imgs.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
                                            onOpenImageViewer(imgs, idx)
                                        } else {
                                            onDetailsRequest(item)
                                        }
                                    },
                                    onLongClick = { onToggleSelection(item) }
                                )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    FileThumbnail(item = item, size = 44.dp)
                                    if (state.isMultiSelectActive) {
                                        FmRoundedCheckbox(
                                            checked = isSelected,
                                            onCheckedChange = { onToggleSelection(item) }
                                        )
                                    } else {
                                        Box {
                                            IconButton(
                                                onClick = { activeItemMenu = item },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.MoreVert,
                                                    contentDescription = "خيارات",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            FileItemDropdown(
                                                expanded = activeItemMenu?.id == item.id,
                                                item = item,
                                                onDismiss = { activeItemMenu = null },
                                                onDetails = { onDetailsRequest(item) },
                                                onRename = { onRenameSingleRequest(item) },
                                                onCopy = { onCopyRequest(listOf(item)) },
                                                onMove = { onMoveRequest(listOf(item)) },
                                                onShare = { onShareRequest(listOf(item)) },
                                                onConvert = { onBatchConvertRequest(listOf(item)) },
                                                onResize = { onBatchResizeRequest(listOf(item)) },
                                                onDelete = { onDeleteRequest(listOf(item)) }
                                            )
                                        }
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.titleMedium.ltr(),
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val sub = if (item.isDirectory) {
                                        "${item.childCount} ملف · ${FormatUtils.formatBytes(item.sizeBytes)}"
                                    } else {
                                        "${item.extension.uppercase().ifBlank { "FILE" }} · ${FormatUtils.formatBytes(item.sizeBytes)}"
                                    }
                                    Text(
                                        text = sub,
                                        style = MaterialTheme.typography.bodySmall.ltr(),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Multi-Selection Action Bar matching 19_multi_selection.png
            if (state.isMultiSelectActive) {
                Surface(
                    color = MaterialTheme.extraColors.navBarBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.navigationBarsPadding()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MultiSelectActionItem(
                                label = "إعادة تسمية",
                                icon = Icons.Outlined.Edit,
                                highlighted = true,
                                destructive = false,
                                onClick = {
                                    if (selectedItems.size == 1 && selectedItems.first().isDirectory) {
                                        onRenameSingleRequest(selectedItems.first())
                                    } else {
                                        onBatchRenameRequest(selectedItems.filter { !it.isDirectory })
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                            MultiSelectActionItem(
                                label = "نسخ",
                                icon = Icons.Outlined.ContentCopy,
                                highlighted = false,
                                destructive = false,
                                onClick = { onCopyRequest(selectedItems) },
                                modifier = Modifier.weight(1f)
                            )
                            MultiSelectActionItem(
                                label = "نقل",
                                icon = Icons.Outlined.OpenWith,
                                highlighted = false,
                                destructive = false,
                                onClick = { onMoveRequest(selectedItems) },
                                modifier = Modifier.weight(1f)
                            )
                            MultiSelectActionItem(
                                label = "حذف",
                                icon = Icons.Outlined.Delete,
                                highlighted = false,
                                destructive = true,
                                onClick = { onDeleteRequest(selectedItems) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // FAB matching 02_files.png & 03_folders.png
        if (!state.isMultiSelectActive) {
            FloatingActionButton(
                onClick = onCreateFolderRequest,
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("files_create_folder_fab")
            ) {
                Icon(Icons.Filled.CreateNewFolder, contentDescription = "مجلد جديد")
            }
        }
    }
}

@Composable
private fun MultiSelectActionItem(
    label: String,
    icon: ImageVector,
    highlighted: Boolean,
    destructive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tint = when {
        destructive -> MaterialTheme.colorScheme.error
        highlighted -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .width(56.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (highlighted) MaterialTheme.colorScheme.primaryContainer else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FileItemDropdown(
    expanded: Boolean,
    item: FileItem,
    onDismiss: () -> Unit,
    onDetails: () -> Unit,
    onRename: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onShare: () -> Unit,
    onConvert: () -> Unit,
    onResize: () -> Unit,
    onDelete: () -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(text = { Text("تفاصيل الملف") }, onClick = { onDismiss(); onDetails() })
        DropdownMenuItem(text = { Text("إعادة تسمية") }, onClick = { onDismiss(); onRename() })
        DropdownMenuItem(text = { Text("نسخ") }, onClick = { onDismiss(); onCopy() })
        DropdownMenuItem(text = { Text("نقل") }, onClick = { onDismiss(); onMove() })
        if (!item.isDirectory) {
            DropdownMenuItem(text = { Text("مشاركة") }, onClick = { onDismiss(); onShare() })
        }
        if (item.isImage) {
            DropdownMenuItem(text = { Text("تحويل الصيغة") }, onClick = { onDismiss(); onConvert() })
            DropdownMenuItem(text = { Text("تغيير الحجم") }, onClick = { onDismiss(); onResize() })
        }
        DropdownMenuItem(
            text = { Text("حذف", color = MaterialTheme.colorScheme.error) },
            onClick = { onDismiss(); onDelete() }
        )
    }
}

private fun SortField.arabicSortLabel(): String = when (this) {
    SortField.NAME -> "الاسم"
    SortField.SIZE -> "الحجم"
    SortField.DATE -> "التاريخ"
    SortField.TYPE -> "النوع"
}
