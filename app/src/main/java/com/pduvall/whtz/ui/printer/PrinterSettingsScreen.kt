package com.pduvall.whtz.ui.printer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pduvall.whtz.data.printer.PrinterSettingsStore

@Composable
fun PrinterSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    vm: PrinterViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by vm.state.collectAsStateWithLifecycle()

    // Connecting to the printer needs BOTH: CONNECT to open the socket, and SCAN because the
    // print library calls BluetoothAdapter.cancelDiscovery() (which requires SCAN on API 31+).
    fun hasBtPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) ==
                PackageManager.PERMISSION_GRANTED)

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { vm.refresh(hasBtPermission()) }

    LaunchedEffect(Unit) { vm.refresh(hasBtPermission()) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Printer", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            TextButton(onClick = onBack) { Text("Back") }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Pair your thermal printer (e.g. NETUM NT-861) in Android's Bluetooth settings first, " +
                "then select it below.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(16.dp))

        if (!state.bluetoothReady) {
            Text(
                "Bluetooth appears to be off. Enable it in Android settings.",
                color = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.height(12.dp))
        }

        if (!state.hasPermission) {
            Button(onClick = {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_SCAN,
                    ),
                )
            }) {
                Text("Grant Bluetooth permission")
            }
        } else {
            Text("Paper width", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Row {
                WidthOption("58 mm", selected = state.widthMm < 80) { vm.setWidth(58) }
                Spacer(Modifier.width(8.dp))
                WidthOption("80 mm", selected = state.widthMm >= 80) { vm.setWidth(80) }
            }

            Spacer(Modifier.height(16.dp))
            Text("Print darkness", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Row {
                WidthOption("Light", selected = state.brightness >= PrinterSettingsStore.BRIGHTNESS_LIGHT) {
                    vm.setBrightness(PrinterSettingsStore.BRIGHTNESS_LIGHT)
                }
                Spacer(Modifier.width(8.dp))
                WidthOption("Normal", selected = state.brightness == PrinterSettingsStore.BRIGHTNESS_NORMAL) {
                    vm.setBrightness(PrinterSettingsStore.BRIGHTNESS_NORMAL)
                }
                Spacer(Modifier.width(8.dp))
                WidthOption("Dark", selected = state.brightness <= PrinterSettingsStore.BRIGHTNESS_DARK) {
                    vm.setBrightness(PrinterSettingsStore.BRIGHTNESS_DARK)
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Paired devices", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            if (state.devices.isEmpty()) {
                Text("No paired devices found. Pair your printer in Android settings, then reopen this screen.")
            } else {
                state.devices.forEach { device ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { vm.select(device) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = device.address == state.selectedAddress,
                            onClick = { vm.select(device) },
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(device.name)
                            Text(device.address, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { vm.testPrint() },
                enabled = state.selectedAddress != null && !state.busy,
            ) { Text("Test print") }
        }

        if (state.busy) {
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        state.message?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun WidthOption(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick) { Text(label) }
    }
}
