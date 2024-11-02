package com.pandulapeter.kubriko.implementation

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.sceneEditorIntegration.EditableMetadata
import com.pandulapeter.kubriko.implementation.managers.InputManagerImpl
import com.pandulapeter.kubriko.implementation.managers.ActorManagerImpl
import com.pandulapeter.kubriko.implementation.managers.MetadataManagerImpl
import com.pandulapeter.kubriko.implementation.managers.SerializationManagerImpl
import com.pandulapeter.kubriko.implementation.managers.StateManagerImpl
import com.pandulapeter.kubriko.implementation.managers.ViewportManagerImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

internal class KubrikoImpl(
    vararg editableMetadata: EditableMetadata<*>,
) : Kubriko, CoroutineScope {

    override val actorManager by lazy { ActorManagerImpl(this) }
    override val inputManager by lazy { InputManagerImpl(this) }
    override val metadataManager by lazy { MetadataManagerImpl(this) }
    override val serializationManager by lazy { SerializationManagerImpl(editableMetadata = editableMetadata) }
    override val stateManager by lazy { StateManagerImpl(this) }
    override val viewportManager by lazy { ViewportManagerImpl() }

    override val coroutineContext = SupervisorJob() + Dispatchers.Default
    override var isEditor = false
}