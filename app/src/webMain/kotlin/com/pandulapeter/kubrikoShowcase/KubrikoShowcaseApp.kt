package com.pandulapeter.kubrikoShowcase

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import kotlinx.browser.document
import org.w3c.dom.events.Event

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    CanvasBasedWindow(title = "Kubriko") {
        val isInFullscreenMode = remember { mutableStateOf(false) }
        DisposableEffect(Unit) {
            val fullscreenListener: (Event) -> Unit = {
                if (isInFullscreenMode.value) {
                    isInFullscreenMode.value = document.fullscreenElement != null
                }
            }
            val loadingScreen = document.querySelector(".loadingScreen")
            val loadingListener: (Event) -> Unit = {
                loadingScreen?.remove()
            }
            document.addEventListener(EVENT_FULLSCREEN_CHANGE, fullscreenListener)
            document.addEventListener(EVENT_LOADING_DONE, loadingListener)
            onDispose {
                document.removeEventListener(EVENT_FULLSCREEN_CHANGE, fullscreenListener)
                document.removeEventListener(EVENT_LOADING_DONE, loadingListener)
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

private const val EVENT_FULLSCREEN_CHANGE = "fullscreenchange"
private const val EVENT_LOADING_DONE = "load"
