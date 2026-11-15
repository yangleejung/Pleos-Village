package com.grad.pleosvillage.ui.arrival

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grad.pleosvillage.R
import com.grad.pleosvillage.data.FakeRepository
import com.grad.pleosvillage.ui.art.ResidentArt
import com.grad.pleosvillage.ui.art.ResidentKind
import com.grad.pleosvillage.ui.art.SEllipse
import com.grad.pleosvillage.ui.art.SvgEl
import com.grad.pleosvillage.ui.art.VIcon
import com.grad.pleosvillage.ui.art.bub
import com.grad.pleosvillage.ui.art.cloud
import com.grad.pleosvillage.ui.art.drawEls
import com.grad.pleosvillage.ui.art.drop
import com.grad.pleosvillage.ui.art.flower
import com.grad.pleosvillage.ui.art.heart
import com.grad.pleosvillage.ui.art.leaf
import com.grad.pleosvillage.ui.art.sparkle
import com.grad.pleosvillage.ui.art.star5
import com.grad.pleosvillage.ui.design.Art
import com.grad.pleosvillage.ui.design.Brand
import com.grad.pleosvillage.ui.design.ButtonTouchInset
import com.grad.pleosvillage.ui.design.DesignScaled
import com.grad.pleosvillage.ui.design.Dimens
import com.grad.pleosvillage.ui.design.Jua
import com.grad.pleosvillage.ui.design.LocalDrivingRestricted
import com.grad.pleosvillage.ui.design.LocalVillageColors
import com.grad.pleosvillage.ui.design.LocalWidthClass
import com.grad.pleosvillage.ui.design.ScoreBar
import com.grad.pleosvillage.ui.design.StatCard
import com.grad.pleosvillage.ui.design.Ui
import com.grad.pleosvillage.ui.design.VButton
import com.grad.pleosvillage.ui.design.WidthClass
import com.grad.pleosvillage.ui.design.bodyStyle
import com.grad.pleosvillage.ui.design.juaStyle
import com.grad.pleosvillage.ui.model.ArrivalState
import com.grad.pleosvillage.ui.model.ArrivalUi
import com.grad.pleosvillage.ui.model.ResidentUi
import com.grad.pleosvillage.ui.model.eunNeun
import com.grad.pleosvillage.ui.model.iGa
import com.grad.pleosvillage.ui.theme.PleosVillageTheme
import kotlinx.coroutines.launch

/*
 * Arrival summary. UI_SPEC 4.1. Mockup screen 1 (v4).
 * Left: art panel for the met resident. Right: title, safety score + 80-point bar, trip values, missed residents, buttons.
 * Animation (2.5 s max): score bar 0 → score (0.8 s) → resident pops up with 1–2 sparkles (0.4 s) → points number (0.4 s).
 * Tapping the screen skips straight to the final state. With driving restriction on, the final state is shown without animation.
 */

private const val BAR_MS = 800
private const val POP_MS = 400
private const val POINTS_MS = 400

/** Animation progress. Every value goes 0→1. */
private class ArrivalAnim {
    val bar = Animatable(0f)
    val pop = Animatable(0f)
    val points = Animatable(0f)
    suspend fun snapToEnd() { bar.snapTo(1f); pop.snapTo(1f); points.snapTo(1f) }
}

@Composable
fun ArrivalScreen(
    ui: ArrivalUi,
    onClose: () -> Unit,
    /** If null, [마을로 데려가기] is hidden (trip detail in records). */
    onGoVillage: (() -> Unit)?,
    modifier: Modifier = Modifier,
    /** If false, shows the final state without animation (trip detail, UI_SPEC 4.6). */
    animate: Boolean = true,
) {
    val restricted = LocalDrivingRestricted.current
    val anim = remember(ui.id) { ArrivalAnim() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(ui.id, restricted) {
        if (restricted || !animate) { anim.snapToEnd(); return@LaunchedEffect }
        anim.bar.animateTo(1f, tween(BAR_MS, easing = LinearOutSlowInEasing))
        anim.pop.animateTo(1f, tween(POP_MS, easing = EaseOutBack))
        anim.points.animateTo(1f, tween(POINTS_MS))
    }
    val skip = Modifier.clickable(remember { MutableInteractionSource() }, indication = null) {
        scope.launch { anim.snapToEnd() }
    }
    val c = LocalVillageColors.current
    Box(modifier.fillMaxSize().background(c.bg).then(skip)) {
        if (LocalWidthClass.current == WidthClass.Wide) WideArrival(ui, anim, onClose, onGoVillage)
        else NarrowArrival(ui, anim, onClose, onGoVillage)
    }
}

/* ---------------- Wide layout (mockup screen 1) ---------------- */

@Composable
private fun WideArrival(ui: ArrivalUi, anim: ArrivalAnim, onClose: () -> Unit, onGoVillage: (() -> Unit)?) {
    Box(Modifier.fillMaxSize()) {
        ArtPanel(
            ui, anim.pop.value, blob = 320.dp, blobTop = 142.dp,
            modifier = Modifier.padding(start = 32.dp, top = 32.dp, bottom = 32.dp).width(520.dp).fillMaxHeight(),
        )
        Column(Modifier.padding(start = 598.dp, end = 48.dp, top = 44.dp, bottom = 38.dp - ButtonTouchInset).fillMaxSize()) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Summary(ui, anim, titleSize = 40)
            }
            BottomActions(ui, onClose, onGoVillage)
        }
    }
}

/* ---------------- Narrow layout: top to bottom art → title/score → values → buttons (pinned to bottom) ---------------- */

@Composable
private fun NarrowArrival(ui: ArrivalUi, anim: ArrivalAnim, onClose: () -> Unit, onGoVillage: (() -> Unit)?) {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 20.dp)) {
            ArtPanel(ui, anim.pop.value, blob = 190.dp, blobTop = 66.dp, modifier = Modifier.fillMaxWidth().height(350.dp))
            Spacer(Modifier.height(20.dp))
            Summary(ui, anim, titleSize = 30)
        }
        BottomActions(ui, onClose, onGoVillage, Modifier.padding(horizontal = 20.dp, vertical = 14.dp - ButtonTouchInset))
    }
}

/* ---------------- Left art panel ---------------- */

/** Size reference for the art inside the panel. Mockup panel 516×680, resident 320 (top 142). */
@Composable
private fun ArtPanel(ui: ArrivalUi, pop: Float, blob: Dp, blobTop: Dp, modifier: Modifier) {
    val met = ui.met.firstOrNull()
    val shown = met ?: ui.hint
    val kind = shown?.kind ?: ResidentKind.Rain
    val measurer = rememberTextMeasurer()
    val prop = met?.let { propCaption(it.kind) }
    val shape = RoundedCornerShape(18.dp)
    Box(modifier.clip(shape).background(Ui.Panel).border(Dimens.Outline, Ui.Ol, shape)) {
        // Decoration: small items matching the resident kind + clouds + sparkles + grass ellipse and flowers (mockups panelDeco)
        Canvas(Modifier.fillMaxSize()) {
            val lw = Dimens.Outline.toPx()
            val u = 1.dp.toPx()
            val b = blob.toPx() / u
            val top = blobTop.toPx() / u
            drawEls(panelDeco(kind, met != null, size.width / u, size.height / u, b, top).let { els -> els.map { scaleEl(it, u) } }, 1f, lw)
        }
        // Resident: appears with a bouncy pop
        val popScale = if (met != null) 0.6f + 0.4f * pop else 1f
        val lift = if (met != null) (1f - pop.coerceIn(0f, 1f)) * 40f else 0f
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val left = (maxWidth - blob) / 2
            ResidentArt(
                kind, blob,
                Modifier.offset(x = left, y = blobTop + lift.dp)
                    .graphicsLayer { scaleX = popScale; scaleY = popScale; alpha = if (met != null) pop.coerceIn(0f, 1f) else 1f; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.9f) },
                silhouette = met == null,
                line = if (met != null) Dimens.BigArtOutline else Dimens.Outline,
            )
            // Prop caption arrow (mockups: the arrow tip points beside the umbrella)
            if (prop != null) {
                val propText = stringResource(prop)
                Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = pop.coerceIn(0f, 1f) }) {
                    val k = blob.toPx() / 320f
                    val ox = left.toPx() ; val oy = blobTop.toPx()
                    fun x(v: Float) = ox + (v - 98f) * k
                    fun y(v: Float) = oy + (v - 142f) * k
                    val arrow = PathParser().parsePathString(
                        "M${x(456f)} ${y(112f)} C${x(474f)} ${y(136f)} ${x(468f)} ${y(172f)} ${x(428f)} ${y(196f)} " +
                            "M${x(433.5f)} ${y(187.6f)} L${x(428f)} ${y(196f)} L${x(438f)} ${y(196.6f)}",
                    ).toPath()
                    drawPath(arrow, Ui.Ol, style = Stroke(Dimens.Outline.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                    val t = measurer.measure(propText, juaStyle((18f * maxOf(k, 1.dp.toPx() * 0.8f) / (density * fontScale)).sp).copy(color = Ui.Ink))
                    val right = minOf(x(484f), size.width - 16.dp.toPx())
                    drawText(t, topLeft = Offset(right - t.size.width, y(98f) - t.firstBaseline))
                }
            }
            // 1–2 sparkles (only when met, after the pop)
            if (met != null && pop > 0.6f) {
                Canvas(Modifier.fillMaxSize()) {
                    val k = blob.toPx() / 320f
                    val ox = left.toPx(); val oy = blobTop.toPx()
                    val a = ((pop - 0.6f) / 0.4f).coerceIn(0f, 1f)
                    drawEls(
                        listOf(
                            scaleEl(sparkle(ox / k + 10f, oy / k + 40f, 10f, Art.Butter), k),
                            scaleEl(sparkle(ox / k + 300f, oy / k + 250f, 7f), k),
                        ),
                        1f, Dimens.Outline.toPx(), a,
                    )
                }
            }
        }
        Brand(Modifier.padding(start = 38.dp, top = 34.dp), color = Ui.Ink2)
        // Bottom: number·odds·name / newly met·date
        Row(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = 38.dp, end = 38.dp, bottom = 30.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(Modifier.weight(1f)) {
                if (shown != null && shown.probabilityPct != null) {
                    Text(stringResource(R.string.panel_meta, shown.no, shown.probabilityPct), style = bodyStyle(14.sp), color = Ui.Ink2)
                }
                Text(shown?.name ?: stringResource(R.string.panel_unknown), style = juaStyle(32.sp), color = Ui.Ink, modifier = Modifier.padding(top = 4.dp))
            }
            Text(
                if (met != null) stringResource(R.string.met_new) + "\n" + ui.dateShort else stringResource(R.string.panel_next),
                style = bodyStyle(14.sp), color = Ui.Ink2, textAlign = TextAlign.End,
            )
        }
    }
}

private fun propCaption(kind: ResidentKind): Int? = when (kind) {
    ResidentKind.Rain -> R.string.prop_rain
    ResidentKind.Night -> R.string.prop_night
    ResidentKind.Family -> R.string.prop_family
    ResidentKind.Winter -> R.string.prop_winter
    ResidentKind.LongTrip -> R.string.prop_long
    ResidentKind.Charge -> R.string.prop_charge
    else -> null
}

/**
 * Art panel decoration (coordinates in dp). mockups panelDeco(): 10 raindrops, 2 clouds, sparkles, a star, bubbles, grass ellipse and flowers.
 * The raindrop spots hold small items matching the resident kind (rain = raindrop, night = star, family = heart, winter = sparkle).
 */
private fun panelDeco(kind: ResidentKind, met: Boolean, w: Float, h: Float, blob: Float, blobTop: Float): List<SvgEl> {
    val sx = w / 516f; val sy = h / 680f
    val out = mutableListOf<SvgEl>()
    val spots = listOf(56 to 124, 474 to 262, 38 to 300, 84 to 414, 470 to 410, 150 to 92, 34 to 524, 482 to 556, 118 to 240, 210 to 560)
    // In a short panel (narrow layout), skip spots that overlap the logo on top and the name row at the bottom.
    val short = h < 500f
    spots.forEachIndexed { q, (x0, y0) ->
        if (short && (y0 < 200 || y0 > 480)) return@forEachIndexed
        val x = x0 * sx; val y = y0 * sy
        val r = if (q % 3 != 0) 5.5f else 6.5f
        out += when (kind) {
            ResidentKind.Night -> star5(x, y - 4, r)
            ResidentKind.Family -> heart(x, y - 2, r / 6.5f, Art.Pink)
            ResidentKind.Winter -> sparkle(x, y - 4, r + 1, Art.White)
            ResidentKind.Rain -> drop(x, y, r)
            else -> sparkle(x, y - 4, r, Art.Butter)
        }
    }
    out += cloud(24 * sx, 204 * sy, .72f)
    out += cloud(404 * sx, 330 * sy, .62f)
    out += sparkle(452 * sx, 498 * sy, 8f)
    out += sparkle(112 * sx, 176 * sy, 6.5f, Art.Butter)
    out += star5(474 * sx, 204 * sy, 6f)
    if (!short) {
        out += bub(60 * sx, 476 * sy, 4f)
        out += bub(72 * sx, 462 * sy, 2.6f)
    }
    if (met) {
        // Grass ellipse and flowers (relative to the resident's feet; mockup: center (258, 458) when the resident is 320)
        val k = blob / 320f
        val gx = w / 2; val gy = blobTop + 316f * k
        fun px(v: Float) = gx + (v - 258f) * k
        fun py(v: Float) = gy + (v - 458f) * k
        out += SEllipse(gx, gy, 176f * k, 32f * k, Art.Sage)
        out += leaf(px(102f), py(462f), 20 * k, -152f); out += leaf(px(146f), py(470f), 18 * k, -38f)
        out += flower(px(120f), py(450f), 13 * k); out += flower(px(156f), py(462f), 10 * k, Art.Pink, Art.Butter)
        out += leaf(px(368f), py(468f), 19 * k, -142f); out += leaf(px(414f), py(462f), 18 * k, -28f)
        out += flower(px(386f), py(450f), 12.5f * k); out += flower(px(414f), py(462f), 9 * k)
        out += leaf(px(326f), py(476f), 12 * k, -125f, Art.Sage2); out += leaf(px(328f), py(476f), 12 * k, -55f, Art.Sage2)
    }
    return out
}

/** Scales a shape in dp coordinates to px (stroke width unchanged). */
private fun scaleEl(el: SvgEl, u: Float): SvgEl = com.grad.pleosvillage.ui.art.SGroup(listOf(el), s = u)

/* ---------------- Right: title, score, values, missed residents ---------------- */

@Composable
private fun Summary(ui: ArrivalUi, anim: ArrivalAnim, titleSize: Int) {
    val c = LocalVillageColors.current
    val met = ui.met.firstOrNull()
    Text(stringResource(R.string.arrival_at, ui.arrivedAt), style = bodyStyle(15.sp), color = c.fg2)
    val title = when (ui.state) {
        ArrivalState.Results, ArrivalState.LaterStop ->
            if (met != null) stringResource(R.string.arrival_title_met, met.envShort, met.name.iGa()) else stringResource(R.string.arrival_title_missed)
        ArrivalState.BelowPass -> stringResource(R.string.arrival_title_below, ui.score)
        ArrivalState.ShortTrip -> stringResource(R.string.arrival_title_short)
        ArrivalState.DailyLimit -> stringResource(R.string.arrival_title_limit)
        ArrivalState.NoEnvironment -> stringResource(R.string.arrival_title_none)
    }
    Text(title, style = juaStyle(titleSize.sp), color = c.fg, modifier = Modifier.padding(top = 12.dp))

    // Safety score
    Row(Modifier.padding(top = 18.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("${(ui.score * anim.bar.value).toInt()}", style = juaStyle(74.sp).copy(lineHeight = 74.sp), color = c.fg)
        Column(Modifier.padding(bottom = 10.dp)) {
            Text(stringResource(R.string.score_label), style = bodyStyle(15.sp, FontWeight.Bold), color = c.fg)
            val lost = -ui.deductions.sumOf { it.points }
            Text(
                if (lost == 0) stringResource(R.string.score_clean) else stringResource(R.string.score_minus, lost),
                style = bodyStyle(14.sp), color = if (lost == 0) c.fg2 else c.warn,
            )
        }
    }
    ScoreBar(ui.score, anim.bar.value, Modifier.padding(top = 12.dp))

    // Trip values (deductions, if any, on top)
    val rows = mutableListOf<Pair<String, String>>()
    val colors = mutableMapOf<Int, Color>()
    ui.deductions.forEach { d ->
        colors[rows.size] = Ui.CardWarn
        rows += stringResource(R.string.row_deduction, d.label, d.count) to "${d.points}"
    }
    rows += stringResource(R.string.row_duration) to ui.duration
    rows += stringResource(R.string.row_env) to (ui.environments.joinToString(" · ").ifEmpty { stringResource(R.string.env_none) })
    rows += stringResource(R.string.row_people) to stringResource(R.string.people_count, ui.passengers)
    if (ui.points > 0) rows += stringResource(R.string.row_points) to stringResource(R.string.points_plus, (ui.points * anim.points.value).toInt())
    StatCard(rows, Modifier.fillMaxWidth().padding(top = 4.dp), valueColors = colors)
    ui.unchecked.forEach {
        Text(stringResource(R.string.unchecked_item, it), style = bodyStyle(13.sp), color = c.fg2, modifier = Modifier.padding(top = 6.dp))
    }

    // Missed residents, residents also met, status note
    Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        when (ui.state) {
            ArrivalState.Results, ArrivalState.LaterStop -> {
                ui.met.drop(1).forEach { r ->
                    ResidentLine(r, silhouette = false, strong = stringResource(R.string.met_also, r.name), sub = stringResource(R.string.panel_meta, r.no, r.probabilityPct ?: 0))
                }
                ui.missed.forEach { r ->
                    val left = r.remainingToCeiling ?: 0
                    ResidentLine(
                        r, silhouette = true, strong = stringResource(R.string.missed_title, r.name.eunNeun()),
                        sub = if (left <= 1) stringResource(R.string.missed_next) else stringResource(R.string.missed_more, left, r.probabilityPct ?: 0),
                    )
                }
            }
            ArrivalState.BelowPass -> Note(stringResource(R.string.state_below_sub))
            ArrivalState.ShortTrip -> Note(stringResource(R.string.state_short_sub))
            ArrivalState.DailyLimit -> Note(stringResource(R.string.state_limit_sub))
            ArrivalState.NoEnvironment -> ui.hint?.let { h ->
                ResidentLine(h, silhouette = true, strong = h.name, sub = stringResource(R.string.state_none_hint, h.name.eunNeun(), h.environment))
            }
        }
    }
}

@Composable
private fun ResidentLine(r: ResidentUi, silhouette: Boolean, strong: String, sub: String) {
    val c = LocalVillageColors.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ResidentArt(r.kind, 46.dp, silhouette = silhouette, silLine = c.olOnBg)
        Column {
            Text(strong, style = bodyStyle(15.sp, FontWeight.Bold), color = c.fg)
            Text(sub, style = bodyStyle(15.sp), color = c.fg2)
        }
    }
}

@Composable
private fun Note(text: String) {
    Text(text, style = bodyStyle(15.sp), color = LocalVillageColors.current.fg2)
}

@Composable
private fun BottomActions(ui: ArrivalUi, onClose: () -> Unit, onGoVillage: (() -> Unit)?, modifier: Modifier = Modifier) {
    val c = LocalVillageColors.current
    val wide = LocalWidthClass.current == WidthClass.Wide
    Column(modifier) {
        if (!wide) Text(stringResource(R.string.chances_left, ui.chancesLeft), style = bodyStyle(14.sp), color = c.fg2, modifier = Modifier.padding(bottom = 8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (wide) {
                Text(stringResource(R.string.chances_left, ui.chancesLeft), style = bodyStyle(14.sp), color = c.fg2)
                Spacer(Modifier.weight(1f))
                VButton(stringResource(R.string.close), onClose, primary = onGoVillage == null)
                if (onGoVillage != null) VButton(stringResource(R.string.go_village), onGoVillage, icon = VIcon.Arrow, iconAfter = true)
            } else {
                VButton(stringResource(R.string.close), onClose, Modifier.weight(1f), primary = onGoVillage == null)
                if (onGoVillage != null) VButton(stringResource(R.string.go_village), onGoVillage, Modifier.weight(1.8f), icon = VIcon.Arrow, iconAfter = true)
            }
        }
    }
}

/* ---------------- Previews ---------------- */

@Preview(widthDp = 1152, heightDp = 748)
@Composable
private fun ArrivalWidePreview() = PleosVillageTheme(night = false) {
    DesignScaled { ArrivalScreen(FakeRepository.arrivals.getValue("met"), {}, {}) }
}

@Preview(widthDp = 1152, heightDp = 748)
@Composable
private fun ArrivalWideNightPreview() = PleosVillageTheme(night = true) {
    DesignScaled { ArrivalScreen(FakeRepository.arrivals.getValue("met"), {}, {}) }
}

@Preview(widthDp = 1152, heightDp = 748)
@Composable
private fun ArrivalBelowPreview() = PleosVillageTheme(night = false) {
    DesignScaled { ArrivalScreen(FakeRepository.arrivals.getValue("below"), {}, {}) }
}

@Preview(widthDp = 571, heightDp = 748)
@Composable
private fun ArrivalNarrowPreview() = PleosVillageTheme(night = false) {
    DesignScaled { ArrivalScreen(FakeRepository.arrivals.getValue("met"), {}, {}) }
}
