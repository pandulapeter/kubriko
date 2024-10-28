package com.pandulapeter.gameTemplate.engine.implementation

import com.pandulapeter.gameTemplate.engine.Engine
import com.pandulapeter.gameTemplate.engine.implementation.managers.GameObjectManagerImpl
import com.pandulapeter.gameTemplate.engine.implementation.managers.InputManagerImpl
import com.pandulapeter.gameTemplate.engine.implementation.managers.MetadataManagerImpl
import com.pandulapeter.gameTemplate.engine.implementation.managers.SerializationManagerImpl
import com.pandulapeter.gameTemplate.engine.implementation.managers.StateManagerImpl
import com.pandulapeter.gameTemplate.engine.implementation.managers.ViewportManagerImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

internal object EngineImpl : Engine, CoroutineScope {

    override val gameObjectManager by lazy { GameObjectManagerImpl() }
    override val inputManager by lazy { InputManagerImpl() }
    override val metadataManager by lazy { MetadataManagerImpl() }
    override val serializationManager by lazy { SerializationManagerImpl() }
    override val stateManager by lazy { StateManagerImpl() }
    override val viewportManager by lazy { ViewportManagerImpl() }

    override val coroutineContext = SupervisorJob() + Dispatchers.Default
}