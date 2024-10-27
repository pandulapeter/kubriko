package com.pandulapeter.gameTemplate.engine

import com.pandulapeter.gameTemplate.engine.managers.ViewportManager
import com.pandulapeter.gameTemplate.engine.managers.GameObjectManager
import com.pandulapeter.gameTemplate.engine.managers.MetadataManager
import com.pandulapeter.gameTemplate.engine.managers.StateManager
import com.pandulapeter.gameTemplate.engine.implementation.EngineImpl
import com.pandulapeter.gameTemplate.engine.managers.InputManager

interface Engine {
    val gameObjectManager: GameObjectManager
    val inputManager: InputManager
    val metadataManager: MetadataManager
    val stateManager: StateManager
    val viewportManager: ViewportManager

    companion object {
        const val MAPS_LOCATION = "gameplay-controller/src/commonMain/maps"

        fun get(): Engine = EngineImpl
    }
}