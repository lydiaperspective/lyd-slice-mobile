package com.lydiaperspective.lydslice.data

import java.util.UUID

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

/**
 * Machine-level configuration inspired by Cura's Printer / Extruder settings.
 * The built-in R1+ dimensions follow Marlin's published machine reference and still
 * require confirmation against the usable travel of the individual physical printer.
 */
data class PrinterProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Custom FFF Printer",
    val bedWidthMm: Float = 200f,
    val bedDepthMm: Float = 200f,
    val buildHeightMm: Float = 200f,
    val buildPlateShape: String = "Rectangular",
    val originAtCenter: Boolean = false,
    val heatedBed: Boolean = false,
    val heatedBuildVolume: Boolean = false,
    val nozzleDiametersMm: List<Float> = listOf(0.4f),
    val activeNozzleDiameterMm: Float = 0.4f,
    val filamentDiameterMm: Float = 1.75f,
    val extruderCount: Int = 1,
    val gcodeFlavor: String = "Marlin",
    val headXMinMm: Float = 0f,
    val headYMinMm: Float = 0f,
    val headXMaxMm: Float = 0f,
    val headYMaxMm: Float = 0f,
    val gantryHeightMm: Float = 0f,
    val maxNozzleTemperatureC: Int = 290,
    val maxBedTemperatureC: Int = 100,
    /** Positive magnitude in mm; emitted as M565 Z-negative for R1+ firmware. */
    val zProbeOffsetMm: Float = 0f,
    val startGcode: String = "",
    val endGcode: String = "",
    val isBuiltIn: Boolean = false
)
