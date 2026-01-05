package com.rabbit.magicphotos.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MagicPhotoEntity::class],
    version = 2,
    exportSchema = false
)
abstract class MagicPhotoDatabase : RoomDatabase() {
    
    abstract fun magicPhotoDao(): MagicPhotoDao
    
    companion object {
        private const val DATABASE_NAME = "magic_photos.db"
        
        @Volatile
        private var INSTANCE: MagicPhotoDatabase? = null
        
        fun getInstance(context: Context): MagicPhotoDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }
        
        private fun buildDatabase(context: Context): MagicPhotoDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                MagicPhotoDatabase::class.java,
                DATABASE_NAME
            )
            .fallbackToDestructiveMigration()
            .build()
        }
    }
}

