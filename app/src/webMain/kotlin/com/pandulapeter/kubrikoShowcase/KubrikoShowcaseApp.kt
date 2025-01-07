package com.pandulapeter.kubrikoShowcase

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.CanvasBasedWindow
import kotlinx.browser.document
import org.w3c.dom.events.Event

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    CanvasBasedWindow(applyDefaultStyles = false) {
        val isInFullscreenMode = remember { mutableStateOf(false) }
        DisposableEffect(Unit) {
            val fullscreenListener: (Event) -> Unit = {
                if (isInFullscreenMode.value) {
                    isInFullscreenMode.value = document.fullscreenElement != null
                }
            }
            document.addEventListener(EVENT_FULLSCREEN_CHANGE, fullscreenListener)
            onDispose { document.removeEventListener(EVENT_FULLSCREEN_CHANGE, fullscreenListener) }
        }
        Box(
            modifier = Modifier.drawBehind {
                drawRect(
                    color = Color.Transparent,
                    size = size,
                    blendMode = BlendMode.Clear
                )
            }.fillMaxSize(),
        ) {
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
}

private const val EVENT_FULLSCREEN_CHANGE = "fullscreenchange"
