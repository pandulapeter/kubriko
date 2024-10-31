package com.pandulapeter.kubriko.engine.implementation.managers

import com.pandulapeter.kubriko.engine.actor.Actor
import com.pandulapeter.kubriko.engine.actor.ActorRegistrationForEditor
import com.pandulapeter.kubriko.engine.actor.traits.AvailableInEditor
import com.pandulapeter.kubriko.engine.managers.SerializationManager
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.reflect.KClass

internal class SerializationManagerImpl(
    vararg actorTypesAvailableInEditor: ActorRegistrationForEditor<out Actor>,
) : SerializationManager {

    private val typeIdsToDeserializers = actorTypesAvailableInEditor.associate { registration -> registration.typeId to registration.deserializeState }
    private val typeResolvers = actorTypesAvailableInEditor.associate { registration -> registration.type to registration.typeId }
    override val typeIdsForEditor = typeIdsToDeserializers.keys
    private val json = Json { ignoreUnknownKeys = true }

    override fun resolveTypeId(type: KClass<*>) = typeResolvers[type].orEmpty()

    override suspend fun serializeInstanceStates(
        instanceStates: List<AvailableInEditor.State<*>>,
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