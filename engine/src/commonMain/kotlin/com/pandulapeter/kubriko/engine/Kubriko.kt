package com.pandulapeter.kubriko.engine

import com.pandulapeter.kubriko.engine.gameObject.traits.AvailableInEditor
import com.pandulapeter.kubriko.engine.implementation.KubrikoImpl
import com.pandulapeter.kubriko.engine.managers.InputManager
import com.pandulapeter.kubriko.engine.managers.InstanceManager
import com.pandulapeter.kubriko.engine.managers.MetadataManager
import com.pandulapeter.kubriko.engine.managers.SerializationManager
import com.pandulapeter.kubriko.engine.managers.StateManager
import com.pandulapeter.kubriko.engine.managers.ViewportManager
import kotlin.reflect.KClass

interface Kubriko {
    val inputManager: InputManager
    val instanceManager: InstanceManager
    val metadataManager: MetadataManager
    val serializationManager: SerializationManager
    val stateManager: StateManager
    val viewportManager: ViewportManager

    companion object {
        fun newInstance(
            vararg typesAvailableInEditor: Triple<String, KClass<*>, (String) -> AvailableInEditor.State<*>>
        ): Kubriko = KubrikoImpl(
            typesAvailableInEditor = typesAvailableInEditor,
        )
    }
}