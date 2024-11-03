package com.pandulapeter.kubriko

import com.pandulapeter.kubriko.Kubriko.Companion.newInstance
import com.pandulapeter.kubriko.implementation.KubrikoImpl
import com.pandulapeter.kubriko.managers.ActorManager
import com.pandulapeter.kubriko.managers.InputManager
import com.pandulapeter.kubriko.managers.MetadataManager
import com.pandulapeter.kubriko.managers.StateManager
import com.pandulapeter.kubriko.managers.ViewportManager

/**
 * Holds references to the individual Manager classes that control the different aspects of a game.
 * See the documentations of the specific Managers for detailed information.
 * Use the static [newInstance] function to instantiate a [Kubriko] implementation.
 * Provide that instance to the [EngineCanvas] Composable to draw the game world.
 */
interface Kubriko {

    val actorManager: ActorManager
    val inputManager: InputManager
    val metadataManager: MetadataManager
    val stateManager: StateManager
    val viewportManager: ViewportManager

    companion object {
        /**
         * Creates a new [Kubriko] instance.
         */
        fun newInstance(): Kubriko = KubrikoImpl()
    }
}