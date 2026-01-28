# OP-1 Sync

Android app for synchronizing compositions from Teenage Engineering OP-1 Field.

## Features (MVP)

- 📱 **USB-MTP Connection** - Connect OP-1 Field directly to your phone
- 📂 **File Browser** - Browse tapes, synth patches, drum kits
- ☁️ **Cloud Backup** - Backup to Google Drive (coming soon)
- 🎨 **TE-Inspired Design** - Dark theme with orange accents

## Tech Stack

- **Kotlin** + **Jetpack Compose**
- **Hilt** for dependency injection
- **android.mtp.MtpDevice** for USB-MTP communication
- **Media3/ExoPlayer** for audio playback
- **Room** for local database
- **Google Drive API** for cloud backup

## Building

Open the project in Android Studio and sync Gradle. 

```bash
./gradlew assembleDebug
```

## Project Structure

```
app/
├── core/
│   └── usb/           # MTP connection handling
├── feature/
│   ├── home/          # Main dashboard
│   ├── browser/       # OP-1 file browser
│   ├── library/       # Local library
│   ├── backup/        # Cloud backup
│   └── settings/      # App settings
└── ui/
    └── theme/         # TE-inspired theming
```

## Requirements

- Android 7.0+ (API 24+)
- USB-C OTG support
- OP-1 Field in MTP mode

## License

MIT
