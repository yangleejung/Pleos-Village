package com.grad.pleosvillage.ui.art

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import com.grad.pleosvillage.ui.design.Art
import kotlin.math.roundToInt

/**
 * A small shape list for porting SVG pieces from mockups.html. Coordinates are in SVG units.
 *
 * Stroke width (REFERENCES §3): every drawing is filled, then stroked with round-capped lines of the same width (2dp).
 * Strokes do not get thicker when the drawing is scaled up (SVG vector-effect: non-scaling-stroke).
 * So [sw] is "a multiple of the base stroke width". Only when [scaleStroke] is true is it in SVG units (scales with the drawing).
 */
sealed interface SvgEl

/** Default outline color. */
val OL = Art.Ol

data class SPath(
    val d: String,
    val fill: Color? = null,
    val stroke: Color? = OL,
    val sw: Float = 1f,
    val alpha: Float = 1f,
    /** Dash pattern [on, off] as multiples of the stroke width (not px ratios at the 2dp base stroke). */
    val dash: FloatArray? = null,
    val roundCap: Boolean = true,
    val scaleStroke: Boolean = false,
    /** Width in SVG units added to the stroke width (thick lines that scale with the drawing, like paths and fence rails). */
    val extraUnits: Float = 0f,
) : SvgEl {
    val path: Path by lazy { parsePath(d) }
}

data class SCircle(
    val cx: Float, val cy: Float, val r: Float,
    val fill: Color? = null, val stroke: Color? = OL, val sw: Float = 1f, val alpha: Float = 1f,
) : SvgEl

data class SEllipse(
    val cx: Float, val cy: Float, val rx: Float, val ry: Float,
    val fill: Color? = null, val stroke: Color? = OL, val sw: Float = 1f, val alpha: Float = 1f,
    val rotateDeg: Float = 0f,
) : SvgEl

data class SRect(
    val x: Float, val y: Float, val w: Float, val h: Float, val rx: Float = 0f,
    val fill: Color? = null, val stroke: Color? = OL, val sw: Float = 1f, val alpha: Float = 1f,
) : SvgEl

/** A group of pieces, translated and scaled (SVG <g transform="translate scale">). */
data class SGroup(
    val children: List<SvgEl>,
    val tx: Float = 0f, val ty: Float = 0f, val s: Float = 1f,
    val alpha: Float = 1f,
    /** Horizontal flip (decorate's "돌리기"). Flips around (tx, ty). */
    val flipX: Boolean = false,
) : SvgEl

private val pathCache = HashMap<String, Path>()

/** SVG path string → Path. Each distinct string is parsed only once. */
fun parsePath(d: String): Path = synchronized(pathCache) {
    pathCache.getOrPut(d) { PathParser().parsePathString(d).toPath() }
}

/** Number for SVG coordinate strings (two decimal places, no exponent notation). mockups.html f2(). */
fun f2(v: Float): String {
    val r = (v * 100f).roundToInt()
    val neg = r < 0
    val a = kotlin.math.abs(r)
    val s = "${a / 100}" + when (val frac = a % 100) {
        0 -> ""
        else -> if (frac % 10 == 0) ".${frac / 10}" else ".${frac.toString().padStart(2, '0')}"
    }
    return if (neg && a != 0) "-$s" else s
}

fun f2(v: Double) = f2(v.toFloat())

private fun stroke(widthPx: Float, dash: FloatArray?, roundCap: Boolean) = Stroke(
    width = widthPx,
    cap = if (roundCap) StrokeCap.Round else StrokeCap.Butt,
    join = StrokeJoin.Round,
    pathEffect = dash?.let { d -> PathEffect.dashPathEffect(FloatArray(d.size) { d[it] * widthPx / 2f }) },
)

/**
 * Draws the shapes. This DrawScope is already in SVG unit coordinates, with 1 SVG unit = [unitPx] screen px.
 * @param linePx Base stroke width (screen px). Usually 2dp.
 */
fun DrawScope.drawEls(elements: List<SvgEl>, unitPx: Float, linePx: Float, alpha: Float = 1f) =
    elements.forEach { drawEl(it, alpha, unitPx, linePx) }

/** Draws shapes in viewBox (square) units, fitted to this DrawScope's size. */
fun DrawScope.drawSvg(elements: List<SvgEl>, viewBox: Float, linePx: Float) {
    val k = size.minDimension / viewBox
    scale(k, k, pivot = Offset.Zero) { drawEls(elements, k, linePx) }
}

private fun DrawScope.drawEl(el: SvgEl, parentAlpha: Float, unitPx: Float, linePx: Float) {
    fun w(sw: Float, scaled: Boolean = false) = if (scaled) sw else sw * linePx / unitPx
    when (el) {
        is SPath -> {
            val a = el.alpha * parentAlpha
            el.fill?.let { drawPath(el.path, it, alpha = a, style = Fill) }
            el.stroke?.let { drawPath(el.path, it, alpha = a, style = stroke(w(el.sw, el.scaleStroke) + el.extraUnits, el.dash, el.roundCap)) }
        }
        is SCircle -> {
            val a = el.alpha * parentAlpha
            el.fill?.let { drawCircle(it, el.r, Offset(el.cx, el.cy), alpha = a) }
            el.stroke?.let { drawCircle(it, el.r, Offset(el.cx, el.cy), alpha = a, style = Stroke(w(el.sw))) }
        }
        is SEllipse -> {
            val a = el.alpha * parentAlpha
            val draw: DrawScope.() -> Unit = {
                val tl = Offset(el.cx - el.rx, el.cy - el.ry)
                val sz = Size(el.rx * 2, el.ry * 2)
                el.fill?.let { drawOval(it, tl, sz, alpha = a) }
                el.stroke?.let { drawOval(it, tl, sz, alpha = a, style = Stroke(w(el.sw))) }
            }
            if (el.rotateDeg != 0f) rotate(el.rotateDeg, Offset(el.cx, el.cy), draw) else draw()
        }
        is SRect -> {
            val a = el.alpha * parentAlpha
            val cr = CornerRadius(el.rx, el.rx)
            el.fill?.let { drawRoundRect(it, Offset(el.x, el.y), Size(el.w, el.h), cr, alpha = a) }
            el.stroke?.let {
                drawRoundRect(it, Offset(el.x, el.y), Size(el.w, el.h), cr, alpha = a, style = Stroke(w(el.sw), join = StrokeJoin.Round))
            }
        }
        is SGroup -> translate(el.tx, el.ty) {
            scale(if (el.flipX) -el.s else el.s, el.s, pivot = Offset.Zero) {
                el.children.forEach { drawEl(it, parentAlpha * el.alpha, unitPx * el.s, linePx) }
            }
        }
    }
}

/** Draws in a coordinate system scaled by k at (x, y). */
internal inline fun DrawScope.translateAndScale(x: Float, y: Float, k: Float, crossinline block: DrawScope.() -> Unit) {
    translate(x, y) { scale(k, k, Offset.Zero) { block() } }
}
