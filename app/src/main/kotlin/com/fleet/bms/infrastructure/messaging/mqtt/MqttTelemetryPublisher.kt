package com.fleet.bms.infrastructure.messaging.mqtt

import com.fleet.bms.application.dto.TelemetryMessageDto
import com.fleet.bms.domain.model.BatteryTelemetry
import com.fleet.bms.domain.repository.ConnectionState
import com.fleet.bms.domain.repository.TelemetryPublisherPort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import timber.log.Timber

/**
 * Adapter: MQTT Telemetry Publisher
 * 
 * Implements TelemetryPublisherPort using Eclipse Paho MQTT.
 * Publishes to: fleet/{vehicleId}/bms/telemetry
 */
class MqttTelemetryPublisher(
    private val config: MqttConfig
) : TelemetryPublisherPort {
    
    private var client: MqttClient? = null
    private var connectionState: ConnectionState = ConnectionState.DISCONNECTED
    
    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
    }
    
    override suspend fun connect(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (client?.isConnected == true) {
                Timber.d("Already connected to MQTT")
                return@withContext Result.success(Unit)
            }
            
            connectionState = ConnectionState.CONNECTING
            Timber.i("Connecting to MQTT broker: ${config.brokerUrl}")
            
            client = MqttClient(
                config.brokerUrl,
                config.clientId,
                MemoryPersistence()
            )
            
            val options = MqttConnectOptions().apply {
                userName = config.username
                password = config.password.toCharArray()
                isCleanSession = config.cleanSession
                connectionTimeout = config.connectionTimeout
                keepAliveInterval = config.keepAliveInterval
                isAutomaticReconnect = true
            }
            
            client?.setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    Timber.w(cause, "MQTT connection lost")
                    connectionState = ConnectionState.RECONNECTING
                }
                
                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    // Not subscribing, only publishing
                }
                
                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                    Timber.v("MQTT message delivered")
                }
            })
            
            client?.connect(options)
            
            connectionState = ConnectionState.CONNECTED
            Timber.i("Connected to MQTT broker successfully")
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            connectionState = ConnectionState.ERROR
            Timber.e(e, "Failed to connect to MQTT broker")
            Result.failure(e)
        }
    }
    
    override suspend fun publish(telemetry: BatteryTelemetry): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val mqttClient = client
            if (mqttClient == null || !mqttClient.isConnected) {
                return@withContext Result.failure(Exception("Not connected to MQTT broker"))
            }
            
            // Convert to DTO
            val dto = TelemetryMessageDto.fromDomain(telemetry)
            
            // Serialize to JSON
            val jsonPayload = json.encodeToString(dto)
            
            // Build topic: fleet/{vehicleId}/bms/telemetry
            val topic = "fleet/${telemetry.vehicleId.value}/bms/telemetry"
            
            // Create MQTT message
            val message = MqttMessage(jsonPayload.toByteArray()).apply {
                qos = config.qos
                isRetained = config.retained
            }
            
            // Publish
            mqttClient.publish(topic, message)
            
            Timber.d("Published telemetry to $topic (${jsonPayload.length} bytes)")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to publish telemetry")
            Result.failure(e)
        }
    }
    
    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            client?.disconnect()
            client?.close()
            client = null
            connectionState = ConnectionState.DISCONNECTED
            Timber.i("Disconnected from MQTT broker")
        } catch (e: Exception) {
            Timber.w(e, "Error disconnecting from MQTT")
        }
    }
    
    override fun isConnected(): Boolean = client?.isConnected == true
    
    override fun getConnectionState(): ConnectionState = connectionState
}
