package com.fleet.bms.infrastructure.hardware.protocol

import com.fleet.bms.domain.model.CanFrame
import com.fleet.bms.domain.model.CanId
import com.fleet.bms.domain.service.ParsedCanData
import com.fleet.bms.infrastructure.config.ProtocolConfig
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.Instant

class BatteryProtocolDecoderTest {

    private lateinit var decoder: EnnoidBmsProtocolDecoder

    @Before
    fun setup() {
        decoder = EnnoidBmsProtocolDecoder(ProtocolConfig())
    }
    
    @Test
    fun `should decode pack status message 0x100`() {
        // Pack status: SOC=85%, Voltage=370V, Current=-45A, Power=-16650W
        val data = byteArrayOf(
            0x21, 0x34.toByte(),  // SOC: 8500 * 0.01 = 85%
            0x0E, 0x74.toByte(),  // Voltage: 3700 * 0.1 = 370V
            0xFE.toByte(), 0x1C.toByte(),  // Current: -450 * 0.1 = -45A
            0xBF.toByte(), 0x16.toByte()   // Power: -16650 * 0.1
        )
        
        val frame = CanFrame(
            id = CanId(0x100),
            data = data,
            timestamp = Instant.now()
        )
        
        val result = decoder.parse(frame)
        
        assertNotNull(result)
        assertTrue(result is ParsedCanData.PackStatus)
        
        val packStatus = (result as ParsedCanData.PackStatus).status
        assertEquals(85.0, packStatus.soc.value, 0.1)
        assertEquals(370.0, packStatus.voltage.value, 0.1)
        assertEquals(-45.0, packStatus.current.value, 0.1)
    }
    
    @Test
    fun `should decode temperature message 0x102`() {
        // Temps: Min=28°C, Max=32°C, Avg=30°C
        val data = byteArrayOf(
            0x01, 0x18,  // Min: 280 * 0.1 = 28°C
            0x01, 0x40,  // Max: 320 * 0.1 = 32°C
            0x01, 0x2C,  // Avg: 300 * 0.1 = 30°C
            0x00, 0x00
        )
        
        val frame = CanFrame(
            id = CanId(0x102),
            data = data,
            timestamp = Instant.now()
        )
        
        val result = decoder.parse(frame)
        
        assertNotNull(result)
        assertTrue(result is ParsedCanData.Temperatures)
        
        val temps = (result as ParsedCanData.Temperatures).reading
        assertEquals(28.0, temps.min, 0.1)
        assertEquals(32.0, temps.max, 0.1)
        assertEquals(30.0, temps.avg, 0.1)
    }
    
    @Test
    fun `should decode cell voltages message 0x110`() {
        // First 8 cells, each at 3.6V
        // Cell voltage = 2.5 + (byte * 0.02)
        // For 3.6V: byte = (3.6 - 2.5) / 0.02 = 55 = 0x37
        val data = byteArrayOf(0x37, 0x37, 0x37, 0x37, 0x37, 0x37, 0x37, 0x37)
        
        val frame = CanFrame(
            id = CanId(0x110),
            data = data,
            timestamp = Instant.now()
        )
        
        val result = decoder.parse(frame)
        
        assertNotNull(result)
        assertTrue(result is ParsedCanData.CellVoltages)
        
        val cellVoltages = result as ParsedCanData.CellVoltages
        assertEquals(0, cellVoltages.startIndex)
        assertEquals(8, cellVoltages.voltages.size)
        cellVoltages.voltages.forEach { voltage ->
            assertEquals(3.6, voltage, 0.05)
        }
    }
    
    @Test
    fun `should return null for unknown message ID`() {
        val data = byteArrayOf(0x00, 0x00, 0x00, 0x00)
        val frame = CanFrame(
            id = CanId(0x999),  // Unknown ID
            data = data,
            timestamp = Instant.now()
        )
        
        val result = decoder.parse(frame)
        
        assertNull(result)
    }
}
