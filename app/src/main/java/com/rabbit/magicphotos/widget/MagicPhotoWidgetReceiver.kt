package com.rabbit.magicphotos.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class MagicPhotoWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MagicPhotoWidget()
    
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // Start auto-rotation when first widget is added
        WidgetRotationWorker.start(context)
    }
    
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        // Stop auto-rotation when last widget is removed
        WidgetRotationWorker.stop(context)
    }
    
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        // Ensure rotation is running whenever widgets are updated
        WidgetRotationWorker.start(context)
    }
    
    companion object {
        /**
         * Update all widget instances.
         * Call this after sync completes to refresh widgets with new photos.
         */
        suspend fun updateAllWidgets(context: Context) {
            val manager = GlanceAppWidgetManager(context)
            val widget = MagicPhotoWidget()
            val glanceIds = manager.getGlanceIds(MagicPhotoWidget::class.java)
            glanceIds.forEach { glanceId ->
                widget.update(context, glanceId)
            }
        }
    }
}
