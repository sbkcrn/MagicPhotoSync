package com.rabbit.magicphotos.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rabbit.magicphotos.data.local.MagicPhotoEntity
import com.rabbit.magicphotos.data.local.TokenStorage
import com.rabbit.magicphotos.data.repository.PhotoRepository
import com.rabbit.magicphotos.sync.SyncWorker
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = PhotoRepository(application)
    private val tokenStorage = TokenStorage(application)
    
    // UI State
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    
    // Photos
    val photos: StateFlow<List<MagicPhotoEntity>> = repository.allPhotos
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    // Selected photo index for detail view
    private val _selectedIndex = MutableStateFlow(0)
    val selectedIndex: StateFlow<Int> = _selectedIndex.asStateFlow()
    
    // Selected photo for detail view (kept for backward compatibility)
    private val _selectedPhoto = MutableStateFlow<MagicPhotoEntity?>(null)
    val selectedPhoto: StateFlow<MagicPhotoEntity?> = _selectedPhoto.asStateFlow()
    
    init {
        // Check login status
        _uiState.update { it.copy(isLoggedIn = tokenStorage.isLoggedIn) }
        
        // Only sync if logged in AND it's been more than 5 minutes since last sync
        if (tokenStorage.isLoggedIn) {
            val timeSinceLastSync = System.currentTimeMillis() - tokenStorage.lastSyncTime
            val fiveMinutes = 5 * 60 * 1000L
            
            if (timeSinceLastSync > fiveMinutes) {
                syncPhotos()
            }
        }
        
        // Update photo count when photos change
        viewModelScope.launch {
            photos.collect { photoList ->
                _uiState.update { it.copy(photoCount = photoList.size) }
            }
        }
    }
    
    fun onLoginSuccess(token: String, expiry: Long, userId: String, email: String) {
        tokenStorage.saveToken(token, expiry, userId, email)
        _uiState.update { 
            it.copy(
                isLoggedIn = true,
                userEmail = email
            )
        }
        
        // Schedule sync and run immediately
        SyncWorker.schedule(getApplication())
        syncPhotos()
    }
    
    fun syncPhotos() {
        if (_uiState.value.isSyncing) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, error = null) }
            
            val result = repository.syncPhotos()
            
            result.fold(
                onSuccess = { newCount ->
                    _uiState.update { 
                        it.copy(
                            isSyncing = false,
                            lastSyncTime = tokenStorage.lastSyncTime
                        )
                    }
                    
                    // Download photos
                    repository.downloadAllPending()
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(
                            isSyncing = false,
                            error = error.message ?: "Sync failed"
                        )
                    }
                }
            )
        }
    }
    
    fun selectPhoto(photo: MagicPhotoEntity) {
        _selectedPhoto.value = photo
        
        // Find the index in the current photos list
        viewModelScope.launch {
            photos.value.indexOfFirst { it.id == photo.id }.let { index ->
                if (index >= 0) {
                    _selectedIndex.value = index
                }
            }
        }
        
        // Download if not already downloaded
        if (!photo.isDownloaded) {
            viewModelScope.launch {
                repository.downloadPhoto(photo)
                // Refresh the selected photo
                _selectedPhoto.value = repository.getPhotoById(photo.id)
            }
        }
    }
    
    fun updateSelectedIndex(index: Int) {
        _selectedIndex.value = index
        // Also update the selected photo for consistency
        photos.value.getOrNull(index)?.let { photo ->
            _selectedPhoto.value = photo
            
            // Download if not already downloaded
            if (!photo.isDownloaded) {
                viewModelScope.launch {
                    repository.downloadPhoto(photo)
                }
            }
        }
    }
    
    fun clearSelectedPhoto() {
        _selectedPhoto.value = null
        _selectedIndex.value = 0
    }
    
    fun toggleWidgetDisplay(photoId: String, show: Boolean) {
        viewModelScope.launch {
            repository.setShowInWidget(photoId, show)
            // Refresh the selected photo to update UI
            _selectedPhoto.value?.let { photo ->
                if (photo.id == photoId) {
                    _selectedPhoto.value = repository.getPhotoById(photoId)
                }
            }
        }
    }
    
    fun setAutoSync(enabled: Boolean) {
        tokenStorage.autoSyncEnabled = enabled
        _uiState.update { it.copy(autoSyncEnabled = enabled) }
        
        if (enabled) {
            SyncWorker.schedule(getApplication())
        } else {
            SyncWorker.cancel(getApplication())
        }
    }
    
    fun clearCache() {
        viewModelScope.launch {
            repository.clearCache()
        }
    }
    
    fun signOut() {
        viewModelScope.launch {
            // Cancel sync
            SyncWorker.cancel(getApplication())
            
            // Clear data
            repository.clearAll()
            tokenStorage.clear()
            
            // Update state
            _uiState.update {
                MainUiState(isLoggedIn = false)
            }
            _selectedPhoto.value = null
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    // Get current state values
    val isLoggedIn: Boolean get() = tokenStorage.isLoggedIn
    val userEmail: String? get() = tokenStorage.userEmail
    val lastSyncTime: Long get() = tokenStorage.lastSyncTime
    val autoSyncEnabled: Boolean get() = tokenStorage.autoSyncEnabled
}

data class MainUiState(
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val error: String? = null,
    val userEmail: String? = null,
    val lastSyncTime: Long = 0,
    val autoSyncEnabled: Boolean = true,
    val photoCount: Int = 0
)

