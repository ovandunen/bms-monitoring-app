package com.fleet.bms.sniffer.domain.usecase

import com.fleet.bms.sniffer.domain.model.CanIdEntry
import com.fleet.bms.sniffer.infrastructure.CanIdRegistry
import com.fleet.bms.sniffer.infrastructure.CanLogExporter
import com.fleet.bms.sniffer.infrastructure.CanSocketReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber

/**
 * Use Case: CAN Sniffing Session
 *
 * Session-based comparison:
 * - baseline: ignition OFF
 * - ignition_on: VCU startup frames
 * - charging: SOC, voltage
 * - driving: speed, current, RPM
 * - braking: regen current
 */
class CanSniffingUseCase(
    private val socketReader: CanSocketReader,
    private val registry: CanIdRegistry,
    private val exporter: CanLogExporter,
    private val scope: CoroutineScope
) {

    private var sniffJob: Job? = null
    private var sessionStartTime: Long = 0
    private var currentSessionLabel: String? = null

    private val _framesPerSecond = MutableStateFlow(0f)
    val framesPerSecond: StateFlow<Float> = _framesPerSecond.asStateFlow()

    private val _isSniffing = MutableStateFlow(false)
    val isSniffing: StateFlow<Boolean> = _isSniffing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var frameCountLastSecond = 0
    private var lastFpsUpdate = System.currentTimeMillis()

    fun startSession(label: String) {
        if (_isSniffing.value) {
            Timber.w("Already sniffing, stop first")
            return
        }
        currentSessionLabel = label.ifBlank { "session" }
        sessionStartTime = System.currentTimeMillis()
        _error.value = null

        sniffJob = socketReader.readFrames()
            .onEach { frame ->
                registry.recordFrame(frame, currentSessionLabel)
                updateFps()
            }
            .catch { e ->
                Timber.e(e, "Sniffing error")
                _error.value = e.message ?: "CAN read failed"
                _isSniffing.value = false
            }
            .launchIn(scope)

        _isSniffing.value = true
        Timber.i("Started sniffing session: $currentSessionLabel")
    }

    fun stopSession() {
        sniffJob?.cancel()
        sniffJob = null
        socketReader.close()
        _isSniffing.value = false
        _framesPerSecond.value = 0f
        Timber.i("Stopped sniffing session: $currentSessionLabel")
    }

    fun getEntries(): List<CanIdEntry> = registry.getEntriesSortedByChangeFrequency()

    fun getActiveIds(): Set<Int> = registry.getActiveIds()

    fun getIdsNotInBaseline(baselineIds: Set<Int>): List<Int> =
        registry.getEntries().keys.filter { it !in baselineIds }

    fun clearRegistry() {
        registry.clear()
    }

    suspend fun exportCurrentSession(): Result<String> {
        val label = currentSessionLabel ?: "session"
        val entries = registry.getEntries().values.toList()
        val endTime = System.currentTimeMillis()
        return exporter.export(
            sessionLabel = label,
            startTime = sessionStartTime,
            endTime = endTime,
            framesTotal = registry.getFrameCount(),
            entries = entries
        )
    }

    private fun updateFps() {
        frameCountLastSecond++
        val now = System.currentTimeMillis()
        if (now - lastFpsUpdate >= 1000) {
            _framesPerSecond.value = frameCountLastSecond.toFloat()
            frameCountLastSecond = 0
            lastFpsUpdate = now
        }
    }
}
