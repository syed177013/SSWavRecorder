package com.example.recorder

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.recorder.ui.RecordScreen
import com.example.recorder.ui.RecordingsScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                val perms = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()) {}
                LaunchedEffect(Unit) {
                    perms.launch(arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS))
                }
                var tab by remember { mutableIntStateOf(0) }
                Scaffold(bottomBar = {
                    NavigationBar {
                        NavigationBarItem(tab == 0, { tab = 0 }, { Text("●") }, label = { Text("Record") })
                        NavigationBarItem(tab == 1, { tab = 1 }, { Text("≡") }, label = { Text("Recordings") })
                    }
                }) { pad ->
                    androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.padding(pad)) {
                        if (tab == 0) RecordScreen() else RecordingsScreen()
                    }
                }
            }
        }
    }
}
