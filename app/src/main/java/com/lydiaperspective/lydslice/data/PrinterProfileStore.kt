package com.lydiaperspective.lydslice.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object PrinterProfileStore {
    private const val PREFS = "lyd_slice_printers"
    private const val KEY_PROFILES = "custom_printer_profiles"
    private const val KEY_SELECTED = "selected_printer_profile"

    fun builtInProfiles(): List<PrinterProfile> = listOf(
        PrinterProfile(
            id = "robo3d-r1-plus",
            name = "Robo 3D R1 Plus",
            bedWidthMm = 220f,
            bedDepthMm = 220f,
            buildHeightMm = 250f,
            buildPlateShape = "Rectangular",
            heatedBed = true,
            nozzleDiametersMm = listOf(0.4f),
            activeNozzleDiameterMm = 0.4f,
            filamentDiameterMm = 1.75f,
            extruderCount = 1,
            gcodeFlavor = "Marlin",
            maxNozzleTemperatureC = 290,
            maxBedTemperatureC = 100,
            isBuiltIn = true
        ),
        PrinterProfile(
            id = "generic-fff",
            name = "Generic FFF Printer",
            bedWidthMm = 200f,
            bedDepthMm = 200f,
            buildHeightMm = 200f,
            heatedBed = false,
            nozzleDiametersMm = listOf(0.4f),
            activeNozzleDiameterMm = 0.4f,
            filamentDiameterMm = 1.75f,
            extruderCount = 1,
            gcodeFlavor = "Marlin",
            isBuiltIn = true
        )
    )

    fun loadCustom(context: Context): List<PrinterProfile> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_PROFILES, "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { array.getJSONObject(it).toPrinterProfile() }
        }.getOrDefault(emptyList())
    }

    fun selectedId(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_SELECTED, null)

    fun saveSelectedId(context: Context, id: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_SELECTED, id).apply()
    }

    fun saveCustom(context: Context, profiles: List<PrinterProfile>) {
        val array = JSONArray()
        profiles.filterNot { it.isBuiltIn }.forEach { array.put(it.toJson()) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_PROFILES, array.toString()).apply()
    }

    private fun PrinterProfile.toJson() = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("bedWidthMm", bedWidthMm.toDouble())
        .put("bedDepthMm", bedDepthMm.toDouble())
        .put("buildHeightMm", buildHeightMm.toDouble())
        .put("buildPlateShape", buildPlateShape)
        .put("originAtCenter", originAtCenter)
        .put("heatedBed", heatedBed)
        .put("heatedBuildVolume", heatedBuildVolume)
        .put("nozzleDiametersMm", JSONArray(nozzleDiametersMm))
        .put("activeNozzleDiameterMm", activeNozzleDiameterMm.toDouble())
        .put("filamentDiameterMm", filamentDiameterMm.toDouble())
        .put("extruderCount", extruderCount)
        .put("gcodeFlavor", gcodeFlavor)
        .put("headXMinMm", headXMinMm.toDouble())
        .put("headYMinMm", headYMinMm.toDouble())
        .put("headXMaxMm", headXMaxMm.toDouble())
        .put("headYMaxMm", headYMaxMm.toDouble())
        .put("gantryHeightMm", gantryHeightMm.toDouble())
        .put("maxNozzleTemperatureC", maxNozzleTemperatureC)
        .put("maxBedTemperatureC", maxBedTemperatureC)
        .put("startGcode", startGcode)
        .put("endGcode", endGcode)

    private fun JSONObject.toPrinterProfile(): PrinterProfile {
        val nozzles = optJSONArray("nozzleDiametersMm") ?: JSONArray().put(0.4)
        val nozzleValues = (0 until nozzles.length()).mapNotNull { nozzles.optDouble(it).toFloat().takeIf { v -> v > 0f } }
            .ifEmpty { listOf(0.4f) }
        return PrinterProfile(
            id = optString("id", java.util.UUID.randomUUID().toString()),
            name = optString("name", "Custom FFF Printer"),
            bedWidthMm = optDouble("bedWidthMm", 200.0).toFloat(),
            bedDepthMm = optDouble("bedDepthMm", 200.0).toFloat(),
            buildHeightMm = optDouble("buildHeightMm", 200.0).toFloat(),
            buildPlateShape = optString("buildPlateShape", "Rectangular"),
            originAtCenter = optBoolean("originAtCenter", false),
            heatedBed = optBoolean("heatedBed", false),
            heatedBuildVolume = optBoolean("heatedBuildVolume", false),
            nozzleDiametersMm = nozzleValues,
            activeNozzleDiameterMm = optDouble("activeNozzleDiameterMm", nozzleValues.first().toDouble()).toFloat(),
            filamentDiameterMm = optDouble("filamentDiameterMm", 1.75).toFloat(),
            extruderCount = optInt("extruderCount", 1),
            gcodeFlavor = optString("gcodeFlavor", "Marlin"),
            headXMinMm = optDouble("headXMinMm", 0.0).toFloat(),
            headYMinMm = optDouble("headYMinMm", 0.0).toFloat(),
            headXMaxMm = optDouble("headXMaxMm", 0.0).toFloat(),
            headYMaxMm = optDouble("headYMaxMm", 0.0).toFloat(),
            gantryHeightMm = optDouble("gantryHeightMm", 0.0).toFloat(),
            maxNozzleTemperatureC = optInt("maxNozzleTemperatureC", 290),
            maxBedTemperatureC = optInt("maxBedTemperatureC", 100),
            startGcode = optString("startGcode", ""),
            endGcode = optString("endGcode", "")
        )
    }
}
