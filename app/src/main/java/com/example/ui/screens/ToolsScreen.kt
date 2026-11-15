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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.BatchUndoGroupEntity
import com.example.ui.components.FmCard
import com.example.ui.components.FmIconBadge
import com.example.ui.components.FmTopAppBar
import com.example.ui.theme.extraColors

@Composable
fun ToolsScreen(
    batchHistory: List<BatchUndoGroupEntity>,
    onOpenDrawer: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenBatchRename: () -> Unit,
    onOpenBatchConvert: () -> Unit,
    onOpenBatchResize: () -> Unit,
    onOpenBatchPipeline: () -> Unit,
    onOpenImagesGallery: () -> Unit,
    onOpenOperations: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        FmTopAppBar(
            title = "الأدوات",
            onNavigationClick = onOpenDrawer,
            isMenuIcon = true,
            actions = {
                IconButton(onClick = onOpenSearch, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Outlined.Search, contentDescription = "بحث")
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Featured Card matching 14_tools.png
            item {
                FmCard(
                    containerColor = MaterialTheme.extraColors.heroCardBg,
                    borderColor = MaterialTheme.extraColors.heroCardBorder,
                    cornerRadius = 20.dp,
                    onClick = onOpenBatchRename,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tools_batch_rename")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "إعادة التسمية الجماعية",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "الأداة الأهم · أرقام تسلسلية، بحث واستبدال",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // 2x2 Grid of Square Tool Cards matching 14_tools.png
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ToolGridCard(
                        title = "تحويل الصيغ",
                        subtitle = "PNG · JPG · WEBP · BMP",
                        icon = Icons.Outlined.SwapHoriz,
                        onClick = onOpenBatchConvert,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tools_batch_convert")
                    )
                    ToolGridCard(
                        title = "تغيير الأحجام",
                        subtitle = "نسبة مئوية أو أبعاد محددة",
                        icon = Icons.Outlined.OpenInFull,
                        onClick = onOpenBatchResize,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tools_batch_resize")
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ToolGridCard(
                        title = "المعالجة المتسلسلة",
                        subtitle = "تسمية + تحويل + حجم معاً",
                        icon = Icons.Outlined.AccountTree,
                        onClick = onOpenBatchPipeline,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tools_batch_pipeline")
                    )
                    ToolGridCard(
                        title = "معاينة الصور",
                        subtitle = "عرض سريع ومقارنة الحجم",
                        icon = Icons.Outlined.Visibility,
                        onClick = onOpenImagesGallery,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tools_image_preview")
                    )
                }
            }

            // Compact Operations & Log entry card
            item {
                Spacer(Modifier.height(4.dp))
                FmCard(
                    cornerRadius = 16.dp,
                    onClick = onOpenOperations,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FmIconBadge(icon = Icons.Outlined.History, size = 42.dp, cornerRadius = 12.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "العمليات والسجل",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (batchHistory.isEmpty()) "متابعة التقدم وسجل المعالجات"
                                else "${batchHistory.size} عملية مسجلة · إمكانية التراجع",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
        }
    }
}

@Composable
private fun ToolGridCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FmCard(
        cornerRadius = 18.dp,
        onClick = onClick,
        modifier = modifier.heightIn(min = 152.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            FmIconBadge(icon = icon, size = 46.dp, cornerRadius = 13.dp)
            Spacer(Modifier.height(22.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
