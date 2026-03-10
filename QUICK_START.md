# Quick Start Guide - CarRapide BMS Android App

**5-minute guide to get started**

---

## ✅ What's Already Created

1. **Build Configuration** ✅
   - `settings.gradle.kts`
   - `build.gradle.kts` (root + app)
   - `gradle.properties`

2. **Android Configuration** ✅
   - `AndroidManifest.xml` (USB permissions, services)
   - `res/values/strings.xml`
   - `res/values/colors.xml`
   - `res/xml/usb_device_filter.xml` (PCAN-USB FD)

3. **Dependency Injection (Hilt)** ✅
   - `DomainModule.kt`
   - `InfrastructureModule.kt`
   - `UseCaseModule.kt`

4. **Application Class** ✅
   - `BmsApplication.kt`

5. **UI Foundation** ✅
   - `MainActivity.kt`
   - `Theme.kt`
   - `Type.kt`

6. **Documentation** ✅
   - `README.md` - Architecture overview
   - `IMPLEMENTATION_GUIDE.md` - Complete domain + application code
   - `IMPLEMENTATION_GUIDE_PART2.md` - Complete infrastructure code
   - `IMPLEMENTATION_COMPLETE_SUMMARY.md` - Full manifest + examples
   - `COMPLETE_CODE_PACKAGE.md` - Project status
   - `PROJECT_STATUS.md` - Detailed checklist

---

## 📝 Next Steps

### Option 1: Copy Remaining Files (Recommended)

**Time: 2-3 hours**

All code is provided in the implementation guides. Simply copy-paste:

1. **Domain Layer** (from `IMPLEMENTATION_GUIDE.md`)
   - Open guide
   - Find "Domain Layer" section
   - Copy each code block to corresponding file
   - Files: `MeasurementTypes.kt`, `CellVoltages.kt`, `CanFrame.kt`, `BatteryTelemetry.kt`, `TelemetryAggregator.kt`, `AlertEvaluator.kt`, `CanBusPort.kt`, `TelemetryPublisherPort.kt`, `TelemetryStoragePort.kt`

2. **Application Layer** (from `IMPLEMENTATION_GUIDE.md`)
   - Copy use cases and DTOs
   - Files: `CollectTelemetryUseCase.kt`, `PublishTelemetryUseCase.kt`, `SyncBufferedDataUseCase.kt`, `StartMonitoringUseCase.kt`, `StopMonitoringUseCase.kt`, `TelemetryMessageDto.kt`

3. **Infrastructure Layer** (from `IMPLEMENTATION_GUIDE_PART2.md`)
   - Copy adapters and implementations
   - Files: `PcanUsbAdapter.kt`, `BatteryProtocolDecoder.kt`, `MqttTelemetryPublisher.kt`, `TelemetryDatabase.kt`, `TelemetryEntity.kt`, `TelemetryDao.kt`, `LocalTelemetryRepository.kt`

4. **Interface Layer** (from `IMPLEMENTATION_COMPLETE_SUMMARY.md`)
   - Copy ViewModel and UI components
   - Files: `DashboardScreen.kt`, `DashboardViewModel.kt`, `BatteryStatusCard.kt`, etc.

### Option 2: Request AI to Create Files

Ask your AI assistant:
- "Create all domain layer files from the implementation guide"
- "Create all application layer files"
- "Create all infrastructure files"
- "Create all UI components"

### Option 3: Generate from Script

Create a script to generate files from the guides (advanced).

---

## 🚀 Build the App

Once files are created:

```bash
cd /Users/janet/ovd/project/bms-integration/bms-android-app

# Build debug APK
./gradlew assembleDebug

# Install to connected device/emulator
./gradlew installDebug

# Run tests
./gradlew test
./gradlew connectedAndroidTest
```

---

## 🧪 Test Backend First (Without Android)

**Before creating all Android files**, test the backend:

### 1. Create Python Simulator

Create `telemetry-simulator.py`:

```python
#!/usr/bin/env python3
import paho.mqtt.client as mqtt
import json
import time
import random
from datetime import datetime

BROKER = "localhost"
PORT = 1883
USERNAME = "backend"
PASSWORD = "backend123"

def generate_telemetry():
    return {
        "messageId": f"msg-{int(time.time() * 1000)}",
        "batteryPackId": "550e8400-e29b-41d4-a716-446655440000",
        "vehicleId": "vehicle_001",
        "timestamp": datetime.utcnow().isoformat() + "Z",
        "stateOfCharge": random.uniform(70.0, 85.0),
        "voltage": random.uniform(370.0, 380.0),
        "current": random.uniform(-50.0, -40.0),
        "temperatureMin": random.uniform(27.0, 29.0),
        "temperatureMax": random.uniform(31.0, 33.0),
        "temperatureAvg": random.uniform(29.0, 31.0),
        "cellVoltageMin": random.uniform(3.25, 3.28),
        "cellVoltageMax": random.uniform(3.30, 3.33),
        "cellVoltageDelta": random.uniform(0.03, 0.05),
        "cellVoltages": [random.uniform(3.28, 3.31) for _ in range(114)]
    }

def main():
    client = mqtt.Client()
    client.username_pw_set(USERNAME, PASSWORD)
    client.connect(BROKER, PORT, 60)
    
    print(f"Publishing to: fleet/vehicle_001/bms/telemetry")
    
    try:
        while True:
            telemetry = generate_telemetry()
            payload = json.dumps(telemetry)
            result = client.publish("fleet/vehicle_001/bms/telemetry", payload, qos=1)
            
            if result.rc == 0:
                print(f"✓ Published: SOC={telemetry['stateOfCharge']:.1f}%")
            else:
                print(f"✗ Failed: {result.rc}")
            
            time.sleep(10)
            
    except KeyboardInterrupt:
        client.disconnect()

if __name__ == "__main__":
    main()
```

### 2. Start Backend

```bash
# Terminal 1: Start infrastructure
cd /Users/janet/ovd/project/bms-integration/app/fleet-ddd-system
docker-compose up -d

# Wait for services to start (30 seconds)

# Terminal 2: Start backend (after fixing Gradle 8.5)
./gradlew quarkusDev
```

### 3. Run Simulator

```bash
# Terminal 3: Run simulator
python3 telemetry-simulator.py
```

### 4. Verify Data Flow

```bash
# Terminal 4: Query backend API
curl http://localhost:8080/api/v1/batteries/550e8400-e29b-41d4-a716-446655440000

# Should see telemetry data!
```

This proves the backend works end-to-end! 🎉

---

## 📊 Project Completion Checklist

| Task | Status | Time |
|------|--------|------|
| Build configuration | ✅ Complete | - |
| Android configuration | ✅ Complete | - |
| Hilt DI modules | ✅ Complete | - |
| Application class | ✅ Complete | - |
| UI foundation | ✅ Complete | - |
| Domain layer (10 files) | 📝 Code provided | 30 min |
| Application layer (6 files) | 📝 Code provided | 20 min |
| Infrastructure layer (8 files) | 📝 Code provided | 40 min |
| Interface layer (10+ files) | 📝 Code provided | 60 min |
| **Total remaining** | | **2-3 hours** |

---

## 🎯 Priority Order

1. **Immediate: Test backend** (30 min)
   - Proves architecture works
   - Validates data flow
   - No Android needed

2. **Then: Create Android files** (2-3 hours)
   - Copy from guides
   - Build app
   - Deploy to tablet

3. **Finally: Hardware integration** (1-2 days)
   - Connect PCAN-USB FD
   - Connect to ENNOID BMS
   - Test real telemetry flow

---

## 💡 Pro Tips

1. **Start with backend testing** - Validates everything before hardware
2. **Use the guides** - Every line of code is provided
3. **Follow DDD layers** - Create domain → application → infrastructure → interface
4. **Test incrementally** - Build after each layer
5. **Use simulator** - Test without hardware first

---

## 🆘 If You Need Help

**Backend won't build?**
- Fix Gradle 8.5 (see `DATA_FLOW_ANALYSIS.md` in backend)

**Need specific files created?**
- Ask: "Create all domain layer files"
- Ask: "Create PcanUsbAdapter.kt"

**Architecture questions?**
- Read `README.md` for overview
- Read `IMPLEMENTATION_COMPLETE_SUMMARY.md` for details

---

**You're 2-3 hours away from a complete DDD Android BMS app! 🚀**

Start with backend testing to validate architecture, then copy remaining files from guides.
