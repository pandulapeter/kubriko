package com.pandulapeter.kubriko.sceneSerializer

import com.pandulapeter.kubriko.sceneSerializer.implementation.SceneSerializerImpl
import com.pandulapeter.kubriko.sceneSerializer.integration.Serializable
import com.pandulapeter.kubriko.sceneSerializer.integration.SerializableMetadata
import kotlin.reflect.KClass

/**
 * TODO: Documentation
 */
interface SceneSerializer<MD : SerializableMetadata<out T>, out T : Serializable<out T>> {

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
        ): SceneSerializer<MD, T> = SceneSerializerImpl(
            serializableMetadata = serializableMetadata,
        )
    }
}