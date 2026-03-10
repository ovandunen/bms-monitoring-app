package com.fleet.bms.domain.service

import com.fleet.bms.domain.model.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TelemetryAggregatorTest {
    
    private lateinit var aggregator: TelemetryAggregator
    private val batteryPackId = BatteryPackId("test-pack-id")
    private val vehicleId = VehicleId("test-vehicle-id")
    
    @Before
    fun setup() {
        aggregator = TelemetryAggregator(batteryPackId, vehicleId)
    }
    
    @Test
    fun `should aggregate pack status`() {
        val packStatus = PackStatus(
            soc = StateOfCharge(85.0),
            voltage = Voltage(370.0),
            current = Current(-45.0),
            power = Power(-16650.0)
        )
        
        val result = aggregator.aggregate(ParsedCanData.PackStatus(packStatus))
        
        assertTrue(result is AggregationResult.Incomplete)
        assertTrue((result as AggregationResult.Incomplete).percentage > 0)
    }
    
    @Test
    fun `should return complete when all data received`() {
        // Add pack status
        aggregator.aggregate(ParsedCanData.PackStatus(
            PackStatus(
                soc = StateOfCharge(85.0),
                voltage = Voltage(370.0),
                current = Current(-45.0),
                power = Power(-16650.0)
            )
        ))
        
        // Add temperatures
        aggregator.aggregate(ParsedCanData.Temperatures(
            TemperatureReading(min = 28.0, max = 32.0, avg = 30.0)
        ))
        
        // Add all 114 cell voltages (15 messages, 8 cells each)
        for (i in 0..14) {
            val voltages = List(8) { 3.6 }
            aggregator.aggregate(ParsedCanData.CellVoltages(voltages, i * 8))
        }
        
        val result = aggregator.aggregate(ParsedCanData.CellVoltages(
            List(6) { 3.6 }, 112  // Last 2 cells
        ))
        
        assertTrue(result is AggregationResult.Complete)
        val telemetry = (result as AggregationResult.Complete).telemetry
        
        assertEquals(batteryPackId, telemetry.batteryPackId)
        assertEquals(vehicleId, telemetry.vehicleId)
        assertEquals(85.0, telemetry.stateOfCharge.value, 0.001)
        assertEquals(370.0, telemetry.voltage.value, 0.001)
    }
    
    @Test
    fun `should reset after complete aggregation`() {
        // Complete one cycle
        aggregator.aggregate(ParsedCanData.PackStatus(
            PackStatus(
                soc = StateOfCharge(85.0),
                voltage = Voltage(370.0),
                current = Current(-45.0),
                power = Power(-16650.0)
            )
        ))
        aggregator.aggregate(ParsedCanData.Temperatures(
            TemperatureReading(min = 28.0, max = 32.0, avg = 30.0)
        ))
        for (i in 0..14) {
            aggregator.aggregate(ParsedCanData.CellVoltages(List(8) { 3.6 }, i * 8))
        }
        
        aggregator.reset()
        
        // Should be incomplete after reset
        val result = aggregator.aggregate(ParsedCanData.PackStatus(
            PackStatus(
                soc = StateOfCharge(80.0),
                voltage = Voltage(365.0),
                current = Current(-40.0),
                power = Power(-14600.0)
            )
        ))
        
        assertTrue(result is AggregationResult.Incomplete)
    }
}
