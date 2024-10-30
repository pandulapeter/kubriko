package com.pandulapeter.gameTemplate.engine

import com.pandulapeter.gameTemplate.engine.gameObject.traits.AvailableInEditor
import com.pandulapeter.gameTemplate.engine.implementation.EngineImpl
import com.pandulapeter.gameTemplate.engine.managers.InputManager
import com.pandulapeter.gameTemplate.engine.managers.InstanceManager
import com.pandulapeter.gameTemplate.engine.managers.MetadataManager
import com.pandulapeter.gameTemplate.engine.managers.SerializationManager
import com.pandulapeter.gameTemplate.engine.managers.StateManager
import com.pandulapeter.gameTemplate.engine.managers.ViewportManager
import kotlin.reflect.KClass

interface Engine {
    val inputManager: InputManager
    val instanceManager: InstanceManager
    val metadataManager: MetadataManager
    val serializationManager: SerializationManager
    val stateManager: StateManager
    val viewportManager: ViewportManager

    companion object {
        fun newInstance(
            vararg typesAvailableInEditor: Triple<String, KClass<*>, (String) -> AvailableInEditor.State<*>>
        ): Engine = EngineImpl(
            typesAvailableInEditor = typesAvailableInEditor,
        )
    }
}