package com.rabbit.magicphotos.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.rabbit.magicphotos.MainActivity
import com.rabbit.magicphotos.R
import java.io.File

object NotificationUtils {
    
    private const val CHANNEL_ID_SYNC = "sync_channel"
    private const val CHANNEL_ID_NEW_PHOTOS = "new_photos_channel"
    private const val NOTIFICATION_ID_SYNC = 1001
    private const val NOTIFICATION_ID_NEW_PHOTO = 1002
    
    /**
     * Create notification channels (call on app startup).
     */
    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            
            // Sync progress channel
            val syncChannel = NotificationChannel(
                CHANNEL_ID_SYNC,
                "Sync Progress",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows sync progress"
                setShowBadge(false)
            }
            
            // New photos channel
            val photosChannel = NotificationChannel(
                CHANNEL_ID_NEW_PHOTOS,
                "New Photos",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifies when new Magic Photos are synced"
            }
            
            notificationManager.createNotificationChannels(listOf(syncChannel, photosChannel))
        }
    }
    
    /**
     * Check if notification permission is granted.
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
    
    /**
     * Show sync in progress notification.
     */
    fun showSyncingNotification(context: Context) {
        if (!hasNotificationPermission(context)) return
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_SYNC)
            .setSmallIcon(R.drawable.ic_sync_notification)
            .setContentTitle("Syncing Magic Photos")
            .setContentText("Downloading new photos...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setProgress(0, 0, true)
            .build()
        
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_SYNC, notification)
    }
    
    /**
     * Cancel sync notification.
     */
    fun cancelSyncNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_SYNC)
    }
    
    /**
     * Show notification for new photos synced.
     */
    fun showNewPhotosNotification(context: Context, count: Int, latestPhotoPath: String? = null) {
        if (!hasNotificationPermission(context)) return
        if (count <= 0) return
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val builder = NotificationCompat.Builder(context, CHANNEL_ID_NEW_PHOTOS)
            .setSmallIcon(R.drawable.ic_photo_notification)
            .setContentTitle(if (count == 1) "New Magic Photo!" else "$count new Magic Photos!")
            .setContentText("Tap to view your latest photos")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
        
        // Add big picture style if we have a photo
        latestPhotoPath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(path)
                if (bitmap != null) {
                    builder.setStyle(
                        NotificationCompat.BigPictureStyle()
                            .bigPicture(bitmap)
                            .bigLargeIcon(null as android.graphics.Bitmap?)
                    )
                    builder.setLargeIcon(bitmap)
                }
            }
        }
        
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_NEW_PHOTO, builder.build())
    }
}

