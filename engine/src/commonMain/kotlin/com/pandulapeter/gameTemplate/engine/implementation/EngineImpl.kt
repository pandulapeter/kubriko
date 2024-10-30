package com.pandulapeter.gameTemplate.engine.implementation

import com.pandulapeter.gameTemplate.engine.Engine
import com.pandulapeter.gameTemplate.engine.implementation.managers.InputManagerImpl
import com.pandulapeter.gameTemplate.engine.implementation.managers.InstanceManagerImpl
import com.pandulapeter.gameTemplate.engine.implementation.managers.MetadataManagerImpl
import com.pandulapeter.gameTemplate.engine.implementation.managers.SerializationManagerImpl
import com.pandulapeter.gameTemplate.engine.implementation.managers.StateManagerImpl
import com.pandulapeter.gameTemplate.engine.implementation.managers.ViewportManagerImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

internal class EngineImpl : Engine, CoroutineScope {

    override val coroutineContext = SupervisorJob() + Dispatchers.Default
    override val inputManager by lazy { InputManagerImpl(this) }
    override val instanceManager by lazy { InstanceManagerImpl(this) }
    override val metadataManager by lazy { MetadataManagerImpl(this) }
    override val serializationManager by lazy { SerializationManagerImpl(this) }
    override val stateManager by lazy { StateManagerImpl(this) }
    override val viewportManager by lazy { ViewportManagerImpl() }
}