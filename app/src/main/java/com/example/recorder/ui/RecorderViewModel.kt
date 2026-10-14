package com.example.recorder.ui

import android.app.Application
import android.content.*
import android.os.IBinder
import androidx.lifecycle.*
import com.example.recorder.audio.*
import com.example.recorder.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface RecAction {
    object Start : RecAction; object Pause : RecAction; object Resume : RecAction
    object Stop : RecAction; object Abort : RecAction
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RecorderViewModel @Inject constructor(
    private val app: Application, private val dao: RecordingDao,
) : AndroidViewModel(app) {

    private val engineFlow = MutableStateFlow<RecorderEngine?>(null)
    val ui: StateFlow<RecUi> = engineFlow.flatMapLatest { it?.ui ?: flowOf(RecUi()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RecUi())
    val tick: StateFlow<Long> = engineFlow.flatMapLatest { it?.tick ?: flowOf(0L) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
    private val empty = FloatArray(RecorderEngine.AMP_COUNT)
    val amps get() = engineFlow.value?.amps ?: empty
    val recordings = dao.all().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val dir get() = app.getExternalFilesDir("recordings")!!

    private val conn = object : ServiceConnection {
        override fun onServiceConnected(n: ComponentName, b: IBinder) {
            engineFlow.value = (b as RecordingService.LocalBinder).engine
        }
        override fun onServiceDisconnected(n: ComponentName) { engineFlow.value = null }
    }

    init {
        RecoveryManager.recover(dir)
        app.bindService(Intent(app, RecordingService::class.java), conn, Context.BIND_AUTO_CREATE)
    }

    fun onAction(a: RecAction) {
        val e = engineFlow.value ?: return
        when (a) {
            RecAction.Start -> {
                app.startForegroundService(Intent(app, RecordingService::class.java))
                e.start(dir, 44100, false)
            }
            RecAction.Pause -> e.pause()
            RecAction.Resume -> e.resume()
            RecAction.Abort -> viewModelScope.launch { e.stop(abort = true) }
            RecAction.Stop -> viewModelScope.launch {
                val dur = e.ui.value.elapsedMs
                e.stop()?.let { f ->
                    dao.insert(Recording(title = f.nameWithoutExtension, path = f.path,
                        durationMs = dur, createdAt = System.currentTimeMillis(),
                        format = "wav", sizeBytes = f.length()))
                }
            }
        }
    }

    fun delete(r: Recording) = viewModelScope.launch {
        java.io.File(r.path).delete(); dao.delete(r)
    }

    override fun onCleared() { app.unbindService(conn) }
}
