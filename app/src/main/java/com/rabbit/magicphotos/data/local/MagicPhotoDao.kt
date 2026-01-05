package com.rabbit.magicphotos.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MagicPhotoDao {
    
    @Query("SELECT * FROM magic_photos WHERE archived = 0 ORDER BY createdOn DESC")
    fun getAllPhotos(): Flow<List<MagicPhotoEntity>>
    
    @Query("SELECT * FROM magic_photos WHERE archived = 0 ORDER BY createdOn DESC LIMIT :limit")
    fun getRecentPhotos(limit: Int): Flow<List<MagicPhotoEntity>>
    
    @Query("SELECT * FROM magic_photos WHERE id = :id")
    suspend fun getPhotoById(id: String): MagicPhotoEntity?
    
    @Query("SELECT * FROM magic_photos WHERE archived = 0 ORDER BY createdOn DESC LIMIT 1")
    suspend fun getLatestPhoto(): MagicPhotoEntity?
    
    @Query("SELECT * FROM magic_photos WHERE archived = 0 ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomPhoto(): MagicPhotoEntity?
    
    @Query("SELECT * FROM magic_photos WHERE originalImageLocal IS NULL OR aiImageLocal IS NULL")
    suspend fun getPhotosNeedingDownload(): List<MagicPhotoEntity>
    
    @Query("SELECT * FROM magic_photos WHERE aiImageLocal IS NOT NULL AND archived = 0 ORDER BY createdOn DESC")
    suspend fun getDownloadedPhotos(): List<MagicPhotoEntity>
    
    @Query("SELECT * FROM magic_photos WHERE aiImageLocal IS NOT NULL AND archived = 0 AND showInWidget = 1 ORDER BY createdOn DESC LIMIT 15")
    suspend fun getWidgetPhotos(): List<MagicPhotoEntity>
    
    @Query("UPDATE magic_photos SET showInWidget = :show WHERE id = :id")
    suspend fun setShowInWidget(id: String, show: Boolean)
    
    @Query("SELECT COUNT(*) FROM magic_photos WHERE archived = 0")
    suspend fun getPhotoCount(): Int
    
    @Query("SELECT id FROM magic_photos")
    suspend fun getAllIds(): List<String>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: MagicPhotoEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotos(photos: List<MagicPhotoEntity>)
    
    @Update
    suspend fun updatePhoto(photo: MagicPhotoEntity)
    
    @Query("UPDATE magic_photos SET originalImageLocal = :localPath WHERE id = :id")
    suspend fun updateOriginalLocalPath(id: String, localPath: String)
    
    @Query("UPDATE magic_photos SET aiImageLocal = :localPath WHERE id = :id")
    suspend fun updateAiLocalPath(id: String, localPath: String)
    
    @Query("UPDATE magic_photos SET downloadedAt = :timestamp WHERE id = :id")
    suspend fun updateDownloadedAt(id: String, timestamp: Long)
    
    @Query("UPDATE magic_photos SET archived = 1 WHERE id = :id")
    suspend fun archivePhoto(id: String)
    
    @Query("UPDATE magic_photos SET archived = 1 WHERE id NOT IN (:activeIds)")
    suspend fun archivePhotosNotIn(activeIds: List<String>)
    
    @Query("DELETE FROM magic_photos WHERE id = :id")
    suspend fun deletePhoto(id: String)
    
    @Query("DELETE FROM magic_photos")
    suspend fun deleteAllPhotos()
    
    @Query("UPDATE magic_photos SET originalImageLocal = NULL, aiImageLocal = NULL, downloadedAt = NULL")
    suspend fun clearAllLocalPaths()
}

