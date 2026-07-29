package de.robinthor.digiworldexplorer

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import de.robinthor.digiworldexplorer.capture.ScreenCaptureService

class MainActivity : ComponentActivity() {
    private var status by mutableStateOf("Bereit – noch keine Bildschirmfreigabe")

    private val captureConsent = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            ScreenCaptureService.start(this, result.resultCode, result.data!!)
            status = "Bildschirmfreigabe aktiv – Automatik bleibt pausiert"
        } else {
            status = "Bildschirmfreigabe wurde nicht erteilt"
        }
    }

    private val notificationsPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ControlScreen(
                        status = status,
                        onAccessibilitySettings = {
                            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        },
                        onStartCapture = { requestCapture() },
                        onStop = {
                            ScreenCaptureService.stop(this)
                            status = "Gestoppt – Bildschirmfreigabe beendet"
                        },
                    )
                }
            }
        }
    }

    private fun requestCapture() {
        val manager = getSystemService(MediaProjectionManager::class.java)
        captureConsent.launch(manager.createScreenCaptureIntent())
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationsPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@androidx.compose.runtime.Composable
private fun ControlScreen(
    status: String,
    onAccessibilitySettings: () -> Unit,
    onStartCapture: () -> Unit,
    onStop: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("DigiWorld Explorer", style = MaterialTheme.typography.headlineMedium)
        Text("Android Development Spike", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(20.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text("Status", style = MaterialTheme.typography.labelLarge)
                Text(status)
                Spacer(Modifier.height(8.dp))
                Text("Sicherheitsmodus: Keine automatischen Gesten")
            }
        }
        Spacer(Modifier.height(20.dp))
        Button(onClick = onStartCapture, modifier = Modifier.fillMaxWidth()) {
            Text("Bildschirmfreigabe starten")
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onAccessibilitySettings, modifier = Modifier.fillMaxWidth()) {
            Text("Bedienungshilfe öffnen")
        }
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth()) {
                Text("Stopp")
            }
        }
    }
}
