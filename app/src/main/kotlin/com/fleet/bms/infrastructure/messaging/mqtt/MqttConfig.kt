package com.fleet.bms.infrastructure.messaging.mqtt

/**
 * MQTT Configuration
 * 
 * Configuration for Eclipse Paho MQTT client.
 */
data class MqttConfig(
    val brokerUrl: String,
    val clientId: String,
    val username: String,
    val password: String,
    val cleanSession: Boolean = false,
    val connectionTimeout: Int = 30,
    val keepAliveInterval: Int = 60,
    val qos: Int = 1,  // At least once
    val retained: Boolean = false
)
