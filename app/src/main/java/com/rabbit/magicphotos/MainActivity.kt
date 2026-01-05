package com.rabbit.magicphotos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.rabbit.magicphotos.ui.MagicPhotosNavigation
import com.rabbit.magicphotos.ui.theme.MagicPhotoSyncTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            MagicPhotoSyncTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MagicPhotosNavigation()
                }
            }
        }
    }
}

