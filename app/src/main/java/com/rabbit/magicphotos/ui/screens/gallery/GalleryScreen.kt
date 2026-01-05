package com.rabbit.magicphotos.ui.screens.gallery

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
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
import com.rabbit.magicphotos.data.local.MagicPhotoEntity
import com.rabbit.magicphotos.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    photos: List<MagicPhotoEntity>,
    isLoading: Boolean,
    isSyncing: Boolean,
    onPhotoClick: (MagicPhotoEntity) -> Unit,
    onSyncClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    // Rotation animation for sync icon
    val infiniteTransition = rememberInfiniteTransition(label = "syncRotation")
    val syncRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "✨",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Magic Photos",
                            style = MaterialTheme.typography.titleLarge,
                            color = Charcoal
                        )
                    }
                },
                actions = {
                    // Sync button with animation
                    IconButton(
                        onClick = onSyncClick,
                        enabled = !isSyncing
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Sync",
                            modifier = Modifier.graphicsLayer {
                                rotationZ = if (isSyncing) syncRotation else 0f
                            },
                            tint = if (isSyncing) RabbitOrange else Charcoal
                        )
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Charcoal
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
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
            when {
                isLoading && photos.isEmpty() -> {
                    LoadingState()
                }
                photos.isEmpty() -> {
                    EmptyState(onSyncClick = onSyncClick)
                }
                else -> {
                    PhotoGrid(
                        photos = photos,
                        onPhotoClick = onPhotoClick
                    )
                }
            }
            
            // Sync indicator overlay
            if (isSyncing) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    color = RabbitOrange,
                    trackColor = Beige200
                )
            }
        }
    }
}

@Composable
private fun PhotoGrid(
    photos: List<MagicPhotoEntity>,
    onPhotoClick: (MagicPhotoEntity) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(
            items = photos,
            key = { _, photo -> photo.id }
        ) { index, photo ->
            // Simple fade-in only on first appearance, not on every recomposition
            PhotoCard(
                photo = photo,
                onClick = { onPhotoClick(photo) }
            )
        }
    }
}

@Composable
private fun PhotoCard(
    photo: MagicPhotoEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }
    
    // Press animation
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "cardScale"
    )
    
    // Image loading state - keyed by photo.id to prevent resets
    var loadState by remember(photo.id) { mutableStateOf<AsyncImagePainter.State?>(null) }
    
    // Use local file if available (already downloaded), otherwise S3 URL
    val imageSource = remember(photo.id, photo.aiImageLocal) {
        photo.aiImageLocal?.let { File(it) } ?: photo.aiImageS3
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .scale(scale)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Charcoal.copy(alpha = 0.1f),
                spotColor = Charcoal.copy(alpha = 0.1f)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Beige200
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Image with proper caching
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageSource)
                    .crossfade(300)
                    .memoryCacheKey(photo.id)  // Cache by photo ID
                    .diskCacheKey(photo.id)     // Disk cache by photo ID
                    .build(),
                contentDescription = photo.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onState = { loadState = it }
            )
            
            // Loading shimmer
            if (loadState is AsyncImagePainter.State.Loading) {
                ShimmerEffect(modifier = Modifier.fillMaxSize())
            }
            
            // Bottom gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Charcoal.copy(alpha = 0.7f)
                            )
                        )
                    )
            )
            
            // Date badge
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateFormat.format(Date(photo.createdOn)),
                    style = MaterialTheme.typography.labelMedium,
                    color = OffWhite
                )
            }
            
            // Magic sparkle badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(RabbitOrange.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✨",
                    style = MaterialTheme.typography.labelSmall
                )
            }
            
            // Download indicator if not downloaded
            if (!photo.isDownloaded) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Warning)
                )
            }
        }
    }
}

@Composable
private fun ShimmerEffect(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
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

@Composable
private fun EmptyState(onSyncClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated camera icon
        val infiniteTransition = rememberInfiniteTransition(label = "bounce")
        val bounce by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bounceAnim"
        )
        
        Box(
            modifier = Modifier
                .size(120.dp)
                .graphicsLayer {
                    translationY = bounce * -10f
                }
                .clip(CircleShape)
                .background(Beige200),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.PhotoCamera,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = Beige500
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "No Magic Photos yet",
            style = MaterialTheme.typography.headlineSmall,
            color = Charcoal
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "Take photos with your Rabbit R1\nto see them appear here",
            style = MaterialTheme.typography.bodyMedium,
            color = CharcoalMuted,
            textAlign = TextAlign.Center,
            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onSyncClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = RabbitOrange
            ),
            shape = RoundedCornerShape(24.dp),
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp)
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sync Photos")
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = RabbitOrange,
                strokeWidth = 3.dp,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading photos...",
                style = MaterialTheme.typography.bodyMedium,
                color = CharcoalMuted
            )
        }
    }
}
