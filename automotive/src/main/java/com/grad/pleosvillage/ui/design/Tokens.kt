package com.grad.pleosvillage.ui.design

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * UI color tokens (v4 pastel line art). docs/design/REFERENCES.md §2.
 * At night (system night mode) only the background and text on the background change. Cards stay light, and text inside cards ([ink], [ink2]) is the same as by day.
 */
@Immutable
data class VillageColors(
    /** Screen background */
    val bg: Color,
    /** Drawer and section background */
    val surface2: Color,
    /** Cards, speech bubbles (light even at night) */
    val card: Color,
    /** Outline placed directly on the background (a light line at night) */
    val olOnBg: Color,
    /** Titles, numbers, and body text on the background */
    val fg: Color,
    /** Descriptions on the background */
    val fg2: Color,
    /** 80-point pass / "만남" on the background */
    val ok: Color,
    /** Below 80 points / deductions on the background */
    val warn: Color,
    val isNight: Boolean,
) {
    /** Outline for all cards and buttons. Same day and night. */
    val ol get() = Ui.Ol
    /** Text on light cards. Same day and night. */
    val ink get() = Ui.Ink
    val ink2 get() = Ui.Ink2
    val btn get() = Ui.Btn
    val btnFg get() = Ui.BtnFg
    val sel get() = Ui.Sel
}

val DayColors = VillageColors(
    bg = Color(0xFFF6F6FE),
    surface2 = Color(0xFFECEDFB),
    card = Color(0xFFFFFFFF),
    olOnBg = Color(0xFF3D3B52),
    fg = Color(0xFF2E2C42),
    fg2 = Color(0xFF5B5873),
    ok = Color(0xFF2F7A5E),
    warn = Color(0xFFB5533C),
    isNight = false,
)

val NightColors = VillageColors(
    bg = Color(0xFF2E3157),
    surface2 = Color(0xFF383C68),
    card = Color(0xFFF7F6FE),
    olOnBg = Color(0xFFC9C8EA),
    fg = Color(0xFFF2F1FF),
    fg2 = Color(0xFFC9C8EA),
    ok = Color(0xFF9FE0C3),
    warn = Color(0xFFFFB7A3),
    isNight = true,
)

val LocalVillageColors = staticCompositionLocalOf { DayColors }

/** UI colors that are the same day and night. */
object Ui {
    val Ol = Color(0xFF3D3B52)
    /** Text on light cards. card #FFFFFF 13.5:1, #F7F6FE 12.6:1 */
    val Ink = Color(0xFF2E2C42)
    /** Descriptions on light cards. 6.8:1 */
    val Ink2 = Color(0xFF5B5873)
    /** Pass/fail text on light cards (cards are light even at night) */
    val CardOk = Color(0xFF2F7A5E)
    val CardWarn = Color(0xFFB5533C)
    /** Dashed divider inside cards */
    val CardLine = Color(0xFFE2E1F3)
    val Btn = Color(0xFFB9C3F7)
    val BtnFg = Color(0xFF2E2C42)
    val Sel = Color(0xFFC9CCF5)
    /** Background of the left art panel on the arrival summary */
    val Panel = Color(0xFFE3F2FC)
    val White = Color(0xFFFFFFFF)
}

/** Art colors. Same day and night (only the sky changes at night). REFERENCES §2, mockups.html ART. */
object Art {
    val Ol = Color(0xFF3D3B52)
    val White = Color(0xFFFFFFFF)
    val Cream = Color(0xFFFFF6E6)
    val Stone = Color(0xFFF4F2FB)
    val Pebble = Color(0xFFFFF3E6)
    val Peri = Color(0xFFC9CCF5)
    val Lav = Color(0xFFB9B3EE)
    val Baby = Color(0xFFBFE3F7)
    val Mint = Color(0xFFC8EBD7)
    val Mint2 = Color(0xFFA8DCC0)
    val Sage = Color(0xFFD6F0C9)
    val Sage2 = Color(0xFFBFE3B4)
    val Pink = Color(0xFFF9C9DC)
    val Butter = Color(0xFFFBEAA0)
    val Peach = Color(0xFFFAD3B8)
    val Rose = Color(0xFFF5A6C2)
    val Blush = Color(0xFFF6A5C0)
    val Trunk = Color(0xFFEECBA8)
    val Earth = Color(0xFFF6DEC9)
    val EarthD = Color(0xFFECCBB0)
    val OkFill = Color(0xCCA8DCC0)
    val OkLine = Color(0xFF2F7A5E)
    val NoFill = Color(0xBFF9BCB0)
    val NoLine = Color(0xFFB5533C)
    /** Silhouette of residents not yet met */
    val Sil = Color(0xFFE4E2FA)
}

/** Per-resident stamp inner colors. REFERENCES §2 table. */
object StampTint {
    val Rain = Color(0xFFDDF0FB)
    val Night = Color(0xFFE6E3FA)
    val Family = Color(0xFFFCE3EC)
    val Winter = Color(0xFFE6F0FA)
    val Long = Color(0xFFDFF3E7)
    val Charge = Color(0xFFFCF2CC)
    val Mix = Color(0xFFE7E8FB)
    val Locked = Color(0xFFEEEDFA)
}

/** Dimensions. UI_SPEC §5, REFERENCES §3. */
object Dimens {
    /** UI borders and art strokes */
    val Outline = 2.dp
    /** Stroke for large residents (arrival summary) */
    val BigArtOutline = 3.dp
    val Corner = 16.dp
    val ButtonHeight = 60.dp
    val MinTouch = 76.dp
}
