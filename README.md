# SSwavRecorder

**Super Simple WAV Recorder**


![SSwavRecorder Preview](assets/app_preview.png)


A simple Android voice recorder for when you need a `.wav` file and your phone's built-in recorder doesn't offer that option.

SSwavRecorder records uncompressed, 16-bit PCM audio at 44.1 kHz in mono. No format conversion, no extra steps. Just open the app, record what you need, and get a WAV file.

This is also a small building block for something else I'm working on. More on that later.

## Features

- Record, pause, resume, stop, or discard a recording.
- Live waveform and recording timer.
- Uncompressed WAV output (16-bit PCM, 44.1 kHz, mono).
- Continue recording in the background.
- Pause when another app takes control of the microphone.
- Automatic stop and save when storage is critically low or the battery is low.
- Recover unfinished recordings after an unexpected app termination.
- Play recordings back, seek through them, and adjust playback speed.
- Share recordings through Android's share sheet.
- Delete recordings when you're done with them.

## Get the APK

Don't have Android Studio? You can download a debug APK built by GitHub Actions.

1. Open the repository's **Actions** tab.
2. Select the **Build APK** workflow, or open a completed run triggered by a push.
3. Download the `SSwavRecorder-debug-apk` artifact.
4. Extract the archive and find `app-debug.apk`.
5. Transfer it to your Android phone and install it. You may need to allow installations from your file manager or browser.

The APK is a debug build signed with the standard Android debug key. It's intended for personal use and testing, not Play Store distribution.

## Build locally

### Requirements

- Android Studio Koala or newer.
- JDK 17.
- Android SDK configured for the project.

Open the project in Android Studio, let Gradle sync, and run it on a device or emulator.

You can also build from the project root:

**Windows**
```powershell
.\gradlew.bat assembleDebug
```

**Linux / macOS**
```bash
./gradlew assembleDebug
```

The APK will be generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Tech stack

- Kotlin
- Jetpack Compose and Material 3
- Kotlin Coroutines and Flow
- Hilt
- Room
- Android `AudioRecord` for recording
- Android `MediaPlayer` for playback
- Gradle and GitHub Actions

**Minimum Android version:** Android 8.0 (API 26)  
**Target SDK:** API 34

## How it works

Audio is captured using Android's `AudioRecord` API and written directly to a WAV file as PCM data. The file is initially written as a temporary `.wav.part` file; its WAV header is updated and the file is finalized when recording stops.

The waveform uses microphone amplitude data to visualize the recording in real time.

Recording runs through a foreground service, so it can continue when the app isn't on screen. Audio is periodically flushed to disk, and unfinished files are checked for recovery when the app starts again.

Recovery is best-effort, not a guarantee against every kind of failure.

## Project structure

```text
app/src/main/java/com/example/recorder/
├── audio/
│   ├── RecorderEngine.kt
│   ├── RecordingService.kt
│   ├── WavWriter.kt
│   └── RecoveryManager.kt
├── data/
│   └── Recording.kt
├── di/
│   └── AppModule.kt
└── ui/
    ├── RecorderViewModel.kt
    ├── PlayerViewModel.kt
    ├── RecordScreen.kt
    ├── RecordingsScreen.kt
    └── Waveform.kt
```

## Known limitations

- WAV only: 44.1 kHz, mono, 16-bit PCM.
- No settings screen for changing the recording format.
- Recordings are stored in app-private storage rather than the public Music folder.
- Some automatically stopped recordings may not appear in the in-app library yet.
- Recovery assumes the configured audio format.
- Starting a recording doesn't automatically pause existing playback.

## What's next?

This app started with a fairly specific requirement: get a WAV recording out of an Android phone without having to jump through hoops.

There's a reason I'm being particular about the output format, though.

Let's just say this isn't the end of the project.

## License

No license has been added yet. Until one is included, the repository is publicly viewable, but reuse and redistribution are not automatically granted.
