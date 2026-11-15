package com.grad.pleosvillage.ui.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.grad.pleosvillage.data.FakeRepository
import com.grad.pleosvillage.data.AppRepository
import com.grad.pleosvillage.ui.dex.ResidentCardDialog
import com.grad.pleosvillage.ui.model.ResidentStatus
import com.grad.pleosvillage.ui.model.ResidentUi
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.grad.pleosvillage.vehicle.NaviDataSource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grad.pleosvillage.R
import com.grad.pleosvillage.ui.art.VIcon
import com.grad.pleosvillage.ui.art.VillageIcon
import com.grad.pleosvillage.ui.design.Brand
import com.grad.pleosvillage.ui.design.Dimens
import com.grad.pleosvillage.ui.design.LocalVillageColors
import com.grad.pleosvillage.ui.design.ScreenTitle
import com.grad.pleosvillage.ui.design.Ui
import com.grad.pleosvillage.ui.design.VButton
import com.grad.pleosvillage.ui.design.VCard
import com.grad.pleosvillage.ui.design.bodyStyle
import com.grad.pleosvillage.ui.design.dashedTop

/*
 * Debug menu (debug build only). UI_SPEC 4.9, SPEC §8.
 * "Load demo data" goes at the top. Items that depend on the judging logic (stage 2) are still disabled.
 */

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DebugMenuScreen(
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onResetOnboarding: () -> Unit,
    onOpenDex: () -> Unit = {},
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    fun toast(msg: String) = android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
    // Temporary: until the judging logic (stage 2), the acquisition card is opened here (cycling through residents).
    var acquired by remember { mutableStateOf<ResidentUi?>(null) }
    var next by remember { mutableIntStateOf(0) }
    val metList = FakeRepository.dex.residents.filter { it.status != ResidentStatus.ComingSoon }
    acquired?.let { r ->
        ResidentCardDialog(r, onDismiss = { acquired = null }, acquiredOn = "11월 14일", onOpenDex = { acquired = null; onOpenDex() })
    }
    val c = LocalVillageColors.current
    Column(Modifier.fillMaxSize().background(c.bg).verticalScroll(rememberScrollState()).padding(horizontal = 44.dp, vertical = 30.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Brand(Modifier.weight(1f))
            VButton(stringResource(R.string.close), onBack, primary = false)
        }
        ScreenTitle(stringResource(R.string.debug_title), Modifier.padding(top = 8.dp))
        Text(stringResource(R.string.debug_desc), style = bodyStyle(15.sp), color = c.fg2, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))

        VCard(Modifier.fillMaxWidth()) {
            Column {
                // Items to wire up in stage 2 (judging logic). Order follows the SPEC §8 debug menu.
                // SPEC §8 debug menu. "Load demo data" goes at the top.
                val engine by AppRepository.state.collectAsState()
                val demoDone = stringResource(R.string.debug_demo_done)
                MenuRow(stringResource(R.string.debug_demo), null, { AppRepository.loadDemo(); toast(demoDone) }, first = true)
                MenuRow(stringResource(R.string.debug_time_scale), "${engine.settings.timeScale}×", { AppRepository.setTimeScale(if (engine.settings.timeScale == 60) 1 else 60) })
                MenuRow(stringResource(R.string.debug_speed_filter), stringResource(if (engine.settings.speedFilter) R.string.debug_on else R.string.debug_off), { AppRepository.setSpeedFilter(!engine.settings.speedFilter) })
                MenuRow(stringResource(R.string.debug_end_trip), engine.trip?.let { "운행 중 · ${it.drivingMs / 60000}분" } ?: "운행 없음", { AppRepository.endTrip() })
                MenuRow(stringResource(R.string.debug_reset_today), "오늘 ${engine.judgeCount[AppRepository.today()] ?: 0}번", { AppRepository.resetToday() })
                MenuRow(stringResource(R.string.debug_reset_all), null, { AppRepository.resetAll() })
                MenuRow(stringResource(R.string.debug_acquired), null, { acquired = metList[next % metList.size]; next++ })
                MenuRow(stringResource(R.string.debug_diag), null, { onOpen("debug/diag") })
                MenuRow(stringResource(R.string.debug_gallery), null, { onOpen("debug/gallery") })
                MenuRow(stringResource(R.string.debug_onboarding_reset), null, onResetOnboarding)
                val naviDemo by NaviDataSource.demo.collectAsState()
                MenuRow(
                    stringResource(R.string.debug_navi_demo),
                    stringResource(if (naviDemo) R.string.debug_on else R.string.debug_off),
                    { NaviDataSource.setDemo(!naviDemo) },
                )
            }
        }
        Text(stringResource(R.string.debug_arrival), style = bodyStyle(16.sp, FontWeight.Bold), color = c.fg, modifier = Modifier.padding(top = 20.dp, bottom = 4.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("met", "below", "short", "limit", "none", "later").forEach { id ->
                VButton(id, { onOpen("arrival/$id") }, primary = false, height = 48.dp, fontSize = 16.sp)
            }
        }
    }
}

/** One menu row. If [onClick] is null, the row is disabled (not wired yet) and shows [note]. */
@Composable
private fun MenuRow(text: String, note: String?, onClick: (() -> Unit)?, first: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().then(if (first) Modifier else Modifier.dashedTop(Ui.CardLine))
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .heightIn(min = Dimens.MinTouch).padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = bodyStyle(16.sp, FontWeight.SemiBold), color = if (onClick != null) Ui.Ink else Ui.Ink2, modifier = Modifier.weight(1f))
        if (note != null) Text(note, style = bodyStyle(13.sp), color = Ui.Ink2)
        if (onClick != null) VillageIcon(VIcon.Chevron, 22.dp, Ui.Ink2)
    }
}

