package com.fleet.bms.domain.model

/**
 * Value Object: State of Charge (0-100%)
 */
@JvmInline
value class StateOfCharge(val value: Double) {
    init {
        require(value in 0.0..100.0) { "SOC must be between 0-100%, got: $value" }
    }
    
    fun isLow() = value < 20.0
    fun isCritical() = value < 10.0
    fun isHigh() = value > 80.0
}

/**
 * Value Object: Voltage (V)
 */
@JvmInline
value class Voltage(val value: Double) {
    init {
        require(value >= 0.0) { "Voltage cannot be negative: $value" }
    }
    
    companion object {
        const val MIN_SAFE_PACK_VOLTAGE = 285.0  // 114S * 2.5V
        const val MAX_SAFE_PACK_VOLTAGE = 478.8  // 114S * 4.2V
    }
    
    fun isSafe() = value in MIN_SAFE_PACK_VOLTAGE..MAX_SAFE_PACK_VOLTAGE
}

/**
 * Value Object: Current (A)
 * Positive = charging, Negative = discharging
 */
@JvmInline
value class Current(val value: Double) {
    fun isCharging() = value > 0.0
    fun isDischarging() = value < 0.0
    fun isIdle() = value == 0.0
}

/**
 * Value Object: Power (W)
 */
@JvmInline
value class Power(val value: Double) {
    companion object {
        fun calculate(voltage: Voltage, current: Current): Power {
            return Power(voltage.value * current.value)
        }
    }
}

/**
 * Value Object: Temperature Reading (°C)
 */
data class TemperatureReading(
    val min: Double,
    val max: Double,
    val avg: Double
) {
    init {
        require(min <= avg) { "Min temp cannot be greater than avg" }
        require(avg <= max) { "Avg temp cannot be greater than max" }
    }
    
    companion object {
        const val MIN_SAFE_TEMP = -20.0
        const val MAX_SAFE_TEMP = 60.0
        const val OPTIMAL_MIN = 15.0
        const val OPTIMAL_MAX = 35.0
    }
    
    fun isSafe() = min >= MIN_SAFE_TEMP && max <= MAX_SAFE_TEMP
    fun isOptimal() = min >= OPTIMAL_MIN && max <= OPTIMAL_MAX
}

/**
 * Value Object: GPS Location
 */
data class GpsLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float? = null
) {
    init {
        require(latitude in -90.0..90.0) { "Invalid latitude: $latitude" }
        require(longitude in -180.0..180.0) { "Invalid longitude: $longitude" }
    }
}

/**
 * Value Object: Battery Warnings
 */
data class BatteryWarnings(
    val hasLowVoltage: Boolean = false,
    val hasHighVoltage: Boolean = false,
    val hasLowTemperature: Boolean = false,
    val hasHighTemperature: Boolean = false,
    val hasCellImbalance: Boolean = false,
    val hasLowSoc: Boolean = false
) {
    fun hasAnyWarning() = hasLowVoltage || hasHighVoltage || 
                          hasLowTemperature || hasHighTemperature || 
                          hasCellImbalance || hasLowSoc
    
    fun criticalWarnings() = listOfNotNull(
        "Low Voltage".takeIf { hasLowVoltage },
        "High Voltage".takeIf { hasHighVoltage },
        "Low Temperature".takeIf { hasLowTemperature },
        "High Temperature".takeIf { hasHighTemperature },
        "Cell Imbalance".takeIf { hasCellImbalance },
        "Low Battery".takeIf { hasLowSoc }
    )
}
