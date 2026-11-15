package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.FolderSizeInfo
import com.example.model.StorageStats
import com.example.ui.components.FmCard
import com.example.ui.components.FmIconBadge
import com.example.ui.components.FmSectionHeader
import com.example.ui.components.FmTopAppBar
import com.example.ui.theme.ltr
import com.example.ui.theme.monoLtr
import com.example.util.FormatUtils

@Composable
fun StorageScreen(
    stats: StorageStats,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onOpenFolder: (FolderSizeInfo) -> Unit
) {
    val total = stats.totalBytes.coerceAtLeast(1L)
    val systemOrOtherUsed = (stats.usedBytes - stats.imageBytes - stats.videoBytes - stats.documentBytes - stats.otherBytes).coerceAtLeast(0L)
    val totalOtherBytes = stats.otherBytes + systemOrOtherUsed

    val imagesDot = MaterialTheme.colorScheme.primary
    val videoDot = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)
    val docsDot = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
    val otherDot = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
    val freeDot = MaterialTheme.colorScheme.surfaceVariant

    val segments = listOf(
        Triple("الصور", stats.imageBytes, imagesDot),
        Triple("الفيديو", stats.videoBytes, videoDot),
        Triple("المستندات", stats.documentBytes, docsDot),
        Triple("أخرى", totalOtherBytes, otherDot),
        Triple("متاحة", stats.freeBytes, freeDot)
    )

    val maxFolderSize = stats.largestFolders.maxOfOrNull { it.sizeBytes }?.coerceAtLeast(1L) ?: 1L

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        FmTopAppBar(
            title = "التخزين",
            onNavigationClick = onBack,
            actions = {
                IconButton(onClick = onRefresh, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "تحديث")
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Headline metric matching 20_storage.png
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = FormatUtils.formatBytes(stats.usedBytes),
                            style = MaterialTheme.typography.displaySmall.ltr(),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "مستخدمة من ${FormatUtils.formatBytes(stats.totalBytes)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 5.dp)
                        )
                    }

                    // Segmented storage bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(7.dp)),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        segments.forEach { (_, bytes, color) ->
                            val weight = (bytes.toFloat() / total.toFloat()).coerceAtLeast(0.02f)
                            Box(
                                modifier = Modifier
                                    .weight(weight)
                                    .fillMaxHeight()
                                    .background(color)
                            )
                        }
                    }
                }
            }

            // Category breakdown list matching 20_storage.png
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    segments.forEachIndexed { idx, (label, bytes, color) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            Text(
                                text = FormatUtils.formatBytes(bytes),
                                style = MaterialTheme.typography.bodyMedium.monoLtr(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (idx < segments.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(4.dp))
                FmSectionHeader(title = "أكبر المجلدات")
            }

            if (stats.largestFolders.isEmpty()) {
                item {
                    FmCard(cornerRadius = 16.dp, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "لا توجد مجلدات تحتوي على ملفات بعد.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(stats.largestFolders, key = { it.id }) { folder ->
                    val fraction = (folder.sizeBytes.toFloat() / maxFolderSize.toFloat()).coerceIn(0.05f, 1f)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        FmCard(
                            cornerRadius = 14.dp,
                            containerColor = Color.Transparent,
                            borderColor = Color.Transparent,
                            onClick = { onOpenFolder(folder) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                FmIconBadge(
                                    icon = Icons.Outlined.Folder,
                                    size = 42.dp,
                                    cornerRadius = 12.dp
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = folder.name,
                                            style = MaterialTheme.typography.titleMedium.ltr(),
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(Modifier.size(8.dp))
                                        Text(
                                            text = FormatUtils.formatBytes(folder.sizeBytes),
                                            style = MaterialTheme.typography.bodyMedium.monoLtr(),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(fraction)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}
