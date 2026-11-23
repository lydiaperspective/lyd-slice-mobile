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
    val layerHeightMm: Float = 0.2f,
    val infillPercent: Int = 20,
    val printSpeedMmPerSecond: Int = 50,
    val wallCount: Int = 2,
    val topBottomLayers: Int = 4,
    val nozzleTemperatureC: Int = 200,
    val bedTemperatureC: Int = 60,
    val supportEnabled: Boolean = false,
    val buildPlateAdhesion: String = "Skirt"
)

data class PrinterProfile(
    val name: String = "Robo 3D R1 Plus",
    val bedWidthMm: Float =  220f,
    val bedDepthMm: Float = 220f,
    val buildHeightMm: Float = 250f,
    val nozzleDiameterMm: Float = 0.4f,
    val filamentDiameterMm: Float = 1.75f,
    val heatedBed: Boolean = true
)
