package com.lydiaperspective.lydslice.engine

import android.content.Context
import android.net.Uri
import com.lydiaperspective.lydslice.data.PrinterProfile
import com.lydiaperspective.lydslice.data.PrintSettings
import com.lydiaperspective.lydslice.data.ModelPlacement
import java.io.File
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.ytkab0bp.slicebeam.slic3r.Model
import ru.ytkab0bp.slicebeam.slic3r.Native
import ru.ytkab0bp.slicebeam.slic3r.PrintConfigDef
import ru.ytkab0bp.slicebeam.slic3r.SliceListener

/**
 * On-device OrcaSlicer Mobile engine adapter.
 *
 * Native ABI and engine archive are pinned in scripts/fetch-orca-mobile-engine.sh.
 * This first integration intentionally slices one STL at a time; it refuses multi-model
 * requests rather than silently overlapping or dropping objects.
 */
class OrcaSlicerEngine(private val context: Context) : SlicingEngine {
    override suspend fun slice(
        request: SliceRequest,
        onProgress: (Float) -> Unit
    ): SliceResult = withContext(Dispatchers.IO) {
        require(request.models.size == 1) {
            "The first Orca integration currently slices one STL at a time. Remove the extra models and try again."
        }
        check(Native.isLoaded()) {
            "The OrcaSlicer native engine could not be loaded: ${Native.getLoadError()?.message ?: "unknown native loading error"}"
        }

        val model = request.models.single()
        val input = copyModelIntoCache(model)
        val outputDirectory = File(context.cacheDir, "slices").apply { mkdirs() }
        val output = File(outputDirectory, safeName(model.displayName) + ".gcode")
        val config = File(outputDirectory, "active-printer.ini")
        writeOrcaConfig(request.printer, request.settings, config)

        Native.set_svg_path_prefix(outputDirectory.absolutePath + File.separator)
        Model(input.absolutePath).use { nativeModel ->
            require(nativeModel.objectsCount > 0) { "The STL does not contain any printable objects." }
            for (index in 0 until nativeModel.objectsCount) nativeModel.ensureOnBed(index)
            val bounds = nativeModel.boundingBoxExactGlobal
            require(bounds.size == 6 && bounds.all { it.isFinite() }) {
                "The slicer could not calculate valid model dimensions."
            }
            val centerX = (bounds[0] + bounds[3]) / 2.0
            val centerY = (bounds[1] + bounds[4]) / 2.0
            val bedOriginX = if (request.printer.originAtCenter) 0.0 else request.printer.bedWidthMm / 2.0
            val bedOriginY = if (request.printer.originAtCenter) 0.0 else request.printer.bedDepthMm / 2.0
            nativeModel.translate(bedOriginX - centerX, bedOriginY - centerY, 0.0)

            val nativeResult = nativeModel.slice(
                config.absolutePath,
                output.absolutePath,
                SliceListener { percent, _ ->
                    onProgress(percent.coerceIn(0, 100) / 100f)
                }
            )
            nativeResult.use { result ->
                require(output.isFile && output.length() > 0L) {
                    "OrcaSlicer returned without creating a G-code file."
                }
                val filamentMm = (0 until 15).sumOf { role ->
                    runCatching { result.getUsedFilamentMM(role) }.getOrDefault(0.0)
                }.toFloat()
                val estimate = readEstimatedSeconds(output)
                val warnings = buildList {
                    if (request.printer.startGcode.isBlank()) {
                        add("Printer start G-code is not configured. Review the exported file before printing.")
                    }
                    if (request.printer.endGcode.isBlank()) {
                        add("Printer end G-code is not configured. Review the exported file before printing.")
                    }
                    add("Robo 3D R1 Plus dimensions and firmware settings in this profile still need verification against your physical printer.")
                }
                SliceResult(
                    gcodeFilePath = output.absolutePath,
                    estimatedPrintTimeSeconds = estimate,
                    filamentUsedMm = filamentMm.takeIf { it > 0f },
                    warnings = warnings
                )
            }
        }
    }

    private fun copyModelIntoCache(model: ModelPlacement): File {
        val directory = File(context.cacheDir, "slice-input").apply { mkdirs() }
        val destination = File(directory, safeName(model.displayName) + ".stl")
        val uri = Uri.parse(model.uri)
        val input = when (uri.scheme) {
            "content" -> context.contentResolver.openInputStream(uri)
            "file" -> File(uri.path ?: error("Invalid model file URI")).inputStream()
            else -> null
        } ?: error("Cannot open ${model.displayName}. Please import the STL again.")
        input.use { source -> destination.outputStream().use(source::copyTo) }
        require(destination.length() >= 84L) { "The selected STL file is empty or incomplete." }
        return destination
    }

    private fun writeOrcaConfig(
        printer: PrinterProfile,
        settings: PrintSettings,
        destination: File
    ) {
        val definitions = PrintConfigDef.getInstance().options
        val values = sortedMapOf<String, String>()
        definitions.forEach { (key, definition) ->
            if (definition.printerTechnology != ru.ytkab0bp.slicebeam.slic3r.ConfigOptionDef.PrinterTechnology.SLA) {
                definition.defaultValue?.let { values[key] = it }
            }
        }
        fun set(key: String, value: String) {
            require(definitions.containsKey(key)) { "The bundled Orca engine does not support required setting '$key'." }
            values[key] = value
        }
        val width = printer.bedWidthMm.toDouble()
        val depth = printer.bedDepthMm.toDouble()
        val x0 = if (printer.originAtCenter) -width / 2.0 else 0.0
        val y0 = if (printer.originAtCenter) -depth / 2.0 else 0.0
        val x1 = x0 + width
        val y1 = y0 + depth
        set("printer_technology", "FFF")
        set("printable_area", "${decimal(x0)}x${decimal(y0)},${decimal(x1)}x${decimal(y0)},${decimal(x1)}x${decimal(y1)},${decimal(x0)}x${decimal(y1)}")
        set("printable_height", decimal(printer.buildHeightMm.toDouble()))
        set("gcode_flavor", printer.gcodeFlavor.lowercase(Locale.ROOT).replace(" ", ""))
        set("nozzle_diameter", decimal(printer.activeNozzleDiameterMm.toDouble()))
        set("filament_diameter", decimal(printer.filamentDiameterMm.toDouble()))
        set("nozzle_temperature", settings.nozzleTemperatureC.toString())
        set("nozzle_temperature_initial_layer", settings.nozzleTemperatureC.toString())
        set("layer_height", decimal(settings.layerHeightMm.toDouble()))
        set("initial_layer_print_height", decimal(settings.layerHeightMm.toDouble()))
        set("wall_loops", settings.wallCount.toString())
        set("top_shell_layers", settings.topBottomLayers.toString())
        set("bottom_shell_layers", settings.topBottomLayers.toString())
        set("sparse_infill_density", "${settings.infillPercent}%")
        set("outer_wall_speed", settings.printSpeedMmPerSecond.toString())
        set("inner_wall_speed", settings.printSpeedMmPerSecond.toString())
        set("sparse_infill_speed", settings.printSpeedMmPerSecond.toString())
        set("initial_layer_speed", settings.initialLayerSpeedMmPerSecond.toString())
        set("enable_support", if (settings.supportEnabled) "1" else "0")
        set("fan_min_speed", settings.coolingFanPercent.toString())
        set("fan_max_speed", settings.coolingFanPercent.toString())
        set("curr_bed_type", "High Temp Plate")
        set("hot_plate_temp", if (printer.heatedBed) settings.bedTemperatureC.toString() else "0")
        set("hot_plate_temp_initial_layer", if (printer.heatedBed) settings.bedTemperatureC.toString() else "0")
        when (settings.buildPlateAdhesion.lowercase(Locale.ROOT)) {
            "brim" -> set("brim_type", "outer_only")
            "none" -> set("skirt_loops", "0")
            else -> set("skirt_loops", "2")
        }
        if (printer.startGcode.isNotBlank()) set("machine_start_gcode", printer.startGcode)
        if (printer.endGcode.isNotBlank()) set("machine_end_gcode", printer.endGcode)

        destination.parentFile?.mkdirs()
        destination.writeText(values.entries.joinToString(separator = "", postfix = "") { (key, value) ->
            "$key = ${value.replace("\n", "\\n")}\n"
        })
    }

    private fun readEstimatedSeconds(file: File): Long? {
        val header = file.useLines { lines ->
            lines.firstOrNull { it.contains("estimated printing time", ignoreCase = true) }
        } ?: return null
        val value = header.substringAfter('=').trim()
        val regex = Regex("(?:(\\d+)h)?\\s*(?:(\\d+)m)?\\s*(?:(\\d+)s)?")
        val match = regex.matchEntire(value) ?: return null
        val hours = match.groupValues[1].toLongOrNull() ?: 0L
        val minutes = match.groupValues[2].toLongOrNull() ?: 0L
        val seconds = match.groupValues[3].toLongOrNull() ?: 0L
        return hours * 3600 + minutes * 60 + seconds
    }

    private fun safeName(name: String): String =
        name.substringBeforeLast('.', name).replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "model" }

    private fun decimal(value: Double): String = java.math.BigDecimal.valueOf(value)
        .stripTrailingZeros().toPlainString()
}
