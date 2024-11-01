package com.pandulapeter.kubriko.engine

import com.pandulapeter.kubriko.engine.Kubriko.Companion.newInstance
import com.pandulapeter.kubriko.engine.editorIntegration.EditableActorMetadata
import com.pandulapeter.kubriko.engine.implementation.KubrikoImpl
import com.pandulapeter.kubriko.engine.managers.InputManager
import com.pandulapeter.kubriko.engine.managers.InstanceManager
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
 */
interface Kubriko {

    val inputManager: InputManager
    val instanceManager: InstanceManager
    val metadataManager: MetadataManager
    val serializationManager: SerializationManager
    val stateManager: StateManager
    val viewportManager: ViewportManager

    /**
     * Designed to be used only for the Editor. Should return {false} under normal circumstances.
     * Setting this value to {true} has the following effects:
     * - A grid is drawn behind the Actors
     * - [Dynamic] Actors no longer receive update events
     * - [Editable] Actors will become visible through their {editorPreview} implementation
     */
    var isEditor: Boolean

    companion object {
        /**
         * Creates a new [Kubriko] instance.
         *
         * @param editableActorMetadata - Any number of actor types that should be registered for usage in the Editor.
         */
        fun newInstance(
            vararg editableActorMetadata: EditableActorMetadata<*>,
        ): Kubriko = KubrikoImpl(
            editableActorMetadata = editableActorMetadata,
        )
    }
}