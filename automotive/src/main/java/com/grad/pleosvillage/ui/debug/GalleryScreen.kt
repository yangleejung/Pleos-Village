package com.grad.pleosvillage.ui.debug

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grad.pleosvillage.ui.art.ResidentArt
import com.grad.pleosvillage.ui.art.ResidentKind
import com.grad.pleosvillage.ui.art.VIcon
import com.grad.pleosvillage.ui.art.VillageIcon
import com.grad.pleosvillage.ui.art.drawEls
import com.grad.pleosvillage.ui.art.house
import com.grad.pleosvillage.ui.design.Brand
import com.grad.pleosvillage.ui.design.Dimens
import com.grad.pleosvillage.ui.design.LocalVillageColors
import com.grad.pleosvillage.ui.design.ScoreBar
import com.grad.pleosvillage.ui.design.SpeechBubble
import com.grad.pleosvillage.ui.design.StampCard
import com.grad.pleosvillage.ui.design.StatCard
import com.grad.pleosvillage.ui.design.TabPill
import com.grad.pleosvillage.ui.design.VButton
import com.grad.pleosvillage.ui.design.bodyStyle
import com.grad.pleosvillage.ui.design.juaStyle

/** Debug only: gathers design system components, residents and houses on one screen for checking on the emulator. (am start ... --es route debug/gallery) */
@Composable
fun GalleryScreen() {
    val c = LocalVillageColors.current
    Column(
        Modifier.fillMaxSize().background(c.bg).verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Brand()
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            VButton("닫기", {}, primary = false)
            VButton("마을로 데려가기", {}, icon = VIcon.Arrow, iconAfter = true)
            VButton("마을 꾸미기", {}, icon = VIcon.Brush)
            TabPill("전체", true, {}); TabPill("만남", false, {})
            SpeechBubble("11월 7일에 왔어요")
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ResidentKind.entries.take(6).forEach { ResidentArt(it, 120.dp) }
            ResidentArt(ResidentKind.Family, 120.dp, silhouette = true)
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(ResidentKind.Rain, ResidentKind.Night, ResidentKind.Family, ResidentKind.Winter).forEach { k ->
                Canvas(Modifier.size(150.dp)) {
                    val u = size.width / 150f
                    scale(u, u, Offset.Zero) { drawEls(house(k, 75f, 130f, 1.3f), u, Dimens.Outline.toPx()) }
                }
            }
            StampCard(ResidentKind.Rain.tint, Modifier.size(214.dp, 164.dp)) { ResidentArt(ResidentKind.Rain, 112.dp, Modifier.align(Alignment.Center)) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(Modifier.width(400.dp)) { ScoreBar(100, 1f); ScoreBar(70, 1f) }
            StatCard(listOf("운전 시간" to "1시간 50분", "포인트" to "+20"), Modifier.width(400.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { VIcon.entries.forEach { VillageIcon(it, 28.dp, c.fg) } }
        Text("Jua 36 주민 도감", style = juaStyle(36.sp), color = c.fg)
        Text("Pretendard 17 본문 글꼴입니다 1234567890", style = bodyStyle(17.sp), color = c.fg2)
    }
}
