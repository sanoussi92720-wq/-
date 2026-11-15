package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.UserSettings
import com.example.model.StorageKind
import com.example.model.StorageRoot
import com.example.model.StorageStats
import com.example.ui.components.FmIconBadge
import com.example.ui.components.FmSectionHeader
import com.example.ui.components.FmSegmentedControl
import com.example.ui.components.FmTopAppBar
import com.example.ui.theme.ThemePreference
import com.example.util.FormatUtils

@Composable
fun SettingsScreen(
    settings: UserSettings,
    storageRoots: List<StorageRoot>,
    storageStats: StorageStats,
    onOpenDrawer: () -> Unit,
    onThemeChange: (ThemePreference) -> Unit,
    onPreserveOriginalsChange: (Boolean) -> Unit,
    onConfirmOverwriteChange: (Boolean) -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenStorage: () -> Unit,
    onClearCache: () -> Unit
) {
    var showAboutDialog by remember { mutableStateOf(false) }
    val safCount = storageRoots.count { it.kind == StorageKind.SAF_TREE }

    Column(modifier = Modifier.fillMaxSize()) {
        FmTopAppBar(
            title = "الإعدادات",
            onNavigationClick = onOpenDrawer,
            isMenuIcon = true
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Appearance Section matching 15_settings.png & light_settings.png
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FmSectionHeader(title = "المظهر")
                    FmSegmentedControl(
                        options = listOf(
                            ThemePreference.LIGHT to "فاتح",
                            ThemePreference.DARK to "داكن",
                            ThemePreference.SYSTEM to "حسب النظام"
                        ),
                        selected = settings.themePreference,
                        onSelect = onThemeChange,
                        tagPrefix = "theme"
                    )
                    SettingsNavigationRow(
                        icon = Icons.Outlined.Language,
                        title = "اللغة",
                        subtitle = "العربية",
                        onClick = {}
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }

            // File Access & Storage Section matching 15_settings.png
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FmSectionHeader(title = "الوصول إلى الملفات")
                    SettingsNavigationRow(
                        icon = Icons.Outlined.VerifiedUser,
                        title = "المجلدات المسموحة",
                        subtitle = if (safCount == 0) "لم تُحدد مجلدات جهاز بعد · اضغط للاختيار"
                        else "$safCount مجلدات · الإذن دائم",
                        onClick = onOpenPermissions,
                        modifier = Modifier.testTag("settings_saf_folders_row")
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsNavigationRow(
                        icon = Icons.Outlined.Storage,
                        title = "تحليل التخزين",
                        subtitle = "${FormatUtils.formatBytes(storageStats.usedBytes)} مستخدمة من ${FormatUtils.formatBytes(storageStats.totalBytes)}",
                        onClick = onOpenStorage,
                        modifier = Modifier.testTag("settings_storage_row")
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }

            // Operations Section matching 15_settings.png
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FmSectionHeader(title = "العمليات")
                    SettingsSwitchRow(
                        icon = Icons.Outlined.Lock,
                        title = "تأكيد قبل تعديل الملفات",
                        subtitle = "لا يتم استبدال الأصل دون موافقتك",
                        checked = settings.confirmBeforeOverwrite,
                        onCheckedChange = onConfirmOverwriteChange,
                        modifier = Modifier.testTag("settings_confirm_overwrite_switch")
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsSwitchRow(
                        icon = Icons.Outlined.ContentCopy,
                        title = "الاحتفاظ بالأصل عند التحويل",
                        subtitle = "حفظ النتائج في مجلد جديد",
                        checked = settings.preserveOriginalsByDefault,
                        onCheckedChange = onPreserveOriginalsChange,
                        modifier = Modifier.testTag("settings_preserve_originals_switch")
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }

            // About Section matching 15_settings.png
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FmSectionHeader(title = "حول")
                    SettingsNavigationRow(
                        icon = Icons.Outlined.Info,
                        title = "عن التطبيق",
                        subtitle = "الإصدار 1.0",
                        onClick = { showAboutDialog = true },
                        modifier = Modifier.testTag("settings_about_row")
                    )
                }
            }
        }
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("FileMaster · الإصدار 1.0") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "مدير ملفات وأداة معالجة جماعية للصور (إعادة التسمية، تحويل الصيغ، تغيير الأحجام، والمعالجة المتسلسلة).",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    TextButton(
                        onClick = {
                            onClearCache()
                            showAboutDialog = false
                        }
                    ) {
                        Text("مسح ذاكرة الصور المؤقتة")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text("إغلاق") }
            }
        )
    }
}

@Composable
private fun SettingsNavigationRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FmIconBadge(icon = icon, size = 44.dp, cornerRadius = 12.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
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

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FmIconBadge(icon = icon, size = 44.dp, cornerRadius = 12.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
