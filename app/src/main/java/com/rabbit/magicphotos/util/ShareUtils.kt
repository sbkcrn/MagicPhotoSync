package com.rabbit.magicphotos.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object ShareUtils {
    
    /**
     * Share a photo using the system share sheet.
     */
    fun sharePhoto(context: Context, imagePath: String, title: String = "Magic Photo") {
        val file = File(imagePath)
        if (!file.exists()) return
        
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "Check out this Magic Photo from my Rabbit R1! 🐰✨")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(
            Intent.createChooser(shareIntent, "Share $title")
        )
    }
    
    /**
     * Share both original and AI-enhanced photos.
     */
    fun sharePhotoPair(
        context: Context, 
        originalPath: String?, 
        aiPath: String?,
        title: String = "Magic Photo"
    ) {
        val uris = ArrayList<Uri>()
        
        originalPath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                uris.add(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
            }
        }
        
        aiPath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                uris.add(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
            }
        }
        
        if (uris.isEmpty()) return
        
        val shareIntent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uris[0])
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/jpeg"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            }
        }
        
        shareIntent.apply {
            putExtra(Intent.EXTRA_TEXT, "Check out this Magic Photo from my Rabbit R1! 🐰✨\n\nOriginal + AI-enhanced")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(
            Intent.createChooser(shareIntent, "Share $title")
        )
    }
    
    /**
     * Save photo to device gallery.
     */
    fun saveToGallery(context: Context, imagePath: String): Boolean {
        return try {
            val file = File(imagePath)
            if (!file.exists()) return false
            
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            // Trigger media scan
            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            mediaScanIntent.data = uri
            context.sendBroadcast(mediaScanIntent)
            
            true
        } catch (e: Exception) {
            false
        }
    }
}

