package com.pandulapeter.gameTemplate.engine

import com.pandulapeter.gameTemplate.engine.implementation.EngineImpl
import com.pandulapeter.gameTemplate.engine.managers.GameObjectManager
import com.pandulapeter.gameTemplate.engine.managers.InputManager
import com.pandulapeter.gameTemplate.engine.managers.MetadataManager
import com.pandulapeter.gameTemplate.engine.managers.StateManager
import com.pandulapeter.gameTemplate.engine.managers.ViewportManager

interface Engine {
    val gameObjectManager: GameObjectManager
    val inputManager: InputManager
    val metadataManager: MetadataManager
    val stateManager: StateManager
    val viewportManager: ViewportManager

    companion object {
        fun get(): Engine = EngineImpl
    }
}