package com.pandulapeter.kubriko.serialization.implementation

import com.pandulapeter.kubriko.serialization.SerializationManager
import com.pandulapeter.kubriko.serialization.integration.Serializable
import com.pandulapeter.kubriko.serialization.integration.SerializableMetadata
import kotlinx.serialization.SerialName
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.reflect.KClass

internal class SerializationManagerImpl<MD : SerializableMetadata<out T>, out T : Serializable<out T>>(
    vararg serializableMetadata: MD,
) : SerializationManager<MD, T>() {
    private val typeIdsToMetadata = serializableMetadata.associateBy { registration -> registration.typeId }
    private val typeIdsToDeserializers = serializableMetadata.associate { registration -> registration.typeId to registration.deserializeState }
    private val typeResolvers = serializableMetadata.associate { registration -> registration.type to registration.typeId }
    override val registeredTypeIds = typeIdsToDeserializers.keys
    private val json = Json { ignoreUnknownKeys = true }

    override fun getTypeId(type: KClass<out @UnsafeVariance T>) = typeResolvers[type]

    override fun getMetadata(typeId: String) = typeIdsToMetadata[typeId]

    override fun serializeActors(
        actors: List<@UnsafeVariance T>,
    ) = json.encodeToString(
        actors.mapNotNull { actor ->
            getTypeId(actor::class)?.let { typeId ->
                ActorStateWrapper(
                    typeId = typeId,
                    serializedState = actor.save().serialize(),
                )
            }
        }
    )

    override fun deserializeActors(
        serializedStates: String,
    ) = json.decodeFromString<List<ActorStateWrapper>>(serializedStates).mapNotNull { wrapper ->
        typeIdsToDeserializers[wrapper.typeId]?.invoke(wrapper.serializedState)?.restore()
    }

    @kotlinx.serialization.Serializable
    private data class ActorStateWrapper(
        @SerialName("typeId") val typeId: String,
        @SerialName("state") val serializedState: String,
    )
}