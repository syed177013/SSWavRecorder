# iOS-style Sound Recorder (Kotlin / Compose / Hilt / Room)

```
app/src/main/java/com/example/recorder/
├── data/        Recording.kt (Room entity+DAO+DB), SettingsStore.kt (DataStore)
├── audio/       WavWriter.kt, RecorderEngine.kt, RecordingService.kt, RecoveryManager.kt
├── ui/          RecorderViewModel.kt, Waveform.kt, RecordScreen.kt
└── di/          (Hilt modules: provide Room DB, DAO, DataStore)
```
Flow: UI -> ViewModel -> (bound) RecordingService -> RecorderEngine -> WavWriter.
State flows back up via StateFlow (unidirectional data flow).

## Manifest essentials
```xml
<uses-permission android:name="android.permission.RECORD_AUDIO"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE"/>
<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
<service android:name=".audio.RecordingService"
    android:foregroundServiceType="microphone" android:exported="false"/>
```
Activity: `android:configChanges="orientation|screenSize|smallestScreenSize|screenLayout"`; the
recording lives in the service, so rotation/split-screen never touches the stream.

## Crash recovery (honest caveat)
No app can run code on SIGKILL. The design instead guarantees minimal loss:
1. Audio is written to `name.wav.part` with a placeholder header and flushed every ~250 ms.
2. Swipe-away -> `onTaskRemoved` / `onDestroy` finalize cleanly.
3. After a hard kill/crash, `RecoveryManager` runs at next launch, rebuilds the header from the
   file length, and moves the file to `Recovered Recordings/`. Loss is at most one flush window.

## Dependencies
hilt, room (+ksp), datastore-preferences, compose BOM, lifecycle-viewmodel-compose, material3.
