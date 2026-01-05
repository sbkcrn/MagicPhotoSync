package com.rabbit.magicphotos.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.layout.*
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.rabbit.magicphotos.MainActivity
import com.rabbit.magicphotos.data.local.MagicPhotoDatabase
import com.rabbit.magicphotos.data.local.MagicPhotoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MagicPhotoWidget : GlanceAppWidget() {
    
    companion object {
        val CURRENT_INDEX_KEY = intPreferencesKey("current_photo_index")
    }
    
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    
    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(110.dp, 110.dp),   // 2x2 Small
            DpSize(180.dp, 110.dp),   // 3x2
            DpSize(250.dp, 110.dp),   // 4x2 Wide
            DpSize(180.dp, 180.dp),   // 3x3
            DpSize(250.dp, 180.dp),   // 4x3
            DpSize(250.dp, 250.dp),   // 4x4 Large
        )
    )
    
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dao = MagicPhotoDatabase.getInstance(context).magicPhotoDao()
        // Get photos selected for widget display (max 15, most recent first)
        val photos = withContext(Dispatchers.IO) {
            dao.getWidgetPhotos()
        }
        
        provideContent {
            val prefs = currentState<androidx.datastore.preferences.core.Preferences>()
            val currentIndex = prefs[CURRENT_INDEX_KEY] ?: 0
            val photo = photos.getOrNull(currentIndex % maxOf(photos.size, 1))
            
            MagicPhotoWidgetContent(
                photo = photo,
                photoCount = photos.size,
                currentIndex = currentIndex,
                hasMultiple = photos.size > 1
            )
        }
    }
}

@Composable
private fun MagicPhotoWidgetContent(
    photo: MagicPhotoEntity?,
    photoCount: Int,
    currentIndex: Int,
    hasMultiple: Boolean
) {
    val size = LocalSize.current
    val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
    
    // Dark theme with Leuchtorange accent
    val blackBackground = Color(0xFF000000)
    val darkGray = Color(0xFF1A1A1A)
    val lightGray = Color(0xFFAAAAAA)
    val white = Color(0xFFFFFFFF)
    val leuchtOrange = Color(0xFFFF4D06)  // Leuchtorange
    
    // Determine layout based on size
    val isWide = size.width >= 180.dp
    val isTall = size.height >= 140.dp
    val isLarge = size.width >= 250.dp && size.height >= 180.dp
    
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(blackBackground))
            .cornerRadius(20.dp)
            .clickable(actionStartActivity(Intent(LocalContext.current, MainActivity::class.java))),
        contentAlignment = Alignment.Center
    ) {
        if (photo != null) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.Top,
                horizontalAlignment = Alignment.Start
            ) {
                // Main image area with orange border
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .defaultWeight()
                        .padding(6.dp)
                ) {
                    // Orange border container
                    Box(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .background(ColorProvider(leuchtOrange))
                            .cornerRadius(16.dp)
                            .padding(2.dp),  // This creates the orange border
                        contentAlignment = Alignment.Center
                    ) {
                        // Inner black container for image
                        Box(
                            modifier = GlanceModifier
                                .fillMaxSize()
                                .background(ColorProvider(blackBackground))
                                .cornerRadius(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val imagePath = photo.aiImageLocal
                            if (imagePath != null && File(imagePath).exists()) {
                                val bitmap = loadBitmap(imagePath)
                                if (bitmap != null) {
                                    Image(
                                        provider = ImageProvider(bitmap),
                                        contentDescription = "Magic Photo",
                                        modifier = GlanceModifier
                                            .fillMaxSize()
                                            .cornerRadius(12.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    PlaceholderBox(darkGray, leuchtOrange)
                                }
                            } else {
                                PlaceholderBox(darkGray, leuchtOrange)
                            }
                        }
                    }
                }
                
                // Bottom bar (only show if tall enough)
                if (isTall) {
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.Start
                    ) {
                        // Date
                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text(
                                text = dateFormat.format(Date(photo.createdOn)),
                                style = TextStyle(
                                    color = ColorProvider(white),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            if (isLarge) {
                                Text(
                                    text = "Magic Photo",
                                    style = TextStyle(
                                        color = ColorProvider(lightGray),
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }
                        
                        // Navigation buttons (if multiple photos)
                        if (hasMultiple && isWide) {
                            Row(
                                horizontalAlignment = Alignment.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Previous button
                                Box(
                                    modifier = GlanceModifier
                                        .size(28.dp)
                                        .cornerRadius(14.dp)
                                        .background(ColorProvider(darkGray))
                                        .clickable(actionRunCallback<PreviousPhotoAction>()),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "‹",
                                        style = TextStyle(
                                            color = ColorProvider(white),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                    )
                                }
                                
                                Spacer(modifier = GlanceModifier.width(6.dp))
                                
                                // Counter
                                Text(
                                    text = "${(currentIndex % photoCount) + 1}/$photoCount",
                                    style = TextStyle(
                                        color = ColorProvider(lightGray),
                                        fontSize = 10.sp
                                    )
                                )
                                
                                Spacer(modifier = GlanceModifier.width(6.dp))
                                
                                // Next button
                                Box(
                                    modifier = GlanceModifier
                                        .size(28.dp)
                                        .cornerRadius(14.dp)
                                        .background(ColorProvider(leuchtOrange))
                                        .clickable(actionRunCallback<NextPhotoAction>()),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "›",
                                        style = TextStyle(
                                            color = ColorProvider(blackBackground),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                    )
                                }
                            }
                        } else if (hasMultiple) {
                            // Dots indicator for small widgets
                            Row {
                                repeat(minOf(photoCount, 5)) { index ->
                                    val isActive = index == currentIndex % photoCount
                                    Box(
                                        modifier = GlanceModifier
                                            .size(if (isActive) 6.dp else 4.dp)
                                            .cornerRadius(3.dp)
                                            .background(
                                                ColorProvider(
                                                    if (isActive) leuchtOrange else darkGray
                                                )
                                            ),
                                        content = {}
                                    )
                                    if (index < minOf(photoCount, 5) - 1) {
                                        Spacer(modifier = GlanceModifier.width(3.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Empty state
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "📷",
                    style = TextStyle(fontSize = if (isLarge) 48.sp else 32.sp)
                )
                Spacer(modifier = GlanceModifier.height(8.dp))
                Text(
                    text = "No photos yet",
                    style = TextStyle(
                        color = ColorProvider(lightGray),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                )
                if (isTall) {
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = "Tap to open",
                        style = TextStyle(
                            color = ColorProvider(leuchtOrange),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaceholderBox(bgColor: Color, accentColor: Color) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(bgColor))
            .cornerRadius(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "📷",
            style = TextStyle(fontSize = 24.sp)
        )
    }
}

// Action to go to next photo
class NextPhotoAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                val current = this[MagicPhotoWidget.CURRENT_INDEX_KEY] ?: 0
                this[MagicPhotoWidget.CURRENT_INDEX_KEY] = current + 1
            }
        }
        MagicPhotoWidget().update(context, glanceId)
    }
}

// Action to go to previous photo
class PreviousPhotoAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                val current = this[MagicPhotoWidget.CURRENT_INDEX_KEY] ?: 0
                this[MagicPhotoWidget.CURRENT_INDEX_KEY] = maxOf(0, current - 1)
            }
        }
        MagicPhotoWidget().update(context, glanceId)
    }
}

private fun loadBitmap(path: String): Bitmap? {
    return try {
        // First, get image dimensions without loading
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(path, options)
        
        // Widget bitmaps have ~15MB limit. ARGB_8888 uses 4 bytes/pixel.
        // 800x800 = 640,000 pixels * 4 = 2.56MB per image (safe margin)
        val maxDimension = 800
        
        // Calculate sample size for initial decode
        options.inSampleSize = calculateInSampleSize(options, maxDimension, maxDimension)
        options.inJustDecodeBounds = false
        // Use ARGB_8888 for better quality
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        
        val decoded = BitmapFactory.decodeFile(path, options) ?: return null
        
        // Scale to exact target if needed (maintains aspect ratio)
        val scale = minOf(
            maxDimension.toFloat() / decoded.width,
            maxDimension.toFloat() / decoded.height,
            1f  // Don't upscale
        )
        
        if (scale < 1f) {
            val newWidth = (decoded.width * scale).toInt()
            val newHeight = (decoded.height * scale).toInt()
            val scaled = Bitmap.createScaledBitmap(decoded, newWidth, newHeight, true)
            if (scaled != decoded) {
                decoded.recycle()
            }
            scaled
        } else {
            decoded
        }
    } catch (e: Exception) {
        null
    }
}

private fun calculateInSampleSize(
    options: BitmapFactory.Options,
    reqWidth: Int,
    reqHeight: Int
): Int {
    val (height: Int, width: Int) = options.outHeight to options.outWidth
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {
        val halfHeight: Int = height / 2
        val halfWidth: Int = width / 2

        // More aggressive scaling - keep dividing until we're under the target
        while (halfHeight / inSampleSize >= reqHeight || halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }

    return inSampleSize
}
