package com.rabbit.magicphotos.data.repository

import android.content.Context
import com.rabbit.magicphotos.data.api.ApiClient
import com.rabbit.magicphotos.data.api.models.FetchJournalRequest
import com.rabbit.magicphotos.data.api.models.JournalEntry
import com.rabbit.magicphotos.data.api.models.isMagicCamera
import com.rabbit.magicphotos.data.local.MagicPhotoDao
import com.rabbit.magicphotos.data.local.MagicPhotoDatabase
import com.rabbit.magicphotos.data.local.MagicPhotoEntity
import com.rabbit.magicphotos.data.local.TokenStorage
import com.rabbit.magicphotos.data.local.toTimestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class PhotoRepository(context: Context) {
    
    private val api = ApiClient.rabbitHoleApi
    private val downloadClient = ApiClient.downloadClient
    private val dao: MagicPhotoDao = MagicPhotoDatabase.getInstance(context).magicPhotoDao()
    private val tokenStorage = TokenStorage(context)
    private val photosDir = File(context.filesDir, "photos").also { it.mkdirs() }
    
    val allPhotos: Flow<List<MagicPhotoEntity>> = dao.getAllPhotos()
    
    fun getRecentPhotos(limit: Int): Flow<List<MagicPhotoEntity>> = dao.getRecentPhotos(limit)
    
    suspend fun getPhotoById(id: String): MagicPhotoEntity? = dao.getPhotoById(id)
    
    suspend fun getLatestPhoto(): MagicPhotoEntity? = dao.getLatestPhoto()
    
    suspend fun getRandomPhoto(): MagicPhotoEntity? = dao.getRandomPhoto()
    
    suspend fun getPhotoCount(): Int = dao.getPhotoCount()
    
    suspend fun setShowInWidget(id: String, show: Boolean) {
        dao.setShowInWidget(id, show)
    }
    
    /**
     * Sync photos from Rabbit Hole API.
     * Returns the number of new photos synced.
     */
    suspend fun syncPhotos(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val token = tokenStorage.accessToken 
                ?: return@withContext Result.failure(Exception("Not logged in"))
            
            // Fetch all journal entries
            val response = api.fetchUserJournal(FetchJournalRequest(accessToken = token))
            
            // Filter only magic camera entries
            val magicCameraEntries = response.journal.entries.filter { it.isMagicCamera() }
            
            // Get existing IDs
            val existingIds = dao.getAllIds().toSet()
            
            // Convert to entities
            val entities = magicCameraEntries.mapNotNull { entry ->
                entry.toEntity()
            }
            
            // Count new photos
            val newPhotos = entities.filter { it.id !in existingIds }
            
            // Insert/update all
            dao.insertPhotos(entities)
            
            // Archive photos that are no longer in the response
            val activeIds = entities.map { it.id }
            dao.archivePhotosNotIn(activeIds)
            
            // Update last sync time
            tokenStorage.lastSyncTime = System.currentTimeMillis()
            
            Result.success(newPhotos.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Download images for a photo entry.
     */
    suspend fun downloadPhoto(photo: MagicPhotoEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val token = tokenStorage.accessToken 
                ?: return@withContext Result.failure(Exception("Not logged in"))
            
            // Get signed URLs for both images
            val s3Urls = listOf(photo.originalImageS3, photo.aiImageS3)
            val urlsJson = Json.encodeToString(s3Urls)
            
            val response = api.fetchJournalEntryResources(token, urlsJson)
            
            if (response.resources.size < 2) {
                return@withContext Result.failure(Exception("Failed to get signed URLs"))
            }
            
            // Download original image
            val originalUrl = response.resources[0]
            val originalFile = File(photosDir, "${photo.id}_original.jpg")
            downloadFile(originalUrl, originalFile)
            dao.updateOriginalLocalPath(photo.id, originalFile.absolutePath)
            
            // Download AI image
            val aiUrl = response.resources[1]
            val aiFile = File(photosDir, "${photo.id}_ai.jpg")
            downloadFile(aiUrl, aiFile)
            dao.updateAiLocalPath(photo.id, aiFile.absolutePath)
            
            // Update downloaded timestamp
            dao.updateDownloadedAt(photo.id, System.currentTimeMillis())
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Download all photos that haven't been downloaded yet.
     */
    suspend fun downloadAllPending(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val pending = dao.getPhotosNeedingDownload()
            var downloaded = 0
            
            for (photo in pending) {
                val result = downloadPhoto(photo)
                if (result.isSuccess) downloaded++
            }
            
            Result.success(downloaded)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Clear all downloaded images.
     */
    suspend fun clearCache(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Delete all files in photos directory
            photosDir.listFiles()?.forEach { it.delete() }
            
            // Clear local paths in database
            dao.clearAllLocalPaths()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete all data (for sign out).
     */
    suspend fun clearAll(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            photosDir.listFiles()?.forEach { it.delete() }
            dao.deleteAllPhotos()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun downloadFile(url: String, destination: File) {
        val request = Request.Builder().url(url).build()
        val response = downloadClient.newCall(request).execute()
        
        if (!response.isSuccessful) {
            throw Exception("Download failed: ${response.code}")
        }
        
        response.body?.byteStream()?.use { input ->
            FileOutputStream(destination).use { output ->
                input.copyTo(output)
            }
        }
    }
    
    private fun JournalEntry.toEntity(): MagicPhotoEntity? {
        val magicData = data?.magicCameraData ?: return null
        val originalUrl = magicData.originalImage?.url ?: return null
        val aiUrl = magicData.aiGeneratedImages.firstOrNull()?.url ?: return null
        
        return MagicPhotoEntity(
            id = id,
            userId = userId,
            createdOn = createdOn.toTimestamp(),
            modifiedOn = modifiedOn.toTimestamp(),
            archived = archived,
            title = title,
            originalImageS3 = originalUrl,
            aiImageS3 = aiUrl,
            syncedAt = System.currentTimeMillis()
        )
    }
}

