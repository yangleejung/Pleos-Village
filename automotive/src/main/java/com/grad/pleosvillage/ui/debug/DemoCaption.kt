package com.grad.pleosvillage.ui.debug

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grad.pleosvillage.ui.design.juaStyle

/*
 * Caption strip for the demo video (debug build only). tools/demo_video.sh changes it per scene.
 *   adb shell am start -n com.grad.pleosvillage/.MainActivity --es caption "운전하면 자동으로 기록돼요"
 * The first character picks the position: "^" top, "~" above the decorate drawer, anything else bottom. An empty string hides it.
 * Does not intercept touches (draw only).
 */
object DemoCaption {
    var text by mutableStateOf("")
}

@Composable
fun DemoCaptionOverlay() {
    val raw = DemoCaption.text
    val (align, pad, body) = when {
        raw.startsWith("^") -> Triple(Alignment.TopCenter, 18.dp, raw.drop(1))
        raw.startsWith("~") -> Triple(Alignment.BottomCenter, 262.dp, raw.drop(1))
        else -> Triple(Alignment.BottomCenter, 26.dp, raw)
    }
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = body.isNotBlank(),
            enter = fadeIn(tween(250)), exit = fadeOut(tween(250)),
            modifier = Modifier.align(align).padding(top = if (align == Alignment.TopCenter) pad else 0.dp, bottom = if (align != Alignment.TopCenter) pad else 0.dp),
        ) {
            Text(
                body,
                style = juaStyle(26.sp),
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 900.dp).clip(RoundedCornerShape(18.dp))
                    .background(Color(0xE62E2C42)).padding(horizontal = 26.dp, vertical = 12.dp),
            )
        }
    }
}
