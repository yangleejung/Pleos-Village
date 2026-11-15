package com.grad.pleosvillage.ui.village

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grad.pleosvillage.R
import com.grad.pleosvillage.ui.art.VIcon
import com.grad.pleosvillage.ui.art.VillageIcon
import com.grad.pleosvillage.ui.design.LabelValue
import com.grad.pleosvillage.ui.design.Ui
import com.grad.pleosvillage.ui.design.VCard
import com.grad.pleosvillage.ui.design.bodyStyle
import com.grad.pleosvillage.ui.design.dashedStart
import com.grad.pleosvillage.ui.design.juaStyle
import com.grad.pleosvillage.ui.design.numberStyle
import com.grad.pleosvillage.vehicle.NaviState
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import com.grad.pleosvillage.ui.art.VillageCanvas
import com.grad.pleosvillage.ui.design.LocalVillageColors
import com.grad.pleosvillage.ui.design.LocalDrivingRestricted
import com.grad.pleosvillage.ui.design.LocalWidthClass
import com.grad.pleosvillage.ui.design.WidthClass
import com.grad.pleosvillage.ui.model.VillageUi

/*
 * Idle mode. If the screen is not touched for IDLE_TIMEOUT_MS on a main tab (village, dex, records, settings),
 * all menus, cards and buttons are hidden and only the village art fills the screen. Touching returns to the previous screen.
 * Top left shows the current time and date; while the navi is guiding, bottom left shows remaining distance, ETA and time left.
 * Resident walking follows the same rule as the village screen (only while stopped and visible, 30 fps max).
 */

const val IDLE_TIMEOUT_MS = 10_000L

/** Time of the last touch, and the number of windows (sheets, confirm dialogs) blocking idle mode. */
@Stable
class IdleController {
    var lastTouch by mutableLongStateOf(SystemClock.uptimeMillis())
        private set
    /** While greater than 0, idle mode is not entered. */
    var blockers by mutableIntStateOf(0)
        private set

    fun touched() { lastTouch = SystemClock.uptimeMillis() }
    fun block() { blockers++ }
    fun unblock() { blockers--; touched() }
}

val LocalIdleController = staticCompositionLocalOf<IdleController?> { null }

/**
 * Prevents idle mode while this screen is shown.
 * Sheets and confirm dialogs are separate windows the app screen's touch watcher can't see, so call this inside them.
 */
@Composable
fun KeepAwake() {
    val idle = LocalIdleController.current ?: return
    DisposableEffect(idle) {
        idle.block()
        onDispose { idle.unblock() }
    }
}

/** Watches touches across the whole screen (without consuming them). */
fun Modifier.watchTouches(idle: IdleController): Modifier = pointerInput(idle) {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent(PointerEventPass.Initial)
            idle.touched()
        }
    }
}

/** Idle screen: village art only. Tapping anywhere calls [onWake] (the touch is not passed to the screen below). */
@Composable
fun IdleVillage(ui: VillageUi, onWake: () -> Unit, navi: NaviState? = null) {
    val wide = LocalWidthClass.current == WidthClass.Wide
    val base = if (wide) ui.spec else ui.specNarrow
    // Residents keep walking while driving (decided 2026-11-14). While driving, touch cannot wake the screen.
    val driving = LocalDrivingRestricted.current
    val time = walkClock(whileDriving = true)
    val spec = base.copy(extras = walking(base.extras, time), bubbles = emptyList())
    Box(
        Modifier.fillMaxSize().background(LocalVillageColors.current.bg)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val e = awaitPointerEvent(PointerEventPass.Initial)
                        e.changes.forEach { it.consume() }
                        if (e.changes.any { it.pressed } && !driving) onWake()
                    }
                }
            },
    ) {
        VillageCanvas(spec, Modifier.fillMaxSize())
        IdleClock(Modifier.align(Alignment.TopStart).padding(start = if (wide) 44.dp else 24.dp, top = if (wide) 30.dp else 20.dp))
        navi?.let { NaviCard(it, Modifier.align(Alignment.BottomStart).padding(start = if (wide) 44.dp else 20.dp, bottom = if (wide) 38.dp else 20.dp)) }
    }
}

/** Current time (updated whenever the minute changes). */
@Composable
private fun rememberNow(): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(60_000L - now % 60_000L + 50)
        }
    }
    return now
}

private fun timeText(millis: Long) = SimpleDateFormat("a h:mm", Locale.KOREA).format(Date(millis))
private fun dateText(millis: Long) = SimpleDateFormat("M월 d일 EEEE", Locale.KOREA).format(Date(millis))

/** Top-left clock: large time + date. Text sits on the background, so it uses the fg color. */
@Composable
private fun IdleClock(modifier: Modifier) {
    val c = LocalVillageColors.current
    val now = rememberNow()
    Column(modifier) {
        Text(timeText(now), style = numberStyle(56.sp), color = c.fg)
        Text(dateText(now), style = bodyStyle(18.sp, FontWeight.SemiBold), color = c.fg2, modifier = Modifier.padding(top = 2.dp))
    }
}

/** Bottom-left route guidance card: destination, remaining distance, ETA, time left. */
@Composable
private fun NaviCard(n: NaviState, modifier: Modifier) {
    VCard(modifier, contentPadding = PaddingValues(horizontal = 22.dp, vertical = 16.dp)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VillageIcon(VIcon.Arrow, 20.dp, Ui.Ink)
                Text(n.destination.ifEmpty { stringResource(R.string.idle_navi_destination) }, style = juaStyle(21.sp), color = Ui.Ink, maxLines = 1)
            }
            Row(Modifier.padding(top = 10.dp)) {
                LabelValue(stringResource(R.string.idle_navi_distance), n.remainMeters?.let { formatDistance(it) } ?: "-", Modifier.padding(end = 22.dp))
                LabelValue(stringResource(R.string.idle_navi_eta), n.etaMillis?.let { timeText(it) } ?: "-", Modifier.dashedStart(Ui.CardLine).padding(horizontal = 22.dp))
                LabelValue(stringResource(R.string.idle_navi_left), n.remainSeconds?.let { formatDuration(it) } ?: "-", Modifier.dashedStart(Ui.CardLine).padding(start = 22.dp))
            }
        }
    }
}

/** 1 km or more: km with one decimal; below that: m. */
private fun formatDistance(m: Int) = if (m >= 1000) String.format(Locale.KOREA, "%.1fkm", m / 1000f) else "${m}m"

/** 1 hour or more: "1시간 5분"; below that: "28분" (under 1 minute: "1분"). */
private fun formatDuration(sec: Int): String {
    val min = maxOf(1, (sec + 59) / 60)
    return if (min >= 60) "${min / 60}시간 ${min % 60}분" else "${min}분"
}
