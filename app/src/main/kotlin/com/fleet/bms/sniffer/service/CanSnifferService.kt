package com.fleet.bms.sniffer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.fleet.bms.sniffer.domain.usecase.CanSniffingUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Foreground service for CAN sniffing.
 * Runs with WakeLock to prevent sleep interruption.
 */
@AndroidEntryPoint
class CanSnifferService : Service() {

    @Inject
    lateinit var sniffingUseCase: CanSniffingUseCase

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var wakeLock: PowerManager.WakeLock? = null

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): CanSnifferService = this@CanSnifferService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CanSniffer::WakeLock")
        wakeLock?.acquire(10 * 60 * 60 * 1000L) // 10 hours max
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val label = intent.getStringExtra(EXTRA_SESSION_LABEL) ?: "sniffing"
                startForeground(NOTIFICATION_ID, createNotification(0f, 0))
                scope.launch {
                    sniffingUseCase.startSession(label)
                    sniffingUseCase.framesPerSecond.collect { fps ->
                        val count = sniffingUseCase.getEntries().size
                        val notification = createNotification(fps, count)
                        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                            .notify(NOTIFICATION_ID, notification)
                    }
                }
            }
            ACTION_STOP -> {
                scope.launch {
                    sniffingUseCase.stopSession()
                }
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.launch {
            sniffingUseCase.stopSession()
        }
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        scope.cancel()
        super.onDestroy()
    }

    fun getSniffingUseCase(): CanSniffingUseCase = sniffingUseCase

    fun updateNotification() {
        scope.launch {
            val fps = sniffingUseCase.framesPerSecond.first()
            val count = sniffingUseCase.getEntries().size
            val notification = createNotification(fps, count)
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .notify(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotification(framesPerSec: Float, uniqueIds: Int): Notification {
        val channelId = "can_sniffer_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "CAN Sniffer",
                NotificationManager.IMPORTANCE_LOW
            ).apply { setShowBadge(false) }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            packageManager.getLaunchIntentForPackage(packageName)?.apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("CAN Sniffer")
            .setContentText("${"%.1f".format(framesPerSec)} fps | $uniqueIds IDs")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 2001
        const val ACTION_START = "com.fleet.bms.sniffer.START"
        const val ACTION_STOP = "com.fleet.bms.sniffer.STOP"
        const val EXTRA_SESSION_LABEL = "session_label"

        fun start(context: Context, sessionLabel: String = "sniffing") {
            val intent = Intent(context, CanSnifferService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SESSION_LABEL, sessionLabel)
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, CanSnifferService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
