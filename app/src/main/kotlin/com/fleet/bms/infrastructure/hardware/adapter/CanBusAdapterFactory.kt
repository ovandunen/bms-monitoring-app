package com.fleet.bms.infrastructure.hardware.adapter

import android.content.Context
import com.fleet.bms.domain.repository.CanBusPort
import com.fleet.bms.infrastructure.config.BmsConfig
import com.fleet.bms.infrastructure.hardware.usb.CanableAdapter
import com.fleet.bms.infrastructure.hardware.usb.PcanUsbFdAdapter
import timber.log.Timber

/**
 * Factory: CAN-Bus Adapter
 *
 * Creates the appropriate CanBusPort implementation based on YAML config.
 *
 * Supported adapters:
 * - pcan-usb-fd: PEAK PCAN-USB FD
 * - canable: CANable/cantact (USB Serial)
 */
object CanBusAdapterFactory {

    fun create(context: Context, config: BmsConfig): CanBusPort {
        val adapterName = config.canAdapter
        val adapterConfig = config.getAdapterConfig(adapterName)

        return when (adapterName) {
            "pcan-usb-fd" -> {
                Timber.i("Creating PCAN-USB FD adapter (config: $adapterConfig)")
                PcanUsbFdAdapter(context, adapterConfig ?: com.fleet.bms.infrastructure.config.AdapterConfig(0x0C72, 0x000C, 115200))
            }
            "canable" -> {
                Timber.i("Creating CANable adapter (config: $adapterConfig)")
                CanableAdapter(context, adapterConfig ?: com.fleet.bms.infrastructure.config.AdapterConfig(0x1D50, 0x606F, 921600))
            }
            else -> {
                Timber.w("Unknown adapter '$adapterName', defaulting to pcan-usb-fd")
                PcanUsbFdAdapter(context, adapterConfig ?: com.fleet.bms.infrastructure.config.AdapterConfig())
            }
        }
    }
}
