package com.example.recorder.audio

import android.annotation.SuppressLint
import android.media.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

enum class RecState { Idle, Recording, Paused }

data class RecUi(
    val state: RecState = RecState.Idle,
    val elapsedMs: Long = 0,
    val bytes: Long = 0,
    val freeBytes: Long = 0,
    val notice: String? = null,
)

class RecorderEngine(private val am: AudioManager) {
    private val _ui = MutableStateFlow(RecUi())
    val ui = _ui.asStateFlow()

    /** Preallocated amplitude ring: zero allocation in the hot loop. */
    val amps = FloatArray(AMP_COUNT)
    private val _tick = MutableStateFlow(0L)   // bumped per buffer; Canvas redraws on tick
    val tick = _tick.asStateFlow()
    private var head = 0

    @Volatile private var paused = false
    @Volatile private var stopped = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    private var writer: WavWriter? = null
    private var focus: AudioFocusRequest? = null

    @SuppressLint("MissingPermission")
    fun start(dir: File, rate: Int, stereo: Boolean, minFree: Long = 50L shl 20) {
        if (job != null) return
        val ch = if (stereo) 2 else 1
        val cfg = if (stereo) AudioFormat.CHANNEL_IN_STEREO else AudioFormat.CHANNEL_IN_MONO
        val minBuf = AudioRecord.getMinBufferSize(rate, cfg, AudioFormat.ENCODING_PCM_16BIT)
        val rec = AudioRecord(MediaRecorder.AudioSource.MIC, rate, cfg,
            AudioFormat.ENCODING_PCM_16BIT, minBuf * 2)
        writer = WavWriter(File(dir, "Rec_${System.currentTimeMillis()}.wav.part"), rate, ch)
        paused = false; stopped = false
        requestFocus()
        job = scope.launch {
            val buf = ByteArray(minBuf)  // pooled: one buffer reused for the whole session
            val pcm = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
            var lastFlush = 0L
            rec.startRecording()
            _ui.update { it.copy(state = RecState.Recording) }
            while (isActive && !stopped) {
                val n = rec.read(buf, 0, buf.size)
                if (n <= 0 || paused) continue   // discard reads while paused (mic stays warm)
                val w = writer ?: break
                w.write(buf, n)
                var peak = 0
                for (i in 0 until n / 2) peak = maxOf(peak, abs(pcm.get(i).toInt()))
                amps[head] = peak / 32768f; head = (head + 1) % AMP_COUNT
                _tick.value += 1
                val now = System.nanoTime()
                if (now - lastFlush > 250_000_000L) {
                    lastFlush = now; w.flush()
                    val free = dir.usableSpace
                    _ui.update { it.copy(freeBytes = free) }
                    if (free < minFree) { notice("Storage low. Recording saved."); stopped = true }
                }
                _ui.update { it.copy(bytes = w.dataBytes, elapsedMs = w.dataBytes * 1000 / (rate * ch * 2L)) }
            }
            rec.stop(); rec.release()
        }
    }

    fun pause() { paused = true; _ui.update { it.copy(state = RecState.Paused) } }
    fun resume() { paused = false; _ui.update { it.copy(state = RecState.Recording) } }

    /** Finalizes the file. Returns the finished .wav, or null if aborted/idle. */
    suspend fun stop(abort: Boolean = false): File? {
        stopped = true; job?.join(); job = null
        abandonFocus()
        val w = writer ?: return null
        writer = null
        w.finish()
        _ui.update { RecUi(notice = it.notice) }
        if (abort) { w.file.delete(); return null }
        return File(w.file.path.removeSuffix(".part")).also { w.file.renameTo(it) }
    }

    fun notice(msg: String) = _ui.update { it.copy(notice = msg) }

    private fun requestFocus() {
        val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setOnAudioFocusChangeListener { c ->
                if (c == AudioManager.AUDIOFOCUS_LOSS || c == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                    pause(); notice("Paused: another app is using the microphone")
                }
            }.build()
        focus = req; am.requestAudioFocus(req)
    }
    private fun abandonFocus() { focus?.let { am.abandonAudioFocusRequest(it) } }

    companion object { const val AMP_COUNT = 64 }
}
