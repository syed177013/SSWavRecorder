package com.example.recorder.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.recorder.audio.RecState

private val IosRed = Color(0xFFFF3B30)
private val IosOrange = Color(0xFFFF9500)

@Composable
fun RecordScreen(vm: RecorderViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val tick = vm.tick.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    fun act(a: RecAction) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); vm.onAction(a) }

    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(64.dp))
        val s = ui.elapsedMs / 1000
        Text("%02d:%02d.%d".format(s / 60, s % 60, ui.elapsedMs % 1000 / 100),
            fontSize = 64.sp, fontWeight = FontWeight.Bold)
        Text("${ui.bytes / 1024} KB  ·  ${ui.freeBytes shr 20} MB free",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(32.dp))
        Waveform(vm.amps, tick, IosRed)
        ui.notice?.let { Text(it, color = IosRed, modifier = Modifier.padding(top = 12.dp)) }
        Spacer(Modifier.weight(1f))

        // Frosted control sheet (add RenderEffect / Haze on API 31+ for a true blur)
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)).padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            when (ui.state) {
                RecState.Idle -> RecButton { act(RecAction.Start) }
                else -> {
                    TextButton({ act(RecAction.Abort) }) { Text("Abort", color = IosRed) }
                    RoundBtn(if (ui.state == RecState.Recording) "❚❚" else "▶", IosOrange) {
                        act(if (ui.state == RecState.Recording) RecAction.Pause else RecAction.Resume)
                    }
                    RoundBtn("■", IosRed) { act(RecAction.Stop) }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun RecButton(onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val sc by animateFloatAsState(if (pressed) 0.9f else 1f,
        spring(Spring.DampingRatioMediumBouncy), label = "press")
    Box(Modifier.size(76.dp).scale(sc).background(IosRed, CircleShape)
        .clickable { pressed = true; onClick(); pressed = false })
}

@Composable
private fun RoundBtn(label: String, color: Color, onClick: () -> Unit) =
    Button(onClick, shape = CircleShape, modifier = Modifier.size(64.dp),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color)) { Text(label) }
