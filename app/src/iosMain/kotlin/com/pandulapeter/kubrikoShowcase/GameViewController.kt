package com.pandulapeter.kubrikoShowcase

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.window.ComposeUIViewController

fun GameViewController() = ComposeUIViewController {
    KubrikoShowcase(
        isInFullscreenMode = isInFullscreenMode.value,
        onFullscreenModeToggled = { isInFullscreenMode.value = !isInFullscreenMode.value },
    )
}

private val isInFullscreenMode = mutableStateOf(false)