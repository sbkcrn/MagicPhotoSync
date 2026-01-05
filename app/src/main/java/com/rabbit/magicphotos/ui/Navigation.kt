package com.rabbit.magicphotos.ui

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rabbit.magicphotos.ui.screens.detail.DetailScreen
import com.rabbit.magicphotos.ui.screens.gallery.GalleryScreen
import com.rabbit.magicphotos.ui.screens.login.LoginScreen
import com.rabbit.magicphotos.ui.screens.settings.SettingsScreen

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Gallery : Screen("gallery")
    data object Detail : Screen("detail")
    data object Settings : Screen("settings")
}

@Composable
fun MagicPhotosNavigation(
    viewModel: MainViewModel = viewModel()
) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsState()
    val photos by viewModel.photos.collectAsState()
    val selectedPhoto by viewModel.selectedPhoto.collectAsState()
    
    // Determine start destination
    val startDestination = if (viewModel.isLoggedIn) {
        Screen.Gallery.route
    } else {
        Screen.Login.route
    }
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Login Screen
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = { token, expiry, userId, email ->
                    viewModel.onLoginSuccess(token, expiry, userId, email)
                    navController.navigate(Screen.Gallery.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onDismiss = { /* Can't dismiss login screen */ }
            )
        }
        
        // Gallery Screen
        composable(Screen.Gallery.route) {
            GalleryScreen(
                photos = photos,
                isLoading = uiState.isLoading,
                isSyncing = uiState.isSyncing,
                onPhotoClick = { photo ->
                    viewModel.selectPhoto(photo)
                    navController.navigate(Screen.Detail.route)
                },
                onSyncClick = { viewModel.syncPhotos() },
                onSettingsClick = { navController.navigate(Screen.Settings.route) }
            )
        }
        
        // Detail Screen
        composable(Screen.Detail.route) {
            val selectedIndex by viewModel.selectedIndex.collectAsState()
            
            DetailScreen(
                photos = photos,
                initialIndex = selectedIndex,
                onBackClick = {
                    viewModel.clearSelectedPhoto()
                    navController.popBackStack()
                },
                onShareClick = { /* TODO: Implement share */ },
                onToggleWidget = { photoId, show ->
                    viewModel.toggleWidgetDisplay(photoId, show)
                },
                onPhotoChanged = { index ->
                    viewModel.updateSelectedIndex(index)
                }
            )
        }
        
        // Settings Screen
        composable(Screen.Settings.route) {
            SettingsScreen(
                userEmail = viewModel.userEmail,
                lastSyncTime = viewModel.lastSyncTime,
                photoCount = uiState.photoCount,
                autoSyncEnabled = viewModel.autoSyncEnabled,
                isSyncing = uiState.isSyncing,
                onBackClick = { navController.popBackStack() },
                onSyncNowClick = { viewModel.syncPhotos() },
                onAutoSyncToggle = { viewModel.setAutoSync(it) },
                onClearCacheClick = { viewModel.clearCache() },
                onSignOutClick = {
                    viewModel.signOut()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}

