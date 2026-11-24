package com.lydiaperspective.lydslice.engine

import android.content.Context
import android.net.Uri
import com.lydiaperspective.lydslice.data.ModelPlacement
import com.lydiaperspective.lydslice.data.PrinterProfile
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Combines multiple STL meshes into one binary STL for the native slicer.
 * Placement xMm/yMm represent each model's XY centre in printer coordinates.
 */
object StlPlateComposer {
    private const val MAX_TRIANGLES = 500_000
    private const val SPACING_MM = 3.0

    private data class Mesh(val vertices: FloatArray, val bounds: Bounds)
    private data class Bounds(val minX: Double, val minY: Double, val minZ: Double,
                              val maxX: Double, val maxY: Double, val maxZ: Double) {
        val width get() = maxX - minX
        val depth get() = maxY - minY
        val height get() = maxZ - minZ
    }

    fun arrange(context: Context, models: List<ModelPlacement>, printer: PrinterProfile): List<ModelPlacement> {
        require(models.isNotEmpty()) { "Import at least one STL model before arranging." }
        require(printer.buildPlateShape.equals("Rectangular", true)) {
            "Automatic arrangement currently supports rectangular build plates only."
        }
        val meshes = models.map { readMesh(openModel(context, it)) }
        val bedW = printer.bedWidthMm.toDouble()
        val bedD = printer.bedDepthMm.toDouble()
        val packed = mutableListOf<ModelPlacement>()
        var cursorX = SPACING_MM
        var cursorY = SPACING_MM
        var rowDepth = 0.0
        for (index in models.indices) {
            val mesh = meshes[index]
            require(mesh.bounds.width <= bedW && mesh.bounds.depth <= bedD) {
                "${models[index].displayName} is larger than the configured build plate."
            }
            if (cursorX + mesh.bounds.width + SPACING_MM > bedW) {
                cursorX = SPACING_MM
                cursorY += rowDepth + SPACING_MM
                rowDepth = 0.0
            }
            require(cursorY + mesh.bounds.depth + SPACING_MM <= bedD) {
                "The models do not fit on this build plate with ${SPACING_MM.toInt()} mm spacing. Remove a model or use a larger printer profile."
            }
            val centerX = cursorX + mesh.bounds.width / 2.0
            val centerY = cursorY + mesh.bounds.depth / 2.0
            val printerX = if (printer.originAtCenter) centerX - bedW / 2.0 else centerX
            val printerY = if (printer.originAtCenter) centerY - bedD / 2.0 else centerY
            packed += models[index].copy(
                xMm = printerX.toFloat(),
                yMm = printerY.toFloat(),
                zMm = 0f,
                rotationZDegrees = 0f,
                sizeXmm = mesh.bounds.width.toFloat(),
                sizeYmm = mesh.bounds.depth.toFloat(),
                sizeZmm = mesh.bounds.height.toFloat()
            )
            cursorX += mesh.bounds.width + SPACING_MM
            rowDepth = max(rowDepth, mesh.bounds.depth)
        }
        return packed
    }

    fun compose(
        context: Context,
        models: List<ModelPlacement>,
        printer: PrinterProfile,
        destination: File
    ): File {
        require(models.isNotEmpty()) { "No models were supplied for plate composition." }
        require(models.size <= 100) { "A single plate is limited to 100 imported models." }
        var totalTriangles = 0
        val all = models.map { model ->
            val mesh = readMesh(openModel(context, model))
            totalTriangles += mesh.vertices.size / 9
            require(totalTriangles <= MAX_TRIANGLES) {
                "The combined plate exceeds the supported triangle limit of $MAX_TRIANGLES."
            }
            model to mesh
        }
        require(totalTriangles > 0) { "The combined plate contains no triangles." }
        val plateMinX = if (printer.originAtCenter) -printer.bedWidthMm / 2.0 else 0.0
        val plateMinY = if (printer.originAtCenter) -printer.bedDepthMm / 2.0 else 0.0
        val plateMaxX = plateMinX + printer.bedWidthMm
        val plateMaxY = plateMinY + printer.bedDepthMm
        val plateMinZ = 0.0
        val output = ByteBuffer.allocate(84 + totalTriangles * 50).order(ByteOrder.LITTLE_ENDIAN)
        val header = "Lyd Slice multi-model plate".toByteArray(Charsets.US_ASCII)
        output.put(header.copyOf(80))
        output.putInt(totalTriangles)
        for ((placement, mesh) in all) {
            val angle = placement.rotationZDegrees * PI / 180.0
            val c = cos(angle)
            val s = sin(angle)
            val sourceCenterX = (mesh.bounds.minX + mesh.bounds.maxX) / 2.0
            val sourceCenterY = (mesh.bounds.minY + mesh.bounds.maxY) / 2.0
            for (offset in mesh.vertices.indices step 9) {
                val transformed = DoubleArray(9)
                var minZ = Double.POSITIVE_INFINITY
                var maxZ = Double.NEGATIVE_INFINITY
                for (vertex in 0 until 3) {
                    val x = mesh.vertices[offset + vertex * 3].toDouble() - sourceCenterX
                    val y = mesh.vertices[offset + vertex * 3 + 1].toDouble() - sourceCenterY
                    val z = mesh.vertices[offset + vertex * 3 + 2].toDouble() - mesh.bounds.minZ + placement.zMm
                    transformed[vertex * 3] = placement.xMm + x * c - y * s
                    transformed[vertex * 3 + 1] = placement.yMm + x * s + y * c
                    transformed[vertex * 3 + 2] = z
                    minZ = min(minZ, z)
                    maxZ = max(maxZ, z)
                }
                var insideXY = true
                for (vertex in 0 until 3) {
                    val x = transformed[vertex * 3]
                    val y = transformed[vertex * 3 + 1]
                    if (x < plateMinX - 0.01 || x > plateMaxX + 0.01 ||
                        y < plateMinY - 0.01 || y > plateMaxY + 0.01) insideXY = false
                }
                require(insideXY && minZ >= plateMinZ - 0.01 &&
                    maxZ <= printer.buildHeightMm + 0.01) {
                    "Model '${placement.displayName}' is outside the configured build volume. Use Auto arrange or adjust its placement."
                }
                val ax = transformed[3] - transformed[0]
                val ay = transformed[4] - transformed[1]
                val az = transformed[5] - transformed[2]
                val bx = transformed[6] - transformed[0]
                val by = transformed[7] - transformed[1]
                val bz = transformed[8] - transformed[2]
                var nx = ay * bz - az * by
                var ny = az * bx - ax * bz
                var nz = ax * by - ay * bx
                val length = kotlin.math.sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(1e-12)
                nx /= length; ny /= length; nz /= length
                output.putFloat(nx.toFloat()).putFloat(ny.toFloat()).putFloat(nz.toFloat())
                transformed.forEach { output.putFloat(it.toFloat()) }
                output.putShort(0)
            }
        }
        destination.parentFile?.mkdirs()
        destination.writeBytes(output.array())
        return destination
    }

    private fun openModel(context: Context, model: ModelPlacement): File {
        val uri = Uri.parse(model.uri)
        return when (uri.scheme) {
            "file" -> File(uri.path ?: error("Invalid model path"))
            "content" -> {
                val temp = File(context.cacheDir, "plate-${model.id}.stl")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    temp.outputStream().use { output -> input.copyTo(output) }
                } ?: error("Cannot open ${model.displayName}")
                temp
            }
            else -> error("Unsupported STL location for ${model.displayName}")
        }.also { require(it.isFile && it.length() >= 84L) { "${model.displayName} is not a valid STL file." } }
    }

    private fun readMesh(file: File): Mesh {
        require(file.length() <= 64L * 1024L * 1024L) { "STL file is larger than the 64 MB safety limit." }
        RandomAccessFile(file, "r").use { input ->
            val header = ByteArray(84)
            input.readFully(header)
            val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
            buffer.position(80)
            val count = buffer.int
            val expected = 84L + count.toLong() * 50L
            if (count > 0 && expected == file.length()) {
                require(count <= MAX_TRIANGLES) { "STL contains too many triangles." }
                val vertices = FloatArray(count * 9)
                val record = ByteArray(50)
                var minX = Double.POSITIVE_INFINITY; var minY = minX; var minZ = minX
                var maxX = Double.NEGATIVE_INFINITY; var maxY = maxX; var maxZ = maxX
                var target = 0
                repeat(count) {
                    input.readFully(record)
                    val tri = ByteBuffer.wrap(record).order(ByteOrder.LITTLE_ENDIAN)
                    tri.position(12)
                    repeat(3) {
                        val x = tri.float; val y = tri.float; val z = tri.float
                        require(x.isFinite() && y.isFinite() && z.isFinite()) {
                            "STL contains non-finite vertex coordinates."
                        }
                        vertices[target++] = x; vertices[target++] = y; vertices[target++] = z
                        minX = min(minX, x.toDouble()); minY = min(minY, y.toDouble()); minZ = min(minZ, z.toDouble())
                        maxX = max(maxX, x.toDouble()); maxY = max(maxY, y.toDouble()); maxZ = max(maxZ, z.toDouble())
                    }
                }
                require(maxX > minX && maxY > minY && maxZ > minZ) {
                    "STL has no measurable 3D volume."
                }
                return Mesh(vertices, Bounds(minX, minY, minZ, maxX, maxY, maxZ))
            }
        }
        val text = file.readText(Charsets.US_ASCII)
        val vertexPattern = Regex("""(?i)vertex\s+([-+0-9.eE]+)\s+([-+0-9.eE]+)\s+([-+0-9.eE]+)""")
        val matches = vertexPattern.findAll(text).toList()
        require(matches.isNotEmpty() && matches.size % 3 == 0) { "STL is neither a valid binary nor ASCII mesh." }
        require(matches.size / 3 <= MAX_TRIANGLES) { "STL contains too many triangles." }
        val vertices = FloatArray(matches.size * 3)
        var minX = Double.POSITIVE_INFINITY; var minY = minX; var minZ = minX
        var maxX = Double.NEGATIVE_INFINITY; var maxY = maxX; var maxZ = maxX
        matches.forEachIndexed { index, match ->
            val x = match.groupValues[1].toFloat(); val y = match.groupValues[2].toFloat(); val z = match.groupValues[3].toFloat()
            require(x.isFinite() && y.isFinite() && z.isFinite()) { "STL contains non-finite vertex coordinates." }
            vertices[index * 3] = x; vertices[index * 3 + 1] = y; vertices[index * 3 + 2] = z
            minX = min(minX, x.toDouble()); minY = min(minY, y.toDouble()); minZ = min(minZ, z.toDouble())
            maxX = max(maxX, x.toDouble()); maxY = max(maxY, y.toDouble()); maxZ = max(maxZ, z.toDouble())
        }
        require(maxX > minX && maxY > minY && maxZ > minZ) { "STL has no measurable 3D volume." }
        return Mesh(vertices, Bounds(minX, minY, minZ, maxX, maxY, maxZ))
    }
}
