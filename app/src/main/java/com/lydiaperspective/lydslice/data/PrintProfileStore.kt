package com.lydiaperspective.lydslice.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object PrintProfileStore {
    private const val PREFS = "lyd_slice_profiles"
    private const val KEY_PROFILES = "custom_profiles"

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
        PrintProfile("ABS · Enclosed / low cooling", PrintSettings(
            material = "ABS", qualityBalance = 50f, layerHeightMm = 0.20f,
            infillPercent = 20, printSpeedMmPerSecond = 35, wallCount = 3,
            nozzleTemperatureC = 240, bedTemperatureC = 95, coolingFanPercent = 0,
            initialLayerSpeedMmPerSecond = 18
        ), isBuiltIn = true)
    )

    fun loadCustom(context: Context): List<PrintProfile> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_PROFILES, "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                PrintProfile(item.getString("name"), item.getJSONObject("settings").toSettings())
            }
        }.getOrDefault(emptyList())
    }

    fun saveCustom(context: Context, profiles: List<PrintProfile>) {
        val array = JSONArray()
        profiles.filterNot { it.isBuiltIn }.forEach { profile ->
            array.put(JSONObject().put("name", profile.name).put("settings", profile.settings.toJson()))
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_PROFILES, array.toString()).apply()
    }

    private fun PrintSettings.toJson() = JSONObject()
        .put("material", material).put("qualityBalance", qualityBalance.toDouble())
        .put("layerHeightMm", layerHeightMm.toDouble()).put("infillPercent", infillPercent)
        .put("printSpeedMmPerSecond", printSpeedMmPerSecond).put("wallCount", wallCount)
        .put("topBottomLayers", topBottomLayers).put("nozzleTemperatureC", nozzleTemperatureC)
        .put("bedTemperatureC", bedTemperatureC).put("coolingFanPercent", coolingFanPercent)
        .put("initialLayerSpeedMmPerSecond", initialLayerSpeedMmPerSecond)
        .put("supportEnabled", supportEnabled).put("buildPlateAdhesion", buildPlateAdhesion)

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
        supportEnabled = optBoolean("supportEnabled", false),
        buildPlateAdhesion = optString("buildPlateAdhesion", "Skirt")
    )
}
