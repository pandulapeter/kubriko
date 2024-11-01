package com.pandulapeter.kubriko.engine.implementation.managers

import com.pandulapeter.kubriko.engine.editorIntegration.EditableActorMetadata
import com.pandulapeter.kubriko.engine.managers.SerializationManager
import com.pandulapeter.kubriko.engine.traits.Editable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.reflect.KClass

internal class SerializationManagerImpl(
    vararg editableActorMetadata: EditableActorMetadata<out Editable<*>>,
) : SerializationManager {

    private val typeIdsToDeserializers = editableActorMetadata.associate { registration -> registration.typeId to registration.deserializeState }
    private val typeResolvers = editableActorMetadata.associate { registration -> registration.type to registration.typeId }
    override val typeIdsForEditor = typeIdsToDeserializers.keys
    private val json = Json { ignoreUnknownKeys = true }

    override fun resolveTypeId(type: KClass<*>) = typeResolvers[type]

    override suspend fun serializeActors(
        actors: List<Editable<*>>,
    ) = json.encodeToString(
        actors.mapNotNull { actor ->
            resolveTypeId(actor::class)?.let { typeId ->
                InstanceStateWrapper(
                    typeId = typeId,
                    serializedState = actor.save().serialize(),
                )
            }
        }
    )

    override suspend fun deserializeActors(
        serializedStates: String,
    ) = json.decodeFromString<List<InstanceStateWrapper>>(serializedStates).mapNotNull { wrapper ->
        typeIdsToDeserializers[wrapper.typeId]?.invoke(wrapper.serializedState)?.restore()
    }


    @Serializable
    private data class InstanceStateWrapper(
        @SerialName("typeId") val typeId: String,
        @SerialName("state") val serializedState: String,
    )
}