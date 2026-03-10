package com.fleet.bms.domain.service

import com.fleet.bms.domain.model.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.Instant

class AlertEvaluatorTest {
    
    private lateinit var evaluator: AlertEvaluator
    
    @Before
    fun setup() {
        evaluator = AlertEvaluator()
    }
    
    @Test
    fun `should detect low voltage alert`() {
        val telemetry = createTelemetry(voltage = 290.0)
        
        val alerts = evaluator.evaluate(telemetry)
        
        assertTrue(alerts.any { it is BatteryAlert.VoltageLow })
    }
    
    @Test
    fun `should detect critical low voltage`() {
        val telemetry = createTelemetry(voltage = 280.0)
        
        val alerts = evaluator.evaluate(telemetry)
        
        assertTrue(alerts.any { 
            it is BatteryAlert.CriticalVoltageLow && it.severity == AlertSeverity.CRITICAL 
        })
    }
    
    @Test
    fun `should detect high temperature`() {
        val telemetry = createTelemetry(
            tempMin = 30.0,
            tempMax = 65.0,
            tempAvg = 45.0
        )
        
        val alerts = evaluator.evaluate(telemetry)
        
        assertTrue(alerts.any { 
            it is BatteryAlert.HighTemperature && it.severity == AlertSeverity.CRITICAL 
        })
    }
    
    @Test
    fun `should detect cell imbalance`() {
        val voltages = MutableList(114) { 3.6 }
        voltages[0] = 3.3  // Low
        voltages[50] = 3.8  // High
        
        val telemetry = createTelemetry(cellVoltages = voltages)
        
        val alerts = evaluator.evaluate(telemetry)
        
        assertTrue(alerts.any { it is BatteryAlert.CellImbalance })
    }
    
    @Test
    fun `should detect low SOC`() {
        val telemetry = createTelemetry(soc = 3.0)
        
        val alerts = evaluator.evaluate(telemetry)
        
        assertTrue(alerts.any { 
            it is BatteryAlert.CriticalLowSoc && it.severity == AlertSeverity.CRITICAL 
        })
    }
    
    @Test
    fun `should return no alerts for healthy battery`() {
        val telemetry = createTelemetry()
        
        val alerts = evaluator.evaluate(telemetry)
        
        assertTrue(alerts.isEmpty())
    }
    
    private fun createTelemetry(
        soc: Double = 85.0,
        voltage: Double = 370.0,
        current: Double = -45.0,
        tempMin: Double = 28.0,
        tempMax: Double = 32.0,
        tempAvg: Double = 30.0,
        cellVoltages: List<Double> = List(114) { 3.6 }
    ): BatteryTelemetry {
        return BatteryTelemetry(
            messageId = MessageId.generate(),
            batteryPackId = BatteryPackId("test-pack"),
            vehicleId = VehicleId("test-vehicle"),
            timestamp = Instant.now(),
            stateOfCharge = StateOfCharge(soc),
            voltage = Voltage(voltage),
            current = Current(current),
            power = Power(voltage * current),
            temperatures = TemperatureReading(min = tempMin, max = tempMax, avg = tempAvg),
            cellVoltages = CellVoltages(cellVoltages)
        )
    }
}
