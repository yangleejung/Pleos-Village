package com.grad.pleosvillage.vehicle

import android.content.Context
import android.util.Log
import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.constants.NavigationStatus
import ai.pleos.playground.navi.data.DestinationArrivedInfo
import ai.pleos.playground.navi.data.DestinationInfo
import ai.pleos.playground.navi.data.DrivingInfo
import ai.pleos.playground.navi.data.RouteStateInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Remaining distance/time during route guidance (shown at the bottom left of the idle screen).
 * @param remainMeters Remaining distance to destination (m). DestinationInfo.remainDistance
 * @param remainSeconds Remaining time to destination (s). DrivingState.duration or DestinationInfo.remainTime
 * @param updatedAt Time the value was received (System.currentTimeMillis). ETA = updatedAt + remainSeconds
 */
data class NaviState(
    val destination: String,
    val remainMeters: Int?,
    val remainSeconds: Int?,
    val updatedAt: Long,
) {
    val etaMillis: Long? get() = remainSeconds?.let { updatedAt + it * 1000L }
}

/*
 * Reads route guidance via Pleos NaviHelper SDK 3.0.5. Read-only (no route request/cancel).
 * Permission: pleos.car.permission.NAVI_ROUTE (get*Info). Units per the SDK source KDoc:
 *   DestinationInfo.remainDistance (m), remainTime (s), DrivingState.duration (s from current position)
 * Routing state: onNavigationStatus == ROUTE_DRIVE, or RouteStateInfo.isRouting.
 * The emulator cannot change navi events. Check the screen with "내비 예시" in the debug menu.
 */
object NaviDataSource {
    private const val TAG = "PleosVillage"
    /** Interval for re-querying remaining distance during guidance. onDestinationInfo only fires "on change", so we also poll. */
    private const val POLL_MS = 30_000L

    private val _state = MutableStateFlow<NaviState?>(null)
    /** A value while guiding, otherwise null. */
    val state: StateFlow<NaviState?> = _state.asStateFlow()

    private val _demo = MutableStateFlow(false)
    /** Debug: show sample navi data. */
    val demo: StateFlow<Boolean> = _demo.asStateFlow()

    private var helper: NaviHelper? = null
    private var routing = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var pollJob: Job? = null

    private val listener = object : NaviHelperEventListener {
        override fun onNavigationStatus(status: NavigationStatus) {
            Log.d(TAG, "navi status=$status")
            setRouting(status == NavigationStatus.ROUTE_DRIVE)
        }

        override fun onRouteStateInfo(routeStateInfo: RouteStateInfo) {
            Log.d(TAG, "navi isRouting=${routeStateInfo.isRouting}")
            setRouting(routeStateInfo.isRouting)
        }

        override fun onDestinationInfo(destinationInfo: DestinationInfo) {
            if (!routing) return
            _state.value = NaviState(
                destination = destinationInfo.name,
                remainMeters = destinationInfo.remainDistance,
                remainSeconds = destinationInfo.remainTime,
                updatedAt = System.currentTimeMillis(),
            )
        }

        override fun onDrivingInfo(drivingInfo: DrivingInfo) {
            if (!routing) return
            val d = drivingInfo.destination
            _state.update { old ->
                NaviState(
                    destination = old?.destination?.ifEmpty { null } ?: d.name,
                    remainMeters = old?.remainMeters,
                    remainSeconds = d.duration,
                    updatedAt = System.currentTimeMillis(),
                )
            }
        }

        override fun onDestinationArrived(destinationArrivedInfo: DestinationArrivedInfo) = setRouting(false)
        override fun onRouteCancelled() = setRouting(false)
        override fun onRoutingCompleted() { helper?.getDestinationInfo() }

        override fun onError(errorCode: NaviErrorCode) {
            Log.w(TAG, "navi error=$errorCode")
        }
    }

    private fun setRouting(on: Boolean) {
        if (routing == on) return
        routing = on
        pollJob?.cancel()
        if (!on) { _state.value = null; return }
        pollJob = scope.launch {
            while (isActive) {
                runCatching { helper?.getDestinationInfo() }.onFailure { Log.w(TAG, "navi getDestinationInfo", it) }
                delay(POLL_MS)
            }
        }
    }

    /** Called when the app UI is created. The app keeps working if it fails (vehicle without navi). */
    fun start(context: Context) {
        if (helper != null) return
        val h = NaviHelper(context.applicationContext)
        helper = h
        runCatching {
            h.initialize(
                {
                    Log.d(TAG, "navi connected")
                    h.addListener(listener)
                    runCatching { h.getRouteStateInfo() }
                },
                { e -> Log.w(TAG, "navi connect failed", e) },
            )
        }.onFailure { Log.w(TAG, "navi initialize", it) }
    }

    fun stop() {
        pollJob?.cancel()
        helper?.let { h -> runCatching { h.removeListener(listener); h.release() } }
        helper = null
        routing = false
        _state.value = null
    }

    fun setDemo(on: Boolean) { _demo.value = on }

    /** Debug sample: 12.4 km, 28 min remaining. */
    fun demoState() = NaviState("할머니 댁", 12_400, 28 * 60, System.currentTimeMillis())
}
