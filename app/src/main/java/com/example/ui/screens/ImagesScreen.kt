package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.FileItem
import com.example.model.ImageFormat
import com.example.ui.components.FmPrimaryButton
import com.example.ui.components.FmTopAppBar
import com.example.ui.state.GalleryState
import com.example.ui.theme.extraColors
import com.example.ui.theme.ltr

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImagesScreen(
    state: GalleryState,
    onOpenDrawer: () -> Unit,
    onFormatFilterChange: (ImageFormat?) -> Unit,
    onFolderFilterChange: (String?) -> Unit,
    onToggleSelect: (FileItem) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onImportPhotos: () -> Unit,
    onOpenViewer: (List<FileItem>, Int) -> Unit,
    onBatchRename: (List<FileItem>) -> Unit,
    onBatchConvert: (List<FileItem>) -> Unit,
    onBatchResize: (List<FileItem>) -> Unit,
    onBatchPipeline: (List<FileItem>) -> Unit,
    onShare: (List<FileItem>) -> Unit,
    onDelete: (List<FileItem>) -> Unit
) {
    var showSearch by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    var menuExpanded by remember { mutableStateOf(false) }
    var folderMenuExpanded by remember { mutableStateOf(false) }

    val hasSelection = state.selectedIds.isNotEmpty()
    val filteredImages = remember(state.displayedImages, searchText) {
        if (searchText.isBlank()) state.displayedImages
        else state.displayedImages.filter { it.name.contains(searchText.trim(), ignoreCase = true) }
    }
    val selectedImages = remember(filteredImages, state.selectedIds) {
        filteredImages.filter { it.id in state.selectedIds }
    }

    BackHandler(enabled = hasSelection || showSearch) {
        if (hasSelection) onDeselectAll()
        else {
            showSearch = false
            searchText = ""
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (hasSelection) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDeselectAll, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "إلغاء التحديد")
                }
                Text(
                    text = "${selectedImages.size} محددة",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onSelectAll, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Outlined.CheckBox, contentDescription = "تحديد الكل")
                }
                IconButton(onClick = { onShare(selectedImages) }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Outlined.Share, contentDescription = "مشاركة")
                }
                IconButton(onClick = { onDelete(selectedImages) }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Outlined.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                }
            }
        } else {
            FmTopAppBar(
                title = "الصور",
                onNavigationClick = onOpenDrawer,
                isMenuIcon = true,
                actions = {
                    IconButton(
                        onClick = {
                            showSearch = !showSearch
                            if (!showSearch) searchText = ""
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Outlined.Search, contentDescription = "بحث")
                    }
                    Box {
                        IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "المزيد")
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("استيراد صور من الجهاز") },
                                onClick = { menuExpanded = false; onImportPhotos() }
                            )
                            DropdownMenuItem(
                                text = { Text("تحديد الكل") },
                                onClick = { menuExpanded = false; onSelectAll() }
                            )
                            DropdownMenuItem(
                                text = { Text("إعادة التسمية الجماعية") },
                                onClick = { menuExpanded = false; onBatchRename(filteredImages) }
                            )
                            DropdownMenuItem(
                                text = { Text("تحويل الصيغ") },
                                onClick = { menuExpanded = false; onBatchConvert(filteredImages) }
                            )
                            DropdownMenuItem(
                                text = { Text("تغيير الأحجام") },
                                onClick = { menuExpanded = false; onBatchResize(filteredImages) }
                            )
                            DropdownMenuItem(
                                text = { Text("المعالجة المتسلسلة") },
                                onClick = { menuExpanded = false; onBatchPipeline(filteredImages) }
                            )
                        }
                    }
                }
            )
        }

        if (showSearch) {
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = { Text("ابحث بالاسم...") },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.ltr(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        // Format filter chips matching 04_image_gallery.png ("الكل", "PNG", "JPG", "WEBP")
        val formatChips: List<Pair<ImageFormat?, String>> = listOf(
            null to "الكل",
            ImageFormat.PNG to "PNG",
            ImageFormat.JPEG to "JPG",
            ImageFormat.WEBP to "WEBP",
            ImageFormat.BMP to "BMP"
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(formatChips) { (fmt, label) ->
                val active = state.formatFilter == fmt
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    border = BorderStroke(
                        1.dp,
                        if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                        else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onFormatFilterChange(fmt) }
                        .testTag("gallery_chip_${fmt?.name?.lowercase() ?: "all"}")
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Folder + Count subheader matching 04_image_gallery.png ("S18_Theme"  "200 صورة")
        val folderName = state.availableFolders.firstOrNull { it.first == state.folderFilter }?.second ?: "كل المجلدات"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Text(
                    text = folderName,
                    style = MaterialTheme.typography.titleMedium.ltr(),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { folderMenuExpanded = true }
                        .padding(vertical = 2.dp)
                )
                DropdownMenu(expanded = folderMenuExpanded, onDismissRequest = { folderMenuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("كل المجلدات") },
                        onClick = { folderMenuExpanded = false; onFolderFilterChange(null) }
                    )
                    state.availableFolders.forEach { (id, label) ->
                        DropdownMenuItem(
                            text = { Text(label, style = MaterialTheme.typography.bodyMedium.ltr()) },
                            onClick = { folderMenuExpanded = false; onFolderFilterChange(id) }
                        )
                    }
                }
            }
            Text(
                text = "${filteredImages.size} صورة",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (filteredImages.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(52.dp)
                    )
                    Text(
                        text = "لا توجد صور بعد",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "استورد صوراً من معرض جهازك أو اختر مجلداً للبدء",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FmPrimaryButton(
                        text = "استيراد صور",
                        leadingIcon = Icons.Outlined.AddPhotoAlternate,
                        onClick = onImportPhotos,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(filteredImages, key = { _, item -> item.id }) { idx, img ->
                    val selected = img.id in state.selectedIds
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .then(
                                if (selected) Modifier.border(
                                    2.5.dp,
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(14.dp)
                                ) else Modifier
                            )
                            .combinedClickable(
                                onClick = {
                                    if (hasSelection) onToggleSelect(img)
                                    else onOpenViewer(filteredImages, idx)
                                },
                                onLongClick = { onToggleSelect(img) }
                            )
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(img.uri)
                                .size(300)
                                .crossfade(true)
                                .build(),
                            contentDescription = img.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (selected) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "محددة",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Batch Actions Bar when images are selected
        if (hasSelection) {
            Surface(color = MaterialTheme.extraColors.navBarBg, modifier = Modifier.fillMaxWidth()) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        GalleryBatchAction("تسمية", Icons.Outlined.Edit, { onBatchRename(selectedImages) }, Modifier.weight(1f))
                        GalleryBatchAction("تحويل", Icons.Outlined.SwapHoriz, { onBatchConvert(selectedImages) }, Modifier.weight(1f))
                        GalleryBatchAction("حجم", Icons.Outlined.OpenInFull, { onBatchResize(selectedImages) }, Modifier.weight(1f))
                        GalleryBatchAction("متسلسلة", Icons.Outlined.AccountTree, { onBatchPipeline(selectedImages) }, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryBatchAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp)
    ) {
        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
