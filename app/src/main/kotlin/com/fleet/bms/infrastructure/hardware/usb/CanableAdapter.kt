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
 * Adapter: CANable / cantact
 *
 * Implements CanBusPort for CANable (USB Serial CAN adapter).
 * Uses slcan/cantact protocol - similar ASCII format to PCAN.
 * VID=0x1D50, PID=0x606F (or 0x6070 for cantact)
 */
class CanableAdapter(
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
            } ?: return@withContext Result.failure(Exception("CANable not found (VID=${adapterConfig.vendorId}, PID=${adapterConfig.productId})"))

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

            // CANable slcan: open channel, set bitrate
            usbPort?.write("O\r".toByteArray(), 1000)  // Open channel
            Thread.sleep(50)
            val bitrateCmd = when (adapterConfig.usbBaudRate) {
                921600 -> "S6\r"  // 500k
                1000000 -> "S8\r" // 1M
                else -> "S6\r"
            }
            usbPort?.write(bitrateCmd.toByteArray(), 1000)
            Thread.sleep(50)

            Timber.i("CANable connected (baud=${adapterConfig.usbBaudRate})")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to connect CANable")
            Result.failure(e)
        }
    }

    override suspend fun configure(config: CanBusConfig): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val port = usbPort ?: return@withContext Result.failure(Exception("Not connected"))
            val cmd = when (config.bitrate) {
                125_000 -> "S3\r"
                250_000 -> "S4\r"
                500_000 -> "S6\r"
                1_000_000 -> "S8\r"
                else -> "S6\r"
            }
            port.write(cmd.toByteArray(), 1000)
            Thread.sleep(50)
            Timber.i("CANable configured: ${config.bitrate} bps")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "CANable configure failed")
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
                        parseSlcanFrames(String(buffer, 0, n)).forEach { trySend(it) }
                    }
                } catch (e: Exception) {
                    if (isActive) Timber.w(e, "CAN read error")
                }
            }
        }
        awaitClose {
            usbPort?.write("C\r".toByteArray(), 500)  // Close channel
            Timber.d("CANable read stopped")
        }
    }

    private fun parseSlcanFrames(data: String): List<CanFrame> = buildList {
        data.split("\r", "\n").forEach { line ->
            if (line.isEmpty()) return@forEach
            try {
                if (line[0] != 't' && line[0] != 'T' && line[0] != 'r' && line[0] != 'R') return@forEach
                val isExt = line[0] == 'T' || line[0] == 'R'
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
        usbPort?.write("C\r".toByteArray(), 500)
        usbPort?.close()
        usbPort = null
        device = null
        Timber.i("CANable disconnected")
    }

    override fun isConnected(): Boolean = usbPort != null
}
