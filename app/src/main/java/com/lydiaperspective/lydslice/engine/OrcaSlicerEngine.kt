package com.lydiaperspective.lydslice.engine

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.lydiaperspective.lydslice.data.PrinterProfile
import com.lydiaperspective.lydslice.data.PrintSettings
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
 * Multiple imported STLs are packed onto a single plate and merged into a binary STL
 * before being passed to the native OrcaSlicer model API.
 */
class OrcaSlicerEngine(private val context: Context) : SlicingEngine {
    override suspend fun slice(
        request: SliceRequest,
        onProgress: (Float) -> Unit
    ): SliceResult = withContext(Dispatchers.IO) {
        validateRequest(request)
        check(Native.isLoaded()) {
            "The OrcaSlicer native engine could not be loaded: ${Native.getLoadError()?.message ?: "unknown native loading error"}"
        }

        val arrangedModels = StlPlateComposer.arrange(context, request.models, request.printer)
        val outputDirectory = File(context.filesDir, "slices").apply { mkdirs() }
        val input = StlPlateComposer.compose(
            context,
            arrangedModels,
            request.printer,
            File(outputDirectory, "combined-plate.stl")
        )
        val output = File(outputDirectory, "plate-print.gcode")
        val config = File(outputDirectory, "active-printer.ini")
        if (output.exists()) require(output.delete()) { "Cannot replace the previous G-code file." }
        writeOrcaConfig(request.printer, request.settings, config)
        onProgress(0f)

        Native.set_svg_path_prefix(outputDirectory.absolutePath + File.separator)
        Model(input.absolutePath).use { nativeModel ->
            require(nativeModel.objectsCount > 0) { "The STL does not contain any printable objects." }
            for (index in 0 until nativeModel.objectsCount) nativeModel.ensureOnBed(index)
            val bounds = nativeModel.boundingBoxExactGlobal
            require(bounds.size == 6 && bounds.all { it.isFinite() }) {
                "The slicer could not calculate valid model dimensions."
            }
            require(bounds[3] > bounds[0] && bounds[4] > bounds[1] && bounds[5] > bounds[2]) {
                "The STL has no measurable printable volume."
            }
            val modelWidth = bounds[3] - bounds[0]
            val modelDepth = bounds[4] - bounds[1]
            val modelHeight = bounds[5] - bounds[2]
            require(modelWidth <= request.printer.bedWidthMm.toDouble() + 0.01) {
                "Model width is ${decimal(modelWidth)} mm, larger than the ${request.printer.bedWidthMm} mm build plate."
            }
            require(modelDepth <= request.printer.bedDepthMm.toDouble() + 0.01) {
                "Model depth is ${decimal(modelDepth)} mm, larger than the ${request.printer.bedDepthMm} mm build plate."
            }
            require(modelHeight <= request.printer.buildHeightMm.toDouble() + 0.01) {
                "Model height is ${decimal(modelHeight)} mm, larger than the ${request.printer.buildHeightMm} mm build height."
            }
            val centerX = (bounds[0] + bounds[3]) / 2.0
            val centerY = (bounds[1] + bounds[4]) / 2.0
            val bedOriginX = if (request.printer.originAtCenter) 0.0 else request.printer.bedWidthMm / 2.0
            val bedOriginY = if (request.printer.originAtCenter) 0.0 else request.printer.bedDepthMm / 2.0
            nativeModel.translate(bedOriginX - centerX, bedOriginY - centerY, -bounds[2])

            val plateMinX = if (request.printer.originAtCenter) -request.printer.bedWidthMm / 2.0 else 0.0
            val plateMinY = if (request.printer.originAtCenter) -request.printer.bedDepthMm / 2.0 else 0.0
            val placedBounds = nativeModel.boundingBoxExactGlobal
            require(
                placedBounds[0] >= plateMinX - 0.01 &&
                    placedBounds[1] >= plateMinY - 0.01 &&
                    placedBounds[2] >= -0.01 &&
                    placedBounds[3] <= plateMinX + request.printer.bedWidthMm.toDouble() + 0.01 &&
                    placedBounds[4] <= plateMinY + request.printer.bedDepthMm.toDouble() + 0.01 &&
                    placedBounds[5] <= request.printer.buildHeightMm + 0.01
            ) {
                "The model could not be positioned fully inside the configured build volume."
            }

            val nativeResult = nativeModel.slice(
                config.absolutePath,
                output.absolutePath,
                SliceListener { percent, _ ->
                    val progress = percent.coerceIn(0, 100) / 100f
                    Handler(Looper.getMainLooper()).post { onProgress(progress) }
                }
            )
            nativeResult.use { result ->
                require(output.isFile && output.length() > 0L) {
                    "OrcaSlicer returned without creating a G-code file."
                }
                validateGcodeOutput(output)
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
                    add("R1+ firmware axis limits are 240 × 254 × 240 mm. Confirm the actual protected printable bed area and first-layer Z offset before printing near the limits.")
                }
                Handler(Looper.getMainLooper()).post { onProgress(1f) }
                SliceResult(
                    gcodeFilePath = output.absolutePath,
                    estimatedPrintTimeSeconds = estimate,
                    filamentUsedMm = filamentMm.takeIf { it > 0f },
                    warnings = warnings
                )
            }
        }
    }


    private fun validateRequest(request: SliceRequest) {
        require(request.models.isNotEmpty()) { "Import at least one STL model before slicing." }
        require(request.models.size <= 100) { "A single plate supports up to 100 models." }
        val printer = request.printer
        val settings = request.settings
        require(printer.buildPlateShape.equals("Rectangular", ignoreCase = true)) {
            "Only rectangular build plates are supported by this slicer integration. Circular and custom plate geometry must be added before slicing."
        }
        require(printer.bedWidthMm.isFinite() && printer.bedWidthMm > 0f) {
            "Printer build-plate width must be greater than zero."
        }
        require(printer.bedDepthMm.isFinite() && printer.bedDepthMm > 0f) {
            "Printer build-plate depth must be greater than zero."
        }
        require(printer.buildHeightMm.isFinite() && printer.buildHeightMm > 0f) {
            "Printer build height must be greater than zero."
        }
        require(printer.activeNozzleDiameterMm.isFinite() && printer.activeNozzleDiameterMm > 0f) {
            "Select a valid installed nozzle diameter before slicing."
        }
        require(printer.filamentDiameterMm.isFinite() && printer.filamentDiameterMm > 0f) {
            "Filament diameter must be greater than zero."
        }
        require(settings.layerHeightMm.isFinite() && settings.layerHeightMm > 0f) {
            "Layer height must be greater than zero."
        }
        require(settings.layerHeightMm <= printer.activeNozzleDiameterMm) {
            "Layer height cannot exceed the selected nozzle diameter."
        }
        require(settings.nozzleTemperatureC in 1..printer.maxNozzleTemperatureC) {
            "Nozzle temperature must be between 1°C and the printer profile's ${printer.maxNozzleTemperatureC}°C limit."
        }
        if (printer.heatedBed) {
            require(settings.bedTemperatureC in 0..printer.maxBedTemperatureC) {
                "Bed temperature exceeds the printer profile's ${printer.maxBedTemperatureC}°C limit."
            }
        }
        require(settings.printSpeedMmPerSecond in 1..300) {
            "Print speed must be between 1 and 300 mm/s."
        }
        require(settings.initialLayerSpeedMmPerSecond in 1..100) {
            "Initial-layer speed must be between 1 and 100 mm/s."
        }
        require(settings.infillPercent in 0..100) {
            "Infill must be between 0% and 100%."
        }
        require(settings.wallCount in 1..20 && settings.topBottomLayers in 0..30) {
            "Wall and top/bottom layer counts are outside supported ranges."
        }
        require(printer.gcodeFlavor.lowercase(Locale.ROOT) != "other") {
            "Choose a specific G-code flavor. The generic 'Other' option cannot be mapped safely to OrcaSlicer."
        }
    }

    private fun validateGcodeOutput(file: File) {
        val hasMotion = file.useLines { lines ->
            lines.any { line ->
                val command = line.trimStart()
                command.startsWith("G0 ") || command.startsWith("G1 ") ||
                    command == "G0" || command == "G1"
            }
        }
        require(hasMotion) {
            "The slicer output contains no movement commands. The generated file was rejected."
        }
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
        // Keep the slicer's extrusion mode explicit so it matches the R1+ M83 start code.
        set("use_relative_e_distances", "1")
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
        // OrcaSlicer validates relative extrusion against the before-layer-change
        // hook (the actual setting name is before_layer_change_gcode).
        // Resetting E here keeps the cumulative extrusion coordinate bounded.
        set("before_layer_change_gcode", ";BEFORE_LAYER_CHANGE\nG92 E0\n;[layer_z]")
        when (settings.buildPlateAdhesion.lowercase(Locale.ROOT)) {
            "brim" -> set("brim_type", "outer_only")
            "none" -> set("skirt_loops", "0")
            else -> set("skirt_loops", "2")
        }
        if (printer.startGcode.isNotBlank()) {
            // ctkjose/R1PLUSFIRMWARE defines a 1 mm probe offset by default and
            // implements M565. Emit the configured offset before G28, which applies it.
            val startGcode = buildString {
                if (printer.zProbeOffsetMm > 0f) {
                    append("M565 Z-")
                    append(decimal(printer.zProbeOffsetMm.toDouble()))
                    append("\n")
                }
                append(printer.startGcode)
            }
            set("machine_start_gcode", startGcode)
        }
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
