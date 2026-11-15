package com.grad.pleosvillage.ui.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grad.pleosvillage.ui.village.KeepAwake
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grad.pleosvillage.ui.design.ScaledDialog
import com.grad.pleosvillage.BuildConfig
import com.grad.pleosvillage.R
import com.grad.pleosvillage.data.FakeRepository
import com.grad.pleosvillage.ui.art.VIcon
import com.grad.pleosvillage.ui.art.VillageIcon
import com.grad.pleosvillage.ui.design.Art
import com.grad.pleosvillage.ui.design.Brand
import com.grad.pleosvillage.ui.design.DesignScaled
import com.grad.pleosvillage.ui.design.Dimens
import com.grad.pleosvillage.ui.design.LocalVillageColors
import com.grad.pleosvillage.ui.design.LocalWidthClass
import com.grad.pleosvillage.ui.design.ScreenTitle
import com.grad.pleosvillage.ui.design.Ui
import com.grad.pleosvillage.ui.design.VButton
import com.grad.pleosvillage.ui.design.VCard
import com.grad.pleosvillage.ui.design.WidthClass
import com.grad.pleosvillage.ui.design.bodyStyle
import com.grad.pleosvillage.ui.design.dashedTop
import com.grad.pleosvillage.ui.design.juaStyle
import com.grad.pleosvillage.ui.dex.OddsTable
import com.grad.pleosvillage.ui.model.CheckUi
import com.grad.pleosvillage.ui.model.DexUi
import com.grad.pleosvillage.ui.model.SettingsUi
import com.grad.pleosvillage.ui.nav.AdaptiveScaffold
import com.grad.pleosvillage.ui.nav.BottomBarHeight
import com.grad.pleosvillage.ui.nav.Tab
import com.grad.pleosvillage.ui.theme.PleosVillageTheme

/*
 * Settings. UI_SPEC 4.7.
 * - Items checked on this vehicle: list of supported/unsupported. Unsupported items say "이 차량에서는 확인하지 않아요".
 * - Odds info: full table of every resident's environment, odds and ceiling (same table as the dex).
 * - Data: "모든 기록은 이 차 안에만 저장돼요. 위치는 저장하지 않아요." [기록 지우기] (confirm dialog).
 * - Replay onboarding. Version (tap 7 times in a debug build for the debug menu).
 */

private const val DEBUG_TAPS = 7

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    ui: SettingsUi,
    dex: DexUi,
    onSelectTab: (Tab) -> Unit,
    onOnboarding: () -> Unit,
    onOpenDebug: () -> Unit,
) {
    val wide = LocalWidthClass.current == WidthClass.Wide
    val c = LocalVillageColors.current
    var showOdds by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }

    AdaptiveScaffold(Tab.Settings, onSelectTab, railTop = 189.dp) {
        Box(Modifier.fillMaxSize()) {
            if (wide) Brand(Modifier.padding(start = 44.dp, top = 34.dp))
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(
                    start = if (wide) 174.dp else 20.dp, end = if (wide) 44.dp else 20.dp,
                    top = if (wide) 66.dp else 20.dp, bottom = if (wide) 24.dp else BottomBarHeight + 16.dp,
                ),
            ) {
                if (!wide) Brand(Modifier.padding(bottom = 6.dp))
                ScreenTitle(stringResource(R.string.settings_title), size = if (wide) 36.sp else 28.sp)
                Spacer(Modifier.size(16.dp))
                if (wide) {
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        ChecksCard(ui.checks, Modifier.weight(1f))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            OddsCard { showOdds = true }
                            DataCard { confirmClear = true }
                            AboutCard(ui, onOnboarding, onOpenDebug)
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        ChecksCard(ui.checks, Modifier.fillMaxWidth())
                        OddsCard { showOdds = true }
                        DataCard { confirmClear = true }
                        AboutCard(ui, onOnboarding, onOpenDebug)
                    }
                }
            }
        }
    }

    if (showOdds) {
        ModalBottomSheet({ showOdds = false }, containerColor = c.card) { KeepAwake(); OddsTable(dex) }
    }
    if (confirmClear) {
        val context = LocalContext.current
        val msg = stringResource(R.string.settings_data_cleared)
        ConfirmDialog(
            title = stringResource(R.string.settings_data_confirm_title),
            body = stringResource(R.string.settings_data_confirm_body),
            ok = stringResource(R.string.settings_data_confirm_ok),
            onDismiss = { confirmClear = false },
            onOk = {
                confirmClear = false
                // Real storage arrives in stage 2 (judging logic). For now there are no records to clear.
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            },
        )
    }
}

/** Settings card with a title. */
@Composable
private fun SectionCard(title: String, desc: String?, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    VCard(modifier.fillMaxWidth(), contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 16.dp, bottom = 16.dp)) {
        Column {
            Text(title, style = juaStyle(21.sp), color = Ui.Ink)
            if (desc != null) Text(desc, style = bodyStyle(14.sp), color = Ui.Ink2, modifier = Modifier.padding(top = 4.dp))
            content()
        }
    }
}

@Composable
private fun ChecksCard(checks: List<CheckUi>, modifier: Modifier) {
    SectionCard(stringResource(R.string.settings_checks), stringResource(R.string.settings_checks_desc), modifier) {
        Spacer(Modifier.size(6.dp))
        checks.forEachIndexed { i, ch ->
            Row(
                Modifier.fillMaxWidth().then(if (i > 0) Modifier.dashedTop(Ui.CardLine) else Modifier).padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Support is shown with icon + text (not by color alone)
                Box(
                    Modifier.size(28.dp).clip(CircleShape).background(if (ch.supported) Art.Mint2 else Ui.White)
                        .border(Dimens.Outline, Ui.Ol, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { VillageIcon(if (ch.supported) VIcon.Check else VIcon.Close, 18.dp, Ui.Ink) }
                Column(Modifier.weight(1f)) {
                    Text(ch.name, style = bodyStyle(16.sp, FontWeight.Bold), color = Ui.Ink)
                    Text(
                        if (ch.supported) ch.use else stringResource(R.string.settings_check_off),
                        style = bodyStyle(13.sp), color = if (ch.supported) Ui.Ink2 else Ui.CardWarn,
                    )
                }
            }
        }
    }
}

@Composable
private fun OddsCard(onOpen: () -> Unit) {
    SectionCard(stringResource(R.string.settings_odds), stringResource(R.string.settings_odds_desc)) {
        VButton(stringResource(R.string.settings_odds_open), onOpen, primary = false, icon = VIcon.Book, height = 52.dp, fontSize = 17.sp)
    }
}

@Composable
private fun DataCard(onClear: () -> Unit) {
    SectionCard(stringResource(R.string.settings_data), stringResource(R.string.settings_data_desc)) {
        VButton(stringResource(R.string.settings_data_clear), onClear, primary = false, height = 52.dp, fontSize = 17.sp)
    }
}

/** Replay onboarding + version (tap 7 times in a debug build for the debug menu). */
@Composable
private fun AboutCard(ui: SettingsUi, onOnboarding: () -> Unit, onOpenDebug: () -> Unit) {
    val context = LocalContext.current
    var taps by remember { mutableIntStateOf(0) }
    VCard(Modifier.fillMaxWidth()) {
        Column {
            ListRow(stringResource(R.string.settings_onboarding), onOnboarding, chevron = true)
            ListRow(
                stringResource(R.string.settings_version, BuildConfig.VERSION_NAME.ifEmpty { ui.version }),
                onClick = {
                    if (!BuildConfig.DEBUG) return@ListRow
                    taps += 1
                    val left = DEBUG_TAPS - taps
                    when {
                        left <= 0 -> { taps = 0; onOpenDebug() }
                        left <= 3 -> Toast.makeText(context, context.getString(R.string.settings_debug_left, left), Toast.LENGTH_SHORT).show()
                    }
                },
                chevron = false, divider = true,
            )
        }
    }
}

@Composable
private fun ListRow(text: String, onClick: () -> Unit, chevron: Boolean, divider: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().then(if (divider) Modifier.dashedTop(Ui.CardLine) else Modifier)
            .clickable(role = Role.Button, onClick = onClick).heightIn(min = Dimens.MinTouch).padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = bodyStyle(16.sp, FontWeight.SemiBold), color = Ui.Ink, modifier = Modifier.weight(1f))
        if (chevron) VillageIcon(VIcon.Chevron, 22.dp, Ui.Ink2)
    }
}

/** Confirm dialog: light card + title + description + [취소][확인]. */
@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    ok: String,
    onDismiss: () -> Unit,
    onOk: () -> Unit,
    cancel: String = stringResource(R.string.cancel),
    /** The [취소] button. Tapping outside or pressing back always calls [onDismiss] (just closes the dialog). */
    onCancel: () -> Unit = onDismiss,
) {
    ScaledDialog(onDismissRequest = onDismiss) {
        KeepAwake()
        VCard(Modifier.widthIn(max = 520.dp), color = LocalVillageColors.current.card, contentPadding = PaddingValues(24.dp)) {
            Column {
                Text(title, style = juaStyle(24.sp), color = Ui.Ink)
                Text(body, style = bodyStyle(16.sp), color = Ui.Ink2, modifier = Modifier.padding(top = 8.dp))
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)) {
                    VButton(cancel, onCancel, primary = false)
                    VButton(ok, onOk)
                }
            }
        }
    }
}

/* ---------------- Previews ---------------- */

@Preview(widthDp = 1152, heightDp = 748)
@Composable
private fun SettingsWidePreview() = PleosVillageTheme(night = false) {
    DesignScaled { SettingsScreen(FakeRepository.settings, FakeRepository.dex, {}, {}, {}) }
}

@Preview(widthDp = 1152, heightDp = 748)
@Composable
private fun SettingsNightPreview() = PleosVillageTheme(night = true) {
    DesignScaled { SettingsScreen(FakeRepository.settings, FakeRepository.dex, {}, {}, {}) }
}

@Preview(widthDp = 571, heightDp = 748)
@Composable
private fun SettingsNarrowPreview() = PleosVillageTheme(night = false) {
    DesignScaled { SettingsScreen(FakeRepository.settings, FakeRepository.dex, {}, {}, {}) }
}
