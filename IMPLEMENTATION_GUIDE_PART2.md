# Android DDD Implementation Guide - Part 2

**Infrastructure & Interface Layers**

---

## Infrastructure Layer (Adapters)

### 1. CAN-Bus Adapter (USB)

#### `infrastructure/hardware/usb/PcanUsbAdapter.kt`

```kotlin
package com.fleet.bms.infrastructure.hardware.usb

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import com.fleet.bms.domain.model.CanFrame
import com.fleet.bms.domain.model.CanId
import com.fleet.bms.domain.repository.CanBusConfig
import com.fleet.bms.domain.repository.CanBusPort
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import java.nio.ByteBuffer
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Adapter: PCAN-USB FD Implementation
 * 
 * Implements CanBusPort for PEAK PCAN-USB FD adapter.
 * Uses USB bulk transfers to read CAN frames.
 */
@Singleton
class PcanUsbAdapter @Inject constructor(
    @ApplicationContext private val context: Context
) : CanBusPort {
    
    private var usbDevice: UsbDevice? = null
    private var connection: UsbDeviceConnection? = null
    private val usbManager: UsbManager by lazy {
        context.getSystemService(Context.USB_SERVICE) as UsbManager
    }
    
    private var readJob: Job? = null
    private var isReading = false
    
    companion object {
        // PCAN-USB VID/PID
        private const val PCAN_VENDOR_ID = 0x0c72
        private const val PCAN_PRODUCT_ID = 0x000c  // PCAN-USB FD
        
        // USB endpoints
        private const val BULK_IN_ENDPOINT = 0x81
        private const val BULK_OUT_ENDPOINT = 0x02
        
        // Frame sizes
        private const val CAN_FRAME_SIZE = 16
    }
    
    override suspend fun connect(): Result<Unit> = withContext(Dispatchers.IO) {
        Timber.i("Connecting to PCAN-USB")
        
        try {
            // Find PCAN-USB device
            usbDevice = findPcanDevice() ?: return@withContext Result.failure(
                Exception("PCAN-USB device not found")
            )
            
            // Request permission if needed
            if (!usbManager.hasPermission(usbDevice)) {
                requestPermission(usbDevice!!)
                return@withContext Result.failure(
                    Exception("USB permission required")
                )
            }
            
            // Open connection
            connection = usbManager.openDevice(usbDevice)
            
            if (connection == null) {
                return@withContext Result.failure(
                    Exception("Failed to open USB device")
                )
            }
            
            // Claim interface
            val intf = usbDevice!!.getInterface(0)
            connection!!.claimInterface(intf, true)
            
            Timber.i("Connected to PCAN-USB successfully")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to connect to PCAN-USB")
            Result.failure(e)
        }
    }
    
    override suspend fun configure(config: CanBusConfig): Result<Unit> = withContext(Dispatchers.IO) {
        Timber.i("Configuring PCAN-USB: ${config.bitrate} bps")
        
        try {
            // Send USB control transfer to configure bitrate
            val bitrateBytes = bitrateToBytes(config.bitrate)
            
            val result = connection?.controlTransfer(
                0x41,  // Request type: vendor-specific, host-to-device
                0x09,  // Request: set bitrate
                0,
                0,
                bitrateBytes,
                bitrateBytes.size,
                5000
            )
            
            if (result == null || result < 0) {
                return@withContext Result.failure(
                    Exception("Failed to configure PCAN-USB")
                )
            }
            
            Timber.i("PCAN-USB configured successfully")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to configure PCAN-USB")
            Result.failure(e)
        }
    }
    
    override fun readFrames(): Flow<CanFrame> = flow {
        Timber.i("Starting CAN frame reading")
        isReading = true
        
        val buffer = ByteArray(CAN_FRAME_SIZE)
        val endpoint = usbDevice?.getInterface(0)?.getEndpoint(0)  // Bulk IN
        
        while (isReading && currentCoroutineContext().isActive) {
            try {
                val bytesRead = connection?.bulkTransfer(
                    endpoint,
                    buffer,
                    buffer.size,
                    100  // 100ms timeout
                )
                
                if (bytesRead != null && bytesRead > 0) {
                    val frame = parseCanFrame(buffer, bytesRead)
                    if (frame != null) {
                        emit(frame)
                    }
                }
                
                // Small delay to prevent CPU spinning
                delay(1)
                
            } catch (e: Exception) {
                Timber.e(e, "Error reading CAN frame")
                if (e is CancellationException) throw e
            }
        }
    }
    
    override suspend fun sendFrame(frame: CanFrame): Result<Unit> = withContext(Dispatchers.IO) {
        // Implement if needed for diagnostics
        Result.success(Unit)
    }
    
    override suspend fun disconnect() {
        Timber.i("Disconnecting PCAN-USB")
        isReading = false
        readJob?.cancel()
        connection?.close()
        connection = null
        usbDevice = null
    }
    
    override fun isConnected(): Boolean = connection != null
    
    private fun findPcanDevice(): UsbDevice? {
        return usbManager.deviceList.values.find { device ->
            device.vendorId == PCAN_VENDOR_ID && device.productId == PCAN_PRODUCT_ID
        }
    }
    
    private fun requestPermission(device: UsbDevice) {
        val intent = PendingIntent.getBroadcast(
            context,
            0,
            Intent("com.fleet.bms.USB_PERMISSION"),
            PendingIntent.FLAG_IMMUTABLE
        )
        usbManager.requestPermission(device, intent)
    }
    
    private fun bitrateToBytes(bitrate: Int): ByteArray {
        // Convert bitrate to PCAN-USB format
        // This is hardware-specific
        return when (bitrate) {
            500_000 -> byteArrayOf(0x00, 0x1C)  // 500 kbps
            250_000 -> byteArrayOf(0x01, 0x1C)  // 250 kbps
            else -> byteArrayOf(0x00, 0x1C)
        }
    }
    
    private fun parseCanFrame(buffer: ByteArray, length: Int): CanFrame? {
        if (length < 12) return null  // Minimum frame size
        
        try {
            // Parse PCAN-USB frame format
            val id = ByteBuffer.wrap(buffer, 0, 4).int and 0x1FFFFFFF
            val dlc = buffer[4].toInt() and 0x0F
            val data = buffer.copyOfRange(8, 8 + dlc)
            
            return CanFrame(
                id = CanId(id),
                data = data,
                timestamp = Instant.now(),
                isExtended = (id > 0x7FF)
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse CAN frame")
            return null
        }
    }
}
```

### 2. CAN Protocol Parser

#### `infrastructure/hardware/protocol/BatteryProtocolDecoder.kt`

```kotlin
package com.fleet.bms.infrastructure.hardware.protocol

import com.fleet.bms.domain.model.*
import com.fleet.bms.domain.service.*
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Adapter: Battery Protocol Decoder
 * 
 * Decodes ENNOID BMS CAN protocol into domain concepts.
 * 
 * Message IDs:
 * 0x100: Pack status (SOC, Voltage, Current, Power)
 * 0x101: Reserved
 * 0x102: Temperatures (min, max, avg)
 * 0x103: Cell voltage stats
 * 0x104: Warning flags
 * 0x110-0x11E: Cell voltages (8 cells per frame, 15 frames for 114 cells)
 * 0x180-0x181: GPS coordinates
 */
@Singleton
class BatteryProtocolDecoder @Inject constructor() : CanProtocolParser {
    
    override fun parse(frame: CanFrame): ParsedCanData? {
        return try {
            when (frame.id.value) {
                0x100 -> parsePackStatus(frame.data)
                0x102 -> parseTemperatures(frame.data)
                0x103 -> parseCellStats(frame.data)
                0x104 -> parseWarnings(frame.data)
                in 0x110..0x11E -> parseCellVoltages(frame)
                0x180 -> parseGpsLatitude(frame.data)
                0x181 -> parseGpsLongitude(frame.data)
                else -> {
                    Timber.v("Unknown CAN ID: 0x${frame.id.value.toString(16)}")
                    null
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error parsing CAN frame: ${frame.id.value}")
            null
        }
    }
    
    private fun parsePackStatus(data: ByteArray): ParsedCanData.PackStatus {
        // Bytes 0-1: SOC (scale 0.01, 0-10000 = 0-100%)
        val socRaw = decodeUInt16(data, 0)
        val soc = StateOfCharge((socRaw * 0.01).coerceIn(0.0, 100.0))
        
        // Bytes 2-3: Voltage (scale 0.1V, 0-5000 = 0-500V)
        val voltageRaw = decodeUInt16(data, 2)
        val voltage = Voltage((voltageRaw * 0.1).coerceIn(0.0, 500.0))
        
        // Bytes 4-5: Current (scale 0.1A, signed, -2100 to +2100 = -210A to +210A)
        val currentRaw = decodeInt16(data, 4)
        val current = Current((currentRaw * 0.1).coerceIn(-210.0, 210.0))
        
        // Bytes 6-7: Power (scale 0.1W, signed)
        val powerRaw = decodeInt16(data, 6)
        val power = Power(powerRaw * 0.1)
        
        return ParsedCanData.PackStatus(
            PackStatus(soc, voltage, current, power)
        )
    }
    
    private fun parseTemperatures(data: ByteArray): ParsedCanData.Temperatures {
        // Bytes 0-1: Min temp (scale 0.1°C, signed, offset -40°C)
        val minRaw = decodeInt16(data, 0)
        val min = (minRaw * 0.1) - 40.0
        
        // Bytes 2-3: Max temp
        val maxRaw = decodeInt16(data, 2)
        val max = (maxRaw * 0.1) - 40.0
        
        // Bytes 4-5: Avg temp
        val avgRaw = decodeInt16(data, 4)
        val avg = (avgRaw * 0.1) - 40.0
        
        return ParsedCanData.Temperatures(
            TemperatureReading(
                min = min.coerceIn(-20.0, 80.0),
                max = max.coerceIn(-20.0, 80.0),
                average = avg.coerceIn(-20.0, 80.0)
            )
        )
    }
    
    private fun parseCellStats(data: ByteArray): ParsedCanData.CellStats {
        // Bytes 0-1: Min cell voltage (scale 0.001V)
        val minRaw = decodeUInt16(data, 0)
        val min = minRaw * 0.001
        
        // Bytes 2-3: Max cell voltage
        val maxRaw = decodeUInt16(data, 2)
        val max = maxRaw * 0.001
        
        // Bytes 4-5: Average cell voltage
        val avgRaw = decodeUInt16(data, 4)
        val avg = avgRaw * 0.001
        
        val delta = max - min
        
        return ParsedCanData.CellStats(
            CellVoltageStats(min, max, delta, avg)
        )
    }
    
    private fun parseCellVoltages(frame: CanFrame): ParsedCanData.CellVoltages {
        val data = frame.data
        
        // Frame 0x110 = cells 0-7, 0x111 = cells 8-15, etc.
        val frameIndex = frame.id.value - 0x110
        val startCell = frameIndex * 8
        
        // Each byte represents one cell voltage
        // Offset: 2.5V, Scale: 0.02V per bit
        // Range: 2.5V to 7.6V (covers LFP range 2.5-3.65V)
        val voltages = data.map { byte ->
            2.5 + (byte.toUByte().toInt() * 0.02)
        }
        
        return ParsedCanData.CellVoltages(voltages, startCell)
    }
    
    private fun parseWarnings(data: ByteArray): ParsedCanData.Warnings {
        val flags = data[0].toInt()
        
        return ParsedCanData.Warnings(
            BatteryWarnings(
                hasLowVoltage = (flags and 0x01) != 0,
                hasHighVoltage = (flags and 0x02) != 0,
                hasLowTemperature = (flags and 0x04) != 0,
                hasHighTemperature = (flags and 0x08) != 0,
                hasCellImbalance = (flags and 0x10) != 0,
                hasOverCurrent = (flags and 0x20) != 0,
                hasLowSoc = (flags and 0x40) != 0
            )
        )
    }
    
    private var gpsLatitude: Double? = null
    
    private fun parseGpsLatitude(data: ByteArray): ParsedCanData? {
        val latRaw = decodeInt32(data, 0)
        gpsLatitude = latRaw / 1_000_000.0
        return null  // Need both lat and lon before emitting
    }
    
    private fun parseGpsLongitude(data: ByteArray): ParsedCanData? {
        val lonRaw = decodeInt32(data, 0)
        val longitude = lonRaw / 1_000_000.0
        
        val latitude = gpsLatitude ?: return null
        
        return ParsedCanData.GpsCoordinates(
            GpsLocation(
                latitude = latitude,
                longitude = longitude,
                timestamp = Instant.now()
            )
        )
    }
    
    // Helper functions for byte decoding
    private fun decodeUInt16(data: ByteArray, offset: Int): Int {
        if (offset + 1 >= data.size) return 0
        return ((data[offset].toInt() and 0xFF) shl 8) or 
               (data[offset + 1].toInt() and 0xFF)
    }
    
    private fun decodeInt16(data: ByteArray, offset: Int): Int {
        val unsigned = decodeUInt16(data, offset)
        return if (unsigned >= 32768) unsigned - 65536 else unsigned
    }
    
    private fun decodeInt32(data: ByteArray, offset: Int): Int {
        if (offset + 3 >= data.size) return 0
        return ((data[offset].toInt() and 0xFF) shl 24) or
               ((data[offset + 1].toInt() and 0xFF) shl 16) or
               ((data[offset + 2].toInt() and 0xFF) shl 8) or
               (data[offset + 3].toInt() and 0xFF)
    }
}

/**
 * Interface for protocol parsing
 */
interface CanProtocolParser {
    fun parse(frame: CanFrame): ParsedCanData?
}
```

### 3. MQTT Publisher Adapter

#### `infrastructure/messaging/mqtt/MqttTelemetryPublisher.kt`

```kotlin
package com.fleet.bms.infrastructure.messaging.mqtt

import com.fleet.bms.application.dto.TelemetryMessageDto
import com.fleet.bms.application.dto.toDto
import com.fleet.bms.domain.model.BatteryTelemetry
import com.fleet.bms.domain.repository.ConnectionState
import com.fleet.bms.domain.repository.TelemetryPublisherPort
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.eclipse.paho.client.mqttv3.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Adapter: MQTT Telemetry Publisher
 * 
 * Implements TelemetryPublisherPort using Eclipse Paho MQTT client.
 * Publishes to backend MQTT broker.
 */
@Singleton
class MqttTelemetryPublisher @Inject constructor(
    private val config: MqttConfig
) : TelemetryPublisherPort {
    
    private var client: MqttClient? = null
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }
    
    override suspend fun connect(): Result<Unit> {
        Timber.i("Connecting to MQTT: ${config.brokerUrl}")
        _connectionState.value = ConnectionState.CONNECTING
        
        return try {
            client = MqttClient(
                config.brokerUrl,
                config.clientId,
                null  // Use memory persistence (not file)
            )
            
            val options = MqttConnectOptions().apply {
                userName = config.username
                password = config.password.toCharArray()
                isCleanSession = false  // Persist session for QoS
                connectionTimeout = 30
                keepAliveInterval = 60
                isAutomaticReconnect = true
            }
            
            // Set callback for connection events
            client?.setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    Timber.w(cause, "MQTT connection lost")
                    _connectionState.value = ConnectionState.DISCONNECTED
                }
                
                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    // Not subscribing to anything
                }
                
                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                    Timber.v("Message delivery complete")
                }
            })
            
            client?.connect(options)
            
            _connectionState.value = ConnectionState.CONNECTED
            Timber.i("Connected to MQTT successfully")
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to connect to MQTT")
            _connectionState.value = ConnectionState.ERROR
            Result.failure(e)
        }
    }
    
    override suspend fun publish(telemetry: BatteryTelemetry): Result<Unit> {
        if (!isConnected()) {
            return Result.failure(Exception("Not connected to MQTT"))
        }
        
        return try {
            // Convert domain model to DTO
            val dto: TelemetryMessageDto = telemetry.toDto()
            
            // Serialize to JSON
            val jsonPayload = json.encodeToString(dto)
            
            // Create MQTT topic
            val topic = "fleet/${telemetry.vehicleId.value}/bms/telemetry"
            
            // Create MQTT message
            val message = MqttMessage(jsonPayload.toByteArray()).apply {
                qos = 1  // At least once delivery
                isRetained = false
            }
            
            // Publish
            client?.publish(topic, message)
            
            Timber.d("Published telemetry: ${telemetry.messageId.value} to $topic")
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to publish telemetry")
            Result.failure(e)
        }
    }
    
    override suspend fun disconnect() {
        Timber.i("Disconnecting from MQTT")
        try {
            client?.disconnect()
            _connectionState.value = ConnectionState.DISCONNECTED
        } catch (e: Exception) {
            Timber.e(e, "Error disconnecting from MQTT")
        } finally {
            client = null
        }
    }
    
    override fun isConnected(): Boolean = client?.isConnected == true
    
    override fun connectionState(): StateFlow<ConnectionState> = _connectionState.asStateFlow()
}

/**
 * MQTT Configuration
 */
data class MqttConfig(
    val brokerUrl: String,          // e.g., "tcp://mqtt.fleet.cloud:1883"
    val clientId: String,           // Unique client ID
    val username: String,
    val password: String
)
```

### 4. Local Storage Adapter (Room)

#### `infrastructure/persistence/room/TelemetryDatabase.kt`

```kotlin
package com.fleet.bms.infrastructure.persistence.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [TelemetryEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TelemetryDatabase : RoomDatabase() {
    abstract fun telemetryDao(): TelemetryDao
}
```

#### `infrastructure/persistence/room/TelemetryEntity.kt`

```kotlin
package com.fleet.bms.infrastructure.persistence.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fleet.bms.domain.model.*
import java.time.Instant

@Entity(tableName = "telemetry")
data class TelemetryEntity(
    @PrimaryKey
    val messageId: String,
    val batteryPackId: String,
    val vehicleId: String,
    val timestamp: Long,  // Unix timestamp
    val stateOfCharge: Double,
    val voltage: Double,
    val current: Double,
    val power: Double,
    val temperatureMin: Double,
    val temperatureMax: Double,
    val temperatureAvg: Double,
    val cellVoltages: String,  // JSON array
    val isSynced: Boolean = false
)

/**
 * Converters for domain model ↔ entity
 */
fun BatteryTelemetry.toEntity(): TelemetryEntity {
    return TelemetryEntity(
        messageId = messageId.value,
        batteryPackId = batteryPackId.value,
        vehicleId = vehicleId.value,
        timestamp = timestamp.toEpochMilli(),
        stateOfCharge = stateOfCharge.value,
        voltage = voltage.value,
        current = current.value,
        power = power.value,
        temperatureMin = temperatures.min,
        temperatureMax = temperatures.max,
        temperatureAvg = temperatures.average,
        cellVoltages = cellVoltages.voltages.joinToString(","),
        isSynced = false
    )
}

fun TelemetryEntity.toDomain(): BatteryTelemetry {
    val cellVoltagesList = cellVoltages.split(",").map { it.toDouble() }
    
    return BatteryTelemetry(
        messageId = MessageId(messageId),
        batteryPackId = BatteryPackId(batteryPackId),
        vehicleId = VehicleId(vehicleId),
        timestamp = Instant.ofEpochMilli(timestamp),
        stateOfCharge = StateOfCharge(stateOfCharge),
        voltage = Voltage(voltage),
        current = Current(current),
        power = Power(power),
        temperatures = TemperatureReading(temperatureMin, temperatureMax, temperatureAvg),
        cellVoltages = CellVoltages(cellVoltagesList)
    )
}
```

#### `infrastructure/persistence/room/TelemetryDao.kt`

```kotlin
package com.fleet.bms.infrastructure.persistence.room

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TelemetryDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(telemetry: TelemetryEntity)
    
    @Query("SELECT * FROM telemetry WHERE isSynced = 0 ORDER BY timestamp ASC")
    suspend fun getAllUnsynced(): List<TelemetryEntity>
    
    @Query("SELECT * FROM telemetry WHERE isSynced = 0 ORDER BY timestamp ASC")
    fun observeUnsynced(): Flow<List<TelemetryEntity>>
    
    @Query("SELECT * FROM telemetry ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(): TelemetryEntity?
    
    @Query("DELETE FROM telemetry WHERE messageId = :messageId")
    suspend fun delete(messageId: String)
    
    @Query("DELETE FROM telemetry")
    suspend fun deleteAll()
    
    @Query("SELECT COUNT(*) FROM telemetry WHERE isSynced = 0")
    suspend fun getUnsyncedCount(): Int
    
    @Query("UPDATE telemetry SET isSynced = 1 WHERE messageId = :messageId")
    suspend fun markAsSynced(messageId: String)
}
```

#### `infrastructure/persistence/room/LocalTelemetryRepository.kt`

```kotlin
package com.fleet.bms.infrastructure.persistence.room

import com.fleet.bms.domain.model.BatteryTelemetry
import com.fleet.bms.domain.repository.TelemetryStoragePort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Adapter: Local Telemetry Storage
 * 
 * Implements TelemetryStoragePort using Room database.
 */
@Singleton
class LocalTelemetryRepository @Inject constructor(
    private val dao: TelemetryDao
) : TelemetryStoragePort {
    
    override suspend fun store(telemetry: BatteryTelemetry): Result<Unit> {
        return try {
            val entity = telemetry.toEntity()
            dao.insert(entity)
            Timber.d("Stored telemetry locally: ${telemetry.messageId.value}")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to store telemetry")
            Result.failure(e)
        }
    }
    
    override suspend fun getBuffered(): List<BatteryTelemetry> {
        return try {
            dao.getAllUnsynced().map { it.toDomain() }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get buffered telemetry")
            emptyList()
        }
    }
    
    override fun observeBuffered(): Flow<List<BatteryTelemetry>> {
        return dao.observeUnsynced().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override suspend fun removeBuffered(telemetry: BatteryTelemetry): Result<Unit> {
        return try {
            dao.delete(telemetry.messageId.value)
            Timber.d("Removed buffered telemetry: ${telemetry.messageId.value}")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to remove telemetry")
            Result.failure(e)
        }
    }
    
    override suspend fun clearAll() {
        dao.deleteAll()
    }
    
    override suspend fun getBufferSize(): Int {
        return dao.getUnsyncedCount()
    }
    
    override suspend fun getLatest(): BatteryTelemetry? {
        return dao.getLatest()?.toDomain()
    }
}
```

---

## Interface Layer (UI)

### ViewModels

#### `interfaces/ui/dashboard/DashboardViewModel.kt`

```kotlin
package com.fleet.bms.interfaces.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fleet.bms.application.usecase.*
import com.fleet.bms.domain.repository.CanBusConfig
import com.fleet.bms.domain.service.AlertSeverity
import com.fleet.bms.domain.service.BatteryAlert
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val startMonitoringUseCase: StartMonitoringUseCase,
    private val stopMonitoringUseCase: StopMonitoringUseCase,
    private val collectTelemetryUseCase: CollectTelemetryUseCase,
    private val publishTelemetryUseCase: PublishTelemetryUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    fun startMonitoring() {
        viewModelScope.launch {
            Timber.i("Starting monitoring from UI")
            
            // Start CAN-Bus and MQTT connections
            val config = CanBusConfig(bitrate = 500_000)
            startMonitoringUseCase.execute(config)
                .onSuccess {
                    // Start collecting telemetry
                    collectTelemetry()
                }
                .onFailure { error ->
                    Timber.e(error, "Failed to start monitoring")
                    _uiState.value = DashboardUiState.Error(
                        error.message ?: "Failed to start monitoring"
                    )
                }
        }
    }
    
    private suspend fun collectTelemetry() {
        collectTelemetryUseCase.execute().collect { result ->
            when (result) {
                is TelemetryResult.Success -> {
                    val telemetry = result.telemetry
                    val alerts = result.alerts
                    
                    // Update UI
                    _uiState.value = DashboardUiState.Success(
                        telemetry = telemetry.toUiModel(),
                        alerts = alerts,
                        severity = determineSeverity(alerts)
                    )
                    
                    // Publish to cloud (fire and forget)
                    viewModelScope.launch {
                        publishTelemetryUseCase.execute(telemetry)
                    }
                }
                
                is TelemetryResult.Partial -> {
                    // Show loading with percentage
                    _uiState.value = DashboardUiState.Loading
                }
                
                is TelemetryResult.Error -> {
                    _uiState.value = DashboardUiState.Error(result.message)
                }
            }
        }
    }
    
    fun stopMonitoring() {
        viewModelScope.launch {
            stopMonitoringUseCase.execute()
            _uiState.value = DashboardUiState.Stopped
        }
    }
    
    private fun determineSeverity(alerts: List<BatteryAlert>): AlertSeverity {
        if (alerts.isEmpty()) return AlertSeverity.NONE
        return alerts.maxOf { it.severity }
    }
    
    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            stopMonitoringUseCase.execute()
        }
    }
}

/**
 * UI State
 */
sealed class DashboardUiState {
    object Loading : DashboardUiState()
    object Stopped : DashboardUiState()
    
    data class Success(
        val telemetry: BatteryTelemetryUiModel,
        val alerts: List<BatteryAlert>,
        val severity: AlertSeverity
    ) : DashboardUiState()
    
    data class Error(val message: String) : DashboardUiState()
}

/**
 * UI Model (presentation layer concern)
 */
data class BatteryTelemetryUiModel(
    val stateOfCharge: String,          // "75.5%"
    val voltage: String,                // "377.2 V"
    val current: String,                // "-45.3 A"
    val power: String,                  // "-17.1 kW"
    val temperatureMin: String,         // "28.5°C"
    val temperatureMax: String,         // "32.1°C"
    val cellVoltages: List<CellVoltageUiModel>,
    val warnings: List<String>
)

data class CellVoltageUiModel(
    val index: Int,
    val voltage: String,
    val isLow: Boolean,
    val isHigh: Boolean
)

/**
 * Extension: Convert domain to UI model
 */
fun com.fleet.bms.domain.model.BatteryTelemetry.toUiModel(): BatteryTelemetryUiModel {
    return BatteryTelemetryUiModel(
        stateOfCharge = "${stateOfCharge.value.format(1)}%",
        voltage = "${voltage.value.format(1)} V",
        current = "${current.value.format(1)} A",
        power = "${power.value / 1000.0.format(1)} kW",
        temperatureMin = "${temperatures.min.format(1)}°C",
        temperatureMax = "${temperatures.max.format(1)}°C",
        cellVoltages = cellVoltages.voltages.mapIndexed { index, v ->
            CellVoltageUiModel(
                index = index + 1,
                voltage = "${v.format(3)} V",
                isLow = v < 3.0,
                isHigh = v > 3.6
            )
        },
        warnings = warnings?.let { w ->
            buildList {
                if (w.hasLowVoltage) add("Low Voltage")
                if (w.hasHighVoltage) add("High Voltage")
                if (w.hasLowTemperature) add("Low Temperature")
                if (w.hasHighTemperature) add("High Temperature")
                if (w.hasCellImbalance) add("Cell Imbalance")
                if (w.hasOverCurrent) add("Over Current")
                if (w.hasLowSoc) add("Low SOC")
            }
        } ?: emptyList()
    )
}

fun Double.format(decimals: Int): String = "%.${decimals}f".format(this)
```

---

This is Part 2 of the implementation guide containing:
- ✅ Infrastructure layer (CAN adapter, MQTT, Room DB)
- ✅ Interface layer (ViewModel)

Part 3 will contain:
- Compose UI screens
- Dependency Injection (Hilt modules)
- Testing
- Complete file manifest

Would you like me to create Part 3 with the UI components and DI setup?