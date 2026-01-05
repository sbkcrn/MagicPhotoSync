package com.rabbit.magicphotos.sync

import android.content.Context
import android.util.Log
import androidx.work.*
import com.rabbit.magicphotos.data.local.MagicPhotoDatabase
import com.rabbit.magicphotos.data.local.TokenStorage
import com.rabbit.magicphotos.data.repository.PhotoRepository
import com.rabbit.magicphotos.util.NotificationUtils
import com.rabbit.magicphotos.widget.MagicPhotoWidgetReceiver
import java.util.concurrent.TimeUnit

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    private val repository = PhotoRepository(context)
    private val tokenStorage = TokenStorage(context)
    private val dao = MagicPhotoDatabase.getInstance(context).magicPhotoDao()
    
    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting sync work")
        
        // Check if logged in
        if (!tokenStorage.isLoggedIn) {
            Log.d(TAG, "Not logged in, skipping sync")
            return Result.success()
        }
        
        // Show syncing notification
        NotificationUtils.showSyncingNotification(applicationContext)
        
        try {
            // Sync photos from API
            val syncResult = repository.syncPhotos()
            if (syncResult.isFailure) {
                Log.e(TAG, "Sync failed", syncResult.exceptionOrNull())
                NotificationUtils.cancelSyncNotification(applicationContext)
                return Result.retry()
            }
            
            val newPhotos = syncResult.getOrDefault(0)
            Log.d(TAG, "Synced $newPhotos new photos")
            
            // Download pending photos
            val downloadResult = repository.downloadAllPending()
            val downloaded = downloadResult.getOrDefault(0)
            Log.d(TAG, "Downloaded $downloaded photos")
            
            // Cancel sync notification
            NotificationUtils.cancelSyncNotification(applicationContext)
            
            // Show new photos notification if any new ones
            if (newPhotos > 0) {
                val latestPhoto = dao.getLatestPhoto()
                NotificationUtils.showNewPhotosNotification(
                    applicationContext,
                    newPhotos,
                    latestPhoto?.aiImageLocal
                )
            }
            
            // Update widgets
            try {
                MagicPhotoWidgetReceiver.updateAllWidgets(applicationContext)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update widgets", e)
            }
            
            return Result.success()
            
        } catch (e: Exception) {
            Log.e(TAG, "Sync error", e)
            NotificationUtils.cancelSyncNotification(applicationContext)
            return Result.retry()
        }
    }
    
    companion object {
        private const val TAG = "SyncWorker"
        const val WORK_NAME = "magic_photo_sync"
        
        /**
         * Schedule periodic sync.
         */
        fun schedule(context: Context, intervalHours: Long = 6) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()
            
            val request = PeriodicWorkRequestBuilder<SyncWorker>(
                intervalHours, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()
            
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
            
            Log.d(TAG, "Scheduled periodic sync every $intervalHours hours")
        }
        
        /**
         * Run sync immediately.
         */
        fun runNow(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()
            
            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    "${WORK_NAME}_immediate",
                    ExistingWorkPolicy.REPLACE,
                    request
                )
            
            Log.d(TAG, "Started immediate sync")
        }
        
        /**
         * Cancel all sync work.
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d(TAG, "Cancelled sync work")
        }
    }
}
