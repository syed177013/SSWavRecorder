package com.example.recorder.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/**
 * `tick` is read only inside the draw scope, so each audio buffer triggers a
 * redraw, not a recomposition. Nothing is allocated per frame.
 */
@Composable
fun Waveform(amps: FloatArray, tick: State<Long>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxWidth().height(120.dp)) {
        tick.value
        val n = amps.size
        val step = size.width / n
        val mid = size.height / 2
        for (i in 0 until n) {
            val h = (amps[i] * size.height).coerceAtLeast(4f)
            val x = step * i + step / 2
            drawLine(color, Offset(x, mid - h / 2), Offset(x, mid + h / 2),
                strokeWidth = step * 0.55f, cap = StrokeCap.Round)
        }
    }
}
