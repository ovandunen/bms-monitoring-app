package com.fleet.bms.domain.model

/**
 * Value Object: Cell Voltages (114 cells)
 * 
 * Represents the individual cell voltages in a 114S battery pack.
 * Enforces validation rules for safety.
 */
data class CellVoltages(
    val voltages: List<Double>
) {
    companion object {
        const val CELL_COUNT = 114
        const val MIN_CELL_VOLTAGE = 2.5
        const val MAX_CELL_VOLTAGE = 4.2
        const val NOMINAL_CELL_VOLTAGE = 3.6
        const val MAX_SAFE_DELTA = 0.10  // 100mV max imbalance
    }
    
    init {
        require(voltages.size == CELL_COUNT) {
            "Must have exactly $CELL_COUNT cell voltages, got ${voltages.size}"
        }
        require(voltages.all { it in MIN_CELL_VOLTAGE..MAX_CELL_VOLTAGE }) {
            val invalid = voltages.withIndex().filter { 
                it.value !in MIN_CELL_VOLTAGE..MAX_CELL_VOLTAGE 
            }
            "All cell voltages must be between $MIN_CELL_VOLTAGE and $MAX_CELL_VOLTAGE V. " +
            "Invalid cells: ${invalid.map { "Cell ${it.index}: ${it.value}V" }}"
        }
    }
    
    fun min(): Double = voltages.minOrNull() ?: 0.0
    fun max(): Double = voltages.maxOrNull() ?: 0.0
    fun delta(): Double = max() - min()
    fun average(): Double = voltages.average()
    
    fun isBalanced(maxDelta: Double = MAX_SAFE_DELTA): Boolean = delta() <= maxDelta
    
    fun getImbalancedCells(threshold: Double = MAX_SAFE_DELTA): List<Pair<Int, Double>> {
        val avg = average()
        return voltages.withIndex()
            .filter { kotlin.math.abs(it.value - avg) > threshold }
            .map { it.index to it.value }
    }
    
    fun getLowVoltageCells(threshold: Double = 3.0): List<Pair<Int, Double>> {
        return voltages.withIndex()
            .filter { it.value < threshold }
            .map { it.index to it.value }
    }
    
    fun getHighVoltageCells(threshold: Double = 4.1): List<Pair<Int, Double>> {
        return voltages.withIndex()
            .filter { it.value > threshold }
            .map { it.index to it.value }
    }
    
    override fun toString(): String {
        return "CellVoltages(count=$CELL_COUNT, min=${min()}V, max=${max()}V, " +
               "delta=${delta()}V, avg=${average()}V, balanced=${isBalanced()})"
    }
}
