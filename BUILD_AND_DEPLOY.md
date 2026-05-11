# Build and Deploy Guide

## Prerequisites

1. **Android SDK** installed (API 28-34)
2. **JDK 17** installed
3. **Android tablet** with USB debugging enabled
4. **PCAN-USB FD** device (optional for testing)

---

## Step 1: Configure MQTT Settings

### Option A: Edit SharedPreferences (Recommended for Development)

The app will use default values on first run. To customize, add these to your tablet's shared preferences after first launch, or modify the `DomainModule.kt` defaults:

**Default Values (in DomainModule.kt):**
```kotlin
// Battery Pack ID
val batteryId = "550e8400-e29b-41d4-a716-446655440000"

// Vehicle ID
val vehicleId = "vehicle_001"
```

**Default Values (in InfrastructureModule.kt):**
```kotlin
// MQTT Broker
val brokerUrl = "tcp://mqtt.fleet.cloud:1883"  // Change to your broker
val username = "backend"
val password = "backend123"
```

### Option B: Create Configuration File

Create a configuration screen in the app (future enhancement) or modify the DI modules directly.

**For Local Testing (with backend):**
```kotlin
brokerUrl = "tcp://10.0.2.2:1883"  // Android emulator → host
// OR
brokerUrl = "tcp://192.168.1.100:1883"  // Real device → local network
```

**For Production (Cloud MQTT):**
```kotlin
brokerUrl = "tcp://your-mqtt-broker.com:1883"
// OR secure connection
brokerUrl = "ssl://your-mqtt-broker.com:8883"
```

---

## Step 2: Build the App

### Using Gradle Wrapper (Recommended)

```bash
cd /Users/janet/ovd/project/bms-integration/bms-android-app

# Make wrapper executable (first time only)
chmod +x gradlew

# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Output: app/build/outputs/apk/debug/app-debug.apk
```

### Build Variants

```bash
# Debug build (with logging)
./gradlew assembleDebug

# Release build (optimized, requires signing)
./gradlew assembleRelease
```

---

## Step 3: Run Tests

```bash
# Run all unit tests
./gradlew test

# Run specific test
./gradlew test --tests CellVoltagesTest

# Run with coverage
./gradlew testDebugUnitTest jacocoTestReport

# View results
open app/build/reports/tests/testDebugUnitTest/index.html
```

---

## Step 4: Deploy to Device

### Option A: Via Gradle

```bash
# Connect device via USB and enable USB debugging

# Install debug APK
./gradlew installDebug

# Install and launch
./gradlew installDebug
adb shell am start -n com.fleet.bms/.interfaces.ui.MainActivity
```

### Option B: Via ADB Manually

```bash
# Build APK first
./gradlew assembleDebug

# Install
adb install app/build/outputs/apk/debug/app-debug.apk

# Or reinstall (replace existing)
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Launch app
adb shell am start -n com.fleet.bms/.interfaces.ui.MainActivity
```

### Option C: Via Android Studio

1. Open project in Android Studio
2. Click "Run" (green play button)
3. Select connected device
4. App will build and deploy automatically

---

## Step 5: Configure for Your Environment

### Local Backend Testing

**Edit `InfrastructureModule.kt` line 40:**
```kotlin
@Provides
@Singleton
fun provideMqttConfig(
    @ApplicationContext context: Context
): MqttConfig {
    return MqttConfig(
        brokerUrl = "tcp://10.0.2.2:1883",  // Emulator
        // OR
        brokerUrl = "tcp://192.168.1.100:1883",  // Your laptop IP
        clientId = "android_${UUID.randomUUID()}",
        username = "backend",
        password = "backend123"
    )
}
```

### Production Cloud

**Edit `InfrastructureModule.kt` line 40:**
```kotlin
@Provides
@Singleton
fun provideMqttConfig(
    @ApplicationContext context: Context
): MqttConfig {
    return MqttConfig(
        brokerUrl = "tcp://mqtt.carrapide.sn:1883",  // Your cloud broker
        clientId = "android_${UUID.randomUUID()}",
        username = "fleet_client",
        password = "your_secure_password"
    )
}
```

---

## Step 6: Verify Installation

### Check Logs

```bash
# View all logs
adb logcat

# Filter for app logs only
adb logcat | grep "BMS"

# Filter for errors
adb logcat *:E

# Clear and watch
adb logcat -c && adb logcat | grep -i "fleet\|bms\|mqtt\|can"
```

### Expected Log Output

```
I/BmsApplication: BMS Application started
I/DashboardViewModel: Starting monitoring from ViewModel
I/StartMonitoringUseCase: Starting battery monitoring
I/PcanUsbAdapter: Found PCAN-USB device: /dev/bus/usb/...
I/PcanUsbAdapter: PCAN-USB connected successfully
I/MqttTelemetryPublisher: Connecting to MQTT broker: tcp://...
I/MqttTelemetryPublisher: Connected to MQTT broker successfully
D/CollectTelemetryUseCase: Telemetry complete: SOC=85.0%, Voltage=370.0V, Alerts=0
D/MqttTelemetryPublisher: Published telemetry to fleet/vehicle_001/bms/telemetry
```

---

## Troubleshooting

### Build Errors

**Error: "SDK location not found"**
```bash
# Create local.properties
echo "sdk.dir=/Users/janet/Library/Android/sdk" > local.properties
```

**Error: "Gradle version incompatible"**
```bash
# Use wrapper with specific version
./gradlew wrapper --gradle-version=8.2
./gradlew assembleDebug
```

### USB Device Not Found

**Error: "PCAN-USB device not found"**

1. Check USB connection
2. Grant USB permissions in Android
3. Check device filter in `usb_device_filter.xml`
4. Verify VID/PID: `lsusb` (on host) or check Android USB manager

```bash
# Check USB devices on Android
adb shell ls /dev/bus/usb/
```

### MQTT Connection Fails

**Error: "Failed to connect to MQTT broker"**

1. Check broker URL and port
2. Verify network connectivity
3. Check firewall rules
4. Test broker with MQTT client:

```bash
# Test broker connectivity
mosquitto_pub -h mqtt.fleet.cloud -p 1883 -u backend -P backend123 -t test -m "hello"
```

### App Crashes

```bash
# View crash logs
adb logcat | grep -i "exception\|error\|crash"

# Check specific error
adb logcat AndroidRuntime:E *:S
```

---

## Performance Testing

### Battery Drain

```bash
# Monitor battery usage
adb shell dumpsys batterystats | grep "com.fleet.bms"

# Reset battery stats
adb shell dumpsys batterystats --reset
```

### Memory Usage

```bash
# Monitor memory
adb shell dumpsys meminfo com.fleet.bms

# Monitor in real-time
watch -n 1 "adb shell dumpsys meminfo com.fleet.bms | grep TOTAL"
```

### Network Traffic

```bash
# Monitor data usage
adb shell dumpsys package com.fleet.bms | grep "Data usage"
```

---

## Building Release APK (Production)

### Step 1: Create Keystore

```bash
keytool -genkey -v -keystore bms-release-key.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias bms-key
```

### Step 2: Configure Signing

Add to `app/build.gradle.kts`:
```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("../bms-release-key.jks")
            storePassword = "your_store_password"
            keyAlias = "bms-key"
            keyPassword = "your_key_password"
        }
    }
    
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            // ... rest of release config
        }
    }
}
```

### Step 3: Build Release

```bash
./gradlew assembleRelease

# Output: app/build/outputs/apk/release/app-release.apk
```

---

## Quick Reference Commands

```bash
# Full clean build
./gradlew clean assembleDebug

# Build and install
./gradlew installDebug

# Run tests
./gradlew test

# Check for updates
./gradlew --refresh-dependencies

# View dependencies
./gradlew dependencies

# Generate APK and list output
./gradlew assembleDebug && ls -lh app/build/outputs/apk/debug/
```

---

## Next Steps

1. ✅ Build app: `./gradlew assembleDebug`
2. ✅ Run tests: `./gradlew test`
3. ✅ Deploy: `./gradlew installDebug`
4. 🔧 Configure MQTT settings for your environment
5. 🔌 Connect PCAN-USB FD device
6. 🔋 Connect ENNOID BMS
7. 🚀 Start monitoring!

---

**Your app is ready to build and deploy!** 🎉
