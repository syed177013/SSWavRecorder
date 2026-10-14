package com.example.recorder.audio

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Streams PCM16 to disk. Header is written as placeholder, patched on finish/recovery. */
class WavWriter(val file: File, private val sampleRate: Int, private val channels: Int) {
    private val raf = RandomAccessFile(file, "rw")
    var dataBytes = 0L; private set

    init { raf.setLength(0); raf.write(ByteArray(44)) }

    fun write(buf: ByteArray, len: Int) { raf.write(buf, 0, len); dataBytes += len }

    /** Pushes data to disk so a crash loses at most one flush interval. */
    fun flush() { raf.fd.sync() }

    fun finish() { patchHeader(raf, sampleRate, channels); raf.close() }

    companion object {
        fun patchHeader(raf: RandomAccessFile, sampleRate: Int, channels: Int) {
            val data = (raf.length() - 44).coerceAtLeast(0)
            val h = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            h.put("RIFF".toByteArray()).putInt((36 + data).toInt()).put("WAVE".toByteArray())
            h.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(channels.toShort())
            h.putInt(sampleRate).putInt(sampleRate * channels * 2)
            h.putShort((channels * 2).toShort()).putShort(16)
            h.put("data".toByteArray()).putInt(data.toInt())
            raf.seek(0); raf.write(h.array())
        }
    }
}
