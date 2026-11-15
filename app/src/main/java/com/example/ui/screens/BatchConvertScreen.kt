package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.ConvertConfig
import com.example.model.FileItem
import com.example.model.ImageFormat
import com.example.model.ImagePreviewComparison
import com.example.ui.components.ConfirmBatchExecuteDialog
import com.example.ui.components.FileThumbnail
import com.example.ui.components.FmCard
import com.example.ui.components.FmIconBadge
import com.example.ui.components.FmPrimaryButton
import com.example.ui.components.FmSectionHeader
import com.example.ui.components.FmSegmentedControl
import com.example.ui.components.FmTopAppBar
import com.example.ui.theme.monoLtr
import com.example.util.FormatUtils
import kotlin.math.roundToInt

@Composable
fun BatchConvertScreen(
    sourceImages: List<FileItem>,
    config: ConvertConfig,
    previewComparison: ImagePreviewComparison?,
    confirmBeforeOverwrite: Boolean,
    onUpdateConfig: ((ConvertConfig) -> ConvertConfig) -> Unit,
    onPickOutputFolder: () -> Unit,
    onImportPhotos: () -> Unit,
    onExecuteConvert: () -> Unit,
    onBack: () -> Unit
) {
    var showOverwriteConfirm by remember { mutableStateOf(false) }

    val totalSize = remember(sourceImages) { sourceImages.sumOf { it.sizeBytes } }
    val sourceFormatSummary = remember(sourceImages) {
        sourceImages.mapNotNull { it.extension.uppercase().takeIf { e -> e.isNotBlank() } }
            .distinct()
            .take(3)
            .joinToString(" · ")
            .ifBlank { "IMAGES" }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        FmTopAppBar(
            title = "تحويل صيغ الصور",
            onNavigationClick = onBack
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Source Summary Card matching 08_converter.png
            item {
                FmCard(cornerRadius = 16.dp, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val first = sourceImages.firstOrNull()
                        if (first != null) {
                            FileThumbnail(item = first, size = 46.dp)
                        } else {
                            FmIconBadge(icon = Icons.Outlined.Folder, size = 46.dp, cornerRadius = 12.dp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${sourceImages.size} صورة محددة",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$sourceFormatSummary · ${FormatUtils.formatBytes(totalSize)} إجمالاً",
                                style = MaterialTheme.typography.bodySmall.monoLtr(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        TextButton(onClick = onImportPhotos) {
                            Text(
                                text = "تغيير",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Target Format Selector matching 08_converter.png
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FmSectionHeader(title = "الصيغة المطلوبة")
                    FmSegmentedControl(
                        options = listOf(
                            ImageFormat.PNG to "PNG",
                            ImageFormat.JPEG to "JPG",
                            ImageFormat.WEBP to "WEBP",
                            ImageFormat.BMP to "BMP"
                        ),
                        selected = config.targetFormat,
                        onSelect = { fmt -> onUpdateConfig { it.copy(targetFormat = fmt) } },
                        tagPrefix = "convert_format"
                    )
                }
            }

            // Quality Slider Card matching 08_converter.png
            if (config.targetFormat.supportsQuality) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FmSectionHeader(title = "الجودة")
                        FmCard(cornerRadius = 16.dp, modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "جودة الصورة",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${config.quality}%",
                                        style = MaterialTheme.typography.titleSmall.monoLtr(),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Slider(
                                    value = config.quality.toFloat(),
                                    onValueChange = { v ->
                                        onUpdateConfig { it.copy(quality = v.roundToInt().coerceIn(10, 100)) }
                                    },
                                    valueRange = 10f..100f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = MaterialTheme.colorScheme.primary,
                                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                                if (previewComparison != null) {
                                    Text(
                                        text = "تقدير الحجم للعينة: ${FormatUtils.formatBytes(previewComparison.originalSizeBytes)} ← ${FormatUtils.formatBytes(previewComparison.estimatedNewSizeBytes)}",
                                        style = MaterialTheme.typography.labelSmall.monoLtr(),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Output Folder Card matching 08_converter.png
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FmSectionHeader(title = "مجلد الحفظ")
                    FmCard(
                        cornerRadius = 16.dp,
                        onClick = onPickOutputFolder,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("convert_output_folder_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FmIconBadge(icon = Icons.Outlined.Folder, size = 44.dp, cornerRadius = 12.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = config.outputFolder.label,
                                    style = MaterialTheme.typography.titleSmall.monoLtr(),
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (config.outputFolder.isSameAsSource) "داخل مجلد الصور الأصلية" else "سيُحفظ في هذا المجلد",
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

            // Preserve Originals Toggle matching 08_converter.png
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FmIconBadge(icon = Icons.Outlined.Lock, size = 44.dp, cornerRadius = 12.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "الاحتفاظ بالملفات الأصلية",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "لن تُحذف الأصول تلقائياً",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = config.preserveOriginals,
                        onCheckedChange = { checked -> onUpdateConfig { it.copy(preserveOriginals = checked) } },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }

        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                FmPrimaryButton(
                    text = "ابدأ التحويل",
                    trailingIcon = Icons.Outlined.SwapHoriz,
                    enabled = sourceImages.isNotEmpty(),
                    onClick = {
                        if (!config.preserveOriginals && confirmBeforeOverwrite) {
                            showOverwriteConfirm = true
                        } else {
                            onExecuteConvert()
                        }
                    },
                    modifier = Modifier.testTag("convert_execute_button")
                )
            }
        }
    }

    if (showOverwriteConfirm) {
        ConfirmBatchExecuteDialog(
            title = "تحويل وحذف الأصول؟",
            message = "تم إيقاف الاحتفاظ بالملفات الأصلية. سيتم حذف الصور الأصلية بعد تحويلها إلى ${config.targetFormat.label}.",
            confirmLabel = "تحويل",
            onConfirm = {
                showOverwriteConfirm = false
                onExecuteConvert()
            },
            onDismiss = { showOverwriteConfirm = false }
        )
    }
}
