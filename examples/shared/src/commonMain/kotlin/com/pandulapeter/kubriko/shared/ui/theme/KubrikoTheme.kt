package com.pandulapeter.kubriko.shared.ui.theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp

@Composable
fun KubrikoTheme(
    content: @Composable () -> Unit
) = MaterialTheme(
    colorScheme = if (isSystemInDarkTheme()) darkScheme else lightScheme,
    typography = KubrikoTypography(),
    content = {
        // Fade in the UI after the font is loaded
        AnimatedVisibility(
            visible = KubrikoTypography().bodyMedium.fontSize > 1.sp,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            content()
        }
    }
)

private val lightScheme = lightColorScheme(
    primary = KubrikoColors.brandPrimary,
    onPrimary = KubrikoColors.onBrandPrimary,
    primaryContainer = KubrikoColors.brandPrimary,
    onPrimaryContainer = KubrikoColors.onBrandPrimary,
    secondary = KubrikoColors.brandPrimary,
)

private val darkScheme = darkColorScheme(
    primary = KubrikoColors.brandPrimary,
    onPrimary = KubrikoColors.onBrandPrimary,
    primaryContainer = KubrikoColors.brandPrimary,
    onPrimaryContainer = KubrikoColors.onBrandPrimary,
    secondary = KubrikoColors.brandSecondary,
)