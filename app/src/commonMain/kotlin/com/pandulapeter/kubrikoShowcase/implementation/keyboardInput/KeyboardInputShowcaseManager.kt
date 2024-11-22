package com.pandulapeter.kubrikoShowcase.implementation.keyboardInput

import androidx.compose.ui.input.key.Key
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputAware
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal class KeyboardInputShowcaseManager : Manager(), KeyboardInputAware {

    private val _activeKeys = MutableStateFlow(emptySet<Key>())
    val activeKeys = _activeKeys.asStateFlow()

    override fun onInitialize(kubriko: Kubriko) {
        kubriko.require<ActorManager>().add(this)
    }

    override fun handleActiveKeys(activeKeys: ImmutableSet<Key>) = _activeKeys.update { activeKeys }
}