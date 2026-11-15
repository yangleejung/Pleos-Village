package com.grad.pleosvillage.ui.art

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.grad.pleosvillage.ui.design.Dimens
import com.grad.pleosvillage.ui.design.LocalVillageColors
import com.grad.pleosvillage.ui.theme.PleosVillageTheme

/** Object placement for mockup screen 2 (village). mockups.html VOBJ, VSTONES. */
val MockVillageObjects = listOf(
    VillageObject(2, 2, ObjType.Resident, ResidentKind.Rain, selected = true),
    VillageObject(4, 2, ObjType.Resident, ResidentKind.Night),
    VillageObject(2, 5, ObjType.Resident, ResidentKind.Winter),
    VillageObject(5, 6, ObjType.Resident, ResidentKind.Family),
    VillageObject(6, 3, ObjType.Lamp),
    VillageObject(0, 2, ObjType.Lamp),
    VillageObject(2, 4, ObjType.Bench),
)
val MockFences = listOf(Fence(.9f, .25f, 2.8f, .25f), Fence(.25f, 4.1f, .25f, 5.7f))
val MockStones = listOf(4.15f to 1.65f, 4.6f to 1.55f, 5.05f to 1.6f)

/** The same village as mockup screen 2 (wide layout, 1152×748). */
val MockVillageSpec = VillageSpec(
    extras = MockVillageObjects, fences = MockFences, stones = MockStones,
    sky = listOf(
        SkyItem.Cloud(330f, 76f, 1f), SkyItem.Cloud(1000f, 222f, .8f), SkyItem.Cloud(985f, 612f, .8f),
        SkyItem.Star(480f, 118f, 7f), SkyItem.Star(1092f, 330f, 6f), SkyItem.Star(262f, 560f, 6f), SkyItem.Star(790f, 676f, 5f),
        SkyItem.Spark(566f, 62f, 9f), SkyItem.Spark(1070f, 470f, 8f), SkyItem.Spark(246f, 170f, 7f),
        SkyItem.Bub(1046f, 524f, 4f), SkyItem.Bub(1060f, 508f, 2.6f),
    ),
    moon = Triple(600f, 92f, 20f),
    ox = 660f, oy = 156f, tw = 110f,
    bubbleStyle = BubbleStyle(dx = 146f, by = 48f, ax = 26f, ay = 40f, side = true),
    bubbles = listOf(Bubble(2, 2, "11월 7일에 왔어요", BubbleStyle(dx = 146f, by = 48f, ax = 26f, ay = 40f, side = true))),
)

/** The same village as mockup screen 4 (narrow layout, 571×748). */
val MockVillageSpecNarrow = MockVillageSpec.copy(
    w = 571f,
    sky = listOf(
        SkyItem.Cloud(36f, 214f, .72f), SkyItem.Star(228f, 150f, 6f), SkyItem.Spark(470f, 170f, 7f),
        SkyItem.Star(520f, 476f, 5f), SkyItem.Spark(60f, 470f, 6f), SkyItem.Bub(500f, 250f, 3.5f),
    ),
    moon = Triple(262f, 126f, 15f),
    ox = 286f, oy = 206f, tw = 68f, deco = .4f,
    bubbleStyle = BubbleStyle(dx = 134f, by = 30f, ax = 14f, ay = 26f, side = true),
    bubbles = listOf(Bubble(2, 2, "11월 7일에 왔어요", BubbleStyle(dx = 134f, by = 30f, ax = 14f, ay = 26f, side = true))),
)

/**
 * Draws the village art filling this box (xMidYMid slice). The caller paints the background color.
 * @param zoom, pan User zoom and pan values (used on the village screen). pan is in screen px.
 */
@Composable
fun VillageCanvas(
    spec: VillageSpec,
    modifier: Modifier = Modifier,
    zoom: Float = 1f,
    pan: Offset = Offset.Zero,
    line: Dp = Dimens.Outline,
) {
    val measurer = rememberTextMeasurer()
    val night = LocalVillageColors.current.isNight
    Canvas(modifier.clipToBounds()) {
        val (k, dx, dy) = sliceFit(spec, size.width, size.height)
        val unit = k * zoom
        translate(dx + pan.x, dy + pan.y) {
            scale(unit, unit, pivot = Offset.Zero) {
                drawVillage(spec, measurer, unit, line.toPx(), night)
            }
        }
    }
}

@Preview(widthDp = 1152, heightDp = 748, showBackground = true, backgroundColor = 0xFFF6F6FE)
@Composable
private fun VillagePreview() = PleosVillageTheme(night = false) { VillageCanvas(MockVillageSpec, Modifier.fillMaxSize()) }

@Preview(widthDp = 1152, heightDp = 748, showBackground = true, backgroundColor = 0xFF2E3157)
@Composable
private fun VillageNightPreview() = PleosVillageTheme(night = true) { VillageCanvas(MockVillageSpec, Modifier.fillMaxSize()) }

@Preview(widthDp = 571, heightDp = 748, showBackground = true, backgroundColor = 0xFFF6F6FE)
@Composable
private fun VillageNarrowPreview() = PleosVillageTheme(night = false) { VillageCanvas(MockVillageSpecNarrow, Modifier.fillMaxSize()) }
