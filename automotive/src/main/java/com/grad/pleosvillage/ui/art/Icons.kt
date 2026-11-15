package com.grad.pleosvillage.ui.art

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.grad.pleosvillage.ui.design.Art
import com.grad.pleosvillage.ui.design.Dimens
import com.grad.pleosvillage.ui.design.Ui

/**
 * Line icons. mockups.html IC (viewBox 24, stroke 2, round caps). Color is set by the caller.
 * Home~Arrow match the mockup; the rest (Clock, Rain, People, Seat, Star, Ticket) were drawn with the same rules.
 */
enum class VIcon { House, Book, Chart, Gear, Brush, Undo, Redo, Lock, Arrow, Plus, Minus, Clock, Rain, People, Seat, Star, Ticket, Check, Close, Chevron }

private fun line(vararg d: String): List<String> = d.toList()

private val ICONS: Map<VIcon, List<String>> = mapOf(
    VIcon.House to line("M3.5 11 12 4.5l8.5 6.5", "M5.5 10v9.5h13V10"),
    VIcon.Book to line("M4 5.5C6.5 4 9.5 4 12 6c2.5-2 5.5-2 8-.5V19c-2.5-1.5-5.5-1.5-8 .5-2.5-2-5.5-2-8-.5Z", "M12 6v13.5"),
    VIcon.Chart to line("M5 20V12", "M12 20V5", "M19 20v-9"),
    VIcon.Gear to line(
        "M15 12a3 3 0 1 1-6 0a3 3 0 1 1 6 0Z",
        "M12 3v2.5M12 18.5V21M3 12h2.5M18.5 12H21M5.6 5.6l1.8 1.8M16.6 16.6l1.8 1.8M5.6 18.4l1.8-1.8M16.6 7.4l1.8-1.8",
    ),
    VIcon.Brush to line("m14.5 4.5 5 5L11 18l-5-5Z", "M6 13c-2 0-3 1.5-3 3.5V20h3.5c2 0 3.5-1 3.5-3"),
    VIcon.Undo to line("M9 14 4 9l5-5", "M4 9h10.5a5.5 5.5 0 0 1 0 11H11"),
    VIcon.Redo to line("m15 14 5-5-5-5", "M20 9H9.5a5.5 5.5 0 0 0 0 11H13"),
    VIcon.Lock to line("M7.5 11h9a2.5 2.5 0 0 1 2.5 2.5v4a2.5 2.5 0 0 1-2.5 2.5h-9A2.5 2.5 0 0 1 5 17.5v-4A2.5 2.5 0 0 1 7.5 11Z", "M8 11V8a4 4 0 0 1 8 0v3"),
    VIcon.Arrow to line("M5 12h14M13 6l6 6-6 6"),
    VIcon.Plus to line("M5 12h14M12 5v14"),
    VIcon.Minus to line("M5 12h14"),
    VIcon.Clock to line("M21 12a9 9 0 1 1-18 0a9 9 0 1 1 18 0Z", "M12 7v5l3.5 2"),
    VIcon.Rain to line("M7 15.5a4 4 0 0 1 .5-8 5 5 0 0 1 9.5 1.5 3.3 3.3 0 0 1 0 6.5Z", "M9 18.5l-1 2M13 18.5l-1 2M17 18.5l-1 2"),
    VIcon.People to line("M12 8.5a3 3 0 1 1-6 0a3 3 0 1 1 6 0Z", "M3.5 19.5c.5-3.5 2.5-5 5.5-5s5 1.5 5.5 5", "M18.5 10a2.5 2.5 0 1 1-5 0a2.5 2.5 0 1 1 5 0Z", "M16 14.5c2.5 0 4 1.3 4.5 4"),
    VIcon.Seat to line("M8 4h6a2 2 0 0 1 2 2v8H8Z", "M5.5 14h13v3.5a1.5 1.5 0 0 1-1.5 1.5H7a1.5 1.5 0 0 1-1.5-1.5Z", "M8 19v2M16 19v2"),
    VIcon.Star to line("M12 3.5l2.6 5.3 5.9.9-4.3 4.1 1 5.8L12 16.8l-5.2 2.8 1-5.8-4.3-4.1 5.9-.9Z"),
    VIcon.Check to line("M5 12.5l4.5 4.5L19 7.5"),
    VIcon.Close to line("M6.5 6.5l11 11M17.5 6.5l-11 11"),
    VIcon.Chevron to line("M9.5 5.5l6.5 6.5-6.5 6.5"),
    VIcon.Ticket to line("M4 7h16v3.5a1.5 1.5 0 0 0 0 3V17H4v-3.5a1.5 1.5 0 0 0 0-3Z", "M9.5 7.5v9"),
)

private val iconEls = HashMap<Pair<VIcon, Color>, List<SvgEl>>()

fun iconElements(icon: VIcon, color: Color): List<SvgEl> = synchronized(iconEls) {
    iconEls.getOrPut(icon to color) {
        ICONS.getValue(icon).map { SPath(it, fill = null, stroke = color, sw = 2f, scaleStroke = true) }
    }
}

/** A single line icon. */
@Composable
fun VillageIcon(icon: VIcon, size: Dp, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) { drawSvg(iconElements(icon, color), 24f, 2.dp.toPx()) }
}

/** App logo: lavender house + yellow door. mockups.html logo(). Stroke fixed at 2dp. */
private val LOGO = listOf(
    SPath("M3.5 11.5 12 4.5l8.5 7V19a1.5 1.5 0 0 1-1.5 1.5H5A1.5 1.5 0 0 1 3.5 19Z", Art.Peri),
    SPath("M9.5 20.5v-4a2.5 2.5 0 0 1 5 0v4", Art.Butter),
)

@Composable
fun BrandLogo(size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) { drawSvg(LOGO, 24f, Dimens.Outline.toPx()) }
}

@Preview(showBackground = true, backgroundColor = 0xFFF6F6FE)
@Composable
private fun IconsPreview() {
    Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BrandLogo(24.dp)
        VIcon.entries.forEach { VillageIcon(it, 24.dp, Ui.Ink) }
    }
}
