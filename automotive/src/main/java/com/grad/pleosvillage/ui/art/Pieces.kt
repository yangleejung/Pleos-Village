package com.grad.pleosvillage.ui.art

import androidx.compose.ui.graphics.Color
import com.grad.pleosvillage.ui.design.Art
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/*
 * Small art pieces. Ported as-is from mockups.html "작은 그림 조각" (star5, sparkle, bub, cloud, moonD, flower, leaf, drop, heart, hl, umbrella) and
 * face(). All return a list of shapes in SVG unit coordinates.
 */

private fun fx(v: Float) = f2(v)
private fun fx(v: Double) = f2(v)

fun star5(x: Float, y: Float, r: Float, fill: Color = Art.Butter): SvgEl {
    val sb = StringBuilder()
    for (q in 0 until 10) {
        val a = -PI / 2 + q * PI / 5
        val rr = if (q % 2 == 1) r * .5 else r.toDouble()
        sb.append(if (q == 0) "M" else "L").append(fx(x + rr * cos(a))).append(' ').append(fx(y + rr * sin(a)))
    }
    return SPath("${sb}Z", fill)
}

fun sparkle(x: Float, y: Float, r: Float, fill: Color = Art.White): SvgEl {
    val c = r * .16f
    return SPath(
        "M${fx(x)} ${fx(y - r)} Q${fx(x + c)} ${fx(y - c)} ${fx(x + r)} ${fx(y)} Q${fx(x + c)} ${fx(y + c)} ${fx(x)} ${fx(y + r)} " +
            "Q${fx(x - c)} ${fx(y + c)} ${fx(x - r)} ${fx(y)} Q${fx(x - c)} ${fx(y - c)} ${fx(x)} ${fx(y - r)}Z",
        fill,
    )
}

fun bub(x: Float, y: Float, r: Float): SvgEl = SCircle(x, y, r, Art.White)

private const val CLOUD_D = "M6 0 A8 8 0 0 1 8 -14 A11 11 0 0 1 26 -24 A13 13 0 0 1 50 -26 A11 11 0 0 1 68 -16 A10.5 10.5 0 0 1 80 0 Z"

fun cloud(x: Float, y: Float, k: Float): SvgEl = SGroup(listOf(SPath(CLOUD_D, Art.White)), tx = x, ty = y, s = k)

fun moonD(cx: Float, cy: Float, r: Float) =
    "M${fx(cx - r)} ${fx(cy)} A${fx(r)} ${fx(r)} 0 1 0 ${fx(cx)} ${fx(cy + r)} A${fx(r * .8f)} ${fx(r * .8f)} 0 1 1 ${fx(cx - r)} ${fx(cy)}Z"

fun moon(cx: Float, cy: Float, r: Float): SvgEl = SPath(moonD(cx, cy, r), Art.Butter)

fun flower(x: Float, y: Float, r: Float, fill: Color = Art.White, ctr: Color = Art.Lav): List<SvgEl> {
    val r0 = r * .5
    val pts = (0 until 5).map { q ->
        val a = -PI / 2 + PI / 5 + q * 2 * PI / 5
        Pair(x + r0 * cos(a), y + r0 * sin(a))
    }
    val ch = 2 * r0 * sin(PI / 5)
    val rl = fx(ch * .62)
    val sb = StringBuilder("M${fx(pts[0].first)} ${fx(pts[0].second)}")
    for (q in 1..5) {
        val p = pts[q % 5]
        sb.append(" A$rl $rl 0 1 1 ${fx(p.first)} ${fx(p.second)}")
    }
    return listOf(SPath("${sb}Z", fill), SCircle(x, y, r * .3f, ctr))
}

/** Leaf. From (x, y), length len in the direction of ang degrees. Computes the SVG translate + rotate per point. */
fun leaf(x: Float, y: Float, len: Float, ang: Float, fill: Color = Art.Mint2): SvgEl {
    val a = ang * PI / 180
    val ca = cos(a); val sa = sin(a)
    fun p(u: Double, v: Double) = "${fx(x + u * ca - v * sa)} ${fx(y + u * sa + v * ca)}"
    val l = len.toDouble()
    return SPath("M${p(0.0, 0.0)} Q${p(l * .5, -l * .36)} ${p(l, 0.0)} Q${p(l * .5, l * .36)} ${p(0.0, 0.0)}Z", fill)
}

fun drop(x: Float, y: Float, r: Float, fill: Color = Art.Baby): SvgEl = SPath(
    "M${fx(x)} ${fx(y - r * 1.9f)} C${fx(x + r * .5f)} ${fx(y - r * 1.1f)} ${fx(x + r)} ${fx(y - r * .5f)} ${fx(x + r)} ${fx(y)} " +
        "A${fx(r)} ${fx(r)} 0 0 1 ${fx(x - r)} ${fx(y)} C${fx(x - r)} ${fx(y - r * .5f)} ${fx(x - r * .5f)} ${fx(y - r * 1.1f)} ${fx(x)} ${fx(y - r * 1.9f)}Z",
    fill,
)

fun heart(x: Float, y: Float, s: Float, fill: Color = Art.Rose): SvgEl = SPath(
    "M${fx(x)} ${fx(y + 5 * s)} C${fx(x - 7 * s)} ${fx(y)} ${fx(x - 7 * s)} ${fx(y - 6 * s)} ${fx(x - 3 * s)} ${fx(y - 7 * s)} " +
        "C${fx(x - 1 * s)} ${fx(y - 7.5f * s)} ${fx(x)} ${fx(y - 6 * s)} ${fx(x)} ${fx(y - 4.5f * s)} " +
        "C${fx(x)} ${fx(y - 6 * s)} ${fx(x + 1 * s)} ${fx(y - 7.5f * s)} ${fx(x + 3 * s)} ${fx(y - 7 * s)} " +
        "C${fx(x + 7 * s)} ${fx(y - 6 * s)} ${fx(x + 7 * s)} ${fx(y)} ${fx(x)} ${fx(y + 5 * s)}Z",
    fill,
)

/** Small bright highlight on a round object (no outline). */
fun hl(cx: Float, cy: Float, rx: Float, ry: Float, rot: Float, op: Float = .8f): SvgEl =
    SEllipse(cx, cy, rx, ry, Art.White, stroke = null, alpha = op, rotateDeg = rot)

/** Umbrella canopy: top (ax, ay), width w, a different one of 4 pastel colors per panel. */
fun umbrella(ax: Float, ay: Float, w: Float, cols: List<Color>): List<SvgEl> {
    val by = ay + w * .48f
    val x0 = ax - w / 2
    val step = w / 4
    val rr = fx(step * .6f)
    return (0 until 4).map { q ->
        val l = fx(x0 + q * step)
        val r = fx(x0 + (q + 1) * step)
        val d = when (q) {
            0 -> "M${fx(ax)} ${fx(ay)} Q${fx(x0 + step * .12f)} ${fx(ay + w * .03f)} $l ${fx(by)} A$rr $rr 0 0 1 $r ${fx(by)} Z"
            3 -> "M${fx(ax)} ${fx(ay)} L$l ${fx(by)} A$rr $rr 0 0 1 $r ${fx(by)} Q${fx(x0 + w - step * .12f)} ${fx(ay + w * .03f)} ${fx(ax)} ${fx(ay)}Z"
            else -> "M${fx(ax)} ${fx(ay)} L$l ${fx(by)} A$rr $rr 0 0 1 $r ${fx(by)} Z"
        }
        SPath(d, cols[q])
    }
}

val UmbrellaColors = listOf(Art.Pink, Art.Butter, Art.Mint2, Art.Peri)

/** Dot eyes, small mouth, pink oval cheeks. Face center (x, y), size s. */
fun face(x: Float, y: Float, s: Float = 1f): List<SvgEl> = listOf(
    SEllipse(x - 7 * s, y, 2.6f * s, 3.2f * s, OL, stroke = null),
    SEllipse(x + 7 * s, y, 2.6f * s, 3.2f * s, OL, stroke = null),
    SPath("M${fx(x - 3 * s)} ${fx(y + 6 * s)} Q${fx(x)} ${fx(y + 9 * s)} ${fx(x + 3 * s)} ${fx(y + 6 * s)}"),
    SEllipse(x - 12.5f * s, y + 5 * s, 4.4f * s, 2.7f * s, Art.Blush, stroke = null),
    SEllipse(x + 12.5f * s, y + 5 * s, 4.4f * s, 2.7f * s, Art.Blush, stroke = null),
)
