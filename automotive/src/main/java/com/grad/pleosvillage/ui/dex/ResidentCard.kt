package com.grad.pleosvillage.ui.dex

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grad.pleosvillage.ui.design.ScaledDialog
import androidx.compose.ui.window.DialogProperties
import com.grad.pleosvillage.R
import com.grad.pleosvillage.data.FakeRepository
import com.grad.pleosvillage.ui.art.ResidentArt
import com.grad.pleosvillage.ui.art.SGroup
import com.grad.pleosvillage.ui.art.VIcon
import com.grad.pleosvillage.ui.art.drawEls
import com.grad.pleosvillage.ui.art.sparkle
import com.grad.pleosvillage.ui.art.star5
import com.grad.pleosvillage.ui.design.Art
import com.grad.pleosvillage.ui.design.DesignScaled
import com.grad.pleosvillage.ui.design.Dimens
import com.grad.pleosvillage.ui.design.LocalDrivingRestricted
import com.grad.pleosvillage.ui.design.LocalWidthClass
import com.grad.pleosvillage.ui.design.StampCard
import com.grad.pleosvillage.ui.design.StampTint
import com.grad.pleosvillage.ui.design.Ui
import com.grad.pleosvillage.ui.design.VButton
import com.grad.pleosvillage.ui.design.VCard
import com.grad.pleosvillage.ui.design.WidthClass
import com.grad.pleosvillage.ui.design.bodyStyle
import com.grad.pleosvillage.ui.design.dashedTop
import com.grad.pleosvillage.ui.design.juaStyle
import com.grad.pleosvillage.ui.model.ResidentStatus
import com.grad.pleosvillage.ui.model.ResidentUi
import com.grad.pleosvillage.ui.theme.PleosVillageTheme
import com.grad.pleosvillage.ui.village.KeepAwake

/*
 * Resident big card. Shows a large stamp card in the center of the screen.
 * - Tapping a resident in the dex: art, name, environment to meet it, date met (or "아직 못 만났어요"), [마을에서 보기][닫기].
 *   Odds and ceiling are not shown (decided 2026-11-14; they appear only in settings > odds info).
 * - When a new resident is met (acquisition card): "새 주민을 만났어요!" on top, the card pops in with 1–2 sparkles.
 *   Until the judging logic (stage 2) lands, it is opened by a temporary button on the village screen (debug build).
 * With driving restriction on, the final state is shown without animation.
 */

@Composable
fun ResidentCardDialog(
    r: ResidentUi,
    onDismiss: () -> Unit,
    /** [마을에서 보기] for a met resident. Hidden if null. */
    onShowInVillage: (() -> Unit)? = null,
    /** Date shown on the acquisition card (e.g. "11월 14일"). If null, this is the dex detail. */
    acquiredOn: String? = null,
    onOpenDex: (() -> Unit)? = null,
) {
    ScaledDialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        KeepAwake()
        val restricted = LocalDrivingRestricted.current
        val pop = remember(r.id) { Animatable(if (acquiredOn != null && !restricted) 0f else 1f) }
        LaunchedEffect(r.id) { if (pop.value < 1f) pop.animateTo(1f, tween(450, easing = EaseOutBack)) }
        val wide = LocalWidthClass.current == WidthClass.Wide
        Column(
            Modifier.widthIn(max = if (wide) 520.dp else 440.dp).fillMaxWidth().padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .graphicsLayer {
                    val p = pop.value
                    scaleX = 0.7f + 0.3f * p; scaleY = scaleX
                    alpha = p.coerceIn(0f, 1f)
                    transformOrigin = TransformOrigin(0.5f, 0.6f)
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (acquiredOn != null) {
                // The scrim behind the dialog is dark, so use light text
                Text(stringResource(R.string.card_acquired_title), style = juaStyle(34.sp), color = Ui.White, modifier = Modifier.padding(bottom = 12.dp))
            }
            ResidentBigCard(r, acquiredOn, sparkles = acquiredOn != null && pop.value > 0.7f)
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                VButton(stringResource(R.string.close), onDismiss, Modifier.weight(1f), primary = onShowInVillage == null && onOpenDex == null)
                if (onOpenDex != null) VButton(stringResource(R.string.card_open_dex), onOpenDex, Modifier.weight(1.4f), icon = VIcon.Book)
                else if (onShowInVillage != null && r.status == ResidentStatus.Met) {
                    VButton(stringResource(R.string.detail_show_in_village), onShowInVillage, Modifier.weight(1.4f), icon = VIcon.House)
                }
            }
        }
    }
}

/** Big card joining the stamp card (art) and the info card below into a single piece. */
@Composable
fun ResidentBigCard(r: ResidentUi, acquiredOn: String? = null, sparkles: Boolean = false) {
    val met = r.status == ResidentStatus.Met || acquiredOn != null
    VCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
        Column {
            Box {
                StampCard(if (met) r.kind.tint else StampTint.Locked, Modifier.fillMaxWidth().aspectRatio(1.35f), inset = 14.dp) {
                    Text(r.no, style = bodyStyle(14.sp, FontWeight.Bold), color = Ui.Ink, modifier = Modifier.padding(start = 12.dp, top = 9.dp))
                    Box(Modifier.fillMaxSize().padding(top = 18.dp, bottom = 8.dp), contentAlignment = Alignment.Center) {
                        ResidentArt(r.kind, 210.dp, silhouette = !met, line = if (met) Dimens.BigArtOutline else Dimens.Outline)
                    }
                }
                if (sparkles) {
                    // 1–2 sparkles (no big confetti or light rays)
                    Canvas(Modifier.matchParentSize()) {
                        val u = 1.dp.toPx()
                        drawEls(
                            listOf(
                                SGroup(listOf(sparkle(48f, 70f, 12f, Art.Butter)), s = u),
                                SGroup(listOf(star5(size.width / u - 56f, 92f, 8f)), s = u),
                            ),
                            1f, Dimens.Outline.toPx(),
                        )
                    }
                }
            }
            Column(Modifier.padding(start = 8.dp, end = 8.dp, top = 14.dp, bottom = 4.dp)) {
                Text(r.name, style = juaStyle(32.sp), color = Ui.Ink)
                Text(r.environment, style = bodyStyle(16.sp), color = Ui.Ink2, modifier = Modifier.padding(top = 2.dp))
                when {
                    acquiredOn != null -> InfoRow(stringResource(R.string.detail_met_day), acquiredOn, first = true)
                    r.status == ResidentStatus.Met -> {
                        InfoRow(stringResource(R.string.detail_met_day), r.metDate ?: "", first = true)
                        r.metStory?.let { Text(it, style = bodyStyle(15.sp), color = Ui.Ink2, modifier = Modifier.padding(top = 6.dp)) }
                    }
                    else -> InfoRow(stringResource(R.string.card_status), stringResource(R.string.dex_not_yet), first = true, color = Ui.Ink2)
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, first: Boolean = false, color: androidx.compose.ui.graphics.Color = Ui.Ink) {
    Row(
        Modifier.fillMaxWidth().padding(top = if (first) 10.dp else 0.dp).then(Modifier.dashedTop(Ui.CardLine)).padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = bodyStyle(15.sp), color = Ui.Ink2, modifier = Modifier.weight(1f))
        Text(value, style = bodyStyle(16.sp, FontWeight.Bold), color = color, textAlign = TextAlign.End)
    }
}

/* ---------------- Previews ---------------- */

@Preview(widthDp = 600, heightDp = 760, showBackground = true, backgroundColor = 0xFF55536B)
@Composable
private fun BigCardMetPreview() = PleosVillageTheme(night = false) {
    DesignScaled { Box(Modifier.padding(20.dp)) { ResidentBigCard(FakeRepository.rain) } }
}

@Preview(widthDp = 600, heightDp = 760, showBackground = true, backgroundColor = 0xFF55536B)
@Composable
private fun BigCardNotYetPreview() = PleosVillageTheme(night = false) {
    DesignScaled { Box(Modifier.padding(20.dp)) { ResidentBigCard(FakeRepository.longTrip) } }
}
