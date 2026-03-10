package com.fleet.bms.sniffer.infrastructure

import com.fleet.bms.sniffer.domain.model.CanIdEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

/**
 * Exports sniffing session to JSON for later analysis.
 */
class CanLogExporter(
    private val basePath: String = "/sdcard/can_logs"
) {

    private val json = Json { prettyPrint = true }

    suspend fun export(
        sessionLabel: String,
        startTime: Long,
        endTime: Long,
        framesTotal: Long,
        entries: List<CanIdEntry>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val dir = File(basePath)
            if (!dir.exists()) {
                dir.mkdirs()
            }

            val filename = "session_${startTime}_$sessionLabel.json"
            val file = File(dir, filename)

            val dto = ExportDto(
                session = sessionLabel,
                start = startTime,
                end = endTime,
                frames_total = framesTotal,
                unique_ids = entries.map { entry ->
                    UniqueIdDto(
                        id = "0x${entry.id.toString(16).uppercase()}",
                        id_hex = entry.idHex,
                        frame_count = entry.frameCount,
                        value_changes = entry.valueChangeCount,
                        last_value = entry.lastValueHex(),
                        first_seen = entry.firstSeen,
                        active_during = entry.activeDuringSessions.toList()
                    )
                }
            )

            FileOutputStream(file).use { fos ->
                fos.write(json.encodeToString(dto).toByteArray(Charsets.UTF_8))
            }

            Timber.i("Exported session to ${file.absolutePath}")
            Result.success(file.absolutePath)
        } catch (e: Exception) {
            Timber.e(e, "Failed to export session")
            Result.failure(e)
        }
    }

    @Serializable
    private data class ExportDto(
        val session: String,
        val start: Long,
        val end: Long,
        val frames_total: Long,
        val unique_ids: List<UniqueIdDto>
    )

    @Serializable
    private data class UniqueIdDto(
        val id: String,
        val id_hex: String,
        val frame_count: Long,
        val value_changes: Int,
        val last_value: String,
        val first_seen: Long,
        val active_during: List<String>
    )
}
