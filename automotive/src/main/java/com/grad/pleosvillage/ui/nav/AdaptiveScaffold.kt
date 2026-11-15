package com.grad.pleosvillage.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.grad.pleosvillage.R
import com.grad.pleosvillage.ui.art.VIcon
import com.grad.pleosvillage.ui.design.BottomTabItem
import com.grad.pleosvillage.ui.design.Dimens
import com.grad.pleosvillage.ui.design.LocalVillageColors
import com.grad.pleosvillage.ui.design.LocalWidthClass
import com.grad.pleosvillage.ui.design.NavItem
import com.grad.pleosvillage.ui.design.Ui
import com.grad.pleosvillage.ui.design.WidthClass

/** The 4 main tabs. UI_SPEC §2. */
enum class Tab(val route: String, val icon: VIcon, val label: Int) {
    Village("village", VIcon.House, R.string.nav_village),
    Dex("dex", VIcon.Book, R.string.nav_dex),
    Records("records", VIcon.Chart, R.string.nav_records),
    Settings("settings", VIcon.Gear, R.string.nav_settings),
}

/** Height of the bottom tab bar in the narrow layout. Screen content leaves this much space. */
val BottomBarHeight = 78.dp

/**
 * Wide layout: vertical menu on the left (mockup screens 2/3, only the selected item gets the sel background). Narrow layout: bottom tab bar (mockup screen 4, white background + top outline).
 * @param railTop Top position of the menu in the wide layout. Village 229dp, dex 189dp (mockup position minus the padding of the 76dp touch target).
 */
@Composable
fun AdaptiveScaffold(
    current: Tab,
    onSelect: (Tab) -> Unit,
    railTop: Dp = 236.dp,
    content: @Composable () -> Unit,
) {
    val c = LocalVillageColors.current
    Box(Modifier.fillMaxSize().background(c.bg)) {
        content()
        if (LocalWidthClass.current == WidthClass.Wide) {
            Column(Modifier.padding(start = 28.dp, top = railTop)) {
                Tab.entries.forEach { NavItem(it.icon, stringResource(it.label), it == current, { onSelect(it) }) }
            }
        } else {
            BottomTabBar(current, onSelect, Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun BottomTabBar(current: Tab, onSelect: (Tab) -> Unit, modifier: Modifier) {
    Row(
        modifier.fillMaxWidth().height(BottomBarHeight).background(LocalVillageColors.current.card)
            .drawBehind {
                val lw = Dimens.Outline.toPx()
                drawLine(Ui.Ol, Offset(0f, lw / 2), Offset(size.width, lw / 2), lw)
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Tab.entries.forEach { tab ->
            BottomTabItem(tab.icon, stringResource(tab.label), tab == current, { onSelect(tab) }, Modifier.weight(1f).fillMaxHeight())
        }
    }
}
