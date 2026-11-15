package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.FileItem
import com.example.ui.components.FmTopAppBar
import com.example.ui.theme.monoLtr
import com.example.util.FormatUtils
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun ImageViewerScreen(
    images: List<FileItem>,
    initialIndex: Int,
    dimensionsCache: Map<String, Pair<Int, Int>>,
    onRequestDimensions: (FileItem) -> Unit,
    onBack: () -> Unit,
    onRenameSingle: (FileItem) -> Unit,
    onConvertSingle: (FileItem) -> Unit,
    onResizeSingle: (FileItem) -> Unit,
    onShareSingle: (FileItem) -> Unit,
    onDeleteSingle: (FileItem) -> Unit,
    onShowDetails: (FileItem) -> Unit
) {
    if (images.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            FmTopAppBar(title = "معاينة الصورة", onNavigationClick = onBack)
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("لا توجد صور للمعاينة", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    val safeStart = initialIndex.coerceIn(0, images.lastIndex)
    val pagerState = rememberPagerState(initialPage = safeStart) { images.size }
    val stripState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var currentScale by remember { mutableFloatStateOf(1f) }
    var menuExpanded by remember { mutableStateOf(false) }

    val currentItem = images[pagerState.currentPage.coerceIn(0, images.lastIndex)]
    val dims = dimensionsCache[currentItem.id]

    LaunchedEffect(pagerState.currentPage) {
        currentScale = 1f
        onRequestDimensions(currentItem)
        stripState.animateScrollToItem(pagerState.currentPage)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        FmTopAppBar(
            title = currentItem.name,
            onNavigationClick = onBack,
            centerTitle = true,
            actions = {
                Box {
                    IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "خيارات")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("تحويل الصيغة") },
                            onClick = { menuExpanded = false; onConvertSingle(currentItem) }
                        )
                        DropdownMenuItem(
                            text = { Text("تغيير الحجم") },
                            onClick = { menuExpanded = false; onResizeSingle(currentItem) }
                        )
                        DropdownMenuItem(
                            text = { Text("تفاصيل الملف") },
                            onClick = { menuExpanded = false; onShowDetails(currentItem) }
                        )
                    }
                }
            }
        )

        // Canvas Preview Area matching 10_image_preview.png
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
        ) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = currentScale <= 1.05f,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val img = images[page]
                var scale by remember(img.id) { mutableFloatStateOf(1f) }
                var offset by remember(img.id) { mutableStateOf(Offset.Zero) }
                val transformState = rememberTransformableState { zoomChange, panChange, _ ->
                    val nextScale = (scale * zoomChange).coerceIn(1f, 4f)
                    scale = nextScale
                    currentScale = nextScale
                    offset = if (nextScale > 1f) offset + panChange else Offset.Zero
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(img.id) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (scale > 1.1f) {
                                        scale = 1f
                                        offset = Offset.Zero
                                    } else {
                                        scale = 2f
                                    }
                                    currentScale = scale
                                }
                            )
                        }
                        .transformable(state = transformState),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(img.uri)
                            .crossfade(true)
                            .build(),
                        contentDescription = img.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            )
                    )
                }
            }

            // Index Pill (Start/Right bottom of canvas)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.6f),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp)
            ) {
                Text(
                    text = "${pagerState.currentPage + 1} / ${images.size}",
                    style = MaterialTheme.typography.labelLarge.monoLtr(),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                )
            }

            // Zoom Pill (End/Left bottom of canvas)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.6f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${(currentScale * 100f).roundToInt()}%",
                        style = MaterialTheme.typography.labelLarge.monoLtr(),
                        color = Color.White
                    )
                    Icon(
                        imageVector = Icons.Outlined.ZoomIn,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Filmstrip Thumbnails + Metadata line matching 10_image_preview.png
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LazyRow(
                state = stripState,
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(images, key = { _, item -> item.id }) { idx, img ->
                    val active = idx == pagerState.currentPage
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(img.uri)
                            .size(160)
                            .build(),
                        contentDescription = img.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .then(
                                if (active) Modifier.border(
                                    2.dp,
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(10.dp)
                                ) else Modifier
                            )
                            .clickable {
                                scope.launch { pagerState.animateScrollToPage(idx) }
                            }
                    )
                }
            }

            val ext = currentItem.extension.uppercase().ifBlank { "IMG" }
            val sizeStr = FormatUtils.formatBytes(currentItem.sizeBytes)
            val dimsStr = if (dims != null) " · ${dims.first}×${dims.second}" else ""
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = "$ext · $sizeStr$dimsStr",
                    style = MaterialTheme.typography.bodySmall.monoLtr(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // Bottom 4-Action Bar matching 10_image_preview.png
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ViewerBottomAction("مشاركة", Icons.Outlined.Share, { onShareSingle(currentItem) }, Modifier.weight(1f))
            ViewerBottomAction("التفاصيل", Icons.Outlined.Info, { onShowDetails(currentItem) }, Modifier.weight(1f))
            ViewerBottomAction("إعادة تسمية", Icons.Outlined.Edit, { onRenameSingle(currentItem) }, Modifier.weight(1f))
            ViewerBottomAction("حذف", Icons.Outlined.Delete, { onDeleteSingle(currentItem) }, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ViewerBottomAction(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
