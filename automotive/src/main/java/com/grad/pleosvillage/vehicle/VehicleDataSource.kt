package com.grad.pleosvillage.vehicle

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.api.model.area.WindowArea
import ai.pleos.playground.vehicle.api.model.capability.AccessType
import ai.pleos.playground.vehicle.api.model.capability.CapabilityConfig
import ai.pleos.playground.vehicle.constant.control.LightState
import ai.pleos.playground.vehicle.constant.control.WiperState
import ai.pleos.playground.vehicle.listener.HvacDefrosterStateListener
import ai.pleos.playground.vehicle.listener.BrakePedalStatusListener
import ai.pleos.playground.vehicle.listener.LightStateListener
import ai.pleos.playground.vehicle.listener.SeatOccupancyListener
import ai.pleos.playground.vehicle.listener.SteeringWheelAngleListener
import ai.pleos.playground.vehicle.listener.WiperStateListener
import android.car.Car
import android.car.VehicleGear
import android.car.VehiclePropertyIds
import android.car.drivingstate.CarUxRestrictions
import android.car.drivingstate.CarUxRestrictionsManager
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

/**
 * Central class for reading vehicle data. Used the same way by the UI (Activity) and the recording service (Foreground Service).
 *
 * Each value goes: capability check (check…Capabilities) → current value read (getCurrent…) → change listener (register…).
 * Gear is read from CarPropertyManager GEAR_SELECTION because the Vehicle SDK Gear API is deprecated
 * ("use the Google GEAR_SELECTION property").
 *
 * Seatbelt-unbuckled warning was removed: the emulator doesn't support it (ERROR_NOT_SUPPORTED_PROPERTY, property 0x25200709).
 *
 * Read-only. No set API or CONTROL_CAR_* permissions.
 */
/**
 * @param owner Name attached to logs. Tells which side (service/UI) received the value.
 */
class VehicleDataSource(context: Context, private val owner: String = "app") {

    private val appContext = context.applicationContext
    private val vehicle = Vehicle(appContext)
    private var car: Car? = null

    private val _state = MutableStateFlow(VehicleState())
    val state: StateFlow<VehicleState> = _state.asStateFlow()

    private val cleanups = mutableListOf<() -> Unit>()
    private val lastLogged = ConcurrentHashMap<String, String>()

    @Volatile
    private var started = false

    fun start() {
        if (started) return
        started = true
        Log.i(TAG, "[$owner] VehicleDataSource start")
        try {
            vehicle.initialize()
        } catch (e: Exception) {
            Log.e(TAG, "Vehicle.initialize 실패", e)
        }
        car = runCatching { Car.createCar(appContext) }
            .onFailure { Log.e(TAG, "Car.createCar 실패", it) }
            .getOrNull()
        startUxRestrictions()
        startGear()
        startWiper()
        startLowBeam()
        startSeatOccupancy()
        startSteeringAngle()
        startDefroster()
        startBrake()
    }

    fun release() {
        if (!started) return
        started = false
        val actions = synchronized(cleanups) { cleanups.toList().also { cleanups.clear() } }
        actions.forEach { action -> runCatching(action).onFailure { Log.w(TAG, "unregister 실패", it) } }
        runCatching { vehicle.release() }.onFailure { Log.w(TAG, "Vehicle.release 실패", it) }
        runCatching { car?.disconnect() }
        car = null
        Log.i(TAG, "[$owner] VehicleDataSource released")
    }

    // ---------- Driving restriction (CarUxRestrictionsManager) ----------

    private fun startUxRestrictions() {
        val label = "주행규제"
        try {
            val c = car ?: run {
                setReading(label, "unsupported") { it.copy(drivingRestricted = Reading.Unsupported("Car 서비스 연결 실패")) }
                return
            }
            val um = c.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as CarUxRestrictionsManager
            val put: (CarUxRestrictions?) -> Unit = { r ->
                val restricted = r?.isRequiresDistractionOptimization
                setReading(label, restricted?.toString() ?: "null") { it.copy(drivingRestricted = valueOrNull(restricted)) }
            }
            put(um.currentCarUxRestrictions)
            um.registerListener { r -> put(r) }
            addCleanup { um.unregisterListener() }
        } catch (e: Exception) {
            fail(label, e) { s, r -> s.copy(drivingRestricted = r) }
        }
    }

    // ---------- Gear (CarPropertyManager) ----------

    private val gearCallback = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) {
            if (value.propertyId == VehiclePropertyIds.GEAR_SELECTION) {
                setGear(value.value as? Int)
            }
        }

        override fun onErrorEvent(propertyId: Int, areaId: Int) {
            setReading("기어", "error($propertyId)") { it.copy(gear = Reading.Error("변경 알림 오류")) }
        }
    }

    private fun startGear() {
        try {
            val c = car ?: run {
                setReading("기어", "unsupported") { it.copy(gear = Reading.Unsupported("Car 서비스 연결 실패")) }
                return
            }
            val pm = c.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
            if (pm.getCarPropertyConfig(VehiclePropertyIds.GEAR_SELECTION) == null) {
                setReading("기어", "unsupported") { it.copy(gear = Reading.Unsupported("미지원 또는 권한 없음")) }
                return
            }
            setReading("기어", "supported") { it.copy(gear = Reading.Waiting) }
            runCatching { pm.getIntProperty(VehiclePropertyIds.GEAR_SELECTION, GLOBAL_AREA) }
                .onSuccess { setGear(it) }
                .onFailure { Log.w(TAG, "기어 조회 실패", it) }
            val ok = pm.registerCallback(
                gearCallback,
                VehiclePropertyIds.GEAR_SELECTION,
                CarPropertyManager.SENSOR_RATE_ONCHANGE,
            )
            if (ok) addCleanup { pm.unregisterCallback(gearCallback) } else Log.w(TAG, "기어 register 실패")
        } catch (e: Exception) {
            fail("기어", e) { s, r -> s.copy(gear = r) }
        }
    }

    private fun setGear(raw: Int?) {
        val gear = when (raw) {
            null -> null
            VehicleGear.GEAR_PARK -> GearPosition.PARK
            VehicleGear.GEAR_REVERSE -> GearPosition.REVERSE
            VehicleGear.GEAR_NEUTRAL -> GearPosition.NEUTRAL
            VehicleGear.GEAR_DRIVE -> GearPosition.DRIVE
            VehicleGear.GEAR_UNKNOWN -> GearPosition.UNKNOWN
            else -> GearPosition.OTHER
        }
        setReading("기어", gear?.name ?: "null") { it.copy(gear = valueOrNull(gear)) }
    }

    // ---------- Wipers ----------

    private fun startWiper() {
        val wiper = vehicle.getWiper()
        call("와이퍼", { s, r -> s.copy(wiperSupport = r) }) {
            wiper.checkWiperStateCapabilities({ config, areaMap ->
                if (!checkReadable("와이퍼", config) { s, r -> s.copy(wiperSupport = r) }) return@checkWiperStateCapabilities
                val areas = areaMap.keys.toList()
                Log.i(TAG, "와이퍼 지원 영역: ${areas.map { it.label() }}")
                areas.forEach { area ->
                    val key = area.label()
                    val label = "와이퍼[$key]"
                    val put: (Reading<String>) -> Unit = { r ->
                        setReading(label, logText(r)) { it.copy(wiper = it.wiper + (key to r)) }
                    }
                    put(Reading.Waiting)
                    call(label, { s, _ -> s }) {
                        wiper.getCurrentWiperState(area, { v -> put(valueOrNull(v?.name)) }, { e -> logFail(label, e) })
                    }
                    val listener = object : WiperStateListener {
                        override fun onWiperStateUpdated(state: WiperState?) = put(valueOrNull(state?.name))
                        override fun onFailed(e: Exception) = logFail(label, e)
                    }
                    call(label, { s, _ -> s }) {
                        wiper.registerWiperState(area, listener)
                        addCleanup { wiper.unregisterWiperState(area, listener) }
                    }
                }
            }, { e -> fail("와이퍼", e) { s, r -> s.copy(wiperSupport = r) } })
        }
    }

    // ---------- Low beam ----------

    private fun startLowBeam() {
        val light = vehicle.getLight()
        val label = "하향등"
        val put: (Reading<String>) -> Unit = { r -> setReading(label, logText(r)) { it.copy(lowBeam = r) } }
        call(label, { s, r -> s.copy(lowBeam = r) }) {
            light.checkLowBeamLightStateCapabilities({ config, states ->
                if (!checkReadable(label, config) { s, r -> s.copy(lowBeam = r) }) return@checkLowBeamLightStateCapabilities
                Log.i(TAG, "하향등 지원 상태: $states")
                put(Reading.Waiting)
                call(label, { s, _ -> s }) {
                    light.getCurrentLowBeamLightState({ v -> put(valueOrNull(v?.name)) }, { e -> logFail(label, e) })
                }
                val listener = object : LightStateListener {
                    override fun onLightStateUpdated(state: LightState?) = put(valueOrNull(state?.name))
                    override fun onFailed(e: Exception) = logFail(label, e)
                }
                call(label, { s, _ -> s }) {
                    light.registerLowBeamLightState(listener)
                    addCleanup { light.unregisterLowBeamLightState(listener) }
                }
            }, { e -> fail(label, e) { s, r -> s.copy(lowBeam = r) } })
        }
    }

    // ---------- Seat occupancy ----------

    private fun startSeatOccupancy() {
        val seat = vehicle.getSeat()
        call("탑승", { s, r -> s.copy(seatOccupancySupport = r) }) {
            seat.checkSeatOccupancyCapabilities({ config, areas ->
                if (!checkReadable("탑승", config) { s, r -> s.copy(seatOccupancySupport = r) }) return@checkSeatOccupancyCapabilities
                Log.i(TAG, "탑승 지원 좌석: ${areas.map { it.label() }}")
                areas.forEach { area ->
                    val key = area.label()
                    val label = "탑승[$key]"
                    val put: (Reading<Boolean>) -> Unit = { r ->
                        setReading(label, logText(r)) { it.copy(seatOccupancy = it.seatOccupancy + (key to r)) }
                    }
                    put(Reading.Waiting)
                    call(label, { s, _ -> s }) {
                        seat.getCurrentSeatOccupancy(area, { v -> put(valueOrNull(v)) }, { e -> logFail(label, e) })
                    }
                    val listener = object : SeatOccupancyListener {
                        override fun onSeatOccupancyUpdated(occupancy: Boolean?) = put(valueOrNull(occupancy))
                        override fun onFailed(e: Exception) = logFail(label, e)
                    }
                    call(label, { s, _ -> s }) {
                        seat.registerSeatOccupancy(area, listener)
                        addCleanup { seat.unregisterSeatOccupancy(area, listener) }
                    }
                }
            }, { e -> fail("탑승", e) { s, r -> s.copy(seatOccupancySupport = r) } })
        }
    }

    // ---------- Steering angle ----------

    private fun startSteeringAngle() {
        val wheel = vehicle.getSteeringWheel()
        val label = "조향각"
        val put: (Float?) -> Unit = { v ->
            // To avoid log flooding, record only when the value rounded to 0.1° changes.
            val rounded = v?.let { (it * 10).roundToInt() / 10f }
            setReading(label, rounded?.toString() ?: "null") { it.copy(steeringAngle = valueOrNull(rounded)) }
        }
        call(label, { s, r -> s.copy(steeringAngle = r) }) {
            wheel.checkSteeringWheelAngleCapabilities({ config, bounds ->
                if (!checkReadable(label, config) { s, r -> s.copy(steeringAngle = r) }) return@checkSteeringWheelAngleCapabilities
                Log.i(TAG, "조향각 범위: $bounds")
                setReading(label, "supported") { it.copy(steeringAngle = Reading.Waiting) }
                call(label, { s, _ -> s }) {
                    wheel.getCurrentSteeringWheelAngle({ v -> put(v) }, { e -> logFail(label, e) })
                }
                val listener = object : SteeringWheelAngleListener {
                    override fun onSteeringWheelAngleUpdated(angle: Float?) = put(angle)
                    override fun onFailed(e: Exception) = logFail(label, e)
                }
                call(label, { s, _ -> s }) {
                    wheel.registerSteeringWheelAngle(listener)
                    addCleanup { wheel.unregisterSteeringWheelAngle(listener) }
                }
            }, { e -> fail(label, e) { s, r -> s.copy(steeringAngle = r) } })
        }
    }

    // ---------- Brake pedal (pressed or not) ----------

    private fun startBrake() {
        val brake = vehicle.getBrake()
        val label = "브레이크"
        val put: (Boolean?) -> Unit = { v -> setReading(label, v?.toString() ?: "null") { it.copy(brakePressed = valueOrNull(v)) } }
        call(label, { s, r -> s.copy(brakePressed = r) }) {
            brake.checkBrakePedalStatusCapabilities({ config ->
                if (!checkReadable(label, config) { s, r -> s.copy(brakePressed = r) }) return@checkBrakePedalStatusCapabilities
                setReading(label, "supported") { it.copy(brakePressed = Reading.Waiting) }
                call(label, { s, _ -> s }) { brake.getCurrentBrakePedalStatus({ v -> put(v) }, { e -> logFail(label, e) }) }
                val listener = object : BrakePedalStatusListener {
                    override fun onBrakePedalStatusUpdated(status: Boolean?) = put(status)
                    override fun onFailed(e: Exception) = logFail(label, e)
                }
                call(label, { s, _ -> s }) {
                    brake.registerBrakePedalStatus(listener)
                    addCleanup { brake.unregisterBrakePedalStatus(listener) }
                }
            }, { e -> fail(label, e) { s, r -> s.copy(brakePressed = r) } })
        }
    }

    // ---------- Defroster ----------

    private fun startDefroster() {
        val hvac = vehicle.getHVAC()
        call("성에제거", { s, r -> s.copy(defrosterSupport = r) }) {
            hvac.checkHvacDefrosterCapabilities({ config, areas ->
                if (!checkReadable("성에제거", config) { s, r -> s.copy(defrosterSupport = r) }) return@checkHvacDefrosterCapabilities
                Log.i(TAG, "성에제거 지원 창: ${areas.map { it.label() }}")
                areas.forEach { area ->
                    val key = area.label()
                    val label = "성에제거[$key]"
                    val put: (Reading<Boolean>) -> Unit = { r ->
                        setReading(label, logText(r)) { it.copy(defroster = it.defroster + (key to r)) }
                    }
                    put(Reading.Waiting)
                    call(label, { s, _ -> s }) {
                        hvac.getHvacDefrosterState(area, { v -> put(valueOrNull(v)) }, { e -> logFail(label, e) })
                    }
                    val listener = object : HvacDefrosterStateListener {
                        override fun onHvacDefrosterStateUpdated(state: Boolean?) = put(valueOrNull(state))
                        override fun onFailed(e: Exception) = logFail(label, e)
                    }
                    call(label, { s, _ -> s }) {
                        hvac.registerHvacDefrosterState(area, listener)
                        addCleanup { hvac.unregisterHvacDefrosterState(area, listener) }
                    }
                }
            }, { e -> fail("성에제거", e) { s, r -> s.copy(defrosterSupport = r) } })
        }
    }

    // ---------- Common ----------

    /** Checks whether the value is readable; marks it Unsupported if not. */
    private fun checkReadable(
        label: String,
        config: CapabilityConfig?,
        set: (VehicleState, Reading<Nothing>) -> VehicleState,
    ): Boolean {
        val access = config?.accessType
        Log.i(TAG, "$label capability: $access")
        val readable = access == AccessType.READ || access == AccessType.READ_WRITE
        if (readable) {
            _state.update { set(it, Reading.Waiting) }
        } else {
            setReading(label, "unsupported($access)") { set(it, Reading.Unsupported("accessType=$access")) }
        }
        return readable
    }

    /** Updates the state, logging only when the value changes. */
    private fun setReading(label: String, logValue: String, transform: (VehicleState) -> VehicleState) {
        val previous = lastLogged.put(label, logValue)
        if (previous != logValue) {
            Log.i(TAG, "[$owner] $label: ${previous ?: "-"} -> $logValue")
        }
        _state.update(transform)
    }

    private inline fun call(
        label: String,
        noinline set: (VehicleState, Reading<Nothing>) -> VehicleState,
        block: () -> Unit,
    ) {
        try {
            block()
        } catch (e: Exception) {
            fail(label, e, set)
        }
    }

    private fun fail(label: String, e: Exception, set: (VehicleState, Reading<Nothing>) -> VehicleState) {
        Log.w(TAG, "$label 실패: ${e.javaClass.simpleName}: ${e.message}")
        val reading = if (e is SecurityException) {
            Reading.Unsupported("권한 없음")
        } else {
            Reading.Error("${e.javaClass.simpleName}: ${e.message}")
        }
        setReading(label, "fail(${e.javaClass.simpleName})") { set(it, reading) }
    }

    private fun logFail(label: String, e: Exception) {
        Log.w(TAG, "$label 오류: ${e.javaClass.simpleName}: ${e.message}")
    }

    private fun addCleanup(action: () -> Unit) {
        synchronized(cleanups) { cleanups.add(action) }
    }

    private fun <T> valueOrNull(v: T?): Reading<T> =
        if (v == null) Reading.Error("값 없음(null)") else Reading.Value(v, System.currentTimeMillis())

    private fun logText(r: Reading<*>): String = when (r) {
        is Reading.Value<*> -> r.value.toString()
        Reading.Waiting -> "waiting"
        Reading.Checking -> "checking"
        is Reading.Unsupported -> "unsupported"
        is Reading.Error -> "error"
    }

    private fun SeatArea.label(): String = values.joinToString("+") { it.name }
    private fun WindowArea.label(): String = values.joinToString("+") { it.name }

    companion object {
        const val TAG = "PleosVillage"
        // The areaId of a global property is 0.
        private const val GLOBAL_AREA = 0
    }
}
