package de.robinthor.digiworldexplorer

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.*
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import de.robinthor.digiworldexplorer.capture.ScreenCaptureService
import de.robinthor.digiworldexplorer.strategy.AutomationState

class MainActivity:ComponentActivity(){
 private var status by mutableStateOf("Bereit – Automatik ist aus");private var captureActive by mutableStateOf(false);private var autoActive by mutableStateOf(false)
 private val consent=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){r->if(r.resultCode==Activity.RESULT_OK&&r.data!=null){AutomationState.stop();ScreenCaptureService.start(this,r.resultCode,r.data!!);captureActive=true;autoActive=false;status="Analyse aktiv – Automatik ist aus"}else status="Bildschirmfreigabe nicht erteilt"}
 private val notifications=registerForActivityResult(ActivityResultContracts.RequestPermission()){}
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);AutomationState.stop();if(Build.VERSION.SDK_INT>=33&&ContextCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)notifications.launch(Manifest.permission.POST_NOTIFICATIONS);setContent{MaterialTheme{Surface(Modifier.fillMaxSize()){ControlScreen(status,captureActive,autoActive,{startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))},{consent.launch(getSystemService(MediaProjectionManager::class.java).createScreenCaptureIntent())},{if(captureActive){ScreenCaptureService.setAutomation(this,true);autoActive=true;status="AUTOMATIK AKTIV – Stopp jederzeit möglich"}},{ScreenCaptureService.setAutomation(this,false);autoActive=false;status="Analyse aktiv – Automatik gestoppt"},{ScreenCaptureService.stop(this);captureActive=false;autoActive=false;status="Alles gestoppt"})}}}}
}
@Composable private fun ControlScreen(status:String,capture:Boolean,auto:Boolean,onAccess:()->Unit,onCapture:()->Unit,onAuto:()->Unit,onAutoStop:()->Unit,onAllStop:()->Unit){Column(Modifier.fillMaxSize().padding(24.dp),verticalArrangement=Arrangement.Center){Text("DigiWorld Explorer",style=MaterialTheme.typography.headlineMedium);Spacer(Modifier.height(14.dp));Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp)){Text("Status",style=MaterialTheme.typography.labelLarge);Text(status);Text("Raster-Overlay: aktiv")}};Spacer(Modifier.height(16.dp));Button(onClick=onCapture,Modifier.fillMaxWidth()){Text(if(capture)"Bildschirmfreigabe erneuern" else "1. Bildschirmfreigabe starten")};OutlinedButton(onClick=onAccess,Modifier.fillMaxWidth()){Text("2. Bedienungshilfe öffnen")};Spacer(Modifier.height(10.dp));Button(onClick=onAuto,enabled=capture&&!auto,modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=MaterialTheme.colorScheme.tertiary)){Text("3. AUTOMATIK STARTEN")};Button(onClick=onAutoStop,enabled=capture,modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=MaterialTheme.colorScheme.error)){Text("AUTOMATIK SOFORT STOPPEN")};OutlinedButton(onClick=onAllStop,Modifier.fillMaxWidth()){Text("Alles stoppen")};Spacer(Modifier.height(10.dp));Text("Auch über die Android-Benachrichtigung stoppbar. Wegwischen der App stoppt die Automatik.",style=MaterialTheme.typography.bodySmall)}}