package com.grad.pleosvillage.ui.art

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import com.grad.pleosvillage.ui.design.Art
import com.grad.pleosvillage.ui.design.Jua
import com.grad.pleosvillage.ui.design.TailSide
import com.grad.pleosvillage.ui.design.Ui
import com.grad.pleosvillage.ui.design.bubblePath
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/*
 * Village: a round grassy island. Ported as-is from mockups.html village(), house(), tree(), flowerPatch(), fountain(), lamp(), bench(), fence(),
 * pond(), stone(), deco(), sky(). Coordinates are mockup SVG units (app area 1152×748).
 * Tile coords ↔ screen coords: x = OX + (u - v) * TW/2, y = OY + (u + v) * TH/2, TH = TW/2 (REFERENCES §3)
 * All strokes are fixed at 2dp on screen (Svg.kt).
 */

/** Kinds of objects in the village. All but Resident can be moved in decorate mode (House can only be moved, not stored). */
enum class ObjType { Resident, House, Tree, Flowers, Fountain, Lamp, Bench, Lantern, Hill, Stones, Fence }

/** An object in the village (placed separately, not from the map characters). */
data class VillageObject(
    val i: Int,
    val j: Int,
    val type: ObjType,
    val kind: ResidentKind? = null,
    val selected: Boolean = false,
    /** For the walking animation: position offset in tiles and hop height (mockup units). */
    val di: Float = 0f,
    val dj: Float = 0f,
    val lift: Float = 0f,
    /** Horizontal flip (decorate's "돌리기") */
    val flip: Boolean = false,
    /** Preview before placing (semi-transparent) */
    val ghost: Boolean = false,
)

/** Fence: tile vertex coordinates (u0, v0) → (u1, v1). */
data class Fence(val u0: Float, val v0: Float, val u1: Float, val v1: Float)

/** Sky decoration. */
sealed interface SkyItem {
    data class Cloud(val x: Float, val y: Float, val z: Float) : SkyItem
    data class Star(val x: Float, val y: Float, val r: Float) : SkyItem
    data class Spark(val x: Float, val y: Float, val r: Float) : SkyItem
    data class Bub(val x: Float, val y: Float, val r: Float) : SkyItem
}

/** Speech bubble shape. The o (dx, by, ax, ay, side) of mockups.html bubble(). */
data class BubbleStyle(val dx: Float = 0f, val by: Float = 60f, val ax: Float = 0f, val ay: Float = 58f, val side: Boolean = false)

data class Bubble(val i: Int, val j: Int, val text: String, val style: BubbleStyle = BubbleStyle())

enum class TileMark { Ok, No }

data class VillageSpec(
    val w: Float = 1152f,
    val h: Float = 748f,
    val n: Int = 7,
    val tw: Float = 104f,
    val ox: Float? = null,
    val oy: Float? = null,
    val map: List<String> = DEFAULT_MAP,
    /** House kind for map character 'h' tiles (follows the resident shape). */
    val houses: Map<Pair<Int, Int>, ResidentKind> = DEFAULT_HOUSES,
    val extras: List<VillageObject> = emptyList(),
    val fences: List<Fence> = emptyList(),
    val stones: List<Pair<Float, Float>> = emptyList(),
    val sky: List<SkyItem> = emptyList(),
    /** Moon drawn only at night (x, y, r). */
    val moon: Triple<Float, Float, Float>? = null,
    val background: Boolean = true,
    val plaza: Boolean = true,
    /** Density of small decorations (grass, flowers). null means the default. */
    val deco: Float? = null,
    val bubbles: List<Bubble> = emptyList(),
    val bubbleStyle: BubbleStyle = BubbleStyle(),
    val edit: Boolean = false,
    val marks: Map<Pair<Int, Int>, TileMark> = emptyMap(),
) {
    val th get() = tw / 2
    val k get() = tw / 104f
    val originX get() = ox ?: (w / 2)
    val originY get() = oy ?: (h / 2 - n * th / 2)

    /** Tile vertex coordinates (u, v) → screen. mockups Q() */
    fun q(u: Float, v: Float) = Offset(originX + (u - v) * tw / 2, originY + (u + v) * th / 2)

    /** Center of tile (i, j). mockups P() */
    fun center(i: Float, j: Float) = q(i + .5f, j + .5f)

    /** Top vertex of tile (i, j). */
    fun p(i: Int, j: Int) = q(i.toFloat(), j.toFloat())

    /** Screen coords → tile (i, j). null if outside the island. Used for hit testing. */
    fun tileAt(x: Float, y: Float): Pair<Int, Int>? {
        val a = (x - originX) / (tw / 2)
        val b = (y - originY) / (th / 2)
        val i = floor((a + b) / 2).toInt()
        val j = floor((b - a) / 2).toInt()
        return if (i in 0 until n && j in 0 until n) i to j else null
    }

    companion object {
        val DEFAULT_MAP = listOf(
            "g g g p g t g",
            "t h g p g h g",
            "g g g p g g t",
            "f p p F p p g",
            "g g g p g g f",
            "g h g p g g h",
            "t g w p t g g",
        )
        val DEFAULT_HOUSES = mapOf(
            (1 to 1) to ResidentKind.Rain, (5 to 1) to ResidentKind.Night,
            (1 to 5) to ResidentKind.Winter, (6 to 5) to ResidentKind.Family,
        )
    }
}

/* ------------------------------------------------------------------ */
/* Village props (anchor point at bottom center) */

private fun door(x: Float, y: Float, w: Float, h: Float, fill: androidx.compose.ui.graphics.Color): SvgEl = SPath(
    "M${f2(x - w / 2)} ${f2(y)} V${f2(y - h + w / 2)} A${f2(w / 2)} ${f2(w / 2)} 0 0 1 ${f2(x + w / 2)} ${f2(y - h + w / 2)} V${f2(y)} Z", fill,
)

private fun rwin(x: Float, y: Float, r: Float, fill: androidx.compose.ui.graphics.Color = Art.Butter): List<SvgEl> = listOf(
    SCircle(x, y, r, fill),
    SPath("M${f2(x - r)} ${f2(y)} H${f2(x + r)} M${f2(x)} ${f2(y - r)} V${f2(y + r)}"),
)

/** A different house for each resident. mockups house(kind). */
fun house(kind: ResidentKind, cx: Float, cy: Float, k: Float): List<SvgEl> {
    fun x(v: Float) = f2(cx + v * k)
    fun y(v: Float) = f2(cy + v * k)
    return when (kind) {
        ResidentKind.Night -> listOf(
            SPath("M${x(0f)} ${y(-52f)} V${y(-58f)}"),
            moon(cx + 3 * k, cy - 67 * k, 9 * k),
            star5(cx + 20 * k, cy - 58 * k, 4.5f * k),
            SPath("M${x(-22f)} ${y(2f)} V${y(-30f)} A${f2(22 * k)} ${f2(22 * k)} 0 0 1 ${x(22f)} ${y(-30f)} V${y(2f)} Z", Art.Lav),
        ) + rwin(cx, cy - 34 * k, 7.5f * k, Art.Butter) + door(cx, cy + 2 * k, 14 * k, 20 * k, Art.Peri)
        ResidentKind.Family -> listOf(
            SRect(cx - 30 * k, cy - 28 * k, 62 * k, 30 * k, 5 * k, Art.White),
            SPath("M${x(2f)} ${y(-25f)} L${x(19f)} ${y(-47f)} L${x(36f)} ${y(-25f)} Z", Art.Peach),
            SPath("M${x(-37f)} ${y(-26f)} L${x(-12f)} ${y(-58f)} L${x(13f)} ${y(-26f)} Z", Art.Pink),
            heart(cx - 12 * k, cy - 37 * k, 1.05f * k),
        ) + rwin(cx - 13 * k, cy - 12 * k, 6.5f * k, Art.Butter) + door(cx + 17 * k, cy + 2 * k, 13 * k, 20 * k, Art.Peach)
        ResidentKind.Winter -> listOf(
            SPath("M${x(0f)} ${y(-31f)} V${y(-47f)}"),
            SPath("M${x(0f)} ${y(-47f)} L${x(12f)} ${y(-43f)} L${x(0f)} ${y(-39f)} Z", Art.Mint2),
            SPath("M${x(-32f)} ${y(2f)} C${x(-32f)} ${y(-44f)} ${x(32f)} ${y(-44f)} ${x(32f)} ${y(2f)} Z", Art.White),
            SPath("M${x(-27f)} ${y(-14f)} Q${x(0f)} ${y(-20f)} ${x(27f)} ${y(-14f)} M${x(-10f)} ${y(-17f)} V${y(-28f)} M${x(13f)} ${y(-17f)} V${y(-27f)}"),
            door(cx + 8 * k, cy + 2 * k, 18 * k, 18 * k, Art.Baby),
            sparkle(cx - 27 * k, cy - 36 * k, 5 * k, Art.Baby),
        )
        // Rain (umbrella roof). Long-trip and charge houses are phase 2 work, so they reuse this shape.
        else -> listOf(
            SRect(cx - 24 * k, cy - 34 * k, 48 * k, 36 * k, 6 * k, Art.White),
            door(cx - 9 * k, cy + 2 * k, 14 * k, 24 * k, Art.Peach),
        ) + rwin(cx + 11 * k, cy - 16 * k, 7 * k, Art.Baby) + listOf(
            SPath("M${x(0f)} ${y(-66f)} V${y(-72f)}"),
            SCircle(cx, cy - 74.5f * k, 2.8f * k, Art.Butter),
        ) + umbrella(cx, cy - 66 * k, 74 * k, UmbrellaColors)
    }
}

fun tree(cx: Float, cy: Float, k: Float, alt: Boolean = false): List<SvgEl> {
    val small = k < .8f
    val out = mutableListOf<SvgEl>(
        SRect(cx - 4 * k, cy - 20 * k, 8 * k, 22 * k, 3 * k, Art.Trunk),
        SCircle(cx, cy - 36 * k, 19 * k, if (alt) Art.Sage2 else Art.Mint2),
        hl(cx - 7 * k, cy - 45 * k, 5 * k, 3.2f * k, -35f, .6f),
    )
    val fr = if (small) listOf(Triple(7f, -34f, 4.2f) to Art.Pink, Triple(-6f, -26f, 4.2f) to Art.Peach)
    else listOf(Triple(7f, -38f, 3.4f) to Art.Pink, Triple(-5f, -28f, 3.4f) to Art.Peach, Triple(10f, -25f, 3.1f) to Art.Pink)
    fr.forEach { (t, c) -> out += SCircle(cx + t.first * k, cy + t.second * k, t.third * k, c) }
    return out
}

fun flowerPatch(cx: Float, cy: Float, k: Float): List<SvgEl> {
    fun x(v: Float) = cx + v * k
    fun y(v: Float) = cy + v * k
    if (k < .8f) {
        return listOf(leaf(x(-14f), y(2f), 14 * k, -155f), leaf(x(12f), y(3f), 13 * k, -25f)) +
            flower(x(-6f), y(-5f), 9 * k) + flower(x(9f), y(-4f), 8 * k, Art.Pink, Art.Butter)
    }
    return listOf(leaf(x(-17f), y(3f), 13 * k, -155f), leaf(x(15f), y(4f), 12 * k, -25f), leaf(x(0f), y(6f), 10 * k, -95f)) +
        flower(x(-10f), y(-6f), 7 * k) + flower(x(7f), y(-8f), 7 * k, Art.Pink, Art.Butter) +
        flower(x(-1f), y(2f), 6.5f * k) + flower(x(16f), y(-1f), 5.5f * k)
}

fun fountain(cx: Float, cy: Float, k: Float): List<SvgEl> = listOf(
    SEllipse(cx, cy, 30 * k, 13 * k, Art.Stone),
    SEllipse(cx, cy - 2 * k, 23 * k, 8.5f * k, Art.Baby),
    SRect(cx - 4 * k, cy - 24 * k, 8 * k, 22 * k, 2 * k, Art.Stone),
    SEllipse(cx, cy - 24 * k, 11 * k, 4.5f * k, Art.Stone),
    drop(cx, cy - 33 * k, 3.8f * k),
    drop(cx - 14 * k, cy - 23 * k, 3.2f * k),
    drop(cx + 14 * k, cy - 23 * k, 3.2f * k),
)

fun lamp(cx: Float, cy: Float, k: Float): List<SvgEl> = listOf(
    SRect(cx - 2.5f * k, cy - 46 * k, 5 * k, 44 * k, 2 * k, Art.Peri),
    SRect(cx - 6.5f * k, cy - 5 * k, 13 * k, 6 * k, 2.5f * k, Art.Peri),
    SCircle(cx, cy - 53 * k, 8 * k, Art.Butter),
    SPath("M${f2(cx - 7.5f * k)} ${f2(cy - 58 * k)} Q${f2(cx)} ${f2(cy - 68 * k)} ${f2(cx + 7.5f * k)} ${f2(cy - 58 * k)} Z", Art.Peri),
)

fun bench(cx: Float, cy: Float, k: Float): List<SvgEl> = listOf(
    SRect(cx - 15 * k, cy - 24 * k, 4.5f * k, 25 * k, 1.5f * k, Art.White),
    SRect(cx + 10.5f * k, cy - 24 * k, 4.5f * k, 25 * k, 1.5f * k, Art.White),
    SRect(cx - 20 * k, cy - 27 * k, 40 * k, 8 * k, 3 * k, Art.Peach),
    SRect(cx - 22 * k, cy - 12 * k, 44 * k, 6 * k, 3 * k, Art.Peach),
)

fun fence(p0: Offset, p1: Offset, k: Float): List<SvgEl> {
    val out = mutableListOf<SvgEl>()
    val n = max(2, (hypot(p1.x - p0.x, p1.y - p0.y) / (19 * k)).roundToInt())
    listOf(-6f, -12f).forEach { h ->
        val d = "M${f2(p0.x)} ${f2(p0.y + h * k)} L${f2(p1.x)} ${f2(p1.y + h * k)}"
        out += SPath(d, stroke = OL, sw = 2f, extraUnits = 4 * k)
        out += SPath(d, stroke = Art.White, sw = 0f, extraUnits = 4 * k)
    }
    for (q in 0..n) {
        val x = p0.x + (p1.x - p0.x) * q / n
        val y = p0.y + (p1.y - p0.y) * q / n
        out += SPath(
            "M${f2(x - 3 * k)} ${f2(y + 1 * k)} V${f2(y - 15 * k)} L${f2(x)} ${f2(y - 19 * k)} L${f2(x + 3 * k)} ${f2(y - 15 * k)} V${f2(y + 1 * k)} Z",
            Art.White,
        )
    }
    return out
}

fun pond(x: Float, y: Float, k: Float): List<SvgEl> {
    fun px(v: Float) = f2(x + v * k)
    fun py(v: Float) = f2(y + v * k)
    return listOf(
        SPath(
            "M${px(-42f)} ${py(0f)} C${px(-44f)} ${py(-16f)} ${px(-12f)} ${py(-22f)} ${px(8f)} ${py(-19f)} " +
                "C${px(34f)} ${py(-16f)} ${px(46f)} ${py(-6f)} ${px(40f)} ${py(8f)} C${px(34f)} ${py(20f)} ${px(-6f)} ${py(22f)} ${px(-26f)} ${py(15f)} " +
                "C${px(-38f)} ${py(11f)} ${px(-41f)} ${py(6f)} ${px(-42f)} ${py(0f)}Z",
            Art.Baby,
        ),
        hl(x - 24 * k, y - 9 * k, 7 * k, 2.2f * k, -12f, .85f),
        SPath("M${px(-30f)} ${py(5f)} L${px(-24f)} ${py(1.5f)} A${f2(7 * k)} ${f2(4.5f * k)} 0 1 1 ${px(-24f)} ${py(7f)} Z", Art.Mint2),
        SPath("M${px(2f)} ${py(-2f)} V${py(-20f)}"),
        SPath("M${px(3.5f)} ${py(-19f)} L${px(3.5f)} ${py(-5f)} L${px(15f)} ${py(-5f)} Z", Art.White),
        SPath("M${px(-11f)} ${py(-2f)} L${px(17f)} ${py(-2f)} Q${px(14f)} ${py(7f)} ${px(3f)} ${py(7f)} Q${px(-8f)} ${py(7f)} ${px(-11f)} ${py(-2f)} Z", Art.Peach),
    )
}

private fun stone(x: Float, y: Float, k: Float): SvgEl = SEllipse(x, y, 6.5f * k, 3.6f * k, Art.Pebble)

private fun deco(x: Float, y: Float, k: Float, t: Int): List<SvgEl> {
    val s = max(k, .75f)
    return when (t) {
        0 -> listOf(leaf(x - 1, y, 8 * s, -125f, Art.Sage2), leaf(x + 1, y, 8 * s, -55f, Art.Sage2))
        1 -> listOf(leaf(x, y, 8 * s, -150f, Art.Mint2)) + flower(x + 3 * s, y - 6 * s, 5 * s)
        else -> listOf(leaf(x, y, 8 * s, -30f, Art.Mint2)) + flower(x - 3 * s, y - 6 * s, 5 * s, Art.Pink, Art.Butter)
    }
}

/** Decorate item: lantern (mockups.html lantern, origin moved from the 112×86 box to bottom center). */
private fun lantern(k: Float): List<SvgEl> = listOf(
    SGroup(
        listOf(
            SPath("M56 8 V20"),
            SRect(48f, 18f, 16f, 6f, 2.5f, Art.Peri),
            SEllipse(56f, 40f, 18f, 16f, Art.Lav),
            SEllipse(56f, 40f, 7.5f, 8f, Art.Butter),
            SRect(50f, 55f, 12f, 5f, 2f, Art.Peri),
            SPath("M56 60 V70"),
            SRect(53f, 70f, 6f, 4f, 1.5f, Art.Peri),
        ),
        tx = -56f * k * .9f, ty = -76f * k * .9f, s = k * .9f,
    ),
)

/** Decorate item: small mountain (mockups.html mountain). */
private fun hill(k: Float): List<SvgEl> = listOf(
    SGroup(
        listOf(
            SPath("M12 74 L44 26 Q48 20 52 26 L64 44 L73 33 Q77 28 81 33 L102 74 Z", Art.Mint2),
            SPath("M36 38 L44 26 Q48 20 52 26 L60 38 L54 35 L48 40 L42 35 Z", Art.White),
        ) + flower(26f, 70f, 6f),
        tx = -57f * k * .8f, ty = -76f * k * .8f, s = k * .8f,
    ),
)

/** Three stepping stones. */
private fun stonesItem(k: Float): List<SvgEl> = listOf(
    SEllipse(-16 * k, 2 * k, 6.5f * k, 3.6f * k, Art.Pebble),
    SEllipse(0f, -3 * k, 6.5f * k, 3.6f * k, Art.Pebble),
    SEllipse(16 * k, 1 * k, 6.5f * k, 3.6f * k, Art.Pebble),
)

private val objCache = HashMap<String, List<SvgEl>>()

/**
 * Art for one object (anchor = bottom center of the tile = origin). Item cards in the decorate drawer use this art too.
 * @param tw Tile width (mockup units). Used for the fence length.
 */
fun objectElements(type: ObjType, kind: ResidentKind?, k: Float, alt: Boolean = false, tw: Float = 104f * k): List<SvgEl> =
    synchronized(objCache) {
        objCache.getOrPut("$type/$kind/$k/$alt/$tw") {
            when (type) {
                ObjType.House -> house(kind ?: ResidentKind.Rain, 0f, 0f, k * 1.1f)
                ObjType.Tree -> tree(0f, 0f, k, alt)
                ObjType.Flowers -> flowerPatch(0f, 0f, k)
                ObjType.Fountain -> fountain(0f, 0f, k)
                ObjType.Lamp -> lamp(0f, 0f, k)
                ObjType.Bench -> bench(0f, 0f, k)
                ObjType.Lantern -> lantern(k)
                ObjType.Hill -> hill(k)
                ObjType.Stones -> stonesItem(k)
                ObjType.Fence -> fence(Offset(-tw * .3f, -tw * .15f + 6 * k), Offset(tw * .3f, tw * .15f + 6 * k), k)
                ObjType.Resident -> emptyList()
            }
        }
    }

/** Sky: clouds, stars, sparkles, bubbles. At night, stars, sparkles, and the moon instead of clouds. mockups sky() */
fun skyElements(items: List<SkyItem>, moon: Triple<Float, Float, Float>?, night: Boolean): List<SvgEl> {
    val out = mutableListOf<SvgEl>()
    items.forEach { s ->
        when (s) {
            is SkyItem.Cloud -> if (night) {
                out += star5(s.x + 22 * s.z, s.y - 16 * s.z, 6f); out += sparkle(s.x + 60 * s.z, s.y - 6 * s.z, 7f, Art.Butter)
            } else out += cloud(s.x, s.y, s.z)
            is SkyItem.Star -> out += star5(s.x, s.y, s.r)
            is SkyItem.Spark -> out += sparkle(s.x, s.y, s.r, if (night) Art.Butter else Art.White)
            is SkyItem.Bub -> out += bub(s.x, s.y, s.r)
        }
    }
    if (night && moon != null) out += moon(moon.first, moon.second, moon.third)
    return out
}

/* ------------------------------------------------------------------ */
/* Whole village */

private fun hash(i: Int, j: Int, s: Int): Float {
    val x = sin(i * 127.1 + j * 311.7 + s * 74.7) * 43758.5453
    return (x - floor(x)).toFloat()
}

private fun rrPts(x0: Float, y0: Float, x1: Float, y1: Float, r: Float, n: Int): List<Pair<Float, Float>> {
    val pts = mutableListOf<Pair<Float, Float>>()
    fun arc(cx: Float, cy: Float, a0: Float) {
        for (q in 0..n) {
            val a = (a0 + 90f * q / n) * PI / 180
            pts += Pair((cx + r * cos(a)).toFloat(), (cy + r * sin(a)).toFloat())
        }
    }
    arc(x1 - r, y0 + r, -90f); arc(x1 - r, y1 - r, 0f); arc(x0 + r, y1 - r, 90f); arc(x0 + r, y0 + r, 180f)
    return pts
}

private fun resample(ch: List<Offset>, sp: Float): List<Offset> {
    val out = mutableListOf(ch[0])
    var carry = 0f
    for (q in 1 until ch.size) {
        val a = ch[q - 1]; val b = ch[q]
        val len = hypot(b.x - a.x, b.y - a.y)
        var d = sp - carry
        while (d <= len) { out += Offset(a.x + (b.x - a.x) * d / len, a.y + (b.y - a.y) * d / len); d += sp }
        carry = len - (d - sp)
    }
    val last = ch.last(); val pl = out.last()
    if (hypot(last.x - pl.x, last.y - pl.y) > sp * .35f) out += last else out[out.size - 1] = last
    return out
}

/** Parts that do not change between draws (island, paths, pond, map objects). */
private class VillageStatic(
    val back: List<SvgEl>,
    val grassClip: String,
    val grid: List<SvgEl>,
    /** (depth, i, shape) — drawn in depth order together with residents. */
    val objs: List<Triple<Float, Float, List<SvgEl>>>,
)

private data class StaticKey(val spec: VillageSpec, val night: Boolean)

private val staticCache = object : LinkedHashMap<StaticKey, VillageStatic>(8, .75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<StaticKey, VillageStatic>?) = size > 6
}

private fun staticOf(spec: VillageSpec, night: Boolean): VillageStatic {
    val key = StaticKey(spec.copy(extras = spec.extras.filter { it.type != ObjType.Resident }, bubbles = emptyList()), night)
    return synchronized(staticCache) { staticCache.getOrPut(key) { buildStatic(key.spec, night) } }
}

private fun buildStatic(s: VillageSpec, night: Boolean): VillageStatic {
    val tw = s.tw; val th = s.th; val k = s.k; val n = s.n
    val dep = tw * .32f
    val ox = s.originX
    val map = s.map.map { it.split(' ') }
    val back = mutableListOf<SvgEl>()
    if (s.background) back += skyElements(s.sky, s.moon, night)

    // Island top (rounded rect in isometric) and dirt sides
    val pts = rrPts(-.3f, -.3f, n + .3f, n + .3f, 1.7f, 12).map { (u, v) -> s.q(u, v) }
    var iL = 0; var iR = 0; var iB = 0
    pts.forEachIndexed { q, p -> if (p.x < pts[iL].x) iL = q; if (p.x > pts[iR].x) iR = q; if (p.y > pts[iB].y) iB = q }
    val cnt = pts.size
    fun walk(a: Int, b: Int): List<Offset> { val r = mutableListOf<Offset>(); var q = a; while (true) { r += pts[q]; if (q == b) break; q = (q + 1) % cnt }; return r }
    val front = walk(iR, iL); val backEdge = walk(iL, iR); val frontR = walk(iR, iB); val frontL = walk(iB, iL)
    val hw = pts[iR].x - ox
    fun sh(x: Float) = dep * (.7f + .3f * (1 - min(1f, ((x - ox) / hw).let { it * it })))
    fun band(ch: List<Offset>) = "M" + ch.joinToString(" L") { "${f2(it.x)} ${f2(it.y)}" } + " L" +
        ch.reversed().joinToString(" L") { "${f2(it.x)} ${f2(it.y + sh(it.x))}" } + "Z"
    back += SPath(band(frontL), Art.Earth)
    back += SPath(band(frontR), Art.EarthD)
    val fr = resample(front, 22 * k)
    listOf(Triple(.2f, .55f, 1f), Triple(.47f, .6f, .8f), Triple(.76f, .5f, 1.1f)).forEach { (t, dd, z) ->
        val p = fr[(t * (fr.size - 1)).roundToInt()]
        back += SEllipse(p.x, p.y + sh(p.x) * dd + 3 * k, 5.5f * k * z, 3.3f * k * z, Art.Pebble)
    }
    val ra = f2(22 * k * .62f)
    val gd = StringBuilder("M" + backEdge.joinToString(" L") { "${f2(it.x)} ${f2(it.y)}" })
    for (q in 1 until fr.size) {
        val p = fr[q]
        if (q < 2 || q > fr.size - 2) gd.append(" L${f2(p.x)} ${f2(p.y)}") else gd.append(" A$ra $ra 0 0 1 ${f2(p.x)} ${f2(p.y)}")
    }
    gd.append("Z")
    back += SPath(gd.toString(), Art.Sage)

    // Cream paths (outline first, fill after, so intersections stay clean)
    val a = s.center(3f, 0f); val b = s.center(3f, 6f); val c = s.center(0f, 3f); val d = s.center(6f, 3f); val mm = s.center(3f, 3f)
    val p1 = "M${f2(a.x)} ${f2(a.y)} Q${f2(mm.x + 10 * k)} ${f2(mm.y - th)} ${f2(mm.x)} ${f2(mm.y)} T${f2(b.x)} ${f2(b.y)}"
    val p2 = "M${f2(c.x)} ${f2(c.y)} Q${f2(mm.x - tw * .6f)} ${f2(mm.y + 8 * k)} ${f2(mm.x)} ${f2(mm.y)} T${f2(d.x)} ${f2(d.y)}"
    val w1 = tw * .3f; val w2 = tw * .28f
    back += SPath(p1, stroke = OL, sw = 2f, extraUnits = w1)
    back += SPath(p2, stroke = OL, sw = 2f, extraUnits = w2)
    if (s.plaza) back += SEllipse(mm.x, mm.y, tw * .5f, th * .5f, OL, OL, sw = 2f)
    back += SPath(p1, stroke = Art.Cream, sw = 0f, extraUnits = w1)
    back += SPath(p2, stroke = Art.Cream, sw = 0f, extraUnits = w2)
    if (s.plaza) back += SEllipse(mm.x, mm.y, tw * .5f, th * .5f, Art.Cream, stroke = null)

    // Pond, stepping stones
    for (i in 0 until n) for (j in 0 until n) if (map[j][i] == "w") { val p = s.center(i.toFloat(), j.toFloat()); back += pond(p.x, p.y, k) }
    s.stones.forEach { (u, v) -> val p = s.q(u, v); back += stone(p.x, p.y, k) }

    // Edit grid (inside the island only)
    val grid = mutableListOf<SvgEl>()
    if (s.edit) for (i in 0 until n) for (j in 0 until n) {
        val p = s.center(i.toFloat(), j.toFloat())
        val dia = "M${f2(p.x)} ${f2(p.y - th / 2)} L${f2(p.x + tw / 2)} ${f2(p.y)} L${f2(p.x)} ${f2(p.y + th / 2)} L${f2(p.x - tw / 2)} ${f2(p.y)}Z"
        grid += SPath(dia, stroke = OL, sw = .75f, alpha = .32f, dash = floatArrayOf(4f, 8f))
        when (s.marks[i to j]) {
            TileMark.Ok -> grid += SPath(dia, Art.OkFill, Art.OkLine)
            TileMark.No -> grid += SPath(dia, Art.NoFill, Art.NoLine)
            null -> {}
        }
    }

    // Objects: map characters + separately placed objects + fences + small decorations
    data class O(val i: Float, val j: Float, val els: List<SvgEl>)
    val objs = mutableListOf<O>()
    val taken = HashSet<Pair<Int, Int>>()
    for (i in 0 until n) for (j in 0 until n) {
        val ch = map[j][i]
        val p = s.center(i.toFloat(), j.toFloat())
        val els = when (ch) {
            "t" -> tree(p.x, p.y, k, (i + j) % 2 == 1)
            "f" -> flowerPatch(p.x, p.y, k)
            "F" -> fountain(p.x, p.y, k)
            "h" -> house(s.houses[i to j] ?: ResidentKind.Rain, p.x, p.y, k * 1.1f)
            else -> null
        }
        if (els != null) { objs += O(i.toFloat(), j.toFloat(), els); taken += i to j }
    }
    s.extras.filter { it.type != ObjType.Resident }.forEach { o ->
        val p = s.center(o.i.toFloat(), o.j.toFloat())
        val els = objectElements(o.type, o.kind, k, (o.i + o.j) % 2 == 1, tw)
        objs += O(o.i.toFloat(), o.j.toFloat(), listOf(SGroup(els, tx = p.x, ty = p.y, alpha = if (o.ghost) .55f else 1f, flipX = o.flip)))
        if (!o.ghost) taken += o.i to o.j
    }
    s.fences.forEach { f ->
        val fi = (f.u0 + f.u1) / 2 - .5f; val fj = (f.v0 + f.v1) / 2 - .5f
        objs += O(fi, fj, fence(s.q(f.u0, f.v0), s.q(f.u1, f.v1), k))
        taken += fi.roundToInt() to fj.roundToInt()
    }
    // Leave resident spots empty
    s.extras.filter { it.type == ObjType.Resident }.forEach { taken += it.i to it.j }
    val dens = s.deco ?: if (s.edit) .28f else .42f
    for (i in 0 until n) for (j in 0 until n) {
        if (map[j][i] != "g" || (i to j) in taken) continue
        if (hash(i, j, 1) > dens) continue
        val di = i + (hash(i, j, 2) - .5f) * .5f; val dj = j + (hash(i, j, 3) - .5f) * .5f
        val p = s.center(di, dj)
        objs += O(di, dj, deco(p.x, p.y, k, floor(hash(i, j, 4) * 3).toInt()))
    }
    return VillageStatic(back, gd.toString(), grid, objs.map { Triple(it.i + it.j, it.i, it.els) })
}

/** One resident (shadow under the feet + body). 'res' in mockups village(). */
private fun residentAt(spec: VillageSpec, o: VillageObject): List<SvgEl> {
    val p = spec.center(o.i + o.di, o.j + o.dj)
    val tw = spec.tw
    val sz = tw * .68f
    val shadow: SvgEl = if (o.selected) SEllipse(p.x, p.y, tw * .27f, tw * .1f, Art.Peri)
    else SEllipse(p.x, p.y, tw * .2f, tw * .07f, Art.Sage2, stroke = null)
    val kind = o.kind ?: ResidentKind.Rain
    return listOf(shadow, SGroup(residentElements(kind, false), tx = p.x - sz / 2, ty = p.y - sz * .9f - o.lift, s = sz / 100f))
}

/**
 * Draws the whole village. This DrawScope is already transformed to mockup coordinates (1 unit = [unitPx] px).
 * @param linePx Stroke width (px), usually 2dp.
 */
fun DrawScope.drawVillage(spec: VillageSpec, measurer: TextMeasurer, unitPx: Float, linePx: Float, night: Boolean) {
    val st = staticOf(spec, night)
    drawEls(st.back, unitPx, linePx)
    if (st.grid.isNotEmpty()) clipPath(parsePath(st.grassClip)) { drawEls(st.grid, unitPx, linePx) }

    val residents = spec.extras.filter { it.type == ObjType.Resident }
        .map { Triple(it.i + it.di + it.j + it.dj, it.i + it.di, residentAt(spec, it)) }
    (st.objs + residents).sortedWith(compareBy({ it.first }, { it.second })).forEach { drawEls(it.third, unitPx, linePx) }

    spec.bubbles.forEach { b -> drawMapBubble(spec, b, measurer, unitPx, linePx) }
}

/** Speech bubble in the village. mockups bubble() + layoutBubbles(). Height 40, text Jua 18 (mockup units). */
private fun DrawScope.drawMapBubble(spec: VillageSpec, b: Bubble, measurer: TextMeasurer, unitPx: Float, linePx: Float) {
    val o = b.style
    val p = spec.center(b.i.toFloat(), b.j.toFloat())
    val cx = p.x + o.dx; val by = p.y - o.by; val ax = p.x + o.ax; val ay = p.y - o.ay
    val layout = measurer.measure(b.text, TextStyle(fontFamily = Jua, fontSize = unitsToSp(18f * unitPx), color = Ui.Ink))
    val tw = layout.size.width / unitPx
    val w = tw + 36f; val h = 40f
    val path = bubblePath(cx - w / 2, by - h, cx + w / 2, by, ax, ay, if (o.side) TailSide.Left else TailSide.Bottom, 12f, 8f)
    drawPath(path, Ui.White)
    drawPath(path, Ui.Ol, style = Stroke(linePx / unitPx, cap = StrokeCap.Round, join = StrokeJoin.Round))
    translateAndScale(0f, 0f, 1f / unitPx) {
        drawText(layout, topLeft = Offset((cx - tw / 2) * unitPx, (by - h / 2) * unitPx - layout.size.height / 2f))
    }
}

/**
 * Fits the village centered in the box (xMidYMid meet). (scale, dx, dy)
 * At mockup proportions (wide 1152×748, narrow 571×748) this is the same as filling; in windows with other proportions the island is not cropped.
 */
fun sliceFit(spec: VillageSpec, cw: Float, ch: Float): Triple<Float, Float, Float> {
    val k = minOf(cw / spec.w, ch / spec.h)
    return Triple(k, (cw - spec.w * k) / 2, (ch - spec.h * k) / 2)
}

