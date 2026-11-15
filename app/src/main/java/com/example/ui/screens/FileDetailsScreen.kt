package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.OpenWith
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.FileCategory
import com.example.model.FileItem
import com.example.ui.components.FmCard
import com.example.ui.components.FmTopAppBar
import com.example.ui.components.arabicLabel
import com.example.ui.theme.ltr
import com.example.ui.theme.monoLtr
import com.example.util.FormatUtils

@Composable
fun FileDetailsScreen(
    item: FileItem?,
    dimensions: Pair<Int, Int>?,
    onBack: () -> Unit,
    onRename: (FileItem) -> Unit,
    onCopy: (FileItem) -> Unit,
    onMove: (FileItem) -> Unit,
    onDelete: (FileItem) -> Unit,
    onShare: (FileItem) -> Unit,
    onConvert: (FileItem) -> Unit,
    onResize: (FileItem) -> Unit
) {
    if (item == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            FmTopAppBar(title = "تفاصيل الملف", onNavigationClick = onBack)
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("لم يتم اختيار ملف", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    var menuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        FmTopAppBar(
            title = "تفاصيل الملف",
            onNavigationClick = onBack,
            actions = {
                Box {
                    IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "خيارات")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("مشاركة") },
                            onClick = { menuExpanded = false; onShare(item) }
                        )
                        if (item.isImage) {
                            DropdownMenuItem(
                                text = { Text("تحويل الصيغة") },
                                onClick = { menuExpanded = false; onConvert(item) }
                            )
                            DropdownMenuItem(
                                text = { Text("تغيير الحجم") },
                                onClick = { menuExpanded = false; onResize(item) }
                            )
                        }
                    }
                }
            }
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Large Preview Banner matching 11_file_details.png
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.isImage && !item.isDirectory) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(item.uri)
                                .crossfade(true)
                                .build(),
                            contentDescription = item.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        val fallbackIcon = when (item.category) {
                            FileCategory.FOLDER -> Icons.Outlined.Folder
                            FileCategory.VIDEO -> Icons.Outlined.VideoFile
                            FileCategory.AUDIO -> Icons.Outlined.AudioFile
                            FileCategory.DOCUMENT -> Icons.Outlined.Description
                            FileCategory.ARCHIVE -> Icons.Outlined.FolderZip
                            else -> Icons.AutoMirrored.Filled.InsertDriveFile
                        }
                        Icon(
                            imageVector = fallbackIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }
            }

            // Title & size subtitle
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.headlineSmall.ltr(),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    val extText = item.extension.uppercase().takeIf { it.isNotBlank() }?.let { " $it" } ?: ""
                    Text(
                        text = "${item.category.arabicLabel()}$extText · ${FormatUtils.formatBytes(item.sizeBytes)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Metadata grouped card matching 11_file_details.png
            item {
                FmCard(cornerRadius = 18.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        val pathDisplay = if (item.isSaf) "SAF / ${item.name}" else item.parentId
                        DetailInfoRow(label = "المسار", value = pathDisplay, mono = true)
                        if (dimensions != null) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            DetailInfoRow(label = "الأبعاد", value = "${dimensions.first} × ${dimensions.second} px", mono = true)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        DetailInfoRow(label = "الحجم", value = FormatUtils.formatBytes(item.sizeBytes), mono = true)
                        if (!item.isDirectory) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            DetailInfoRow(
                                label = "الصيغة",
                                value = item.extension.uppercase().ifBlank { item.mimeType },
                                mono = true
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        DetailInfoRow(
                            label = "آخر تعديل",
                            value = FormatUtils.formatDateTime(item.lastModifiedMillis),
                            mono = true
                        )
                    }
                }
            }
        }

        // Bottom 4 square action tiles matching 11_file_details.png
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DetailActionTile(
                label = "إعادة تسمية",
                icon = Icons.Outlined.Edit,
                isDestructive = false,
                onClick = { onRename(item) },
                modifier = Modifier.weight(1f)
            )
            DetailActionTile(
                label = "نسخ",
                icon = Icons.Outlined.ContentCopy,
                isDestructive = false,
                onClick = { onCopy(item) },
                modifier = Modifier.weight(1f)
            )
            DetailActionTile(
                label = "نقل",
                icon = Icons.Outlined.OpenWith,
                isDestructive = false,
                onClick = { onMove(item) },
                modifier = Modifier.weight(1f)
            )
            DetailActionTile(
                label = "حذف",
                icon = Icons.Outlined.Delete,
                isDestructive = true,
                onClick = { onDelete(item) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DetailInfoRow(
    label: String,
    value: String,
    mono: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.size(16.dp))
        Text(
            text = value,
            style = if (mono) MaterialTheme.typography.bodyMedium.monoLtr() else MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DetailActionTile(
    label: String,
    icon: ImageVector,
    isDestructive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fg = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    FmCard(
        cornerRadius = 16.dp,
        onClick = onClick,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = fg,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
