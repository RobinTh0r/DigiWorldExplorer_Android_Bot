package de.robinthor.digiworldexplorer
import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.*
import android.app.LocaleManager
import android.os.LocaleList
import java.util.Locale
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import de.robinthor.digiworldexplorer.accessibility.DigiWorldAccessibilityService
import de.robinthor.digiworldexplorer.capture.ScreenCaptureService
import de.robinthor.digiworldexplorer.strategy.AutomationState
private enum class UiStatus{READY,CAPTURING,AUTOMATIC,CAPTURE_DENIED,STOPPED}
class MainActivity:ComponentActivity(){
 private var status by mutableStateOf(UiStatus.READY);private var capture by mutableStateOf(false);private var auto by mutableStateOf(false);private var grid by mutableStateOf(true);private var access by mutableStateOf(false);private var overlay by mutableStateOf(false)
 private val consent=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){r->if(r.resultCode==Activity.RESULT_OK&&r.data!=null){AutomationState.stop();ScreenCaptureService.start(this,r.resultCode,r.data!!);capture=true;auto=false;status=UiStatus.CAPTURING}else status=UiStatus.CAPTURE_DENIED}
 private val notifications=registerForActivityResult(ActivityResultContracts.RequestPermission()){}
 override fun attachBaseContext(base:Context){if(Build.VERSION.SDK_INT<33){val tag=base.getSharedPreferences("settings",MODE_PRIVATE).getString("language",null);if(tag!=null){val c=base.resources.configuration;c.setLocale(Locale.forLanguageTag(tag));super.attachBaseContext(base.createConfigurationContext(c));return}};super.attachBaseContext(base)}
 override fun onCreate(b:Bundle?){super.onCreate(b);AutomationState.stop();if(Build.VERSION.SDK_INT>=33&&ContextCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)notifications.launch(Manifest.permission.POST_NOTIFICATIONS);setContent{MaterialTheme{Surface(Modifier.fillMaxSize()){Screen(status,capture,auto,grid,access,overlay,{startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))},{startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:$packageName")))},{grid=!grid;AutomationState.overlayEnabled=grid;DigiWorldAccessibilityService.instance?.setOverlayEnabled(grid)},{consent.launch(getSystemService(MediaProjectionManager::class.java).createScreenCaptureIntent())},{if(capture&&access){ScreenCaptureService.setAutomation(this,true);auto=true;status=UiStatus.AUTOMATIC}},{ScreenCaptureService.setAutomation(this,false);auto=false;status=UiStatus.CAPTURING},{ScreenCaptureService.stop(this);capture=false;auto=false;status=UiStatus.STOPPED},{setLanguage(it)},{startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(getString(R.string.footer_repo_url))))})}}}}
 private fun setLanguage(tag:String){getSharedPreferences("settings",MODE_PRIVATE).edit().putString("language",tag).apply();if(Build.VERSION.SDK_INT>=33)getSystemService(LocaleManager::class.java).applicationLocales=LocaleList.forLanguageTags(tag) else {val c=resources.configuration;c.setLocale(Locale.forLanguageTag(tag));resources.updateConfiguration(c,resources.displayMetrics);recreate()}}
 override fun onResume(){super.onResume();access=(getSystemService(Context.ACCESSIBILITY_SERVICE)as AccessibilityManager).getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK).any{it.resolveInfo.serviceInfo.packageName==packageName&&it.resolveInfo.serviceInfo.name==DigiWorldAccessibilityService::class.java.name};overlay=Settings.canDrawOverlays(this)}
}
@Composable private fun Screen(s:UiStatus,capture:Boolean,auto:Boolean,grid:Boolean,access:Boolean,overlay:Boolean,onAccess:()->Unit,onOverlay:()->Unit,onGrid:()->Unit,onCapture:()->Unit,onStart:()->Unit,onStop:()->Unit,onAll:()->Unit,onLang:(String)->Unit,onRepo:()->Unit){val st=when(s){UiStatus.READY->R.string.status_ready;UiStatus.CAPTURING->R.string.status_capture;UiStatus.AUTOMATIC->R.string.status_auto;UiStatus.CAPTURE_DENIED->R.string.status_capture_denied;UiStatus.STOPPED->R.string.status_stopped};Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(stringResource(R.string.app_title),style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);Text("v${BuildConfig.VERSION_NAME}",style=MaterialTheme.typography.labelSmall)};Text(if(auto)"●" else "○")};Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp)){Text(stringResource(R.string.status_title),fontWeight=FontWeight.Bold);Text(stringResource(st))}};Text(stringResource(R.string.setup_title),fontWeight=FontWeight.Bold);Permission(1,R.string.permission_accessibility,access,onAccess);Permission(2,R.string.permission_overlay,overlay,onOverlay);Button(onClick=onCapture,Modifier.fillMaxWidth()){Text(stringResource(if(capture)R.string.capture_renew else R.string.capture_start))};Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(onClick=onGrid,enabled=overlay,modifier=Modifier.weight(1f)){Text(stringResource(if(grid)R.string.grid_hide else R.string.grid_show))};Button(onClick=onStart,enabled=capture&&access&&!auto,modifier=Modifier.weight(1f)){Text(stringResource(R.string.auto_start))}};Button(onClick=onStop,enabled=auto,modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=MaterialTheme.colorScheme.error)){Text(stringResource(R.string.auto_stop))};OutlinedButton(onClick=onAll,Modifier.fillMaxWidth()){Text(stringResource(R.string.stop_all))};Text(stringResource(R.string.safety_note),style=MaterialTheme.typography.bodySmall);Text(stringResource(R.string.character_tip),style=MaterialTheme.typography.bodySmall);Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){TextButton(onClick=onRepo,modifier=Modifier.weight(1f)){Text(stringResource(R.string.source_code))};TextButton(onClick={onLang("de")}){Text("🇩🇪 DE")};TextButton(onClick={onLang("en")}){Text("🇬🇧 EN")}}}}
@Composable private fun Permission(n:Int,label:Int,ok:Boolean,click:()->Unit){OutlinedButton(onClick=click,Modifier.fillMaxWidth()){Text("$n. ${stringResource(label)}",Modifier.weight(1f));Text(stringResource(if(ok)R.string.permission_granted else R.string.permission_missing),fontWeight=FontWeight.Bold)}}