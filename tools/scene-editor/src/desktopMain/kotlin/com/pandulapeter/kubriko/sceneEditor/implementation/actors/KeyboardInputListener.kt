package com.pandulapeter.kubriko.sceneEditor.implementation.actors

import androidx.compose.ui.input.key.Key
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputAware
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.sceneEditor.implementation.helpers.handleKeyPressed
import com.pandulapeter.kubriko.sceneEditor.implementation.helpers.handleKeys
import kotlinx.collections.immutable.ImmutableSet

internal class KeyboardInputListener(
    private val viewportManager: ViewportManager,
    private val navigateBack: () -> Unit,
) : KeyboardInputAware, Unique {

    override fun handleActiveKeys(activeKeys: ImmutableSet<Key>) = viewportManager.handleKeys(activeKeys)

    override fun onKeyPressed(key: Key) = handleKeyPressed(
        key = key,
        onNavigateBackRequested = navigateBack,
    )
}