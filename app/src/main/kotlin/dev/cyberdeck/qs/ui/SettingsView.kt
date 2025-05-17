package dev.cyberdeck.qs.ui

import androidx.camera.extensions.ExtensionMode
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.cyberdeck.qs.common.Settings
import kotlinx.coroutines.launch

@Composable
fun SettingsView() {
    Column {
        Text("Wide Angle Lens")
        WideAngleSelection()
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Text("Mode")
        ModeSelection()
    }
}

@Composable
private fun WideAngleSelection() {
    val context = LocalContext.current
    val settings = remember(context) { Settings.get(context) }
    val wideAngle by settings.wideAngle().collectAsState(false)
    val scope = rememberCoroutineScope()

    Switch(
        checked = wideAngle,
        onCheckedChange = {
            scope.launch { settings.toggleWideAngle() }
        }
    )
}

@Composable
fun ModeSelection() {
    val context = LocalContext.current
    val settings = remember(context) { Settings.get(context) }
    val selectedMode by settings.mode().collectAsState(false)
    val scope = rememberCoroutineScope()

    val modes = mapOf(
        ExtensionMode.NONE to "None",
        ExtensionMode.AUTO to "Automatic",
        ExtensionMode.NIGHT to "Night",
        ExtensionMode.HDR to "HDR",
    )

    Column(Modifier.selectableGroup()) {
        modes.forEach { (mode, text) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = (mode == selectedMode),
                        onClick = {
                            scope.launch {
                                settings.setMode(mode)
                            }
                        },
                        role = Role.RadioButton
                    )
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (mode == selectedMode),
                    onClick = null
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
    }
}