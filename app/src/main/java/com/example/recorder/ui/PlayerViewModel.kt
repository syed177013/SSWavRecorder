package com.example.recorder.ui

import android.media.MediaPlayer
import android.media.PlaybackParams
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recorder.data.Recording
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerUi(
    val id: Long = -1, val playing: Boolean = false,
    val posMs: Int = 0, val durMs: Int = 0, val speed: Float = 1f,
)

@HiltViewModel
class PlayerViewModel @Inject constructor() : ViewModel() {
    private val _ui = MutableStateFlow(PlayerUi())
    val ui = _ui.asStateFlow()
    private var mp: MediaPlayer? = null
    private var ticker: Job? = null
    private val speeds = listOf(0.5f, 1f, 1.5f, 2f)

    /** Tap on a row: start it, or toggle pause/resume if it is already loaded. */
    fun toggle(r: Recording) {
        val cur = mp
        if (cur != null && _ui.value.id == r.id) {
            if (cur.isPlaying) pause() else resume()
            return
        }
        val speed = _ui.value.speed
        release()
        val p = MediaPlayer()
        try { p.setDataSource(r.path); p.prepare() } catch (e: Exception) { p.release(); return }
        p.setOnCompletionListener {
            ticker?.cancel()
            it.seekTo(0)
            _ui.update { s -> s.copy(playing = false, posMs = 0) }
        }
        mp = p
        _ui.value = PlayerUi(r.id, false, 0, p.duration, speed)
        resume()
    }

    private fun resume() {
        val p = mp ?: return
        p.start()
        applySpeed()
        _ui.update { it.copy(playing = true) }
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (isActive) {
                mp?.let { m -> _ui.update { it.copy(posMs = m.currentPosition) } }
                delay(100)
            }
        }
    }

    private fun pause() {
        mp?.pause(); ticker?.cancel()
        _ui.update { it.copy(playing = false) }
    }

    fun seek(ms: Int) { mp?.seekTo(ms); _ui.update { it.copy(posMs = ms) } }

    fun cycleSpeed() {
        val next = speeds[(speeds.indexOf(_ui.value.speed) + 1) % speeds.size]
        _ui.update { it.copy(speed = next) }
        applySpeed()
    }

    private fun applySpeed() {
        val p = mp ?: return
        val wasPlaying = p.isPlaying
        p.playbackParams = PlaybackParams().setSpeed(_ui.value.speed)
        if (!wasPlaying) p.pause()   // some API levels auto-start when params change
    }

    /** Call before deleting a file so it isn't held open. */
    fun stopIfPlaying(id: Long) { if (_ui.value.id == id) release() }

    private fun release() {
        ticker?.cancel()
        mp?.release(); mp = null
        _ui.update { PlayerUi(speed = it.speed) }
    }

    override fun onCleared() { release() }
}