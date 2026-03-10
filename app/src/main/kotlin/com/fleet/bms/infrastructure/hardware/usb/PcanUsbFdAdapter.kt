package com.fleet.bms.infrastructure.hardware.usb

import android.content.Context
import com.fleet.bms.domain.model.CanFrame
import com.fleet.bms.domain.model.CanId
import com.fleet.bms.domain.repository.CanBusConfig
import com.fleet.bms.domain.repository.CanBusPort
import com.fleet.bms.infrastructure.config.AdapterConfig
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.Instant

/**
 * Adapter: PCAN-USB FD
 *
 * Implements CanBusPort for PEAK PCAN-USB FD.
 * VID/PID and baud rate are configurable via AdapterConfig (from bms_config.yml).
 */
class PcanUsbFdAdapter(
    private val context: Context,
    private val adapterConfig: AdapterConfig
) : CanBusPort {

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as android.hardware.usb.UsbManager
    private var usbPort: com.hoho.android.usbserial.driver.UsbSerialPort? = null
    private var device: android.hardware.usb.UsbDevice? = null

    override suspend fun connect(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val drivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
            val driver = drivers.firstOrNull {
                it.device.vendorId == adapterConfig.vendorId && it.device.productId == adapterConfig.productId
            } ?: return@withContext Result.failure(Exception("PCAN-USB FD not found (VID=${adapterConfig.vendorId}, PID=${adapterConfig.productId})"))

            device = driver.device
            if (!usbManager.hasPermission(device)) {
                return@withContext Result.failure(Exception("USB permission not granted"))
            }

            val connection = usbManager.openDevice(device)
                ?: return@withContext Result.failure(Exception("Failed to open USB device"))

            usbPort = driver.ports[0].apply {
                open(connection)
                setParameters(
                    adapterConfig.usbBaudRate,
                    8,
                    com.hoho.android.usbserial.driver.UsbSerialPort.STOPBITS_1,
                    com.hoho.android.usbserial.driver.UsbSerialPort.PARITY_NONE
                )
            }

            Timber.i("PCAN-USB FD connected (baud=${adapterConfig.usbBaudRate})")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to connect PCAN-USB FD")
            Result.failure(e)
        }
    }

    override suspend fun configure(config: CanBusConfig): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val port = usbPort ?: return@withContext Result.failure(Exception("Not connected"))
            val bitrateCode = when (config.bitrate) {
                125_000 -> "3"
                250_000 -> "4"
                500_000 -> "6"
                1_000_000 -> "8"
                else -> "6"
            }
            port.write("S$bitrateCode\r".toByteArray(), 1000)
            Thread.sleep(100)
            Timber.i("PCAN-USB FD configured: ${config.bitrate} bps")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "PCAN configure failed")
            Result.failure(e)
        }
    }

    override fun readFrames(): Flow<CanFrame> = callbackFlow {
        val port = usbPort ?: run {
            close(Exception("Not connected"))
            return@callbackFlow
        }
        val buffer = ByteArray(256)
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            while (isActive) {
                try {
                    val n = port.read(buffer, 100)
                    if (n > 0) {
                        parsePcanFrames(String(buffer, 0, n)).forEach { trySend(it) }
                    }
                } catch (e: Exception) {
                    if (isActive) Timber.w(e, "CAN read error")
                }
            }
        }
        awaitClose { Timber.d("PCAN read stopped") }
    }

    private fun parsePcanFrames(data: String): List<CanFrame> = buildList {
        data.split("\r", "\n").forEach { line ->
            if (line.isEmpty()) return@forEach
            try {
                if (line[0] != 't' && line[0] != 'T') return@forEach
                val isExt = line[0] == 'T'
                val idLen = if (isExt) 8 else 3
                val id = line.substring(1, 1 + idLen).toInt(16)
                val len = line[1 + idLen].toString().toInt()
                val start = 1 + idLen + 1
                val bytes = ByteArray(len) { i -> line.substring(start + i * 2, start + i * 2 + 2).toInt(16).toByte() }
                add(CanFrame(CanId(id), bytes, Instant.now(), isExt))
            } catch (_: Exception) {}
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        usbPort?.close()
        usbPort = null
        device = null
        Timber.i("PCAN-USB FD disconnected")
    }

    override fun isConnected(): Boolean = usbPort != null
}
