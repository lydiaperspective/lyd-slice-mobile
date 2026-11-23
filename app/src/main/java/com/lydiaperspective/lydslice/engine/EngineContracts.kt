package com.lydiaperspective.lydslice.engine

import com.lydiaperspective.lydslice.data.ModelPlacement
import com.lydiaperspective.lydslice.data.PrinterProfile
import com.lydiaperspective.lydslice.data.PrintSettings

/**
 * Boundary between the Android app and a future native/embedded slicing engine.
 * Implementations must not report success until a complete G-code file is produced.
 */
interface SlicingEngine {
    suspend fun slice(request: SliceRequest, onProgress: (Float) -> Unit): SliceResult
}

data class SliceRequest(
    val models: List<ModelPlacement>,
    val printer: PrinterProfile,
    val settings: PrintSettings
)

data class SliceResult(
    val gcodeFilePath: String,
    val estimatedPrintTimeSeconds: Long?,
    val filamentUsedMm: Float?,
    val warnings: List<String>
)

/**
 * Auto-arrangement should compare candidate layouts using slicer-derived estimates,
 * not just minimize occupied area. A geometry-only implementation can be supplied
 * first, then upgraded to evaluate candidates with SlicingEngine.
 */
interface BuildPlateArranger {
    suspend fun arrange(
        models: List<ModelPlacement>,
        printer: PrinterProfile,
        objective: ArrangementObjective
    ): List<ModelPlacement>
}

enum class ArrangementObjective {
    MINIMIZE_PRINT_TIME,
    MINIMIZE_BUILD_PLATE_AREA,
    MINIMIZE_HEIGHT
}
