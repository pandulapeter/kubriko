package com.pandulapeter.gameTemplate.engine.managers

import androidx.compose.ui.input.key.Key
import kotlinx.coroutines.flow.SharedFlow

interface InputManager {
    val activeKeys: SharedFlow<Set<Key>>
    val onKeyReleased: SharedFlow<Key>
}