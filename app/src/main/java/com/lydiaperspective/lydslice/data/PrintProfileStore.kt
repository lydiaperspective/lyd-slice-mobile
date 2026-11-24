package com.lydiaperspective.lydslice.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object PrintProfileStore {
    private const val PREFS = "lyd_slice_profiles"
    private const val KEY_PROFILES = "custom_profiles"
    private const val KEY_LEGACY_MIGRATED = "custom_profiles_printer_scoped_migrated"

    private fun keyForPrinter(printerProfileId: String) = "${KEY_PROFILES}_$printerProfileId"

    fun builtInProfiles(): List<PrintProfile> = listOf(
        PrintProfile("PLA · Balanced", PrintSettings(), isBuiltIn = true),
        PrintProfile("PLA · Fine detail", PrintSettings(
            qualityBalance = 20f, layerHeightMm = 0.12f, infillPercent = 15,
            printSpeedMmPerSecond = 30, wallCount = 3, nozzleTemperatureC = 200,
            bedTemperatureC = 60, initialLayerSpeedMmPerSecond = 18
        ), isBuiltIn = true),
        PrintProfile("PLA · Faster draft", PrintSettings(
            qualityBalance = 85f, layerHeightMm = 0.27f, infillPercent = 15,
            printSpeedMmPerSecond = 50, wallCount = 2, nozzleTemperatureC = 205,
            bedTemperatureC = 55, initialLayerSpeedMmPerSecond = 22
        ), isBuiltIn = true),
        PrintProfile("ABS · Ventilated / low cooling", PrintSettings(
            material = "ABS", qualityBalance = 50f, layerHeightMm = 0.20f,
            infillPercent = 20, printSpeedMmPerSecond = 35, wallCount = 3,
            nozzleTemperatureC = 240, bedTemperatureC = 95, coolingFanPercent = 0,
            initialLayerSpeedMmPerSecond = 18
        ), isBuiltIn = true)
    )

    fun loadCustom(context: Context, printerProfileId: String): List<PrintProfile> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val scopedKey = keyForPrinter(printerProfileId)
        if (!prefs.contains(scopedKey) && !prefs.getBoolean(KEY_LEGACY_MIGRATED, false)) {
            val legacy = prefs.getString(KEY_PROFILES, "[]") ?: "[]"
            prefs.edit().putString(scopedKey, legacy)
                .putBoolean(KEY_LEGACY_MIGRATED, true).apply()
        }
        val raw = prefs.getString(scopedKey, "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                PrintProfile(
                    name = item.getString("name"),
                    settings = item.getJSONObject("settings").toSettings(),
                    id = item.optString("id").ifBlank { java.util.UUID.randomUUID().toString() }
                )
            }
        }.getOrDefault(emptyList())
    }

    fun saveCustom(context: Context, printerProfileId: String, profiles: List<PrintProfile>) {
        val array = JSONArray()
        profiles.filterNot { it.isBuiltIn }.forEach { profile ->
            array.put(JSONObject().put("id", profile.id).put("name", profile.name)
                .put("settings", profile.settings.toJson()))
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(keyForPrinter(printerProfileId), array.toString()).apply()
    }

    fun deleteForPrinter(context: Context, printerProfileId: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove(keyForPrinter(printerProfileId)).apply()
    }

    private fun PrintSettings.toJson() = JSONObject()
        .put("material", material).put("qualityBalance", qualityBalance.toDouble())
        .put("layerHeightMm", layerHeightMm.toDouble()).put("infillPercent", infillPercent)
        .put("printSpeedMmPerSecond", printSpeedMmPerSecond).put("wallCount", wallCount)
        .put("topBottomLayers", topBottomLayers).put("nozzleTemperatureC", nozzleTemperatureC)
        .put("bedTemperatureC", bedTemperatureC).put("coolingFanPercent", coolingFanPercent)
        .put("initialLayerSpeedMmPerSecond", initialLayerSpeedMmPerSecond)
        .put("initialLayerInfillSpeedMmPerSecond", initialLayerInfillSpeedMmPerSecond)
        .put("firstLayerFlowPercent", firstLayerFlowPercent)
        .put("firstLayerFanOffLayers", firstLayerFanOffLayers)
        .put("brimWidthMm", brimWidthMm.toDouble()).put("raftLayers", raftLayers)
        .put("skirtLoops", skirtLoops)
        .put("supportEnabled", supportEnabled).put("supportType", supportType)
        .put("buildPlateAdhesion", buildPlateAdhesion)

    private fun JSONObject.toSettings() = PrintSettings(
        material = optString("material", "PLA"),
        qualityBalance = optDouble("qualityBalance", 50.0).toFloat(),
        layerHeightMm = optDouble("layerHeightMm", 0.20).toFloat(),
        infillPercent = optInt("infillPercent", 20),
        printSpeedMmPerSecond = optInt("printSpeedMmPerSecond", 40),
        wallCount = optInt("wallCount", 2),
        topBottomLayers = optInt("topBottomLayers", 4),
        nozzleTemperatureC = optInt("nozzleTemperatureC", 205),
        bedTemperatureC = optInt("bedTemperatureC", 60),
        coolingFanPercent = optInt("coolingFanPercent", 100),
        initialLayerSpeedMmPerSecond = optInt("initialLayerSpeedMmPerSecond", 20),
        initialLayerInfillSpeedMmPerSecond = optInt("initialLayerInfillSpeedMmPerSecond", 20),
        firstLayerFlowPercent = optInt("firstLayerFlowPercent", 100),
        firstLayerFanOffLayers = optInt("firstLayerFanOffLayers", 1),
        brimWidthMm = optDouble("brimWidthMm", 5.0).toFloat(),
        raftLayers = optInt("raftLayers", 0),
        skirtLoops = optInt("skirtLoops", 2),
        supportEnabled = optBoolean("supportEnabled", false),
        supportType = optString("supportType", "tree(auto)"),
        buildPlateAdhesion = optString("buildPlateAdhesion", "Brim")
    )
}
