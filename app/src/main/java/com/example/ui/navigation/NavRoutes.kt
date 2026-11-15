package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.ui.graphics.vector.ImageVector

sealed class MainTab(
    val route: String,
    val tagKey: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Home : MainTab("tab_home", "home", "الرئيسية", Icons.Filled.Home, Icons.Outlined.Home)
    data object Files : MainTab("tab_files", "files", "الملفات", Icons.Filled.Folder, Icons.Outlined.Folder)
    data object Images : MainTab("tab_images", "images", "الصور", Icons.Filled.Image, Icons.Outlined.Image)
    data object Tools : MainTab("tab_tools", "tools", "الأدوات", Icons.Filled.GridView, Icons.Outlined.GridView)
    data object Settings : MainTab("tab_settings", "settings", "الإعدادات", Icons.Filled.Tune, Icons.Outlined.Tune)

    companion object {
        val all = listOf(Home, Files, Images, Tools, Settings)
    }
}

object ToolRoutes {
    const val BATCH_RENAME = "tool_batch_rename"
    const val BATCH_CONVERT = "tool_batch_convert"
    const val BATCH_RESIZE = "tool_batch_resize"
    const val BATCH_PIPELINE = "tool_batch_pipeline"
    const val IMAGE_VIEWER = "image_viewer"
    const val FILE_DETAILS = "file_details"
    const val OPERATIONS = "operations_center"
    const val STORAGE = "storage_analysis"
    const val PERMISSIONS = "permissions_access"
}
