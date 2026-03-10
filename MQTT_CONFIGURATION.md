# MQTT Configuration Guide

## Current Configuration

The app uses Eclipse Paho MQTT client with these default settings:

### Location in Code

**File:** `app/src/main/kotlin/com/fleet/bms/infrastructure/android/di/InfrastructureModule.kt`

**Lines 40-55:**
```kotlin
@Provides
@Singleton
fun provideMqttConfig(
    @ApplicationContext context: Context
): MqttConfig {
    val prefs = context.getSharedPreferences("bms_config", Context.MODE_PRIVATE)
    
    return MqttConfig(
        brokerUrl = prefs.getString("mqtt_broker", "tcp://localhost:1883")
            ?: "tcp://localhost:1883",
        clientId = "android_${UUID.randomUUID()}",
        username = prefs.getString("mqtt_username", "backend") ?: "backend",
        password = prefs.getString("mqtt_password", "backend123") ?: "backend123"
    )
}
```

---

## Configuration Options

### Option 1: Hardcode for Development (Quick)

Edit `InfrastructureModule.kt` directly:

```kotlin
return MqttConfig(
    brokerUrl = "tcp://192.168.1.100:1883",  // Your broker IP
    clientId = "android_${UUID.randomUUID()}",
    username = "backend",
    password = "backend123",
    cleanSession = false,
    connectionTimeout = 30,
    keepAliveInterval = 60,
    qos = 1,  // At least once delivery
    retained = false
)
```

### Option 2: Use SharedPreferences (Flexible)

Set configuration programmatically on first launch or via ADB:

```bash
# Set MQTT broker
adb shell "run-as com.fleet.bms sh -c 'echo \"tcp://mqtt.carrapide.sn:1883\" > /data/data/com.fleet.bms/shared_prefs/bms_config.xml'"

# Or via command line
adb shell
run-as com.fleet.bms
echo '<?xml version="1.0" encoding="utf-8"?>
<map>
    <string name="mqtt_broker">tcp://mqtt.carrapide.sn:1883</string>
    <string name="mqtt_username">fleet_client</string>
    <string name="mqtt_password">secure_password</string>
    <string name="battery_pack_id">550e8400-e29b-41d4-a716-446655440000</string>
    <string name="vehicle_id">vehicle_001</string>
</map>' > /data/data/com.fleet.bms/shared_prefs/bms_config.xml
```

### Option 3: Environment-Based Configuration

Create different configurations for dev/staging/production:

**File:** `app/build.gradle.kts`

```kotlin
android {
    buildTypes {
        debug {
            buildConfigField("String", "MQTT_BROKER", "\"tcp://10.0.2.2:1883\"")
            buildConfigField("String", "MQTT_USERNAME", "\"backend\"")
            buildConfigField("String", "MQTT_PASSWORD", "\"backend123\"")
        }
        release {
            buildConfigField("String", "MQTT_BROKER", "\"tcp://mqtt.carrapide.sn:1883\"")
            buildConfigField("String", "MQTT_USERNAME", "\"fleet_client\"")
            buildConfigField("String", "MQTT_PASSWORD", "\"production_password\"")
        }
    }
}
```

Then in `InfrastructureModule.kt`:
```kotlin
return MqttConfig(
    brokerUrl = BuildConfig.MQTT_BROKER,
    clientId = "android_${UUID.randomUUID()}",
    username = BuildConfig.MQTT_USERNAME,
    password = BuildConfig.MQTT_PASSWORD
)
```

---

## Environment-Specific Settings

### 1. Local Development (Backend on Laptop)

**Broker:** Your laptop running Docker Compose with EMQX

```kotlin
MqttConfig(
    brokerUrl = "tcp://10.0.2.2:1883",  // Android emulator → host
    // OR
    brokerUrl = "tcp://192.168.1.100:1883",  // Real device → laptop IP
    clientId = "android_dev_${UUID.randomUUID()}",
    username = "backend",
    password = "backend123"
)
```

**Find your laptop IP:**
```bash
# macOS
ifconfig | grep "inet " | grep -v 127.0.0.1

# Linux
ip addr show | grep "inet " | grep -v 127.0.0.1

# Use that IP in the Android app
```

### 2. Staging/Testing (Cloud MQTT)

```kotlin
MqttConfig(
    brokerUrl = "tcp://mqtt-staging.carrapide.sn:1883",
    clientId = "android_staging_${UUID.randomUUID()}",
    username = "fleet_test",
    password = "staging_password",
    cleanSession = false,  // Persist session
    connectionTimeout = 30,
    keepAliveInterval = 60
)
```

### 3. Production (Cloud MQTT with TLS)

```kotlin
MqttConfig(
    brokerUrl = "ssl://mqtt.carrapide.sn:8883",  // Secure connection
    clientId = "android_prod_${UUID.randomUUID()}",
    username = "fleet_client",
    password = System.getenv("MQTT_PASSWORD") ?: "fallback_password",
    cleanSession = false,
    connectionTimeout = 30,
    keepAliveInterval = 60,
    qos = 1  // At least once
)
```

**Note:** For SSL/TLS, you'll need to add certificate handling in `MqttTelemetryPublisher.kt`

---

## MQTT Broker Setup

### Option A: Use Existing Backend EMQX (Recommended)

Your backend already has EMQX configured!

**Location:** `/Users/janet/ovd/project/bms-integration/app/fleet-ddd-system/docker-compose.yml`

```yaml
emqx:
  image: emqx/emqx:5.1.0
  ports:
    - "1883:1883"    # MQTT
    - "8083:8083"    # WebSocket
    - "18083:18083"  # Dashboard
```

**Access:**
- MQTT: `tcp://localhost:1883`
- Dashboard: `http://localhost:18083` (admin/public)

**Configuration:**
```bash
cd /Users/janet/ovd/project/bms-integration/app/fleet-ddd-system
docker-compose up -d emqx

# Wait for startup
sleep 10

# Test connection
mosquitto_pub -h localhost -p 1883 -u backend -P backend123 \
  -t "fleet/vehicle_001/bms/telemetry" -m '{"test": "message"}'
```

### Option B: Cloud MQTT Broker

Popular options:
- **HiveMQ Cloud** (free tier available)
- **CloudMQTT** (free tier available)
- **AWS IoT Core** (production-grade)
- **Azure IoT Hub**

---

## Testing MQTT Connection

### 1. Test from Computer

```bash
# Subscribe to telemetry topic
mosquitto_sub -h localhost -p 1883 -u backend -P backend123 \
  -t "fleet/+/bms/telemetry" -v

# Publish test message
mosquitto_pub -h localhost -p 1883 -u backend -P backend123 \
  -t "fleet/vehicle_001/bms/telemetry" \
  -m '{"messageId":"test-123","batteryPackId":"test-pack","vehicleId":"vehicle_001","timestamp":"2026-01-30T12:00:00Z","stateOfCharge":85.0,"voltage":370.0,"current":-45.0,"temperatureMin":28.0,"temperatureMax":32.0,"temperatureAvg":30.0,"cellVoltageMin":3.28,"cellVoltageMax":3.31,"cellVoltageDelta":0.03,"cellVoltages":[3.3,3.3,3.3]}'
```

### 2. Monitor from Android

```bash
# Watch app logs for MQTT activity
adb logcat | grep -i mqtt

# Expected output:
# I/MqttTelemetryPublisher: Connecting to MQTT broker: tcp://...
# I/MqttTelemetryPublisher: Connected to MQTT broker successfully
# D/MqttTelemetryPublisher: Published telemetry to fleet/vehicle_001/bms/telemetry
```

### 3. Verify Backend Receives Data

```bash
# Query backend API
curl http://localhost:8080/api/v1/batteries/550e8400-e29b-41d4-a716-446655440000

# Should see telemetry data stored
```

---

## Topic Structure

The app publishes to:

```
fleet/{vehicleId}/bms/telemetry
```

**Examples:**
- `fleet/vehicle_001/bms/telemetry`
- `fleet/vehicle_042/bms/telemetry`
- `fleet/CR-DKR-001/bms/telemetry`

**Backend subscribes to:**
```
fleet/+/bms/telemetry
```

The `+` is a wildcard that matches any vehicle ID.

---

## Connection States

The app tracks connection state:

```kotlin
enum class ConnectionState {
    DISCONNECTED,   // Not connected
    CONNECTING,     // Attempting connection
    CONNECTED,      // Successfully connected
    RECONNECTING,   // Lost connection, retrying
    ERROR          // Connection error
}
```

Monitor in UI or logs to see current state.

---

## Offline Buffering

If MQTT connection fails, data is automatically buffered in Room database:

```
app/src/main/kotlin/com/fleet/bms/infrastructure/persistence/room/
├── TelemetryDatabase.kt       ← Local storage
├── TelemetryEntity.kt         ← Buffered records
├── TelemetryDao.kt            ← Database queries
└── LocalTelemetryRepository.kt ← Buffer management
```

**View buffered data:**
```bash
adb shell
run-as com.fleet.bms
sqlite3 databases/telemetry-db

SELECT COUNT(*) FROM telemetry_buffer WHERE isSynced = 0;
```

**Automatic sync** happens when connection is restored.

---

## Quick Configuration Commands

### Set MQTT Broker (Development)

```bash
# Edit InfrastructureModule.kt, line 45, change:
brokerUrl = "tcp://192.168.1.100:1883"  # Your laptop IP
```

### Set MQTT Broker (Production)

```bash
# Edit InfrastructureModule.kt, line 45, change:
brokerUrl = "tcp://mqtt.carrapide.sn:1883"  # Your cloud broker
```

### Rebuild and Deploy

```bash
cd /Users/janet/ovd/project/bms-integration/bms-android-app
./gradlew clean installDebug
```

---

## Troubleshooting

### "Connection refused"
- Check broker is running: `docker ps | grep emqx`
- Check port: `nc -zv localhost 1883`
- Check firewall rules

### "Authentication failed"
- Verify username/password in EMQX dashboard
- Check credentials in app match broker

### "Connection timeout"
- Check network connectivity
- Increase timeout in `MqttConfig`
- Check broker logs: `docker logs emqx`

### "SSL/TLS error"
- Use `tcp://` for non-secure (development)
- Use `ssl://` for secure (production)
- Add certificates for production SSL

---

## Next Steps

1. ✅ **Choose configuration method** (hardcode vs SharedPreferences)
2. ✅ **Set broker URL** to match your environment
3. ✅ **Set credentials** (username/password)
4. ✅ **Rebuild app**: `./gradlew clean installDebug`
5. ✅ **Test connection**: Watch logs for "Connected to MQTT broker"
6. ✅ **Verify data flow**: Check backend receives telemetry

---

**Your MQTT configuration is ready!** 🎉

**Default for local testing:**
```kotlin
brokerUrl = "tcp://10.0.2.2:1883"  // Emulator → localhost
username = "backend"
password = "backend123"
```

**Change as needed for your environment!**
