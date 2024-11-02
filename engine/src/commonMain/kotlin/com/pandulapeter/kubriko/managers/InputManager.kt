package com.pandulapeter.kubriko.managers

import androidx.compose.ui.input.key.Key
import kotlinx.coroutines.flow.SharedFlow

/**
 * TODO: Documentation
 */
interface InputManager {
    val activeKeys: SharedFlow<Set<Key>>
    val onKeyPressed: SharedFlow<Key>
    val onKeyReleased: SharedFlow<Key>

    fun isKeyPressed(key: Key) : Boolean
}