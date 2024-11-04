package com.pandulapeter.kubriko.actorSerializer

import com.pandulapeter.kubriko.actorSerializer.implementation.ActorSerializerImpl
import com.pandulapeter.kubriko.actorSerializer.integration.Serializable
import com.pandulapeter.kubriko.actorSerializer.integration.SerializableMetadata
import kotlin.reflect.KClass

/**
 * TODO: Documentation
 */
interface ActorSerializer<MD : SerializableMetadata<out T>, out T : Serializable<out T>> {

    val registeredTypeIds: Set<String>

    fun getTypeId(type: KClass<out @UnsafeVariance T>): String?

    fun getMetadata(typeId: String): MD?

    suspend fun serializeActors(actors: List<@UnsafeVariance T>): String

    suspend fun deserializeActors(serializedStates: String): List<T>

    companion object {

        /**
         * TODO: Documentation. Mention the shortcut in Metadata to this generic mess
         */
        fun <MD : SerializableMetadata<out T>, T : Serializable<out T>> newInstance(
            vararg serializableMetadata: MD,
        ): ActorSerializer<MD, T> = ActorSerializerImpl(
            serializableMetadata = serializableMetadata,
        )
    }
}