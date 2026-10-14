package com.example.recorder.audio

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.os.*
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class RecordingService : Service() {
    inner class LocalBinder : Binder() { val engine get() = this@RecordingService.engine }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    lateinit var engine: RecorderEngine; private set

    private val battery = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            engine.notice("Battery low. Recording saved.")
            scope.launch { engine.stop() }
        }
    }

    override fun onCreate() {
        engine = RecorderEngine(getSystemService(AudioManager::class.java))
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel("rec", "Recording", NotificationManager.IMPORTANCE_LOW))
        registerReceiver(battery, IntentFilter(Intent.ACTION_BATTERY_LOW))
    }

    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int {
        val n = NotificationCompat.Builder(this, "rec").setContentTitle("Recording…")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now).setOngoing(true).build()
        androidx.core.app.ServiceCompat.startForeground(this, 1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        return START_NOT_STICKY
    }

    override fun onBind(i: Intent?): IBinder = LocalBinder()

    /** Swiped from recents: finalize synchronously so the WAV header is valid. */
    override fun onTaskRemoved(rootIntent: Intent?) { runBlocking { engine.stop() }; stopSelf() }

    override fun onDestroy() {
        runBlocking { engine.stop() }
        unregisterReceiver(battery); scope.cancel()
    }
}
