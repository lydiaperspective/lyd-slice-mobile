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
import kotlin.math.sqrt

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
    private data class Orientation(val x: Float, val y: Float, val bounds: Bounds, val contactArea: Double)

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
            val orientation = chooseOrientation(mesh)
            val oriented = orientation.bounds
            val orientations = listOf(
                Triple(0f, oriented.width, oriented.depth),
                Triple(90f, oriented.depth, oriented.width)
            )
            var orientation = orientations.firstOrNull { (_, width, depth) ->
                cursorX + width + SPACING_MM <= bedW &&
                    cursorY + depth + SPACING_MM <= bedD
            }
            if (orientation == null) {
                cursorX = SPACING_MM
                cursorY += rowDepth + SPACING_MM
                rowDepth = 0.0
                orientation = orientations.firstOrNull { (_, width, depth) ->
                    cursorX + width + SPACING_MM <= bedW &&
                        cursorY + depth + SPACING_MM <= bedD
                }
            }
            val selectedOrientation = orientation ?: error(
                "${models[index].displayName} does not fit on the remaining build plate, even when rotated 90°. Remove a model or use a larger printer profile."
            )
            val (rotation, width, depth) = selectedOrientation
            val centerX = cursorX + width / 2.0
            val centerY = cursorY + depth / 2.0
            val printerX = if (printer.originAtCenter) centerX - bedW / 2.0 else centerX
            val printerY = if (printer.originAtCenter) centerY - bedD / 2.0 else centerY
            packed += models[index].copy(
                xMm = printerX.toFloat(),
                yMm = printerY.toFloat(),
                zMm = 0f,
                rotationXDegrees = orientation.x,
                rotationYDegrees = orientation.y,
                rotationZDegrees = rotation,
                sizeXmm = width.toFloat(),
                sizeYmm = depth.toFloat(),
                sizeZmm = oriented.height.toFloat()
            )
            cursorX += width + SPACING_MM
            rowDepth = max(rowDepth, depth)
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
            val sourceCenterX = (mesh.bounds.minX + mesh.bounds.maxX) / 2.0
            val sourceCenterY = (mesh.bounds.minY + mesh.bounds.maxY) / 2.0
            val sourceCenterZ = (mesh.bounds.minZ + mesh.bounds.maxZ) / 2.0
            val orientedBounds = transformedBounds(mesh, placement.rotationXDegrees, placement.rotationYDegrees)
            val rotatedMinZ = orientedBounds.minZ
            for (offset in mesh.vertices.indices step 9) {
                val transformed = DoubleArray(9)
                var minZ = Double.POSITIVE_INFINITY
                var maxZ = Double.NEGATIVE_INFINITY
                for (vertex in 0 until 3) {
                    val local = rotate(
                        mesh.vertices[offset + vertex * 3].toDouble() - sourceCenterX,
                        mesh.vertices[offset + vertex * 3 + 1].toDouble() - sourceCenterY,
                        mesh.vertices[offset + vertex * 3 + 2].toDouble() - sourceCenterZ,
                        placement.rotationXDegrees, placement.rotationYDegrees, placement.rotationZDegrees
                    )
                    val x = local[0]
                    val y = local[1]
                    val z = local[2] - rotatedMinZ + placement.zMm
                    transformed[vertex * 3] = placement.xMm + x
                    transformed[vertex * 3 + 1] = placement.yMm + y
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

    /**
     * Tests six axis-aligned bed-facing orientations. It prefers the largest flat
     * contact face, then lower height and a smaller footprint. This is a geometric
     * heuristic, not a full support-generation or print-time simulation.
     */
    private fun chooseOrientation(mesh: Mesh): Orientation {
        val candidates = listOf(0f to 0f, 180f to 0f, 90f to 0f, -90f to 0f, 0f to 90f, 0f to -90f)
            .map { (rx, ry) ->
                val bounds = transformedBounds(mesh, rx, ry)
                var contact = 0.0
                val cx = (mesh.bounds.minX + mesh.bounds.maxX) / 2.0
                val cy = (mesh.bounds.minY + mesh.bounds.maxY) / 2.0
                val cz = (mesh.bounds.minZ + mesh.bounds.maxZ) / 2.0
                for (offset in mesh.vertices.indices step 9) {
                    val p = (0..2).map { vertex ->
                        rotate(mesh.vertices[offset + vertex * 3].toDouble() - cx,
                            mesh.vertices[offset + vertex * 3 + 1].toDouble() - cy,
                            mesh.vertices[offset + vertex * 3 + 2].toDouble() - cz, rx, ry, 0f)
                    }
                    val ax = p[1][0] - p[0][0]; val ay = p[1][1] - p[0][1]; val az = p[1][2] - p[0][2]
                    val bx = p[2][0] - p[0][0]; val by = p[2][1] - p[0][1]; val bz = p[2][2] - p[0][2]
                    val nx = ay * bz - az * by; val ny = az * bx - ax * bz; val nz = ax * by - ay * bx
                    val twiceArea = sqrt(nx * nx + ny * ny + nz * nz)
                    if (p.all { it[2] <= bounds.minZ + 0.05 } && twiceArea > 1e-9 &&
                        kotlin.math.abs(nz) >= twiceArea * 0.995) contact += twiceArea / 2.0
                }
                Orientation(rx, ry, bounds, contact)
            }
        return candidates.maxWithOrNull(
            compareBy<Orientation> { it.contactArea }
                .thenBy { -it.bounds.height }
                .thenBy { -(it.bounds.width * it.bounds.depth) }
        ) ?: Orientation(0f, 0f, mesh.bounds, 0.0)
    }

    private fun transformedBounds(mesh: Mesh, rx: Float, ry: Float): Bounds {
        val cx = (mesh.bounds.minX + mesh.bounds.maxX) / 2.0
        val cy = (mesh.bounds.minY + mesh.bounds.maxY) / 2.0
        val cz = (mesh.bounds.minZ + mesh.bounds.maxZ) / 2.0
        var minX = Double.POSITIVE_INFINITY; var minY = minX; var minZ = minX
        var maxX = Double.NEGATIVE_INFINITY; var maxY = maxX; var maxZ = maxX
        for (i in mesh.vertices.indices step 3) {
            val p = rotate(mesh.vertices[i].toDouble() - cx, mesh.vertices[i + 1].toDouble() - cy,
                mesh.vertices[i + 2].toDouble() - cz, rx, ry, 0f)
            minX = min(minX, p[0]); maxX = max(maxX, p[0])
            minY = min(minY, p[1]); maxY = max(maxY, p[1])
            minZ = min(minZ, p[2]); maxZ = max(maxZ, p[2])
        }
        return Bounds(minX, minY, minZ, maxX, maxY, maxZ)
    }

    private fun rotate(x: Double, y: Double, z: Double, rx: Float, ry: Float, rz: Float): DoubleArray {
        val ax = rx * PI / 180.0; val ay = ry * PI / 180.0; val az = rz * PI / 180.0
        val x1 = x
        val y1 = y * cos(ax) - z * sin(ax)
        val z1 = y * sin(ax) + z * cos(ax)
        val x2 = x1 * cos(ay) + z1 * sin(ay)
        val y2 = y1
        val z2 = -x1 * sin(ay) + z1 * cos(ay)
        return doubleArrayOf(x2 * cos(az) - y2 * sin(az), x2 * sin(az) + y2 * cos(az), z2)
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
