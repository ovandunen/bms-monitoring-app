package com.fleet.bms.sniffer.domain.model

/**
 * Likely candidate for a known metric based on observed behavior.
 */
data class CanIdCandidate(
    val id: Int,
    val idHex: String,
    val metricType: MetricType,
    val score: Float,
    val frameCount: Long,
    val valueChangeCount: Int,
    val reason: String
) {
    enum class MetricType {
        SOC,           // State of charge - changes slowly, 0-100
        VOLTAGE,       // Pack voltage - stable high value
        CURRENT,       // Charge/discharge - changes fast, +/- 
        SPEED,         // Changes with acceleration, starts at 0
        RPM,           // Changes fast when driving, 0 when stopped
        TEMPERATURE,   // Changes very slowly, narrow range
        UNKNOWN        // Active but type unclear
    }
}
