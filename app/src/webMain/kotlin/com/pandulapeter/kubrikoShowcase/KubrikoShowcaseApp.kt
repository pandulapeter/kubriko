package com.pandulapeter.kubrikoShowcase

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import kotlinx.browser.document
import org.w3c.dom.events.Event

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    CanvasBasedWindow(title = "Kubriko") {
        DisposableEffect(Unit) {
            val listener: (Event) -> Unit = {
                if (isInFullscreenMode.value) {
                    isInFullscreenMode.value = document.fullscreenElement != null
                }
            }
            document.addEventListener("fullscreenchange", listener)
            onDispose {
                document.removeEventListener("fullscreenchange", listener)
            }
        }
        KubrikoShowcase(
            isInFullscreenMode = isInFullscreenMode.value,
            onFullscreenModeToggled = {
                isInFullscreenMode.value = !isInFullscreenMode.value
                if (isInFullscreenMode.value) {
                    document.documentElement?.requestFullscreen()
                } else {
                    if (document.fullscreenElement != null) {
                        document.exitFullscreen()
                    }
                }
            },
        )
    }
}

private val isInFullscreenMode = mutableStateOf(false)