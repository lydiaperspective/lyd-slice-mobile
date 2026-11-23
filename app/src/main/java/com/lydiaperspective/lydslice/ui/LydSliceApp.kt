package com.lydiaperspective.lydslice.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.CropRotate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lydiaperspective.lydslice.data.ModelPlacement
import com.lydiaperspective.lydslice.data.PrintSettings
import java.util.UUID

private enum class AppTab(val label: String) {
    Workspace("Workspace"), Settings("Settings"), Printer("Printer")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LydSliceApp() {
    var selectedTab by remember { mutableStateOf(AppTab.Workspace) }
    val models = remember { mutableStateListOf<ModelPlacement>() }
    var settings by remember { mutableStateOf(PrintSettings()) }

    val stlPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            models.add(
                ModelPlacement(
                    id = UUID.randomUUID().toString(),
                    displayName = uri.lastPathSegment?.substringAfterLast('/') ?: "STL model",
                    uri = uri.toString()
                )
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Lyd Slice", fontWeight = FontWeight.Bold)
                        Text(
                            "Mobile 3D printing studio",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = when (tab) {
                                    AppTab.Workspace -> Icons.Default.ViewInAr
                                    AppTab.Settings -> Icons.Default.Settings
                                    AppTab.Printer -> Icons.Default.Cable
                                },
                                contentDescription = tab.label
                            )
                        },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        when (selectedTab) {
            AppTab.Workspace -> WorkspaceScreen(
                modifier = Modifier.padding(padding),
                models = models,
                onImport = { stlPicker.launch(arrayOf("model/stl", "application/sla", "application/octet-stream", "*/*")) },
                onRemove = { model -> models.remove(model) }
            )
            AppTab.Settings -> SettingsScreen(
                modifier = Modifier.padding(padding),
                settings = settings,
                onSettingsChange = { settings = it }
            )
            AppTab.Printer -> PrinterScreen(Modifier.padding(padding))
        }
    }
}

@Composable
private fun WorkspaceScreen(
    modifier: Modifier = Modifier,
    models: List<ModelPlacement>,
    onImport: () -> Unit,
    onRemove: (ModelPlacement) -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Build plate", style = MaterialTheme.typography.titleLarge)
                Text("Robo 3D R1 Plus · 220 × 220 mm", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = onImport) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text(" Import STL")
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ViewInAr, contentDescription = null, modifier = Modifier.size(52.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("3D build-plate preview", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "The interactive 3D viewport is the next milestone.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Models imported: ${models.size}")
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { /* Auto-arrange engine will be connected here. */ }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.CropRotate, contentDescription = null)
                Text(" Auto arrange")
            }
            Button(onClick = { /* Slicing engine will be connected here. */ }, modifier = Modifier.weight(1f), enabled = models.isNotEmpty()) {
                Text("Slice")
            }
        }

        Text("Models", style = MaterialTheme.typography.titleMedium)
        if (models.isEmpty()) {
            Text("Import one or more STL files to begin.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(models, key = { it.id }) { model ->
                    Card {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ViewInAr, contentDescription = null)
                            Text(model.displayName, modifier = Modifier.weight(1f).padding(start = 10.dp), maxLines = 1)
                            IconButton(onClick = { onRemove(model) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove model")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    modifier: Modifier = Modifier,
    settings: PrintSettings,
    onSettingsChange: (PrintSettings) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Print settings", style = MaterialTheme.typography.headlineSmall)
            Text("Initial values for the R1 Plus profile", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            SettingSlider("Layer height", "${"%.2f".format(settings.layerHeightMm)} mm", settings.layerHeightMm, 0.08f, 0.32f) {
                onSettingsChange(settings.copy(layerHeightMm = it))
            }
        }
        item {
            SettingSlider("Infill", "${settings.infillPercent}%", settings.infillPercent.toFloat(), 0f, 100f) {
                onSettingsChange(settings.copy(infillPercent = it.toInt()))
            }
        }
        item {
            SettingSlider("Print speed", "${settings.printSpeedMmPerSecond} mm/s", settings.printSpeedMmPerSecond.toFloat(), 10f, 100f) {
                onSettingsChange(settings.copy(printSpeedMmPerSecond = it.toInt()))
            }
        }
        item {
            SettingSlider("Wall count", settings.wallCount.toString(), settings.wallCount.toFloat(), 1f, 6f) {
                onSettingsChange(settings.copy(wallCount = it.toInt().coerceAtLeast(1)))
            }
        }
        item {
            SettingSlider("Nozzle temperature", "${settings.nozzleTemperatureC} °C", settings.nozzleTemperatureC.toFloat(), 170f, 240f) {
                onSettingsChange(settings.copy(nozzleTemperatureC = it.toInt()))
            }
        }
        item {
            SettingSlider("Bed temperature", "${settings.bedTemperatureC} °C", settings.bedTemperatureC.toFloat(), 0f, 100f) {
                onSettingsChange(settings.copy(bedTemperatureC = it.toInt()))
            }
        }
    }
}

@Composable
private fun SettingSlider(
    label: String,
    valueLabel: String,
    value: Float,
    rangeStart: Float,
    rangeEnd: Float,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
            Text(valueLabel, color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = rangeStart..rangeEnd)
    }
}

@Composable
private fun PrinterScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Printer connection", style = MaterialTheme.typography.headlineSmall)
        Card {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Robo 3D R1 Plus", style = MaterialTheme.typography.titleLarge)
                Text("OctoPrint connection has not been configured.")
                Text("Next: save the OctoPrint URL and API key securely, then add connection testing and G-code upload.")
                OutlinedButton(onClick = { /* OctoPrint setup screen will be added next. */ }) {
                    Icon(Icons.Default.Cable, contentDescription = null)
                    Text(" Configure OctoPrint")
                }
            }
        }
        Text("Printer profile", style = MaterialTheme.typography.titleMedium)
        Text("Build volume, nozzle size, filament diameter, start G-code and end G-code will be editable in a later profile editor.")
    }
}
