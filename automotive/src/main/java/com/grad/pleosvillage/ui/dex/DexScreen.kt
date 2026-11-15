package com.grad.pleosvillage.ui.dex

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grad.pleosvillage.ui.village.KeepAwake
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grad.pleosvillage.R
import com.grad.pleosvillage.data.FakeRepository
import com.grad.pleosvillage.ui.art.ResidentArt
import com.grad.pleosvillage.ui.art.VIcon
import com.grad.pleosvillage.ui.design.Brand
import com.grad.pleosvillage.ui.design.DesignScaled
import com.grad.pleosvillage.ui.design.LocalVillageColors
import com.grad.pleosvillage.ui.design.LocalWidthClass
import com.grad.pleosvillage.ui.design.ScreenTitle
import com.grad.pleosvillage.ui.design.StampCard
import com.grad.pleosvillage.ui.design.StampTint
import com.grad.pleosvillage.ui.design.TabPill
import com.grad.pleosvillage.ui.design.TextLink
import com.grad.pleosvillage.ui.design.Ui
import com.grad.pleosvillage.ui.design.VButton
import com.grad.pleosvillage.ui.design.VCard
import com.grad.pleosvillage.ui.design.WidthClass
import com.grad.pleosvillage.ui.design.bodyStyle
import com.grad.pleosvillage.ui.design.juaStyle
import com.grad.pleosvillage.ui.model.DexUi
import com.grad.pleosvillage.ui.model.ResidentStatus
import com.grad.pleosvillage.ui.model.ResidentUi
import com.grad.pleosvillage.ui.nav.AdaptiveScaffold
import com.grad.pleosvillage.ui.nav.BottomBarHeight
import com.grad.pleosvillage.ui.nav.Tab
import com.grad.pleosvillage.ui.theme.PleosVillageTheme

/*
 * Resident dex. UI_SPEC 4.4/4.5. Mockup screen 3 (v4 stamp album).
 * Wide layout 4 columns, narrow layout 2 columns. 3 card types (met, not yet, coming soon). Tapping a card opens the resident big card (ResidentCardDialog).
 * The dex does not show odds or ceilings (decided 2026-11-14). The full public table is in settings > odds info.
 * Residents the vehicle doesn't support are removed from the list by FakeRepository / the real Repository.
 */

enum class DexFilter(val label: Int) {
    All(R.string.filter_all), Met(R.string.filter_met), NotYet(R.string.filter_not_yet), Soon(R.string.filter_soon)
}

/** Slightly different tilt per stamp (mockups ROT). */
private val ROT = floatArrayOf(-.8f, .5f, -.3f, .7f, .4f, -.6f, .9f, -.4f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DexScreen(ui: DexUi, onSelectTab: (Tab) -> Unit, onShowInVillage: (ResidentUi) -> Unit = {}) {
    val wide = LocalWidthClass.current == WidthClass.Wide
    val c = LocalVillageColors.current
    var filter by remember { mutableStateOf(DexFilter.All) }
    var detail by remember { mutableStateOf<ResidentUi?>(null) }
    val shown = ui.residents.filter {
        when (filter) {
            DexFilter.All -> true
            DexFilter.Met -> it.status == ResidentStatus.Met
            DexFilter.NotYet -> it.status == ResidentStatus.NotYet
            DexFilter.Soon -> it.status == ResidentStatus.ComingSoon
        }
    }

    AdaptiveScaffold(Tab.Dex, onSelectTab, railTop = 189.dp) {
        Box(Modifier.fillMaxSize()) {
            if (wide) Brand(Modifier.padding(start = 44.dp, top = 34.dp))
            Column(
                Modifier.fillMaxSize().padding(
                    start = if (wide) 174.dp else 20.dp, end = if (wide) 44.dp else 20.dp,
                    top = if (wide) 66.dp else 20.dp, bottom = if (wide) 0.dp else BottomBarHeight,
                ),
            ) {
                if (!wide) Brand(Modifier.padding(bottom = 6.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        ScreenTitle(stringResource(R.string.dex_title), size = if (wide) 36.sp else 28.sp)
                        Text(
                            stringResource(R.string.dex_desc, ui.available.size, ui.metCount),
                            style = bodyStyle(15.sp), color = c.fg2, modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    DexFilter.entries.forEach { f -> TabPill(stringResource(f.label), f == filter, { filter = f }) }
                }
                LazyVerticalGrid(
                    GridCells.Fixed(if (wide) 4 else 2),
                    horizontalArrangement = Arrangement.spacedBy(if (wide) 26.dp else 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 0.dp, bottom = 20.dp),
                ) {
                    items(shown, key = { it.id }) { r ->
                        DexStamp(r, ROT[ui.residents.indexOf(r) % ROT.size]) { if (r.status != ResidentStatus.ComingSoon) detail = r }
                    }
                }
            }
        }
    }

    detail?.let { r ->
        ResidentCardDialog(r, onDismiss = { detail = null }, onShowInVillage = { detail = null; onShowInVillage(r) })
    }
}

/** Stamp card + name, environment, status. Mockup stamp(). */
@Composable
private fun DexStamp(r: ResidentUi, rot: Float, onClick: () -> Unit) {
    val c = LocalVillageColors.current
    val met = r.status == ResidentStatus.Met
    val soon = r.status == ResidentStatus.ComingSoon
    Column(Modifier.rotate(rot).clickable(role = Role.Button, onClick = onClick)) {
        StampCard(if (met) r.kind.tint else StampTint.Locked, Modifier.fillMaxWidth().aspectRatio(214f / 164f)) {
            Text(r.no, style = bodyStyle(11.sp, FontWeight.Bold), color = Ui.Ink, modifier = Modifier.padding(start = 9.dp, top = 6.dp))
            Box(Modifier.fillMaxSize().padding(top = 8.dp).then(if (soon) Modifier.alpha(0.6f) else Modifier), contentAlignment = Alignment.Center) {
                if (met) ResidentArt(r.kind, 112.dp) else ResidentArt(r.kind, 96.dp, silhouette = true)
            }
        }
        Column(Modifier.padding(start = 4.dp, end = 4.dp, top = 10.dp)) {
            Text(r.name, style = juaStyle(19.sp), color = c.fg)
            Text(r.environment, style = bodyStyle(13.sp), color = c.fg2, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
            val (text, color) = when (r.status) {
                ResidentStatus.Met -> stringResource(R.string.dex_met_on, r.metDate ?: "") to c.ok
                ResidentStatus.NotYet -> stringResource(R.string.dex_not_yet) to c.fg2
                ResidentStatus.ComingSoon -> stringResource(R.string.coming_soon) to c.fg2
            }
            Text(text, style = bodyStyle(13.sp, FontWeight.Bold), color = color, modifier = Modifier.padding(top = 3.dp))
        }
    }
}

/* ---------------- Odds info sheet (light card background, ink text) ---------------- */

/** Odds info table. Shared by the dex and the settings "확률 정보". */
@Composable
fun OddsTable(ui: DexUi) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.odds_title), style = juaStyle(30.sp), color = Ui.Ink)
        Text(stringResource(R.string.dex_rule), style = bodyStyle(16.sp), color = Ui.Ink2)
        ui.available.forEach { r ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ResidentArt(r.kind, 56.dp, silhouette = r.status != ResidentStatus.Met)
                Text(r.name, style = juaStyle(20.sp), color = Ui.Ink, modifier = Modifier.width(140.dp))
                Text(r.environment, style = bodyStyle(16.sp), color = Ui.Ink2, modifier = Modifier.weight(1f))
                Text(stringResource(R.string.dex_odds, r.probabilityPct ?: 0, r.ceiling ?: 0), style = bodyStyle(16.sp, FontWeight.Bold), color = Ui.Ink)
            }
        }
        Text(stringResource(R.string.odds_note), style = bodyStyle(14.sp), color = Ui.Ink2)
        Spacer(Modifier.height(16.dp))
    }
}

/* ---------------- Previews ---------------- */

@Preview(widthDp = 1152, heightDp = 748)
@Composable
private fun DexWidePreview() = PleosVillageTheme(night = false) { DesignScaled { DexScreen(FakeRepository.dex, {}) } }

@Preview(widthDp = 1152, heightDp = 748)
@Composable
private fun DexWideNightPreview() = PleosVillageTheme(night = true) { DesignScaled { DexScreen(FakeRepository.dex, {}) } }

@Preview(widthDp = 571, heightDp = 748)
@Composable
private fun DexNarrowPreview() = PleosVillageTheme(night = false) { DesignScaled { DexScreen(FakeRepository.dex, {}) } }
