package com.lydiaperspective.lydslice.network

/**
 * API boundary for OctoPrint. The implementation will use authenticated HTTPS where
 * available and keep API keys in Android Keystore-backed encrypted storage.
 */
data class OctoPrintConnection(
    val baseUrl: String,
    val apiKey: String
)

data class OctoPrintPrinterStatus(
    val connected: Boolean,
    val printing: Boolean,
    val stateText: String
)

interface OctoPrintClient {
    suspend fun testConnection(connection: OctoPrintConnection): Result<OctoPrintPrinterStatus>
    suspend fun uploadGcode(connection: OctoPrintConnection, filePath: String): Result<String>
    suspend fun startPrint(connection: OctoPrintConnection, uploadedFileName: String): Result<Unit>
}
