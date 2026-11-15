package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.StorageKind
import com.example.model.StorageRoot
import com.example.ui.components.FmCard
import com.example.ui.components.FmIconBadge
import com.example.ui.components.FmOutlinedButton
import com.example.ui.components.FmPrimaryButton
import com.example.ui.components.FmSectionHeader
import com.example.ui.components.FmTopAppBar
import com.example.ui.theme.extraColors
import com.example.ui.theme.ltr

@Composable
fun PermissionsScreen(
    storageRoots: List<StorageRoot>,
    permissionDenied: Boolean,
    onPickSafFolder: () -> Unit,
    onRevokeSafFolder: (String) -> Unit,
    onOpenAppSettings: () -> Unit,
    onBack: () -> Unit
) {
    val safRoots = storageRoots.filter { it.kind == StorageKind.SAF_TREE }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        FmTopAppBar(
            title = "الوصول إلى الملفات",
            onNavigationClick = onBack
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                if (permissionDenied) {
                    // 18b_permission_denied.png
                    FmIconBadge(
                        icon = Icons.Outlined.Lock,
                        size = 72.dp,
                        cornerRadius = 20.dp,
                        containerColor = MaterialTheme.extraColors.errorBg,
                        iconTint = MaterialTheme.colorScheme.error
                    )
                } else {
                    // 18_permissions.png
                    FmIconBadge(
                        icon = Icons.Outlined.VerifiedUser,
                        size = 72.dp,
                        cornerRadius = 20.dp,
                        containerColor = MaterialTheme.extraColors.iconBadgeBg,
                        iconTint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (permissionDenied) "لم يتم منح الوصول"
                        else "اختر المجلدات التي يستطيع التطبيق الوصول إليها",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (permissionDenied)
                            "بدون اختيار مجلد لا يستطيع التطبيق عرض الملفات أو تعديلها. لم يتغير أي شيء على جهازك."
                        else
                            "لا نطلب صلاحية الوصول إلى كل الملفات. تحدد أنت المجلد، ونعمل داخله فقط.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            item {
                FmCard(cornerRadius = 18.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (permissionDenied) {
                            PermissionBulletRow(
                                icon = Icons.Outlined.Refresh,
                                text = "يمكنك إعادة المحاولة واختيار مجلد"
                            )
                            PermissionBulletRow(
                                icon = Icons.Outlined.Tune,
                                text = "أو إدارة الأذونات من إعدادات التطبيق"
                            )
                        } else {
                            PermissionBulletRow(
                                icon = Icons.Outlined.Folder,
                                text = "وصول للمجلد الذي تختاره فقط"
                            )
                            PermissionBulletRow(
                                icon = Icons.Outlined.Lock,
                                text = "يبقى الإذن محفوظاً بعد إغلاق التطبيق"
                            )
                            PermissionBulletRow(
                                icon = Icons.Outlined.Close,
                                text = "يمكنك سحب الإذن في أي وقت"
                            )
                        }
                    }
                }
            }

            if (safRoots.isNotEmpty() && !permissionDenied) {
                item {
                    FmSectionHeader(title = "المجلدات المسموحة حالياً (${safRoots.size})")
                }
                items(safRoots, key = { it.id }) { root ->
                    FmCard(cornerRadius = 16.dp, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FmIconBadge(icon = Icons.Outlined.Folder, size = 40.dp, cornerRadius = 11.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = root.name,
                                    style = MaterialTheme.typography.titleSmall.ltr(),
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "إذن دائم (SAF)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = { root.treeUri?.let(onRevokeSafFolder) }) {
                                Text("سحب الإذن", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (permissionDenied) {
                FmPrimaryButton(
                    text = "إعادة المحاولة",
                    onClick = onPickSafFolder
                )
                FmOutlinedButton(
                    text = "فتح إعدادات الأذونات",
                    onClick = onOpenAppSettings,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                FmPrimaryButton(
                    text = "اختيار مجلد",
                    onClick = onPickSafFolder
                )
                TextButton(onClick = onBack) {
                    Text(
                        text = "ليس الآن",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionBulletRow(
    icon: ImageVector,
    text: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        FmIconBadge(icon = icon, size = 40.dp, cornerRadius = 11.dp)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}
