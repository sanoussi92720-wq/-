package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.BatchOperationType
import com.example.model.BatchProgress
import com.example.model.BatchResultSummary
import com.example.model.BreadcrumbNode
import com.example.model.FileCategory
import com.example.model.FileItem
import com.example.model.OutputFolderChoice
import com.example.model.StorageKind
import com.example.model.StorageRoot
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.extraColors
import com.example.ui.theme.ltr
import com.example.ui.theme.monoLtr
import kotlinx.coroutines.launch

@Composable
fun FmTopAppBar(
    title: String,
    onNavigationClick: (() -> Unit)? = null,
    isMenuIcon: Boolean = false,
    navigationContentDescription: String = if (isMenuIcon) "القائمة" else "رجوع",
    navigationTestTag: String? = null,
    centerTitle: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onNavigationClick != null) {
            IconButton(
                onClick = onNavigationClick,
                modifier = Modifier
                    .size(48.dp)
                    .let { if (navigationTestTag != null) it.testTag(navigationTestTag) else it }
            ) {
                Icon(
                    imageVector = if (isMenuIcon) Icons.Filled.Menu else Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = navigationContentDescription,
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        } else {
            Spacer(Modifier.width(12.dp))
        }

        if (centerTitle) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.ltr(),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            content = actions
        )
    }
}

@Composable
fun FmCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    cornerRadius: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Surface(
        shape = shape,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier.let {
            if (onClick != null) it.clip(shape).clickable(onClick = onClick) else it
        },
        content = content
    )
}

@Composable
fun FmIconBadge(
    icon: ImageVector,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    cornerRadius: Dp = 12.dp,
    containerColor: Color = MaterialTheme.extraColors.iconBadgeBg,
    iconTint: Color = MaterialTheme.colorScheme.primary
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}

@Composable
fun FmPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trailingIcon: ImageVector? = null,
    leadingIcon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (trailingIcon != null) {
            Spacer(Modifier.width(6.dp))
            Icon(trailingIcon, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun FmOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    leadingIcon: ImageVector? = null
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor),
        modifier = modifier.height(52.dp)
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun <T> FmSegmentedControl(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    tagPrefix: String = "seg"
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEach { (value, label) ->
                val active = value == selected
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (active) MaterialTheme.colorScheme.surface else Color.Transparent,
                    border = if (active) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelect(value) }
                        .testTag("${tagPrefix}_${value.toString().lowercase()}")
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FmRoundedCheckbox(
    checked: Boolean,
    onCheckedChange: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (checked) MaterialTheme.colorScheme.primary else Color.Transparent
    val border = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Checkbox, onClick = onCheckedChange),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(bg)
                .border(1.5.dp, border, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

@Composable
fun FmStepIndicator(
    steps: List<String>,
    currentStepIndex: Int,
    onStepClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        steps.forEachIndexed { idx, label ->
            val isCompleted = idx < currentStepIndex
            val isActive = idx == currentStepIndex
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = idx <= currentStepIndex) { onStepClick(idx) }
                    .padding(horizontal = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isActive -> MaterialTheme.colorScheme.primary
                                isCompleted -> MaterialTheme.extraColors.iconBadgeBg
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Text(
                            text = "${idx + 1}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    color = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (idx < steps.lastIndex) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 14.dp, start = 6.dp, end = 6.dp)
                        .height(2.dp)
                        .background(
                            if (idx < currentStepIndex) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                )
            }
        }
    }
}

@Composable
fun FmSectionHeader(
    title: String,
    trailing: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
        trailing?.invoke()
    }
}

@Composable
fun FileThumbnail(
    item: FileItem,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)
    if (item.isImage && !item.isDirectory) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.uri)
                .size(256)
                .crossfade(true)
                .build(),
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
    } else {
        val isFolder = item.category == FileCategory.FOLDER
        val icon = when (item.category) {
            FileCategory.FOLDER -> Icons.Outlined.Folder
            FileCategory.IMAGE -> Icons.Filled.Image
            FileCategory.VIDEO -> Icons.Filled.VideoFile
            FileCategory.AUDIO -> Icons.Filled.AudioFile
            FileCategory.DOCUMENT -> Icons.Filled.Description
            FileCategory.ARCHIVE -> Icons.Filled.FolderZip
            FileCategory.OTHER -> Icons.AutoMirrored.Filled.InsertDriveFile
        }
        val tint = if (isFolder) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        val bg = if (isFolder) MaterialTheme.extraColors.iconBadgeBg else MaterialTheme.colorScheme.surfaceVariant
        Box(
            modifier = modifier
                .size(size)
                .clip(shape)
                .background(bg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = item.category.label,
                tint = tint,
                modifier = Modifier.size(size * 0.52f)
            )
        }
    }
}

@Composable
fun BatchProgressDialog(
    progress: BatchProgress,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(text = progress.operationType.arabicLabel(), style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "تم إنجاز ${progress.completedCount} من ${progress.totalCount}",
                    style = MaterialTheme.typography.bodyMedium
                )
                LinearProgressIndicator(
                    progress = { progress.fraction },
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )
                if (progress.currentFileName.isNotBlank()) {
                    Text(
                        text = progress.currentFileName,
                        style = MaterialTheme.typography.labelSmall.monoLtr(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "نجح: ${progress.successCount}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.extraColors.successFg
                    )
                    if (progress.failureCount > 0) {
                        Text(
                            text = "أخطاء: ${progress.failureCount}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.testTag("cancel_batch_button")
            ) {
                Text("إلغاء العملية", color = MaterialTheme.colorScheme.error)
            }
        }
    )
}

@Composable
fun BatchResultDialog(
    summary: BatchResultSummary,
    onDismiss: () -> Unit,
    onUndo: ((Long) -> Unit)? = null,
    onPreviewOutputs: ((List<FileItem>, Int) -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = if (summary.failureCount == 0) Icons.Filled.CheckCircle else Icons.Filled.Error,
                    contentDescription = null,
                    tint = if (summary.failureCount == 0) MaterialTheme.extraColors.successFg else MaterialTheme.colorScheme.error
                )
                Text(
                    text = "اكتملت عملية ${summary.operationType.arabicLabel()}",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FmCard(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    cornerRadius = 12.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("نجح", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "${summary.successCount}",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.extraColors.successFg,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("أخطاء", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "${summary.failureCount}",
                                style = MaterialTheme.typography.headlineSmall,
                                color = if (summary.failureCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (summary.outputFiles.any { it.isImage } && onPreviewOutputs != null) {
                    val imageOutputs = summary.outputFiles.filter { it.isImage }
                    Text(
                        text = "المخرجات (${imageOutputs.size})",
                        style = MaterialTheme.typography.labelLarge
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(imageOutputs.take(15).withIndex().toList()) { (idx, file) ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(68.dp)
                                    .clickable {
                                        onDismiss()
                                        onPreviewOutputs(imageOutputs, idx)
                                    }
                            ) {
                                FileThumbnail(item = file, size = 60.dp)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = file.name,
                                    style = MaterialTheme.typography.labelSmall.ltr(),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                if (summary.failures.isNotEmpty()) {
                    Text(
                        text = "الأخطاء",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    LazyColumn(modifier = Modifier.heightIn(max = 120.dp)) {
                        items(summary.failures) { fail ->
                            Text(
                                text = "• ${fail.fileName}: ${fail.reason}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.testTag("batch_result_done_button")
            ) {
                Text("تم")
            }
        },
        dismissButton = {
            val undoId = summary.undoGroupId
            if (undoId != null && onUndo != null) {
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onUndo(undoId)
                    },
                    modifier = Modifier.testTag("batch_result_undo_button")
                ) {
                    Icon(Icons.Filled.Undo, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("تراجع")
                }
            }
        }
    )
}

/**
 * Folder picker dialog redesigned to match 17_folder_picker.png.
 */
@Composable
fun FolderPickerDialog(
    title: String,
    roots: List<StorageRoot>,
    allowSameAsSource: Boolean,
    loadSubfolders: suspend (Boolean, String, String?) -> List<FileItem>,
    onCreateFolder: suspend (Boolean, String, String?, String) -> FileItem?,
    onSelectSameAsSource: (() -> Unit)? = null,
    onSelectFolder: (OutputFolderChoice) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedRoot by remember { mutableStateOf(roots.firstOrNull()) }
    var breadcrumbs by remember { mutableStateOf<List<BreadcrumbNode>>(emptyList()) }
    var subfolders by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var highlightedFolder by remember { mutableStateOf<FileItem?>(null) }
    var showNewFolderInput by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    fun navigateRoot(root: StorageRoot) {
        selectedRoot = root
        highlightedFolder = null
        val initial = if (root.kind == StorageKind.SAF_TREE && root.treeUri != null) {
            val treeUri = android.net.Uri.parse(root.treeUri)
            val docId = android.provider.DocumentsContract.getTreeDocumentId(treeUri)
            BreadcrumbNode(root.name, docId, isSaf = true, treeUri = root.treeUri)
        } else {
            BreadcrumbNode(root.name, root.rootPath ?: "", isSaf = false)
        }
        breadcrumbs = listOf(initial)
    }

    LaunchedEffect(roots) {
        if (breadcrumbs.isEmpty() && roots.isNotEmpty()) {
            navigateRoot(roots.first())
        }
    }

    val currentNode = breadcrumbs.lastOrNull()
    LaunchedEffect(currentNode) {
        if (currentNode != null) {
            highlightedFolder = null
            subfolders = loadSubfolders(currentNode.isSaf, currentNode.pathOrDocId, currentNode.treeUri)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header matching 17_folder_picker.png
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "إغلاق")
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { showNewFolderInput = !showNewFolderInput },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Outlined.CreateNewFolder, contentDescription = "مجلد جديد")
                    }
                }

                // Roots switcher if multiple roots
                if (roots.size > 1) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        items(roots) { root ->
                            val active = root.id == selectedRoot?.id
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.clickable { navigateRoot(root) }
                            ) {
                                Text(
                                    text = root.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // Breadcrumb path (monospace LTR like 17_folder_picker.png)
                if (currentNode != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            if (breadcrumbs.size > 1) {
                                IconButton(
                                    onClick = { breadcrumbs = breadcrumbs.dropLast(1) },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "المجلد السابق")
                                }
                            }
                            Text(
                                text = breadcrumbs.joinToString(" / ") { it.name },
                                style = MaterialTheme.typography.labelMedium.monoLtr(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (allowSameAsSource && onSelectSameAsSource != null) {
                            TextButton(
                                onClick = {
                                    onSelectSameAsSource()
                                    onDismiss()
                                }
                            ) {
                                Text("نفس المجلد الأصلي", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                if (showNewFolderInput && currentNode != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newFolderName,
                            onValueChange = { newFolderName = it },
                            label = { Text("اسم المجلد الجديد") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                val trimmed = newFolderName.trim()
                                if (trimmed.isNotEmpty()) {
                                    scope.launch {
                                        onCreateFolder(
                                            currentNode.isSaf,
                                            currentNode.pathOrDocId,
                                            currentNode.treeUri,
                                            trimmed
                                        )
                                        subfolders = loadSubfolders(
                                            currentNode.isSaf,
                                            currentNode.pathOrDocId,
                                            currentNode.treeUri
                                        )
                                        newFolderName = ""
                                        showNewFolderInput = false
                                    }
                                }
                            }
                        ) { Text("إنشاء") }
                    }
                }

                // Folder rows list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp, max = 340.dp)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (subfolders.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "لا توجد مجلدات فرعية هنا — سيتم استخدام المجلد الحالي",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        items(subfolders, key = { it.id }) { folder ->
                            val isSelected = highlightedFolder?.id == folder.id
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) MaterialTheme.extraColors.selectedRowBg else Color.Transparent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        if (isSelected) {
                                            // Double tap / tap already selected enters folder
                                            val next = if (folder.isSaf && folder.safDocumentId != null) {
                                                BreadcrumbNode(folder.name, folder.safDocumentId, true, folder.safTreeUri)
                                            } else {
                                                BreadcrumbNode(folder.name, folder.pathOrUri, false)
                                            }
                                            breadcrumbs = breadcrumbs + next
                                        } else {
                                            highlightedFolder = folder
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    FmIconBadge(
                                        icon = Icons.Outlined.Folder,
                                        size = 40.dp,
                                        cornerRadius = 11.dp,
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.extraColors.iconBadgeBg,
                                        iconTint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = folder.name,
                                            style = MaterialTheme.typography.titleSmall.ltr(),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${folder.childCount} ملف",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "محدد",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    } else {
                                        IconButton(
                                            onClick = {
                                                val next = if (folder.isSaf && folder.safDocumentId != null) {
                                                    BreadcrumbNode(folder.name, folder.safDocumentId, true, folder.safTreeUri)
                                                } else {
                                                    BreadcrumbNode(folder.name, folder.pathOrUri, false)
                                                }
                                                breadcrumbs = breadcrumbs + next
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                contentDescription = "فتح المجلد",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "سيحصل التطبيق على صلاحية قراءة وكتابة هذا المجلد ومجلداته الفرعية فقط.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    FmPrimaryButton(
                        text = "استخدام هذا المجلد",
                        enabled = highlightedFolder != null || currentNode != null,
                        onClick = {
                            val chosen = highlightedFolder
                            val choice = if (chosen != null) {
                                if (chosen.isSaf) {
                                    OutputFolderChoice(
                                        label = chosen.name,
                                        isSameAsSource = false,
                                        safTreeUri = chosen.safTreeUri,
                                        safDocumentId = chosen.safDocumentId
                                    )
                                } else {
                                    OutputFolderChoice(
                                        label = chosen.name,
                                        isSameAsSource = false,
                                        localDirPath = chosen.pathOrUri
                                    )
                                }
                            } else {
                                val node = currentNode ?: return@FmPrimaryButton
                                if (node.isSaf) {
                                    OutputFolderChoice(
                                        label = node.name,
                                        isSameAsSource = false,
                                        safTreeUri = node.treeUri,
                                        safDocumentId = node.pathOrDocId
                                    )
                                } else {
                                    OutputFolderChoice(
                                        label = node.name,
                                        isSameAsSource = false,
                                        localDirPath = node.pathOrDocId
                                    )
                                }
                            }
                            onSelectFolder(choice)
                            onDismiss()
                        },
                        modifier = Modifier.testTag("folder_picker_confirm")
                    )
                }
            }
        }
    }
}

@Composable
fun ConfirmBatchExecuteDialog(
    title: String,
    message: String,
    confirmLabel: String = "تنفيذ",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.testTag("confirm_batch_execute_button")
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
fun SingleRenameDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("إعادة التسمية") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("الاسم الجديد") },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.monoLtr(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("single_rename_input")
            )
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim()) },
                enabled = name.isNotBlank() && name.trim() != currentName,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.testTag("single_rename_confirm")
            ) { Text("حفظ") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
fun CreateItemDialog(
    title: String,
    label: String,
    initialValue: String = "",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(label) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.ltr(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_item_input")
            )
        },
        confirmButton = {
            Button(
                onClick = { if (text.isNotBlank()) onConfirm(text.trim()) },
                enabled = text.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.testTag("create_item_confirm")
            ) { Text("إنشاء") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
fun ConfirmDeleteDialog(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("حذف $count ${if (count == 1) "عنصر" else "عناصر"}؟") },
        text = {
            Text("سيتم حذف العناصر المحددة نهائياً من وحدة التخزين.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier.testTag("confirm_delete_button")
            ) { Text("حذف") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

fun FileCategory.arabicLabel(): String = when (this) {
    FileCategory.FOLDER -> "مجلد"
    FileCategory.IMAGE -> "صورة"
    FileCategory.VIDEO -> "فيديو"
    FileCategory.AUDIO -> "صوت"
    FileCategory.DOCUMENT -> "مستند"
    FileCategory.ARCHIVE -> "أرشيف"
    FileCategory.OTHER -> "ملف"
}

fun BatchOperationType.arabicLabel(): String = when (this) {
    BatchOperationType.RENAME -> "إعادة التسمية الجماعية"
    BatchOperationType.CONVERT -> "تحويل صيغ الصور"
    BatchOperationType.RESIZE -> "تغيير أحجام الصور"
    BatchOperationType.PIPELINE -> "المعالجة المتسلسلة"
}
