package com.fleet.bms.infrastructure.android.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class BmsUserSettings(
    val batteryPackId: String,
    val vehicleId: String,
    val mqttBroker: String,
    val mqttUsername: String,
    val mqttPassword: String,
) {
    companion object {
        fun defaults() = BmsUserSettings(
            batteryPackId = "550e8400-e29b-41d4-a716-446655440000",
            vehicleId = "vehicle_001",
            mqttBroker = "tcp://mqtt.fleet.cloud:1883",
            mqttUsername = "backend",
            mqttPassword = "backend123",
        )
    }
}

private object PrefsKeys {
    val BATTERY_PACK_ID = stringPreferencesKey("battery_pack_id")
    val VEHICLE_ID = stringPreferencesKey("vehicle_id")
    val MQTT_BROKER = stringPreferencesKey("mqtt_broker")
    val MQTT_USERNAME = stringPreferencesKey("mqtt_username")
    val MQTT_PASSWORD = stringPreferencesKey("mqtt_password")
}

/**
 * User-editable BMS/MQTT settings (DataStore) with one-time migration from legacy [bms_config] SharedPreferences.
 */
@Singleton
class BmsSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>,
) {

    private val legacyPrefs = context.getSharedPreferences("bms_config", Context.MODE_PRIVATE)

    suspend fun load(): BmsUserSettings {
        val p = dataStore.data.first()
        val stored = p[PrefsKeys.BATTERY_PACK_ID]
        if (stored == null) {
            val migrated = readLegacyOrDefaults()
            save(migrated)
            return migrated
        }
        return BmsUserSettings(
            batteryPackId = stored,
            vehicleId = p[PrefsKeys.VEHICLE_ID] ?: BmsUserSettings.defaults().vehicleId,
            mqttBroker = p[PrefsKeys.MQTT_BROKER] ?: BmsUserSettings.defaults().mqttBroker,
            mqttUsername = p[PrefsKeys.MQTT_USERNAME] ?: BmsUserSettings.defaults().mqttUsername,
            mqttPassword = p[PrefsKeys.MQTT_PASSWORD] ?: BmsUserSettings.defaults().mqttPassword,
        )
    }

    suspend fun save(settings: BmsUserSettings) {
        dataStore.edit { pref ->
            pref[PrefsKeys.BATTERY_PACK_ID] = settings.batteryPackId
            pref[PrefsKeys.VEHICLE_ID] = settings.vehicleId
            pref[PrefsKeys.MQTT_BROKER] = settings.mqttBroker
            pref[PrefsKeys.MQTT_USERNAME] = settings.mqttUsername
            pref[PrefsKeys.MQTT_PASSWORD] = settings.mqttPassword
        }
        legacyPrefs.edit().apply {
            putString("battery_pack_id", settings.batteryPackId)
            putString("vehicle_id", settings.vehicleId)
            putString("mqtt_broker", settings.mqttBroker)
            putString("mqtt_username", settings.mqttUsername)
            putString("mqtt_password", settings.mqttPassword)
            apply()
        }
    }

    private fun readLegacyOrDefaults(): BmsUserSettings {
        val d = BmsUserSettings.defaults()
        return BmsUserSettings(
            batteryPackId = legacyPrefs.getString("battery_pack_id", d.batteryPackId)!!,
            vehicleId = legacyPrefs.getString("vehicle_id", d.vehicleId)!!,
            mqttBroker = legacyPrefs.getString("mqtt_broker", d.mqttBroker)!!,
            mqttUsername = legacyPrefs.getString("mqtt_username", d.mqttUsername)!!,
            mqttPassword = legacyPrefs.getString("mqtt_password", d.mqttPassword)!!,
        )
    }
}
