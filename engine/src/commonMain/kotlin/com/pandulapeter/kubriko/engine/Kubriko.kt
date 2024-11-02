package com.pandulapeter.kubriko.engine

import com.pandulapeter.kubriko.engine.Kubriko.Companion.newInstance
import com.pandulapeter.kubriko.engine.editorIntegration.EditableMetadata
import com.pandulapeter.kubriko.engine.implementation.KubrikoImpl
import com.pandulapeter.kubriko.engine.managers.InputManager
import com.pandulapeter.kubriko.engine.managers.ActorManager
import com.pandulapeter.kubriko.engine.managers.MetadataManager
import com.pandulapeter.kubriko.engine.managers.SerializationManager
import com.pandulapeter.kubriko.engine.managers.StateManager
import com.pandulapeter.kubriko.engine.managers.ViewportManager
import com.pandulapeter.kubriko.engine.traits.Dynamic
import com.pandulapeter.kubriko.engine.traits.Editable

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
    val serializationManager: SerializationManager
    val stateManager: StateManager
    val viewportManager: ViewportManager

    /**
     * Designed to be used only for the Scene Editor. Should return {false} under normal circumstances.
     * Setting this value to {true} has the following effects:
     * - [Dynamic] Actors no longer receive update events
     * - [Editable] Actors will become visible through their [Editable.editorPreview] implementations
     */
    var isEditor: Boolean

    companion object {
        /**
         * Creates a new [Kubriko] instance.
         *
         * @param editableMetadata - Any number of actor types that should be registered for usage in the Scene Editor.
         */
        fun newInstance(
            vararg editableMetadata: EditableMetadata<*>,
        ): Kubriko = KubrikoImpl(
            editableMetadata = editableMetadata,
        )
    }
}