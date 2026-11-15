package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.BatchUndoGroupEntity
import com.example.model.BatchProgress
import com.example.model.BatchResultSummary
import com.example.ui.components.FmCard
import com.example.ui.components.FmIconBadge
import com.example.ui.components.FmOutlinedButton
import com.example.ui.components.FmSectionHeader
import com.example.ui.components.FmSegmentedControl
import com.example.ui.components.FmTopAppBar
import com.example.ui.components.arabicLabel
import com.example.ui.theme.extraColors
import com.example.ui.theme.ltr
import com.example.ui.theme.monoLtr
import com.example.util.FormatUtils
import kotlin.math.roundToInt

private enum class OperationsTab { ACTIVE, HISTORY }

@Composable
fun OperationsScreen(
    batchProgress: BatchProgress?,
    latestResult: BatchResultSummary?,
    batchHistory: List<BatchUndoGroupEntity>,
    onCancelBatch: () -> Unit,
    onUndoBatch: (Long) -> Unit,
    onBack: () -> Unit
) {
    var activeTab by remember { mutableStateOf(OperationsTab.ACTIVE) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        FmTopAppBar(
            title = "العمليات",
            onNavigationClick = onBack,
            actions = {
                IconButton(
                    onClick = { activeTab = OperationsTab.HISTORY },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Outlined.History, contentDescription = "السجل")
                }
            }
        )

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            FmSegmentedControl(
                options = listOf(
                    OperationsTab.ACTIVE to "قيد التنفيذ",
                    OperationsTab.HISTORY to "السجل"
                ),
                selected = activeTab,
                onSelect = { activeTab = it },
                tagPrefix = "ops_tab"
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (activeTab == OperationsTab.ACTIVE) {
                val successCount = batchProgress?.successCount ?: latestResult?.successCount ?: 0
                val errorCount = batchProgress?.failureCount ?: latestResult?.failureCount ?: 0
                val totalCount = batchProgress?.totalCount ?: latestResult?.totalCount ?: 0
                val completedCount = batchProgress?.completedCount ?: latestResult?.totalCount ?: 0
                val skippedCount = (totalCount - successCount - errorCount).coerceAtLeast(0)
                    .let { if (batchProgress?.isRunning == true) 0 else it }

                // Active or Latest Progress Card matching 13_processing_results.png
                item {
                    if (batchProgress != null && batchProgress.isRunning) {
                        val pct = (batchProgress.fraction * 100f).roundToInt()
                        FmCard(cornerRadius = 18.dp, modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = batchProgress.operationType.arabicLabel(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "$pct%",
                                        style = MaterialTheme.typography.titleMedium.monoLtr(),
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                LinearProgressIndicator(
                                    progress = { batchProgress.fraction },
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "جاري المعالجة",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${batchProgress.completedCount} / ${batchProgress.totalCount}",
                                        style = MaterialTheme.typography.bodySmall.monoLtr(),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (batchProgress.currentFileName.isNotBlank()) {
                                    Text(
                                        text = batchProgress.currentFileName,
                                        style = MaterialTheme.typography.bodySmall.monoLtr(),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                FmOutlinedButton(
                                    text = "إلغاء العملية",
                                    leadingIcon = Icons.Outlined.Stop,
                                    contentColor = MaterialTheme.colorScheme.error,
                                    onClick = onCancelBatch,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    } else if (latestResult != null) {
                        FmCard(cornerRadius = 18.dp, modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = latestResult.operationType.arabicLabel(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "100%",
                                        style = MaterialTheme.typography.titleMedium.monoLtr(),
                                        color = MaterialTheme.extraColors.successFg,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { 1f },
                                    color = MaterialTheme.extraColors.successFg,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                )
                                Text(
                                    text = "اكتمل $completedCount من $totalCount · ${latestResult.message}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        FmCard(cornerRadius = 18.dp, modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "لا توجد عملية قيد التنفيذ حالياً",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 3 Stat Tiles matching 13_processing_results.png (نجح | أخطاء | تم تخطيه)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OperationStatTile(
                            label = "نجح",
                            count = successCount,
                            icon = Icons.Outlined.Check,
                            tint = MaterialTheme.extraColors.successFg,
                            modifier = Modifier.weight(1f)
                        )
                        OperationStatTile(
                            label = "أخطاء",
                            count = errorCount,
                            icon = Icons.Outlined.WarningAmber,
                            tint = MaterialTheme.extraColors.warningFg,
                            modifier = Modifier.weight(1f)
                        )
                        OperationStatTile(
                            label = "تم تخطيه",
                            count = skippedCount,
                            icon = Icons.Outlined.Info,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Error callout matching 13_processing_results.png
                val failures = latestResult?.failures.orEmpty()
                if (failures.isNotEmpty()) {
                    items(failures.take(5)) { fail ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
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
                                    imageVector = Icons.Outlined.WarningAmber,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = fail.fileName,
                                        style = MaterialTheme.typography.titleSmall.monoLtr(),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = fail.reason,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(4.dp))
                    FmSectionHeader(title = "العمليات السابقة")
                }

                items(batchHistory.take(5), key = { it.groupId }) { group ->
                    HistoryGroupRow(group = group, onUndo = onUndoBatch)
                }
            } else {
                if (batchHistory.isEmpty()) {
                    item {
                        FmCard(cornerRadius = 16.dp, modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("السجل فارغ حالياً", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else {
                    items(batchHistory, key = { it.groupId }) { group ->
                        HistoryGroupRow(group = group, onUndo = onUndoBatch)
                    }
                }
            }
        }
    }
}

@Composable
private fun OperationStatTile(
    label: String,
    count: Int,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    FmCard(cornerRadius = 16.dp, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleLarge.monoLtr(),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun HistoryGroupRow(
    group: BatchUndoGroupEntity,
    onUndo: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FmIconBadge(
                icon = if (group.isUndone) Icons.Outlined.WarningAmber else Icons.Outlined.Check,
                size = 44.dp,
                cornerRadius = 12.dp,
                containerColor = if (group.isUndone) MaterialTheme.extraColors.warningBg else MaterialTheme.extraColors.successBg,
                iconTint = if (group.isUndone) MaterialTheme.extraColors.warningFg else MaterialTheme.extraColors.successFg
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.summaryText,
                    style = MaterialTheme.typography.titleSmall.ltr(),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${group.operationType} · ${FormatUtils.formatRelativeArabicDate(group.timestampMillis)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (group.isReversible && !group.isUndone) {
                TextButton(onClick = { onUndo(group.groupId) }) {
                    Text("تراجع", fontWeight = FontWeight.Bold)
                }
            } else {
                Text(
                    text = if (group.isUndone) "تم التراجع" else "مكتملة",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}
