package com.pandulapeter.kubriko.shared.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.delay

@Composable
fun isFontFamilyLoaded(fontFamily: FontFamily): Boolean {
    val isFontConditionMet = !fontFamily.toString().contains("emptyFont")
    if (fontLoadingDelay == 0L) {
        return isFontConditionMet
    } else {
        val fontLoaded = remember { mutableStateOf(false) }
        LaunchedEffect(isFontConditionMet) {
            if (isFontConditionMet) {
                delay(fontLoadingDelay)
                fontLoaded.value = true
            }
        }
        return fontLoaded.value
    }
}

internal expect val fontLoadingDelay: Long