package com.grad.pleosvillage.ui.records

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grad.pleosvillage.R
import com.grad.pleosvillage.data.FakeRepository
import com.grad.pleosvillage.ui.art.ResidentArt
import com.grad.pleosvillage.ui.art.VIcon
import com.grad.pleosvillage.ui.art.VillageIcon
import com.grad.pleosvillage.ui.design.Art
import com.grad.pleosvillage.ui.design.Brand
import com.grad.pleosvillage.ui.design.DesignScaled
import com.grad.pleosvillage.ui.design.Dimens
import com.grad.pleosvillage.ui.design.LocalVillageColors
import com.grad.pleosvillage.ui.design.LocalWidthClass
import com.grad.pleosvillage.ui.design.ScreenTitle
import com.grad.pleosvillage.ui.design.TabPill
import com.grad.pleosvillage.ui.design.Ui
import com.grad.pleosvillage.ui.design.VCard
import com.grad.pleosvillage.ui.design.WidthClass
import com.grad.pleosvillage.ui.design.bodyStyle
import com.grad.pleosvillage.ui.design.juaStyle
import com.grad.pleosvillage.ui.design.numberStyle
import com.grad.pleosvillage.ui.model.ArrivalState
import com.grad.pleosvillage.ui.model.DayScore
import com.grad.pleosvillage.ui.model.RecordsUi
import com.grad.pleosvillage.ui.model.TripUi
import com.grad.pleosvillage.ui.model.WeekStat
import com.grad.pleosvillage.ui.nav.AdaptiveScaffold
import com.grad.pleosvillage.ui.nav.BottomBarHeight
import com.grad.pleosvillage.ui.nav.Tab
import com.grad.pleosvillage.ui.theme.PleosVillageTheme

/*
 * Records. UI_SPEC 4.6. Two tabs: this week | trips.
 * This week: 4 cards (average safety score, trips scoring 80+, sharp steering/hard braking, share of 2h+ trips with a rest) vs. last week,
 *            plus 7 per-day safety score bars (80-point line).
 * Trips: trip list. Tapping opens trip detail (that trip's arrival summary without animation).
 * There is no mockup, so this uses the same frame as the dex (logo, title, tabs) and v4 components.
 */

private enum class RecordsTab(val label: Int) { Week(R.string.records_tab_week), Trips(R.string.records_tab_trips) }

@Composable
fun RecordsScreen(ui: RecordsUi, onSelectTab: (Tab) -> Unit, onOpenTrip: (String) -> Unit) {
    val wide = LocalWidthClass.current == WidthClass.Wide
    var tab by rememberSaveable { mutableStateOfTab() }
    AdaptiveScaffold(Tab.Records, onSelectTab, railTop = 189.dp) {
        Box(Modifier.fillMaxSize()) {
            if (wide) Brand(Modifier.padding(start = 44.dp, top = 34.dp))
            Column(
                Modifier.fillMaxSize().padding(
                    start = if (wide) 174.dp else 20.dp, end = if (wide) 44.dp else 20.dp,
                    top = if (wide) 66.dp else 20.dp, bottom = if (wide) 0.dp else BottomBarHeight,
                ),
            ) {
                if (!wide) Brand(Modifier.padding(bottom = 6.dp))
                ScreenTitle(stringResource(R.string.records_title), size = if (wide) 36.sp else 28.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    RecordsTab.entries.forEach { t -> TabPill(stringResource(t.label), t.ordinal == tab, { tab = t.ordinal }) }
                }
                if (tab == RecordsTab.Week.ordinal) WeekTab(ui, wide) else TripsTab(ui, onOpenTrip)
            }
        }
    }
}

private fun mutableStateOfTab() = androidx.compose.runtime.mutableIntStateOf(RecordsTab.Week.ordinal)

/* ---------------- This week ---------------- */

@Composable
private fun WeekTab(ui: RecordsUi, wide: Boolean) {
    val c = LocalVillageColors.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        Text(stringResource(R.string.records_week_desc, ui.weekRange), style = bodyStyle(15.sp), color = c.fg2)
        Spacer(Modifier.height(14.dp))
        val rows = if (wide) listOf(ui.stats) else ui.stats.chunked(2)
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    row.forEach { StatTile(it, Modifier.weight(1f)) }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        VCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 16.dp, bottom = 14.dp)) {
            Column {
                Text(stringResource(R.string.records_days_title), style = juaStyle(20.sp), color = Ui.Ink)
                DayBars(ui.days, Modifier.fillMaxWidth().height(if (wide) 190.dp else 170.dp).padding(top = 10.dp))
            }
        }
    }
}

/** One this-week card: label, value, comparison with last week. */
@Composable
private fun StatTile(s: WeekStat, modifier: Modifier) {
    VCard(modifier, contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp)) {
        Column {
            Text(s.label, style = bodyStyle(14.sp), color = Ui.Ink2, maxLines = 2, modifier = Modifier.heightIn(min = 40.dp))
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 4.dp)) {
                Text(s.value, style = numberStyle(34.sp), color = Ui.Ink)
                Text(s.unit, style = bodyStyle(15.sp), color = Ui.Ink2, modifier = Modifier.padding(start = 3.dp, bottom = 5.dp))
            }
            Text(s.compare, style = bodyStyle(13.sp, FontWeight.Bold), color = if (s.good) Ui.CardOk else Ui.CardWarn, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

/**
 * Per-day safety score bars. A single series, so no legend; only the 80-point line is labeled.
 * 80+ is mint, below is apricot. To avoid relying on color alone, scores are written in the row below, with sub-80 in the warn color.
 */
@Composable
private fun DayBars(days: List<DayScore>, modifier: Modifier) {
    val dayNone = stringResource(R.string.records_day_none)
    Column(modifier) {
        val measurer = rememberTextMeasurer()
        Canvas(Modifier.fillMaxWidth().weight(1f)) {
            val lw = Dimens.Outline.toPx()
            val label = measurer.measure("80", bodyStyle(12.sp, FontWeight.SemiBold).copy(color = Ui.Ink2))
            val right = size.width - label.size.width - 6.dp.toPx()
            val top = 8.dp.toPx()
            val base = size.height - lw / 2
            val h = base - top
            val slot = right / days.size
            val barW = minOf(slot * 0.46f, 44.dp.toPx())
            val r = CornerRadius(8.dp.toPx())
            val stroke = Stroke(lw, join = StrokeJoin.Round)
            days.forEachIndexed { i, d ->
                val s = d.score ?: return@forEachIndexed
                val bh = h * s / 100f
                val x = slot * i + (slot - barW) / 2
                // Bar rounded only at the top (square where it meets the baseline)
                val bar = Path().apply {
                    addRoundRect(RoundRect(Rect(x, base - bh, x + barW, base), topLeft = r, topRight = r, bottomLeft = CornerRadius.Zero, bottomRight = CornerRadius.Zero))
                }
                drawPath(bar, if (s >= 80) Art.Mint2 else Art.NoFill)
                drawPath(bar, Ui.Ol, style = stroke)
            }
            drawLine(Ui.Ol, Offset(0f, base), Offset(right, base), lw, cap = StrokeCap.Round)
            // 80-point line (dashed) and its label
            val y80 = base - h * 0.8f
            drawLine(
                Ui.Ol, Offset(0f, y80), Offset(right, y80), lw,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())), cap = StrokeCap.Round,
            )
            drawText(label, topLeft = Offset(right + 6.dp.toPx(), y80 - label.size.height / 2f))
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp, end = 26.dp)) {
            days.forEach { d ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(d.label, style = bodyStyle(14.sp, FontWeight.Bold), color = Ui.Ink)
                    val s = d.score
                    Text(
                        if (s == null) dayNone else stringResource(R.string.score_badge, s),
                        style = bodyStyle(12.sp, if (s != null && s < 80) FontWeight.Bold else FontWeight.Medium),
                        color = when { s == null -> Ui.Ink2; s < 80 -> Ui.CardWarn; else -> Ui.Ink2 },
                    )
                }
            }
        }
    }
}

/* ---------------- Trips ---------------- */

@Composable
private fun TripsTab(ui: RecordsUi, onOpenTrip: (String) -> Unit) {
    val c = LocalVillageColors.current
    Column(Modifier.fillMaxSize()) {
        Text(stringResource(R.string.records_trips_desc, ui.trips.size), style = bodyStyle(15.sp), color = c.fg2)
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(top = 14.dp, bottom = 24.dp),
        ) {
            items(ui.trips, key = { it.id }) { t -> TripRow(t) { onOpenTrip(t.id) } }
        }
    }
}

@Composable
private fun TripRow(t: TripUi, onClick: () -> Unit) {
    val wide = LocalWidthClass.current == WidthClass.Wide
    VCard(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = Dimens.MinTouch).padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(Modifier.width(if (wide) 150.dp else 110.dp)) {
                Text(t.day, style = juaStyle(18.sp), color = Ui.Ink)
                Text(t.time, style = bodyStyle(14.sp), color = Ui.Ink2)
            }
            Column(Modifier.weight(1f)) {
                Text(t.duration, style = bodyStyle(16.sp, FontWeight.Bold), color = Ui.Ink)
                val env = t.environments.joinToString(" · ").ifEmpty { stringResource(R.string.records_no_env) }
                Text(env, style = bodyStyle(14.sp), color = Ui.Ink2, maxLines = 1)
            }
            t.met.take(2).forEach { ResidentArt(it.kind, 44.dp) }
            ScoreBadge(t)
            VillageIcon(VIcon.Chevron, 22.dp, Ui.Ink2)
        }
    }
}

/** Score pill: mint for 80+, apricot below. Short trips show "짧은 운행" instead of a score. */
@Composable
private fun ScoreBadge(t: TripUi) {
    val short = t.summary.state == ArrivalState.ShortTrip
    val bg = when { short -> Ui.White; t.score >= 80 -> Art.Mint2; else -> Art.NoFill }
    Text(
        if (short) stringResource(R.string.records_short) else stringResource(R.string.score_badge, t.score),
        style = bodyStyle(15.sp, FontWeight.Bold).copy(fontFeatureSettings = "tnum"),
        color = Ui.Ink,
        modifier = Modifier.clip(CircleShape).background(bg).border(Dimens.Outline, Ui.Ol, CircleShape)
            .padding(horizontal = 14.dp, vertical = 5.dp),
    )
}

/* ---------------- Previews ---------------- */

@Preview(widthDp = 1152, heightDp = 748)
@Composable
private fun RecordsWidePreview() = PleosVillageTheme(night = false) { DesignScaled { RecordsScreen(FakeRepository.records, {}, {}) } }

@Preview(widthDp = 1152, heightDp = 748)
@Composable
private fun RecordsNightPreview() = PleosVillageTheme(night = true) { DesignScaled { RecordsScreen(FakeRepository.records, {}, {}) } }

@Preview(widthDp = 571, heightDp = 748)
@Composable
private fun RecordsNarrowPreview() = PleosVillageTheme(night = false) { DesignScaled { RecordsScreen(FakeRepository.records, {}, {}) } }
