package com.grad.pleosvillage.ui.design

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Wide layout (landscape) / narrow layout (portrait). UI_SPEC §1: 840dp width threshold. */
enum class WidthClass { Narrow, Wide }

val LocalWidthClass = staticCompositionLocalOf { WidthClass.Wide }

/** True when driving restrictions are on. All animations stop on this one value. UI_SPEC §6. */
val LocalDrivingRestricted = staticCompositionLocalOf { false }

private val WideDesign = Pair(1152.dp, 748.dp)
private val NarrowDesign = Pair(571.dp, 748.dp)
private val WideThreshold = 840.dp
private const val MAX_SCALE = 1.6f

/**
 * Raises density so the screen is filled at the mockup's proportions (app area 1152×748, narrow layout 571×748).
 * Mockup values can be used directly as dp/sp. If the screen is smaller than the mockup, it is not scaled down (keeps touch targets).
 *
 * @param forceWidth Debug only: forces a smaller width to view the narrow layout on the emulator.
 */
@Composable
fun DesignScaled(
    modifier: Modifier = Modifier,
    forceWidth: Dp? = null,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val w = forceWidth ?: maxWidth
        val h = maxHeight
        val widthClass = if (w >= WideThreshold) WidthClass.Wide else WidthClass.Narrow
        val (dw, dh) = if (widthClass == WidthClass.Wide) WideDesign else NarrowDesign
        val scale = minOf(w / dw, h / dh).coerceIn(1f, MAX_SCALE)
        val base = LocalDensity.current
        CompositionLocalProvider(
            LocalDensity provides Density(base.density * scale, base.fontScale),
            LocalWidthClass provides widthClass,
        ) { content() }
    }
}

/**
 * A Dialog is a separate window, so the density raised by [DesignScaled] does not carry over (text and cards shrink).
 * So the caller's density and layout type are provided again inside the dialog.
 */
@Composable
fun ScaledDialog(
    onDismissRequest: () -> Unit,
    properties: androidx.compose.ui.window.DialogProperties = androidx.compose.ui.window.DialogProperties(),
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val widthClass = LocalWidthClass.current
    androidx.compose.ui.window.Dialog(onDismissRequest, properties) {
        CompositionLocalProvider(LocalDensity provides density, LocalWidthClass provides widthClass) { content() }
    }
}
