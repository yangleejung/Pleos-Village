package com.grad.pleosvillage.ui.art

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grad.pleosvillage.ui.design.Art
import com.grad.pleosvillage.ui.design.Dimens
import com.grad.pleosvillage.ui.design.Jua
import com.grad.pleosvillage.ui.design.StampTint
import com.grad.pleosvillage.ui.design.Ui

/**
 * Resident kind. Determines the name, body shape, and stamp inner color. REFERENCES §2 "주민별 몸 모양", SPEC 4.3 for names.
 * @param residentName Resident name (team decision, 2026-11-14)
 */
enum class ResidentKind(val tint: Color, val residentName: String) {
    Rain(StampTint.Rain, "레이니"),
    Night(StampTint.Night, "무니"),
    Family(StampTint.Family, "해삐"),
    Winter(StampTint.Winter, "스노이"),
    LongTrip(StampTint.Long, "저니"),
    Charge(StampTint.Charge, "재피"),
    /** Location residents (out of scope). Silhouette only. */
    Sea(StampTint.Locked, "웨이브"),
    Mountain(StampTint.Locked, "우디"),
}

/* Resident bodies. Ported as-is from mockups.html body(kind) (viewBox 100). */

private const val DROP = "M50 14 C58 30 78 44 78 64 C78 80 66 90 50 90 C34 90 22 80 22 64 C22 44 42 30 50 14Z"
private const val ROUND = "M22 62 C22 42 35 30 50 30 C65 30 78 42 78 62 C78 79 66 90 50 90 C34 90 22 79 22 62Z"

private fun body(kind: ResidentKind): List<SvgEl> = when (kind) {
    ResidentKind.Rain -> listOf(
        SPath("M82 33 V52 Q82 58 73 57"),
        SPath("M82 17 V11.5"),
        SPath(DROP, Art.Baby),
        hl(36f, 55f, 3.6f, 7f, 28f),
    ) + umbrella(82f, 17f, 32f, UmbrellaColors) + face(50f, 65f)
    ResidentKind.Night -> listOf(
        star5(87f, 21f, 4.6f),
        SPath(ROUND, Art.Lav),
        hl(35f, 46f, 5f, 3f, -35f, .7f),
        moon(67f, 25f, 11f),
    ) + face(50f, 63f)
    ResidentKind.Family -> listOf(
        SPath("M12 64 C12 46 24 36 39 36 C54 36 65 46 65 64 C65 80 54 89 39 89 C24 89 12 80 12 64Z", Art.Pink),
        hl(25f, 51f, 4.5f, 2.8f, -35f),
    ) + face(38f, 62f, .9f) + listOf(
        SPath("M57 73 C57 62 64 56 73 56 C82 56 89 62 89 73 C89 83 82 89 73 89 C64 89 57 83 57 73Z", Art.Peach),
    ) + face(73f, 73f, .62f) + heart(65f, 40f, .95f)
    ResidentKind.Winter -> listOf(
        sparkle(18f, 34f, 5.5f, Art.Baby),
        bub(84f, 40f, 2.8f),
        SCircle(50f, 60f, 28f, Art.White),
        SPath("M23.5 64 Q50 75 76.5 64 L76 72.5 Q50 83 24 72.5Z", Art.Mint2),
        SPath("M61 75 L67 91 L57.5 90 L54.5 77Z", Art.Mint2),
    ) + face(50f, 54f)
    ResidentKind.LongTrip -> listOf(
        SRect(21f, 47f, 17f, 28f, 6f, Art.Peri),
        SPath("M21 57 H37"),
        SPath("M36 32 C36 17 64 17 64 32 L66 77 C66 89 34 89 34 77Z", Art.Mint),
        hl(43f, 28f, 3.4f, 2.2f, -30f),
        SPath("M36.5 40 Q42 50 37 66"),
    ) + face(52f, 48f, .85f)
    ResidentKind.Charge -> listOf(
        SPath("M54 6 L44 25 L51 25 L46 39 L61 19 L53.5 19 L58 6Z", Art.Peach),
        SPath("M22 63 C22 45 35 35 50 35 C65 35 78 45 78 63 C78 79 66 90 50 90 C34 90 22 79 22 63Z", Art.Butter),
        hl(35f, 49f, 5f, 3f, -35f),
    ) + face(50f, 64f)
    // Location residents have no body yet (only the silhouette is shown).
    ResidentKind.Sea, ResidentKind.Mountain -> silhouette()
}

private fun silhouette(line: Color = OL): List<SvgEl> = listOf(SPath(ROUND, Art.Sil, line, dash = floatArrayOf(5f, 5f)))

private val bodyCache = HashMap<ResidentKind, List<SvgEl>>()
private val silCache = HashMap<Color, List<SvgEl>>()

/**
 * Resident art shapes (viewBox 100). A silhouette is a lavender body + dashed line (the "?" glyph is drawn by [drawResident]).
 * @param silLine Silhouette dashed-line color. A light line (#C9C8EA) when placed directly on the night background. REFERENCES §2.
 */
fun residentElements(kind: ResidentKind, silhouette: Boolean, silLine: Color = OL): List<SvgEl> =
    if (silhouette) synchronized(silCache) { silCache.getOrPut(silLine) { silhouette(silLine) } }
    else synchronized(bodyCache) { bodyCache.getOrPut(kind) { body(kind) } }

/**
 * Draws a resident of size sizePx at (left, top) of this DrawScope.
 * @param linePx Stroke width (px). Usually 2dp; 3dp for the large resident on the arrival summary.
 */
fun DrawScope.drawResident(
    kind: ResidentKind,
    silhouette: Boolean,
    left: Float,
    top: Float,
    sizePx: Float,
    linePx: Float,
    measurer: TextMeasurer?,
    alpha: Float = 1f,
    silLine: Color = OL,
) {
    val k = sizePx / 100f
    translateAndScale(left, top, k) {
        drawEls(residentElements(kind, silhouette, silLine), k, linePx, alpha)
        if (silhouette && measurer != null) {
            // mockups: <text x=50 y=71 font-size=26 text-anchor=middle>  (y is the text baseline)
            val layout = measurer.measure("?", TextStyle(fontFamily = Jua, fontSize = unitsToSp(26f * k) , color = Ui.Ink2))
            translateAndScale(0f, 0f, 1f / k) {
                drawText(
                    layout,
                    topLeft = Offset(50f * k - layout.size.width / 2f, 71f * k - layout.firstBaseline),
                    alpha = alpha,
                )
            }
        }
    }
}

/** Inside a DrawScope, the sp equal in size to "n in px". */
internal fun DrawScope.unitsToSp(px: Float) = (px / (density * fontScale)).sp

/** A single resident drawing. */
@Composable
fun ResidentArt(
    kind: ResidentKind,
    size: Dp,
    modifier: Modifier = Modifier,
    silhouette: Boolean = false,
    line: Dp = Dimens.Outline,
    /** Silhouette dashed-line color. Pass LocalVillageColors.olOnBg when placed directly on the night background. */
    silLine: Color = OL,
) {
    val measurer = rememberTextMeasurer()
    Canvas(modifier.size(size)) {
        drawResident(kind, silhouette, 0f, 0f, this.size.minDimension, line.toPx(), measurer.takeIf { silhouette }, silLine = silLine)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF6F6FE, widthDp = 1000, heightDp = 330)
@Composable
private fun ResidentsPreview() {
    Column(Modifier.padding(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ResidentKind.entries.take(6).forEach { ResidentArt(it, 130.dp) }
            ResidentArt(ResidentKind.Family, 130.dp, silhouette = true)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ResidentArt(ResidentKind.Rain, 170.dp, line = Dimens.BigArtOutline)
            ResidentArt(ResidentKind.Family, 170.dp, line = Dimens.BigArtOutline)
            ResidentArt(ResidentKind.Rain, 48.dp)
        }
    }
}
