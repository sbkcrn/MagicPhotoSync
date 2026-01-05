# Magic Photo Sync 🥕✨

An Android app to sync and display Magic Photos from your Rabbit R1 device, with a home screen widget.

## Download

📥 **[Download Latest APK](releases/MagicPhotoSync-v1.0.0.apk)**

## Features

### 📸 Photo Gallery
- View all your Magic Photos in a clean grid
- Photos cached locally for instant loading
- Swipe down to manually sync new photos

### 🔍 Photo Viewer
- **Vertical swipe** - Navigate between photos
- **Horizontal swipe** - Toggle between Magic (AI-enhanced) and Original versions
- **Widget toggle** - Select which photos appear in the widget
- **Save to gallery** - Download photos to your device's photo gallery
- **Share** - Share photos individually or as pairs

### 🖼️ Home Screen Widget
- Default 3×3 size with navigation controls
- Auto-rotates through selected photos every 30 seconds
- Navigate manually with ◀ ▶ buttons
- Shows up to 15 most recent selected photos
- Black background with orange accent border
- Tap to open app

### 🔄 Smart Sync
- Only syncs when needed (5+ minute cooldown)
- Background sync via WorkManager
- Battery and network-aware

### 🎨 Dark Theme
- Pure black background
- Leuchtorange (#FF4D06) accents
- Clean, modern UI

### 🔐 Secure
- Login via official Rabbit Hole website
- Encrypted token storage
- Photos stored locally on device

## Installation

### From APK
1. Download `MagicPhotoSync-v1.0.0.apk` from the [releases](releases/) folder
2. Enable "Install from unknown sources" on your Android device
3. Install the APK
4. Open the app and log in with your Rabbit Hole account

### Build from Source

**Requirements:**
- JDK 17
- Android SDK 34
- Min SDK: API 26 (Android 8.0)

```bash
# Clone the repository
git clone https://github.com/sbkcrn/MagicPhotoSync.git
cd MagicPhotoSync

# Build debug APK
./gradlew assembleDebug

# APK will be at: app/build/outputs/apk/debug/app-debug.apk
```

## Usage

1. **Login** - Open the app and sign in with your Rabbit Hole credentials
2. **Sync** - Photos will sync automatically, or tap the refresh icon
3. **Browse** - Swipe through your photos vertically
4. **Compare** - Swipe horizontally to see Magic vs Original
5. **Widget** - Long-press home screen → Widgets → Magic Photo Sync
6. **Select for Widget** - Tap the widget icon (⊞) on any photo to include/exclude it from the widget

## Architecture

```
app/src/main/java/com/rabbit/magicphotos/
├── data/
│   ├── api/           # Retrofit API client
│   ├── local/         # Room database & token storage
│   └── repository/    # Data repository
├── sync/              # Background sync worker
├── ui/
│   ├── screens/       # Compose screens (login, gallery, detail, settings)
│   └── theme/         # Dark theme with Leuchtorange accents
├── util/              # Share & notification utilities
└── widget/            # Glance app widget
```

## Tech Stack

| Component | Library |
|-----------|---------|
| UI | Jetpack Compose + Material 3 |
| Database | Room |
| Networking | Retrofit + OkHttp |
| Image Loading | Coil (with caching) |
| Background Work | WorkManager |
| Widget | Glance |
| Serialization | Kotlinx Serialization |

## License

MIT License - feel free to use and modify.

---

**Note:** This is a community project and is not affiliated with Rabbit Inc.
