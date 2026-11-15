package com.grad.pleosvillage.ui.onboarding

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grad.pleosvillage.R
import com.grad.pleosvillage.data.FakeRepository
import com.grad.pleosvillage.ui.art.ResidentArt
import com.grad.pleosvillage.ui.art.ResidentKind
import com.grad.pleosvillage.ui.art.VIcon
import com.grad.pleosvillage.ui.art.VillageCanvas
import com.grad.pleosvillage.ui.art.VillageIcon
import com.grad.pleosvillage.ui.design.Brand
import com.grad.pleosvillage.ui.design.DesignScaled
import com.grad.pleosvillage.ui.design.Dimens
import com.grad.pleosvillage.ui.design.LocalVillageColors
import com.grad.pleosvillage.ui.design.LocalWidthClass
import com.grad.pleosvillage.ui.design.ScoreBar
import com.grad.pleosvillage.ui.design.TextLink
import com.grad.pleosvillage.ui.design.Ui
import com.grad.pleosvillage.ui.design.VButton
import com.grad.pleosvillage.ui.design.VCard
import com.grad.pleosvillage.ui.design.WidthClass
import com.grad.pleosvillage.ui.design.juaStyle
import com.grad.pleosvillage.ui.theme.PleosVillageTheme

/*
 * Onboarding (first launch, 3 pages). UI_SPEC 4.8.
 * 1. Automatic records / 2. 80+ points means a resident / 3. Residents gather into a village. [시작하기], skip.
 * Can be replayed from settings > "시작 안내 다시 보기".
 */

/** Remembers whether the app has been launched before (stored on the device only). */
object OnboardingPrefs {
    private const val FILE = "ui_prefs"
    private const val KEY = "onboarding_done"
    fun done(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(KEY, false)
    fun setDone(context: Context, done: Boolean) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putBoolean(KEY, done).apply()
}

private val PAGES = listOf(R.string.onboarding_1, R.string.onboarding_2, R.string.onboarding_3)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val c = LocalVillageColors.current
    val wide = LocalWidthClass.current == WidthClass.Wide
    var page by rememberSaveable { mutableIntStateOf(0) }
    val last = page == PAGES.lastIndex
    Box(Modifier.fillMaxSize().background(c.bg)) {
        Row(Modifier.fillMaxWidth().padding(start = 32.dp, end = 24.dp, top = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            Brand(Modifier.weight(1f))
            if (!last) TextLink(stringResource(R.string.onboarding_skip), onFinish)
        }
        Column(
            Modifier.align(Alignment.Center).widthIn(max = 720.dp).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            VCard(Modifier.fillMaxWidth().height(if (wide) 330.dp else 300.dp), color = Ui.Panel) {
                PageArt(page)
            }
            Text(
                stringResource(PAGES[page]), style = juaStyle(if (wide) 28.sp else 24.sp), color = c.fg,
                textAlign = TextAlign.Center, modifier = Modifier.padding(top = 24.dp),
            )
            Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PAGES.indices.forEach { i ->
                    // Current page is a long filled pill, others are hollow circles (not distinguished by color alone)
                    Box(
                        Modifier.size(width = if (i == page) 28.dp else 12.dp, height = 12.dp).clip(CircleShape)
                            .background(if (i == page) c.olOnBg else c.bg)
                            .border(Dimens.Outline, c.olOnBg, CircleShape),
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            VButton(
                stringResource(if (last) R.string.onboarding_start else R.string.onboarding_next),
                { if (last) onFinish() else page += 1 },
                icon = VIcon.Arrow, iconAfter = true,
            )
        }
    }
}

/** Art per page: 1 = residents resting instead of a car, 2 = 80-point bar and residents, 3 = a small village. */
@Composable
private fun PageArt(page: Int) {
    when (page) {
        0 -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Bottom) {
                ResidentArt(ResidentKind.Night, 120.dp)
                VillageIcon(VIcon.Clock, 64.dp, Ui.Ink)
                ResidentArt(ResidentKind.Winter, 120.dp)
            }
        }
        1 -> Column(Modifier.fillMaxSize().padding(horizontal = 48.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
                ResidentArt(ResidentKind.Rain, 130.dp, line = Dimens.BigArtOutline)
                ResidentArt(ResidentKind.Family, 130.dp, line = Dimens.BigArtOutline)
            }
            ScoreBar(92, 1f, Modifier.padding(top = 16.dp))
        }
        else -> VillageCanvas(FakeRepository.village.spec.copy(bubbles = emptyList(), sky = emptyList()), Modifier.fillMaxSize())
    }
}

@Preview(widthDp = 1152, heightDp = 748)
@Composable
private fun OnboardingPreview() = PleosVillageTheme(night = false) { DesignScaled { OnboardingScreen {} } }

@Preview(widthDp = 571, heightDp = 748)
@Composable
private fun OnboardingNarrowPreview() = PleosVillageTheme(night = false) { DesignScaled { OnboardingScreen {} } }
