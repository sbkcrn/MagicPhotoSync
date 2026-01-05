package com.rabbit.magicphotos.ui.screens.detail

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.rabbit.magicphotos.data.local.MagicPhotoEntity
import com.rabbit.magicphotos.ui.theme.*
import com.rabbit.magicphotos.util.ShareUtils
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DetailScreen(
    photos: List<MagicPhotoEntity>,
    initialIndex: Int,
    onBackClick: () -> Unit,
    onShareClick: () -> Unit,
    onToggleWidget: ((String, Boolean) -> Unit)? = null,
    onPhotoChanged: ((Int) -> Unit)? = null
) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("MMMM d, yyyy • h:mm a", Locale.getDefault()) }
    val scope = rememberCoroutineScope()
    
    if (photos.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = RabbitOrange)
        }
        return
    }
    
    // Vertical pager for navigating between photos
    val verticalPagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, photos.size - 1),
        pageCount = { photos.size }
    )
    
    // Track current photo
    val currentPhoto = photos.getOrNull(verticalPagerState.currentPage)
    
    // Notify when photo changes
    LaunchedEffect(verticalPagerState.currentPage) {
        onPhotoChanged?.invoke(verticalPagerState.currentPage)
    }
    
    // Bottom sheet for share options
    var showShareSheet by remember { mutableStateOf(false) }
    var shareHorizontalPage by remember { mutableIntStateOf(0) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // Photo counter
                    if (photos.size > 1) {
                        Text(
                            text = "${verticalPagerState.currentPage + 1} / ${photos.size}",
                            style = MaterialTheme.typography.titleMedium,
                            color = CharcoalMuted
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Charcoal
                        )
                    }
                },
                actions = {
                    currentPhoto?.let { photo ->
                        // Download indicator
                        if (!photo.isDownloaded) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(end = 8.dp),
                                strokeWidth = 2.dp,
                                color = RabbitOrange
                            )
                        }
                        
                        // Widget toggle button - uses photo.showInWidget directly from database
                        if (onToggleWidget != null) {
                            IconButton(
                                onClick = {
                                    onToggleWidget(photo.id, !photo.showInWidget)
                                }
                            ) {
                                Icon(
                                    imageVector = if (photo.showInWidget) Icons.Filled.Widgets else Icons.Outlined.Widgets,
                                    contentDescription = if (photo.showInWidget) "Remove from widget" else "Add to widget",
                                    tint = if (photo.showInWidget) RabbitOrange else CharcoalMuted
                                )
                            }
                        }
                        
                        // Save to gallery button
                        IconButton(
                            onClick = {
                                photo.aiImageLocal?.let { path ->
                                    saveToGallery(context, path, "MagicPhoto_${photo.id}")
                                }
                            }
                        ) {
                            Icon(
                                Icons.Outlined.Download,
                                contentDescription = "Save to gallery",
                                tint = Charcoal
                            )
                        }
                        
                        IconButton(onClick = { showShareSheet = true }) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "Share",
                                tint = Charcoal
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Vertical pager for photos
            VerticalPager(
                state = verticalPagerState,
                modifier = Modifier.fillMaxSize()
            ) { photoIndex ->
                val photo = photos[photoIndex]
                PhotoPage(
                    photo = photo,
                    dateFormat = dateFormat,
                    onHorizontalPageChanged = { page ->
                        shareHorizontalPage = page
                    }
                )
            }
            
            // Vertical scroll indicator (right side)
            if (photos.size > 1) {
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 8.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    repeat(minOf(photos.size, 10)) { index ->
                        val isSelected = verticalPagerState.currentPage == index
                        val dotHeight by animateDpAsState(
                            targetValue = if (isSelected) 16.dp else 6.dp,
                            label = "dotHeight"
                        )
                        val dotColor by animateColorAsState(
                            targetValue = if (isSelected) RabbitOrange else Beige400,
                            label = "dotColor"
                        )
                        
                        Box(
                            modifier = Modifier
                                .padding(vertical = 2.dp)
                                .width(4.dp)
                                .height(dotHeight)
                                .clip(RoundedCornerShape(2.dp))
                                .background(dotColor)
                        )
                    }
                    
                    if (photos.size > 10) {
                        Text(
                            text = "...",
                            style = MaterialTheme.typography.labelSmall,
                            color = CharcoalMuted,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
    
    // Share Bottom Sheet
    if (showShareSheet && currentPhoto != null) {
        ModalBottomSheet(
            onDismissRequest = { showShareSheet = false },
            containerColor = Beige100,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            ShareOptionsSheet(
                photo = currentPhoto,
                currentPage = shareHorizontalPage,
                onDismiss = { showShareSheet = false }
            )
        }
    }
}

// Overload for single photo (backward compatibility)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DetailScreen(
    photo: MagicPhotoEntity?,
    onBackClick: () -> Unit,
    onShareClick: () -> Unit,
    onToggleWidget: ((Boolean) -> Unit)? = null
) {
    if (photo == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = RabbitOrange)
        }
        return
    }
    
    DetailScreen(
        photos = listOf(photo),
        initialIndex = 0,
        onBackClick = onBackClick,
        onShareClick = onShareClick,
        onToggleWidget = if (onToggleWidget != null) { id, show -> onToggleWidget(show) } else null,
        onPhotoChanged = null
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoPage(
    photo: MagicPhotoEntity,
    dateFormat: SimpleDateFormat,
    onHorizontalPageChanged: (Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Horizontal pager for magic/original toggle
    val horizontalPagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    
    // Notify parent of horizontal page changes
    LaunchedEffect(horizontalPagerState.currentPage) {
        onHorizontalPageChanged(horizontalPagerState.currentPage)
    }
    
    // Animation states
    var isImageLoaded by remember { mutableStateOf(false) }
    val imageAlpha by animateFloatAsState(
        targetValue = if (isImageLoaded) 1f else 0f,
        animationSpec = tween(500),
        label = "imageAlpha"
    )
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Image Pager (horizontal for magic/original)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            HorizontalPager(
                state = horizontalPagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val imageSource = if (page == 0) {
                    photo.aiImageLocal?.let { File(it) } ?: photo.aiImageS3
                } else {
                    photo.originalImageLocal?.let { File(it) } ?: photo.originalImageS3
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    var loadState by remember { mutableStateOf<AsyncImagePainter.State?>(null) }
                    
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(imageSource)
                            .crossfade(true)
                            .build(),
                        contentDescription = if (page == 0) "AI Enhanced" else "Original",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(20.dp))
                            .graphicsLayer { alpha = imageAlpha },
                        contentScale = ContentScale.Fit,
                        onState = { state ->
                            loadState = state
                            if (state is AsyncImagePainter.State.Success) {
                                isImageLoaded = true
                            }
                        }
                    )
                    
                    // Loading shimmer
                    if (loadState is AsyncImagePainter.State.Loading) {
                        ShimmerPlaceholder(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(20.dp))
                        )
                    }
                }
            }
            
            // Horizontal page indicator dots
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(2) { index ->
                    val isSelected = horizontalPagerState.currentPage == index
                    val dotSize by animateDpAsState(
                        targetValue = if (isSelected) 8.dp else 6.dp,
                        label = "dotSize"
                    )
                    val dotColor by animateColorAsState(
                        targetValue = if (isSelected) RabbitOrange else Beige400,
                        label = "dotColor"
                    )
                    
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(dotSize)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                }
            }
        }
        
        // Tab Selector
        Card(
            modifier = Modifier
                .padding(horizontal = 48.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Beige200),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
            ) {
                TabButton(
                    text = "✨ Magic",
                    isSelected = horizontalPagerState.currentPage == 0,
                    onClick = {
                        scope.launch {
                            horizontalPagerState.animateScrollToPage(0)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                TabButton(
                    text = "📷 Original",
                    isSelected = horizontalPagerState.currentPage == 1,
                    onClick = {
                        scope.launch {
                            horizontalPagerState.animateScrollToPage(1)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Date
        Text(
            text = dateFormat.format(Date(photo.createdOn)),
            style = MaterialTheme.typography.bodySmall,
            color = CharcoalMuted,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) RabbitOrange else Color.Transparent,
        animationSpec = tween(200),
        label = "tabBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) OffWhite else CharcoalMuted,
        animationSpec = tween(200),
        label = "tabText"
    )
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.95f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "tabScale"
    )
    
    Surface(
        onClick = onClick,
        modifier = modifier.scale(scale),
        shape = RoundedCornerShape(24.dp),
        color = backgroundColor
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
            color = textColor,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ShareOptionsSheet(
    photo: MagicPhotoEntity,
    currentPage: Int,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Text(
            text = "Share Photo",
            style = MaterialTheme.typography.titleLarge,
            color = Charcoal
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Share current photo
        ShareOption(
            icon = Icons.Outlined.Share,
            title = if (currentPage == 0) "Share Magic Photo" else "Share Original",
            subtitle = "Share the currently viewed photo",
            onClick = {
                val path = if (currentPage == 0) photo.aiImageLocal else photo.originalImageLocal
                path?.let { ShareUtils.sharePhoto(context, it, photo.title) }
                onDismiss()
            }
        )
        
        // Share both
        if (photo.isDownloaded) {
            ShareOption(
                icon = Icons.Outlined.PhotoLibrary,
                title = "Share Both Photos",
                subtitle = "Share original and magic version together",
                onClick = {
                    ShareUtils.sharePhotoPair(
                        context,
                        photo.originalImageLocal,
                        photo.aiImageLocal,
                        photo.title
                    )
                    onDismiss()
                }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ShareOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Beige200
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = RabbitOrange,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Charcoal
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = CharcoalMuted
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Beige400
            )
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun ShimmerPlaceholder(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerProgress"
    )
    
    Box(
        modifier = modifier.background(
            Brush.horizontalGradient(
                colors = listOf(
                    Beige200,
                    Beige100,
                    Beige200
                ),
                startX = shimmerProgress * 1000f - 500f,
                endX = shimmerProgress * 1000f + 500f
            )
        )
    )
}

/**
 * Save an image file to the device's photo gallery.
 */
private fun saveToGallery(context: Context, imagePath: String, displayName: String) {
    try {
        val file = File(imagePath)
        if (!file.exists()) {
            Toast.makeText(context, "Image not found", Toast.LENGTH_SHORT).show()
            return
        }
        
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MagicPhotos")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        
        uri?.let {
            resolver.openOutputStream(it)?.use { outputStream ->
                FileInputStream(file).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }
            
            Toast.makeText(context, "Saved to gallery", Toast.LENGTH_SHORT).show()
        } ?: run {
            Toast.makeText(context, "Failed to save", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
