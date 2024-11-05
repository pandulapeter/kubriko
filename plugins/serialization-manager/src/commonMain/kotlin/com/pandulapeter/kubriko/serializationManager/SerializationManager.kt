package com.pandulapeter.kubriko.serializationManager

import com.pandulapeter.kubriko.serializationManager.implementation.SerializationManagerImpl
import com.pandulapeter.kubriko.serializationManager.integration.Serializable
import com.pandulapeter.kubriko.serializationManager.integration.SerializableMetadata
import com.pandulapeter.kubriko.manager.Manager
import kotlin.reflect.KClass

/**
 * TODO: Documentation
 */
abstract class SerializationManager<MD : SerializableMetadata<out T>, out T : Serializable<out T>> : Manager() {

    abstract val registeredTypeIds: Set<String>

    abstract fun getTypeId(type: KClass<out @UnsafeVariance T>): String?

    abstract fun getMetadata(typeId: String): MD?

    abstract fun serializeActors(actors: List<@UnsafeVariance T>): String

    abstract fun deserializeActors(serializedStates: String): List<T>

    companion object {

        /**
         * TODO: Documentation. Mention the shortcut in Metadata to this generic mess
         */
        fun <MD : SerializableMetadata<out T>, T : Serializable<out T>> newInstance(
            vararg serializableMetadata: MD,
        ): SerializationManager<MD, T> = SerializationManagerImpl(
            serializableMetadata = serializableMetadata,
        )
    }
}