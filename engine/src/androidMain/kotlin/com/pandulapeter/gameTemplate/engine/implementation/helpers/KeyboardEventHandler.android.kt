package com.pandulapeter.gameTemplate.engine.implementation.helpers

import android.app.Activity
import android.view.KeyEvent
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val DEBOUNCE_TIME_MILLIS = 70L

// TODO: Needs to be improved.
@Composable
internal actual fun rememberKeyboardEventHandler(
    onKeyPressed: (Key) -> Unit,
    onKeyReleased: (Key) -> Unit
): KeyboardEventHandler = object : KeyboardEventHandler {

    private val scope = rememberCoroutineScope()

    private val activity = LocalContext.current as Activity
    private val keyReleasedTimestamps = mutableMapOf<Key, Long>()
    private val keyListener = View.OnUnhandledKeyEventListener { _, event ->
        val key = Key(event.keyCode)
        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (keyReleasedTimestamps.containsKey(key)) {
                    keyReleasedTimestamps.remove(key)
                } else {
                    onKeyPressed(key)
                }
                true
            }

            KeyEvent.ACTION_UP -> {
                keyReleasedTimestamps[key] = System.currentTimeMillis()
                true
            }

            else -> true
        }
    }

    override fun startListening() {
        scope.launch {
            while (isActive) {
                delay(DEBOUNCE_TIME_MILLIS)
                if (keyReleasedTimestamps.isNotEmpty()) {
                    val currentTime = System.currentTimeMillis()
                    keyReleasedTimestamps
                        .filter { (_, releaseTime) -> currentTime - releaseTime > DEBOUNCE_TIME_MILLIS }
                        .map { it.key }
                        .let { releasedKeys ->
                            releasedKeys.forEach { key ->
                                onKeyReleased(key)
                                keyReleasedTimestamps.remove(key)
                            }
                        }
                }
            }
        }
        activity.window.decorView.rootView.addOnUnhandledKeyEventListener(keyListener)
    }

    override fun stopListening() = activity.window.decorView.rootView.removeOnUnhandledKeyEventListener(keyListener)
}