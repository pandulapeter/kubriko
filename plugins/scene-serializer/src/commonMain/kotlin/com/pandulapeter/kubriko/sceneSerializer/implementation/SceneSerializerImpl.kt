package com.pandulapeter.kubriko.sceneSerializer.implementation

import com.pandulapeter.kubriko.sceneSerializer.SceneSerializer
import com.pandulapeter.kubriko.sceneSerializer.integration.Serializable
import com.pandulapeter.kubriko.sceneSerializer.integration.SerializableMetadata
import kotlinx.serialization.SerialName
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.reflect.KClass

internal class SceneSerializerImpl<MD : SerializableMetadata<out T>, out T : Serializable<out T>>(
    vararg editableMetadata: MD,
) : SceneSerializer<MD, T> {
    private val typeIdsToMetadata = editableMetadata.associate { registration -> registration.typeId to registration }
    private val typeIdsToDeserializers = editableMetadata.associate { registration -> registration.typeId to registration.deserializeState }
    private val typeResolvers = editableMetadata.associate { registration -> registration.type to registration.typeId }
    override val registeredTypeIds = typeIdsToDeserializers.keys
    private val json = Json { ignoreUnknownKeys = true }

    override fun getTypeId(type: KClass<out @UnsafeVariance T>) = typeResolvers[type]

    override fun getMetadata(typeId: String) = typeIdsToMetadata[typeId]

    override suspend fun serializeActors(
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

    override suspend fun deserializeActors(
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