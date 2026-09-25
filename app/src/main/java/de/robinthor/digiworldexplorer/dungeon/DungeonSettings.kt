package de.robinthor.digiworldexplorer.dungeon

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.robinthor.digiworldexplorer.R
import de.robinthor.digiworldexplorer.strategy.AutomationState

data class DungeonRotationSettings(
    val enabledCards: Set<DungeonKey> = DungeonKey.entries.toSet(),
    val normalAttempts: Int = 2,
    val useAdAttempts: Boolean = true,
) {
    init { require(normalAttempts in 1..2) }
    fun budget(key: DungeonKey, adSkipPass: Boolean, completedToday: Set<DungeonKey> = emptySet()): DungeonBudget = when {
        key in completedToday -> DungeonBudget()
        else -> when (key) {
        DungeonKey.APOCALYMON_WALL -> DungeonBudget(if (key in enabledCards) 1 else 0)
        DungeonKey.DAILY -> DungeonBudget(if (key in enabledCards) 1 else 0)
        else -> {
            val normal = if (key in enabledCards) normalAttempts else 0
            val ads = if (normal > 0 && useAdAttempts && adSkipPass) 2 else 0
            DungeonBudget(attempts = normal + ads, adTickets = ads)
        }
        }
    }
}

object DungeonSettingsStore {
    private const val NORMAL_ATTEMPTS = "dungeon_normal_attempts"
    private const val USE_ADS = "dungeon_use_ads"
    private fun cardKey(key: DungeonKey) = "dungeon_card_${key.name.lowercase()}"
    fun load(context: Context): DungeonRotationSettings {
        val p = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        return DungeonRotationSettings(
            enabledCards = DungeonKey.entries.filterTo(mutableSetOf()) { p.getBoolean(cardKey(it), true) },
            normalAttempts = p.getInt(NORMAL_ATTEMPTS, 2).coerceIn(1, 2),
            useAdAttempts = p.getBoolean(USE_ADS, true),
        )
    }
    fun save(context: Context, settings: DungeonRotationSettings) {
        val edit = context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit()
            .putInt(NORMAL_ATTEMPTS, settings.normalAttempts).putBoolean(USE_ADS, settings.useAdAttempts)
        DungeonKey.entries.forEach { edit.putBoolean(cardKey(it), it in settings.enabledCards) }
        edit.apply()
    }
}

@Composable fun DungeonSettings(enabled: Boolean, onEnabled: (Boolean) -> Unit) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var settings by remember { mutableStateOf(DungeonSettingsStore.load(context)) }
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.dungeon_rotation_title), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.dungeon_rotation_hint), style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = enabled, onCheckedChange = {
                onEnabled(it); AutomationState.autoDungeonEnabled = it
                if (!it) DungeonRotationRequest.cancel()
            })
        }
        TextButton(onClick = { expanded = !expanded }, contentPadding = PaddingValues(0.dp)) {
            Text(stringResource(if (expanded) R.string.dungeon_settings_hide else R.string.dungeon_settings_show))
        }
        if (expanded) {
            Text(stringResource(R.string.dungeon_attempts_title), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1, 2).forEach { count -> FilterChip(
                    selected = settings.normalAttempts == count,
                    onClick = { settings = settings.copy(normalAttempts = count); DungeonSettingsStore.save(context, settings) },
                    label = { Text(stringResource(if (count == 1) R.string.dungeon_attempt_once else R.string.dungeon_attempt_twice)) },
                ) }
            }
            DungeonSettingSwitch(stringResource(R.string.dungeon_ads_title), stringResource(R.string.dungeon_ads_hint), settings.useAdAttempts) {
                settings = settings.copy(useAdAttempts = it); DungeonSettingsStore.save(context, settings)
            }
            Text(stringResource(R.string.dungeon_cards_title), style = MaterialTheme.typography.labelLarge)
            DungeonKey.entries.forEach { key -> DungeonSettingSwitch(
                dungeonCardLabel(key),
                when (key) {
                    DungeonKey.APOCALYMON_WALL -> stringResource(R.string.dungeon_apocalymon_limit)
                    DungeonKey.DAILY -> stringResource(R.string.dungeon_vs_limit)
                    else -> ""
                },
                key in settings.enabledCards,
            ) { checked ->
                settings = settings.copy(enabledCards = if (checked) settings.enabledCards + key else settings.enabledCards - key)
                DungeonSettingsStore.save(context, settings)
            } }
        }
    }
}

@Composable private fun DungeonSettingSwitch(title: String, hint: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.bodyMedium); if (hint.isNotBlank()) Text(hint, style = MaterialTheme.typography.labelSmall) }
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

private fun dungeonCardLabel(key: DungeonKey) = when (key) {
    DungeonKey.APOCALYMON_WALL -> "Apocalymon Wall"
    DungeonKey.DEMIDEVIMON -> "DemiDevimon"
    DungeonKey.BAKEMON -> "Bakemon"
    DungeonKey.DIGIFACTORY -> "Digifactory"
    DungeonKey.NETWORK_DEFENSE -> "Network Defense Ops"
    DungeonKey.METAL_SEA -> "Metal Sea"
    DungeonKey.DAILY -> "VS Defense-type Digimon"
}
