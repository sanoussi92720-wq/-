package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.model.FileItem
import com.example.model.ImageFormat
import com.example.model.PipelineConfig
import com.example.ui.components.FmCard
import com.example.ui.components.FmIconBadge
import com.example.ui.components.FmPrimaryButton
import com.example.ui.components.FmSectionHeader
import com.example.ui.components.FmSegmentedControl
import com.example.ui.components.FmTopAppBar
import com.example.ui.theme.extraColors
import com.example.ui.theme.monoLtr
import kotlin.math.roundToInt

@Composable
fun BatchPipelineScreen(
    sourceImages: List<FileItem>,
    config: PipelineConfig,
    onUpdateConfig: ((PipelineConfig) -> PipelineConfig) -> Unit,
    onPickOutputFolder: () -> Unit,
    onImportPhotos: () -> Unit,
    onExecutePipeline: () -> Unit,
    onBack: () -> Unit
) {
    var expandedStep by remember { mutableIntStateOf(-1) }
    var menuExpanded by remember { mutableStateOf(false) }
    var addStepMenuExpanded by remember { mutableStateOf(false) }

    val sourceFolderText = remember(sourceImages) {
        val first = sourceImages.firstOrNull()
        when {
            first == null -> "Pictures"
            first.isSaf -> "SAF / ${first.name}"
            else -> first.parentId.substringAfterLast('/')
        }
    }

    val activeStepsCount = listOf(config.enableRename, config.enableConvert, config.enableResize).count { it }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        FmTopAppBar(
            title = "المعالجة المتسلسلة",
            onNavigationClick = onBack,
            actions = {
                Box {
                    IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "خيارات")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("اختيار صور أخرى") },
                            onClick = { menuExpanded = false; onImportPhotos() }
                        )
                        DropdownMenuItem(
                            text = { Text("إعادة ضبط الخطوات") },
                            onClick = { menuExpanded = false; onUpdateConfig { PipelineConfig() } }
                        )
                    }
                }
            }
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Source Folder Card matching 12_pipeline.png
            item {
                FmCard(cornerRadius = 16.dp, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FmIconBadge(icon = Icons.Outlined.Folder, size = 44.dp, cornerRadius = 12.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${sourceImages.size} صورة",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = sourceFolderText,
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

            item {
                FmSectionHeader(title = "الخطوات")
            }

            // Numbered Step Cards matching 12_pipeline.png
            item {
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    var stepNumber = 1

                    if (config.enableRename) {
                        val idx = stepNumber++
                        PipelineStepCard(
                            stepNumber = idx,
                            title = "إعادة التسمية",
                            summary = "${config.renameConfig.customBaseName}${"#".repeat(config.renameConfig.digits)} · البداية ${config.renameConfig.startNumber}",
                            expanded = expandedStep == 1,
                            onClick = { expandedStep = if (expandedStep == 1) -1 else 1 },
                            trailingSwitchChecked = config.enableRename,
                            onToggle = { onUpdateConfig { it.copy(enableRename = false) } }
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = config.renameConfig.customBaseName,
                                    onValueChange = { base ->
                                        onUpdateConfig { it.copy(renameConfig = it.renameConfig.copy(customBaseName = base)) }
                                    },
                                    label = { Text("الاسم الموحد") },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium.monoLtr(),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = config.renameConfig.startNumber.toString(),
                                        onValueChange = { raw ->
                                            val n = raw.filter { it.isDigit() }.toIntOrNull() ?: 1
                                            onUpdateConfig { it.copy(renameConfig = it.renameConfig.copy(startNumber = n)) }
                                        },
                                        label = { Text("البداية") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyMedium.monoLtr(),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = config.renameConfig.digits.toString(),
                                        onValueChange = { raw ->
                                            val d = (raw.filter { it.isDigit() }.toIntOrNull() ?: 3).coerceIn(1, 6)
                                            onUpdateConfig { it.copy(renameConfig = it.renameConfig.copy(digits = d)) }
                                        },
                                        label = { Text("الخانات") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyMedium.monoLtr(),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    if (config.enableConvert) {
                        if (stepNumber > 1) StepConnector()
                        val idx = stepNumber++
                        PipelineStepCard(
                            stepNumber = idx,
                            title = "تحويل الصيغة",
                            summary = "إلى ${config.targetFormat.name} · جودة ${config.quality}%",
                            expanded = expandedStep == 2,
                            onClick = { expandedStep = if (expandedStep == 2) -1 else 2 },
                            trailingSwitchChecked = config.enableConvert,
                            onToggle = { onUpdateConfig { it.copy(enableConvert = false) } }
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                FmSegmentedControl(
                                    options = listOf(
                                        ImageFormat.PNG to "PNG",
                                        ImageFormat.JPEG to "JPG",
                                        ImageFormat.WEBP to "WEBP",
                                        ImageFormat.BMP to "BMP"
                                    ),
                                    selected = config.targetFormat,
                                    onSelect = { fmt -> onUpdateConfig { it.copy(targetFormat = fmt) } },
                                    tagPrefix = "pipe_fmt"
                                )
                                if (config.targetFormat.supportsQuality) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("الجودة", style = MaterialTheme.typography.labelMedium)
                                        Text("${config.quality}%", style = MaterialTheme.typography.labelMedium.monoLtr())
                                    }
                                    Slider(
                                        value = config.quality.toFloat(),
                                        onValueChange = { v ->
                                            onUpdateConfig { it.copy(quality = v.roundToInt().coerceIn(10, 100)) }
                                        },
                                        valueRange = 10f..100f
                                    )
                                }
                            }
                        }
                    }

                    if (config.enableResize) {
                        if (stepNumber > 1) StepConnector()
                        val idx = stepNumber
                        PipelineStepCard(
                            stepNumber = idx,
                            title = "تغيير الحجم",
                            summary = "${config.resizeConfig.targetWidth}×${config.resizeConfig.targetHeight} px",
                            expanded = expandedStep == 3,
                            onClick = { expandedStep = if (expandedStep == 3) -1 else 3 },
                            trailingSwitchChecked = config.enableResize,
                            onToggle = { onUpdateConfig { it.copy(enableResize = false) } }
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = config.resizeConfig.targetWidth.toString(),
                                    onValueChange = { raw ->
                                        val w = raw.filter { it.isDigit() }.toIntOrNull() ?: 1080
                                        onUpdateConfig { it.copy(resizeConfig = it.resizeConfig.copy(targetWidth = w)) }
                                    },
                                    label = { Text("العرض (px)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium.monoLtr(),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = config.resizeConfig.targetHeight.toString(),
                                    onValueChange = { raw ->
                                        val h = raw.filter { it.isDigit() }.toIntOrNull() ?: 1080
                                        onUpdateConfig { it.copy(resizeConfig = it.resizeConfig.copy(targetHeight = h)) }
                                    },
                                    label = { Text("الارتفاع (px)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium.monoLtr(),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    if (activeStepsCount < 3) {
                        Spacer(Modifier.height(10.dp))
                        Box {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { addStepMenuExpanded = true }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 14.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "إضافة خطوة",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            DropdownMenu(expanded = addStepMenuExpanded, onDismissRequest = { addStepMenuExpanded = false }) {
                                if (!config.enableRename) {
                                    DropdownMenuItem(
                                        text = { Text("إعادة التسمية") },
                                        onClick = { addStepMenuExpanded = false; onUpdateConfig { it.copy(enableRename = true) } }
                                    )
                                }
                                if (!config.enableConvert) {
                                    DropdownMenuItem(
                                        text = { Text("تحويل الصيغة") },
                                        onClick = { addStepMenuExpanded = false; onUpdateConfig { it.copy(enableConvert = true) } }
                                    )
                                }
                                if (!config.enableResize) {
                                    DropdownMenuItem(
                                        text = { Text("تغيير الحجم") },
                                        onClick = { addStepMenuExpanded = false; onUpdateConfig { it.copy(enableResize = true) } }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Output Folder Section matching 12_pipeline.png
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FmSectionHeader(title = "حفظ النتائج في")
                    FmCard(
                        cornerRadius = 16.dp,
                        onClick = onPickOutputFolder,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FmIconBadge(icon = Icons.Outlined.CreateNewFolder, size = 44.dp, cornerRadius = 12.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = config.outputFolder.label,
                                    style = MaterialTheme.typography.titleSmall.monoLtr(),
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (config.preserveOriginals) "مجلد جديد · الأصول لا تُمسّ" else "مع تحديث الملفات الأصلية",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                FmPrimaryButton(
                    text = "تشغيل المعالجة",
                    trailingIcon = Icons.Filled.PlayArrow,
                    enabled = sourceImages.isNotEmpty() && activeStepsCount > 0,
                    onClick = onExecutePipeline,
                    modifier = Modifier.testTag("pipeline_execute_button")
                )
            }
        }
    }
}

@Composable
private fun StepConnector() {
    Box(
        modifier = Modifier
            .padding(start = 31.dp)
            .width(2.dp)
            .height(10.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

@Composable
private fun PipelineStepCard(
    stepNumber: Int,
    title: String,
    summary: String,
    expanded: Boolean,
    onClick: () -> Unit,
    trailingSwitchChecked: Boolean,
    onToggle: (Boolean) -> Unit,
    expandedContent: @Composable () -> Unit
) {
    FmCard(
        cornerRadius = 16.dp,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.extraColors.iconBadgeBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$stepNumber",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall.monoLtr(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Icon(
                    imageVector = Icons.Filled.DragIndicator,
                    contentDescription = "تعديل الخطوة",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                if (expanded) {
                    Switch(checked = trailingSwitchChecked, onCheckedChange = onToggle)
                }
            }

            if (expanded) {
                Spacer(Modifier.height(12.dp))
                expandedContent()
            }
        }
    }
}
