package com.lydiaperspective.lydslice.data

data class ModelPlacement(
    val id: String,
    val displayName: String,
    val uri: String,
    val xMm: Float = 0f,
    val yMm: Float = 0f,
    val zMm: Float = 0f,
    val rotationZDegrees: Float = 0f,
    val sizeXmm: Float = 20f,
    val sizeYmm: Float = 20f,
    val sizeZmm: Float = 20f
)

data class PrintSettings(
    val material: String = "PLA",
    val qualityBalance: Float = 50f,
    val layerHeightMm: Float = 0.20f,
    val infillPercent: Int = 20,
    val printSpeedMmPerSecond: Int = 40,
    val wallCount: Int = 2,
    val topBottomLayers: Int = 4,
    val nozzleTemperatureC: Int = 205,
    val bedTemperatureC: Int = 60,
    val coolingFanPercent: Int = 100,
    val initialLayerSpeedMmPerSecond: Int = 20,
    val supportEnabled: Boolean = false,
    val buildPlateAdhesion: String = "Skirt"
)

data class PrintProfile(
    val name: String,
    val settings: PrintSettings,
    val isBuiltIn: Boolean = false
)

data class PrinterProfile(
    val name: String = "Robo 3D R1 Plus",
    // These dimensions remain provisional until verified against the user's machine.
    val bedWidthMm: Float = 220f,
    val bedDepthMm: Float = 220f,
    val buildHeightMm: Float = 250f,
    val nozzleDiameterMm: Float = 0.4f,
    val filamentDiameterMm: Float = 1.75f,
    val heatedBed: Boolean = true
)
