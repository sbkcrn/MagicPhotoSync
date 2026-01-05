package com.rabbit.magicphotos.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * Worker that rotates widget photos every 5 seconds.
 * Uses a chain of one-time work requests to achieve frequent updates.
 */
class WidgetRotationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        try {
            val manager = GlanceAppWidgetManager(applicationContext)
            val glanceIds = manager.getGlanceIds(MagicPhotoWidget::class.java)
            
            if (glanceIds.isEmpty()) {
                Log.d(TAG, "No widgets found, stopping rotation")
                return Result.success()
            }
            
            // Update each widget's index and refresh
            for (glanceId in glanceIds) {
                updateAppWidgetState(
                    applicationContext,
                    PreferencesGlanceStateDefinition,
                    glanceId
                ) { prefs ->
                    prefs.toMutablePreferences().apply {
                        val current = this[MagicPhotoWidget.CURRENT_INDEX_KEY] ?: 0
                        this[MagicPhotoWidget.CURRENT_INDEX_KEY] = current + 1
                    }
                }
                MagicPhotoWidget().update(applicationContext, glanceId)
            }
            
            Log.d(TAG, "Rotated ${glanceIds.size} widgets")
            
            // Schedule the next rotation
            scheduleNext(applicationContext)
            
            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error rotating widgets", e)
            return Result.retry()
        }
    }

    companion object {
        private const val TAG = "WidgetRotationWorker"
        const val WORK_NAME = "widget_rotation"
        private const val ROTATION_INTERVAL_SECONDS = 30L

        /**
         * Start the rotation cycle.
         */
        fun start(context: Context) {
            val request = OneTimeWorkRequestBuilder<WidgetRotationWorker>()
                .setInitialDelay(ROTATION_INTERVAL_SECONDS, TimeUnit.SECONDS)
                .addTag(WORK_NAME)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    request
                )
            
            Log.d(TAG, "Started widget rotation (${ROTATION_INTERVAL_SECONDS}s interval)")
        }

        /**
         * Schedule the next rotation.
         */
        private fun scheduleNext(context: Context) {
            val request = OneTimeWorkRequestBuilder<WidgetRotationWorker>()
                .setInitialDelay(ROTATION_INTERVAL_SECONDS, TimeUnit.SECONDS)
                .addTag(WORK_NAME)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    request
                )
        }

        /**
         * Stop the rotation cycle.
         */
        fun stop(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d(TAG, "Stopped widget rotation")
        }
    }
}

