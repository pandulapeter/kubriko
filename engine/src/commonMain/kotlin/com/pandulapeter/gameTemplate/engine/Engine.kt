package com.pandulapeter.gameTemplate.engine

import com.pandulapeter.gameTemplate.engine.managers.ViewportManager
import com.pandulapeter.gameTemplate.engine.managers.GameObjectManager
import com.pandulapeter.gameTemplate.engine.managers.MetadataManager
import com.pandulapeter.gameTemplate.engine.managers.StateManager
import com.pandulapeter.gameTemplate.engine.implementation.EngineImpl

interface Engine {
    val viewportManager: ViewportManager
    val stateManager: StateManager
    val gameObjectManager: GameObjectManager
    val metadataManager: MetadataManager

    companion object {
        fun get(): Engine = EngineImpl
    }
}