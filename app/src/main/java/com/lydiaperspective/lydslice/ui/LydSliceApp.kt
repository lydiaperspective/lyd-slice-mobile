package com.lydiaperspective.lydslice.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lydiaperspective.lydslice.data.ModelPlacement
import com.lydiaperspective.lydslice.data.PrintProfile
import com.lydiaperspective.lydslice.data.PrintProfileStore
import com.lydiaperspective.lydslice.data.PrintSettings
import com.lydiaperspective.lydslice.data.PrinterProfile
import com.lydiaperspective.lydslice.data.PrinterProfileStore
import java.util.UUID
import kotlin.math.roundToInt

private enum class AppTab(val label: String) {
    Workspace("Workspace"), Settings("Settings"), Printer("Printer")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LydSliceApp() {
    var selectedTab by remember { mutableStateOf(AppTab.Workspace) }
    val models = remember { mutableStateListOf<ModelPlacement>() }
    var settings by remember { mutableStateOf(PrintSettings()) }
    val context = LocalContext.current
    val customProfiles = remember {
        mutableStateListOf<PrintProfile>().apply { addAll(PrintProfileStore.loadCustom(context)) }
    }
    val builtInProfiles = remember { PrintProfileStore.builtInProfiles() }
    val builtInPrinterProfiles = remember { PrinterProfileStore.builtInProfiles() }
    val customPrinterProfiles = remember {
        mutableStateListOf<PrinterProfile>().apply { addAll(PrinterProfileStore.loadCustom(context)) }
    }
    var activePrinter by remember {
        mutableStateOf((builtInPrinterProfiles + customPrinterProfiles).firstOrNull {
            it.id == PrinterProfileStore.selectedId(context)
        } ?: builtInPrinterProfiles.first())
    }

    val stlPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            models.add(ModelPlacement(
                id = UUID.randomUUID().toString(),
                displayName = uri.lastPathSegment?.substringAfterLast('/') ?: "STL model",
                uri = uri.toString()
            ))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Column {
                    Text("Lyd Slice", fontWeight = FontWeight.Bold)
                    Text("Mobile 3D printing studio", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            })
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
                                    AppTab.Settings, AppTab.Printer -> Icons.Default.Settings
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
                modifier = Modifier.padding(padding), models = models,
                onImport = { stlPicker.launch(arrayOf("model/stl", "application/sla", "application/octet-stream", "*/*")) },
                onRemove = { models.remove(it) }, printer = activePrinter
            )
            AppTab.Settings -> SettingsScreen(
                modifier = Modifier.padding(padding), settings = settings, printer = activePrinter,
                onSettingsChange = { settings = it },
                profiles = builtInProfiles + customProfiles,
                onApplyProfile = { settings = it.settings },
                onSaveProfile = { name ->
                    val index = customProfiles.indexOfFirst { it.name.equals(name, ignoreCase = true) }
                    val profile = PrintProfile(name.trim(), settings)
                    if (index >= 0) customProfiles[index] = profile else customProfiles.add(profile)
                    PrintProfileStore.saveCustom(context, customProfiles.toList())
                },
                onDeleteProfile = {
                    customProfiles.remove(it)
                    PrintProfileStore.saveCustom(context, customProfiles.toList())
                }
            )
            AppTab.Printer -> PrinterScreen(
                modifier = Modifier.padding(padding),
                profile = activePrinter,
                profiles = builtInPrinterProfiles + customPrinterProfiles,
                onSelectProfile = {
                    activePrinter = it
                    PrinterProfileStore.saveSelectedId(context, it.id)
                },
                onProfileChange = { edited ->
                    val updated = if (activePrinter.isBuiltIn) edited.copy(
                        id = UUID.randomUUID().toString(),
                        name = if (edited.name != activePrinter.name) edited.name else activePrinter.name + " (Custom)",
                        isBuiltIn = false
                    ) else edited.copy(isBuiltIn = false)
                    activePrinter = updated
                    val index = customPrinterProfiles.indexOfFirst { it.id == updated.id }
                    if (index >= 0) customPrinterProfiles[index] = updated else customPrinterProfiles.add(updated)
                    PrinterProfileStore.saveCustom(context, customPrinterProfiles.toList())
                    PrinterProfileStore.saveSelectedId(context, updated.id)
                },
                onAddCustom = {
                    val created = PrinterProfile(name = "My Custom Printer")
                    customPrinterProfiles.add(created)
                    activePrinter = created
                    PrinterProfileStore.saveCustom(context, customPrinterProfiles.toList())
                    PrinterProfileStore.saveSelectedId(context, created.id)
                },
                onDeleteCustom = {
                    customPrinterProfiles.remove(it)
                    PrinterProfileStore.saveCustom(context, customPrinterProfiles.toList())
                    if (activePrinter.id == it.id) {
                        activePrinter = builtInPrinterProfiles.first()
                        PrinterProfileStore.saveSelectedId(context, activePrinter.id)
                    }
                }
            )
        }
    }
}

@Composable
private fun WorkspaceScreen(
    modifier: Modifier = Modifier,
    models: List<ModelPlacement>,
    onImport: () -> Unit,
    onRemove: (ModelPlacement) -> Unit,
    printer: PrinterProfile
) {
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Build plate", style = MaterialTheme.typography.titleLarge)
                Text(printer.name + " · " + printer.bedWidthMm.toInt() + " × " + printer.bedDepthMm.toInt() + " mm",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = onImport) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text(" Import STL")
            }
        }
        Card(
            Modifier.fillMaxWidth().weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ViewInAr, contentDescription = null, modifier = Modifier.size(52.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("3D build-plate preview", style = MaterialTheme.typography.titleMedium)
                    Text("The interactive 3D viewport is the next milestone.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text("Models imported: ${models.size}")
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { /* Auto-arrange engine will be connected here. */ }, Modifier.weight(1f)) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Text(" Auto arrange")
            }
            Button(onClick = { /* Slicing engine will be connected here. */ },
                Modifier.weight(1f), enabled = models.isNotEmpty()) { Text("Slice") }
        }
        Text("Models", style = MaterialTheme.typography.titleMedium)
        if (models.isEmpty()) {
            Text("Import one or more STL files to begin.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(models, key = { it.id }) { model ->
                    Card {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ViewInAr, contentDescription = null)
                            Text(model.displayName, Modifier.weight(1f).padding(start = 10.dp), maxLines = 1)
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
    printer: PrinterProfile,
    onSettingsChange: (PrintSettings) -> Unit,
    profiles: List<PrintProfile>,
    onApplyProfile: (PrintProfile) -> Unit,
    onSaveProfile: (String) -> Unit,
    onDeleteProfile: (PrintProfile) -> Unit
) {
    var showProfileMenu by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var profileName by remember { mutableStateOf("") }
    val warning = settingsWarning(settings)

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save print profile") },
            text = {
                OutlinedTextField(value = profileName, onValueChange = { profileName = it },
                    label = { Text("Profile name") }, singleLine = true)
            },
            confirmButton = {
                Button(onClick = {
                    if (profileName.isNotBlank()) {
                        onSaveProfile(profileName.trim())
                        showSaveDialog = false
                    }
                }, enabled = profileName.isNotBlank()) { Text("Save") }
            },
            dismissButton = { OutlinedButton(onClick = { showSaveDialog = false }) { Text("Cancel") } }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Print profiles", style = MaterialTheme.typography.headlineSmall)
            Text("Choose a starting point or save your own tuned settings.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box {
                OutlinedButton(onClick = { showProfileMenu = true }) {
                    Text("Choose profile · ${profiles.size} available")
                }
                androidx.compose.material3.DropdownMenu(
                    expanded = showProfileMenu, onDismissRequest = { showProfileMenu = false }
                ) {
                    profiles.forEach { profile ->
                        androidx.compose.material3.DropdownMenuItem(
                            text = {
                                Column {
                                    Text(profile.name)
                                    Text("${profile.settings.layerHeightMm} mm · ${profile.settings.printSpeedMmPerSecond} mm/s",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            onClick = { onApplyProfile(profile); showProfileMenu = false }
                        )
                    }
                }
            }
            Button(onClick = { profileName = ""; showSaveDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text(" Save current settings as profile")
            }
            Text("Built-in presets are starting points, not validated guarantees for every filament brand or modified printer.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Text("Material", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("PLA", "ABS").forEach { material ->
                    FilterChip(
                        selected = settings.material == material,
                        onClick = {
                            onSettingsChange(settings.copy(
                                material = material,
                                nozzleTemperatureC = if (material == "PLA") 205 else 240,
                                bedTemperatureC = if (material == "PLA") 60 else 95,
                                coolingFanPercent = if (material == "PLA") 100 else 0
                            ))
                        },
                        label = { Text(material) }
                    )
                }
            }
            if (settings.material == "ABS") {
                Text(
                    "ABS safety: printing can release irritating fumes and fine particles. Use only with suitable ventilation and follow the filament maker's safety guidance. This printer is not assumed to have an enclosure.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        item {
            Text("Quality ↔ speed", style = MaterialTheme.typography.titleMedium)
            Text(when {
                settings.qualityBalance < 30f -> "Detail priority"
                settings.qualityBalance > 70f -> "Speed priority"
                else -> "Balanced"
            }, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            Slider(value = settings.qualityBalance.coerceIn(0f, 100f), onValueChange = { value ->
                val position = value / 100f
                val layer = (0.08f + position * 0.22f)
                val speed = (25f + position * 30f).roundToInt()
                onSettingsChange(settings.copy(
                    qualityBalance = value,
                    layerHeightMm = (layer * 100f).roundToInt() / 100f,
                    printSpeedMmPerSecond = speed
                ))
            }, valueRange = 0f..100f)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Higher detail", style = MaterialTheme.typography.labelSmall)
                Text("Faster prints", style = MaterialTheme.typography.labelSmall)
            }
            Text("Adjusts layer height and print speed together. Fine-tune either value below.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (warning != null) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Print setting warning", fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer)
                        Text(warning, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }
        item {
            SettingSlider("Layer height", "${"%.2f".format(settings.layerHeightMm)} mm",
                settings.layerHeightMm, 0.05f, 0.32f) {
                onSettingsChange(settings.copy(layerHeightMm = (it * 100f).roundToInt() / 100f))
            }
        }
        item {
            SettingSlider("Infill", "${settings.infillPercent}%", settings.infillPercent.toFloat(), 0f, 100f) {
                onSettingsChange(settings.copy(infillPercent = it.roundToInt()))
            }
            Text("More infill uses more material and time; it does not replace adequate wall thickness.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            SettingSlider("Print speed", "${settings.printSpeedMmPerSecond} mm/s",
                settings.printSpeedMmPerSecond.toFloat(), 10f, 100f) {
                onSettingsChange(settings.copy(printSpeedMmPerSecond = it.roundToInt()))
            }
        }
        item {
            SettingSlider("Wall count", settings.wallCount.toString(), settings.wallCount.toFloat(), 1f, 6f) {
                onSettingsChange(settings.copy(wallCount = it.roundToInt().coerceIn(1, 6)))
            }
        }
        item {
            SettingSlider("Nozzle temperature", "${settings.nozzleTemperatureC} °C",
                settings.nozzleTemperatureC.toFloat(), 170f, printer.maxNozzleTemperatureC.toFloat()) {
                onSettingsChange(settings.copy(nozzleTemperatureC = it.roundToInt()))
            }
        }
        if (printer.heatedBed) {
            item {
                SettingSlider("Bed temperature", "${settings.bedTemperatureC} °C",
                    settings.bedTemperatureC.toFloat(), 0f, printer.maxBedTemperatureC.toFloat()) {
                    onSettingsChange(settings.copy(bedTemperatureC = it.roundToInt()))
                }
            }
        }
        item {
            SettingSlider("Part cooling fan", "${settings.coolingFanPercent}%",
                settings.coolingFanPercent.toFloat(), 0f, 100f) {
                onSettingsChange(settings.copy(coolingFanPercent = it.roundToInt()))
            }
        }
        item {
            SettingSlider("Initial layer speed", "${settings.initialLayerSpeedMmPerSecond} mm/s",
                settings.initialLayerSpeedMmPerSecond.toFloat(), 5f, 40f) {
                onSettingsChange(settings.copy(initialLayerSpeedMmPerSecond = it.roundToInt()))
            }
        }
        items(profiles.filterNot { it.isBuiltIn }) { profile ->
            OutlinedButton(onClick = { onDeleteProfile(profile) }) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Text(" Delete saved profile: ${profile.name}")
            }
        }
    }
}

private fun settingsWarning(settings: PrintSettings): String? {
    if (settings.material == "PLA") {
        if (settings.nozzleTemperatureC !in 190..210)
            return "Robo's PLA guidance is 190–210°C. Outside this range, extrusion or stringing may worsen. Check your filament maker's label."
        if (settings.bedTemperatureC !in 50..70)
            return "Robo's PLA bed guidance is 50–70°C. A large deviation can contribute to adhesion problems or deformation."
        if (settings.coolingFanPercent < 30)
            return "Low part cooling can reduce PLA detail and bridging quality. Reduce it only to troubleshoot a specific issue."
    } else {
        if (settings.nozzleTemperatureC !in 230..250)
            return "Robo's ABS guidance is 230–250°C. Confirm the filament's own range and your hot-end condition."
        if (settings.bedTemperatureC !in 80..100)
            return "Robo's ABS bed guidance is 80–100°C. Too little bed heat can increase warping and poor adhesion."
        if (settings.coolingFanPercent > 20)
            return "Robo warns that ABS is prone to warping and recommends little or no part cooling. High fan speed may increase lifting."
    }
    if (settings.layerHeightMm > 0.30f)
        return "This exceeds Robo's documented 0.30 mm layer-height range. Detail and layer consistency may suffer."
    if (settings.printSpeedMmPerSecond > 60)
        return "Above 60 mm/s is outside this app's conservative R1 Plus starting range. Motion or extrusion may become inconsistent."
    if (settings.initialLayerSpeedMmPerSecond > 30)
        return "A fast first layer can reduce bed adhesion. Robo recommends a slower first layer when adhesion is unreliable."
    if (settings.qualityBalance > 80f)
        return "Strong speed priority trades away visible detail and can make extrusion less consistent."
    return null
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
        Slider(value = value.coerceIn(rangeStart, rangeEnd), onValueChange = onValueChange,
            valueRange = rangeStart..rangeEnd)
    }
}

@Composable
private fun PrinterScreen(
    modifier: Modifier = Modifier,
    profile: PrinterProfile,
    profiles: List<PrinterProfile>,
    onSelectProfile: (PrinterProfile) -> Unit,
    onProfileChange: (PrinterProfile) -> Unit,
    onAddCustom: () -> Unit,
    onDeleteCustom: (PrinterProfile) -> Unit
) {
    var printerMenu by remember { mutableStateOf(false) }
    var shapeMenu by remember { mutableStateOf(false) }
    var flavorMenu by remember { mutableStateOf(false) }
    var nozzleMenu by remember { mutableStateOf(false) }
    val nozzleChoices = listOf(0.2f, 0.25f, 0.3f, 0.4f, 0.5f, 0.6f, 0.8f, 1.0f)
    LazyColumn(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Printer setup", style = MaterialTheme.typography.headlineSmall)
            Text("Choose a preset or create a custom machine. Editing a built-in preset creates a separate custom copy.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box {
                OutlinedButton(onClick = { printerMenu = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(profile.name)
                }
                androidx.compose.material3.DropdownMenu(printerMenu, { printerMenu = false }) {
                    profiles.forEach { preset ->
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(preset.name + if (preset.isBuiltIn) " · Built-in" else " · Custom") },
                            onClick = { onSelectProfile(preset); printerMenu = false }
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAddCustom) { Icon(Icons.Default.Add, null); Text(" Add custom") }
                if (!profile.isBuiltIn) OutlinedButton(onClick = { onDeleteCustom(profile) }) {
                    Icon(Icons.Default.Delete, null); Text(" Delete")
                }
            }
            Text(if (profile.isBuiltIn) "Built-in preset · edits are saved as a custom profile."
                else "Custom profile · edits are saved automatically.",
                color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(
                value = profile.name,
                onValueChange = { onProfileChange(profile.copy(name = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Printer profile name") },
                singleLine = true
            )
        }
        item {
            Text("Build volume", style = MaterialTheme.typography.titleLarge)
            PrinterNumberField("X width (mm)", profile.bedWidthMm) { onProfileChange(profile.copy(bedWidthMm = it.coerceAtLeast(1f))) }
            PrinterNumberField("Y depth (mm)", profile.bedDepthMm) { onProfileChange(profile.copy(bedDepthMm = it.coerceAtLeast(1f))) }
            PrinterNumberField("Z height (mm)", profile.buildHeightMm) { onProfileChange(profile.copy(buildHeightMm = it.coerceAtLeast(1f))) }
            Box {
                OutlinedButton(onClick = { shapeMenu = true }) { Text("Build plate shape: " + profile.buildPlateShape) }
                androidx.compose.material3.DropdownMenu(shapeMenu, { shapeMenu = false }) {
                    listOf("Rectangular", "Circular", "Custom").forEach { shape ->
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(shape) },
                            onClick = { onProfileChange(profile.copy(buildPlateShape = shape)); shapeMenu = false }
                        )
                    }
                }
            }
            ProfileSwitch("Origin at center (0,0)", profile.originAtCenter) { onProfileChange(profile.copy(originAtCenter = it)) }
            ProfileSwitch("Heated bed", profile.heatedBed) { onProfileChange(profile.copy(heatedBed = it)) }
            ProfileSwitch("Heated build volume / chamber", profile.heatedBuildVolume) { onProfileChange(profile.copy(heatedBuildVolume = it)) }
            if (!profile.heatedBed) Text("This printer has no heated bed. Bed temperature must not be used by generated machine code.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            if (profile.id == "robo3d-r1-plus") Text("R1 Plus build volume is a provisional 220 × 220 × 250 mm. Verify actual usable travel before slicing.",
                color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        item {
            Text("Extruders and nozzle", style = MaterialTheme.typography.titleLarge)
            PrinterNumberField("Number of extruders", profile.extruderCount.toFloat()) {
                onProfileChange(profile.copy(extruderCount = it.toInt().coerceIn(1, 4)))
            }
            Text("Supported nozzle sizes (select all that apply)", style = MaterialTheme.typography.labelMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items(nozzleChoices) { size ->
                    FilterChip(
                        selected = profile.nozzleDiametersMm.any { kotlin.math.abs(it - size) < 0.001f },
                        onClick = {
                            val values = profile.nozzleDiametersMm.toMutableList()
                            val index = values.indexOfFirst { kotlin.math.abs(it - size) < 0.001f }
                            if (index >= 0 && values.size > 1) values.removeAt(index) else if (index < 0) values.add(size)
                            val sorted = values.sorted()
                            val active = if (sorted.contains(profile.activeNozzleDiameterMm)) profile.activeNozzleDiameterMm else sorted.first()
                            onProfileChange(profile.copy(nozzleDiametersMm = sorted, activeNozzleDiameterMm = active))
                        }, label = { Text(size.toString()) }
                    )
                }
            }
            Box {
                OutlinedButton(onClick = { nozzleMenu = true }) {
                    Text("Installed nozzle: " + profile.activeNozzleDiameterMm + " mm")
                }
                androidx.compose.material3.DropdownMenu(nozzleMenu, { nozzleMenu = false }) {
                    profile.nozzleDiametersMm.forEach { size ->
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(size.toString() + " mm") },
                            onClick = { onProfileChange(profile.copy(activeNozzleDiameterMm = size)); nozzleMenu = false }
                        )
                    }
                }
            }
            PrinterNumberField("Filament diameter (mm)", profile.filamentDiameterMm) {
                onProfileChange(profile.copy(filamentDiameterMm = it.coerceAtLeast(0.1f)))
            }
            PrinterNumberField("Maximum nozzle temperature (°C)", profile.maxNozzleTemperatureC.toFloat()) {
                onProfileChange(profile.copy(maxNozzleTemperatureC = it.toInt().coerceIn(0, 500)))
            }
            PrinterNumberField("Maximum bed temperature (°C)", profile.maxBedTemperatureC.toFloat()) {
                onProfileChange(profile.copy(maxBedTemperatureC = it.toInt().coerceIn(0, 200)))
            }
        }
        item {
            Text("Machine settings", style = MaterialTheme.typography.titleLarge)
            Box {
                OutlinedButton(onClick = { flavorMenu = true }) { Text("G-code flavor: " + profile.gcodeFlavor) }
                androidx.compose.material3.DropdownMenu(flavorMenu, { flavorMenu = false }) {
                    listOf("Marlin", "RepRap", "Klipper", "UltiGCode", "Griffin", "Makerbot", "Other").forEach { flavor ->
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(flavor) },
                            onClick = { onProfileChange(profile.copy(gcodeFlavor = flavor)); flavorMenu = false }
                        )
                    }
                }
            }
            Text("Printhead clearance (mm)", style = MaterialTheme.typography.titleMedium)
            PrinterNumberField("X min", profile.headXMinMm) { onProfileChange(profile.copy(headXMinMm = it)) }
            PrinterNumberField("Y min", profile.headYMinMm) { onProfileChange(profile.copy(headYMinMm = it)) }
            PrinterNumberField("X max", profile.headXMaxMm) { onProfileChange(profile.copy(headXMaxMm = it)) }
            PrinterNumberField("Y max", profile.headYMaxMm) { onProfileChange(profile.copy(headYMaxMm = it)) }
            PrinterNumberField("Gantry height (mm)", profile.gantryHeightMm) { onProfileChange(profile.copy(gantryHeightMm = it)) }
        }
        item {
            Text("Start and end G-code", style = MaterialTheme.typography.titleLarge)
            Text("Saved with this profile. Not yet used to generate G-code; machine scripts must be validated against firmware before use.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(profile.startGcode, { onProfileChange(profile.copy(startGcode = it)) },
                modifier = Modifier.fillMaxWidth(), label = { Text("Machine start G-code") }, minLines = 4)
            OutlinedTextField(profile.endGcode, { onProfileChange(profile.copy(endGcode = it)) },
                modifier = Modifier.fillMaxWidth(), label = { Text("Machine end G-code") }, minLines = 4)
        }
    }
}

@Composable
private fun PrinterNumberField(label: String, value: Float, onValueChange: (Float) -> Unit) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = { next -> text = next; next.toFloatOrNull()?.let(onValueChange) },
        modifier = Modifier.fillMaxWidth(), label = { Text(label) }, singleLine = true
    )
}

@Composable
private fun ProfileSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        androidx.compose.material3.Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
