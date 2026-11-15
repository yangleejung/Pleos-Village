package com.grad.pleosvillage.vehicle

/** Current state of one vehicle value. Progresses: capability check → read → change notifications. */
sealed interface Reading<out T> {
    /** Still checking whether it is supported. */
    data object Checking : Reading<Nothing>

    /** This vehicle (or emulator) does not support it, or read permission is missing. */
    data class Unsupported(val reason: String) : Reading<Nothing>

    /** Supported; waiting for a read result or change notification. */
    data object Waiting : Reading<Nothing>

    data class Value<T>(val value: T, val updatedAtMillis: Long) : Reading<T>

    data class Error(val message: String) : Reading<Nothing>
}

/** Gear. Mapped from the CarPropertyManager GEAR_SELECTION value. */
enum class GearPosition { PARK, REVERSE, NEUTRAL, DRIVE, UNKNOWN, OTHER }

/**
 * Bundle of vehicle values shared by the diagnostics screen and the (next-stage) recording service.
 * Per-area values (seats, windows) are stored as area name → value. Area names are SDK area constant names (e.g. ROW_2_LEFT).
 */
data class VehicleState(
    /** true while driving (platform requires distraction optimization). Based on CarUxRestrictionsManager. */
    val drivingRestricted: Reading<Boolean> = Reading.Checking,
    val gear: Reading<GearPosition> = Reading.Checking,
    val wiper: Map<String, Reading<String>> = emptyMap(),
    val wiperSupport: Reading<Unit> = Reading.Checking,
    val lowBeam: Reading<String> = Reading.Checking,
    val seatOccupancy: Map<String, Reading<Boolean>> = emptyMap(),
    val seatOccupancySupport: Reading<Unit> = Reading.Checking,
    val steeringAngle: Reading<Float> = Reading.Checking,
    val defroster: Map<String, Reading<Boolean>> = emptyMap(),
    val defrosterSupport: Reading<Unit> = Reading.Checking,
    /** Brake pedal pressed (SDK gives only pressed/not, no pedal %). Used to detect harsh braking. */
    val brakePressed: Reading<Boolean> = Reading.Checking,
)
