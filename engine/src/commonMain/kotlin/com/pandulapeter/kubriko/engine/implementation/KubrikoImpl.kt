package com.pandulapeter.kubriko.engine.implementation

import com.pandulapeter.kubriko.engine.Kubriko
import com.pandulapeter.kubriko.engine.gameObject.traits.AvailableInEditor
import com.pandulapeter.kubriko.engine.implementation.managers.InputManagerImpl
import com.pandulapeter.kubriko.engine.implementation.managers.InstanceManagerImpl
import com.pandulapeter.kubriko.engine.implementation.managers.MetadataManagerImpl
import com.pandulapeter.kubriko.engine.implementation.managers.SerializationManagerImpl
import com.pandulapeter.kubriko.engine.implementation.managers.StateManagerImpl
import com.pandulapeter.kubriko.engine.implementation.managers.ViewportManagerImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.reflect.KClass

internal class KubrikoImpl(
    vararg typesAvailableInEditor: Triple<String, KClass<*>, (String) -> AvailableInEditor.State<*>>
) : Kubriko, CoroutineScope {
    override val coroutineContext = SupervisorJob() + Dispatchers.Default
    override val inputManager by lazy { InputManagerImpl(this) }
    override val instanceManager by lazy { InstanceManagerImpl(this) }
    override val metadataManager by lazy { MetadataManagerImpl(this) }
    override val serializationManager by lazy { SerializationManagerImpl(serializableTypes = typesAvailableInEditor) }
    override val stateManager by lazy { StateManagerImpl(this) }
    override val viewportManager by lazy { ViewportManagerImpl() }
}