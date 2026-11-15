package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.RecentEntryEntity
import com.example.model.FileItem
import com.example.model.FolderSizeInfo
import com.example.ui.components.FmCard
import com.example.ui.components.FmIconBadge
import com.example.ui.components.FmPrimaryButton
import com.example.ui.components.FmSectionHeader
import com.example.ui.components.FmTopAppBar
import com.example.ui.theme.extraColors
import com.example.ui.theme.ltr
import com.example.ui.theme.monoLtr
import com.example.util.FormatUtils

@Composable
fun HomeScreen(
    recentFolders: List<RecentEntryEntity>,
    quickFolders: List<FolderSizeInfo>,
    galleryImages: List<FileItem>,
    onOpenDrawer: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenBatchRename: () -> Unit,
    onOpenBatchConvert: () -> Unit,
    onOpenBatchResize: () -> Unit,
    onOpenImagesGallery: () -> Unit,
    onOpenBatchPipeline: () -> Unit,
    onOpenRecentFolder: (RecentEntryEntity) -> Unit,
    onOpenQuickFolder: (FolderSizeInfo) -> Unit
) {
    val sampleOriginal = galleryImages.firstOrNull()?.name ?: "photo_001.png"
    val sampleExt = sampleOriginal.substringAfterLast('.', "png")
    val sampleRenamed = "MyTheme_001.$sampleExt"

    Column(modifier = Modifier.fillMaxSize()) {
        FmTopAppBar(
            title = "FileMaster",
            onNavigationClick = onOpenDrawer,
            isMenuIcon = true,
            navigationTestTag = "home_menu_button",
            actions = {
                IconButton(
                    onClick = onOpenSearch,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("home_search_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "بحث",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Featured Hero Card matching 01_home.png & light_home.png
            item {
                FmCard(
                    containerColor = MaterialTheme.extraColors.heroCardBg,
                    borderColor = MaterialTheme.extraColors.heroCardBorder,
                    cornerRadius = 20.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "إعادة التسمية الجماعية",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "أعد تسمية مئات الملفات والصور دفعة واحدة",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Preview pill: origName <- renamedName
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = sampleOriginal,
                                    style = MaterialTheme.typography.labelLarge.monoLtr(),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp)
                                        .size(16.dp)
                                )
                                Text(
                                    text = sampleRenamed,
                                    style = MaterialTheme.typography.labelLarge.monoLtr(),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        FmPrimaryButton(
                            text = "ابدأ الآن",
                            onClick = onOpenBatchRename,
                            modifier = Modifier.testTag("home_hero_batch_rename")
                        )
                    }
                }
            }

            // Quick Tools 2x2 Grid matching 01_home.png
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FmSectionHeader(title = "أدوات سريعة")

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickToolCard(
                            title = "تحويل الصيغ",
                            subtitle = "PNG · JPG · WEBP",
                            icon = Icons.Outlined.SwapHoriz,
                            onClick = onOpenBatchConvert,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_tool_convert")
                        )
                        QuickToolCard(
                            title = "تغيير الأحجام",
                            subtitle = "دفعة واحدة",
                            icon = Icons.Outlined.OpenInFull,
                            onClick = onOpenBatchResize,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_tool_resize")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickToolCard(
                            title = "معالجة متسلسلة",
                            subtitle = "عدة خطوات",
                            icon = Icons.Outlined.AccountTree,
                            onClick = onOpenBatchPipeline,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_tool_pipeline")
                        )
                        QuickToolCard(
                            title = "معرض الصور",
                            subtitle = "معاينة",
                            icon = Icons.Outlined.Image,
                            onClick = onOpenImagesGallery,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_tool_gallery")
                        )
                    }
                }
            }

            // Recent / Workspace Folders section matching 01_home.png
            item {
                FmSectionHeader(title = "المجلدات الأخيرة")
            }

            if (recentFolders.isNotEmpty()) {
                items(recentFolders.take(5), key = { it.id }) { entry ->
                    val matchingQuick = quickFolders.firstOrNull { it.pathOrUri == entry.pathOrUri }
                    val itemCountText = if (matchingQuick != null) "${matchingQuick.itemCount} ملف · " else ""
                    RecentFolderCard(
                        name = entry.name,
                        subtitle = "$itemCountText${FormatUtils.formatRelativeArabicDate(entry.lastAccessedMillis)}",
                        onClick = { onOpenRecentFolder(entry) }
                    )
                }
            } else {
                items(quickFolders.take(5), key = { it.id }) { folder ->
                    RecentFolderCard(
                        name = folder.name,
                        subtitle = "${folder.itemCount} ملف · ${FormatUtils.formatRelativeArabicDate(folder.lastModifiedMillis)}",
                        onClick = { onOpenQuickFolder(folder) }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickToolCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FmCard(
        cornerRadius = 16.dp,
        onClick = onClick,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FmIconBadge(
                icon = icon,
                size = 40.dp,
                cornerRadius = 11.dp
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun RecentFolderCard(
    name: String,
    subtitle: String,
    onClick: () -> Unit
) {
    FmCard(
        cornerRadius = 16.dp,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FmIconBadge(
                icon = Icons.Outlined.Folder,
                size = 44.dp,
                cornerRadius = 12.dp
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium.ltr(),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
