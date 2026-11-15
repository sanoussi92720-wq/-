package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FindReplace
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.engine.BatchRenameEngine
import com.example.model.FileItem
import com.example.model.RenameConfig
import com.example.model.RenameConflictStatus
import com.example.model.RenameMode
import com.example.model.RenamePreviewItem
import com.example.model.RenameTargetScope
import com.example.ui.components.ConfirmBatchExecuteDialog
import com.example.ui.components.FileThumbnail
import com.example.ui.components.FmCard
import com.example.ui.components.FmIconBadge
import com.example.ui.components.FmOutlinedButton
import com.example.ui.components.FmPrimaryButton
import com.example.ui.components.FmRoundedCheckbox
import com.example.ui.components.FmSegmentedControl
import com.example.ui.components.FmStepIndicator
import com.example.ui.components.FmTopAppBar
import com.example.ui.theme.extraColors
import com.example.ui.theme.ltr
import com.example.ui.theme.monoLtr
import com.example.util.FormatUtils

@Composable
fun BatchRenameScreen(
    folderLabel: String,
    candidateFiles: List<FileItem>,
    selectedFileIds: Set<String>,
    config: RenameConfig,
    previews: List<RenamePreviewItem>,
    confirmBeforeOverwrite: Boolean,
    onToggleFileSelection: (FileItem) -> Unit,
    onSelectAllFiles: () -> Unit,
    onDeselectAllFiles: () -> Unit,
    onPickSourceFolder: () -> Unit,
    onUpdateConfig: ((RenameConfig) -> RenameConfig) -> Unit,
    onAutoResolveConflicts: () -> Unit,
    onExecuteRename: () -> Unit,
    onImportPhotos: () -> Unit,
    onBack: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(0) } // 0=Files, 1=Settings, 2=Preview
    var menuExpanded by remember { mutableStateOf(false) }
    var showExecuteConfirm by remember { mutableStateOf(false) }

    BackHandler {
        if (currentStep > 0) currentStep-- else onBack()
    }

    val title = when (currentStep) {
        0 -> "إعادة التسمية الجماعية"
        1 -> "إعدادات إعادة التسمية"
        else -> "معاينة الأسماء الجديدة"
    }

    val hasConflicts = remember(previews) { BatchRenameEngine.hasBlockingConflicts(previews) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        FmTopAppBar(
            title = title,
            onNavigationClick = { if (currentStep > 0) currentStep-- else onBack() },
            actions = {
                Box {
                    IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "خيارات")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("استيراد صور من الجهاز") },
                            onClick = { menuExpanded = false; onImportPhotos() }
                        )
                        DropdownMenuItem(
                            text = { Text("إعادة ضبط الإعدادات") },
                            onClick = {
                                menuExpanded = false
                                onUpdateConfig { RenameConfig() }
                            }
                        )
                    }
                }
            }
        )

        FmStepIndicator(
            steps = listOf("الملفات", "الإعدادات", "المعاينة"),
            currentStepIndex = currentStep,
            onStepClick = { step -> if (step <= currentStep) currentStep = step }
        )

        when (currentStep) {
            0 -> RenameStepFiles(
                folderLabel = folderLabel,
                candidateFiles = candidateFiles,
                selectedFileIds = selectedFileIds,
                config = config,
                previewCount = previews.size,
                onPickSourceFolder = onPickSourceFolder,
                onUpdateScope = { scope -> onUpdateConfig { it.copy(scope = scope) } },
                onToggleFileSelection = { item ->
                    if (config.scope != RenameTargetScope.SELECTED_ONLY) {
                        onUpdateConfig { it.copy(scope = RenameTargetScope.SELECTED_ONLY) }
                    }
                    onToggleFileSelection(item)
                },
                onToggleSelectAll = {
                    if (selectedFileIds.size >= candidateFiles.size && candidateFiles.isNotEmpty()) {
                        onDeselectAllFiles()
                    } else {
                        onSelectAllFiles()
                    }
                },
                onNext = { currentStep = 1 },
                modifier = Modifier.weight(1f)
            )

            1 -> RenameStepSettings(
                config = config,
                samplePreview = previews.firstOrNull(),
                onUpdateConfig = onUpdateConfig,
                onPrevious = { currentStep = 0 },
                onNext = { currentStep = 2 },
                modifier = Modifier.weight(1f)
            )

            else -> RenameStepPreview(
                previews = previews,
                hasConflicts = hasConflicts,
                confirmBeforeOverwrite = confirmBeforeOverwrite,
                onAutoResolveConflicts = onAutoResolveConflicts,
                onExecuteClick = {
                    if (confirmBeforeOverwrite) showExecuteConfirm = true
                    else onExecuteRename()
                },
                modifier = Modifier.weight(1f)
            )
        }
    }

    if (showExecuteConfirm) {
        ConfirmBatchExecuteDialog(
            title = "تأكيد إعادة التسمية",
            message = "سيتم إعادة تسمية ${previews.size} ملف حسب النمط المحدد. يمكنك التراجع لاحقاً من السجل.",
            confirmLabel = "تنفيذ (${previews.size})",
            onConfirm = {
                showExecuteConfirm = false
                onExecuteRename()
            },
            onDismiss = { showExecuteConfirm = false }
        )
    }
}

@Composable
private fun RenameStepFiles(
    folderLabel: String,
    candidateFiles: List<FileItem>,
    selectedFileIds: Set<String>,
    config: RenameConfig,
    previewCount: Int,
    onPickSourceFolder: () -> Unit,
    onUpdateScope: (RenameTargetScope) -> Unit,
    onToggleFileSelection: (FileItem) -> Unit,
    onToggleSelectAll: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formatsText = remember(candidateFiles) {
        candidateFiles.mapNotNull { it.extension.uppercase().takeIf { e -> e.isNotBlank() } }
            .distinct()
            .take(3)
            .joinToString(" · ")
            .ifBlank { "ملفات متنوعة" }
    }
    val allSelected = candidateFiles.isNotEmpty() && selectedFileIds.size >= candidateFiles.size
    val activeSelectedCount = if (config.scope == RenameTargetScope.SELECTED_ONLY) {
        selectedFileIds.size
    } else {
        candidateFiles.size
    }

    Column(modifier = modifier.fillMaxWidth()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Folder Card matching 05_rename_main.png & light_rename_main.png
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
                                text = folderLabel,
                                style = MaterialTheme.typography.titleMedium.ltr(),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${candidateFiles.size} ملف · $formatsText",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = onPickSourceFolder) {
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

            // Segmented Scope Control matching 05_rename_main.png
            item {
                val activeScope = if (config.scope == RenameTargetScope.SELECTED_ONLY) {
                    RenameTargetScope.SELECTED_ONLY
                } else {
                    RenameTargetScope.ALL_IN_FOLDER
                }
                FmSegmentedControl(
                    options = listOf(
                        RenameTargetScope.ALL_IN_FOLDER to "كل ملفات المجلد",
                        RenameTargetScope.SELECTED_ONLY to "الملفات المحددة فقط"
                    ),
                    selected = activeScope,
                    onSelect = onUpdateScope,
                    tagPrefix = "rename_scope"
                )
            }

            // Select All Row + Counter
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onToggleSelectAll)
                    ) {
                        FmRoundedCheckbox(checked = allSelected, onCheckedChange = onToggleSelectAll)
                        Text(
                            text = "تحديد الكل",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "$activeSelectedCount من ${candidateFiles.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (candidateFiles.isEmpty()) {
                item {
                    FmCard(cornerRadius = 16.dp, modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("لا توجد ملفات في هذا المجلد", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("اضغط «تغيير» لاختيار مجلد آخر أو استورد صوراً", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(candidateFiles, key = { it.id }) { file ->
                    val checked = config.scope != RenameTargetScope.SELECTED_ONLY || file.id in selectedFileIds
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleFileSelection(file) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FileThumbnail(item = file, size = 44.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = file.name,
                                    style = MaterialTheme.typography.titleSmall.monoLtr(),
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = FormatUtils.formatBytes(file.sizeBytes),
                                    style = MaterialTheme.typography.bodySmall.ltr(),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            FmRoundedCheckbox(
                                checked = checked,
                                onCheckedChange = { onToggleFileSelection(file) }
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }

        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                FmPrimaryButton(
                    text = "التالي: الإعدادات",
                    trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    enabled = previewCount > 0,
                    onClick = onNext,
                    modifier = Modifier.testTag("rename_next_to_settings")
                )
            }
        }
    }
}

@Composable
private fun RenameStepSettings(
    config: RenameConfig,
    samplePreview: RenamePreviewItem?,
    onUpdateConfig: ((RenameConfig) -> RenameConfig) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    var digitsDropdown by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Live sample pill matching 06_rename_settings.png
            if (samplePreview != null) {
                item {
                    FmCard(
                        containerColor = MaterialTheme.colorScheme.surface,
                        cornerRadius = 14.dp,
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
                                text = "مثال:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = samplePreview.originalName,
                                style = MaterialTheme.typography.labelMedium.monoLtr(),
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
                                    .padding(horizontal = 6.dp)
                                    .size(16.dp)
                            )
                            Text(
                                text = samplePreview.newName,
                                style = MaterialTheme.typography.labelLarge.monoLtr(),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Custom Unified Filename field ("الاسم الموحد") matching 06_rename_settings.png
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "الاسم الموحد",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = config.customBaseName,
                        onValueChange = { txt ->
                            onUpdateConfig { it.copy(mode = RenameMode.REPLACE_NAME, customBaseName = txt) }
                        },
                        enabled = config.mode == RenameMode.REPLACE_NAME,
                        placeholder = { Text("MyTheme_", style = MaterialTheme.typography.bodyLarge.monoLtr()) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleMedium.monoLtr(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rename_base_name_input")
                    )
                }
            }

            // 2x2 Grid of Numbering + Prefix/Suffix matching 06_rename_settings.png
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Start Number
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "رقم البداية",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = config.startNumber.toString(),
                            onValueChange = { raw ->
                                val parsed = raw.filter { it.isDigit() }.toIntOrNull() ?: 0
                                onUpdateConfig { it.copy(startNumber = parsed, useSequence = true) }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = MaterialTheme.typography.titleMedium.monoLtr(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("rename_start_number_input")
                        )
                    }

                    // Digits Selector
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "عدد الخانات",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Box {
                            val digitSample = "1".padStart(config.digits, '0')
                            FmCard(
                                cornerRadius = 14.dp,
                                onClick = { digitsDropdown = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 14.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = "${config.digits} ($digitSample)",
                                        style = MaterialTheme.typography.titleMedium.monoLtr(),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            DropdownMenu(expanded = digitsDropdown, onDismissRequest = { digitsDropdown = false }) {
                                listOf(2, 3, 4, 5).forEach { d ->
                                    val ex = "1".padStart(d, '0')
                                    DropdownMenuItem(
                                        text = { Text("$d ($ex)", style = MaterialTheme.typography.bodyMedium.monoLtr()) },
                                        onClick = {
                                            digitsDropdown = false
                                            onUpdateConfig { it.copy(digits = d, useSequence = true) }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "بادئة Prefix",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = config.prefix,
                            onValueChange = { p -> onUpdateConfig { it.copy(prefix = p) } },
                            placeholder = { Text("—") },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.monoLtr(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "لاحقة Suffix",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = config.suffix,
                            onValueChange = { s -> onUpdateConfig { it.copy(suffix = s) } },
                            placeholder = { Text("—") },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.monoLtr(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Find & Replace Switch Row matching 06_rename_settings.png
            item {
                val findReplaceOn = config.mode == RenameMode.MODIFY_ORIGINAL
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FmIconBadge(icon = Icons.Outlined.FindReplace, size = 44.dp, cornerRadius = 12.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "بحث واستبدال",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "داخل أسماء الملفات الحالية",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = findReplaceOn,
                            onCheckedChange = { checked ->
                                onUpdateConfig {
                                    it.copy(
                                        mode = if (checked) RenameMode.MODIFY_ORIGINAL else RenameMode.REPLACE_NAME
                                    )
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    if (findReplaceOn) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = config.findText,
                                onValueChange = { f -> onUpdateConfig { it.copy(findText = f) } },
                                label = { Text("بحث عن") },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.monoLtr(),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = config.replaceText,
                                onValueChange = { r -> onUpdateConfig { it.copy(replaceText = r) } },
                                label = { Text("استبدال بـ") },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.monoLtr(),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Preserve Extension Switch Row matching 06_rename_settings.png
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FmIconBadge(icon = Icons.Outlined.Description, size = 44.dp, cornerRadius = 12.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "الاحتفاظ بالامتداد الأصلي",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = ".png · .jpg · .webp",
                                style = MaterialTheme.typography.bodySmall.monoLtr(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = config.preserveExtension,
                            onCheckedChange = { checked ->
                                onUpdateConfig { it.copy(preserveExtension = checked) }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    if (!config.preserveExtension) {
                        OutlinedTextField(
                            value = config.customExtension,
                            onValueChange = { ext -> onUpdateConfig { it.copy(customExtension = ext) } },
                            label = { Text("الامتداد المخصص (مثال: png)") },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.monoLtr(),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        Surface(color = MaterialTheme.colorScheme.background) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FmOutlinedButton(
                    text = "السابق",
                    onClick = onPrevious,
                    modifier = Modifier.weight(0.35f)
                )
                FmPrimaryButton(
                    text = "معاينة الأسماء",
                    trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    onClick = onNext,
                    modifier = Modifier
                        .weight(0.65f)
                        .testTag("rename_next_to_preview")
                )
            }
        }
    }
}

@Composable
private fun RenameStepPreview(
    previews: List<RenamePreviewItem>,
    hasConflicts: Boolean,
    confirmBeforeOverwrite: Boolean,
    onAutoResolveConflicts: () -> Unit,
    onExecuteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Ready / Conflict Status Banner matching 07_rename_preview.png & light_rename_preview.png
            item {
                if (hasConflicts) {
                    val conflictCount = previews.count {
                        it.conflictStatus != RenameConflictStatus.READY &&
                            it.conflictStatus != RenameConflictStatus.UNCHANGED
                    }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.extraColors.errorBg,
                        border = BorderStroke(1.dp, MaterialTheme.extraColors.errorBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "تعارض في $conflictCount ملف",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "توجد أسماء مكررة أو غير صالحة قبل التنفيذ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = onAutoResolveConflicts) {
                                Text("حل تلقائي", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.extraColors.successBg,
                        border = BorderStroke(1.dp, MaterialTheme.extraColors.successBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.VerifiedUser,
                                contentDescription = null,
                                tint = MaterialTheme.extraColors.successFg,
                                modifier = Modifier.size(24.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "جاهز للتنفيذ",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.extraColors.successFg
                                )
                                Text(
                                    text = "${previews.size} ملف · لا توجد أسماء مكررة أو تعارضات",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Before / After List Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "قبل / بعد",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "عرض ${previews.size} من ${previews.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(previews, key = { it.source.id }) { item ->
                val isError = item.conflictStatus != RenameConflictStatus.READY &&
                    item.conflictStatus != RenameConflictStatus.UNCHANGED
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FileThumbnail(item = item.source, size = 44.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.originalName,
                                style = MaterialTheme.typography.bodySmall.monoLtr(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = item.newName,
                                style = MaterialTheme.typography.titleSmall.monoLtr(),
                                fontWeight = FontWeight.Bold,
                                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (item.conflictReason != null) {
                                Text(
                                    text = item.conflictReason,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        Icon(
                            imageVector = if (isError) Icons.Filled.ErrorOutline else Icons.Filled.Check,
                            contentDescription = null,
                            tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.extraColors.successFg,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }

        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FmPrimaryButton(
                    text = "تنفيذ إعادة التسمية (${previews.size})",
                    enabled = previews.isNotEmpty() && !hasConflicts,
                    onClick = onExecuteClick,
                    modifier = Modifier.testTag("rename_execute_button")
                )
                if (confirmBeforeOverwrite) {
                    Text(
                        text = "سيُطلب منك التأكيد قبل أي تغيير على الملفات",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
