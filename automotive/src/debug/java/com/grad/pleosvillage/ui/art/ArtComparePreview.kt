package com.grad.pleosvillage.ui.art

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.grad.pleosvillage.R

/*
 * Preview showing the mockup PNG and the Canvas drawing side by side (debug builds only).
 * The mockup PNG is a 1600×900 screen captured at 1.5x (2400×1350); the app area is 1152×748 at (432, 56).
 */

private const val MOCK_SCALE = 1.5f
private const val APP_X = 432
private const val APP_Y = 56

/** Crops the (x, y, w, h) region of the app area from the mockup PNG and draws it in this box. Coordinates are mockup units relative to the app area. */
@Composable
fun MockCrop(resId: Int, x: Int, y: Int, w: Int, h: Int, modifier: Modifier = Modifier) {
    val img = androidx.compose.ui.graphics.ImageBitmap.imageResource(resId)
    Canvas(modifier) {
        drawImage(
            img,
            srcOffset = IntOffset(((APP_X + x) * MOCK_SCALE).toInt(), ((APP_Y + y) * MOCK_SCALE).toInt()),
            srcSize = IntSize((w * MOCK_SCALE).toInt(), (h * MOCK_SCALE).toInt()),
            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
            filterQuality = FilterQuality.Medium,
        )
    }
}

@Preview(widthDp = 1160, heightDp = 380)
@Composable
private fun VillageCompare() {
    Row {
        VillageCanvas(MockVillageSpec, Modifier.size(576.dp, 374.dp))
        MockCrop(R.drawable.mock_screen2_village, 0, 0, 1152, 748, Modifier.size(576.dp, 374.dp))
    }
}

@Preview(widthDp = 520, heightDp = 260)
@Composable
private fun ResidentCompare() {
    // Compare at the same size as the rain resident card art area on the dex screen (approx.)
    Row {
        ResidentArt(ResidentKind.Rain, 240.dp)
        MockCrop(R.drawable.mock_screen3_dex, 225, 150, 110, 110, Modifier.size(240.dp))
    }
}

@Suppress("unused")
private fun DrawScope.unused() = Unit
