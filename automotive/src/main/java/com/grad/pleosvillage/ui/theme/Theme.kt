package com.grad.pleosvillage.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.sp
import com.grad.pleosvillage.ui.design.DayColors
import com.grad.pleosvillage.ui.design.LocalVillageColors
import com.grad.pleosvillage.ui.design.NightColors
import com.grad.pleosvillage.ui.design.Ui
import com.grad.pleosvillage.ui.design.bodyStyle

/**
 * App theme. No dynamic color (uses the mockup colors as-is).
 * @param night System night mode. REFERENCES.md §2.
 */
@Composable
fun PleosVillageTheme(
    night: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val c = if (night) NightColors else DayColors
    val scheme = if (night) {
        darkColorScheme(
            primary = Ui.Btn, onPrimary = Ui.BtnFg, background = c.bg, onBackground = c.fg,
            surface = c.card, onSurface = Ui.Ink, surfaceContainerLow = c.card, outline = Ui.Ol,
        )
    } else {
        lightColorScheme(
            primary = Ui.Btn, onPrimary = Ui.BtnFg, background = c.bg, onBackground = c.fg,
            surface = c.card, onSurface = Ui.Ink, surfaceContainerLow = c.card, outline = Ui.Ol,
        )
    }
    val type = Typography(bodyLarge = bodyStyle(17.sp), bodyMedium = bodyStyle(15.sp), labelLarge = bodyStyle(15.sp))
    CompositionLocalProvider(LocalVillageColors provides c) {
        MaterialTheme(colorScheme = scheme, typography = type, content = content)
    }
}
