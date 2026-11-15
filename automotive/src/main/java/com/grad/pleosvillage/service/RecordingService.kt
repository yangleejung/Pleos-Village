package com.grad.pleosvillage.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import com.grad.pleosvillage.data.AppRepository
import androidx.core.content.ContextCompat
import com.grad.pleosvillage.R
import com.grad.pleosvillage.vehicle.VehicleDataSource
import com.grad.pleosvillage.vehicle.VehicleState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/**
 * Foreground Service that receives vehicle data, so recording continues with no app UI.
 * Only this service registers vehicle data listeners. The UI subscribes to [vehicleState].
 *
 * The only notification is this service's ongoing one (importance LOW, no sound/popup). SPEC 6.3.
 */
class RecordingService : Service() {

    private var dataSource: VehicleDataSource? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "[service] onCreate")
        createChannel()
        startForeground(
            NOTIFICATION_ID,
            buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
        )
        dataSource = VehicleDataSource(this, owner = "service").also {
            it.start()
            currentSource.value = it
            // Feed vehicle values into the judging engine (even with no UI, even while driving). SPEC §7 data flow.
            AppRepository.init(this)
            AppRepository.attach(it.state)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "[service] onStartCommand")
        return START_STICKY
    }

    override fun onDestroy() {
        Log.i(TAG, "[service] onDestroy")
        currentSource.value = null
        dataSource?.release()
        dataSource = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.recording_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification =
        Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.recording_notification_title))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

    companion object {
        private const val TAG = VehicleDataSource.TAG
        private const val CHANNEL_ID = "recording"
        private const val NOTIFICATION_ID = 1

        private val currentSource = MutableStateFlow<VehicleDataSource?>(null)

        /** Vehicle values received by the service. Empty state if the service is not running. */
        @OptIn(ExperimentalCoroutinesApi::class)
        val vehicleState: Flow<VehicleState> =
            currentSource.flatMapLatest { it?.state ?: flowOf(VehicleState()) }

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, RecordingService::class.java))
        }
    }
}
