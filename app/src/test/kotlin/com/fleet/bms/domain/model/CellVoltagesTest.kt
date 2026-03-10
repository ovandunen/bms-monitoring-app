package com.fleet.bms.domain.model

import org.junit.Assert.*
import org.junit.Test

class CellVoltagesTest {
    
    @Test
    fun `should create valid cell voltages`() {
        val voltages = List(114) { 3.6 }
        val cellVoltages = CellVoltages(voltages)
        
        assertEquals(114, cellVoltages.voltages.size)
        assertEquals(3.6, cellVoltages.min(), 0.001)
        assertEquals(3.6, cellVoltages.max(), 0.001)
        assertEquals(0.0, cellVoltages.delta(), 0.001)
    }
    
    @Test
    fun `should reject invalid cell count`() {
        val voltages = List(100) { 3.6 }
        
        val exception = assertThrows(IllegalArgumentException::class.java) {
            CellVoltages(voltages)
        }
        
        assertTrue(exception.message!!.contains("Must have exactly 114"))
    }
    
    @Test
    fun `should reject voltages out of range`() {
        val voltages = List(114) { 5.0 }  // Too high
        
        val exception = assertThrows(IllegalArgumentException::class.java) {
            CellVoltages(voltages)
        }
        
        assertTrue(exception.message!!.contains("between 2.5 and 4.2"))
    }
    
    @Test
    fun `should detect imbalanced cells`() {
        val voltages = MutableList(114) { 3.6 }
        voltages[0] = 3.3  // Low cell
        voltages[50] = 3.8  // High cell
        
        val cellVoltages = CellVoltages(voltages)
        
        assertFalse(cellVoltages.isBalanced(maxDelta = 0.10))
        assertEquals(0.5, cellVoltages.delta(), 0.001)
    }
    
    @Test
    fun `should identify low voltage cells`() {
        val voltages = MutableList(114) { 3.6 }
        voltages[5] = 2.9
        voltages[10] = 2.8
        
        val cellVoltages = CellVoltages(voltages)
        val lowCells = cellVoltages.getLowVoltageCells(3.0)
        
        assertEquals(2, lowCells.size)
        assertEquals(5, lowCells[0].first)
        assertEquals(10, lowCells[1].first)
    }
}
