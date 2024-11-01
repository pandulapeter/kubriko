package com.pandulapeter.kubriko.engine.implementation.managers

import com.pandulapeter.kubriko.engine.editorIntegration.EditableMetadata
import com.pandulapeter.kubriko.engine.traits.Editable
import com.pandulapeter.kubriko.engine.managers.SerializationManager
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.reflect.KClass

internal class SerializationManagerImpl(
    vararg editableMetadata: EditableMetadata<out Editable<*>>,
) : SerializationManager {

    private val typeIdsToDeserializers = editableMetadata.associate { registration -> registration.typeId to registration.deserializeState }
    private val typeResolvers = editableMetadata.associate { registration -> registration.type to registration.typeId }
    override val typeIdsForEditor = typeIdsToDeserializers.keys
    private val json = Json { ignoreUnknownKeys = true }

    override fun resolveTypeId(type: KClass<*>) = typeResolvers[type].orEmpty()

    override suspend fun serializeInstanceStates(
        instanceStates: List<Editable.State<out Editable<*>>>,
    ) = json.encodeToString(
        instanceStates.map { state ->
            InstanceStateWrapper(
                typeId = state.typeId,
                serializedState = state.serialize(),
            )
        }
    )

    override suspend fun deserializeInstanceStates(
        serializedStates: String,
    ) = json.decodeFromString<List<InstanceStateWrapper>>(serializedStates).mapNotNull { wrapper ->
        typeIdsToDeserializers[wrapper.typeId]?.invoke(wrapper.serializedState)
    }

    @Serializable
    private data class InstanceStateWrapper(
        @SerialName("typeId") val typeId: String,
        @SerialName("state") val serializedState: String,
    )
}