package com.fleet.bms.sniffer.domain.usecase

import com.fleet.bms.sniffer.domain.model.CanIdCandidate
import com.fleet.bms.sniffer.domain.model.CanIdEntry
import com.fleet.bms.sniffer.infrastructure.CanIdRegistry

/**
 * Detects likely CAN-ID candidates for known metrics
 * based on observed behavior patterns.
 */
class DetectCanCandidatesUseCase(
    private val registry: CanIdRegistry
) {

    fun detectLikelyCandidates(): List<CanIdCandidate> {
        val entries = registry.getEntries().values
        val candidates = mutableListOf<CanIdCandidate>()

        for (entry in entries) {
            val changeRate = if (entry.frameCount > 0) {
                entry.valueChangeCount.toFloat() / entry.frameCount
            } else 0f

            when {
                // SOC: changes slowly, value range 0-100
                changeRate in 0.001f..0.05f && entry.lastValue.isNotEmpty() -> {
                    val firstByte = entry.lastValue[0].toUByte().toInt()
                    if (firstByte in 0..100 || entry.lastValue.size >= 2) {
                        candidates.add(CanIdCandidate(
                            id = entry.id,
                            idHex = entry.idHex,
                            metricType = CanIdCandidate.MetricType.SOC,
                            score = 0.7f,
                            frameCount = entry.frameCount,
                            valueChangeCount = entry.valueChangeCount,
                            reason = "Slow change rate, possible 0-100 range"
                        ))
                    }
                }

                // Voltage: stable high value (~377V for 114S LFP)
                changeRate < 0.01f && entry.lastValue.size >= 2 -> {
                    val possibleVoltage = (entry.lastValue[0].toUByte().toInt() shl 8) or
                            entry.lastValue[1].toUByte().toInt()
                    if (possibleVoltage in 300..420) {
                        candidates.add(CanIdCandidate(
                            id = entry.id,
                            idHex = entry.idHex,
                            metricType = CanIdCandidate.MetricType.VOLTAGE,
                            score = 0.8f,
                            frameCount = entry.frameCount,
                            valueChangeCount = entry.valueChangeCount,
                            reason = "Stable value in voltage range (300-420V)"
                        ))
                    }
                }

                // Current: changes fast, positive/negative
                changeRate > 0.3f && entry.lastValue.size >= 2 -> {
                    candidates.add(CanIdCandidate(
                        id = entry.id,
                        idHex = entry.idHex,
                        metricType = CanIdCandidate.MetricType.CURRENT,
                        score = 0.75f,
                        frameCount = entry.frameCount,
                        valueChangeCount = entry.valueChangeCount,
                        reason = "High change rate, likely dynamic sensor"
                    ))
                }

                // Speed: changes with movement
                changeRate in 0.1f..0.5f -> {
                    candidates.add(CanIdCandidate(
                        id = entry.id,
                        idHex = entry.idHex,
                        metricType = CanIdCandidate.MetricType.SPEED,
                        score = 0.6f,
                        frameCount = entry.frameCount,
                        valueChangeCount = entry.valueChangeCount,
                        reason = "Medium change rate, possible speed"
                    ))
                }

                // RPM: changes fast when driving
                changeRate > 0.5f -> {
                    candidates.add(CanIdCandidate(
                        id = entry.id,
                        idHex = entry.idHex,
                        metricType = CanIdCandidate.MetricType.RPM,
                        score = 0.65f,
                        frameCount = entry.frameCount,
                        valueChangeCount = entry.valueChangeCount,
                        reason = "Very high change rate"
                    ))
                }

                // Temperature: very slow change
                changeRate < 0.001f && entry.frameCount > 100 -> {
                    candidates.add(CanIdCandidate(
                        id = entry.id,
                        idHex = entry.idHex,
                        metricType = CanIdCandidate.MetricType.TEMPERATURE,
                        score = 0.6f,
                        frameCount = entry.frameCount,
                        valueChangeCount = entry.valueChangeCount,
                        reason = "Very slow change, possible temperature"
                    ))
                }

                // Unknown but active
                entry.valueChangeCount > 0 -> {
                    candidates.add(CanIdCandidate(
                        id = entry.id,
                        idHex = entry.idHex,
                        metricType = CanIdCandidate.MetricType.UNKNOWN,
                        score = 0.5f,
                        frameCount = entry.frameCount,
                        valueChangeCount = entry.valueChangeCount,
                        reason = "Active ID, type unclear"
                    ))
                }
            }
        }

        return candidates.sortedByDescending { it.score }
    }
}
