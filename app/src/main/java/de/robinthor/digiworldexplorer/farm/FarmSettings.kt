package de.robinthor.digiworldexplorer.farm

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import de.robinthor.digiworldexplorer.R
import de.robinthor.digiworldexplorer.strategy.AutomationState

@Composable
fun FarmSettings() {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    var enabled by remember { mutableStateOf(preferences.getBoolean("auto_farm_harvest", false)) }
    var watering by remember { mutableStateOf(preferences.getBoolean("farm_watering", true)) }
    var adSkipPass by remember { mutableStateOf(preferences.getBoolean("ad_skip_pass", false)) }
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.farm_harvest_title), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.farm_harvest_hint), style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = enabled, onCheckedChange = {
                enabled = it
                AutomationState.autoFarmEnabled = it
                FarmHarvestAnalyzer.reset()
                preferences.edit().putBoolean("auto_farm_harvest", it).apply()
            })
        }
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.farm_watering_title), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.farm_watering_hint), style = MaterialTheme.typography.labelSmall)
            }
            Switch(checked = watering, onCheckedChange = {
                watering = it
                AutomationState.farmWateringEnabled = it
                preferences.edit().putBoolean("farm_watering", it).apply()
            })
        }
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.ad_skip_pass_title), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.ad_skip_pass_hint), style = MaterialTheme.typography.labelSmall)
            }
            Switch(checked = adSkipPass, onCheckedChange = {
                adSkipPass = it
                AutomationState.adSkipPassEnabled = it
                preferences.edit().putBoolean("ad_skip_pass", it).apply()
            })
        }
    }
}
