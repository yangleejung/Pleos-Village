package com.grad.pleosvillage.ui.design

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.grad.pleosvillage.R

/** Titles, resident names, speech bubbles. SIL OFL (docs/licenses/Jua-OFL.txt). */
val Jua = FontFamily(Font(R.font.jua))

/** Body text and numbers. SIL OFL (docs/licenses/Pretendard-OFL.txt). */
val Pretendard = FontFamily(
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_regular, FontWeight.Medium),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
    Font(R.font.pretendard_bold, FontWeight.Bold),
)

/** Text sizes. Values from the mockup (mockups.html). UI_SPEC §5. */
object TextSizes {
    val ScreenTitle = 36.sp
    val ArrivalTitle = 40.sp
    val BigNumber = 74.sp
    val ResidentName = 32.sp
    val CardName = 19.sp
    val Button = 19.sp
    val Value = 22.sp
    val Body = 17.sp
    val Desc = 15.sp
    val Label = 14.sp
    val Small = 13.sp
}

/** Jua style. Letter spacing slightly tightened as in the mockup. */
fun juaStyle(size: TextUnit) =
    TextStyle(fontFamily = Jua, fontSize = size, lineHeight = size * 1.3, letterSpacing = (-0.01).em)

/** Pretendard style. Default weight Medium (mockup 500). */
fun bodyStyle(size: TextUnit, weight: FontWeight = FontWeight.Medium) =
    TextStyle(fontFamily = Pretendard, fontWeight = weight, fontSize = size, lineHeight = size * 1.45, letterSpacing = (-0.01).em)

/** Numbers: tabular (fixed-width) digits. */
fun numberStyle(size: TextUnit, weight: FontWeight = FontWeight.Bold) =
    bodyStyle(size, weight).copy(fontFeatureSettings = "tnum", lineHeight = size * 1.2)
