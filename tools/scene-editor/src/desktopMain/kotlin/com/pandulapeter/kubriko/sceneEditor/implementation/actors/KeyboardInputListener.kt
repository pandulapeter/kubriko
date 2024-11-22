package com.pandulapeter.kubriko.sceneEditor.implementation.actors

import androidx.compose.ui.input.key.Key
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputAware
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.sceneEditor.implementation.helpers.handleKeyReleased
import com.pandulapeter.kubriko.sceneEditor.implementation.helpers.handleKeys
import kotlinx.collections.immutable.ImmutableSet

internal class KeyboardInputListener(
    private val viewportManager: ViewportManager,
    private val navigateBack: () -> Unit,
) : KeyboardInputAware, Unique {

    override fun handleActiveKeys(activeKeys: ImmutableSet<Key>) = viewportManager.handleKeys(activeKeys)

    override fun onKeyReleased(key: Key) = handleKeyReleased(
        key = key,
        onNavigateBackRequested = navigateBack,
    )
}