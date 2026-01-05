package com.rabbit.magicphotos.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "magic_photos")
data class MagicPhotoEntity(
    @PrimaryKey
    val id: String,
    
    val userId: String,
    val createdOn: Long, // Timestamp in millis
    val modifiedOn: Long,
    val archived: Boolean = false,
    val title: String,
    
    // S3 URIs
    val originalImageS3: String,
    val aiImageS3: String,
    
    // Local file paths (null if not downloaded)
    val originalImageLocal: String? = null,
    val aiImageLocal: String? = null,
    
    // Sync metadata
    val syncedAt: Long? = null,
    val downloadedAt: Long? = null,
    
    // Widget display preference (default true for new photos)
    val showInWidget: Boolean = true
) {
    val isDownloaded: Boolean
        get() = originalImageLocal != null && aiImageLocal != null
    
    val isSynced: Boolean
        get() = syncedAt != null
}

// Converter for dates
fun String.toTimestamp(): Long {
    return try {
        java.time.Instant.parse(this).toEpochMilli()
    } catch (e: Exception) {
        System.currentTimeMillis()
    }
}

