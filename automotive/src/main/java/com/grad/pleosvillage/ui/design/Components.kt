package com.grad.pleosvillage.ui.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grad.pleosvillage.ui.art.BrandLogo
import com.grad.pleosvillage.ui.art.ResidentArt
import com.grad.pleosvillage.ui.art.ResidentKind
import com.grad.pleosvillage.ui.art.VIcon
import com.grad.pleosvillage.ui.art.VillageIcon
import com.grad.pleosvillage.ui.art.f2
import com.grad.pleosvillage.ui.theme.PleosVillageTheme
import kotlin.math.max
import kotlin.math.roundToInt

/*
 * v4 components. docs/design/REFERENCES.md §3.
 * All borders are 2dp #3D3B52; no shadows or gradients.
 */

private val Outline = Dimens.Outline

/* ------------------------------------------------------------------ */
/* Card: white background + 2dp outline + 16dp corners */

@Composable
fun VCard(
    modifier: Modifier = Modifier,
    corner: Dp = Dimens.Corner,
    color: Color = LocalVillageColors.current.card,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(corner)
    Box(
        modifier.clip(shape).background(color).border(Outline, Ui.Ol, shape).padding(contentPadding),
        content = content,
    )
}

/* ------------------------------------------------------------------ */
/* Buttons: primary (btn background) / secondary (white background). 58dp tall + 2dp outline. The background darkens slightly on press (immediate feedback). */

@Composable
fun VButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = true,
    icon: VIcon? = null,
    iconAfter: Boolean = false,
    height: Dp = 58.dp,
    fontSize: TextUnit = TextSizes.Button,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val base = if (primary) Ui.Btn else Ui.White
    val bg = if (pressed) lerpColor(base, Ui.Ol, 0.12f) else base
    val shape = RoundedCornerShape(Dimens.Corner)
    val fg = if (enabled) Ui.BtnFg else Ui.Ink2
    // The visible button uses the mockup height; the touch target is at least 76dp (UI_SPEC §0, item 4).
    Box(
        modifier
            .heightIn(min = Dimens.MinTouch)
            .alpha(if (enabled) 1f else 0.45f)
            .clickable(interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
        propagateMinConstraints = true,
    ) {
        Row(
            Modifier
                .requiredHeight(height)
                .clip(shape)
                .background(bg)
                .border(Outline, Ui.Ol, shape)
                .padding(horizontal = 26.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        ) {
            if (icon != null && !iconAfter) VillageIcon(icon, (fontSize.value * 1.1f).dp, fg)
            Text(text, style = bodyStyle(fontSize, FontWeight.Bold), color = fg, maxLines = 1)
            if (icon != null && iconAfter) VillageIcon(icon, (fontSize.value * 1.05f).dp, fg)
        }
    }
}

/** Transparent touch padding outside the button: (76dp - visible height) / 2. Subtract it when matching mockup positions in layouts. */
val ButtonTouchInset = (Dimens.MinTouch - 58.dp) / 2

/** White circular icon button (undo, zoom). Art 44dp, touch target [touch]. */
@Composable
fun IconCircleButton(icon: VIcon, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 44.dp, touch: Dp = Dimens.MinTouch) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier.size(touch).clickable(interaction, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(size).clip(CircleShape).background(if (pressed) Ui.Sel else Ui.White).border(Outline, Ui.Ol, CircleShape),
            contentAlignment = Alignment.Center,
        ) { VillageIcon(icon, size * 0.46f, Ui.Ink) }
    }
}

/* ------------------------------------------------------------------ */
/* Menus and tabs: only the selected one gets the sel background + outline */

/** One item of the left menu in the wide layout (mockup screens 2·3). Touch target height [touch]. */
@Composable
fun NavItem(icon: VIcon, label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, touch: Dp = Dimens.MinTouch) {
    val c = LocalVillageColors.current
    val shape = RoundedCornerShape(14.dp)
    val fg = if (selected) Ui.Ink else c.fg2
    Box(
        modifier.heightIn(min = touch).clickable(role = Role.Tab, onClick = onClick),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            Modifier.clip(shape)
                .then(if (selected) Modifier.background(Ui.Sel).border(Outline, Ui.Ol, shape) else Modifier)
                .padding(start = 12.dp, end = 17.dp, top = 9.dp, bottom = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            VillageIcon(icon, 22.dp, fg)
            Text(label, style = bodyStyle(17.sp, if (selected) FontWeight.Bold else FontWeight.Medium), color = fg)
        }
    }
}

/** Dex filter and decorate category tabs (pill shaped). */
@Composable
fun TabPill(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, touch: Dp = Dimens.MinTouch) {
    val c = LocalVillageColors.current
    Box(modifier.heightIn(min = touch).clickable(role = Role.Tab, onClick = onClick), contentAlignment = Alignment.Center) {
        Text(
            text,
            style = bodyStyle(16.sp, if (selected) FontWeight.Bold else FontWeight.SemiBold),
            color = if (selected) Ui.Ink else c.fg2,
            modifier = Modifier.clip(CircleShape)
                .then(if (selected) Modifier.background(Ui.Sel).border(Outline, Ui.Ol, CircleShape) else Modifier)
                .padding(horizontal = 16.dp, vertical = 6.dp),
        )
    }
}

/** One bottom tab item in the narrow layout (mockup screen 4). */
@Composable
fun BottomTabItem(icon: VIcon, label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val fg = if (selected) Ui.Ink else Ui.Ink2
    Column(
        modifier.clickable(role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
    ) {
        val shape = RoundedCornerShape(12.dp)
        Box(
            Modifier.clip(shape)
                .then(if (selected) Modifier.background(Ui.Sel).border(Outline, Ui.Ol, shape) else Modifier)
                .padding(horizontal = 16.dp, vertical = 3.dp),
        ) { VillageIcon(icon, 22.dp, fg) }
        Text(label, style = bodyStyle(13.sp, if (selected) FontWeight.Bold else FontWeight.Medium), color = fg)
    }
}

/** Underlined text link ("확률과 천장 보기"). */
@Composable
fun TextLink(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = LocalVillageColors.current.fg) {
    Box(modifier.heightIn(min = Dimens.MinTouch).clickable(role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
        Text(text, style = bodyStyle(15.sp, FontWeight.SemiBold).copy(textDecoration = TextDecoration.Underline), color = color)
    }
}

/* ------------------------------------------------------------------ */
/* Stamp card: white background with a perforated edge (semicircle holes) + inner rounded rect (per-resident color). mockups.html perfPath(), stampBox() */

/** Perforated-edge Path string (px). r = hole radius, sp = hole spacing. */
fun perfPath(w: Float, h: Float, r: Float, sp: Float): String {
    fun seg(len: Float): Pair<Int, Float> { val n = max(2, (len / sp).roundToInt()); return n to len / n }
    val sb = StringBuilder("M0 0")
    var (n, st) = seg(w)
    for (q in 0 until n) { val c = st * (q + .5f); sb.append(" L${f2(c - r)} 0 A${f2(r)} ${f2(r)} 0 0 0 ${f2(c + r)} 0") }
    sb.append(" L${f2(w)} 0")
    seg(h).also { n = it.first; st = it.second }
    for (q in 0 until n) { val c = st * (q + .5f); sb.append(" L${f2(w)} ${f2(c - r)} A${f2(r)} ${f2(r)} 0 0 0 ${f2(w)} ${f2(c + r)}") }
    sb.append(" L${f2(w)} ${f2(h)}")
    seg(w).also { n = it.first; st = it.second }
    for (q in 0 until n) { val c = w - st * (q + .5f); sb.append(" L${f2(c + r)} ${f2(h)} A${f2(r)} ${f2(r)} 0 0 0 ${f2(c - r)} ${f2(h)}") }
    sb.append(" L0 ${f2(h)}")
    seg(h).also { n = it.first; st = it.second }
    for (q in 0 until n) { val c = h - st * (q + .5f); sb.append(" L0 ${f2(c + r)} A${f2(r)} ${f2(r)} 0 0 0 0 ${f2(c - r)}") }
    return sb.append("Z").toString()
}

@Composable
fun StampCard(
    tint: Color,
    modifier: Modifier = Modifier,
    inset: Dp = 12.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier.drawWithCache {
            val lw = Outline.toPx()
            val pad = 1.5f * lw / 2f
            val path: Path = PathParser().parsePathString(
                perfPath(size.width - 2 * pad, size.height - 2 * pad, 3.4.dp.toPx(), 17.dp.toPx()),
            ).toPath()
            val ins = inset.toPx()
            val stroke = Stroke(lw, cap = StrokeCap.Round, join = StrokeJoin.Round)
            onDrawBehind {
                translate(pad, pad) {
                    drawPath(path, Ui.White)
                    drawPath(path, Ui.Ol, style = stroke)
                }
                val cr = CornerRadius(8.dp.toPx())
                val sz = Size(size.width - 2 * ins, size.height - 2 * ins)
                drawRoundRect(tint, Offset(ins, ins), sz, cr)
                drawRoundRect(Ui.Ol, Offset(ins, ins), sz, cr, style = stroke)
            }
        }.padding(inset),
        content = content,
    )
}

/* ------------------------------------------------------------------ */
/* Decorate drawer card: square card (14dp corners, outline) + name + points. The selected card gets the sel background */

@Composable
fun DrawerCard(
    name: String,
    cost: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    locked: Boolean = false,
    selected: Boolean = false,
    /** Count in storage. If present, shows "보관 N" instead of points. */
    stored: Int = 0,
    art: @Composable BoxScope.() -> Unit,
) {
    val c = LocalVillageColors.current
    Column(modifier.width(112.dp).clickable(role = Role.Button, onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        val shape = RoundedCornerShape(14.dp)
        Box(
            Modifier.fillMaxWidth().height(92.dp).clip(shape).background(if (selected) Ui.Sel else Ui.White).border(Outline, Ui.Ol, shape),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.then(if (locked) Modifier.alpha(0.45f) else Modifier), contentAlignment = Alignment.Center, content = art)
        }
        Text(name, style = juaStyle(15.sp), color = c.fg, modifier = Modifier.padding(top = 6.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            if (stored > 0) {
                Text("보관 $stored", style = bodyStyle(13.sp, FontWeight.Bold), color = c.fg)
            } else {
                if (locked) VillageIcon(VIcon.Lock, 13.dp, c.fg2)
                Text("$cost 포인트", style = bodyStyle(13.sp, FontWeight.SemiBold), color = c.fg2)
            }
        }
    }
}


/* ------------------------------------------------------------------ */
/* Speech bubble: white background + outline + tail. Font Jua 18 */

enum class TailSide { Bottom, Left }

/** Speech bubble Path (px). Body (l, top, r, bottom), tail tip (ax, ay). mockups.html layoutBubbles() */
fun bubblePath(l: Float, top: Float, r: Float, bottom: Float, ax: Float, ay: Float, side: TailSide, rad: Float, tailHalf: Float): Path =
    Path().apply {
        moveTo(l + rad, top)
        lineTo(r - rad, top)
        quadraticBezierTo(r, top, r, top + rad)
        lineTo(r, bottom - rad)
        quadraticBezierTo(r, bottom, r - rad, bottom)
        if (side == TailSide.Bottom) {
            val bx = ax.coerceIn(l + rad + tailHalf + 1, r - rad - tailHalf - 1)
            lineTo(bx + tailHalf, bottom); lineTo(ax, ay); lineTo(bx - tailHalf, bottom)
            lineTo(l + rad, bottom)
            quadraticBezierTo(l, bottom, l, bottom - rad)
            lineTo(l, top + rad)
        } else {
            lineTo(l + rad, bottom)
            quadraticBezierTo(l, bottom, l, bottom - rad)
            val my = (top + bottom) / 2
            lineTo(l, my + tailHalf * 0.75f); lineTo(ax, ay); lineTo(l, my - tailHalf * 0.75f)
            lineTo(l, top + rad)
        }
        quadraticBezierTo(l, top, l + rad, top)
        close()
    }

@Composable
fun SpeechBubble(text: String, modifier: Modifier = Modifier, side: TailSide = TailSide.Bottom, fontSize: TextUnit = 18.sp) {
    val tail = 12.dp
    Box(
        modifier
            .padding(if (side == TailSide.Bottom) PaddingValues(bottom = tail) else PaddingValues(start = tail))
            .drawBehind {
                val lw = Outline.toPx()
                val t = tail.toPx()
                val p = if (side == TailSide.Bottom) {
                    bubblePath(0f, 0f, size.width, size.height, size.width * 0.3f, size.height + t, side, 12.dp.toPx(), 8.dp.toPx())
                } else {
                    bubblePath(0f, 0f, size.width, size.height, -t, size.height / 2 + 2.dp.toPx(), side, 12.dp.toPx(), 8.dp.toPx())
                }
                drawPath(p, Ui.White)
                drawPath(p, Ui.Ol, style = Stroke(lw, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            .defaultMinSize(minHeight = 40.dp)
            .padding(horizontal = 18.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = juaStyle(fontSize), color = Ui.Ink) }
}

/* ------------------------------------------------------------------ */
/* Score bar: 14dp bar + 2dp outline, 80-point tick and "80". Mint at 80 or above, apricot below */

@Composable
fun ScoreBar(score: Int, progress: Float, modifier: Modifier = Modifier) {
    val c = LocalVillageColors.current
    val pass = score >= 80
    Box(modifier.fillMaxWidth().height(36.dp)) {
        Canvas(Modifier.fillMaxWidth().height(20.dp)) {
            val lw = Outline.toPx()
            val barH = 14.dp.toPx()
            val top = 3.dp.toPx()
            val r = CornerRadius(8.dp.toPx())
            val bar = Size(size.width, barH)
            drawRoundRect(Ui.White, Offset(0f, top), bar, r)
            val fill = (score * progress / 100f).coerceIn(0f, 1f) * size.width
            if (fill > 0f) drawRoundRect(if (pass) Art.Mint2 else Art.NoFill, Offset(0f, top), Size(max(fill, barH), barH), r)
            drawRoundRect(Ui.Ol, Offset(lw / 2, top + lw / 2), Size(size.width - lw, barH - lw), r, style = Stroke(lw))
            val x = size.width * 0.8f
            drawLine(c.olOnBg, Offset(x, 0f), Offset(x, top + barH + 3.dp.toPx()), strokeWidth = lw, cap = StrokeCap.Round)
        }
        Text(
            "80", style = bodyStyle(12.sp, FontWeight.SemiBold), color = c.fg2,
            modifier = Modifier.align(Alignment.TopStart).padding(top = 20.dp).fillMaxWidth().centerAt(0.8f),
        )
    }
}

/** Centers this text at the [frac] position of the parent width. */
private fun Modifier.centerAt(frac: Float): Modifier = layout { m, cons ->
    val p = m.measure(cons.copy(minWidth = 0))
    layout(cons.maxWidth, p.height) { p.place((cons.maxWidth * frac - p.width / 2f).roundToInt(), 0) }
}

/* ------------------------------------------------------------------ */
/* Value card: several "운전 시간 ··· 1시간 50분" rows with dashed dividers */

@Composable
fun StatCard(rows: List<Pair<String, String>>, modifier: Modifier = Modifier, fontSize: TextUnit = 17.sp, valueColors: Map<Int, Color> = emptyMap()) {
    VCard(modifier, contentPadding = PaddingValues(horizontal = 20.dp, vertical = 2.dp)) {
        Column {
            rows.forEachIndexed { i, (label, value) ->
                Row(
                    Modifier.fillMaxWidth()
                        .then(if (i > 0) Modifier.dashedTop(Ui.CardLine) else Modifier)
                        .padding(vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(label, style = bodyStyle(fontSize), color = Ui.Ink2, modifier = Modifier.weight(1f))
                    Text(value, style = bodyStyle(fontSize, FontWeight.Bold).copy(fontFeatureSettings = "tnum"), color = valueColors[i] ?: Ui.Ink)
                }
            }
        }
    }
}

/** Top 2dp dashed line (divider inside a card). */
fun Modifier.dashedTop(color: Color): Modifier = drawBehind {
    val lw = Outline.toPx()
    drawLine(
        color, Offset(0f, lw / 2), Offset(size.width, lw / 2), strokeWidth = lw,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
    )
}

/** Left 2dp dashed line (divider between stat cells). */
fun Modifier.dashedStart(color: Color): Modifier = drawBehind {
    val lw = Outline.toPx()
    drawLine(
        color, Offset(lw / 2, 0f), Offset(lw / 2, size.height), strokeWidth = lw,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
    )
}

/** Small label + large value (stat cell). Used inside light cards. */
@Composable
fun LabelValue(label: String, value: String, modifier: Modifier = Modifier, unit: String? = null, valueSize: TextUnit = 22.sp, alignEnd: Boolean = false) {
    Column(modifier, horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Text(label, style = bodyStyle(14.sp), color = Ui.Ink2)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = numberStyle(valueSize), color = Ui.Ink)
            if (unit != null) Text(unit, style = bodyStyle(14.sp), color = Ui.Ink2, modifier = Modifier.padding(bottom = 2.dp))
        }
    }
}

/** Logo + "Pleos Village". */
@Composable
fun Brand(modifier: Modifier = Modifier, color: Color = LocalVillageColors.current.fg2) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BrandLogo(22.dp)
        Text("Pleos Village", style = juaStyle(17.sp), color = color)
    }
}

/** Screen title (Jua). */
@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier, size: TextUnit = TextSizes.ScreenTitle, color: Color = LocalVillageColors.current.fg) {
    Text(text, style = juaStyle(size), color = color, modifier = modifier)
}

internal fun lerpColor(a: Color, b: Color, t: Float) = Color(
    red = a.red + (b.red - a.red) * t,
    green = a.green + (b.green - a.green) * t,
    blue = a.blue + (b.blue - a.blue) * t,
    alpha = 1f,
)

/* ------------------------------------------------------------------ */
/* Previews (day · night) */

@Composable
private fun ComponentsSample() {
    val c = LocalVillageColors.current
    Column(Modifier.background(c.bg).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Brand()
        ScreenTitle("주민 도감")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            VButton("닫기", {}, primary = false)
            VButton("마을로 데려가기", {}, icon = VIcon.Arrow, iconAfter = true)
            VButton("마을 꾸미기", {}, icon = VIcon.Brush)
            IconCircleButton(VIcon.Undo, {})
            IconCircleButton(VIcon.Plus, {})
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column { NavItem(VIcon.House, "마을", true, {}); NavItem(VIcon.Book, "도감", false, {}) }
            TabPill("전체", true, {}); TabPill("만남", false, {}); TabPill("아직", false, {})
            BottomTabItem(VIcon.House, "마을", true, {}, Modifier.width(90.dp))
            BottomTabItem(VIcon.Book, "도감", false, {}, Modifier.width(90.dp))
            TextLink("확률과 천장 보기", {})
        }
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.Top) {
            StampCard(ResidentKind.Rain.tint, Modifier.size(214.dp, 164.dp)) {
                ResidentArt(ResidentKind.Rain, 112.dp, Modifier.align(Alignment.Center))
            }
            StampCard(StampTint.Locked, Modifier.size(214.dp, 164.dp)) {
                ResidentArt(ResidentKind.LongTrip, 96.dp, Modifier.align(Alignment.Center), silhouette = true)
            }
            DrawerCard("분수", 120, {}, selected = true) { ResidentArt(ResidentKind.Night, 70.dp) }
            DrawerCard("등불", 80, {}, locked = true) { ResidentArt(ResidentKind.Charge, 70.dp) }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SpeechBubble("11월 7일에 왔어요")
                SpeechBubble("분수, 여기 둘까요?", side = TailSide.Left)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(Modifier.width(420.dp)) {
                ScoreBar(100, 1f)
                Spacer(Modifier.height(4.dp))
                ScoreBar(70, 1f)
            }
            StatCard(listOf("운전 시간" to "1시간 50분", "오늘의 환경" to "비 · 가족 동승", "포인트" to "+20"), Modifier.width(420.dp))
        }
        VCard(contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(36.dp)) {
                LabelValue("이번 주 평균", "94", unit = " 점 · ↑8")
                LabelValue("포인트", "3,240")
                LabelValue("오늘 남은 기회", "1", unit = " / 2")
            }
        }
    }
}

@Preview(widthDp = 1100, heightDp = 760)
@Composable
private fun ComponentsDayPreview() = PleosVillageTheme(night = false) { ComponentsSample() }

@Preview(widthDp = 1100, heightDp = 760)
@Composable
private fun ComponentsNightPreview() = PleosVillageTheme(night = true) { ComponentsSample() }
