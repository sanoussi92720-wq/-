package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.model.FileItem
import com.example.model.OutputFolderChoice
import com.example.ui.components.BatchProgressDialog
import com.example.ui.components.BatchResultDialog
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.CreateItemDialog
import com.example.ui.components.FmIconBadge
import com.example.ui.components.FolderPickerDialog
import com.example.ui.components.SingleRenameDialog
import com.example.ui.navigation.MainTab
import com.example.ui.navigation.ToolRoutes
import com.example.ui.screens.BatchConvertScreen
import com.example.ui.screens.BatchPipelineScreen
import com.example.ui.screens.BatchRenameScreen
import com.example.ui.screens.BatchResizeScreen
import com.example.ui.screens.FileDetailsScreen
import com.example.ui.screens.FilesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ImageViewerScreen
import com.example.ui.screens.ImagesScreen
import com.example.ui.screens.OperationsScreen
import com.example.ui.screens.PermissionsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StorageScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.state.MainViewModel
import com.example.ui.state.UiMessage
import com.example.ui.theme.FileMasterTheme
import com.example.ui.theme.ThemePreference
import com.example.ui.theme.extraColors
import com.example.ui.theme.monoLtr
import com.example.util.FormatUtils
import kotlinx.coroutines.launch

private enum class FolderPickerTarget {
    CONVERT, RESIZE, PIPELINE, RENAME_SOURCE
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val settings by viewModel.userSettings.collectAsStateWithLifecycle()

            val isDark = when (settings.themePreference) {
                ThemePreference.SYSTEM -> isSystemInDarkTheme()
                ThemePreference.LIGHT -> false
                ThemePreference.DARK -> true
            }

            DisposableEffect(isDark) {
                enableEdgeToEdge(
                    statusBarStyle = if (isDark) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                    },
                    navigationBarStyle = if (isDark) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                    }
                )
                onDispose {}
            }

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                FileMasterTheme(
                    themePreference = settings.themePreference,
                    dynamicColor = settings.dynamicColor
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.background,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        FileMasterRoot(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun FileMasterRoot(viewModel: MainViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val app = context.applicationContext as FileMasterApp
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val settings by viewModel.userSettings.collectAsStateWithLifecycle()
    val storageStats by viewModel.storageStats.collectAsStateWithLifecycle()
    val recentFolders by viewModel.recentFolders.collectAsStateWithLifecycle()
    val quickFolders by viewModel.quickFolders.collectAsStateWithLifecycle()
    val fileDetailsTarget by viewModel.fileDetailsTarget.collectAsStateWithLifecycle()
    val permissionDenied by viewModel.permissionDenied.collectAsStateWithLifecycle()

    val explorerState by viewModel.explorer.collectAsStateWithLifecycle()
    val galleryState by viewModel.gallery.collectAsStateWithLifecycle()
    val viewerImages by viewModel.viewerImages.collectAsStateWithLifecycle()
    val viewerIndex by viewModel.viewerInitialIndex.collectAsStateWithLifecycle()
    val viewerDims by viewModel.viewerDimensionsCache.collectAsStateWithLifecycle()

    val renameConfig by viewModel.renameConfig.collectAsStateWithLifecycle()
    val renameFolderLabel by viewModel.renameFolderLabel.collectAsStateWithLifecycle()
    val renameCandidateFiles by viewModel.renameCandidateFiles.collectAsStateWithLifecycle()
    val renameSelectedIds by viewModel.renameSelectedFileIds.collectAsStateWithLifecycle()
    val renamePreviews by viewModel.renamePreviews.collectAsStateWithLifecycle()

    val convertConfig by viewModel.convertConfig.collectAsStateWithLifecycle()
    val convertSources by viewModel.convertSourceItems.collectAsStateWithLifecycle()
    val convertPreview by viewModel.convertPreview.collectAsStateWithLifecycle()

    val resizeConfig by viewModel.resizeConfig.collectAsStateWithLifecycle()
    val resizeSources by viewModel.resizeSourceItems.collectAsStateWithLifecycle()
    val resizeComparisons by viewModel.resizeComparisons.collectAsStateWithLifecycle()
    val resizePreview by viewModel.resizePreview.collectAsStateWithLifecycle()

    val pipelineConfig by viewModel.pipelineConfig.collectAsStateWithLifecycle()
    val pipelineSources by viewModel.pipelineSourceItems.collectAsStateWithLifecycle()

    val batchHistory by viewModel.batchHistory.collectAsStateWithLifecycle()
    val batchProgress by viewModel.batchProgress.collectAsStateWithLifecycle()
    val batchResult by viewModel.batchResult.collectAsStateWithLifecycle()
    val latestBatchResult by viewModel.latestBatchResult.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    // Dialog states
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showCreateFileDialog by remember { mutableStateOf(false) }
    var renameSingleTarget by remember { mutableStateOf<FileItem?>(null) }
    var deleteTargets by remember { mutableStateOf<List<FileItem>?>(null) }
    var copyMoveTargets by remember { mutableStateOf<Pair<List<FileItem>, Boolean>?>(null) }
    var folderPickerTarget by remember { mutableStateOf<FolderPickerTarget?>(null) }
    var importIntoCurrentFolder by remember { mutableStateOf(false) }
    var openFilesWithSearch by remember { mutableStateOf(false) }

    val safFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            viewModel.setPermissionDenied(false)
            viewModel.mountSafFolder(uri)
        } else if (currentRoute == ToolRoutes.PERMISSIONS) {
            viewModel.setPermissionDenied(true)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 500)
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importPickedPhotos(uris, intoCurrentFolder = importIntoCurrentFolder)
        }
    }

    fun launchPhotoPicker(intoCurrent: Boolean) {
        importIntoCurrentFolder = intoCurrent
        photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    fun shareItems(items: List<FileItem>) {
        if (items.isEmpty()) return
        scope.launch {
            val uris = viewModel.getShareUris(items)
            if (uris.isEmpty()) {
                viewModel.emitMessage("لا توجد ملفات قابلة للمشاركة")
                return@launch
            }
            val intent = if (uris.size == 1) {
                Intent(Intent.ACTION_SEND).apply {
                    type = items.firstOrNull()?.mimeType ?: "*/*"
                    putExtra(Intent.EXTRA_STREAM, uris.first())
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            } else {
                Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = "*/*"
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
            context.startActivity(Intent.createChooser(intent, "مشاركة عبر"))
        }
    }

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun openBatchRename(items: List<FileItem>, folderItems: List<FileItem>? = null) {
        viewModel.prepareBatchRename(items, folderItems)
        navController.navigate(ToolRoutes.BATCH_RENAME)
    }

    fun openBatchConvert(items: List<FileItem>) {
        viewModel.prepareBatchConvert(items)
        navController.navigate(ToolRoutes.BATCH_CONVERT)
    }

    fun openBatchResize(items: List<FileItem>) {
        viewModel.prepareBatchResize(items)
        navController.navigate(ToolRoutes.BATCH_RESIZE)
    }

    fun openBatchPipeline(items: List<FileItem>) {
        viewModel.prepareBatchPipeline(items)
        navController.navigate(ToolRoutes.BATCH_PIPELINE)
    }

    fun openImageViewer(images: List<FileItem>, index: Int) {
        viewModel.openImageViewer(images, index)
        navController.navigate(ToolRoutes.IMAGE_VIEWER)
    }

    fun openFileDetails(item: FileItem) {
        viewModel.openFileDetails(item)
        navController.navigate(ToolRoutes.FILE_DETAILS)
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { msg ->
            when (msg) {
                is UiMessage.Snack -> {
                    val res = snackbarHostState.showSnackbar(
                        message = msg.text,
                        actionLabel = if (msg.undoGroupId != null) "تراجع" else null,
                        duration = if (msg.undoGroupId != null) SnackbarDuration.Long else SnackbarDuration.Short
                    )
                    if (res == SnackbarResult.ActionPerformed && msg.undoGroupId != null) {
                        viewModel.undoBatchRename(msg.undoGroupId)
                    }
                }
            }
        }
    }

    val isMainTab = MainTab.all.any { it.route == currentRoute }
    val hideBottomNavForSelection = (currentRoute == MainTab.Files.route && explorerState.isMultiSelectActive) ||
        (currentRoute == MainTab.Images.route && galleryState.selectedIds.isNotEmpty())
    val showBottomNav = isMainTab && !hideBottomNavForSelection

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = isMainTab,
        drawerContent = {
            // Navigation Drawer matching 16_navigation_drawer.png
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.extraColors.navBarBg,
                drawerContentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.width(308.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 18.dp)
                ) {
                    // Header Brand
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FmIconBadge(
                            icon = Icons.Outlined.Folder,
                            size = 46.dp,
                            cornerRadius = 14.dp,
                            containerColor = MaterialTheme.colorScheme.primary,
                            iconTint = MaterialTheme.colorScheme.onPrimary
                        )
                        Column {
                            Text(
                                text = "FileMaster",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "مدير الملفات والصور",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    // Storage mini-bar matching 16_navigation_drawer.png
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch { drawerState.close() }
                                navController.navigate(ToolRoutes.STORAGE)
                            }
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "الذاكرة الداخلية",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${FormatUtils.formatBytes(storageStats.usedBytes)} / ${FormatUtils.formatBytes(storageStats.totalBytes)}",
                                style = MaterialTheme.typography.labelMedium.monoLtr(),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        LinearProgressIndicator(
                            progress = { storageStats.usedFraction },
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    val drawerColors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent,
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurface
                    )

                    NavigationDrawerItem(
                        label = { Text("الرئيسية", fontWeight = FontWeight.SemiBold) },
                        selected = currentRoute == MainTab.Home.route,
                        icon = { Icon(Icons.Outlined.Home, contentDescription = null) },
                        colors = drawerColors,
                        shape = RoundedCornerShape(28.dp),
                        onClick = { scope.launch { drawerState.close() }; navigateToTab(MainTab.Home.route) },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    NavigationDrawerItem(
                        label = { Text("الملفات") },
                        selected = currentRoute == MainTab.Files.route,
                        icon = { Icon(Icons.Outlined.Folder, contentDescription = null) },
                        colors = drawerColors,
                        shape = RoundedCornerShape(28.dp),
                        onClick = { scope.launch { drawerState.close() }; navigateToTab(MainTab.Files.route) },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    NavigationDrawerItem(
                        label = { Text("الصور") },
                        selected = currentRoute == MainTab.Images.route,
                        icon = { Icon(Icons.Outlined.Image, contentDescription = null) },
                        colors = drawerColors,
                        shape = RoundedCornerShape(28.dp),
                        onClick = { scope.launch { drawerState.close() }; navigateToTab(MainTab.Images.route) },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    Text(
                        text = "الأدوات",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
                    )

                    NavigationDrawerItem(
                        label = { Text("إعادة التسمية الجماعية") },
                        selected = currentRoute == ToolRoutes.BATCH_RENAME,
                        icon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                        colors = drawerColors,
                        shape = RoundedCornerShape(28.dp),
                        onClick = { scope.launch { drawerState.close() }; openBatchRename(emptyList()) },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    NavigationDrawerItem(
                        label = { Text("تحويل الصيغ") },
                        selected = currentRoute == ToolRoutes.BATCH_CONVERT,
                        icon = { Icon(Icons.Outlined.SwapHoriz, contentDescription = null) },
                        colors = drawerColors,
                        shape = RoundedCornerShape(28.dp),
                        onClick = { scope.launch { drawerState.close() }; openBatchConvert(emptyList()) },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    NavigationDrawerItem(
                        label = { Text("تغيير الأحجام") },
                        selected = currentRoute == ToolRoutes.BATCH_RESIZE,
                        icon = { Icon(Icons.Outlined.OpenInFull, contentDescription = null) },
                        colors = drawerColors,
                        shape = RoundedCornerShape(28.dp),
                        onClick = { scope.launch { drawerState.close() }; openBatchResize(emptyList()) },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    NavigationDrawerItem(
                        label = { Text("المعالجة المتسلسلة") },
                        selected = currentRoute == ToolRoutes.BATCH_PIPELINE,
                        icon = { Icon(Icons.Outlined.AccountTree, contentDescription = null) },
                        colors = drawerColors,
                        shape = RoundedCornerShape(28.dp),
                        onClick = { scope.launch { drawerState.close() }; openBatchPipeline(emptyList()) },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 16.dp)
                    )

                    NavigationDrawerItem(
                        label = { Text("العمليات والسجل") },
                        selected = currentRoute == ToolRoutes.OPERATIONS,
                        icon = { Icon(Icons.Outlined.History, contentDescription = null) },
                        colors = drawerColors,
                        shape = RoundedCornerShape(28.dp),
                        onClick = { scope.launch { drawerState.close() }; navController.navigate(ToolRoutes.OPERATIONS) },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    NavigationDrawerItem(
                        label = { Text("التخزين") },
                        selected = currentRoute == ToolRoutes.STORAGE,
                        icon = { Icon(Icons.Outlined.Storage, contentDescription = null) },
                        colors = drawerColors,
                        shape = RoundedCornerShape(28.dp),
                        onClick = { scope.launch { drawerState.close() }; navController.navigate(ToolRoutes.STORAGE) },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    NavigationDrawerItem(
                        label = { Text("المجلدات المسموحة") },
                        selected = currentRoute == ToolRoutes.PERMISSIONS,
                        icon = { Icon(Icons.Outlined.VerifiedUser, contentDescription = null) },
                        colors = drawerColors,
                        shape = RoundedCornerShape(28.dp),
                        onClick = {
                            scope.launch { drawerState.close() }
                            viewModel.setPermissionDenied(false)
                            navController.navigate(ToolRoutes.PERMISSIONS)
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    NavigationDrawerItem(
                        label = { Text("الإعدادات") },
                        selected = currentRoute == MainTab.Settings.route,
                        icon = { Icon(Icons.Outlined.Tune, contentDescription = null) },
                        colors = drawerColors,
                        shape = RoundedCornerShape(28.dp),
                        onClick = { scope.launch { drawerState.close() }; navigateToTab(MainTab.Settings.route) },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavRail = maxWidth >= 600.dp

            Row(modifier = Modifier.fillMaxSize()) {
                if (useNavRail && isMainTab) {
                    NavigationRail(
                        containerColor = MaterialTheme.extraColors.navBarBg,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
                    ) {
                        MainTab.all.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label, maxLines = 1) },
                                modifier = Modifier.testTag("nav_rail_${tab.tagKey}")
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f).fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        NavHost(
                            navController = navController,
                            startDestination = MainTab.Home.route,
                            modifier = Modifier
                                .fillMaxSize()
                                .let { mod -> if (isMainTab) mod.statusBarsPadding() else mod }
                        ) {
                            composable(MainTab.Home.route) {
                                HomeScreen(
                                    recentFolders = recentFolders,
                                    quickFolders = quickFolders,
                                    galleryImages = galleryState.allImages,
                                    onOpenDrawer = { scope.launch { drawerState.open() } },
                                    onOpenSearch = {
                                        openFilesWithSearch = true
                                        navigateToTab(MainTab.Files.route)
                                    },
                                    onOpenBatchRename = { openBatchRename(emptyList()) },
                                    onOpenBatchConvert = { openBatchConvert(emptyList()) },
                                    onOpenBatchResize = { openBatchResize(emptyList()) },
                                    onOpenImagesGallery = { navigateToTab(MainTab.Images.route) },
                                    onOpenBatchPipeline = { openBatchPipeline(emptyList()) },
                                    onOpenRecentFolder = { entry ->
                                        viewModel.openRecentEntry(entry)
                                        navigateToTab(MainTab.Files.route)
                                    },
                                    onOpenQuickFolder = { folder ->
                                        viewModel.openQuickFolder(folder)
                                        navigateToTab(MainTab.Files.route)
                                    }
                                )
                            }

                            composable(MainTab.Files.route) {
                                FilesScreen(
                                    state = explorerState,
                                    initialSearchOpen = openFilesWithSearch,
                                    onConsumeInitialSearch = { openFilesWithSearch = false },
                                    onOpenDrawer = { scope.launch { drawerState.open() } },
                                    onSelectRoot = viewModel::selectStorageRoot,
                                    onMountSafFolder = { safFolderLauncher.launch(null) },
                                    onOpenFolder = viewModel::openFolder,
                                    onNavigateBreadcrumb = viewModel::navigateToBreadcrumb,
                                    onNavigateUp = viewModel::navigateUpFolder,
                                    onSearchQueryChange = viewModel::setSearchQuery,
                                    onToggleViewMode = viewModel::toggleViewMode,
                                    onChangeSort = viewModel::setSort,
                                    onCategoryFilterChange = viewModel::setCategoryFilter,
                                    onToggleSelection = viewModel::toggleFileSelection,
                                    onSelectAll = viewModel::selectAllFiles,
                                    onSelectAllImages = viewModel::selectAllImagesInCurrentFolder,
                                    onDeselectAll = viewModel::deselectAllFiles,
                                    onOpenImageViewer = ::openImageViewer,
                                    onCreateFolderRequest = { showCreateFolderDialog = true },
                                    onCreateFileRequest = { showCreateFileDialog = true },
                                    onImportPhotos = { launchPhotoPicker(intoCurrent = true) },
                                    onRenameSingleRequest = { renameSingleTarget = it },
                                    onDetailsRequest = ::openFileDetails,
                                    onCopyRequest = { items -> copyMoveTargets = items to false },
                                    onMoveRequest = { items -> copyMoveTargets = items to true },
                                    onShareRequest = ::shareItems,
                                    onDeleteRequest = { items -> deleteTargets = items },
                                    onBatchRenameRequest = { items ->
                                        openBatchRename(items, explorerState.rawFiles.filter { !it.isDirectory })
                                    },
                                    onBatchConvertRequest = ::openBatchConvert,
                                    onBatchResizeRequest = ::openBatchResize,
                                    onBatchPipelineRequest = ::openBatchPipeline,
                                    onOpenStorage = { navController.navigate(ToolRoutes.STORAGE) }
                                )
                            }

                            composable(MainTab.Images.route) {
                                ImagesScreen(
                                    state = galleryState,
                                    onOpenDrawer = { scope.launch { drawerState.open() } },
                                    onFormatFilterChange = viewModel::setGalleryFormatFilter,
                                    onFolderFilterChange = viewModel::setGalleryFolderFilter,
                                    onToggleSelect = viewModel::toggleGallerySelection,
                                    onSelectAll = viewModel::selectAllGalleryImages,
                                    onDeselectAll = viewModel::deselectAllGalleryImages,
                                    onImportPhotos = { launchPhotoPicker(intoCurrent = false) },
                                    onOpenViewer = ::openImageViewer,
                                    onBatchRename = { items -> openBatchRename(items, galleryState.displayedImages) },
                                    onBatchConvert = ::openBatchConvert,
                                    onBatchResize = ::openBatchResize,
                                    onBatchPipeline = ::openBatchPipeline,
                                    onShare = ::shareItems,
                                    onDelete = { items -> deleteTargets = items }
                                )
                            }

                            composable(MainTab.Tools.route) {
                                ToolsScreen(
                                    batchHistory = batchHistory,
                                    onOpenDrawer = { scope.launch { drawerState.open() } },
                                    onOpenSearch = {
                                        openFilesWithSearch = true
                                        navigateToTab(MainTab.Files.route)
                                    },
                                    onOpenBatchRename = { openBatchRename(viewModel.getSelectedExplorerFiles()) },
                                    onOpenBatchConvert = { openBatchConvert(viewModel.getSelectedGalleryImages()) },
                                    onOpenBatchResize = { openBatchResize(viewModel.getSelectedGalleryImages()) },
                                    onOpenBatchPipeline = { openBatchPipeline(viewModel.getSelectedGalleryImages()) },
                                    onOpenImagesGallery = { navigateToTab(MainTab.Images.route) },
                                    onOpenOperations = { navController.navigate(ToolRoutes.OPERATIONS) }
                                )
                            }

                            composable(MainTab.Settings.route) {
                                SettingsScreen(
                                    settings = settings,
                                    storageRoots = explorerState.storageRoots,
                                    storageStats = storageStats,
                                    onOpenDrawer = { scope.launch { drawerState.open() } },
                                    onThemeChange = viewModel::setThemePreference,
                                    onPreserveOriginalsChange = viewModel::setPreserveOriginals,
                                    onConfirmOverwriteChange = viewModel::setConfirmBeforeOverwrite,
                                    onOpenPermissions = {
                                        viewModel.setPermissionDenied(false)
                                        navController.navigate(ToolRoutes.PERMISSIONS)
                                    },
                                    onOpenStorage = { navController.navigate(ToolRoutes.STORAGE) },
                                    onClearCache = viewModel::clearCache
                                )
                            }

                            composable(ToolRoutes.BATCH_RENAME) {
                                BatchRenameScreen(
                                    folderLabel = renameFolderLabel,
                                    candidateFiles = renameCandidateFiles,
                                    selectedFileIds = renameSelectedIds,
                                    config = renameConfig,
                                    previews = renamePreviews,
                                    confirmBeforeOverwrite = settings.confirmBeforeOverwrite,
                                    onToggleFileSelection = viewModel::toggleRenameFileSelection,
                                    onSelectAllFiles = viewModel::selectAllRenameFiles,
                                    onDeselectAllFiles = viewModel::deselectAllRenameFiles,
                                    onPickSourceFolder = { folderPickerTarget = FolderPickerTarget.RENAME_SOURCE },
                                    onUpdateConfig = viewModel::updateRenameConfig,
                                    onAutoResolveConflicts = viewModel::autoResolveRenameConflicts,
                                    onExecuteRename = viewModel::executeBatchRename,
                                    onImportPhotos = { launchPhotoPicker(intoCurrent = false) },
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(ToolRoutes.BATCH_CONVERT) {
                                BatchConvertScreen(
                                    sourceImages = convertSources,
                                    config = convertConfig,
                                    previewComparison = convertPreview,
                                    confirmBeforeOverwrite = settings.confirmBeforeOverwrite,
                                    onUpdateConfig = viewModel::updateConvertConfig,
                                    onPickOutputFolder = { folderPickerTarget = FolderPickerTarget.CONVERT },
                                    onImportPhotos = { launchPhotoPicker(intoCurrent = false) },
                                    onExecuteConvert = viewModel::executeBatchConvert,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(ToolRoutes.BATCH_RESIZE) {
                                BatchResizeScreen(
                                    sourceImages = resizeSources,
                                    config = resizeConfig,
                                    comparisons = resizeComparisons,
                                    previewComparison = resizePreview,
                                    confirmBeforeOverwrite = settings.confirmBeforeOverwrite,
                                    onUpdateConfig = viewModel::updateResizeConfig,
                                    onApplyPreset = viewModel::applyResizePreset,
                                    onPickOutputFolder = { folderPickerTarget = FolderPickerTarget.RESIZE },
                                    onImportPhotos = { launchPhotoPicker(intoCurrent = false) },
                                    onExecuteResize = viewModel::executeBatchResize,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(ToolRoutes.BATCH_PIPELINE) {
                                BatchPipelineScreen(
                                    sourceImages = pipelineSources,
                                    config = pipelineConfig,
                                    onUpdateConfig = viewModel::updatePipelineConfig,
                                    onPickOutputFolder = { folderPickerTarget = FolderPickerTarget.PIPELINE },
                                    onImportPhotos = { launchPhotoPicker(intoCurrent = false) },
                                    onExecutePipeline = viewModel::executeBatchPipeline,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(ToolRoutes.IMAGE_VIEWER) {
                                ImageViewerScreen(
                                    images = viewerImages,
                                    initialIndex = viewerIndex,
                                    dimensionsCache = viewerDims,
                                    onRequestDimensions = viewModel::loadDimensionsForItem,
                                    onBack = { navController.popBackStack() },
                                    onRenameSingle = { renameSingleTarget = it },
                                    onConvertSingle = { openBatchConvert(listOf(it)) },
                                    onResizeSingle = { openBatchResize(listOf(it)) },
                                    onShareSingle = { shareItems(listOf(it)) },
                                    onDeleteSingle = { deleteTargets = listOf(it) },
                                    onShowDetails = ::openFileDetails
                                )
                            }

                            composable(ToolRoutes.FILE_DETAILS) {
                                val target = fileDetailsTarget
                                FileDetailsScreen(
                                    item = target,
                                    dimensions = target?.let { viewerDims[it.id] },
                                    onBack = { navController.popBackStack() },
                                    onRename = { renameSingleTarget = it },
                                    onCopy = { copyMoveTargets = listOf(it) to false },
                                    onMove = { copyMoveTargets = listOf(it) to true },
                                    onDelete = { deleteTargets = listOf(it) },
                                    onShare = { shareItems(listOf(it)) },
                                    onConvert = { openBatchConvert(listOf(it)) },
                                    onResize = { openBatchResize(listOf(it)) }
                                )
                            }

                            composable(ToolRoutes.OPERATIONS) {
                                OperationsScreen(
                                    batchProgress = batchProgress,
                                    latestResult = latestBatchResult,
                                    batchHistory = batchHistory,
                                    onCancelBatch = viewModel::cancelActiveBatch,
                                    onUndoBatch = viewModel::undoBatchRename,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(ToolRoutes.STORAGE) {
                                StorageScreen(
                                    stats = storageStats,
                                    onBack = { navController.popBackStack() },
                                    onRefresh = viewModel::refreshDashboardAndGallery,
                                    onOpenFolder = { folder ->
                                        viewModel.openQuickFolder(folder)
                                        navigateToTab(MainTab.Files.route)
                                    }
                                )
                            }

                            composable(ToolRoutes.PERMISSIONS) {
                                PermissionsScreen(
                                    storageRoots = explorerState.storageRoots,
                                    permissionDenied = permissionDenied,
                                    onPickSafFolder = { safFolderLauncher.launch(null) },
                                    onRevokeSafFolder = viewModel::unmountSafFolder,
                                    onOpenAppSettings = {
                                        val intent = Intent(
                                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                            Uri.fromParts("package", context.packageName, null)
                                        )
                                        context.startActivity(intent)
                                    },
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }

                        SnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .let { if (!showBottomNav) it.navigationBarsPadding() else it }
                        )
                    }

                    // Docked 5-tab Bottom Bar matching all main screen mockups
                    if (!useNavRail && showBottomNav) {
                        DockedFiveTabBottomBar(
                            currentRoute = currentRoute,
                            onSelectTab = ::navigateToTab
                        )
                    }
                }
            }
        }
    }

    // ==================== GLOBAL MODALS & DIALOGS ====================

    if (showCreateFolderDialog) {
        CreateItemDialog(
            title = "مجلد جديد",
            label = "اسم المجلد",
            onConfirm = { name ->
                showCreateFolderDialog = false
                viewModel.createFolder(name)
            },
            onDismiss = { showCreateFolderDialog = false }
        )
    }

    if (showCreateFileDialog) {
        CreateItemDialog(
            title = "ملف نصي جديد",
            label = "اسم الملف (مثال: notes.txt)",
            initialValue = "notes.txt",
            onConfirm = { name ->
                showCreateFileDialog = false
                viewModel.createFile(name)
            },
            onDismiss = { showCreateFileDialog = false }
        )
    }

    renameSingleTarget?.let { target ->
        SingleRenameDialog(
            currentName = target.name,
            onConfirm = { newName ->
                renameSingleTarget = null
                viewModel.renameSingleItem(target, newName)
            },
            onDismiss = { renameSingleTarget = null }
        )
    }

    deleteTargets?.let { targets ->
        ConfirmDeleteDialog(
            count = targets.size,
            onConfirm = {
                deleteTargets = null
                viewModel.deleteItems(targets)
            },
            onDismiss = { deleteTargets = null }
        )
    }

    copyMoveTargets?.let { (targets, isMove) ->
        FolderPickerDialog(
            title = if (isMove) "نقل إلى مجلد" else "نسخ إلى مجلد",
            roots = explorerState.storageRoots,
            allowSameAsSource = false,
            loadSubfolders = viewModel::listSubfoldersForPicker,
            onCreateFolder = { isSaf, path, tree, name ->
                app.fileRepository.createFolder(isSaf, path, tree, name)
            },
            onSelectFolder = { choice ->
                copyMoveTargets = null
                val destIsSaf = choice.safTreeUri != null && choice.safDocumentId != null
                val destPath = if (destIsSaf) choice.safDocumentId!! else (choice.localDirPath ?: "")
                viewModel.copyOrMoveItems(targets, destIsSaf, destPath, choice.safTreeUri, isMove)
            },
            onDismiss = { copyMoveTargets = null }
        )
    }

    folderPickerTarget?.let { target ->
        FolderPickerDialog(
            title = "اختيار مجلد",
            roots = explorerState.storageRoots,
            allowSameAsSource = target != FolderPickerTarget.RENAME_SOURCE,
            loadSubfolders = viewModel::listSubfoldersForPicker,
            onCreateFolder = { isSaf, path, tree, name ->
                app.fileRepository.createFolder(isSaf, path, tree, name)
            },
            onSelectSameAsSource = {
                val choice = OutputFolderChoice("نفس مجلد الملفات الأصلية", isSameAsSource = true)
                when (target) {
                    FolderPickerTarget.CONVERT -> viewModel.updateConvertConfig { it.copy(outputFolder = choice) }
                    FolderPickerTarget.RESIZE -> viewModel.updateResizeConfig { it.copy(outputFolder = choice) }
                    FolderPickerTarget.PIPELINE -> viewModel.updatePipelineConfig { it.copy(outputFolder = choice) }
                    FolderPickerTarget.RENAME_SOURCE -> Unit
                }
            },
            onSelectFolder = { choice ->
                when (target) {
                    FolderPickerTarget.CONVERT -> viewModel.updateConvertConfig { it.copy(outputFolder = choice) }
                    FolderPickerTarget.RESIZE -> viewModel.updateResizeConfig { it.copy(outputFolder = choice) }
                    FolderPickerTarget.PIPELINE -> viewModel.updatePipelineConfig { it.copy(outputFolder = choice) }
                    FolderPickerTarget.RENAME_SOURCE -> viewModel.setBatchRenameSourceFolder(choice)
                }
            },
            onDismiss = { folderPickerTarget = null }
        )
    }

    batchProgress?.let { progress ->
        if (progress.isRunning) {
            BatchProgressDialog(
                progress = progress,
                onCancel = viewModel::cancelActiveBatch
            )
        }
    }

    batchResult?.let { summary ->
        BatchResultDialog(
            summary = summary,
            onDismiss = viewModel::dismissBatchResult,
            onUndo = if (summary.undoGroupId != null) viewModel::undoBatchRename else null,
            onPreviewOutputs = { outputs, idx ->
                openImageViewer(outputs, idx)
            }
        )
    }
}

@Composable
private fun DockedFiveTabBottomBar(
    currentRoute: String?,
    onSelectTab: (String) -> Unit
) {
    Surface(
        color = MaterialTheme.extraColors.navBarBg,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MainTab.all.forEach { tab ->
                    val selected = currentRoute == tab.route
                    val tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    val interaction = remember { MutableInteractionSource() }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(
                                interactionSource = interaction,
                                indication = null,
                                onClick = { onSelectTab(tab.route) }
                            )
                            .testTag("nav_tab_${tab.tagKey}")
                    ) {
                        Box(
                            modifier = Modifier
                                .width(54.dp)
                                .height(32.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primaryContainer
                                    else Color.Transparent
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.label,
                                tint = tint,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = tint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
