package com.pandulapeter.kubriko.keyboardInput.implementation

import android.app.Activity
import android.view.KeyEvent
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val DEBOUNCE_TIME_MILLIS = 70L

// TODO: Needs to be improved.
@Composable
internal actual fun createKeyboardEventHandler(
    onKeyPressed: (Key) -> Unit,
    onKeyReleased: (Key) -> Unit,
    coroutineScope: CoroutineScope,
): KeyboardEventHandler = object : KeyboardEventHandler {
    private val isActive = MutableStateFlow(false)
    private val keyReleasedTimestamps = mutableMapOf<Key, Long>()
    private val keyListener = View.OnUnhandledKeyEventListener { _, event ->
        event.toKey()?.let { key ->
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
        } ?: false
    }
    private var currentActivity = LocalContext.current as? Activity

    @Composable
    override fun isValid() = currentActivity === LocalContext.current

    init {
        isActive.onEach { isActive ->
            currentActivity?.let { activity ->
                if (isActive) {
                    activity.window.decorView.rootView.addOnUnhandledKeyEventListener(keyListener)
                } else {
                    activity.window.decorView.rootView.removeOnUnhandledKeyEventListener(keyListener)
                }
            }
        }.launchIn(coroutineScope)
    }

    override fun startListening() {
        coroutineScope.launch {
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
        isActive.update { true }
    }

    override fun stopListening() {
        isActive.update { false }
        currentActivity?.window?.decorView?.rootView?.removeOnUnhandledKeyEventListener(keyListener)
        currentActivity = null
    }
}

private fun KeyEvent.toKey() = keyMap[keyCode]

private val keyMap = mapOf(
    KeyEvent.KEYCODE_A to Key.A,
    KeyEvent.KEYCODE_B to Key.B,
    KeyEvent.KEYCODE_C to Key.C,
    KeyEvent.KEYCODE_D to Key.D,
    KeyEvent.KEYCODE_E to Key.E,
    KeyEvent.KEYCODE_F to Key.F,
    KeyEvent.KEYCODE_G to Key.G,
    KeyEvent.KEYCODE_H to Key.H,
    KeyEvent.KEYCODE_I to Key.I,
    KeyEvent.KEYCODE_J to Key.J,
    KeyEvent.KEYCODE_K to Key.K,
    KeyEvent.KEYCODE_L to Key.L,
    KeyEvent.KEYCODE_M to Key.M,
    KeyEvent.KEYCODE_N to Key.N,
    KeyEvent.KEYCODE_O to Key.O,
    KeyEvent.KEYCODE_P to Key.P,
    KeyEvent.KEYCODE_Q to Key.Q,
    KeyEvent.KEYCODE_R to Key.R,
    KeyEvent.KEYCODE_S to Key.S,
    KeyEvent.KEYCODE_T to Key.T,
    KeyEvent.KEYCODE_U to Key.U,
    KeyEvent.KEYCODE_V to Key.V,
    KeyEvent.KEYCODE_W to Key.W,
    KeyEvent.KEYCODE_X to Key.X,
    KeyEvent.KEYCODE_Y to Key.Y,
    KeyEvent.KEYCODE_Z to Key.Z,

    KeyEvent.KEYCODE_0 to Key.Zero,
    KeyEvent.KEYCODE_1 to Key.One,
    KeyEvent.KEYCODE_2 to Key.Two,
    KeyEvent.KEYCODE_3 to Key.Three,
    KeyEvent.KEYCODE_4 to Key.Four,
    KeyEvent.KEYCODE_5 to Key.Five,
    KeyEvent.KEYCODE_6 to Key.Six,
    KeyEvent.KEYCODE_7 to Key.Seven,
    KeyEvent.KEYCODE_8 to Key.Eight,
    KeyEvent.KEYCODE_9 to Key.Nine,

    KeyEvent.KEYCODE_ENTER to Key.Enter,
    KeyEvent.KEYCODE_ESCAPE to Key.Escape,
    KeyEvent.KEYCODE_DEL to Key.Backspace,
    KeyEvent.KEYCODE_TAB to Key.Tab,
    KeyEvent.KEYCODE_SPACE to Key.Spacebar,
    KeyEvent.KEYCODE_MINUS to Key.Minus,
    KeyEvent.KEYCODE_EQUALS to Key.Equals,
    KeyEvent.KEYCODE_LEFT_BRACKET to Key.LeftBracket,
    KeyEvent.KEYCODE_RIGHT_BRACKET to Key.RightBracket,
    KeyEvent.KEYCODE_BACKSLASH to Key.Backslash,
    KeyEvent.KEYCODE_SEMICOLON to Key.Semicolon,
    KeyEvent.KEYCODE_APOSTROPHE to Key.Apostrophe,
    KeyEvent.KEYCODE_COMMA to Key.Comma,
    KeyEvent.KEYCODE_PERIOD to Key.Period,
    KeyEvent.KEYCODE_SLASH to Key.Slash,

    KeyEvent.KEYCODE_F1 to Key.F1,
    KeyEvent.KEYCODE_F2 to Key.F2,
    KeyEvent.KEYCODE_F3 to Key.F3,
    KeyEvent.KEYCODE_F4 to Key.F4,
    KeyEvent.KEYCODE_F5 to Key.F5,
    KeyEvent.KEYCODE_F6 to Key.F6,
    KeyEvent.KEYCODE_F7 to Key.F7,
    KeyEvent.KEYCODE_F8 to Key.F8,
    KeyEvent.KEYCODE_F9 to Key.F9,
    KeyEvent.KEYCODE_F10 to Key.F10,
    KeyEvent.KEYCODE_F11 to Key.F11,
    KeyEvent.KEYCODE_F12 to Key.F12,

    KeyEvent.KEYCODE_SHIFT_LEFT to Key.ShiftLeft,
    KeyEvent.KEYCODE_SHIFT_RIGHT to Key.ShiftRight,
    KeyEvent.KEYCODE_CTRL_LEFT to Key.CtrlLeft,
    KeyEvent.KEYCODE_CTRL_RIGHT to Key.CtrlRight,
    KeyEvent.KEYCODE_ALT_LEFT to Key.AltLeft,
    KeyEvent.KEYCODE_ALT_RIGHT to Key.AltRight,
    KeyEvent.KEYCODE_META_LEFT to Key.MetaLeft,
    KeyEvent.KEYCODE_META_RIGHT to Key.MetaRight,

    KeyEvent.KEYCODE_CAPS_LOCK to Key.CapsLock,
    KeyEvent.KEYCODE_NUM_LOCK to Key.NumLock,
    KeyEvent.KEYCODE_SCROLL_LOCK to Key.ScrollLock,
    KeyEvent.KEYCODE_INSERT to Key.Insert,
    KeyEvent.KEYCODE_FORWARD_DEL to Key.Delete,
    KeyEvent.KEYCODE_MOVE_HOME to Key.MoveHome,
    KeyEvent.KEYCODE_MOVE_END to Key.MoveEnd,
    KeyEvent.KEYCODE_PAGE_UP to Key.PageUp,
    KeyEvent.KEYCODE_PAGE_DOWN to Key.PageDown,
    KeyEvent.KEYCODE_DPAD_UP to Key.DirectionUp,
    KeyEvent.KEYCODE_DPAD_DOWN to Key.DirectionDown,
    KeyEvent.KEYCODE_DPAD_LEFT to Key.DirectionLeft,
    KeyEvent.KEYCODE_DPAD_RIGHT to Key.DirectionRight,

    KeyEvent.KEYCODE_NUMPAD_0 to Key.NumPad0,
    KeyEvent.KEYCODE_NUMPAD_1 to Key.NumPad1,
    KeyEvent.KEYCODE_NUMPAD_2 to Key.NumPad2,
    KeyEvent.KEYCODE_NUMPAD_3 to Key.NumPad3,
    KeyEvent.KEYCODE_NUMPAD_4 to Key.NumPad4,
    KeyEvent.KEYCODE_NUMPAD_5 to Key.NumPad5,
    KeyEvent.KEYCODE_NUMPAD_6 to Key.NumPad6,
    KeyEvent.KEYCODE_NUMPAD_7 to Key.NumPad7,
    KeyEvent.KEYCODE_NUMPAD_8 to Key.NumPad8,
    KeyEvent.KEYCODE_NUMPAD_9 to Key.NumPad9,
    KeyEvent.KEYCODE_NUMPAD_MULTIPLY to Key.NumPadMultiply,
    KeyEvent.KEYCODE_NUMPAD_ADD to Key.NumPadAdd,
    KeyEvent.KEYCODE_NUMPAD_SUBTRACT to Key.NumPadSubtract,
    KeyEvent.KEYCODE_NUMPAD_DOT to Key.NumPadDot,
    KeyEvent.KEYCODE_NUMPAD_DIVIDE to Key.NumPadDivide
)