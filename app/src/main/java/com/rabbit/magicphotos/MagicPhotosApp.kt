package com.rabbit.magicphotos

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import com.rabbit.magicphotos.data.local.TokenStorage
import com.rabbit.magicphotos.sync.SyncWorker
import com.rabbit.magicphotos.util.NotificationUtils

class MagicPhotosApp : Application(), Configuration.Provider {
    
    lateinit var tokenStorage: TokenStorage
        private set
    
    override fun onCreate() {
        super.onCreate()
        
        tokenStorage = TokenStorage(this)
        
        // Create notification channels
        NotificationUtils.createChannels(this)
        
        // Initialize WorkManager
        WorkManager.initialize(this, workManagerConfiguration)
        
        // Schedule sync if logged in and auto-sync is enabled
        if (tokenStorage.isLoggedIn && tokenStorage.autoSyncEnabled) {
            SyncWorker.schedule(this)
        }
    }
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
