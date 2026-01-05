package com.rabbit.magicphotos.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.rabbit.magicphotos.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userEmail: String?,
    lastSyncTime: Long,
    photoCount: Int,
    autoSyncEnabled: Boolean,
    isSyncing: Boolean,
    onBackClick: () -> Unit,
    onSyncNowClick: () -> Unit,
    onAutoSyncToggle: (Boolean) -> Unit,
    onClearCacheClick: () -> Unit,
    onSignOutClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    val scrollState = rememberScrollState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
        ) {
            // Account Section
            SectionHeader("Account")
            
            SettingsItem(
                icon = Icons.Default.Person,
                title = userEmail ?: "Not signed in",
                subtitle = "$photoCount photos synced"
            )
            
            // Sync Section
            SectionHeader("Sync")
            
            SettingsItem(
                icon = Icons.Default.Refresh,
                title = "Sync Now",
                subtitle = if (lastSyncTime > 0) {
                    "Last synced ${dateFormat.format(Date(lastSyncTime))}"
                } else {
                    "Never synced"
                },
                onClick = onSyncNowClick,
                trailing = {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = RabbitOrange
                        )
                    }
                }
            )
            
            SettingsItem(
                icon = Icons.Default.Schedule,
                title = "Auto Sync",
                subtitle = "Sync photos automatically in background",
                trailing = {
                    Switch(
                        checked = autoSyncEnabled,
                        onCheckedChange = onAutoSyncToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = OffWhite,
                            checkedTrackColor = RabbitOrange,
                            uncheckedThumbColor = Beige400,
                            uncheckedTrackColor = Beige200
                        )
                    )
                }
            )
            
            // Storage Section
            SectionHeader("Storage")
            
            SettingsItem(
                icon = Icons.Default.Delete,
                title = "Clear Cache",
                subtitle = "Remove downloaded photos from device",
                onClick = onClearCacheClick
            )
            
            // About Section
            SectionHeader("About")
            
            SettingsItem(
                icon = Icons.Default.Info,
                title = "Magic Photo Sync",
                subtitle = "Version 1.0.0"
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Sign Out Button
            Button(
                onClick = onSignOutClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Error.copy(alpha = 0.1f),
                    contentColor = Error
                )
            ) {
                Icon(
                    Icons.Default.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out")
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = RabbitOrange,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CharcoalMuted,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = Charcoal
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = CharcoalMuted
            )
        }
        
        trailing?.invoke()
    }
    
    Divider(
        modifier = Modifier.padding(start = 56.dp),
        color = Beige200
    )
}
