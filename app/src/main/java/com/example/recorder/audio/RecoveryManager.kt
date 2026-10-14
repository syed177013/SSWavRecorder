package com.example.recorder.audio

import java.io.File
import java.io.RandomAccessFile

/** Repairs orphaned .wav.part files left by a crash/force-kill. Call on app start. */
object RecoveryManager {
    fun recover(dir: File, sampleRate: Int = 44100, channels: Int = 1): List<File> {
        val out = File(dir, "Recovered Recordings").apply { mkdirs() }
        return dir.listFiles { f -> f.name.endsWith(".wav.part") }.orEmpty().mapNotNull { part ->
            if (part.length() <= 44) { part.delete(); return@mapNotNull null }
            RandomAccessFile(part, "rw").use { WavWriter.patchHeader(it, sampleRate, channels) }
            File(out, part.name.removeSuffix(".part")).also { part.renameTo(it) }
        }
    }
}
