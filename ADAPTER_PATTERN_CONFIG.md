# Adapter Pattern & YAML Configuration

## Overview

The BMS app uses the **adapter pattern** for CAN-Bus hardware and BMS protocols. Protocol selection is **configurable via YAML** (`assets/bms_config.yml`).

---

## Configuration File

**Location:** `app/src/main/assets/bms_config.yml`

```yaml
bms:
  # CAN-Bus hardware adapter
  can_adapter: pcan-usb-fd    # or: canable

  # BMS protocol decoder
  bms_protocol: ennoid

  # CAN-Bus parameters
  can_bus:
    bitrate: 500000
    sample_point: 0.875
    listen_only: false

  # Adapter-specific settings
  adapters:
    pcan-usb-fd:
      vendor_id: 0x0C72
      product_id: 0x000C
      usb_baud_rate: 115200

    canable:
      vendor_id: 0x1D50
      product_id: 0x606F
      usb_baud_rate: 921600

  # Protocol-specific settings
  protocols:
    ennoid:
      cell_count: 114
      message_ids:
        pack_status: 0x100
        pack_current: 0x101
        temperatures: 0x102
        cell_stats: 0x103
        warnings: 0x104
        cell_voltages_start: 0x110
        cell_voltages_end: 0x11E
        gps_latitude: 0x180
        gps_longitude: 0x181
```

---

## Supported Adapters

### CAN-Bus Hardware (`can_adapter`)

| Value | Adapter | Description |
|-------|---------|-------------|
| `pcan-usb-fd` | PCAN-USB FD | PEAK PCAN-USB FD (VID=0x0C72, PID=0x000C) |
| `canable` | CANable | CANable/cantact (VID=0x1D50, PID=0x606F) |

### BMS Protocol (`bms_protocol`)

| Value | Protocol | Description |
|-------|----------|-------------|
| `ennoid` | ENNOID BMS | ENNOID BMS CAN protocol (0x100-0x11E) |

---

## Architecture

```
                    bms_config.yml
                           │
                           ▼
┌──────────────────────────────────────────────────────────────┐
│  ConfigLoader → BmsConfig                                    │
└──────────────────────────────────────────────────────────────┘
                           │
           ┌───────────────┴───────────────┐
           ▼                               ▼
┌──────────────────────┐      ┌──────────────────────┐
│ CanBusAdapterFactory │      │ BmsProtocolFactory    │
│                      │      │                      │
│ • pcan-usb-fd        │      │ • ennoid             │
│ • canable            │      │                      │
└──────────┬───────────┘      └──────────┬───────────┘
           │                             │
           ▼                             ▼
┌──────────────────────┐      ┌──────────────────────┐
│ CanBusPort           │      │ CanProtocolParser     │
│ (domain interface)   │      │ (domain interface)    │
└──────────────────────┘      └──────────────────────┘
           │                             │
           ▼                             ▼
┌──────────────────────┐      ┌──────────────────────┐
│ PcanUsbFdAdapter     │      │ EnnoidBmsProtocolDecoder│
│ CanableAdapter       │      │                      │
└──────────────────────┘      └──────────────────────┘
```

---

## Adding a New Adapter

### 1. Create adapter class

```kotlin
// infrastructure/hardware/usb/MyCanAdapter.kt
class MyCanAdapter(
    private val context: Context,
    private val adapterConfig: AdapterConfig
) : CanBusPort {
    // Implement connect(), configure(), readFrames(), disconnect(), isConnected()
}
```

### 2. Register in factory

```kotlin
// CanBusAdapterFactory.kt
"my-adapter" -> MyCanAdapter(context, adapterConfig ?: AdapterConfig(...))
```

### 3. Add to YAML

```yaml
adapters:
  my-adapter:
    vendor_id: 0x1234
    product_id: 0x5678
    usb_baud_rate: 115200
```

### 4. Use in config

```yaml
bms:
  can_adapter: my-adapter
```

---

## Adding a New BMS Protocol

### 1. Create decoder class

```kotlin
// infrastructure/hardware/protocol/OtherBmsDecoder.kt
class OtherBmsDecoder(
    private val config: ProtocolConfig
) : CanProtocolParser {
    override fun parse(frame: CanFrame): ParsedCanData? { ... }
}
```

### 2. Register in factory

```kotlin
// BmsProtocolFactory.kt
"other-bms" -> OtherBmsDecoder(protocolConfig ?: ProtocolConfig())
```

### 3. Add to YAML

```yaml
protocols:
  other-bms:
    cell_count: 96
    message_ids:
      pack_status: 0x200
      # ...
```

### 4. Use in config

```yaml
bms:
  bms_protocol: other-bms
```

---

## Switching Protocols

To switch from PCAN-USB FD to CANable:

```yaml
bms:
  can_adapter: canable
  bms_protocol: ennoid
```

To use a different BMS (when supported):

```yaml
bms:
  can_adapter: pcan-usb-fd
  bms_protocol: other-bms
```

---

## Files Summary

| File | Purpose |
|------|---------|
| `assets/bms_config.yml` | YAML configuration |
| `config/BmsConfig.kt` | Config data classes |
| `config/ConfigLoader.kt` | Loads YAML from assets |
| `adapter/CanBusAdapterFactory.kt` | Creates adapter from config |
| `adapter/BmsProtocolFactory.kt` | Creates protocol from config |
| `usb/PcanUsbFdAdapter.kt` | PCAN-USB FD implementation |
| `usb/CanableAdapter.kt` | CANable implementation |
| `protocol/EnnoidBmsProtocolDecoder.kt` | ENNOID BMS decoder |
| `di/ConfigModule.kt` | Provides BmsConfig |
| `di/InfrastructureModule.kt` | Uses factories |
| `di/UseCaseModule.kt` | Provides CanBusConfig |

---

## Dependency Flow

```
ConfigLoader.load(context)
    → BmsConfig

CanBusAdapterFactory.create(context, config)
    → CanBusPort (PcanUsbFdAdapter or CanableAdapter)

BmsProtocolFactory.create(config)
    → CanProtocolParser (EnnoidBmsProtocolDecoder)

CollectTelemetryUseCase(canBusPort, protocolParser, ...)
    → Reads CAN frames, parses, aggregates
```

---

**Protocol selection is fully configurable via YAML. No code changes needed to switch adapters or protocols.** ✅
