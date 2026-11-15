package com.grad.pleosvillage.ui.village

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.grad.pleosvillage.R
import com.grad.pleosvillage.data.FakeRepository
import com.grad.pleosvillage.ui.art.Bubble
import com.grad.pleosvillage.ui.art.ObjType
import com.grad.pleosvillage.ui.art.ResidentArt
import com.grad.pleosvillage.ui.art.VIcon
import com.grad.pleosvillage.ui.art.VillageCanvas
import com.grad.pleosvillage.ui.art.VillageSpec
import com.grad.pleosvillage.ui.art.VillageObject
import com.grad.pleosvillage.ui.art.sliceFit
import com.grad.pleosvillage.ui.design.Brand
import com.grad.pleosvillage.ui.design.ButtonTouchInset
import com.grad.pleosvillage.ui.design.DesignScaled
import com.grad.pleosvillage.ui.design.IconCircleButton
import com.grad.pleosvillage.ui.design.LabelValue
import com.grad.pleosvillage.ui.design.LocalDrivingRestricted
import com.grad.pleosvillage.ui.design.LocalVillageColors
import com.grad.pleosvillage.ui.design.LocalWidthClass
import com.grad.pleosvillage.ui.design.ScreenTitle
import com.grad.pleosvillage.ui.design.Ui
import com.grad.pleosvillage.ui.design.VButton
import com.grad.pleosvillage.ui.design.VCard
import com.grad.pleosvillage.ui.design.WidthClass
import com.grad.pleosvillage.ui.design.bodyStyle
import com.grad.pleosvillage.ui.design.dashedStart
import com.grad.pleosvillage.ui.design.juaStyle
import com.grad.pleosvillage.ui.model.ResidentUi
import com.grad.pleosvillage.ui.model.VillageUi
import com.grad.pleosvillage.ui.nav.AdaptiveScaffold
import com.grad.pleosvillage.ui.nav.BottomBarHeight
import com.grad.pleosvillage.ui.nav.Tab
import com.grad.pleosvillage.ui.theme.PleosVillageTheme
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sin

/*
 * Village (home). UI_SPEC 4.2. Mockup screens 2 (wide), 4 (narrow).
 * Drag to pan, pinch or [+][-] to zoom. Tapping a resident shows a bubble and a card below.
 * Residents walk only while stopped and the screen is visible (30 fps max). They stop when driving restriction turns on.
 */

private const val MIN_ZOOM = 0.8f
private const val MAX_ZOOM = 2.2f
private const val FRAME_MS = 33L

@Composable
fun VillageScreen(
    ui: VillageUi,
    onSelectTab: (Tab) -> Unit,
    modifier: Modifier = Modifier,
    onDecorate: () -> Unit = {},
) {
    val wide = LocalWidthClass.current == WidthClass.Wide
    var selectedTile by remember { mutableStateOf(ui.residentsAt.keys.firstOrNull()) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    val time = walkClock()

    AdaptiveScaffold(Tab.Village, onSelectTab, railTop = 229.dp) {
        Box(modifier.fillMaxSize()) {
            val baseSpec = if (wide) ui.spec else ui.specNarrow
            val selected = selectedTile?.let { ui.residentsAt[it] }
            val spec = baseSpec.copy(
                extras = walking(baseSpec.extras, time, selectedTile),
                bubbles = selected?.metDate?.let { d ->
                    val (i, j) = selectedTile!!
                    listOf(Bubble(i, j, stringResource(R.string.met_on, d), baseSpec.bubbleStyle))
                } ?: emptyList(),
            )
            val density = LocalDensity.current
            BoxWithConstraints(Modifier.fillMaxSize().padding(bottom = if (wide) 0.dp else BottomBarHeight)) {
                val cw = with(density) { maxWidth.toPx() }
                val ch = with(density) { maxHeight.toPx() }
                VillageCanvas(
                    spec, Modifier.fillMaxSize()
                        .pointerInput(spec.w) {
                            detectTransformGestures { _, p, z, _ ->
                                zoom = (zoom * z).coerceIn(MIN_ZOOM, MAX_ZOOM)
                                pan += p
                            }
                        }
                        .pointerInput(spec.w, zoom, pan) {
                            detectTapGestures { tap ->
                                val (k, dx, dy) = sliceFit(spec, cw, ch)
                                val x = (tap.x - dx - pan.x) / (k * zoom)
                                val y = (tap.y - dy - pan.y) / (k * zoom)
                                selectedTile = hitResident(spec, ui, x, y) ?: selectedTile
                            }
                        },
                    zoom = zoom, pan = pan,
                )
            }
            val zoomIn = { zoom = (zoom * 1.25f).coerceAtMost(MAX_ZOOM) }
            val zoomOut = { zoom = (zoom / 1.25f).coerceAtLeast(MIN_ZOOM) }
            if (wide) WideOverlay(ui, selected, zoomIn, zoomOut, onDecorate) else NarrowOverlay(ui, selected, zoomIn, zoomOut, onDecorate)
        }
    }
}

/** Tile of the resident at mockup coordinates (x, y). A tap inside the resident art counts as a hit. */
private fun hitResident(spec: VillageSpec, ui: VillageUi, x: Float, y: Float): Pair<Int, Int>? {
    val sz = spec.tw * 0.68f
    fun dist(t: Pair<Int, Int>): Float {
        val p = spec.center(t.first.toFloat(), t.second.toFloat())
        return hypot(x - p.x, y - (p.y - sz * 0.4f))
    }
    return ui.residentsAt.keys.minByOrNull(::dist)?.takeIf { dist(it) < sz * 0.7f }
}

/** Walking clock (seconds). Ticks at 30 fps max only while visible and driving restriction is off. Also used by the idle screen. */
@Composable
internal fun walkClock(whileDriving: Boolean = false): Float {
    val restricted = LocalDrivingRestricted.current
    var resumed by remember { mutableStateOf(true) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, _ -> resumed = owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    var t by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(restricted, resumed) {
        if ((restricted && !whileDriving) || !resumed) return@LaunchedEffect
        val start = withFrameNanos { it } - (t * 1e9).toLong()
        while (true) {
            withFrameNanos { now -> t = (now - start) / 1e9f }
            kotlinx.coroutines.delay(FRAME_MS)
        }
    }
    return t
}

/** Residents shuffle around their spot and bounce. Shared by the village screen and the idle screen. */
internal fun walking(extras: List<VillageObject>, time: Float, selectedTile: Pair<Int, Int>? = null): List<VillageObject> =
    extras.map { o ->
        if (o.type != ObjType.Resident) o else {
            val t = time + (o.i * 1.7f + o.j * 0.9f)
            o.copy(
                selected = (o.i to o.j) == selectedTile,
                di = 0.12f * sin(t * 0.6f),
                lift = 3f * abs(sin(t * PI.toFloat() * 1.2f)),
            )
        }
    }

private fun fmt(n: Int) = NumberFormat.getIntegerInstance(Locale.KOREA).format(n)

/* ---------------- Wide layout (mockup screen 2) ---------------- */

@Composable
private fun WideOverlay(ui: VillageUi, selected: ResidentUi?, zoomIn: () -> Unit, zoomOut: () -> Unit, onDecorate: () -> Unit) {
    val c = LocalVillageColors.current
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.padding(start = 44.dp, top = 34.dp)) {
            Brand()
            ScreenTitle(ui.title, Modifier.padding(top = 10.dp), size = 38.sp)
            Text(ui.subtitle, style = bodyStyle(15.sp), color = c.fg2, modifier = Modifier.padding(top = 4.dp))
        }
        VCard(Modifier.align(Alignment.TopEnd).padding(top = 34.dp, end = 44.dp), contentPadding = PaddingValues(vertical = 12.dp, horizontal = 4.dp)) {
            Row {
                LabelValue(stringResource(R.string.weekly_average), "${ui.weeklyAverage}", Modifier.padding(horizontal = 22.dp), unit = stringResource(R.string.weekly_unit, ui.weeklyDelta))
                LabelValue(stringResource(R.string.points), fmt(ui.points), Modifier.dashedStart(Ui.CardLine).padding(horizontal = 22.dp))
                LabelValue(stringResource(R.string.today_chances), "${ui.chancesLeft}", Modifier.dashedStart(Ui.CardLine).padding(horizontal = 22.dp), unit = stringResource(R.string.of_total, ui.chancesTotal))
            }
        }
        ZoomButtons(zoomIn, zoomOut, Modifier.align(Alignment.CenterEnd).padding(end = 30.dp))
        selected?.let { SelectedCard(it, big = true, modifier = Modifier.align(Alignment.BottomStart).padding(start = 44.dp, bottom = 38.dp).width(446.dp)) }
        DecorateButton(onDecorate, Modifier.align(Alignment.BottomEnd).padding(end = 44.dp, bottom = 38.dp - ButtonTouchInset))
    }
}

/* ---------------- Narrow layout (mockup screen 4) ---------------- */

@Composable
private fun NarrowOverlay(ui: VillageUi, selected: ResidentUi?, zoomIn: () -> Unit, zoomOut: () -> Unit, onDecorate: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(bottom = BottomBarHeight)) {
        Column(Modifier.padding(start = 24.dp, top = 24.dp)) {
            Brand()
            ScreenTitle(ui.title, Modifier.padding(top = 6.dp), size = 28.sp)
        }
        VCard(Modifier.align(Alignment.TopEnd).padding(top = 22.dp, end = 20.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)) {
            Column(horizontalAlignment = Alignment.End) {
                LabelValue(stringResource(R.string.points), fmt(ui.points), valueSize = 19.sp, alignEnd = true)
                LabelValue(stringResource(R.string.today_chance_short), "${ui.chancesLeft}", Modifier.padding(top = 6.dp), unit = stringResource(R.string.of_total, ui.chancesTotal), valueSize = 19.sp, alignEnd = true)
            }
        }
        ZoomButtons(zoomIn, zoomOut, Modifier.align(Alignment.CenterEnd).padding(end = 12.dp))
        Column(Modifier.align(Alignment.BottomCenter).padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 16.dp - ButtonTouchInset), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(14.dp - ButtonTouchInset)) {
            selected?.let { SelectedCard(it, big = false, modifier = Modifier.fillMaxWidth()) }
            DecorateButton(onDecorate, Modifier, height = 52.dp)
        }
    }
}

@Composable
private fun ZoomButtons(zoomIn: () -> Unit, zoomOut: () -> Unit, modifier: Modifier) {
    Column(modifier) {
        IconCircleButton(VIcon.Plus, zoomIn)
        IconCircleButton(VIcon.Minus, zoomOut)
    }
}

@Composable
private fun SelectedCard(r: ResidentUi, big: Boolean, modifier: Modifier) {
    VCard(modifier, contentPadding = if (big) PaddingValues(horizontal = 20.dp, vertical = 16.dp) else PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (big) 16.dp else 12.dp)) {
            ResidentArt(r.kind, if (big) 64.dp else 48.dp)
            Column {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(r.name, style = juaStyle(if (big) 21.sp else 18.sp), color = Ui.Ink)
                    if (big) Text(r.no, style = bodyStyle(13.sp, FontWeight.SemiBold), color = Ui.Ink2, modifier = Modifier.padding(bottom = 3.dp))
                }
                r.metStory?.let {
                    val text = if (big) it else it.substringBefore('\n').trimEnd('.')
                    Text(text, style = bodyStyle(if (big) 15.sp else 13.sp), color = Ui.Ink2, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun DecorateButton(onClick: () -> Unit, modifier: Modifier, height: androidx.compose.ui.unit.Dp = 58.dp) {
    VButton(
        stringResource(R.string.decorate),
        onClick,
        modifier, icon = VIcon.Brush, height = height, fontSize = if (height < 58.dp) 17.sp else 19.sp,
    )
}

/* ---------------- Previews ---------------- */

@Preview(widthDp = 1152, heightDp = 748)
@Composable
private fun VillageWidePreview() = PleosVillageTheme(night = false) {
    DesignScaled { VillageScreen(FakeRepository.village, {}) }
}

@Preview(widthDp = 1152, heightDp = 748)
@Composable
private fun VillageWideNightPreview() = PleosVillageTheme(night = true) {
    DesignScaled { VillageScreen(FakeRepository.village, {}) }
}

@Preview(widthDp = 571, heightDp = 748)
@Composable
private fun VillageNarrowPreview() = PleosVillageTheme(night = false) {
    DesignScaled { VillageScreen(FakeRepository.village, {}) }
}
