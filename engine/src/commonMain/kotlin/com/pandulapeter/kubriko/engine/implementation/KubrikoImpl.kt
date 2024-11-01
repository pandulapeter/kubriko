package com.pandulapeter.kubriko.engine.implementation

import com.pandulapeter.kubriko.engine.Kubriko
import com.pandulapeter.kubriko.engine.editorIntegration.EditableMetadata
import com.pandulapeter.kubriko.engine.implementation.managers.InputManagerImpl
import com.pandulapeter.kubriko.engine.implementation.managers.InstanceManagerImpl
import com.pandulapeter.kubriko.engine.implementation.managers.MetadataManagerImpl
import com.pandulapeter.kubriko.engine.implementation.managers.SerializationManagerImpl
import com.pandulapeter.kubriko.engine.implementation.managers.StateManagerImpl
import com.pandulapeter.kubriko.engine.implementation.managers.ViewportManagerImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

internal class KubrikoImpl(
    vararg editableMetadata: EditableMetadata<*>,
) : Kubriko, CoroutineScope {
    override val coroutineContext = SupervisorJob() + Dispatchers.Default
    override val inputManager by lazy { InputManagerImpl(this) }
    override val instanceManager by lazy { InstanceManagerImpl(this) }
    override val metadataManager by lazy { MetadataManagerImpl(this) }
    override val serializationManager by lazy { SerializationManagerImpl(editableMetadata = editableMetadata) }
    override val stateManager by lazy { StateManagerImpl(this) }
    override val viewportManager by lazy { ViewportManagerImpl() }
}