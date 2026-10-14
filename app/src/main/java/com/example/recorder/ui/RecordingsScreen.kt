package com.example.recorder.ui

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.recorder.data.Recording
import java.io.File

private fun fmt(ms: Int): String { val s = ms / 1000; return "%d:%02d".format(s / 60, s % 60) }

@Composable
fun RecordingsScreen(
    vm: RecorderViewModel = hiltViewModel(),
    player: PlayerViewModel = hiltViewModel(),
) {
    val list by vm.recordings.collectAsStateWithLifecycle()
    val p by player.ui.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    var toDelete by remember { mutableStateOf<Recording?>(null) }

    if (list.isEmpty()) Box(Modifier.fillMaxSize(), Alignment.Center) { Text("No recordings yet") }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(list, key = { it.id }) { r ->
            val active = p.id == r.id
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilledTonalIconButton({ player.toggle(r) }) {
                            Text(if (active && p.playing) "❚❚" else "▶")
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(r.title, style = MaterialTheme.typography.titleMedium)
                            Text("${fmt(r.durationMs.toInt())}  ·  ${r.sizeBytes / 1024} KB",
                                style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton({
                            val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", File(r.path))
                            ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                type = "audio/wav"; putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }, "Share"))
                        }) { Text("Share") }
                        TextButton({ toDelete = r }) { Text("Delete") }
                    }
                    if (active) {
                        // Local drag state so the thumb follows the finger without seeking on every move
                        var drag by remember { mutableStateOf<Float?>(null) }
                        val dur = p.durMs.coerceAtLeast(1)
                        Slider(
                            value = drag ?: (p.posMs.toFloat() / dur),
                            onValueChange = { drag = it },
                            onValueChangeFinished = { drag?.let { player.seek((it * dur).toInt()) }; drag = null },
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(fmt(((drag ?: (p.posMs.toFloat() / dur)) * dur).toInt()),
                                style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.weight(1f))
                            OutlinedButton({ player.cycleSpeed() }) { Text("${p.speed}x") }
                            Spacer(Modifier.weight(1f))
                            Text(fmt(p.durMs), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
    toDelete?.let { r ->
        AlertDialog(onDismissRequest = { toDelete = null },
            title = { Text("Delete recording?") }, text = { Text(r.title) },
            confirmButton = { TextButton({
                player.stopIfPlaying(r.id); vm.delete(r); toDelete = null
            }) { Text("Delete") } },
            dismissButton = { TextButton({ toDelete = null }) { Text("Cancel") } })
    }
}