package com.pandulapeter.kubriko.sceneSerializer.implementation

import com.pandulapeter.kubriko.sceneSerializer.Editable
import com.pandulapeter.kubriko.sceneSerializer.SceneSerializer
import com.pandulapeter.kubriko.sceneSerializer.integration.EditableMetadata
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.reflect.KClass

internal class SceneSerializerImpl(
    vararg editableMetadata: EditableMetadata<out Editable<*>>,
) : SceneSerializer {

    private val typeIdsToDeserializers = editableMetadata.associate { registration -> registration.typeId to registration.deserializeState }
    private val typeResolvers = editableMetadata.associate { registration -> registration.type to registration.typeId }
    override val registeredTypeIds = typeIdsToDeserializers.keys
    private val json = Json { ignoreUnknownKeys = true }

    override fun getTypeId(type: KClass<out Editable<*>>) = typeResolvers[type]

    override fun getType(typeId: String): KClass<out Editable<*>>? = typeResolvers.entries.firstOrNull { (_, value) -> value == typeId }?.key

    override suspend fun serializeActors(
        actors: List<Editable<*>>,
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

    @Serializable
    private data class ActorStateWrapper(
        @SerialName("typeId") val typeId: String,
        @SerialName("state") val serializedState: String,
    )
}