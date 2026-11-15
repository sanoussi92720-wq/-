package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.DEFAULT_RESIZE_PRESETS
import com.example.model.FileItem
import com.example.model.ImagePreviewComparison
import com.example.model.ImageResizeComparison
import com.example.model.ResizeConfig
import com.example.model.ResizeMode
import com.example.ui.components.ConfirmBatchExecuteDialog
import com.example.ui.components.FmCard
import com.example.ui.components.FmIconBadge
import com.example.ui.components.FmPrimaryButton
import com.example.ui.components.FmSectionHeader
import com.example.ui.components.FmSegmentedControl
import com.example.ui.components.FmTopAppBar
import com.example.ui.theme.extraColors
import com.example.ui.theme.monoLtr
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BatchResizeScreen(
    sourceImages: List<FileItem>,
    config: ResizeConfig,
    comparisons: List<ImageResizeComparison>,
    previewComparison: ImagePreviewComparison?,
    confirmBeforeOverwrite: Boolean,
    onUpdateConfig: ((ResizeConfig) -> ResizeConfig) -> Unit,
    onApplyPreset: (String) -> Unit,
    onPickOutputFolder: () -> Unit,
    onImportPhotos: () -> Unit,
    onExecuteResize: () -> Unit,
    onBack: () -> Unit
) {
    var showOverwriteConfirm by remember { mutableStateOf(false) }

    val selectedTab = when (config.mode) {
        ResizeMode.PERCENTAGE -> ResizeMode.PERCENTAGE
        ResizeMode.PRESET -> ResizeMode.PRESET
        else -> ResizeMode.CUSTOM_DIMENSIONS
    }

    val firstComp = comparisons.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        FmTopAppBar(
            title = "تغيير أحجام الصور",
            onNavigationClick = onBack,
            actions = {
                TextButton(onClick = onImportPhotos) {
                    Text("${sourceImages.size} صورة", style = MaterialTheme.typography.labelLarge)
                }
            }
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Segmented Mode Bar matching 09_resizer.png
            item {
                FmSegmentedControl(
                    options = listOf(
                        ResizeMode.CUSTOM_DIMENSIONS to "بالأبعاد",
                        ResizeMode.PERCENTAGE to "بالنسبة %",
                        ResizeMode.PRESET to "أحجام جاهزة"
                    ),
                    selected = selectedTab,
                    onSelect = { mode -> onUpdateConfig { it.copy(mode = mode) } },
                    tagPrefix = "resize_mode"
                )
            }

            if (selectedTab == ResizeMode.PERCENTAGE) {
                item {
                    FmCard(cornerRadius = 16.dp, modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("نسبة التصغير / التكبير", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    "${config.percentage}%",
                                    style = MaterialTheme.typography.titleSmall.monoLtr(),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Slider(
                                value = config.percentage.toFloat(),
                                onValueChange = { v ->
                                    onUpdateConfig { it.copy(percentage = v.roundToInt().coerceIn(10, 200)) }
                                },
                                valueRange = 10f..200f,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            } else {
                // Width x Link x Height Cards matching 09_resizer.png
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "العرض (px)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = config.targetWidth.toString(),
                                onValueChange = { raw ->
                                    val w = raw.filter { it.isDigit() }.toIntOrNull() ?: 0
                                    onUpdateConfig { cfg ->
                                        val nextH = if (cfg.maintainAspectRatio && firstComp != null && firstComp.originalWidth > 0 && w > 0) {
                                            ((w.toFloat() / firstComp.originalWidth.toFloat()) * firstComp.originalHeight).roundToInt().coerceAtLeast(1)
                                        } else cfg.targetHeight
                                        cfg.copy(
                                            mode = ResizeMode.CUSTOM_DIMENSIONS,
                                            targetWidth = w,
                                            targetHeight = nextH
                                        )
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = MaterialTheme.typography.titleMedium.monoLtr(),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Box(
                            modifier = Modifier
                                .padding(top = 20.dp)
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.extraColors.iconBadgeBg)
                                .clickable {
                                    onUpdateConfig { it.copy(maintainAspectRatio = !it.maintainAspectRatio) }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (config.maintainAspectRatio) Icons.Outlined.Link else Icons.Outlined.LinkOff,
                                contentDescription = "ربط الأبعاد",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "الارتفاع (px)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = config.targetHeight.toString(),
                                onValueChange = { raw ->
                                    val h = raw.filter { it.isDigit() }.toIntOrNull() ?: 0
                                    onUpdateConfig { cfg ->
                                        val nextW = if (cfg.maintainAspectRatio && firstComp != null && firstComp.originalHeight > 0 && h > 0) {
                                            ((h.toFloat() / firstComp.originalHeight.toFloat()) * firstComp.originalWidth).roundToInt().coerceAtLeast(1)
                                        } else cfg.targetWidth
                                        cfg.copy(
                                            mode = ResizeMode.CUSTOM_DIMENSIONS,
                                            targetWidth = nextW,
                                            targetHeight = h
                                        )
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = MaterialTheme.typography.titleMedium.monoLtr(),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Aspect Ratio Toggle Row matching 09_resizer.png
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FmIconBadge(icon = Icons.Outlined.Link, size = 44.dp, cornerRadius = 12.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "الحفاظ على نسبة الأبعاد",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "يُحسب الارتفاع تلقائياً",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = config.maintainAspectRatio,
                            onCheckedChange = { checked -> onUpdateConfig { it.copy(maintainAspectRatio = checked) } },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                // Preset Size Chips matching 09_resizer.png
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        FmSectionHeader(title = "أحجام جاهزة")
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DEFAULT_RESIZE_PRESETS.forEach { preset ->
                                val active = config.mode == ResizeMode.PRESET && config.selectedPresetId == preset.id
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(
                                        1.dp,
                                        if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                        else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .clickable { onApplyPreset(preset.id) }
                                ) {
                                    Text(
                                        text = "${preset.width}×${preset.height}",
                                        style = MaterialTheme.typography.labelLarge.monoLtr(),
                                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Comparison Card matching 09_resizer.png ("الأصلي 4032×3024 <- الجديد 1080×810")
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FmSectionHeader(title = "المقارنة")
                    FmCard(cornerRadius = 16.dp, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val origText = when {
                                previewComparison != null -> "${previewComparison.originalWidth}×${previewComparison.originalHeight}"
                                firstComp != null -> "${firstComp.originalWidth}×${firstComp.originalHeight}"
                                else -> "—"
                            }
                            val newText = when {
                                previewComparison != null -> "${previewComparison.newWidth}×${previewComparison.newHeight}"
                                firstComp != null -> "${firstComp.targetWidth}×${firstComp.targetHeight}"
                                else -> "${config.targetWidth}×${config.targetHeight}"
                            }
                            Column {
                                Text("الأصلي", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(origText, style = MaterialTheme.typography.titleMedium.monoLtr(), fontWeight = FontWeight.Bold)
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Column(horizontalAlignment = Alignment.End) {
                                Text("الجديد", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    newText,
                                    style = MaterialTheme.typography.titleMedium.monoLtr(),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Compact Output Folder Card
            item {
                FmCard(
                    cornerRadius = 16.dp,
                    onClick = onPickOutputFolder,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FmIconBadge(icon = Icons.Outlined.Folder, size = 40.dp, cornerRadius = 11.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("مجلد الحفظ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = config.outputFolder.label,
                                style = MaterialTheme.typography.titleSmall.monoLtr(),
                                fontWeight = FontWeight.Bold,
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
        }

        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                FmPrimaryButton(
                    text = "تغيير الأحجام",
                    trailingIcon = Icons.Outlined.OpenInFull,
                    enabled = sourceImages.isNotEmpty(),
                    onClick = {
                        if (!config.preserveOriginals && confirmBeforeOverwrite) {
                            showOverwriteConfirm = true
                        } else {
                            onExecuteResize()
                        }
                    },
                    modifier = Modifier.testTag("resize_execute_button")
                )
            }
        }
    }

    if (showOverwriteConfirm) {
        ConfirmBatchExecuteDialog(
            title = "تأكيد تغيير الأحجام",
            message = "تم إيقاف الاحتفاظ بالملفات الأصلية. هل تريد المتابعة واستبدال الصور؟",
            confirmLabel = "تغيير الأحجام",
            onConfirm = {
                showOverwriteConfirm = false
                onExecuteResize()
            },
            onDismiss = { showOverwriteConfirm = false }
        )
    }
}
