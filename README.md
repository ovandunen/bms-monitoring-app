# BMS Android Application - DDD Architecture

**CarRapide EV Fleet Battery Monitoring System**

## Architecture

This application follows Domain-Driven Design (DDD) with Clean Architecture principles:

```
┌─────────────────────────────────────────────────────────┐
│                    INTERFACE LAYER                       │
│  (UI, Activities, Services, ViewModels, Compose)        │
│                          ↓                               │
│                   APPLICATION LAYER                      │
│      (Use Cases, Application Services, DTOs)            │
│                          ↓                               │
│                     DOMAIN LAYER                         │
│  (Entities, Value Objects, Domain Services, Ports)      │
│                          ↑                               │
│                 INFRASTRUCTURE LAYER                     │
│  (USB/CAN Adapters, MQTT, Room DB, GPS, Android)       │
└─────────────────────────────────────────────────────────┘
```

## Layers

### 1. Domain Layer (Pure Kotlin)
- **NO Android dependencies**
- Business logic and rules
- Value Objects: `BatteryTelemetry`, `CellVoltages`, `CanFrame`
- Domain Services: `TelemetryAggregator`, `AlertEvaluator`
- Repository Interfaces (Ports): `CanBusPort`, `TelemetryPublisherPort`

### 2. Application Layer
- Use Cases orchestrating domain logic
- `CollectTelemetryUseCase`, `PublishTelemetryUseCase`
- DTOs matching backend format
- Application services

### 3. Infrastructure Layer (Adapters)
- Hardware: `PcanUsbAdapter` implements `CanBusPort`
- Messaging: `MqttTelemetryPublisher` implements `TelemetryPublisherPort`
- Persistence: `LocalTelemetryRepository` implements `TelemetryStoragePort`
- Protocol: `BatteryProtocolDecoder` parses CAN frames

### 4. Interface Layer
- Jetpack Compose UI
- ViewModels with StateFlow
- Android Services for background monitoring
- Notifications

## Hardware

- **BMS:** ENNOID BMS (114S configuration)
- **CAN Adapter:** PCAN-USB FD
- **Bitrate:** 500 kbps
- **Protocol:** Custom battery protocol (message IDs 0x100-0x1FF)

## Tech Stack

- Kotlin 1.9.21
- Jetpack Compose
- Coroutines & Flow
- Hilt for DI
- Room for local storage
- Eclipse Paho MQTT
- Timber for logging

## Project Structure

See `ARCHITECTURE.md` for detailed structure.

## Building

```bash
./gradlew assembleDebug
```

## Testing

```bash
# Unit tests (domain & application)
./gradlew test

# Integration tests
./gradlew connectedAndroidTest
```

## Configuration

Edit `config.properties` or use in-app settings:
- Battery Pack ID
- Vehicle ID
- MQTT broker URL
- CAN bitrate

## License

Proprietary - EcoCar Solaire AG
